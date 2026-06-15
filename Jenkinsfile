// ============================================================
//  YAS Monorepo – Jenkins CI Pipeline (Merged)
//  Controller (AWS): orchestration + UI + logs.
//  Agents (máy nhóm): build/test/scan — label: yas-build-worker
//  SonarQube: http://3.27.92.213:9000
//  JDK/Maven tools: JDK-25, Maven (Global Tool Configuration)
// ============================================================
//
//  Pipeline flow:
//  1. Checkout + fetch origin/main (fix shallow clone)
//  2. Detect Changed Modules (shell script, so sánh với origin/main)
//  3. Gitleaks – Secret Scan (luôn chạy, fail nếu phát hiện secret)
//  4. Test + JaCoCo report (chỉ modules thay đổi, skip Integration Tests)
//  5. Coverage Gate (≥ 70%, graceful skip nếu không có code)
//  6. Build (chỉ modules thay đổi)
//  7. SonarQube Analysis (withSonarQubeEnv — liên kết Quality Gate)
//  7b. SonarQube Quality Gate (waitForQualityGate — cần webhook Sonar → Jenkins)
//  8. Snyk Dependency Scan (chỉ modules thay đổi)
//  9. Build/push Docker Hub images from services.yaml after all gates pass
// 10. Update GitOps desired state; ArgoCD owns cluster sync
// ============================================================

def loadServiceCatalog() {
    readYaml file: 'services.yaml'
}

def deployableServicesFromCatalog() {
    def catalog = loadServiceCatalog()
    catalog.services.findAll { service ->
        service.deploy == true && service.path && service.dockerfile
    }
}

def changedDeployableServices(String changedModules) {
    def services = deployableServicesFromCatalog()
    if (!changedModules || changedModules == '__skip_full_ci__') {
        return []
    }

    def changedFiles = changedFilesForCurrentBuild()
    if (changedFiles.isEmpty()) {
        return services
    }

    def serviceChanges = services.findAll { service ->
        def pathPrefix = "${service.path}/" as String
        changedFiles.any { file -> file == service.path || file.startsWith(pathPrefix) }
    }
    if (!serviceChanges.isEmpty()) {
        return serviceChanges
    }

    def sharedImageInputs = [
        'pom.xml',
        'common-library/',
        'docker/',
        'k8s/',
        'scripts/'
    ]
    if (changedFiles.any { file -> sharedImageInputs.any { prefix -> file == prefix || file.startsWith(prefix) } }) {
        return services
    }

    []
}

def changedFilesForCurrentBuild() {
    def baseRef = ''
    if (env.CHANGE_TARGET) {
        baseRef = "origin/${env.CHANGE_TARGET}"
    } else if (sh(script: 'git rev-parse --verify HEAD~1 >/dev/null 2>&1', returnStatus: true) == 0) {
        baseRef = 'HEAD~1'
    }
    if (!baseRef) {
        return []
    }
    sh(script: "git diff --name-only '${baseRef}'...HEAD || true", returnStdout: true)
        .trim()
        .split('\n')
        .collect { it.trim() }
        .findAll { it }
}

def sourceDirectoriesFromCatalog() {
    def catalog = loadServiceCatalog()
    catalog.services.collect { service -> service.path }
        .findAll { path -> path && fileExists("${path}/src/main/java") }
        .collect { path -> [path: "${path}/src/main/java"] }
}

def dockerTagsForCurrentRef() {
    def commitTag = env.GIT_COMMIT ? env.GIT_COMMIT.take(12) : sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()

    if (env.TAG_NAME) {
        if (!(env.TAG_NAME ==~ /^v\d+\.\d+\.\d+$/)) {
            error "Release/staging tags must match vX.Y.Z; got ${env.TAG_NAME}"
        }
        return [commitTag, env.TAG_NAME]
    }

    if (env.BRANCH_NAME == 'main') {
        return [commitTag, 'main', 'latest']
    }

    [commitTag]
}

def imageForService(String dockerhubUser, service, String tag) {
    def catalog = loadServiceCatalog()
    def template = catalog.registry?.imageTemplate ?: 'docker.io/${DOCKERHUB_USERNAME}/yas-${service}:${tag}'
    def imageName = service.imageName ?: "yas-${service.name}"
    def serviceToken = imageName.startsWith('yas-') ? imageName.substring(4) : imageName
    template
        .replace('${DOCKERHUB_USERNAME}', dockerhubUser)
        .replace('${service}', serviceToken as String)
        .replace('${tag}', tag)
}

def buildAndPushImages(List services, List tags) {
    if (services.isEmpty()) {
        echo 'Docker Hub: no deployable changed services'
        return
    }

    withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
        sh 'printf "%s" "$DOCKERHUB_TOKEN" | docker login docker.io -u "$DOCKERHUB_USERNAME" --password-stdin'
        for (service in services) {
            def primaryImage = imageForService(env.DOCKERHUB_USERNAME, service, tags[0])
            sh "docker build -t '${primaryImage}' -f '${service.dockerfile}' '${service.path}'"
            for (tag in tags.drop(1)) {
                sh "docker tag '${primaryImage}' '${imageForService(env.DOCKERHUB_USERNAME, service, tag)}'"
            }
            for (tag in tags) {
                sh "docker push '${imageForService(env.DOCKERHUB_USERNAME, service, tag)}'"
            }
        }
        sh 'docker logout docker.io || true'
    }
}

def updateGitOpsOverlay(String overlay, List services, String tag, String messagePrefix) {
    if (services.isEmpty()) {
        echo "GitOps ${overlay}: no service images to update"
        return
    }
    if (overlay == 'staging' && !(tag ==~ /^v\d+\.\d+\.\d+$/)) {
        error "Staging GitOps updates require immutable vX.Y.Z tags; got ${tag}"
    }
    if (overlay == 'staging' && (tag == 'latest' || tag == 'main')) {
        error "Staging cannot use mutable image tag ${tag}"
    }

    withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
        sshagent(credentials: ['github-gitops-ssh']) {
            dir("deploy/gitops/overlays/${overlay}") {
                for (service in services) {
                    def image = imageForService(env.DOCKERHUB_USERNAME, service, tag)
                    def imageName = service.imageName ?: "yas-${service.name}"
                    sh """
                        set -euo pipefail
                        old_image=\$(awk '/name: .*\\/${imageName}\$/ { print \$3; exit }' kustomization.yaml)
                        if [ -z "\${old_image}" ]; then
                            old_image="${imageName}"
                        fi
                        kustomize edit set image "\${old_image}=${image}"
                    """
                }
            }
            sh """
                set -euo pipefail
                git config user.email "jenkins-cd@local"
                git config user.name "Jenkins CD"
                if git diff --quiet -- deploy/gitops/overlays/${overlay}; then
                    echo "GitOps ${overlay}: no changes"
                else
                    git add deploy/gitops/overlays/${overlay}
                    git diff --cached --check
                    git commit -m "${messagePrefix}: update ${overlay} images to ${tag} [skip ci]"
                    git push origin HEAD:${env.GITOPS_BRANCH}
                fi
            """
        }
    }
}

def selectedServicesFromScope(String serviceScope) {
    def services = deployableServicesFromCatalog()
    if (!serviceScope?.trim()) {
        return services
    }
    def names = serviceScope.split(',').collect { it.trim() }.findAll { it }
    def selected = services.findAll { service -> names.contains(service.name as String) }
    if (selected.size() != names.size()) {
        def found = selected.collect { it.name as String }
        error "Unknown or non-deployable services in SERVICE_SCOPE: ${names.findAll { !found.contains(it) }.join(',')}"
    }
    selected
}

pipeline {
    agent {
        label 'yas-build-worker'
    }

    // JDK/Maven: resolve once via tool step (avoids repeating "Tool Installation" every stage).
    // Names must match Jenkins → Manage Jenkins → Tools (JDK-25, Maven).

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'CD_ACTION',
               choices: ['auto', 'developer_build_stub', 'teardown_developer', 'deploy_dev', 'release_staging', 'rollback_environment', 'cluster_smoke_check'],
               description: 'auto runs multibranch CI/CD. Other values let dedicated Jenkins jobs reuse this Jenkinsfile.')
        string(name: 'SERVICE_SCOPE', defaultValue: '', description: 'Comma-separated catalog service names. Empty means all deployable services for manual CD actions.')
        string(name: 'IMAGE_TAG', defaultValue: 'main', description: 'developer_build_stub only: already-built image tag to write into developer GitOps.')
        string(name: 'RELEASE_TAG', defaultValue: '', description: 'release_staging only: immutable tag in vX.Y.Z format.')
        string(name: 'ROLLBACK_TAG', defaultValue: '', description: 'rollback_environment only: image tag to write into the target GitOps overlay.')
        choice(name: 'TARGET_ENV', choices: ['developer', 'dev', 'staging'], description: 'Manual CD target environment.')
    }

    environment {
        COVERAGE_THRESHOLD       = '70'
        SONAR_HOST_URL           = 'http://3.27.92.213:9000'
        // Phải trùng tên SonarQube server trong Manage Jenkins → System (SonarQube installations).
        SONARQUBE_INSTALLATION   = 'sonar-server'
        MAVEN_OPTS               = '-Xmx512m -XX:MaxMetaspaceSize=256m'
        GITLEAKS_EXPECTED        = '8.18.4'
        GITOPS_BRANCH            = 'lab2/cd-platform'
    }

    stages {

        // ══════════════════════════════════════════════════════
        // STAGE 1 – Checkout & Prepare
        // ══════════════════════════════════════════════════════
        stage('Checkout') {
            steps {
                script {
                    def jdkHome = tool name: 'JDK-25', type: 'jdk'
                    env.JAVA_HOME = jdkHome
                    env.PATH = "${jdkHome}/bin:${env.PATH}"
                    def mvnHome = tool name: 'Maven', type: 'maven'
                    env.M2_HOME = mvnHome
                    env.PATH = "${mvnHome}/bin:${env.PATH}"
                }
                checkout scm
                sh 'git fetch origin main:refs/remotes/origin/main || true'
                // Snyk Maven plugin runs ./mvnw when present; without +x → spawn EACCES → exit -13.
                sh 'find . -path ./.git -prune -o -name mvnw -type f -print -exec chmod +x {} +'
                sh 'chmod +x ci/detect-changed-modules.sh ci/check-coverage.sh ci/verify-ci-tools.sh'
                sh 'if [ -f ci/validate-service-catalog.sh ]; then chmod +x ci/validate-service-catalog.sh; fi'
                sh 'ci/verify-ci-tools.sh'
            }
        }

        stage('Validate Service Catalog') {
            steps {
                sh '''
                    if [ -x ci/validate-service-catalog.sh ] && command -v yq >/dev/null 2>&1; then
                        ci/validate-service-catalog.sh
                    else
                        echo "Catalog validator script or yq unavailable; using Jenkins readYaml validation."
                    fi
                '''
                script {
                    def catalog = loadServiceCatalog()
                    if (catalog.version != 1) {
                        error "services.yaml version must be 1"
                    }
                    if (!catalog.registry?.imageTemplate?.contains('docker.io/${DOCKERHUB_USERNAME}/yas-${service}:${tag}')) {
                        error 'services.yaml registry.imageTemplate must target docker.io/${DOCKERHUB_USERNAME}/yas-${service}:${tag}'
                    }
                    for (service in catalog.services) {
                        if (service.deploy == true) {
                            if (!service.path || !fileExists(service.path as String)) {
                                error "Catalog service ${service.name} path is missing: ${service.path}"
                            }
                            if (!service.dockerfile || !fileExists(service.dockerfile as String)) {
                                error "Catalog service ${service.name} Dockerfile is missing: ${service.dockerfile}"
                            }
                            if (!service.chart || !fileExists(service.chart as String)) {
                                error "Catalog service ${service.name} chart is missing: ${service.chart}"
                            }
                        } else if (!service.exclusionReason) {
                            error "Non-deployable catalog service ${service.name} must document exclusionReason"
                        }
                    }
                    echo "Validated ${catalog.services.size()} catalog services"
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 2 – Detect Changed Modules (monorepo path filter)
        // So sánh với origin/main để lấy đúng danh sách thay đổi
        // ══════════════════════════════════════════════════════
        stage('Detect Changed Modules') {
            steps {
                script {
                    env.CHANGED_MODULES = sh(
                        script: 'ci/detect-changed-modules.sh',
                        returnStdout: true
                    ).trim()
                    echo "CHANGED_MODULES=${env.CHANGED_MODULES}"
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 2b – Validate docs/GitOps/spec-only changes
        // Avoid full Maven/image work for commits generated by GitOps CD jobs.
        // ══════════════════════════════════════════════════════
        stage('Validate Non-Code Changes') {
            when {
                expression { env.CHANGED_MODULES == '__skip_full_ci__' }
            }
            steps {
                sh '''
                    set -euo pipefail
                    echo "Docs/GitOps/spec-only change detected; skipping full Maven CI."
                    find deploy/gitops docs .agents .specify -type f \\( -name '*.yaml' -o -name '*.yml' -o -name '*.md' \\) -print >/tmp/lab2-non-code-files.txt
                    test -s /tmp/lab2-non-code-files.txt
                '''
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 3 – Gitleaks: Secret Scanning
        // Chạy TRƯỚC test/build – phát hiện secret → FAIL ngay
        // ══════════════════════════════════════════════════════
        stage('Gitleaks – Secret Scan') {
            steps {
                sh '''
                    if [ -f gitleaks-baseline.json ]; then
                        gitleaks detect --source=. --no-git --config=gitleaks.toml --baseline-path=gitleaks-baseline.json --report-path=gitleaks-report.json --exit-code=1
                    else
                        gitleaks detect --source=. --no-git --config=gitleaks.toml --report-path=gitleaks-report.json --exit-code=1
                    fi
                    echo "Gitleaks: OK"
                '''
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 4 – Test (JUnit + JaCoCo)
        // Chỉ test modules thay đổi, skip Integration Tests
        // ══════════════════════════════════════════════════════
        stage('Test') {
            when {
                expression { env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    def modules = env.CHANGED_MODULES.split(',')
                    for (module in modules) {
                        sh "mvn -B -ntp -pl ${module} -am test jacoco:report -DskipITs"
                    }
                }
            }
            post {
                always {
                    // 1. Thu thập báo cáo kết quả các case Test (JUnit)
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'

                    // 2. JaCoCo: biểu đồ / source painting trên UI — không qualityGates ở đây (gate 70% ở stage Coverage Gate + ci/check-coverage.sh).
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        id: 'jacoco',
                        name: 'JaCoCo Coverage',

                        // Khai báo đường dẫn để hiển thị mã nguồn (fix lỗi Source file not found)
                        sourceDirectories: sourceDirectoriesFromCatalog()
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 5 – Coverage Gate (≥ 70%)
        // Graceful skip nếu module không có executable code
        // ══════════════════════════════════════════════════════
        stage('Coverage Gate') {
            when {
                expression { env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    def modules = env.CHANGED_MODULES.split(',')
                    def serviceModules = modules.findAll { it != 'common-library' }

                    if (serviceModules.isEmpty()) {
                        echo "Coverage gate: skip"
                    } else {
                        for (module in serviceModules) {
                            sh "ci/check-coverage.sh ${module} ${env.COVERAGE_THRESHOLD}"
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 6 – Build
        // Chỉ build modules thay đổi, skip tests (đã chạy ở trên)
        // ══════════════════════════════════════════════════════
        stage('Build') {
            when {
                expression { env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    def modules = env.CHANGED_MODULES.split(',')
                    for (module in modules) {
                        sh "mvn -B -ntp -DskipTests -pl ${module} -am package"
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 7 – SonarQube Analysis
        // withSonarQubeEnv: Jenkins liên kết lần phân tích với server → hỗ trợ Quality Gate + badge.
        // ══════════════════════════════════════════════════════
        stage('SonarQube – Analysis') {
            when {
                expression { env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    def modules = env.CHANGED_MODULES.split(',')
                    def serviceModules = modules.findAll { it != 'common-library' }

                    if (serviceModules.isEmpty()) {
                        echo "SonarQube: skip"
                    } else {
                        def plArg = serviceModules.join(',')
                        echo "SonarQube: ${plArg}"
                        withSonarQubeEnv("${env.SONARQUBE_INSTALLATION}") {
                            withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                sh """
                                    mvn sonar:sonar \\
                                      -pl ${plArg} -am \\
                                      -Dsonar.host.url=${SONAR_HOST_URL} \\
                                      -Dsonar.token=${SONAR_TOKEN} \\
                                      -Dsonar.organization= \\
                                      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                                """
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 7b – SonarQube Quality Gate (nhận kết quả từ Sonar)
        // Cần webhook SonarQube → Jenkins (Administration → Webhooks). abortPipeline: false = không đỏ build khi QG fail.
        // ══════════════════════════════════════════════════════
        stage('SonarQube – Quality Gate') {
            when {
                expression { env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    def modules = env.CHANGED_MODULES.split(',')
                    def serviceModules = modules.findAll { it != 'common-library' }

                    if (serviceModules.isEmpty()) {
                        echo "SonarQube Quality Gate: skip"
                    } else {
                        withSonarQubeEnv("${env.SONARQUBE_INSTALLATION}") {
                            timeout(time: 15, unit: 'MINUTES') {
                                waitForQualityGate abortPipeline: false
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 8 – Snyk: Dependency Vulnerability Scan
        // Chỉ scan modules thay đổi
        // ══════════════════════════════════════════════════════
        stage('Snyk – Dependency Scan') {
            when {
                expression { env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        // Explicit org avoids REST org-metadata lookup that can 403 on some PATs (noise in SNYK-CLI-0000 summary).
                        def snykOrg = 'vinh-code'
                        def modules = env.CHANGED_MODULES.split(',')
                        def serviceModules = modules.findAll { it != 'common-library' }

                        if (serviceModules.isEmpty()) {
                            echo "Snyk: skip"
                        } else {
                            // Snyk CLI is Node-based; Maven resolver children need RAM — exit -13 is often OOM.
                            // --maven-skip-wrapper: use system `mvn` (tool JDK/Maven in Checkout), not ./mvnw (avoids EACCES on wrapper).
                            // Scan light → full reactor → capped depth (avoid starting with --all-sub-projects only).
                            // Trailing || true: không FAIL stage khi có vulnerability / lỗi Snyk — chỉ ghi log (giống báo cáo nhóm).
                            withEnv([
                                'NODE_OPTIONS=--max-old-space-size=6144',
                                'MAVEN_OPTS=-Xmx1536m -XX:MaxMetaspaceSize=384m'
                            ]) {
                                sh "snyk auth \$SNYK_TOKEN"
                                // Same graph Snyk uses internally — fail fast if Maven cannot resolve deps (before SNYK-CLI-0000 / -13).
                                for (mod in serviceModules) {
                                    sh "mvn -B -ntp dependency:tree -pl ${mod} -am"
                                }
                                for (mod in serviceModules) {
                                    // -d: optional verbose logs; remove after CI stable.
                                    sh "snyk test -d --maven-skip-wrapper --org=${snykOrg} --file=${mod}/pom.xml --severity-threshold=high || snyk test -d --maven-skip-wrapper --org=${snykOrg} --file=${mod}/pom.xml --severity-threshold=high --all-sub-projects || snyk test -d --maven-skip-wrapper --org=${snykOrg} --file=${mod}/pom.xml --severity-threshold=high --all-sub-projects --max-depth=3 || true"
                                }
                                for (mod in serviceModules) {
                                    sh "snyk monitor -d --maven-skip-wrapper --org=${snykOrg} --file=${mod}/pom.xml --project-name=yas-${mod} || snyk monitor -d --maven-skip-wrapper --org=${snykOrg} --file=${mod}/pom.xml --project-name=yas-${mod} --all-sub-projects || snyk monitor -d --maven-skip-wrapper --org=${snykOrg} --file=${mod}/pom.xml --project-name=yas-${mod} --all-sub-projects --max-depth=3 || true"
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Hub – Build and Push Images') {
            when {
                expression { params.CD_ACTION == 'auto' && env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    def services = changedDeployableServices(env.CHANGED_MODULES)
                    def tags = dockerTagsForCurrentRef()
                    echo "Docker Hub services=${services.collect { it.name }.join(',')} tags=${tags.join(',')}"
                    buildAndPushImages(services, tags)
                }
            }
        }

        stage('GitOps – Update Dev') {
            when {
                expression { params.CD_ACTION == 'auto' && env.BRANCH_NAME == 'main' && env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    updateGitOpsOverlay('dev', changedDeployableServices(env.CHANGED_MODULES), 'main', 'deploy_dev')
                }
            }
        }

        stage('GitOps – Update Staging') {
            when {
                expression { params.CD_ACTION == 'auto' && env.TAG_NAME && env.CHANGED_MODULES != '__skip_full_ci__' }
            }
            steps {
                script {
                    updateGitOpsOverlay('staging', changedDeployableServices(env.CHANGED_MODULES), env.TAG_NAME, 'release_staging')
                }
            }
        }

        stage('developer_build Stub') {
            when {
                expression { params.CD_ACTION == 'developer_build_stub' }
            }
            steps {
                script {
                    def services = selectedServicesFromScope(params.SERVICE_SCOPE)
                    echo "developer_build_stub: writing tag ${params.IMAGE_TAG} for ${services.collect { it.name }.join(',')}"
                    updateGitOpsOverlay('developer', services, params.IMAGE_TAG, 'developer_build_stub')
                    echo 'developer_build_stub: no kubectl mutation was performed; ArgoCD owns developer sync.'
                }
            }
        }

        stage('deploy_dev') {
            when {
                expression { params.CD_ACTION == 'deploy_dev' }
            }
            steps {
                script {
                    updateGitOpsOverlay('dev', selectedServicesFromScope(params.SERVICE_SCOPE), 'main', 'deploy_dev')
                }
            }
        }

        stage('release_staging') {
            when {
                expression { params.CD_ACTION == 'release_staging' }
            }
            steps {
                script {
                    if (!(params.RELEASE_TAG ==~ /^v\d+\.\d+\.\d+$/)) {
                        error "RELEASE_TAG must match vX.Y.Z; got ${params.RELEASE_TAG}"
                    }
                    updateGitOpsOverlay('staging', selectedServicesFromScope(params.SERVICE_SCOPE), params.RELEASE_TAG, 'release_staging')
                }
            }
        }

        stage('rollback_environment') {
            when {
                expression { params.CD_ACTION == 'rollback_environment' }
            }
            steps {
                script {
                    if (!(params.TARGET_ENV in ['dev', 'staging'])) {
                        error 'rollback_environment supports TARGET_ENV=dev or staging'
                    }
                    if (!params.ROLLBACK_TAG?.trim()) {
                        error 'ROLLBACK_TAG is required for rollback_environment'
                    }
                    updateGitOpsOverlay(params.TARGET_ENV, selectedServicesFromScope(params.SERVICE_SCOPE), params.ROLLBACK_TAG, 'rollback_environment')
                }
            }
        }

        stage('teardown_developer') {
            when {
                expression { params.CD_ACTION == 'teardown_developer' }
            }
            steps {
                echo 'teardown_developer: update deploy/gitops/overlays/developer in GitOps, then let ArgoCD prune. Direct kubectl deletes are intentionally not used.'
            }
        }

        stage('cluster_smoke_check') {
            when {
                expression { params.CD_ACTION == 'cluster_smoke_check' }
            }
            steps {
                echo 'cluster_smoke_check: run this as a read-only Jenkins job with kubeconfig-readonly/argocd-token credentials configured; no namespace mutation is performed by this Jenkinsfile.'
            }
        }
    }

    // ─── Post-build actions ───────────────────────────────────
    post {
        always {
            cleanWs()
        }
        success {
            echo "PASS ${env.BRANCH_NAME} #${env.BUILD_NUMBER}"
        }
        failure {
            echo "FAIL ${env.BRANCH_NAME} #${env.BUILD_NUMBER}"
        }
    }
}

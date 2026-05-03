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
// ============================================================

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

    environment {
        COVERAGE_THRESHOLD       = '70'
        SONAR_HOST_URL           = 'http://3.27.92.213:9000'
        // Phải trùng tên SonarQube server trong Manage Jenkins → System (SonarQube installations).
        SONARQUBE_INSTALLATION   = 'sonar-server'
        MAVEN_OPTS               = '-Xmx512m -XX:MaxMetaspaceSize=256m'
        GITLEAKS_EXPECTED        = '8.18.4'
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
                sh 'ci/verify-ci-tools.sh'
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
                        sourceDirectories: [
                            [path: 'common-library/src/main/java'],
                            [path: 'backoffice-bff/src/main/java'],
                            [path: 'storefront-bff/src/main/java'],
                            [path: 'payment/src/main/java'],
                            [path: 'payment-paypal/src/main/java'],
                            [path: 'media/src/main/java'],
                            [path: 'cart/src/main/java'],
                            [path: 'customer/src/main/java'],
                            [path: 'inventory/src/main/java'],
                            [path: 'location/src/main/java'],
                            [path: 'order/src/main/java'],
                            [path: 'product/src/main/java'],
                            [path: 'promotion/src/main/java'],
                            [path: 'rating/src/main/java'],
                            [path: 'search/src/main/java'],
                            [path: 'tax/src/main/java'],
                            [path: 'webhook/src/main/java'],
                            [path: 'delivery/src/main/java'],
                            [path: 'recommendation/src/main/java'],
                            [path: 'sampledata/src/main/java']
                        ]
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 5 – Coverage Gate (≥ 70%)
        // Graceful skip nếu module không có executable code
        // ══════════════════════════════════════════════════════
        stage('Coverage Gate') {
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
                                    sh "mvn -B -ntp dependency:tree -f ${mod}/pom.xml"
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

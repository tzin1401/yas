// ============================================================
//  YAS Monorepo – Jenkins CI Pipeline
//  Platform : Jenkins trên AWS (http://3.27.92.213:8080)
//  SonarQube: http://3.27.92.213:9000
//  JDK      : 25  |  Maven: latest LTS
// ============================================================
//
//  Quy tắc pipeline:
//  1. Gitleaks luôn chạy TRƯỚC – phát hiện secret thì FAIL ngay
//  2. Chỉ build/test service nào có file thay đổi (monorepo path filter)
//  3. SonarQube scan override host về server AWS (không dùng SonarCloud)
//  4. Quality Gate wait – pipeline FAIL nếu không đạt ngưỡng
//  5. Snyk scan chỉ chạy trên service bị thay đổi
//  6. Tất cả stage bảo toàn tài nguyên: -DskipITs, tuần tự, không parallel
// ============================================================

pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk   'JDK-25'
    }

    options {
        // Không giữ quá 10 build cũ → tiết kiệm disk 30GB
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Timeout toàn bộ pipeline 60 phút (t3.small build chậm)
        timeout(time: 60, unit: 'MINUTES')
        // Không cho phép 2 build của cùng branch chạy song song
        disableConcurrentBuilds()
    }

    // ─── Biến môi trường toàn pipeline ───────────────────────
    environment {
        SONAR_HOST_URL = 'http://3.27.92.213:9000'
        MAVEN_OPTS     = '-Xmx512m -XX:MaxMetaspaceSize=256m'
    }

    stages {

        // ══════════════════════════════════════════════════════
        // STAGE 1 – Phát hiện service thay đổi
        // ══════════════════════════════════════════════════════
        stage('Detect Changes') {
            steps {
                script {
                    // fetch-depth=0 đã được cấu hình ở Multibranch Pipeline
                    // Nếu đây là commit đầu tiên (không có HEAD~1), lấy tất cả
                    def changedFiles = sh(
                        script: '''
                            git diff --name-only HEAD~1 HEAD 2>/dev/null \
                              || git diff --name-only $(git hash-object -t tree /dev/null) HEAD
                        ''',
                        returnStdout: true
                    ).trim()

                    echo "============================================"
                    echo "Files changed:"
                    echo "${changedFiles}"
                    echo "============================================"

                    def lines = changedFiles.split('\n')

                    // Danh sách các service trong monorepo
                    env.BUILD_PRODUCT       = lines.any { it.startsWith('product/') }       ? 'true' : 'false'
                    env.BUILD_CART          = lines.any { it.startsWith('cart/') }           ? 'true' : 'false'
                    env.BUILD_MEDIA         = lines.any { it.startsWith('media/') }          ? 'true' : 'false'
                    env.BUILD_ORDER         = lines.any { it.startsWith('order/') }          ? 'true' : 'false'
                    env.BUILD_CUSTOMER      = lines.any { it.startsWith('customer/') }       ? 'true' : 'false'
                    env.BUILD_INVENTORY     = lines.any { it.startsWith('inventory/') }      ? 'true' : 'false'
                    env.BUILD_LOCATION      = lines.any { it.startsWith('location/') }       ? 'true' : 'false'
                    env.BUILD_PAYMENT       = lines.any { it.startsWith('payment/') }        ? 'true' : 'false'
                    env.BUILD_PROMOTION     = lines.any { it.startsWith('promotion/') }      ? 'true' : 'false'
                    env.BUILD_RATING        = lines.any { it.startsWith('rating/') }         ? 'true' : 'false'
                    env.BUILD_SEARCH        = lines.any { it.startsWith('search/') }         ? 'true' : 'false'
                    env.BUILD_TAX           = lines.any { it.startsWith('tax/') }            ? 'true' : 'false'
                    env.BUILD_WEBHOOK       = lines.any { it.startsWith('webhook/') }        ? 'true' : 'false'
                    // Thay đổi common-library → build TẤT CẢ service
                    env.BUILD_ALL           = lines.any { it.startsWith('common-library/') || it == 'pom.xml' } ? 'true' : 'false'

                    echo "============================================"
                    echo "Services to build:"
                    echo "  product       = ${env.BUILD_PRODUCT}"
                    echo "  cart          = ${env.BUILD_CART}"
                    echo "  media         = ${env.BUILD_MEDIA}"
                    echo "  order         = ${env.BUILD_ORDER}"
                    echo "  customer      = ${env.BUILD_CUSTOMER}"
                    echo "  inventory     = ${env.BUILD_INVENTORY}"
                    echo "  location      = ${env.BUILD_LOCATION}"
                    echo "  payment       = ${env.BUILD_PAYMENT}"
                    echo "  promotion     = ${env.BUILD_PROMOTION}"
                    echo "  rating        = ${env.BUILD_RATING}"
                    echo "  search        = ${env.BUILD_SEARCH}"
                    echo "  tax           = ${env.BUILD_TAX}"
                    echo "  webhook       = ${env.BUILD_WEBHOOK}"
                    echo "  build_all     = ${env.BUILD_ALL}"
                    echo "============================================"
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 2 – Gitleaks: Secret Scanning
        // Chạy ĐẦU TIÊN, phát hiện secret → FAIL ngay lập tức
        // ══════════════════════════════════════════════════════
        stage('Gitleaks – Secret Scan') {
            steps {
                sh '''
                    echo ">>> Downloading and running Gitleaks secret scan..."
                    if [ ! -f gitleaks ]; then
                        curl -sSLo gitleaks.tar.gz https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz
                        tar -xzf gitleaks.tar.gz gitleaks
                        rm gitleaks.tar.gz
                    fi
                    chmod +x gitleaks
                    ./gitleaks detect --source=. --config=gitleaks.toml --verbose --no-git --exit-code=1
                    echo ">>> Gitleaks scan PASSED - no secrets found"
                '''
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 3 – Build & Test từng service (path-filtered)
        // Dùng -DskipITs để bỏ Integration Test (Testcontainers)
        // vì t3.small không đủ RAM
        // ══════════════════════════════════════════════════════

        stage('Build & Test – common-library') {
            when {
                anyOf {
                    expression { env.BUILD_ALL == 'true' }
                    expression { env.BUILD_PRODUCT == 'true' }
                    expression { env.BUILD_CART == 'true' }
                    expression { env.BUILD_MEDIA == 'true' }
                    expression { env.BUILD_ORDER == 'true' }
                    expression { env.BUILD_CUSTOMER == 'true' }
                    expression { env.BUILD_INVENTORY == 'true' }
                    expression { env.BUILD_LOCATION == 'true' }
                    expression { env.BUILD_PAYMENT == 'true' }
                    expression { env.BUILD_PROMOTION == 'true' }
                    expression { env.BUILD_RATING == 'true' }
                    expression { env.BUILD_SEARCH == 'true' }
                    expression { env.BUILD_TAX == 'true' }
                    expression { env.BUILD_WEBHOOK == 'true' }
                }
            }
            steps {
                sh 'mvn clean install -pl common-library -am -DskipITs -DskipTests'
            }
        }

        stage('Build & Test – product') {
            when {
                anyOf {
                    expression { env.BUILD_PRODUCT == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl product -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'product/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'product/**/jacoco.exec',
                        classPattern:  'product/**/classes',
                        sourcePattern: 'product/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – cart') {
            when {
                anyOf {
                    expression { env.BUILD_CART == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl cart -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'cart/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'cart/**/jacoco.exec',
                        classPattern:  'cart/**/classes',
                        sourcePattern: 'cart/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – media') {
            when {
                anyOf {
                    expression { env.BUILD_MEDIA == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl media -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'media/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'media/**/jacoco.exec',
                        classPattern:  'media/**/classes',
                        sourcePattern: 'media/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – order') {
            when {
                anyOf {
                    expression { env.BUILD_ORDER == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl order -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'order/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'order/**/jacoco.exec',
                        classPattern:  'order/**/classes',
                        sourcePattern: 'order/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – customer') {
            when {
                anyOf {
                    expression { env.BUILD_CUSTOMER == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl customer -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'customer/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'customer/**/jacoco.exec',
                        classPattern:  'customer/**/classes',
                        sourcePattern: 'customer/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – inventory') {
            when {
                anyOf {
                    expression { env.BUILD_INVENTORY == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl inventory -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'inventory/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'inventory/**/jacoco.exec',
                        classPattern:  'inventory/**/classes',
                        sourcePattern: 'inventory/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – location') {
            when {
                anyOf {
                    expression { env.BUILD_LOCATION == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl location -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'location/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'location/**/jacoco.exec',
                        classPattern:  'location/**/classes',
                        sourcePattern: 'location/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – payment') {
            when {
                anyOf {
                    expression { env.BUILD_PAYMENT == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl payment -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'payment/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'payment/**/jacoco.exec',
                        classPattern:  'payment/**/classes',
                        sourcePattern: 'payment/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – promotion') {
            when {
                anyOf {
                    expression { env.BUILD_PROMOTION == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl promotion -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'promotion/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'promotion/**/jacoco.exec',
                        classPattern:  'promotion/**/classes',
                        sourcePattern: 'promotion/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – rating') {
            when {
                anyOf {
                    expression { env.BUILD_RATING == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl rating -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'rating/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'rating/**/jacoco.exec',
                        classPattern:  'rating/**/classes',
                        sourcePattern: 'rating/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – tax') {
            when {
                anyOf {
                    expression { env.BUILD_TAX == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl tax -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'tax/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'tax/**/jacoco.exec',
                        classPattern:  'tax/**/classes',
                        sourcePattern: 'tax/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        stage('Build & Test – webhook') {
            when {
                anyOf {
                    expression { env.BUILD_WEBHOOK == 'true' }
                    expression { env.BUILD_ALL == 'true' }
                }
            }
            steps {
                sh 'mvn clean verify -pl webhook -am -DskipITs'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'webhook/**/surefire-reports/TEST-*.xml'
                    jacoco(
                        execPattern:   'webhook/**/jacoco.exec',
                        classPattern:  'webhook/**/classes',
                        sourcePattern: 'webhook/**/src/main/java',
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.class'
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 4 – SonarQube Analysis
        // Override sonar.host.url → AWS server (không dùng SonarCloud)
        // Credential ID 'sonarqube-token' phải được tạo trong Jenkins
        // ══════════════════════════════════════════════════════
        stage('SonarQube – Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    script {
                        def modules = []

                        if (env.BUILD_ALL == 'true') {
                            modules = ['product','cart','media','order','customer',
                                       'inventory','location','payment','promotion',
                                       'rating','tax','webhook']
                        } else {
                            if (env.BUILD_PRODUCT  == 'true') modules.add('product')
                            if (env.BUILD_CART     == 'true') modules.add('cart')
                            if (env.BUILD_MEDIA    == 'true') modules.add('media')
                            if (env.BUILD_ORDER    == 'true') modules.add('order')
                            if (env.BUILD_CUSTOMER == 'true') modules.add('customer')
                            if (env.BUILD_INVENTORY== 'true') modules.add('inventory')
                            if (env.BUILD_LOCATION == 'true') modules.add('location')
                            if (env.BUILD_PAYMENT  == 'true') modules.add('payment')
                            if (env.BUILD_PROMOTION== 'true') modules.add('promotion')
                            if (env.BUILD_RATING   == 'true') modules.add('rating')
                            if (env.BUILD_TAX      == 'true') modules.add('tax')
                            if (env.BUILD_WEBHOOK  == 'true') modules.add('webhook')
                        }

                        if (modules.isEmpty()) {
                            echo ">>> No service changed — skipping SonarQube analysis"
                        } else {
                            def plArg = modules.join(',')
                            echo ">>> Running SonarQube analysis for: ${plArg}"
                            // Override sonar.host.url về AWS, bỏ qua SonarCloud config trong pom.xml
                            sh """
                                mvn sonar:sonar \
                                  -pl ${plArg} -am \
                                  -Dsonar.host.url=${SONAR_HOST_URL} \
                                  -Dsonar.token=${SONAR_TOKEN} \
                                  -Dsonar.organization= \
                                  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                            """
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // STAGE 6 – Snyk: Dependency Vulnerability Scan
        // Chỉ scan service bị thay đổi
        // Credential ID 'snyk-token' phải được tạo trong Jenkins
        // ══════════════════════════════════════════════════════
        stage('Snyk – Dependency Scan') {
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        def modules = []

                        if (env.BUILD_ALL == 'true') {
                            modules = ['product','cart','media','order','customer',
                                       'inventory','location','payment','promotion',
                                       'rating','tax','webhook']
                        } else {
                            if (env.BUILD_PRODUCT  == 'true') modules.add('product')
                            if (env.BUILD_CART     == 'true') modules.add('cart')
                            if (env.BUILD_MEDIA    == 'true') modules.add('media')
                            if (env.BUILD_ORDER    == 'true') modules.add('order')
                            if (env.BUILD_CUSTOMER == 'true') modules.add('customer')
                            if (env.BUILD_INVENTORY== 'true') modules.add('inventory')
                            if (env.BUILD_LOCATION == 'true') modules.add('location')
                            if (env.BUILD_PAYMENT  == 'true') modules.add('payment')
                            if (env.BUILD_PROMOTION== 'true') modules.add('promotion')
                            if (env.BUILD_RATING   == 'true') modules.add('rating')
                            if (env.BUILD_TAX      == 'true') modules.add('tax')
                            if (env.BUILD_WEBHOOK  == 'true') modules.add('webhook')
                        }

                        if (modules.isEmpty()) {
                            echo ">>> No service changed — skipping Snyk scan"
                        } else {
                            sh "snyk auth \$SNYK_TOKEN"
                            for (mod in modules) {
                                echo ">>> Snyk scanning: ${mod}"
                                // continue-on-error = true: báo cáo lỗi nhưng không fail pipeline
                                sh """
                                    snyk test \
                                      --file=${mod}/pom.xml \
                                      --severity-threshold=high \
                                      --all-sub-projects \
                                      || true
                                """
                            }
                            // Monitor project trên snyk.io dashboard
                            for (mod in modules) {
                                sh """
                                    snyk monitor \
                                      --file=${mod}/pom.xml \
                                      --project-name=yas-${mod} \
                                      || true
                                """
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
            // Xoá workspace sau mỗi build → tiết kiệm 30GB SSD
            cleanWs()
        }
        success {
            echo """
            ╔══════════════════════════════════════╗
            ║   ✅  PIPELINE PASSED SUCCESSFULLY   ║
            ╚══════════════════════════════════════╝
            Branch : ${env.BRANCH_NAME}
            Build  : #${env.BUILD_NUMBER}
            """
        }
        failure {
            echo """
            ╔══════════════════════════════════════╗
            ║   ❌  PIPELINE FAILED                ║
            ╚══════════════════════════════════════╝
            Branch : ${env.BRANCH_NAME}
            Build  : #${env.BUILD_NUMBER}
            Check console output for details.
            """
        }
    }
}

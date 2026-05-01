// ============================================================
//  YAS Monorepo – Jenkins CI Pipeline (Merged)
//  Platform : Jenkins trên AWS (http://3.27.92.213:8080)
//  SonarQube: http://3.27.92.213:9000
//  JDK      : 25  |  Maven: latest LTS
// ============================================================
//
//  Pipeline flow:
//  1. Checkout + fetch origin/main (fix shallow clone)
//  2. Detect Changed Modules (shell script, so sánh với origin/main)
//  3. Gitleaks – Secret Scan (luôn chạy, fail nếu phát hiện secret)
//  4. Test + JaCoCo report (chỉ modules thay đổi, skip Integration Tests)
//  5. Coverage Gate (≥ 70%, graceful skip nếu không có code)
//  6. Build (chỉ modules thay đổi)
//  7. SonarQube Analysis (chỉ modules thay đổi)
//  8. Snyk Dependency Scan (chỉ modules thay đổi)
// ============================================================

pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk   'JDK-25'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        COVERAGE_THRESHOLD = '70'
        SONAR_HOST_URL     = 'http://3.27.92.213:9000'
        MAVEN_OPTS         = '-Xmx512m -XX:MaxMetaspaceSize=256m'
    }

    stages {

        // ══════════════════════════════════════════════════════
        // STAGE 1 – Checkout & Prepare
        // ══════════════════════════════════════════════════════
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git fetch origin main:refs/remotes/origin/main || true'
                sh 'chmod +x ci/detect-changed-modules.sh ci/check-coverage.sh'
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
                    echo "============================================"
                    echo "Modules selected for CI: ${env.CHANGED_MODULES}"
                    echo "============================================"
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
                    echo ">>> Downloading and running Gitleaks secret scan..."
                    if [ ! -f gitleaks ]; then
                        curl -sSLo gitleaks.tar.gz https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz
                        tar -xzf gitleaks.tar.gz gitleaks
                        rm gitleaks.tar.gz
                    fi
                    chmod +x gitleaks

                    # Tạo baseline từ upstream (nếu chưa có)
                    if [ -f gitleaks-baseline.json ]; then
                        echo ">>> Using existing baseline to detect only NEW secrets"
                        ./gitleaks detect --source=. --no-git --config=gitleaks.toml --baseline-path=gitleaks-baseline.json --report-path=gitleaks-report.json --verbose --exit-code=1
                    else
                        echo ">>> No baseline found — running full scan with .gitleaksignore"
                        ./gitleaks detect --source=. --no-git --config=gitleaks.toml --report-path=gitleaks-report.json --verbose --exit-code=1
                    fi
                    echo ">>> Gitleaks scan PASSED - no new secrets found"
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
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true,
                                     artifacts: '**/target/site/jacoco/jacoco.xml'
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
                    for (module in modules) {
                        sh "ci/check-coverage.sh ${module} ${env.COVERAGE_THRESHOLD}"
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
        // Override sonar.host.url → AWS server (không dùng SonarCloud)
        // ══════════════════════════════════════════════════════
        stage('SonarQube – Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    script {
                        def modules = env.CHANGED_MODULES.split(',')
                        def serviceModules = modules.findAll { it != 'common-library' }

                        if (serviceModules.isEmpty()) {
                            echo ">>> No service modules changed — skipping SonarQube analysis"
                        } else {
                            def plArg = serviceModules.join(',')
                            echo ">>> Running SonarQube analysis for: ${plArg}"
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
        // STAGE 8 – Snyk: Dependency Vulnerability Scan
        // Chỉ scan modules thay đổi
        // ══════════════════════════════════════════════════════
        stage('Snyk – Dependency Scan') {
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        def modules = env.CHANGED_MODULES.split(',')
                        def serviceModules = modules.findAll { it != 'common-library' }

                        if (serviceModules.isEmpty()) {
                            echo ">>> No service modules changed — skipping Snyk scan"
                        } else {
                            sh "snyk auth \$SNYK_TOKEN"
                            for (mod in serviceModules) {
                                echo ">>> Snyk scanning: ${mod}"
                                sh """
                                    snyk test \
                                      --file=${mod}/pom.xml \
                                      --severity-threshold=high \
                                      --all-sub-projects \
                                      || true
                                """
                            }
                            for (mod in serviceModules) {
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

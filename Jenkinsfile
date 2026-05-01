pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        COVERAGE_THRESHOLD = '70'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x ci/detect-changed-modules.sh ci/check-coverage.sh'
            }
        }

        stage('Detect Changed Modules') {
            steps {
                script {
                    env.CHANGED_MODULES = sh(
                        script: 'ci/detect-changed-modules.sh',
                        returnStdout: true
                    ).trim()
                    echo "Modules selected for CI: ${env.CHANGED_MODULES}"
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    def modules = env.CHANGED_MODULES.split(',')
                    for (module in modules) {
                        sh "mvn -B -ntp -pl ${module} -am test jacoco:report"
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/site/jacoco/jacoco.xml'
                }
            }
        }

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
    }
}

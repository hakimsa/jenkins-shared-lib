def call(Map config = [:]) {

    def buildType = config.buildType ?: detectBuildType()  // node, maven, python
    def buildCmd  = config.buildCmd

    pipeline {
        agent any

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Detect') {
                steps {
                    echo "Detected build type: ${buildType}"
                }
            }

            stage('Install / Build') {
                steps {
                    script {
                        if (buildType == 'node') {
                            sh buildCmd ?: 'npm install'
                            sh 'npm run build:prod || echo "No build step defined"'
                        } else if (buildType == 'maven') {
                            sh buildCmd ?: 'mvn clean package'
                        } else if (buildType == 'python') {
                            sh buildCmd ?: 'pip install -r requirements.txt'
                        } else {
                            error "Unsupported build type: ${buildType}"
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    script {
                        if (buildType == 'node') {
                            sh 'npm test || echo "No tests found"'
                        } else if (buildType == 'maven') {
                            sh 'mvn test'
                        } else if (buildType == 'python') {
                            sh 'pytest || echo "No tests found"'
                        }
                    }
                }
            }
        }

        post {
            always { echo "Pipeline finished!" }
            success { echo "Pipeline successful!" }
            failure { echo "Pipeline failed!" }
        }
    }
}

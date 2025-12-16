def call(Map config = [:]) {

    def buildType = config.buildType ?: 'maven'
    def buildCmd  = config.buildCmd  ?: ''

    pipeline {
        agent any
        stages {
            stage('Build') {
                steps {
                    script {
                        if (buildType == 'maven') {
                            sh buildCmd ?: 'mvn clean package'
                        }
                        else if (buildType == 'node') {
                            sh buildCmd ?: 'npm install && npm run build'
                        }
                        else if (buildType == 'python') {
                            sh buildCmd ?: 'pip install -r requirements.txt'
                        }
                        else {
                            error "Build type not supported: ${buildType}"
                        }
                    }
                }
            }

            stage('Test') {
                when {
                    expression { config.runTests != false }
                }
                steps {
                    echo "Running tests"
                }
            }

            stage('Deploy') {
                when {
                    expression { config.env == 'prod' }
                }
                steps {
                    echo "Deploying to ${config.env}"
                }
            }
        }
    }
}

def call(Map config = [:]) {

    def buildType = config.buildType ?: 'maven'
    def buildCmd  = config.buildCmd  ?: ''
    def envName   = config.env ?: 'dev'

    pipeline {
        agent {
            docker {
                image buildType == 'maven'  ? 'maven:3.9.6-eclipse-temurin-17' :
                      buildType == 'node'   ? 'node:18' :
                      buildType == 'python' ? 'python:3.11' :
                      'alpine:latest'
            }
        }

        stages {
            stage('Build') {
                steps {
                    script {
                        if (buildCmd) {
                            sh buildCmd
                        }
                        else if (buildType == 'maven') {
                            sh 'mvn clean package'
                        }
                        else if (buildType == 'node') {
                            sh 'npm install && npm run build'
                        }
                        else if (buildType == 'python') {
                            sh 'pip install -r requirements.txt'
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
                    expression { envName == 'prod' }
                }
                steps {
                    echo "Deploying to ${envName}"
                }
            }
        }
    }
}

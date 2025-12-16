def call(Map config = [:]) {

    // Detectar tipo de build o usar el que se pase como parámetro
    def buildType = config.buildType ?: detectBuildType()
    def buildCmd  = config.buildCmd

    pipeline {
        agent any

        stages {
            stage('Detect') {
                steps {
                    echo "Detected build type: ${buildType}"
                }
            }

            stage('Build') {
                steps {
                    script {
                        if (buildType == 'node') {
                            // Usar contenedor Node.js
                            docker.image('node:20').inside {
                                sh buildCmd ?: 'npm install && npm start'
                            }
                        } else if (buildType == 'maven') {
                            docker.image('maven:3.9.6-eclipse-temurin-17').inside {
                                sh buildCmd ?: 'mvn clean package'
                            }
                        } else if (buildType == 'python') {
                            docker.image('python:3.12').inside {
                                sh buildCmd ?: 'pip install -r requirements.txt'
                            }
                        } else {
                            error "Unsupported build type: ${buildType}"
                        }
                    }
                }
            }
        }
    }
}

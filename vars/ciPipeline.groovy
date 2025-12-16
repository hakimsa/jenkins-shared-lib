def call(Map config = [:]) {

    def buildType = config.buildType ?: 'node'  // Node por defecto
    def buildCmd  = config.buildCmd ?: ''

    pipeline {
        agent {
            docker { 
                image 'node:20-alpine' // Imagen Node.js + npm lista
                args '-u 1000:1000'    // Evita problemas de permisos
            }
        }

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
                            sh buildCmd ?: 'npm ci && npm run build'
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
                when {
                    expression { config.runTests != false }
                }
                steps {
                    echo "Running tests..."
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
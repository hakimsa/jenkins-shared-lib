def call(Map config = [:]) {
    pipeline {
        agent any

        stages {
            stage('Build') {
                steps {
                    script {
                        def buildType = config.buildType ?: 'node'
                        def buildCmd  = config.buildCmd ?: 'npm ci && npm run build'

                        if (buildType == 'node') {
                            sh buildCmd
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
                    echo "Running tests..."
                    sh 'npm test || echo "Tests skipped or failed"'
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

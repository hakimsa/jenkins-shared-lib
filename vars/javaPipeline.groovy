def call(Map config = [:]) {
    pipeline {
        agent any
        stages {
            stage('Build') {
                steps {
                    echo "Building project..."
                    sh 'echo mvn clean package'
                }
            }
            stage('Test') {
                steps {
                    echo "Running tests..."
                    sh 'echo mvn test'
                }
            }
            stage('Deploy') {
                when {
                    expression { config.env == 'prod' }
                }
                steps {
                    echo "Deploying to ${config.env}"
                    sh "echo ./deploy.sh ${config.env}"
                }
            }
        }
        post {
            always {
                echo "Cleaning workspace"
                cleanWs()
            }
        }
    }
}


def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            NODE_ENV = config.nodeEnv ?: 'production'
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Install dependencies') {
                steps {
                    sh 'npm install'
                }
            }

            stage('Test') {
                steps {
                    sh 'npm test'
                }
            }

            stage('Build') {
                when {
                    expression { fileExists('package.json') }
                }
                steps {
                    sh 'npm run build'
                }
            }
        }
    }
}

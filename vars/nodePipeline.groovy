def call(Map config = [:]) {

    pipeline {
        agent any

        tools {
            nodejs 'node-18'
        }

        environment {
            APP_NAME = config.appName ?: 'node-app'
            NODE_ENV = config.nodeEnv ?: 'production'
        }

        stages {

            stage('Install') {
                steps {
                    sh '''
                        node -v
                        npm -v
                        npm ci
                    '''
                }
            }

            stage('Test') {
                when {
                    expression { fileExists('package.json') }
                }
                steps {
                    sh 'npm test'
                }
            }

            stage('Build') {
                when {
                    expression {
                        def pkg = readJSON file: 'package.json'
                        return pkg?.scripts?.build != null
                    }
                }
                steps {
                    sh 'npm run build'
                }
            }

            stage('Package') {
                steps {
                    sh '''
                        mkdir -p package

                        cp package.json package/
                        [ -f package-lock.json ] && cp package-lock.json package-lock.json

                        [ -d dist ] && cp -r dist package/
                        [ -d build ] && cp -r build package/
                        [ -f ecosystem.config.js ] && cp ecosystem.config.js package/
                        [ -f .env.example ] && cp .env.example package/

                        cp -r node_modules package/

                        tar -czf ${APP_NAME}.tar.gz package
                    '''
                }
            }
        }

        post {
            success {
                archiveArtifacts artifacts: '*.tar.gz', fingerprint: true
            }
        }
    }
}

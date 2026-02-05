def call(Map config = [:]) {

    // Valores con fallback en Groovy
    def appName = config.appName ?: 'node-app'
    def nodeEnv = config.nodeEnv ?: 'production'

    pipeline {
        agent any

        tools {
            nodejs 'node-18'
        }

        environment {
            APP_NAME = "${appName}"
            NODE_ENV = "${nodeEnv}"
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
                    sh 'echo lanzar test npm test'
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
                    sh """
                        mkdir -p package

                        cp package.json package/
                        [ -f package-lock.json ] && cp package-lock.json package/

                        [ -d dist ] && cp -r dist package/
                        [ -d build ] && cp -r build package/
                        [ -f ecosystem.config.js ] && cp ecosystem.config.js package/
                        [ -f .env.example ] && cp .env.example package/

                        cp -r node_modules package/

                        tar -czf ${appName}.tar.gz package
                    """
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

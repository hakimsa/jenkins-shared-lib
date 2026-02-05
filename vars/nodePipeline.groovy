def call(Map config = [:]) {
    pipeline {
        agent any

        tools {
            nodejs 'node-18'
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
                    anyOf {
                        fileExists('dist')
                        fileExists('build')
                        expression { sh(script: "jq -e '.scripts.build' package.json > /dev/null", returnStatus: true) == 0 }
                    }
                }
                steps {
                    sh 'npm run build || echo "No build step"'
                }
            }

            stage('Package') {
                steps {
                    sh '''
                        mkdir -p package

                        cp package.json package/
                        [ -f package-lock.json ] && cp package-lock.json package/

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
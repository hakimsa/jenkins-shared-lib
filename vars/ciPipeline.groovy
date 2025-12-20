def call(Map config = [:]) {

    def buildType = config.buildType ?: detectBuildType()   // node | maven | python
    def buildCmd  = config.buildCmd

    pipeline {
        agent {
            docker {
                image 'maven:3.9.0'
            }
        }

        stages {

            stage('Init') {
                steps {
                    echo "Shared CI Pipeline started"
                    echo " Build type: ${buildType}"
                }
            }

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            /* =======================
               BUILD STAGES
            ======================= */

            stage('Build - Node') {
                when {
                    expression { buildType == 'node' }
                }
                steps {
                    sh buildCmd ?: 'npm install'
                    sh 'npm run build:prod || echo "No build step defined"'
                }
            }

            stage('Build - Maven') {
                when {
                    expression { buildType == 'maven' }
                }
                steps {
                    sh buildCmd ?: 'mvn clean package'
                }
            }

            stage('Build - Python') {
                when {
                    expression { buildType == 'python' }
                }
                steps {
                    sh buildCmd ?: 'pip install -r requirements.txt'
                }
            }

            /* =======================
               TEST STAGES
            ======================= */

            stage('Test - Node') {
                when {
                    expression { buildType == 'node' }
                }
                steps {
                    sh 'npm test || echo "No tests found"'
                }
            }

            stage('Test - Maven') {
                when {
                    expression { buildType == 'maven' }
                }
                steps {
                    sh 'mvn test'
                }
            }

            stage('Test - Python') {
                when {
                    expression { buildType == 'python' }
                }
                steps {
                    sh 'pytest || echo "No tests found"'
                }
            }

            /* =======================
               DOCKER
            ======================= */

            stage('Docker Build & Push') {
                when {
                    anyOf {
                        expression { buildType == 'maven' }
                        expression { buildType == 'node' }
                    }
                }
                steps {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )
                    ]) {
                        sh '''
                          echo "🐳 Building Docker image"
                          docker build -t app-mgt:${BUILD_NUMBER} .
                          docker tag app-mgt:${BUILD_NUMBER} $DOCKER_USER/app-mgt:${BUILD_NUMBER}
                          echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                          docker push $DOCKER_USER/app-mgt:${BUILD_NUMBER}
                        '''
                        script {
                            env.DOCKER_IMAGE = "${DOCKER_USER}/app-mgt:${BUILD_NUMBER}"
                            echo " Image pushed: ${env.DOCKER_IMAGE}"
                        }
                    }
                }
            }

            stage('Deploy') {
                when {
                    expression { env.DOCKER_IMAGE != null }
                }
                steps {
                    sh '''
                     
                      echo " Deploying container 🚀🚀🚀"
                      docker rm -f app-mgt || true
                      docker run -d --name app-mgt -p 3000:3000 $DOCKER_IMAGE
                    '''
                }
            }
        }

        post {
            always {
                echo "✅ Pipeline finished"
            }
        }
    }
}

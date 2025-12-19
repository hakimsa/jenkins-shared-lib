def call(Map config = [:]) {

    def buildType = config.buildType ?: detectBuildType()
    def buildCmd  = config.buildCmd

    pipeline {
        agent any

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Detect') {
                steps {
                    echo "Detected build type: ${buildType}"
                }
            }

            stage('Build') {
                steps {
                    script {
                        if (buildType == 'maven') {
                            sh buildCmd ?: 'mvn clean package'
                        } else if (buildType == 'node') {
                            sh 'npm install'
                            sh 'npm run build:prod || echo "No build step"'
                        } else {
                            error "Unsupported build type"
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    script {
                        if (buildType == 'maven') {
                            sh 'mvn test'
                        } else if (buildType == 'node') {
                            sh 'npm test || echo "No tests"'
                        }
                    }
                }
            }

            stage('Docker Build & Push') {
                steps {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )
                    ]) {
                        script {
                            sh '''
                              docker build -t app-mgt:${BUILD_NUMBER} .
                              docker tag app-mgt:${BUILD_NUMBER} $DOCKER_USER/app-mgt:${BUILD_NUMBER}
                              echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                              docker push $DOCKER_USER/app-mgt:${BUILD_NUMBER}
                            '''
                            env.DOCKER_IMAGE = "$DOCKER_USER/app-mgt:${BUILD_NUMBER}"
                        }
                    }
                }
            }

            stage('Deploy') {
                steps {
                    script {
                        sh '''
                          docker rm -f app-mgt || true
                          docker run -d --name app-mgt -p 3000:3000 $DOCKER_IMAGE
                        '''
                    }
                }
            }
        }

        post {
            always {
                echo "Pipeline finished"
            }
        }
    }
}

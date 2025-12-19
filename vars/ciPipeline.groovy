def call(Map config = [:]) {

    def buildType = config.buildType ?: detectBuildType()  // node, maven, python
    def buildCmd  = config.buildCmd

    pipeline {
        agent {
        docker { image 'maven:3.9.0' }}
      
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

            stage('Install / Build') {
                steps {
                    script {
                        if (buildType == 'node') {
                            sh buildCmd ?: 'npm install'
                            sh 'npm run build:prod || echo "No build step defined"'
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
                    script {
                        if (buildType == 'node') {
                            sh 'npm test || echo "No tests found"'
                        } else if (buildType == 'maven') {
                            sh 'mvn test'
                        } else if (buildType == 'python') {
                            sh 'pytest || echo "No tests found"'
                        }
                    }
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
            script {
                def imageName = "hakimsamouh/my-app:${env.BUILD_NUMBER}"

                sh '''
                  docker build -t app-mgt:${BUILD_NUMBER} .
                  docker tag app-mgt:${BUILD_NUMBER} ${DOCKER_USER}/app-mgt:${BUILD_NUMBER}
                  echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                  docker push ${DOCKER_USER}/app-mgt:${BUILD_NUMBER}
                '''

                env.DOCKER_IMAGE = "${DOCKER_USER}/app-mgt:${env.BUILD_NUMBER}"
            }
        }
    }


            stage('Deploy') {
                steps {
                    script {
                        def containerName = "app-mgt-${env.BUILD_NUMBER}"
                        sh "docker rm -f ${containerName} || true"
                        sh "docker run -d --name ${containerName} -p 3000:3000 ${env.DOCKER_IMAGE}"
                    }
                }
            }
        }

        post {
            always {
                echo "Pipeline finished!"
            }
        }
    }

}
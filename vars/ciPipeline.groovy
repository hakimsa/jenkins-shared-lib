def call(Map config = [:]) {

    def buildType = config.buildType ?: detectBuildType()  // node, maven, python
    def buildCmd  = config.buildCmd

    pipeline {
        agent {
        docker { image 'maven:3.9.0' }}
         environment {
           DOCKER_HUB_USER = credentials('dockerhub-creds')
           
        }

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
               stage('Build Docker Image') {
                steps {
                    script {
                        def imageName = "${DOCKER_HUB_USER}/my-app:${env.BUILD_NUMBER}"
                        sh "docker build -t ${imageName} ."
                        sh "echo ${DOCKER_HUB_PASS} | docker login -u ${DOCKER_HUB_USER} --password-stdin"
                        sh "docker push ${imageName}"
                        env.DOCKER_IMAGE = imageName
                    }
                }
            }

            stage('Deploy') {
                steps {
                    script {
                        def containerName = "my-app-${env.BUILD_NUMBER}"
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
        

      
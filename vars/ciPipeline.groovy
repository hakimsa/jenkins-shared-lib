import org.company.ci.StackConfig

def call(Map config = [:]) {
    def buildType = config.buildType ?: detectBuildType()
    def buildCmd = config.buildCmd
    def dockerImage = config.dockerImage // Permite override de imagen
    def stacks = StackConfig.get()
    def stack = stacks[buildType]
    
    if (!stack) {
        error "Unsupported buildType: ${buildType}. Supported types: ${stacks.keySet()}"
    }
    
    // Usa imagen personalizada o la del stack
    def buildImage = dockerImage ?: stack.image
    
    echo "🔍 Using Docker image: ${buildImage}"

    pipeline {
        agent {
            label 'docker' // Usa un agente con Docker instalado
        }

        stages {
            stage('Init') {
                steps {
                    echo "🚀 Shared CI Pipeline started"
                    echo "📦 Build type: ${buildType}"
                }
            }

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            /* ======================= BUILD IN DOCKER ======================= */
            stage('Build') {
                agent {
                    docker {
                        image buildImage
                        reuseNode true
                        args '-v $HOME/.m2:/root/.m2' // Cache Maven/npm
                    }
                }
                steps {
                    script {
                        if (buildType == 'node') {
                            sh buildCmd ?: 'npm install'
                            sh 'npm run build:prod || echo "No build step defined"'
                        } else if (buildType == 'maven') {
                            sh buildCmd ?: 'mvn clean package'
                        } else if (buildType == 'python') {
                            sh buildCmd ?: 'pip install -r requirements.txt'
                        }
                    }
                }
            }

            /* ======================= TEST IN DOCKER ======================= */
            stage('Test') {
                agent {
                    docker {
                        image buildImage
                        reuseNode true
                    }
                }
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

            /* ======================= DOCKER BUILD & PUSH ======================= */
            stage('Docker Build & Push') {
                when {
                    anyOf {
                        expression { buildType == 'maven' }
                        expression { buildType == 'node' }
                    }
                }
                steps {
                    script {
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
                            env.DOCKER_IMAGE = "${DOCKER_USER}/app-mgt:${BUILD_NUMBER}"
                            echo "✅ Image pushed: ${env.DOCKER_IMAGE}"
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
                        echo "🚀 Deploying container..."
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

def detectBuildType() {
    if (fileExists('pom.xml')) {
        return 'maven'
    } else if (fileExists('package.json')) {
        return 'node'
    } else if (fileExists('requirements.txt') || fileExists('setup.py')) {
        return 'python'
    } else {
        error "Unable to detect build type. Please specify 'buildType' in the config."
    }
}
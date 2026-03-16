
def call(Map config = [:]) {

    // Valores por defecto con fallback
    def appName = config.appName ?: 'java-app'
    def javaVersion = config.javaVersion ?: 'jdk-17'
    //def buildTool = config.buildTool ?: 'maven'

    pipeline {
        agent { label 'jenkins_sandbox_agent' }

        tools {
            jdk "${javaVersion}"
            maven 'maven-3.9'
        }

        environment {
            APP_NAME = "${appName}"
            JAVA_HOME = tool "${javaVersion}"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    sh '''
                        java -version
                        mvn -version
                        mvn -B clean compile
                    '''
                }
            }

            stage('Test') {
                steps {
                    sh '''
                        echo "Running tests..."
                        
                        echo $APP_NAME
                    '''
                }
            }

            stage('Package') {
                steps {
                    sh '''
                        mvn -B package -DskipTests
                    '''
                }
            }

            stage('Prepare Artifact') {
                steps {
                    sh """
                        mkdir -p package

                        cp target/*.jar package/

                        [ -f Dockerfile ] && cp Dockerfile package/
                        [ -f application.yml ] && cp application.yml package/
                        [ -f application.properties ] && cp application.properties package/

                        tar -czf ${appName}.tar.gz package
                    """
                }
            }

        }

        post {
            success {
                archiveArtifacts artifacts: '*.tar.gz', fingerprint: true
            }
            failure {
                echo "Build failed"
            }
        }
    }
}

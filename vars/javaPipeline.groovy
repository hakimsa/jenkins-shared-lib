def call(Map config = [:]) {

    // Valores con fallback
    def appName = config.appName ?: 'springboot-app'
    def javaEnv = config.javaEnv ?: 'development'
    def jdkVersion = config.javaVersion ?: 'jdk-11'
    def mavenVersion = config.mavenVersion ?: 'maven-3.9'

    pipeline {
        agent { label 'jenkins_sandbox_agent' }

        tools {
            jdk jdkVersion
            maven mavenVersion
        }

        environment {
            APP_NAME = "${appName}"
            JAVA_ENV = "${javaEnv}"
            SPRING_DATASOURCE_URL = "jdbc:postgresql://172.20.0.2:5432/db_hakim"
        }

        stages {

            stage('Checkout') {
                steps {
                    echo "Checkout code for ${APP_NAME}"
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    echo "Building ${APP_NAME} with Maven"

                    withCredentials([usernamePassword(
                        credentialsId: 'postgres-db',
                        usernameVariable: 'DB_USER',
                        passwordVariable: 'DB_PASS'
                    )]) {

                      env.SPRING_DATASOURCE_USERNAME = env.DB_USER
                      env.SPRING_DATASOURCE_PASSWORD = env.DB_PASS

            sh """
            mvn clean package -DskipTests=false \
        
            """
                    }
                }
            }

            stage('Test') {
                steps {
                    echo "Running tests in ${JAVA_ENV}"
                    sh "mvn test"
                }
            }

            stage('Package') {
                steps {
                    echo "Packaging ${APP_NAME}"
                    sh """
                        mkdir -p package
                        cp target/*.jar package/
                        tar -czf ${APP_NAME}.tar.gz package
                    """
                }
            }
        }

        post {
            success {
                echo "Pipeline finished successfully!"
                archiveArtifacts artifacts: '*.tar.gz', fingerprint: true
            }
            failure {
                echo "Pipeline failed!"
            }
        }
    }
}
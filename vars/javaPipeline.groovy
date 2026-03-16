def call(Map config = [:]) {

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
            SPRING_DATASOURCE_URL = "jdbc:postgresql://172.22.0.2:5432/db_hakim"
        }

        stages {

            stage('Checkout') {
                steps {
                    echo "Checkout code for ${APP_NAME}"
                    checkout scm
                }
            }

            stage('Compile') {
                steps {
                    sh "mvn clean compile"
                }
            }

            stage('Test') {
                steps {
                    withCredentials([usernamePassword(
                        credentialsId: 'postgres-db',
                        usernameVariable: 'DB_USER',
                        passwordVariable: 'DB_PASS'
                    )]) {

                        withEnv([
                            "SPRING_DATASOURCE_USERNAME=${DB_USER}",
                            "SPRING_DATASOURCE_PASSWORD=${DB_PASS}"
                        ]) {

                            sh "mvn test"
                        }
                    }
                }
            }

            stage('Package') {
                steps {
                    sh "mvn package -DskipTests"
                }
            }

            stage('Archive') {
                steps {
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
                archiveArtifacts artifacts: '*.tar.gz', fingerprint: true
                echo "Pipeline finished successfully!"
            }
            failure {
                echo "Pipeline failed!"
            }
        }
    }
}
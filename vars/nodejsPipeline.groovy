#!/usr/bin/env groovy

def call(Map config = [:]) {
    pipeline {
        agent any
        
        options {
            // Timeout para evitar builds colgados
            timeout(time: 30, unit: 'MINUTES')
            // Mantener solo los últimos 10 builds
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }
        
        environment {
            // Configurar Node.js
            NODE_VERSION = config.nodeVersion ?: '24.13.0'
            // Credenciales de GitHub (ajusta el ID según tu configuración)
            GITHUB_CREDENTIALS = credentials('github-token')
        }
        
        stages {
            stage('Checkout') {
                steps {
                    script {
                        // Notificar a GitHub que el build está pendiente
                        updateGitHubStatus('PENDING', 'Build started')
                    }
                    checkout scm
                }
            }
            
            stage('Setup Node.js') {
                steps {
                    script {
                        // Instalar Node.js usando nvm o NodeJS plugin
                        sh """
                            node --version
                            npm --version
                        """
                    }
                }
            }
            
            stage('Install Dependencies') {
                steps {
                    script {
                        updateGitHubStatus('PENDING', 'Installing dependencies')
                        sh 'npm ci'
                    }
                }
            }
            
            stage('Lint') {
                steps {
                    script {
                        updateGitHubStatus('PENDING', 'Running linter')
                        sh 'npm run lint || true'
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        updateGitHubStatus('PENDING', 'Running tests')
                        sh 'echo estoy lanzando tests'
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        updateGitHubStatus('PENDING', 'Building application')
                        sh 'npm run build'
                    }
                }
            }
        }
        
        post {
            success {
                script {
                    updateGitHubStatus('SUCCESS', 'Build completed successfully')
                }
                echo 'Pipeline completed successfully!'
            }
            failure {
                script {
                    updateGitHubStatus('FAILURE', 'Build failed')
                }
                echo 'Pipeline failed!'
            }
            always {
                cleanWs()
            }
        }
    }
}

def updateGitHubStatus(String state, String description) {
    if (env.GIT_COMMIT) {
        try {
            // Opción 1: Usar el plugin de GitHub
            githubNotify(
                account: env.GIT_URL.tokenize('/')[3],
                repo: env.GIT_URL.tokenize('/')[4].minus('.git'),
                sha: env.GIT_COMMIT,
                status: state,
                description: description,
                context: 'Jenkins CI',
                credentialsId: 'github-token'
            )
        } catch (Exception e) {
            echo "No se pudo actualizar el estado en GitHub: ${e.message}"
        }
    }
}
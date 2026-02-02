#!/usr/bin/env groovy

def call(Map config = [:]) {
    // Definir variables fuera del pipeline
    def nodeVersion = config.nodeVersion ?: '18'
    def githubCredentialsId = config.githubCredentialsId ?: 'github-token'
    
    pipeline {
        agent any
        
        options {
            timeout(time: 30, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }
        
        environment {
            NODE_VERSION = "${nodeVersion}"
            GITHUB_TOKEN = credentials("${githubCredentialsId}")
        }
        
        stages {
            stage('Checkout') {
                steps {
                    script {
                        updateGitHubStatus('pending', 'Build started')
                    }
                    checkout scm
                }
            }
            
            stage('Setup Node.js') {
                steps {
                    sh """
                        node --version
                        npm --version
                    """
                }
            }
            
            stage('Install Dependencies') {
                steps {
                    script {
                        updateGitHubStatus('pending', 'Installing dependencies')
                    }
                    sh 'npm ci'
                }
            }
            
            stage('Lint') {
                when {
                    expression { fileExists('package.json') }
                }
                steps {
                    script {
                        updateGitHubStatus('pending', 'Running linter')
                        sh 'npm run lint || true'
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        updateGitHubStatus('pending', 'Running tests')
                    }
                    sh 'npm test'
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        updateGitHubStatus('pending', 'Building application')
                    }
                    sh 'npm run build'
                }
            }
        }
        
        post {
            success {
                script {
                    updateGitHubStatus('success', 'Build completed successfully')
                }
                echo 'Pipeline completed successfully!'
            }
            failure {
                script {
                    updateGitHubStatus('error', 'Build failed')
                }
                echo 'Pipeline failed!'
            }
            always {
                cleanWs()
            }
        }
    }
}

// Función para actualizar el estado en GitHub
def updateGitHubStatus(String state, String description) {
    if (!env.GIT_COMMIT) {
        echo 'No GIT_COMMIT found, skipping GitHub status update'
        return
    }
    
    try {
        def repoUrl = env.GIT_URL
        def commit = env.GIT_COMMIT
        
        // Extraer owner y repo
        def urlParts = repoUrl.replaceAll('.git$', '').replaceAll('.*github.com[:/]', '').split('/')
        def owner = urlParts[0]
        def repo = urlParts[1]
        
        def apiUrl = "https://api.github.com/repos/${owner}/${repo}/statuses/${commit}"
        
        def payload = JsonOutput.toJson([
            state: state,
            description: description,
            context: 'Jenkins CI',
            target_url: env.BUILD_URL
        ])
        
        sh """
            curl -s -X POST \
            -H "Authorization: token \${GITHUB_TOKEN}" \
            -H "Content-Type: application/json" \
            -H "Accept: application/vnd.github.v3+json" \
            -d '${payload}' \
            ${apiUrl}
        """
        
        echo "GitHub status updated: ${state} - ${description}"
    } catch (Exception e) {
        echo "Failed to update GitHub status: ${e.message}"
    }
}
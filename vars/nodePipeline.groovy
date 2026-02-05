def call(Map config = [:]) {
    pipeline {
        agent any

        tools {
            nodejs 'node-18'
        }

        stages {
            stage('Install') {
                steps {
                    sh 'node -v'
                    sh 'npm -v'
                    sh 'npm install'
                }
            }
        }
    }
}

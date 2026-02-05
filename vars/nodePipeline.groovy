def call(Map config = [:]) {
    pipeline {
        agent any
        stages {
            stage('Install') {
                steps {
                    sh 'npm install'
                }
            }
        }
    }
}
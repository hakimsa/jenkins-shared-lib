def call(Map config = [:]) {
    pipeline {
        agent any
        stages {
            stage('Build') {
                steps {
                    echo "Hello Shared Library"
                }
            }
        }
    }
}
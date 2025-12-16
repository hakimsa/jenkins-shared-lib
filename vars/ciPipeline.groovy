def call(Map config = [:]) {
    node {
        stage('Build') {
            sh 'npm install'
        }
        stage('Test') {
            sh 'npm test'
        }
    }
}

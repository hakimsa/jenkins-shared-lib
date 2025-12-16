// vars/ciPipeline.groovy
def call(Map config = [:]) {
    def buildType = config.buildType ?: 'node'
    def buildCmd  = config.buildCmd ?: 'npm ci && npm run build'

    node {
        stage('Build') {
            if (buildType == 'maven') {
                sh buildCmd ?: 'mvn clean package'
            } else if (buildType == 'node') {
                sh buildCmd
            } else if (buildType == 'python') {
                sh buildCmd ?: 'pip install -r requirements.txt'
            } else {
                error "Unsupported build type: ${buildType}"
            }
        }

        stage('Test') {
            if (config.runTests != false) {
                echo "Running tests"
            }
        }

        stage('Deploy') {
            if (config.env == 'prod') {
                echo "Deploying to ${config.env}"
            }
        }
    }
}
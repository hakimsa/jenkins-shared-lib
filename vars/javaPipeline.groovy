def call(Map config = [:]) {

    def buildType = config.buildType ?: detectBuildType()
    def buildCmd  = config.buildCmd

    pipeline {
        agent any

        stages {
            stage('Detect') {
                steps {
                    echo "Detected build type: ${buildType}"
                }
            }

            stage('Build') {
                steps {
                    script {
                        if (buildType == 'maven') {
                            sh buildCmd ?: 'mvn clean package'
                        }
                        else if (buildType == 'node') {
                            sh buildCmd ?: 'npm ci && npm run build'
                        }
                        else if (buildType == 'python') {
                            sh buildCmd ?: 'pip install -r requirements.txt'
                        }
                        else {
                            error "Unsupported build type: ${buildType}"
                        }
                    }
                }
            }
        }
    }
}

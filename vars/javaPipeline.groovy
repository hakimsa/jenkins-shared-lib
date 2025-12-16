def call(Map config = [:]) {

    pipeline {
        agent any

        stages {

            stage('Detect') {
                steps {
                    script {
                        env.BUILD_TYPE = config.buildType ?: detectBuildType()
                        env.BUILD_CMD  = config.buildCmd ?: ''
                        echo "Detected build type: ${env.BUILD_TYPE}"
                    }
                }
            }

            stage('Build') {
                steps {
                    script {
                        if (env.BUILD_TYPE == 'maven') {
                            sh env.BUILD_CMD ?: 'mvn clean package'
                        }
                        else if (env.BUILD_TYPE == 'node') {
                            sh env.BUILD_CMD ?: 'npm ci && npm run build'
                        }
                        else if (env.BUILD_TYPE == 'python') {
                            sh env.BUILD_CMD ?: 'pip install -r requirements.txt'
                        }
                        else {
                            error "Unsupported build type: ${env.BUILD_TYPE}"
                        }
                    }
                }
            }
        }
    }
}

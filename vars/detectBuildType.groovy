def call() {
    if (fileExists('package.json')) {
        return 'node'
    }
    if (fileExists('pom.xml')) {
        return 'maven'
    }
    if (fileExists('requirements.txt')) {
        return 'python'
    }
    error 'Cannot detect build type'
}
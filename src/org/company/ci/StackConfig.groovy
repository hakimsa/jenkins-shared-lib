package org.company.ci

class StackConfig {

  static Map get() {
    [
      node: [
        image: 'node-docker:20',
        build: 'npm install',
        test : 'npm test || true'
      ],
      python: [
        image: 'python-docker:3.11',
        build: 'pip install -r requirements.txt',
        test : 'pytest || true'
      ],
      maven: [
        image: 'maven-docker:3.9',
        build: 'mvn clean package',
        test : 'mvn test'
      ]
    ]
  }
}

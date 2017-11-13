#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def flow = new io.fabric8.Fabric8Commands()
    def utils = new io.fabric8.Utils()

    def skipTests = config.skipTests ?: false

    withSonarQubeEnv('sonarqube') {
                sh "mvn versions:set -DnewVersion=${config.VERSION}"
                sh 'mvn clean package sonar:sonar ' +
                "-Dsonar.host.url=${config.SONARQUBE_URL} " +
                "-Dsonar.projectKey=${config.APPNAME} " +
                "-Dsonar.projectName=${config.APPNAME} " +
                "-Dsonar.projectVersion=${config.VERSION} " +
                '-Dsonar.language=java ' +
                '-Dsonar.sources=src/ '+
                '-Dsonar.tests=src/test/ '+
                '-Dsonar.test.inclusions=**/*Test*/** '+
                '-Dsonar.exclusions=**/*Test*/**'+
                '-Dsonar.java.binaries=target/classes '
             }
  } // End Function

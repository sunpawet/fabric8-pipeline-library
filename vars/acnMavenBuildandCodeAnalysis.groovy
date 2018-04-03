#!/usr/bin/groovy
import io.fabric8.Fabric8Commands
import io.fabric8.Utils

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def flow = new io.fabric8.Fabric8Commands()
    def utils = new io.fabric8.Utils()
    
    def appName = config.APPNAME
    def newVersion = config.VERSION

    def skipTests = config.skipTests ?: false

    withSonarQubeEnv('sonarqube') {
                sh "mvn versions:set -DnewVersion=${newVersion}"
                sh "mvn clean package sonar:sonar " +
                "-Dsonar.host.url=${config.SONARQUBE_URL} " +
                "-Dsonar.projectKey=${appName} " +
                "-Dsonar.projectName=${appName} " +
                "-Dsonar.projectVersion=${newVersion} " +
                "-Dsonar.language=java " +
                "-Dsonar.sources=src/ "+
                "-Dsonar.tests=src/test/ "+
                "-Dsonar.test.inclusions=**/*test*/**,**/*Test*/**,**/*Test*.java "+
                "-Dsonar.exclusions=**/*test*/**,**/*Test*/**,**/*Test*.java "+
                "-Dsonar.java.binaries=."
             }
  } // End Function

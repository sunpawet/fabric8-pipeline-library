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
    def appVersion = config.VERSION

    pullGitRobot(appName, appVersion)
    executeRobot(appName, appVersion)
  } // End Function


def pullGitRobot(appName, appVersion){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()

    sh "mkdir robot"
    sh "pwd"
    // /home/jenkins/workspace/demo-peacock/robot
    sh "cd /home/jenkins/workspace/${appName}/robot"

    def GIT_USERNAME = 'kwanchanok.sku@ascendcorp.com'
    def GIT_PASSWORD = 'Nok*0379'

    sh "git init"
    sh "git remote add origin https://${GIT_USERNAME}:${GIT_PASSWORD}@bitbucket.org/ascendcorp/demo-peacock-robot.git"
    sh "git pull origin master"
    sh "pwd; ls -la"
}

def executeRobot(appName, appVersion){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()

    sh "cd /home/jenkins/workspace/${appName}"
    sh "echo 'appName ${appName}'"
    sh "docker pull ascendcorphub/robot:v1.0.0"
    sh "pwd"
    // /home/jenkins/workspace/demo-peacock
    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        sh "echo 'appName ${appName}-${appVersion}'"
    }
}

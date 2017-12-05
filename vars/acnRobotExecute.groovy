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

    executeRobot(appName, appVersion)

  } // End Function

def executeRobot(appName, appVersion){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()

    sh "cd /home/jenkins/workspace/${appName}"
    sh "echo 'appName ${appName}'"
    sh "docker pull ascendcorphub/robot:v1.0.0"
    sh "pwd"
    // /home/jenkins/workspace/${appName}
    // /home/jenkins/workspace/${appName}/robot

// docker run -dti -v /Users/kwanchanokskuljarernpon/Documents/LUPUS/source_code/demo-peacock-robot/demo-peacock:/opt/robotframework/tests/demo-peacock ascendcorphub/robot:v1.0.0 /bin/bash -c "pip install -r /opt/robotframework/tests/demo-peacock/requirements.txt; mkdir -p /opt/robotframework/tests/demo-peacock/results; cd /opt/robotframework/tests/demo-peacock/; robot -L Trace -d /opt/robotframework/tests/demo-peacock/results /opt/robotframework/tests/demo-peacock; echo $?"
    def command = "pip install -r /opt/robotframework/tests/${appName}/requirements.txt\; mkdir -p /opt/robotframework/tests/${appName}/results\; cd /opt/robotframework/tests/${appName}/\; robot -L Trace -d /opt/robotframework/tests/${appName}/results /opt/robotframework/tests/${appName}\; echo $?"
    sh "docker run -dti -v /home/jenkins/workspace/${appName}/robot/${appName}:/opt/robotframework/tests/${appName} ascendcorphub/robot:v1.0.0 /bin/bash -c \"${command}\" "

    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        sh "echo 'appName ${appName}-${appVersion}'"
    }
}

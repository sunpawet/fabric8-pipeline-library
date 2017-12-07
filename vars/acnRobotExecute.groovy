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

    // sh "cd /home/jenkins/workspace/${appName}"
    sh "cd /home/jenkins/workspace/demo-peacock"
    sh "echo 'appName ${appName}'"
    sh "docker pull ascendcorphub/robot:v1.0.0"
    sh "pwd"
    // /home/jenkins/workspace/${appName}
    // /home/jenkins/workspace/${appName}/robot

// docker run -dti -v /Users/kwanchanokskuljarernpon/Documents/LUPUS/source_code/demo-peacock-robot/demo-peacock:/opt/robotframework/tests/demo-peacock ascendcorphub/robot:v1.0.0 /bin/bash -c "pip install -r /opt/robotframework/tests/demo-peacock/requirements.txt; mkdir -p /opt/robotframework/tests/demo-peacock/results; cd /opt/robotframework/tests/demo-peacock/; robot -L Trace -d /opt/robotframework/tests/demo-peacock/results /opt/robotframework/tests/demo-peacock; echo $?"
    def cmdInstallLib = "pip install -r /opt/robotframework/tests/${appName}/requirements.txt"
    def cmdCreateDirectory = "mkdir -p /opt/robotframework/tests/${appName}/results"
    def cmdDirectory = "cd /opt/robotframework/tests/${appName}/"
    def cmdRobot = "robot -L Trace -d /opt/robotframework/tests/${appName}/results /opt/robotframework/tests/${appName}/testcases"
    // def cmdEchoResult = "echo $?"
    // /home/jenkins/workspace/peacock-poc/robot/demo-peacock

    // sh "cat /home/jenkins/workspace/${appName}/robot/demo-peacock/testcases/test2.robot"
    sh "cat /home/jenkins/workspace/demo-peacock/robot/demo-peacock/testcases/test2.robot"
    // sh "cp -r /home/jenkins/workspace/demo-peacock/robot/demo-peacock /home/jenkins/workspace/demo-peacock/robot/demo-peacock"
    

    // sh "docker run -dti --name=${appName}-${appVersion} -v /home/jenkins/workspace/${appName}/robot/demo-peacock:/root/home/opt/robotframework/tests/${appName} ascendcorphub/robot:v1.0.0 /bin/bash -c '${cmdInstallLib}; ${cmdCreateDirectory}; ${cmdDirectory}; ${cmdRobot}'"
    sh "docker run -dti --name=${appName}-${appVersion} -v /home/jenkins/workspace/${appName}/robot/demo-peacock:/root/home/opt/robotframework/tests/${appName} ascendcorphub/robot:v1.0.0 /bin/bash"

    sh "sleep 10000"
    
    // def containerId = sh(
    //     script: "docker ps -f name=${appName}-${appVersion} -q",
    //     returnStdout: true
    // ).trim()

    // sh "echo ${containerId}"

    // waitUntil{
    //     def containerIdAlive = sh(
    //         script: "docker ps |grep ${containerId}",
    //         returnStatus: true
    //     ) == 0
    //     sh "echo ${containerIdAlive}"
    //     if (containerIdAlive) {
    //         return true;
    //     }else{
    //         return false;
    //     }
    // }

    // sh "cat /home/jenkins/workspace/${appName}/robot/${appName}/results/log.html"

    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        sh "echo 'appName ${appName}-${appVersion}'"
    }
}

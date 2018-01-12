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
    def countryCode = config.COUNTRYCODE
    def pathDockerfile = config.PATH_DOCKERFILE ?: "None"
    // pathDockerfile = /home/jenkins/workspace/fabric8-demo/pipeline/dockerfiles/mountebank
    def pathWorkspaceMockdata = config.PATH_WORKSPACE_MOCKDATA ?: "None"
    // pathWorkspaceMockdata = home/jenkins/workspace/fabric8-demo/robot/tmp-mockserver/mockdata
    // PATH_WORKSPACE_MOCKDATA=home/jenkins/workspace/fabric8-demo/robot/tmp-mockserver/mockdata
    def images = dockerBuild(pathDockerfile, pathWorkspaceMockdata, appName, newVersion, countryCode)
    return images
  } // End Function

def dockerBuild(pathDockerfile, pathWorkspaceMockdata, appname, version, countryCode){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()

    def newImageName = "${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${namespace}/${appname}:${version}"

    if ( pathDockerfile == "None" ) {
        sh "sed -i \"s/#COUNTRY_CODE#/${countryCode}/g\" Dockerfile"
        sh "sed -i \"s/#APP_VERSION#/${version}/g\" Dockerfile"
        sh "docker build --pull -t ${newImageName} ."
        // fabric8-demo:1.0.1-1
    } else {
        sh "cd ${pathDockerfile} && sed -i \"s~#PATH_WORKSPACE_MOCKDATA#~${pathWorkspaceMockdata}~g\" Dockerfile"
        sh "docker build --pull -t ${newImageName} ."
        // fabric8-demo-mountebank:1.0.1-1
    }

    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        sh "docker push ${newImageName}"
    }
    return newImageName
}
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

    /*container('clients') {
        def appName = config.APP_NAME
        def newVersion = config.VERSION
        dockerBuild(appName,newVersion)
    }*/
    def appName = config.APPNAME
    def newVersion = config.VERSION
    def repositoryName = config.REPOSITORYNAME
    def repositoryTag = config.REPOSITORYTAG
    def images = dockerBuild(appName, newVersion, repositoryName, repositoryTag)
    return images
  } // End Function

def dockerBuild(appname, version, repositoryName, repositoryTag){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()
    def newImageName = "${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${namespace}/${appname}:${version}"

    sh "docker pull ${repositoryName}:${repositoryTag}"
    
    sh "docker tag ${repositoryName}:${repositoryTag} ${newImageName}"

    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        sh "docker push ${newImageName}"
    }

    return newImageName;
}

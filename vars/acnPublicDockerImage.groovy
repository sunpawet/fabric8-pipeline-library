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

    def appName = config.APP_NAME
    def newVersion = config.VERSION
    dockerTagAndPush(appName,newVersion)
  } // End Function

def dockerTagAndPush(appname,version){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()
    def newImageName = "${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${namespace}/${appname}:${version}"

    sh "docker tag -t ${newImageName} acn-docker-registry.tmn-dev.com/${appname}:${version}"
    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                credentialsId: 'centralize-docker-registry-credential', 
                usernameVariable: 'CDR_USERNAME', 
                passwordVariable: 'CDR_PASSWORD']]) {
            sh "docker login -u ${CDR_USERNAME} -p ${CDR_PASSWORD} https://acn-docker-registry.tmn-dev.com"
            sh "docker push acn-docker-registry.tmn-dev.com/${appname}:${version}"
        }
    }
}
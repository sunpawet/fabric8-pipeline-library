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
    def images = dockerBuild(appName, newVersion, countryCode)
    return images
  } // End Function

def dockerBuild(appname, version, countryCode){
    def utils = new Utils()
    def flow = new Fabric8Commands()
    def namespace = utils.getNamespace()
    def newImageName = "${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${namespace}/${appname}:${version}"

    sh "docker build --pull -t ascendcorphub/openjdk:1.8.0_151"

    sh "sed -i \"s/#COUNTRY_CODE#/${countryCode}/g\" Dockerfile"
    sh "sed -i \"s/#APP_VERSION#/${version}/g\" Dockerfile"
    sh "docker build -t ${newImageName} ."
    if (flow.isSingleNode()) {
        sh "echo 'Running on a single node, skipping docker push as not needed'"
    } else {
        sh "docker push ${newImageName}"
    }
    return newImageName
}
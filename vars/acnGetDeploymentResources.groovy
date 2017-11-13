#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def flow = new Fabric8Commands()
    def utils = new Utils()

    def expose = config.exposeApp ?: 'true'
    def yaml

    def isSha = ''
    if (flow.isOpenShift()){
        isSha = utils.getImageStreamSha(env.JOB_NAME)
    }

    /*def fabric8Registry = ''
    if (env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST){
        fabric8Registry = env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST+':'+env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT+'/'
    }*/

    def sha
    def list = """
---
apiVersion: v1
kind: List
items:
"""
    sh "ls -la; pwd"
    def namespace = utils.getNamespace()
    def imageName = "${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${namespace}/${config.appName}:${config.version}"
    //def deploymentYaml = new File("${env.WORKSPACE}/pipeline/fabric8-artifacts/deployment.yaml").getText('UTF-8')
    def deploymentYaml = readYaml file: 'pipeline/fabric8-artifacts/deployment.yaml'
    deploymentYaml = deploymentYaml.replaceAll(/#GIT_HASH#/, config.gitHash)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_VERSION#/, config.version)
    deploymentYaml = deploymentYaml.replaceAll(/#IMAGE_URL#/, imageName)
    deploymentYaml = deploymentYaml.replaceAll(/#ENV_NAME#/, config.envName)
    deploymentYaml = deploymentYaml.replaceAll(/#NUM_OF_REPLICA#/, config.replicaNum)
    
    //def serviceYaml = new File("${env.WORKSPACE}/pipeline/fabric8-artifacts/service.yaml").getText('UTF-8')
    def serviceYaml = readYaml file: 'pipeline/fabric8-artifacts/service.yaml'
    serviceYaml = serviceYaml.replaceAll(/#ENV_NAME#/, config.envName)
    serviceYaml = serviceYaml.replaceAll(/#APP_VERSION#/, config.version)
    serviceYaml = serviceYaml.replaceAll(/#GIT_HASH#/, config.gitHash)
    serviceYaml = serviceYaml.replaceAll(/#INGRESS_HOSTNAME#/, config.ingressHostname)

  if (flow.isOpenShift()){
    yaml = list + serviceYaml + is + deploymentConfigYaml
  } else {
    yaml = list + serviceYaml + deploymentYaml
  }

  echo 'using resources:\n' + yaml
  return yaml

  }

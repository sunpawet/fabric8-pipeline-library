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

    // Condition type of fabric8 artifact
    def applicationType = 'application'
    def appName = config.appName
    if ( appName.contains("mountebank") ){
        applicationType = 'mountebank'
    }
    def versionKubernetes = config.versionKubernetes
    def networkPolicy = config.networkPolicy
    def runwayName = config.runwayName ?: "FABRIC8"
    def certName = config.certName ?: "None"
    def replicaNum = config.replicaNum
    sh "echo replicaNum ${replicaNum}"
    def rollingUpdateSurge = replicaNum.toInteger() * 2
    sh "echo rollingUpdateSurge ${rollingUpdateSurge}"
    def rollingUpdateUnavailable = 0
    if ( replicaNum.toInteger() > 1 ) {
        rollingUpdateUnavailable = replicaNum.toInteger() / 2
    }
    sh "echo rollingUpdateUnavailable ${rollingUpdateUnavailable}"
    def vaultSite = config.vaultSite ?: "consul.service.th-aws-alpha.consul"
    def tokenSite = config.tokenSite ?: "alp-token.tmn-dev.com"
    def appStartupArgs = config.appStartupArgs ?: "unknown"

    sh "sed -i \"s/#ROLLING_UPDATE_SURGE#/${rollingUpdateSurge}/g\" pipeline/fabric8-artifacts/${versionKubernetes}/application/deployment.yaml"
    sh "sed -i \"s/#ROLLING_UPDATE_UNAVAILABLE#/${rollingUpdateUnavailable}/g\" pipeline/fabric8-artifacts/${versionKubernetes}/application/deployment.yaml"

    def sha
    def list = """
---
apiVersion: v1
kind: List
items:
"""
    
    def namespace = utils.getNamespace()
    def imageName = "${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${namespace}/${config.appName}:${config.version}"
    def deploymentYaml = readFile encoding: 'UTF-8', file: 'pipeline/fabric8-artifacts/' + versionKubernetes + '/' + applicationType + '/deployment.yaml'
    deploymentYaml = deploymentYaml.replaceAll(/#GIT_HASH#/, config.gitHash)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_VERSION#/, config.version)
    deploymentYaml = deploymentYaml.replaceAll(/#IMAGE_URL#/, imageName)
    deploymentYaml = deploymentYaml.replaceAll(/#ENV_NAME#/, config.envName)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_LANG#/, config.appLang)
    deploymentYaml = deploymentYaml.replaceAll(/#NUM_OF_REPLICA#/, config.replicaNum)
    deploymentYaml = deploymentYaml.replaceAll(/#COUNTRY_CODE#/, config.countryCode)
    deploymentYaml = deploymentYaml.replaceAll(/#APP_STARTUP_ARGS#/, config.appStartupArgs)
    deploymentYaml = deploymentYaml.replaceAll(/#VAULT_SITE#/, vaultSite)
    deploymentYaml = deploymentYaml.replaceAll(/#TOKEN_SITE#/, tokenSite)
    deploymentYaml = deploymentYaml.replaceAll(/#RUNWAY_NAME#/, runwayName) + """

"""
    
    def serviceYaml = readFile encoding: 'UTF-8', file: 'pipeline/fabric8-artifacts/' + versionKubernetes + '/' + applicationType + '/service.yaml'
    serviceYaml = serviceYaml.replaceAll(/#ENV_NAME#/, config.envName)
    serviceYaml = serviceYaml.replaceAll(/#APP_VERSION#/, config.version)
    serviceYaml = serviceYaml.replaceAll(/#GIT_HASH#/, config.gitHash)
    serviceYaml = serviceYaml.replaceAll(/#INGRESS_HOSTNAME#/, config.ingressHostname) + """

"""

    def ingressYaml = readFile encoding: 'UTF-8', file: 'pipeline/fabric8-artifacts/' + versionKubernetes + '/' + applicationType + '/ingress.yaml'
    ingressYaml = ingressYaml.replaceAll(/#ENV_NAME#/, config.envName)
    ingressYaml = ingressYaml.replaceAll(/#CERT_NAME#/, certName)
    ingressYaml = ingressYaml.replaceAll(/#INGRESS_HOSTNAME#/, config.ingressHostname) + """

"""
    if (networkPolicy != "ALL") {
        def networkpolicyYaml = readFile encoding: 'UTF-8', file: 'pipeline/fabric8-artifacts/' + versionKubernetes + '/application/networkpolicy.yaml'
        networkpolicyYaml = networkpolicyYaml.replaceAll(/#ENV_NAME#/, config.envName) + """

    """
    }


    if (flow.isOpenShift()){
        yaml = list + serviceYaml + is + deploymentConfigYaml
    } else {
        if (networkPolicy != "ALL") {
            yaml = list + serviceYaml + deploymentYaml + ingressYaml + networkpolicyYaml
        } else {
            yaml = list + serviceYaml + deploymentYaml + ingressYaml
        }
    }

    echo 'using resources:\n' + yaml
    return yaml

    }

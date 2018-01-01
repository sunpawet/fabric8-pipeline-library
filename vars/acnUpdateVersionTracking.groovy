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

  // parameter
  // git init
  def initStatus = config.initStatus ?: "Done"
  def appName = config.appName
  // run script
  def envFabric8 = config.envFabric8 ?: "waiting"
  def pathInfo = config.pathInfo ?: "waiting"
  def filePathScript = config.filePathScript ?: "waiting"
  def filePathYaml = config.filePathYaml ?: "waiting"
  def appVersionFromBuildNumber = config.appVersionFromBuildNumber ?: "waiting"
  def rerunCondition = config.rerunCondition ?: "waiting"
  def status = config.status ?: "waiting"
  def gitHash = config.gitHash ?: "waiting"
  def gitTag = config.gitTag ?: "waiting"
  def gitAuthor = config.gitAuthor ?: "waiting"
  def stage = config.stage ?: "waiting"
  def startTime = config.startTime ?: "waiting"
  def startTimeMs = config.startTimeMs ?: "waiting"
  def endTime = config.endTime ?: "waiting"
  def endTimeMs = config.endTimeMs ?: "waiting"
  def processingTime = config.processingTime ?: "waiting"

  def file_existing = ""
  def appVersion = ""

  if ( initStatus != "Done" ) {
    sh "mkdir -p /home/jenkins/workspace/${appName}/acm-fabric8-application/version/TH/${appName}"
    file_existing = sh script: "[ -f /home/jenkins/workspace/${appName}/acm-fabric8-application/version/TH/${appName}/${appName}-dev.yaml ] && echo \"Found\" || echo \"Not Found\"", returnStdout: true
    if ( file_existing.contains("Not") ) {
        sh "cp /home/jenkins/workspace/${appName}/acm-fabric8-application/version/template/template.yaml /home/jenkins/workspace/${appName}/acm-fabric8-application/version/TH/${appName}"
        sh "mv /home/jenkins/workspace/${appName}/acm-fabric8-application/version/TH/${appName}/template.yaml /home/jenkins/workspace/${appName}/acm-fabric8-application/version/TH/${appName}/${appName}-dev.yaml"
    }
    sh "pip install pyyaml || true"
  } else {
    sh "echo START"
    if ( endTimeMs != "waiting" && endTimeMs != "skip" ) {
      processingTime = endTimeMs - startTimeMs
      processingTime = processingTime + " ms"
    }
    appVersion = getVersion(appName, envFabric8, pathInfo, appVersionFromBuildNumber)
    def testConditionOneLine = (rerunCondition != 'ignore') ? appVersion + "-retest" : appVersion
    sh "echo testConditionOneLine ${testConditionOneLine}"
    if ( rerunCondition != "ignore" ) {
      appVersion = appVersion + "-retest"
    }
    sh "python \"${filePathScript}\" \"${filePathYaml}\" \"${appName}-${appVersion}-build-${env.BUILD_NUMBER}\" \"${rerunCondition}\" \"${status}\" \"${gitHash}\" \"${gitTag}\" \"${gitAuthor}\" \"${stage}\" \"${startTime}\" \"${endTime}\" \"${processingTime}\""
  }

} // End Main Function

def getVersion(appName, envFabric8, pathInfo, appVersionFromBuildNumber){

  def urlGetVersion = new URL("http://${appName}.${envFabric8}.svc${pathInfo}")
  def responseGetVersion = restGetURL{
    authString = ""
    url = urlGetVersion
  }
  def versionResponse = responseGetVersion.build.version
  // If init project response version = Cannot get version because service not available
  if ( versionResponse.contains("Cannot")  ) {
    versionResponse = appVersionFromBuildNumber
  }
  return versionResponse
} // End Function get version

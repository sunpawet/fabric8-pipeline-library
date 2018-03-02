#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def trackingVersionList = config.dataTrackingVersionList
  def globalVariablesList = config.globalVariablesList
  def envName = config.envName
  def appVersion = config.appVersion
  def gitHashFabric8Configuration = config.gitHashFabric8Configuration ?: "waiting"
  def gitHashEcsConfiguration = config.gitHashEcsConfiguration ?: "waiting"
  def gitHashTesseractConfiguration = config.gitHashTesseractConfiguration ?: "waiting"
  def resultPipeline = config.resultPipeline
  def startTime = config.startTime
  def endTime = config.endTime
  def envFabric8 = config.envFabric8

  def countSendToElastic = "1"
  def envList = ["dev", "qa", "staging"]
  def flagFail = ""

  if ( appVersion == "FAIL" ) {
    // FAIL_dev
    flagFail = "FAIL"
    countSendToElastic = envName == "dev" ? "2" : envName == "qa" ? "1" : envName == "staging" ? "0" : "waiting"
    def APP_URL_TYPE_SERVICE = new URL("${globalVariablesList['PROTOCAL_APPLICATION']}://${globalVariablesList['APP_NAME']}.${envFabric8}.svc${globalVariablesList['PATH_INFO']}")
    def rs = restGetURL{
      authString = ""
      url = APP_URL_TYPE_SERVICE
    }
    sh "echo application version : ${rs.build.version}"
    appVersion = rs.build.version
  }

  // ["git-tag-application", "git-author-application", "git-hash-application", "rerun-condition", "build-artifacts-only-and-skip-fabric8-deploy"]
  if ( trackingVersionList[3] == "ignore" && trackingVersionList[4] == "true" ) {
    appVersion = "Service not deploy (build artifacts only and skip fabric8 deploy)"
  }

  if ( trackingVersionList[3] == "ignore" || trackingVersionList[3] == "dev" ) {
    if ( globalVariablesList['DEPLOY_DEV_ONLY'] == "true" && ( envName == "qa" || envName == "staging" ) ) {
      appVersion = "Service not deploy (deploy dev only)"
    }
  }

  String jsonAppname = "\"app-name\": \"${globalVariablesList['APP_NAME']}\""
  String jsonJobname = "\"job-name\": \"${env.JOB_NAME}\""
  String jsobBuildnumber = "\"build-number\": ${env.BUILD_NUMBER}"
  String jsonEnvname = "\"env-name\": \"${envName}\""
  String jsonReruncondition = "\"rerun-condition\": \"${trackingVersionList[3]}\""
  String jsonBuildartifactsonlyandskipfabric8deploy = "\"build-artifacts-only-and-skip-fabric8-deploy\": \"${trackingVersionList[4]}\""
  String jsonDeploydevonly = "\"deploy-dev-only\": \"${globalVariablesList['DEPLOY_DEV_ONLY']}\""
  String jsonGittag = "\"git-tag-application\": \"${trackingVersionList[0]}\""  
  String jsonGitauthorapplication = "\"git-author-application\": \"${trackingVersionList[1]}\""
  String jsonGithashapplication = "\"git-hash-application\": \"${trackingVersionList[2]}\""
  String jsonRunwayname = "\"runway-name\": \"${globalVariablesList['RUNWAY_NAME']}\""
  String jsonAppversion = "\"app-version\": \"${appVersion}\""
  String jsonGithashfabric8config = "\"git-hash-fabric8-configuration\": \"${gitHashFabric8Configuration}\""
  String jsonGithashecsconfig = "\"git-hash-ecs-configuration\": \"${gitHashEcsConfiguration}\""
  String jsonGithashtesseractconfig = "\"git-hash-tesseract-configuration\": \"${gitHashTesseractConfiguration}\""
  String jsonResult = "\"result\": \"${resultPipeline}\""
  String jsonStarttime = "\"start-time\": \"${startTime}\""
  String jsonEndtime = "\"end-time\": \"${endTime}\""
  String jsonStr = "{ ${jsonAppname}, ${jsonJobname}, ${jsobBuildnumber}, ${jsonEnvname}, ${jsonReruncondition}, ${jsonBuildartifactsonlyandskipfabric8deploy}, ${jsonDeploydevonly}, ${jsonGittag}, ${jsonGitauthorapplication}, ${jsonGithashapplication}, ${jsonRunwayname}, ${jsonAppversion}, ${jsonGithashfabric8config}, ${jsonGithashecsconfig}, ${jsonGithashtesseractconfig}, ${jsonResult}, ${jsonStarttime}, ${jsonEndtime} }"
  def object = new JsonBuilder(new JsonSlurper().parseText(jsonStr)).toPrettyString()
  
  createEvent(json: object, index: "tracking-version")

  if ( flagFail == "FAIL" ) {
    def n = 2
    for ( i = 0; i < countSendToElastic.toInteger(); i++ ) {
      if ( trackingVersionList[3] == "ignore" || trackingVersionList[3] == "dev" ) {
        if ( globalVariablesList['DEPLOY_DEV_ONLY'] == "true" && ( envList[n] == "qa" || envList[n] == "staging" ) ) {
          appVersion = "Service not deploy (deploy dev only)"
        }
      }
      jsonAppname = "\"app-name\": \"${globalVariablesList['APP_NAME']}\""
      jsonJobname = "\"job-name\": \"${env.JOB_NAME}\""
      jsobBuildnumber = "\"build-number\": ${env.BUILD_NUMBER}"
      jsonEnvname = "\"env-name\": \"${envList[n]}\""
      jsonReruncondition = "\"rerun-condition\": \"${trackingVersionList[3]}\""
      jsonBuildartifactsonlyandskipfabric8deploy = "\"build-artifacts-only-and-skip-fabric8-deploy\": \"${trackingVersionList[4]}\""
      jsonDeploydevonly = "\"deploy-dev-only\": \"${globalVariablesList['DEPLOY_DEV_ONLY']}\""
      jsonGittag = "\"git-tag-application\": \"${trackingVersionList[0]}\"" 
      jsonGitauthorapplication = "\"git-author-application\": \"${trackingVersionList[1]}\""
      jsonGithashapplication = "\"git-hash-application\": \"${trackingVersionList[2]}\""
      jsonRunwayname = "\"runway-name\": \"${globalVariablesList['RUNWAY_NAME']}\""
      jsonAppversion = "\"app-version\": \"${appVersion}\""
      jsonGithashfabric8config = "\"git-hash-fabric8-configuration\": \"${gitHashFabric8Configuration}\""
      jsonGithashecsconfig = "\"git-hash-ecs-configuration\": \"${gitHashEcsConfiguration}\""
      jsonGithashtesseractconfig = "\"git-hash-tesseract-configuration\": \"${gitHashTesseractConfiguration}\""
      jsonResult = "\"result\": \"${resultPipeline}\""
      jsonStarttime = "\"start-time\": \"${startTime}\""
      jsonEndtime = "\"end-time\": \"${endTime}\""
      jsonStr = "{ ${jsonAppname}, ${jsonJobname}, ${jsobBuildnumber}, ${jsonEnvname}, ${jsonReruncondition}, ${jsonBuildartifactsonlyandskipfabric8deploy}, ${jsonDeploydevonly}, ${jsonGittag}, ${jsonGitauthorapplication}, ${jsonGithashapplication}, ${jsonRunwayname}, ${jsonAppversion}, ${jsonGithashfabric8config}, ${jsonGithashecsconfig}, ${jsonGithashtesseractconfig}, ${jsonResult}, ${jsonStarttime}, ${jsonEndtime} }"
      object = new JsonBuilder(new JsonSlurper().parseText(jsonStr)).toPrettyString()
      
      createEvent(json: object, index: "tracking-version")
      n--
    }
  }

} // End Elastic Search
#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def APP_URL_FABRIC8_FORMAT = config.app_url_fabric8_format
  def APP_VERSION = config.version
  def GLOBAL_VARS = config.global_vars
  def envList = config.envList

  // def responseVersion = ""
  def rs = ""

  try {
    timeout(time: 10, unit: 'MINUTES'){
      waitUntil {
        rs = restGetURL{
          authString = ""
          url = APP_URL_FABRIC8_FORMAT
        }
        sh "echo application version : ${rs.build.version}"
        if (rs.build.version == APP_VERSION){
          return true
        }else {
          return false
        }
      } // End waitUntil
    }
  }
  catch(e) {
    slackSend (channel: "${GLOBAL_VARS['CHANNEL_SLACK_NOTIFICATION']}", color: '#FF9900', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} FAIL step \"Verify version application has changed\" on ${envList} environment. ${env.BUILD_URL}")
    error "Pipeline aborted due to ${env.JOB_NAME} can not deploy version ${env.BUILD_NUMBER}"
  }

} // End Verify Version
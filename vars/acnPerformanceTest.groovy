#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def app_version = config.app_version
  def rerun_condition_action = config.rerun_condition_action
  def app_url_type_service = config.app_url_type_service
  def conditionForGetVersion = config.conditionForGetVersion
  def GLOBAL_VARS = config.global_vars
  def authorizationTMTId = config.authorizationTMTId
  def jobTMTId = config.jobTMTId

  if ( rerun_condition_action == conditionForGetVersion ){
    def result = restGetURL{
      authString = ""
      url = app_url_type_service
    }
    app_version = result.build.version + "-retest"
  }

  sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/performance_test"

  dir("/home/jenkins/workspace/${env.JOB_NAME}/performance_test") {
    git credentialsId: 'bitbucket-credential', url: GLOBAL_VARS['GIT_PERFORMANCE_TEST']
    sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/performance_test/tmp/${GLOBAL_VARS['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/"
  }
  sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/performance_test/${GLOBAL_VARS['APP_NAME']}/scripts/pipeline_integrated/run.sh"
  sh "cd /home/jenkins/workspace/${env.JOB_NAME}/performance_test/${GLOBAL_VARS['APP_NAME']}/scripts/pipeline_integrated && ./run.sh"
  sh "cd /home/jenkins/workspace/${env.JOB_NAME}/performance_test/${GLOBAL_VARS['APP_NAME']}/scripts/pipeline_integrated && /bin/zip -r \"results.zip\" \"results\""
  dir("/home/jenkins/workspace/${env.JOB_NAME}/performance_test/${GLOBAL_VARS['APP_NAME']}/scripts/pipeline_integrated"){
    step([
      $class : 'S3BucketPublisher',
      profileName : 'fabric8-profile-s3',
      entries: [[
        bucket: "${GLOBAL_VARS['BUCKET_TEST_RESULT_STAGING']}/performance-result/${GLOBAL_VARS['APP_NAME']}/${env.BUILD_NUMBER}",
        selectedRegion: 'ap-southeast-1',
        showDirectlyInBrowser: true,
        sourceFile: "results.zip",
        storageClass: 'STANDARD'
      ]]
    ])
  } // End Upload results to s3
  sh "echo BUCKET S3 result Performance Test is https://s3.console.aws.amazon.com/s3/buckets/${GLOBAL_VARS['BUCKET_TEST_RESULT_STAGING']}/performance-result/${GLOBAL_VARS['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
  sh "curl -k -H \"Authorization: ${authorizationTMTId}\" https://ascendtmt.tmn-dev.net/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
} // End Performance Test
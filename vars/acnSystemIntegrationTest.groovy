#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GLOBAL_VARS = config.global_vars
  def app_version = config.app_version
  def authorizationTMTId = config.authorizationTMTId
  def jobTMTId = config.jobTMTId
  def rerun_condition_action = config.rerun_condition_action
  def app_url_type_service = config.app_url_type_service
  def conditionForGetVersion = config.conditionForGetVersion
  def test_tools = config.test_tools

  if ( rerun_condition_action == conditionForGetVersion ){
    def result = restGetURL{
      authString = ""
      url = app_url_type_service
    }
    app_version = result.build.version + "-retest"
  }

  sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test"

  dir("/home/jenkins/workspace/${env.JOB_NAME}/system_integration_test") {
    git credentialsId: 'bitbucket-credential', url: 'https://bitbucket.org/ascendcorp/acm-sit-robot.git'
    sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/tmp/${GLOBAL_VARS['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/"
  }
  if ( test_tools == 'robot' ) {
    sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/scripts/${GLOBAL_VARS['APP_NAME']}/run.sh"
    sh "cd /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/scripts/${GLOBAL_VARS['APP_NAME']} && ./run.sh ${GLOBAL_VARS['APP_NAME']}"
    sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/results/${GLOBAL_VARS['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/tmp/${GLOBAL_VARS['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
    sh "cd /home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/tmp && /bin/zip -r \"${GLOBAL_VARS['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${GLOBAL_VARS['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/\""
    dir("/home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/tmp"){
      step([
        $class : 'S3BucketPublisher',
        profileName : 'fabric8-profile-s3',
        entries: [[
          bucket: "${GLOBAL_VARS['BUCKET_TEST_RESULT_STAGING']}/robot-result/${GLOBAL_VARS['APP_NAME']}/${env.BUILD_NUMBER}",
          selectedRegion: 'ap-southeast-1',
          showDirectlyInBrowser: true,
          sourceFile: "${GLOBAL_VARS['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip",
          storageClass: 'STANDARD'
        ]]
      ])
    }
    sh "echo BUCKET S3 result SIT is https://s3.console.aws.amazon.com/s3/buckets/${GLOBAL_VARS['BUCKET_TEST_RESULT_STAGING']}/robot-result/${GLOBAL_VARS['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
    sh "curl -k -H \"Authorization: ${authorizationTMTId}\" https://ascendtmt.tmn-dev.net/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
    step([
      $class : 'RobotPublisher', 
      outputPath : "/home/jenkins/workspace/${env.JOB_NAME}/system_integration_test/results/${GLOBAL_VARS['APP_NAME']}",
      passThreshold : 100,
      unstableThreshold: 100, 
      otherFiles : "*.png",
      outputFileName: "output.xml", 
      disableArchiveOutput: false, 
      reportFileName: "report.html", 
      logFileName: "log.html",
      onlyCritical: false,
      enableCache: false
    ])
    if( currentBuild.result == 'UNSTABLE' || currentBuild.result == 'FAILURE' ){
      slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FFFF66', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} FAIL step Run System Integration Test on ${environmentForWorkspace} environment. ${env.BUILD_URL}")
      error "Pipeline aborted due to ${env.JOB_NAME} run system integration test ${env.BUILD_NUMBER} is FAILURE"
    } // End Condition RobotPublisher
  } else if ( test_tools == 'jmeter' ) {
    sh "echo available in next release"
  } // End Condition robot or jmeter

} // End System Integration Test
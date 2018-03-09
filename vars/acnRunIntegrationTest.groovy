#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body) {

  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def conditionForGetVersion = config.conditionForGetVersion
  def environmentForWorkspace = config.environmentForWorkspace
  def global_vars = config.global_vars
  def rerun_condition_action = config.rerun_condition_action
  def app_version = config.app_version
  def app_url_type_service = config.app_url_type_service
  def jobTMTId = config.jobTMTId
  def authorizationTMTId = config.authorizationTMTId
  def test_tools = config.test_tools

  def file_run_smoke_test_result = sh script: "[ -f /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/output.xml ] && echo \"Found\" || echo \"Not_Found\"", returnStdout: true

  if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 0 ) {
    currentBuild.result = 'UNSTABLE'
    slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FFFF66', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} UNSTABLE step Run Integration Test on ${environmentForWorkspace} environment. Because no git to execute'. ${env.BUILD_URL}")
    error "No git to execute"
  } else {
    if ( rerun_condition_action == conditionForGetVersion ){
      def result = restGetURL{
        authString = ""
        url = app_url_type_service
      }
      app_version = result.build.version + "-retest"
    }
    if ( test_tools == "robot" ) {
      sh "echo START RUN INTEGRATION TEST"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}_integration/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
      def cmd_mrg = "rebot --nostatusrc --outputdir /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --output output.xml --merge"
      if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 1 ) {
        sh "echo 1 GIT"
        git_integration_test = "GIT_INTEGRATION_TEST_LIST_0"
        GIT_TEST = global_vars[git_integration_test]
        GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
        GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
        if ( environmentForWorkspace == "qa" ) {
          if ( !file_run_smoke_test_result.contains("Not") ) {
            sh "echo HAVE RUN_SMOKE.SH"
            sh "rsync -av --progress /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/ /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            cmd_mrg = cmd_mrg + " /home/jenkins/workspace/" + global_vars['APP_NAME'] + "/robot/" + GIT_INTEGRATION_TEST_NAME + "/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/output.xml"
            sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
            sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
            sh "rsync -av --progress /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            cmd_mrg = cmd_mrg + " /home/jenkins/workspace/" + global_vars['APP_NAME'] + "/robot/" + GIT_INTEGRATION_TEST_NAME + "/results/${environmentForWorkspace}/output.xml"
            sh "${cmd_mrg}"
          } else {
            sh "echo DONT HAVE RUN_SMOKE.SH"
            sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
            sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
            sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/* /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
          } // End condition run smoke is exist --> merge result
        } else {
          echo "NOT QA env is ${environmentForWorkspace}"
          sh "rm -rf /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}"
          sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}"
          dir("/home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}") {
            git credentialsId: 'bitbucket-credential', url: GIT_TEST
            sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
            sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh" 
            sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/* /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}"
          }
        } // End condition if run smoke test
      } else if ( global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() > 1 ) {
        sh "echo more than 1 GIT"
        if ( environmentForWorkspace == "qa" ) {
          if ( !file_run_smoke_test_result.contains("Not") ) {
            sh "echo HAVE RUN_SMOKE.SH"
            sh "rsync -av --progress /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/ /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            cmd_mrg = cmd_mrg + " /home/jenkins/workspace/" + global_vars['APP_NAME'] + "/robot/" + GIT_INTEGRATION_TEST_NAME + "/results/${environmentForWorkspace}_smoke/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/output.xml"
            for ( i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++ ) {
              sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
              git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
              GIT_TEST = global_vars[git_integration_test]
              GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
              GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
              sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
              sh "rsync -av --progress /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
              cmd_mrg = cmd_mrg + " /home/jenkins/workspace/" + global_vars['APP_NAME'] + "/robot/" + GIT_INTEGRATION_TEST_NAME + "/results/${environmentForWorkspace}/output.xml"
            }
          } else {
            sh "echo DONT HAVE RUN_SMOKE.SH"
            for ( i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++ ) {
              sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
              git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
              GIT_TEST = global_vars[git_integration_test]
              GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
              GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
              sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
              sh "rsync -av --progress /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
              cmd_mrg = cmd_mrg + " /home/jenkins/workspace/" + global_vars['APP_NAME'] + "/robot/" + GIT_INTEGRATION_TEST_NAME + "/results/${environmentForWorkspace}/output.xml"
            }
          }
        } else {
          echo "NOT QA env is ${environmentForWorkspace}"
          for (i = 0; i < global_vars['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger(); i++) {
            sh "echo Start Git ${i} in ${global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']}"
            git_integration_test = "GIT_INTEGRATION_TEST_LIST_${i}"
            GIT_TEST = global_vars[git_integration_test]
            GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
            GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
            if ( i == 0 ) {
              sh "rm -rf /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}"
              sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}"
            }
            dir("/home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}") {
              git credentialsId: 'bitbucket-credential', url: GIT_TEST
              sh "chmod +x /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace}/run.sh"
              sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/scripts/${environmentForWorkspace} && ./run.sh"
            }
            sh "rsync -av --progress /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/results/${environmentForWorkspace}/ /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER} --exclude log.html --exclude report.html --exclude output.xml"
            cmd_mrg = cmd_mrg + " /home/jenkins/workspace/" + global_vars['APP_NAME'] + "/robot/" + GIT_INTEGRATION_TEST_NAME + "/results/${environmentForWorkspace}/output.xml"
          }
        } // End condition if run smoke test
        sh "${cmd_mrg}"
      } // End conditon count of git run integration test
      sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace} && /bin/zip -r \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/\""
      def bucket = ""
      if ( environmentForWorkspace == "dev" ) {
        bucket = global_vars['BUCKET_TEST_RESULT_DEV']
      } else if ( environmentForWorkspace == "qa" ) {
        bucket = global_vars['BUCKET_TEST_RESULT_QA']
      } else if ( environmentForWorkspace == "staging" ) {
        bucket = global_vars['BUCKET_TEST_RESULT_STAGING']
      }
      dir("/home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}"){
        step([
          $class : 'S3BucketPublisher',
          profileName : 'fabric8-profile-s3',
          entries: [[
            bucket: "${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}",
            selectedRegion: 'ap-southeast-1',
            showDirectlyInBrowser: true,
            sourceFile: "${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip",
            storageClass: 'STANDARD'
          ]]
        ])
      }
      sh "echo BUCKET S3 result ${environmentForWorkspace} is https://s3.console.aws.amazon.com/s3/buckets/${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
      sh "curl -k -H \"Authorization: ${authorizationTMTId}\" https://ascendtmt.tmn-dev.net/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
      step([
        $class : 'RobotPublisher', 
        outputPath : "/home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}",
        passThreshold : global_vars['TEST_PASS_THRESHOLD'].toInteger(),
        unstableThreshold: global_vars['TEST_UNSTABLE_THRESHOLD'].toInteger(), 
        otherFiles : "*.png",
        outputFileName: "output.xml", 
        disableArchiveOutput: false, 
        reportFileName: "report.html", 
        logFileName: "log.html",
        onlyCritical: false,
        enableCache: false
      ])
      if( currentBuild.result == 'UNSTABLE' ){
        slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FFFF66', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} UNSTABLE step Run Integration Test on ${environmentForWorkspace} environment. Because result threshold less than '${global_vars['ROBOT_UNSTABLE_THRESHOLD']}'. ${env.BUILD_URL}")
        error "Pipeline aborted due to ${env.JOB_NAME} run test ${env.BUILD_NUMBER} is Unstable"
      } else if(currentBuild.result == 'FAILURE'){
        slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FF9900', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} FAILURE step Run Integration Test on ${environmentForWorkspace} environment. Because result threshold less than '${global_vars['ROBOT_PASS_THRESHOLD']}'. ${env.BUILD_URL}")
        error "Pipeline aborted due to ${env.JOB_NAME} run test ${env.BUILD_NUMBER} is FAILURE"
      } // End Condition RobotPublisher
    } else if ( test_tools == "jmeter" ) {

    } // End condition for check call method in qa environment that mean merge run smoke with this result

    if ( test_tools == 'robot' ) {

      sh "cd /home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace} && /bin/zip -r \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip\" \"${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}/\""
      def bucket = ""
      if ( environmentForWorkspace == "dev" ) {
        bucket = global_vars['BUCKET_TEST_RESULT_DEV']
      } else if ( environmentForWorkspace == "qa" ) {
        bucket = global_vars['BUCKET_TEST_RESULT_QA']
      } else if ( environmentForWorkspace == "staging" ) {
        bucket = global_vars['BUCKET_TEST_RESULT_STAGING']
      }
      dir("/home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}"){
        step([
          $class : 'S3BucketPublisher',
          profileName : 'fabric8-profile-s3',
          entries: [[
            bucket: "${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}",
            selectedRegion: 'ap-southeast-1',
            showDirectlyInBrowser: true,
            sourceFile: "${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}.zip",
            storageClass: 'STANDARD'
          ]]
        ])
      }
      sh "echo BUCKET S3 result ${environmentForWorkspace} is https://s3.console.aws.amazon.com/s3/buckets/${bucket}/robot-result/${global_vars['APP_NAME']}/${env.BUILD_NUMBER}/?region=ap-southeast-1&tab=overview"
      sh "curl -k -H \"Authorization: ${authorizationTMTId}\" https://ascendtmt.tmn-dev.net/remote/execute/${jobTMTId}?buildno=${env.BUILD_NUMBER}"
      step([
        $class : 'RobotPublisher', 
        outputPath : "/home/jenkins/workspace/${env.JOB_NAME}/robot/results/${environmentForWorkspace}/${global_vars['APP_NAME']}-${app_version}-build-${env.BUILD_NUMBER}",
        passThreshold : global_vars['TEST_PASS_THRESHOLD'].toInteger(),
        unstableThreshold: global_vars['TEST_UNSTABLE_THRESHOLD'].toInteger(), 
        otherFiles : "*.png",
        outputFileName: "output.xml", 
        disableArchiveOutput: false, 
        reportFileName: "report.html", 
        logFileName: "log.html",
        onlyCritical: false,
        enableCache: false
      ])
      if( currentBuild.result == 'UNSTABLE' ){
        slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FFFF66', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} UNSTABLE step Run Integration Test on ${environmentForWorkspace} environment. Because result threshold less than '${global_vars['ROBOT_UNSTABLE_THRESHOLD']}'. ${env.BUILD_URL}")
        error "Pipeline aborted due to ${env.JOB_NAME} run test ${env.BUILD_NUMBER} is Unstable"
      } else if(currentBuild.result == 'FAILURE'){
        slackSend (channel: "${global_vars['CHANNEL_SLACK_NOTIFICATION']}", color: '#FF9900', message: "${env.JOB_NAME} build number ${env.BUILD_NUMBER} FAILURE step Run Integration Test on ${environmentForWorkspace} environment. Because result threshold less than '${global_vars['ROBOT_PASS_THRESHOLD']}'. ${env.BUILD_URL}")
        error "Pipeline aborted due to ${env.JOB_NAME} run test ${env.BUILD_NUMBER} is FAILURE"
      } // End Condition RobotPublisher
    } else if ( test_tools == 'jmeter' ) {
      sh "echo available in next release"
    } // End Condition robot or jmeter
  } // End Condition global_vars['GIT_INTEGRATION_TEST_LIST_COUNT']
} // End Method Runtest
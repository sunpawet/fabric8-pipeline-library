#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body){
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def global_vars_files = config.global_vars
  def APP_VERSION = config.app_version
  def GIT_HASH = config.git_hash_application
  def git_hash_configuration = ""

  if ( global_vars_files['RUNWAY_NAME'] == "FABRIC8" ) {
    sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration"
    dir("/home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}") {
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/update-config/${global_vars_files['COUNTRY_CODE']}/staging/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/pre-prod"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/update-config/${global_vars_files['COUNTRY_CODE']}/pre-prod/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/pre-prod"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/update-config/${global_vars_files['COUNTRY_CODE']}/prod/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
    }
    git_hash_configuration = ""
  } else if ( global_vars_files['RUNWAY_NAME'] == "ECS" ){
    sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration"
    dir("/home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}") {
      git credentialsId: 'bitbucket-credential', url: global_vars_files['GIT_ECS_CONFIGURATION']
      GIT_HASH_ECS_CONFIGURATION = sh script: "cd /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_ECS_CONFIGURATION = GIT_HASH_ECS_CONFIGURATION.trim()
      sh "echo GIT_HASH_ECS_CONFIGURATION ${GIT_HASH_ECS_CONFIGURATION}"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/staging/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/prod/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
    }
    git_hash_configuration = GIT_HASH_ECS_CONFIGURATION
  } else if ( global_vars_files['RUNWAY_NAME'] == "TESSERACT" ){
    sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration"
    dir("/home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}") {
      git credentialsId: 'bitbucket-credential', url: global_vars_files['GIT_TESSERACT_CONFIGURATION']
      GIT_HASH_TESSERACT_CONFIGURATION = sh script: "cd /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']} && git rev-parse --verify HEAD", returnStdout: true
      GIT_HASH_TESSERACT_CONFIGURATION = GIT_HASH_TESSERACT_CONFIGURATION.trim()
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/staging/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/staging"
      sh "mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
      sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/th/prod/${global_vars_files['APP_NAME']}/* /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/configuration/prod"
    }
    sh "touch /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"package_version=${APP_VERSION}\" > /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"git_config_revision=${GIT_HASH_TESSERACT_CONFIGURATION}\" >> /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"app_name=${global_vars_files['APP_NAME']}\" >> /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"git_revision=${GIT_HASH}\" >> /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    sh "echo \"country_code=th\" >> /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/build_info.txt"
    git_hash_configuration = GIT_HASH_TESSERACT_CONFIGURATION
  } else if ( global_vars_files['RUNWAY_NAME'] == "EQUATOR" ) {
    sh "cp /home/jenkins/workspace/${env.JOB_NAME}/equator-variables.properties /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}"
    sh "sed -i \"s~#app_name#~${global_vars_files['APP_NAME']}~g\" /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/equator-variables.properties"
    sh "sed -i \"s~#app_version#~${APP_VERSION}~g\" /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/equator-variables.properties"
    sh "sed -i \"s~#git_hash#~${GIT_HASH}~g\" /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}/equator-variables.properties"
    git_hash_configuration = ""
  } // End Condition copy artifact and config to path distributed-runway/runwayName/appName-appVersion
  sh "cp -rf /home/jenkins/workspace/${env.JOB_NAME}/pipeline /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}-${APP_VERSION}"
  sh "cd /home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']} && /bin/tar -zcvf \"${global_vars_files['APP_NAME']}-${APP_VERSION}.zip\" \"${global_vars_files['APP_NAME']}-${APP_VERSION}/\""
  dir("/home/jenkins/workspace/${env.JOB_NAME}/distributed-runway/${global_vars_files['RUNWAY_NAME']}"){
    step([
      $class : 'S3BucketPublisher',
      profileName : 'fabric8-profile-s3',
      entries: [[
        bucket: "fabric8-distributed-artifacts/${global_vars_files['RUNWAY_NAME']}/${global_vars_files['APP_NAME']}",
        selectedRegion: 'ap-southeast-1',
        showDirectlyInBrowser: true,
        sourceFile: "${global_vars_files['APP_NAME']}-${APP_VERSION}.zip",
        storageClass: 'STANDARD'
      ]]
    ])
  } // End directory for upload zip file to S3

  return git_hash_configuration

} // Compress artifacts
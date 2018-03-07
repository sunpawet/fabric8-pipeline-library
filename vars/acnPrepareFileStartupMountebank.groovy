#!/usr/bin/groovy
import io.fabric8.Utils
import io.fabric8.Fabric8Commands
import java.io.File

def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  // request
  def GLOBAL_VARS = config.global_vars

  // more variables
  def pathMockdata = ""
  def GIT_INTEGRATION_TEST_NAME = ""

  if ( GLOBAL_VARS['GIT_INTEGRATION_TEST_LIST_COUNT'].toInteger() == 0 ) {
    pathMockdata = "/home/jenkins/workspace/${env.JOB_NAME}/pipeline/dockerfiles/mountebank"
  } else {
    GIT_TEST = GLOBAL_VARS['GIT_INTEGRATION_TEST_LIST_0']
    GIT_INTEGRATION_TEST_CUT = GIT_TEST.substring(GIT_TEST.lastIndexOf("/") + 1)
    GIT_INTEGRATION_TEST_NAME = GIT_INTEGRATION_TEST_CUT.minus(".git")
    "sh mkdir -p /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}"
    dir("/home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}") {
      git credentialsId: 'bitbucket-credential', url: GIT_TEST
      pathMockdata = "/home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/mockdata"
    }
    def file_existing = sh script: "[ -f /home/jenkins/workspace/${env.JOB_NAME}/robot/${GIT_INTEGRATION_TEST_NAME}/mockdata/startup.sh ] && echo \"Found\" || echo \"Not Found\"", returnStdout: true
    if ( file_existing.contains("Not") ) {
      pathMockdata = "/home/jenkins/workspace/${env.JOB_NAME}/pipeline/dockerfiles/mountebank"
    } else{
      sh "cp /home/jenkins/workspace/${env.JOB_NAME}/pipeline/dockerfiles/mountebank/Dockerfile ${pathMockdata}"
    }
  }
  return pathMockdata;
} // End method prepare file mountebank startup 
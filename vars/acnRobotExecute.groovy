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
    
    def appName = config.APPNAME
    def appVersion = config.VERSION

    sh "echo appName"
    // sh "docker images |grep ascendcorphub/robot"
    
} // End Function

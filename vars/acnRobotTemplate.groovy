#!/usr/bin/groovy
import io.fabric8.Fabric8Commands
import io.fabric8.Utils

def call(Map parameters = [:], body) {
    def flow = new Fabric8Commands()
    def utils = new Utils()

    def defaultLabel = buildId('robot')
    def label = parameters.get('label', defaultLabel)

    def robotImage = parameters.get('robotImage', 'ascendcorphub/robot:v1.0.0')
    def jnlpImage = (flow.isOpenShift()) ? 'fabric8/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'
    def inheritFrom = parameters.get('inheritFrom', 'base')

    def cloud = flow.getCloudConfig()

    // 0.13 introduces a breaking change when defining pod env vars so check version before creating build pod
    echo "building using the docker socket"

    podTemplate(cloud: cloud,
            label: label,
            inheritFrom: "${inheritFrom}",
            containers: [
                    containerTemplate (
                            //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}'],
                            name: 'robot',
                            image: "${robotImage}",
                            command: '/bin/sh -c',
                            args: 'cat',
                            ttyEnabled: true,
                            alwaysPullImage: false,
                            workingDir: '/home/jenkins/',
                            //resourceLimitMemory: '640Mi',
                            envVars: [
                                    //envVar(key: '_JAVA_OPTIONS', value: '-Duser.home=/root/ -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dsun.zip.disableMemoryMapping=true -XX:+UseParallelGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xms10m -Xmx192m'),
                                    envVar(key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/')])],
            volumes: [
                    secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                    secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                    secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                    secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                    secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git'),
                    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                    persistentVolumeClaim(mountPath: '/root/data/pvc', claimName: 'nfs-robot', readOnly: false),
                    nfsVolume(mountPath: '/home/jenkins/workspace', serverAddress: '35.200.192.59', serverPath: '/var/nfsshare', readOnly: false),
                    hostPathVolume(hostPath: '/home/jenkins/workspace', mountPath: '/home/jenkins/workspace')])
            {

                body(

                )
            }
}

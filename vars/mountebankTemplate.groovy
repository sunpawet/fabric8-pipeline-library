#!/usr/bin/groovy
import io.fabric8.Fabric8Commands
import io.fabric8.Utils

def call(Map parameters = [:], body) {
    def flow = new Fabric8Commands()
    def utils = new Utils()

    def defaultLabel = buildId('mountebank')
    def label = parameters.get('label', defaultLabel)

    def mountebankImage = parameters.get('mountebankImage', 'ascendcorphub/mountebank:v1.0.1')
    def jnlpImage = (flow.isOpenShift()) ? 'fabric8/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'
    def inheritFrom = parameters.get('inheritFrom', 'base')

    def cloud = flow.getCloudConfig()

    // 0.13 introduces a breaking change when defining pod env vars so check version before creating build pod
    if (utils.isKubernetesPluginVersion013()) {
        echo "Kubernetes Plugin Version 013"
        if (utils.isUseOpenShiftS2IForBuilds()) {
            podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}", serviceAccount: 'jenkins',
                    containers: [
                            containerTemplate(
                                    name: 'jnlp',
                                    image: "${jnlpImage}",
                                    args: '${computer.jnlpmac} ${computer.name}',
                                    workingDir: '/home/jenkins/',
                                    resourceLimitMemory: '512Mi'), // needs to be high to work on OSO
                            containerTemplate(
                                    name: 'mountebank',
                                    image: "${mountebankImage}",
                                    command: '/bin/sh -c',
                                    args: 'cat',
                                    ttyEnabled: true,
                                    workingDir: '/home/jenkins/',
                                    resourceLimitMemory: '1024Mi')],
                    volumes: [
                            secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                            secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                            secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                            secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git')]) {

                body(

                )
            }
        } else {
            echo "building using the docker socket"

            podTemplate(cloud: cloud,
                    label: label,
                    inheritFrom: "${inheritFrom}",
                    containers: [
                            containerTemplate (
                                    //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}'],
                                    name: 'mountebank',
                                    image: "${mountebankImage}",
                                    command: '/bin/sh -c',
                                    args: 'cat',
                                    ttyEnabled: true,
                                    alwaysPullImage: false,
                                    workingDir: '/home/jenkins/')],
                    volumes: [
                            secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                            secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                            secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                            secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                            secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git'),
                            hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')])
                    {

                        body(

                        )
                    }
        }
    } else {
        if (utils.isUseOpenShiftS2IForBuilds()) {
            podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}", serviceAccount: 'jenkins', restartPolicy: 'OnFailure',
                    containers: [
                            [name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}', workingDir: '/home/jenkins/',
                             resourceLimitMemory: '512Mi'], // needs to be high to work on OSO
                            [name: 'mountebank', image: "${mountebankImage}", command: '/bin/sh -c', args: 'cat', ttyEnabled: true, workingDir: '/home/jenkins/',
                             resourceLimitMemory: '1024Mi']],
                    volumes: [secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                              secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                              secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                              secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git')],
                    envVars: [[key: 'GIT_COMMITTER_EMAIL', value: 'fabric8@googlegroups.com'], [key: 'GIT_COMMITTER_NAME', value: 'fabric8']]) {

                body(

                )
            }
        } else {
            echo "building using the docker socket"

            podTemplate(
                cloud: cloud, 
                label: label, 
                inheritFrom: "${inheritFrom}",
                containers: [
                    [
                        name: 'mountebank', 
                        image: "${mountebankImage}", 
                        command: '/bin/bash -c', 
                        args: 'cat', 
                        ttyEnabled: true
                    ]
                ],
                volumes: [
                    secretVolume(secretName: 'jenkins-docker-cfg', mountPath: '/home/jenkins/.docker'),
                    secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg'),
                    secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                    secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                    secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git'),
                    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
                ],
                envVars: [
                    [key: 'DOCKER_HOST', value: 'unix:/var/run/docker.sock'], [key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/']
                ]
            ) {

                body(

                )
            }
        }
    }
}

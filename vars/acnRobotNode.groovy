#!/usr/bin/groovy

def call(Map parameters = [:], body) {

    def defaultLabel = buildId('robot')
    def label = parameters.get('label', defaultLabel)

    acnRobotTemplate(parameters) {
        node(label) {
            body()
        }
    }
}

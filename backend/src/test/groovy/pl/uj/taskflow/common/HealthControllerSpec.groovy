package pl.uj.taskflow.common

import spock.lang.Specification

class HealthControllerSpec extends Specification {

    def "health endpoint reports service as up"() {
        given:
        def controller = new HealthController()

        when:
        def response = controller.health()

        then:
        response.status == "UP"
        response.service == "taskflow-backend"
        response.timestamp
    }
}


package se.lagrummet.rinfo.main.storage

import spock.lang.*
import static se.lagrummet.rinfo.main.storage.ErrorLevel.*

class ErrorLevelSpec extends Specification {

    @Unroll
    def "should stop on error level #stopOnLevel"() {
        expect:
        continueOnLevelMap.each { gotErrorLevel, shouldContinue ->
            assert (gotErrorLevel < stopOnLevel) == shouldContinue
        }
        where:
        stopOnLevel | continueOnLevelMap
        NONE        | [(WARNING): true, (ERROR): true, (EXCEPTION): true]
        WARNING     | [(WARNING): false, (ERROR): false, (EXCEPTION): false]
        ERROR       | [(WARNING): true, (ERROR): false, (EXCEPTION): false]
        EXCEPTION   | [(WARNING): true, (ERROR): true, (EXCEPTION): false]

    }
}

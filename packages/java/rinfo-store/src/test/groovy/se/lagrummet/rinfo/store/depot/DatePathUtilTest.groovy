package se.lagrummet.rinfo.store.depot

import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class DatePathUtilTest {

    def "turns dates into entry history paths"() {
        when:
        def date = new Date(1234567890123)
        then:
        DatePathUtil.toEntryHistoryPath(date) == "2009/02-13/T23_31_30Z"
    }

    def "turns dates into feed archive paths"() {
        when:
        def date = new Date(1234567890123)
        then:
        DatePathUtil.toFeedArchivePath(date) == "2009/02-13/T23_31/30.123Z"
    }

    def "matches valid date paths"() {
        setup:
        def monthDay = { it.matches(DatePathUtil.DAY_NAME_REGEX) }
        def timeSec = { it.matches(DatePathUtil.SEC_NAME_REGEX) }
        expect:
        monthDay("01-01")
        monthDay("12-01")
        monthDay("12-31")
        !monthDay("01-32")
        !monthDay("12-00")
        !monthDay("13-30")
        and:
        timeSec("T01_01_01Z")
        timeSec("T13_28_37Z")
        !timeSec("T01_01_a1Z")
    }

}

package se.lagrummet.rinfo.main

import org.junit.runner.RunWith; import spock.lang.*

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.ConfigurationException

import se.lagrummet.rinfo.main.storage.FeedCollectScheduler
import se.lagrummet.rinfo.main.storage.Storage


@Speck @RunWith(Sputnik)
class ComponentsSpeck {

    def "should fail on bad config"() {
        when:
        def components = new Components(new PropertiesConfiguration())
        then:
        thrown(ConfigurationException)
    }

    def "should be creatable from config"() {
        when:
        def components = new Components(
                new PropertiesConfiguration("rinfo-main.properties"))
        then:
        components.storage instanceof Storage
        components.collectScheduler instanceof FeedCollectScheduler
    }

}

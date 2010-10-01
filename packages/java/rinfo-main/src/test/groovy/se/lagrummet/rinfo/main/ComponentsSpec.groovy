package se.lagrummet.rinfo.main

import spock.lang.*

import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.DefaultConfigurationBuilder
import org.apache.commons.configuration.PropertiesConfiguration

import se.lagrummet.rinfo.main.storage.FeedCollectScheduler
import se.lagrummet.rinfo.main.storage.Storage


class ComponentsSpec extends Specification {

    def "should fail on bad config"() {
        when:
        def components = new Components(new PropertiesConfiguration())
        then:
        thrown(ConfigurationException)
    }

    def "should be creatable from config"() {
        when:
        def configBuilder = new DefaultConfigurationBuilder("config.xml")
        def components = new Components(configBuilder.getConfiguration())
        then:
        components.storage instanceof Storage
        components.collectScheduler instanceof FeedCollectScheduler
    }

}

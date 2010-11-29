package se.lagrummet.rinfo.store.depot


import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.model.Feed

import spock.lang.*


class AtomizerSpec extends Specification {

    def atomizer = new Atomizer()

    def "should use feed skeleton resource"() {
        when: "using file location"
        String path = "src/test/resources/test_feed_skeleton.atom"
        atomizer.setFeedSkeletonPath(path)
        then:
        atomizer.feedSkeletonPath == path
        atomizer.feedSkeleton.title == "Test Feed"

        when: "using (class) resource location"
        atomizer.setFeedSkeletonPath("test_feed_skeleton.atom")
        then:
        atomizer.feedSkeleton.title == "Test Feed"

        when: "set Feed directly"
        def feed = Abdera.getInstance().newFeed()
        atomizer.setFeedSkeleton(feed)
        then:
        atomizer.feedSkeletonPath == null
        atomizer.feedSkeleton == feed
    }

    @Unroll("""get and set checksums where
 writeLegacy: #writeLegacy, readLegacy: #readLegacy""")
    def "should get and set checksums"() {
        given:
        def entry = Abdera.instance.newEntry()
        entry.setContent(new IRI("http://localhost/text/1"), "text/plain")
        def elem = entry.contentElement
        def md5sum = "d41d8cd98f00b204e9800998ecf8427e"

        expect:
        atomizer.getChecksums(elem).size() == 0

        when:
        atomizer.writeLegacyMd5LinkExtension = writeLegacy
        atomizer.readLegacyMd5LinkExtension = readLegacy
        atomizer.setChecksum(elem, "md5", md5sum)
        then:
        def checksums = atomizer.getChecksums(elem)
        checksums.size() == 1
        checksums["md5"] == md5sum
        elem.getAttributeValue(Atomizer.LINK_EXT_MD5) == (writeLegacy? md5sum : null)

        where:
        writeLegacy | readLegacy
        false       | false
        true        | false
        false       | true
        true        | true
    }

}

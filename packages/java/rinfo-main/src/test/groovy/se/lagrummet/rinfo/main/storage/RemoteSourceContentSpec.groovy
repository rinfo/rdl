package se.lagrummet.rinfo.main.storage

import spock.lang.*

class RemoteSourceContentSpec extends Specification {

    def "should handle missing pdf"() {
        setup:
        def urlPath = "http://somedomain.se/some_missing_pdf.pdf"
        def mediaType = "application/pdf"
        FeedCollectorSession feedCollectorSession = Mock()
        feedCollectorSession.getResponseAsInputStream(urlPath) >> {
            throw new FileNotFoundException("pdf missing")
        }
        OutputStream outputStream = Mock()
        def remoteSourceContent = new RemoteSourceContent(feedCollectorSession, urlPath, mediaType, null, null)

        when:
        remoteSourceContent.writeTo(outputStream)

        then:
        notThrown(IOException) // since caught by writeTo()
    }

}
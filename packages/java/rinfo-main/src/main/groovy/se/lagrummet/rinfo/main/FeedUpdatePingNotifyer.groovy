package se.lagrummet.rinfo.main

import org.restlet.Client
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.data.Request


class FeedUpdatePingNotifyer implements Runnable {

    URL feedUrl
    Collection pingTargets

    FeedUpdatePingNotifyer(feedUrl, pingTargets) {
        this.feedUrl = feedUrl
        this.pingTargets = pingTargets
    }

    public void run() {
        for (pingTarget in pingTargets) {
            doPing(pingTarget, feedUrl)
        }
    }

    void doPing(URL pingTarget, URL feedUrl) {
        def request = new Request(Method.POST, pingTarget.toString())
        def feedUrlMsg = "feed=${feedUrl}"
        // TODO: pubsubhubbub:
        // "hub.mode=publish&hub.url=${feedUrl}", "application/x-www-form-urlencoded"
        request.setEntity(feedUrlMsg, MediaType.MULTIPART_FORM_DATA)
        //request.setReferrerRef(...)
        def client = new Client(Protocol.HTTP)
        def response = client.handle(request)
        println response.status
        println response.entity.text
    }

}

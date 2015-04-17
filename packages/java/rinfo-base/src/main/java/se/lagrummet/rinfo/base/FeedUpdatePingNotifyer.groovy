package se.lagrummet.rinfo.base

import org.restlet.Client
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Protocol
import org.restlet.Request


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
        // TODO: pubsubhubbub:
        // "hub.mode=publish&hub.url=${feedUrl}", "application/x-www-form-urlencoded"
        def feedUrlMsg = "feed=${feedUrl}"
        def request = new Request(Method.POST, pingTarget.toString())
        // TODO:? rewrite using HttpClient (just as FeedCollector) instead?
        def client = new Client(Protocol.HTTP)
        try {
            try {
                request.setEntity(feedUrlMsg, MediaType.MULTIPART_FORM_DATA)
                //request.setReferrerRef(...)
                def response = client.handle(request)
                try {
                    println response.status
                    println response.entity?.text
                } finally {
                    response.release()
                }
            } finally {
                request.release()
            }
        } finally {
            client.stop()
        }
    }

}

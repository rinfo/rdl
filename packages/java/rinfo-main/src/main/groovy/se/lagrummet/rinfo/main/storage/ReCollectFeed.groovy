package se.lagrummet.rinfo.main.storage
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.restlet.Request
import org.restlet.Response
import org.restlet.Restlet
import org.restlet.data.MediaType

class ReCollectFeed {
    static Feed generate(reCollectQueue) {

        def feed = Abdera.instance.newFeed()

        feed.setId("tag:lagrummet.se,2014:rinfo:recollect")
        feed.setTitle("rinfo recollect")
        feed.setUpdated(new Date())

        reCollectQueue.each {
            item -> feed.addEntry(item.contentEntry)
        }
        return feed
    }

    static Restlet createFeed(context) {
        return new Restlet(context) {
            @Override
            public void handle(Request request, Response response) {
                def feed = generate(ReCollectQueue.instance.asList)
                response.setEntity(feed.toString(), MediaType.APPLICATION_ATOM);
            }
        };
    }
}

package se.lagrummet.rinfo.main.storage

public class CollectorSource {

    private URI feedId;
    private URL currentFeed;

    public CollectorSource(feedId, currentFeed) {
        this.feedId = feedId;
        this.currentFeed = currentFeed;
    }

    public URI getFeedId() { return feedId; }

    public URL getCurrentFeed() { return currentFeed; }

    public String toString() {
        return "CollectorSource(feedId: <${feedId}>, currentFeed: <${currentFeed}>)";
    }

}

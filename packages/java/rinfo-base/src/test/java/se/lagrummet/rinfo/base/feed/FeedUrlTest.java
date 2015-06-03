package se.lagrummet.rinfo.base.feed;

import org.junit.Assert;
import org.junit.Test;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by christian on 5/25/15.
 */
public class FeedUrlTest {

    @Test
    public void parseBaseUrl() throws MalformedURLException {
        URL base = new URL("http://base.se/sub/index.atom");
        System.out.println(base.getAuthority());
        System.out.println(base.getFile());
        System.out.println(base.getHost());
        System.out.println(base.getPath());
        System.out.println(base.getProtocol());
        System.out.println(base.getQuery());
        System.out.println(base.getRef());
        System.out.println(base.getUserInfo());
        Assert.assertEquals("http://base.se/sub/home.atom", FeedUrl.parse(base, "home.atom").toString());
        Assert.assertEquals("http://base.se/home.atom", FeedUrl.parse(base, "/home.atom").toString());
        Assert.assertEquals("http://new.os/home.atom", FeedUrl.parse(base, "http://new.os/home.atom").toString());
    }
}

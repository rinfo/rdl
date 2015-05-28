package se.lagrummet.rinfo.base.feed;

import org.junit.Assert;
import org.junit.Test;
import se.lagrummet.rinfo.base.feed.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by christian on 5/25/15.
 */
public class UtilsTest {

    @Test
    public void dateParserTest() throws ParseException {
        Assert.assertEquals(1432683034000l, Utils.parseXMLDateTime("2015-05-27T01:30:34").getTime());
        Assert.assertEquals(1396422738000l, Utils.parseXMLDateTime("2014-04-02T09:12:18Z").getTime());
        Assert.assertEquals(1399014678000l, Utils.parseXMLDateTime("2014-05-02T09:11:18Z").getTime());
        Assert.assertEquals(1395415658000l, Utils.parseXMLDateTime("2014-03-21T16:27:38.693943000Z").getTime());
        Assert.assertEquals(1395414209000l, Utils.parseXMLDateTime("2014-03-21T16:03:29.290000Z").getTime());

    }

    @Test
    public void urlCalc() throws MalformedURLException {
        Assert.assertEquals("http://home.se", Utils.calculateUrlBase(new URL("http://home.se/more/index.atom")));
        Assert.assertEquals("http://home.se", Utils.calculateUrlBase(new URL("http://home.se/index.atom")));
        Assert.assertEquals("http://home.se", Utils.calculateUrlBase(new URL("http://home.se")));
        Assert.assertEquals("http://home.se", Utils.calculateUrlBase(new URL("http://home.se?home=10")));

        Assert.assertEquals("http://home.se/more/", Utils.calculateUrlRel(new URL("http://home.se/more/index.atom")));
        Assert.assertEquals("http://home.se/", Utils.calculateUrlRel(new URL("http://home.se/index.atom")));
        Assert.assertEquals("http://home.se/", Utils.calculateUrlRel(new URL("http://home.se")));
        Assert.assertEquals("http://home.se/mucka/", Utils.calculateUrlRel(new URL("http://home.se/mucka/mera?home=10")));

    }

    @Test
    public void replaceParamsInText() {
        Assert.assertEquals("Heluu vänligen på dig Gurra", Utils.replaceParamsInText("Heluu %1 på dig %2", new String[] {"vänligen", "Gurra"}));
        Assert.assertEquals("Heluu orc på dig '' before orc", Utils.replaceParamsInText("Heluu %1 på dig '%2' before %1", new String[] {"orc", ""}));
    }
}

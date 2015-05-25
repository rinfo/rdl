package se.lagrummet.rinfo.base.feed.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by christian on 5/25/15.
 */
public class Utils {
    private static SimpleDateFormat[] acceptedFormats = {new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX")
    };

    public static Date parseXMLDateTime(String value) throws ParseException {
        for (SimpleDateFormat df : acceptedFormats) {
            try {
                return df.parse(value);
            } catch (ParseException ignore) {}
        }
        throw new ParseException("Unable to parse '"+value+"'", 0);
    }

    private static StringBuilder calculateUrl(URL url) {
        StringBuilder sb = new StringBuilder(url.getProtocol()+"://");
        sb.append(url.getHost());
        if (url.getPort()!=-1 && url.getPort()!=url.getDefaultPort())
            sb.append(":"+url.getPort());
        return sb;
    }


    public static String calculateUrlBase(URL url) {
        StringBuilder sb = calculateUrl(url);
        return sb.toString();
    }

    public static String calculateUrlRel(URL url) {
        StringBuilder sb = calculateUrl(url);
        if (url.getPath()!=null && !url.getPath().equals("")) {
            int indexOfLastSlash = url.getPath().lastIndexOf("/");
            if (indexOfLastSlash>0) {
                sb.append(url.getPath().substring(0,indexOfLastSlash));
            }
        }
        sb.append("/");
        return sb.toString();
    }

    public static URL parse(URL base, String feedUrl) throws MalformedURLException {
        if (feedUrl.startsWith("http://") || feedUrl.startsWith("https://"))
            return new URL(feedUrl);
        if (feedUrl.startsWith("/"))
            return new URL(Utils.calculateUrlBase(base)+feedUrl);
        return new URL(Utils.calculateUrlRel(base)+feedUrl);
    }

}

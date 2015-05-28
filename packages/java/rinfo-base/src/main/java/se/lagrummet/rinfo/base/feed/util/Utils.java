package se.lagrummet.rinfo.base.feed.util;

import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by christian on 5/25/15.
 */
public class Utils {
    private static SimpleDateFormat[] acceptedFormats = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
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
        if (value==null)
            return null;
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

    public static long copyStream(InputStream in, OutputStream out, Md5Sum.Md5SumCalculator md5SumCalculator) throws IOException {
        long totalCopied = 0;
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
            md5SumCalculator.update(buffer, 0, len);
            totalCopied += len;
        }
        return totalCopied;
    }

    public static String extractFileName(String url) {
        int indexOfLastSlash = url.lastIndexOf("/");
        if (indexOfLastSlash==-1)
            return url;
        return url.substring(indexOfLastSlash);
    }


    public static String replaceParamsInText(String text, Object[] params) {
        for (int i = 0; i < params.length; i++) {
            text = text.replaceAll("%"+(i+1), params[i] != null ? params[i].toString() : "");
        }
        return text;
    }
}

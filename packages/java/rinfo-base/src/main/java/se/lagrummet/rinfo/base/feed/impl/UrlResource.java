package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.exceptions.ResourceWriteException;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

/**
 * Created by christian on 5/25/15.
 */
public class UrlResource implements ResourceLocator.Resource {

    private Integer size;
    private String url;

    public Integer getSize() {return size;}
    public String getUrl() {return url;}

    public UrlResource(String url) {
        this(url, null);
    }

    public UrlResource(String url, Integer size) {
        this.size = size;
        this.url = url;
    }

    @Override
    public Integer size() {return size;}

    @Override
    public Md5Sum writeTo(OutputStream outputStream, URL baseUrl) throws ResourceWriteException {
        try {
            FeedUrl feedUrl = FeedUrl.parse(baseUrl, url);
            System.out.println("se.lagrummet.rinfo.base.feed.impl.UrlResource.writeTo feedUrl="+feedUrl);
            InputStream in = new BufferedInputStream(feedUrl.getUrl().openStream());
            OutputStream out = new BufferedOutputStream(outputStream);
            Md5Sum.Md5SumCalculator md5SumCalculator = Md5Sum.calculator();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                md5SumCalculator.update(buffer, 0, len);
            }
            in.close();
            out.close();
            return md5SumCalculator.create();
        } catch (IOException e) {
            throw new ResourceWriteException("IO Failed to write "+url, e);
        } catch (NoSuchAlgorithmException e) {
            throw new ResourceWriteException("No such algorithm Failed to write "+url, e);
        }
    }

    @Override
    public String toString() {
        return "UrlResource{" +
                "size=" + size +
                ", url='" + url + '\'' +
                '}';
    }
}

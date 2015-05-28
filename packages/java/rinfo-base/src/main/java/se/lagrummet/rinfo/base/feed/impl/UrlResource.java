package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Report;
import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.exceptions.MD5SumVerificationFailedException;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

/**
 * Created by christian on 5/25/15.
 */
public class UrlResource implements ResourceLocator.Resource {

    public static UrlResource startFeed(String url) {
        return new UrlResource(Report.Group.Main, url, null, null);
    }

    public static UrlResource feed(String url) {
        return new UrlResource(Report.Group.Feed, url, null, null);
    }

    public static UrlResource entry(String url, Md5Sum md5Sum) {
        return new UrlResource(Report.Group.Entry, url, md5Sum, null);
    }

    public static UrlResource entry(String url, Md5Sum md5Sum, int size) {
        return new UrlResource(Report.Group.Entry, url, md5Sum, size);
    }

    final private Integer size;
    final private String url;
    final private Md5Sum md5Sum;
    final private Report.Group reportGroup;

    public Integer getSize() {return size;}
    public String getUrl() {return url;}

    private UrlResource(Report.Group reportGroup, String url, Md5Sum md5Sum, Integer size) {
        this.reportGroup = reportGroup;
        this.md5Sum = md5Sum;
        this.size = size;
        this.url = url;
    }

    @Override
    public Integer size() {return size;}

    @Override
    public void configure(ResourceLocator.ResourceWriter resourceWriter) {
        resourceWriter.setUrl(url);
        if (size!=null)
            resourceWriter.setSize(size.intValue());
    }

    @Override
    public void start(Report report) {
        report.getReportItem(reportGroup, url).start();
    }

    @Override
    public void end(Report report) {
        report.getReportItem(reportGroup, url).end();
    }

    @Override
    public void intermediate(Report report, String info, Object... params) {
        report.getReportItem(reportGroup, url).intermediate(info, params);
    }

    @Override
    public void verifyMd5Sum(Md5Sum md5Sum) throws MD5SumVerificationFailedException {
        if (this.md5Sum==null)
            return;
        if (this.md5Sum.equals(md5Sum))
            return;
        throw new MD5SumVerificationFailedException(getUrl(), this.md5Sum, md5Sum);
    }

    @Override
    public String toString() {
        return "UrlResource{" +
                "size=" + size +
                ", url='" + url + '\'' +
                '}';
    }
}

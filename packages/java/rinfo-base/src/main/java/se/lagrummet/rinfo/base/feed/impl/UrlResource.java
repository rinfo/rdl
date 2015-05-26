package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Report;
import se.lagrummet.rinfo.base.feed.ResourceLocator;

/**
 * Created by christian on 5/25/15.
 */
public class UrlResource implements ResourceLocator.Resource {

    public static UrlResource startFeed(String url) {
        return new UrlResource(Report.Group.Main, url, null);
    }

    public static UrlResource feed(String url) {
        return new UrlResource(Report.Group.Feed, url, null);
    }

    public static UrlResource entry(String url) {
        return new UrlResource(Report.Group.Entry, url, null);
    }

    public static UrlResource entry(String url, int size) {
        return new UrlResource(Report.Group.Entry, url, size);
    }

    private Integer size;
    private String url;
    private Report.Group reportGroup = Report.Group.Unspecified;

    public Integer getSize() {return size;}
    public String getUrl() {return url;}

    private UrlResource(Report.Group reportGroup, String url, Integer size) {
        this.reportGroup = reportGroup;
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
    public void intermediate(Report report, String info) {
        report.getReportItem(reportGroup, url).intermediate(info);
    }

    @Override
    public String toString() {
        return "UrlResource{" +
                "size=" + size +
                ", url='" + url + '\'' +
                '}';
    }
}

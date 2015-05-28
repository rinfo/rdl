package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.type.DocumentUrl;

/**
 * Created by christian on 2015-05-26.
 */
public interface Report {


    enum Group {Unspecified, Feed, Entry, Main, Content}

    ReportItem getReportItem(String id);
    ReportItem getReportItem(Group group, String id);

    void print();
    void printStatusOneLiner();

    interface ReportItem {
        String getId();
        Group getGroup();
        void start();
        void end();
        void intermediate(String info, Object... params);
        void warning(String warningText, Object... params);
    }

    interface Reporter {
        void start(Report report);
        void end(Report report);
        void intermediate(Report report, String info, Object... params);
    }
}

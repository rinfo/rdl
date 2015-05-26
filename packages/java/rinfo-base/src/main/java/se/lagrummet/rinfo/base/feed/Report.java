package se.lagrummet.rinfo.base.feed;

/**
 * Created by christian on 2015-05-26.
 */
public interface Report {

    enum Group {Unspecified, Feed, Entry, Main, Content}

    ReportItem getReportItem(String id);
    ReportItem getReportItem(Group group, String id);

    public void print();

    interface ReportItem {
        String getId();
        Group getGroup();
        void start();
        void end();
        void intermediate(String info);
    }

    interface Reporter {
        void start(Report report);
        void end(Report report);
        void intermediate(Report report, String info);
    }
}

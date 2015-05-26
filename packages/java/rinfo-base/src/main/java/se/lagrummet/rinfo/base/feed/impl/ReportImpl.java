package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Report;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
* Created by christian on 5/21/15.
*/
public class ReportImpl implements Report {

    Map<ReportItemKey, MyReportItem> items = new HashMap<>();

    @Override
    public synchronized Report.ReportItem getReportItem(String id) {
        return getReportItem(Report.Group.Unspecified, id);
    }

    @Override
    public ReportItem getReportItem(Group group, String id) {
        ReportItemKey key = new ReportItemKey(group, id);
        MyReportItem reportItem = items.get(key);
        if (reportItem==null) {
            reportItem = new MyReportItem(key);
            items.put(key, reportItem);
        }
        return reportItem;
    }

    private void info(Report.ReportItem reportItem, String info) {
        System.out.println(reportItem.getId()+"["+reportItem.getGroup()+"]"+" "+info);
    }

    private Iterable<MyReportItem> itemsByGroup(Group group) {
        List<MyReportItem> res = new LinkedList<>();
        for (MyReportItem item : items.values()) {
            if (item.sameGroup(group)) {
                res.add(item);
            }
        }
        return res;
    }

    private long count(Group group) {
        long sum = 0;
        for (MyReportItem item : itemsByGroup(group))
            sum++;
        return sum;
    }

    private long sum(Group group) {
        long sum = 0;
        for (MyReportItem item : itemsByGroup(group))
            if (item.duration()!=null)
                sum += item.duration();
        return sum;
    }

    private double avg(Group group) {
        long sum = 0;
        int count = 0;
        for (MyReportItem item : itemsByGroup(group)) {
            if (item.duration()!=null) {
                sum += item.duration();
                count++;
            }
        }
        if (count==0)
            return 0;
        return sum / count;
    }

    public void print() {
        System.out.println("************ Report of downloaded feed *******************");
        System.out.println("total items: "+items.size());
        System.out.println("Total download time "+sum(Group.Main)+" msec");
        System.out.println("Feeds: ");
        System.out.println("  Total count: "+count(Group.Feed));
        System.out.println("  Total download time: "+sum(Group.Feed)+" msec");
        System.out.println("  Average download time: "+avg(Group.Feed)+" msec");
        System.out.println("Content: ");
        System.out.println("  Total count: "+count(Group.Entry));
        System.out.println("  Total download time: "+sum(Group.Entry)+" msec");
        System.out.println("  Average download time: "+avg(Group.Entry)+" msec");
    }

    private class MyReportItem implements Report.ReportItem {
        private Long start;
        private Long end;
        private ReportItemKey key;

        @Override public String getId() {return key.getId();}
        @Override public Group getGroup() {return key.getGroup();}

        public MyReportItem(ReportItemKey key) {
            this.key = key;
        }

        @Override
        public void start() {
            start = System.currentTimeMillis();
            //info(this, "start");
        }

        @Override
        public void end() {
            end = System.currentTimeMillis();
            info(this, "end");
        }

        @Override
        public void intermediate(String info) {
            //info(this, info);
        }

        public Long duration() {
            if (start==null||end==null)
                return null;
            return end - start;
        }

        public boolean sameGroup(Group group) {
            return key.getGroup().equals(group);
        }
    }

    private class ReportItemKey {
        private Group group;
        private String id;

        public Group getGroup() {return group;}
        public String getId() {return id;}

        private ReportItemKey(Group group, String id) {
            this.group = group;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReportItemKey that = (ReportItemKey) o;

            if (group != that.group) return false;
            if (id != null ? !id.equals(that.id) : that.id != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = group != null ? group.hashCode() : 0;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }
    }


}

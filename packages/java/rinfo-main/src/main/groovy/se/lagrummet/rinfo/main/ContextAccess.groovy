package se.lagrummet.rinfo.main

import org.restlet.Context
import se.lagrummet.rinfo.main.storage.FeedCollectScheduler
import se.lagrummet.rinfo.main.storage.CollectorLog

public class ContextAccess {

    public static final String COLLECT_SCHEDULER = "rinfo.main.restlet.context.collectScheduler";
    public static final String COLLECTOR_LOG = "rinfo.main.restlet.context.collectorLog";

    public static FeedCollectScheduler getCollectScheduler(Context context) {
        return (FeedCollectScheduler) context.getAttributes().get(COLLECT_SCHEDULER);
    }
    public static void setCollectScheduler(Context context, FeedCollectScheduler obj) {
        context.getAttributes().putIfAbsent(COLLECT_SCHEDULER, obj);
    }

    public static CollectorLog getCollectorLog(Context context) {
        return (CollectorLog) context.getAttributes().get(COLLECTOR_LOG);
    }
    public static void setCollectorLog(Context context, CollectorLog obj) {
        context.getAttributes().putIfAbsent(COLLECTOR_LOG, obj);
    }

}

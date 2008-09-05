package se.lagrummet.rinfo.collector

import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import se.lagrummet.rinfo.base.URIMinter
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.collector.FeedCollector


class CollectorRunner {

    FileDepot depot
    private URIMinter uriMinter // TODO: auto-config via collector?
    static final int DEFAULT_POOL_SIZE = 100
    static final int DEFAULT_INITIAL_DELAY = 0
    static final int DEFAULT_SCHEDULE_SECONDS = 4
    private ScheduledExecutorService execPool

    CollectorRunner(FileDepot depot, URIMinter uriMinter) {
        this.depot = depot
        this.uriMinter = uriMinter
    }

    void startup() {
        // TODO: Needs source feed urls!
        execPool = Executors.newScheduledThreadPool(DEFAULT_POOL_SIZE)
        execPool.scheduleAtFixedRate(
            { println("TEST: dummy task") }, DEFAULT_INITIAL_DELAY,
                    DEFAULT_SCHEDULE_SECONDS, TimeUnit.SECONDS)
    }

    void shutdown() {
        execPool.shutdown()
    }

    private void collectFeeds(Collection<URL> feedUrls) {
        def executor = Executors.newSingleThreadExecutor()
        executor.execute({
                for (URL feedUrl : feedUrls) {
                    FeedCollector.readFeed(depot, uriMinter, feedUrl)
                }
            });
        executor.shutdown()
    }

}

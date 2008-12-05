package se.lagrummet.rinfo.service

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException

import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class SesameLoadScheduler extends AbstractCollectScheduler {

    private Repository repo
    private Collection<URL> sourceFeedUrls

    SesameLoadScheduler(AbstractConfiguration config) {
        this.configure(config)
    }

    void configure(AbstractConfiguration config) {
        setInitialDelay(-1) // TODO:? never schedule running collects?
        def repoPath = config.getString("rinfo.service.sesameRepoPath")
        def remoteRepoName = config.getString("rinfo.service.sesameRemoteRepoName")
        configureRepo(repoPath, remoteRepoName)
        sourceFeedUrls = new ArrayList<URL>()
        for (String url : config.getList("rinfo.service.sourceFeedUrls")) {
            sourceFeedUrls.add(new URL(url))
        }
    }

    protected void configureRepo(repoPath, remoteRepoName) {
        if (repo != null) {
            // close previous repo if set - to enable reconfiguration
            repo.shutDown()
        }
        if (repoPath =~ /^https?:/) {
            repo = new HTTPRepository(repoPath, remoteRepoName)
        } else {
            def dataDir = new File(repoPath)
            repo = new SailRepository(new NativeStore(dataDir))
        }
        repo = new NotifyingRepositoryWrapper(repo) // enable notifications
        repo.initialize()
    }

    void shutdown() {
        super.shutdown()
        repo.shutDown()
    }

    public Collection getSourceFeedUrls() {
        return sourceFeedUrls
    }

    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def rdfStoreLoader = new SesameLoader(repo)
        rdfStoreLoader.readFeed(feedUrl)
    }

    protected void addRepositoryListener(listener) {
        repo.addRepositoryListener(listener)
    }

    protected void addRepositoryConnectionListener(listener) {
        repo.addRepositoryConnectionListener(listener)
    }

}

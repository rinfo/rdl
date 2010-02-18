package se.lagrummet.rinfo.main.storage

import org.openrdf.query.QueryLanguage
import org.openrdf.repository.Repository

import org.openrdf.elmo.ElmoModule
import org.openrdf.elmo.sesame.SesameManagerFactory

import se.lagrummet.rinfo.main.storage.log.CollectEvent
import se.lagrummet.rinfo.main.storage.log.FeedEvent
import se.lagrummet.rinfo.main.storage.log.EntryEvent
import se.lagrummet.rinfo.main.storage.log.DeletedEntryEvent
import se.lagrummet.rinfo.main.storage.log.ErrorEvent


class CollectorLog {

    private Repository repo

    public static final String DEFAULT_SYSTEM_BASE_URI =
            "http://rinfo.lagrummet.se/system/"
    public static final String DEFAULT_DATASET_URI =
            "tag:lagrummet.se,2009:rinfo"
    // TODO: set via configuration
    String systemBaseUri = DEFAULT_SYSTEM_BASE_URI
    String entryDatasetUri = DEFAULT_DATASET_URI

    private static ElmoModule module = new ElmoModule()
    static {
        module.addConcept(CollectEvent)
        module.addConcept(FeedEvent)
        module.addConcept(EntryEvent)
        module.addConcept(DeletedEntryEvent)
        module.addConcept(ErrorEvent)
    }

    CollectorLog() { }

    CollectorLog(Repository repo) {
        this.repo = repo
        repo.initialize();
    }

    CollectorLogSession openSession() {
        def factory
        if (repo != null) {
            factory = new SesameManagerFactory(module, repo)
        } else {
            factory = new SesameManagerFactory(module)
        }
        factory.setQueryLanguage(QueryLanguage.SPARQL)
        return new CollectorLogSession(this, factory.createElmoManager())
    }

    public void shutdown() {
        if (repo != null) {
            repo.shutDown()
        }
    }

}

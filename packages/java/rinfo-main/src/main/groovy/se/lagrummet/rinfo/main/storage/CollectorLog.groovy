package se.lagrummet.rinfo.main.storage

import org.openrdf.repository.Repository


class CollectorLog {

    private Repository repo

    String systemBaseUri
    String entryDatasetUri

    CollectorLog() { }

    CollectorLog(Repository repo) {
        this.repo = repo
    }

    CollectorLogSession openSession() {
        assert systemBaseUri != null
        assert entryDatasetUri != null
        return new CollectorLogSession(this, repo.getConnection())
    }

    public void shutdown() {
        if (repo != null) {
            repo.shutDown()
        }
    }

}

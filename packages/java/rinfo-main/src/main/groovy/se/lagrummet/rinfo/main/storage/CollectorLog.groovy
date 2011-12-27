package se.lagrummet.rinfo.main.storage

import org.openrdf.repository.Repository


class CollectorLog {

    private Repository repo

    String reportBaseUri
    String systemDatasetUri

    CollectorLog() { }

    CollectorLog(Repository repo, String reportBaseUri, String systemDatasetUri) {
        this.repo = repo
        this.reportBaseUri = reportBaseUri
        this.systemDatasetUri = systemDatasetUri
    }

    CollectorLogSession openSession() {
        assert reportBaseUri != null
        assert systemDatasetUri != null
        return new CollectorLogSession(this, repo.getConnection())
    }

    public void shutdown() {
        if (repo != null) {
            repo.shutDown()
        }
    }

}

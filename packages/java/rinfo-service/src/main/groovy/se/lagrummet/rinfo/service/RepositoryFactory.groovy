package se.lagrummet.rinfo.service

import org.openrdf.repository.Repository
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper
import org.openrdf.sail.nativerdf.NativeStore

import org.apache.commons.configuration.AbstractConfiguration
import org.apache.commons.configuration.ConfigurationException


class RepositoryFactory {

    static Repository createRepository(AbstractConfiguration config) {
        def repoPath = config.getString("rinfo.service.sesameRepoPath")
        def remoteRepoName = config.getString("rinfo.service.sesameRemoteRepoName")
        def repo
        if (repoPath =~ /^https?:/) {
            repo = new HTTPRepository(repoPath, remoteRepoName)
        } else {
            def dataDir = new File(repoPath)
            repo = new SailRepository(new NativeStore(dataDir))
        }
        repo = new NotifyingRepositoryWrapper(repo) // enable notifications
        repo.initialize()
        return repo
    }

}

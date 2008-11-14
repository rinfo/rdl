package se.lagrummet.rinfo.service.util;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.RepositoryListener;

/**
 * Empty RepositoryListener, intended to be convenient for partial 
 * implementation of interface when only a few of the methods are needed.
 *
 */
public class RepoListener implements RepositoryListener {

	public void getConnection(Repository repo, RepositoryConnection conn) {
	}

	public void initialize(Repository repo) {
	}

	public void setDataDir(Repository repo, File dataDir) {
	}

	public void shutDown(Repository repo) {
	}

}

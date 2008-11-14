package se.lagrummet.rinfo.service.util;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.RepositoryConnectionListener;

/**
 * Empty RepositoryConnectionListener, intended to be convenient for partial 
 * implementation of interface when only a few of the methods are needed.
 * 
 */
public class RepoConnectionListener implements RepositoryConnectionListener {

	public void add(RepositoryConnection conn, Resource subject, 
			URI predicate, Value object, Resource... contexts) {	 
	}

	public void clear(RepositoryConnection conn, Resource... contexts) {		 
	}

	public void clearNamespaces(RepositoryConnection conn) {		 
	}

	public void close(RepositoryConnection conn) {
	}

	public void commit(RepositoryConnection conn) {		 
	}

	public void remove(RepositoryConnection conn, Resource subject, 
			URI predicate, Value object, Resource... contexts) {
	}

	public void removeNamespace(RepositoryConnection conn, String prefix) { }

	public void rollback(RepositoryConnection conn) { }

	public void setAutoCommit(RepositoryConnection conn, boolean autoCommit) { }

	public void setNamespace(RepositoryConnection conn, String prefix, String name) { }

}

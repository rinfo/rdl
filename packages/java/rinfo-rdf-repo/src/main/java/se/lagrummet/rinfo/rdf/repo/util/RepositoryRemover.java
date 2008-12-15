package se.lagrummet.rinfo.rdf.repo.util;

import static org.openrdf.query.QueryLanguage.SERQL;

import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.manager.RepositoryManager;

/**
 * This code has been copied and slightly modified from 
 * org.openrdf.workbench.commands.DeleteServlet. The license can be obtained
 * at: http://www.openrdf.org/license.jsp
 * 
 * This implementation is intended to be used instead of the 
 * org.openrdf.repository.manager.RepositoryManager.removeRepositoryConfig
 * method, since this method fails. The method never finds the context for the 
 * repository to delete and therefore cannot remove it from the SYSTEM 
 * repository. The cause seems to be that the RepositoryConnection.getStatements
 * always returns Statements with null context.
 * See forum topic: http://openrdf.org/forum/mvnforum/viewthread?thread=820. 
 * The below implementation taken from the DeleteServlet has luckily a more
 * successful way to find the context.
 * 
 * @author msher
 */
public class RepositoryRemover {

	/**
	 * Query that yields the context of a specific repository configuration.
	 */
	private static final String CONTEXT_QUERY;

	static {
		StringBuilder query = new StringBuilder(256);
		query.append("SELECT C ");
		query.append("FROM CONTEXT C ");
		query.append("   {} rdf:type {sys:Repository};");
		query.append("      sys:repositoryID {ID} ");
		query
				.append("USING NAMESPACE sys = <http://www.openrdf.org/config/repository#>");
		CONTEXT_QUERY = query.toString();
	}

	public static void dropRepository(String id, RepositoryManager manager) throws Exception {
		Repository systemRepo = manager.getSystemRepository();
		RepositoryConnection con = systemRepo.getConnection();
		try {
			Resource context = findContext(id, con);
			manager.getRepository(id).shutDown();
			con.clear(context);
		} finally {
			con.close();
		}
	}

	private static Resource findContext(String id, RepositoryConnection con)
			throws Exception {
		TupleQuery query = con.prepareTupleQuery(SERQL, CONTEXT_QUERY);
		ValueFactory vf = con.getRepository().getValueFactory();
		query.setBinding("ID", vf.createLiteral(id));
		TupleQueryResult result = query.evaluate();
		try {
			if (!result.hasNext())
				throw new Exception("Cannot find repository of id: " + id);
			BindingSet bindings = result.next();
			Resource context = (Resource) bindings.getValue("C");
			if (result.hasNext())
				throw new Exception(
						"Multiple contexts found for repository '" + id + "'");
			return context;
		} finally {
			result.close();
		}
	}
	
}

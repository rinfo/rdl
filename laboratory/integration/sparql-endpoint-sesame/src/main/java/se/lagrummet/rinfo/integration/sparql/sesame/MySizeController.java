package se.lagrummet.rinfo.integration.sparql.sesame;

import info.aduna.webapp.views.SimpleResponseView;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.server.ServerHTTPException;
import org.openrdf.http.server.ProtocolUtil;
import org.openrdf.http.server.repository.RepositoryInterceptor;
import org.openrdf.http.server.repository.size.SizeController;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class MySizeController extends SizeController {
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
		throws Exception
	{
		ProtocolUtil.logRequestParameters(request);

		Repository repository = RepositoryInterceptor.getRepository(request);
		RepositoryConnection repositoryCon = RepositoryInterceptor.getRepositoryConnection(request);

		ValueFactory vf = repository.getValueFactory();
		Resource[] contexts = ProtocolUtil.parseContextParam(request, Protocol.CONTEXT_PARAM_NAME, vf);

		long size = 4711;

//		long size = -1;
//
//		try {
//			size = repositoryCon.size(contexts);
//		}
//		catch (RepositoryException e) {
//			throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
//		}

		Map<String, Object> model = new HashMap<String, Object>();
		model.put(SimpleResponseView.CONTENT_KEY, String.valueOf(size));
		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}
	
}

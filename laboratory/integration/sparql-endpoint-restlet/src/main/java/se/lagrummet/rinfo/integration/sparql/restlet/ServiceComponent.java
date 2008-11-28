package se.lagrummet.rinfo.integration.sparql.restlet;

import org.restlet.Component;
import org.restlet.VirtualHost;
import org.restlet.data.Protocol;

/**
 * ServiceComponent - its sole purpose is to add the file protocol to a client
 * to enable the ServiceApplication to return files.
 * 
 * TODO: Can this be achieved some other way? Current drawbacks:
 * - component must be called before a call to the application (at startup)
 * - an additional level of configuration
 * 
 * Alt. let the ServiceApplication serve a URI to the file, i.e. by setting
 * the identifier in the returned resource and use a separate component with a
 * file application that serves files - although this has basically the same 
 * drawbacks.
 * 
 * @author marbjo
 */
public class ServiceComponent extends Component {

	public ServiceComponent() {
		super();
		
		/*
		 * TODO: get port configuration from properties file. 
		 */
		
        ServiceApplication servApp = new ServiceApplication();
		VirtualHost servHost = new VirtualHost(getContext().createChildContext());        
		servHost.setHostPort("8181");
		servHost.attach(servApp);
        
        getServers().add(Protocol.HTTP, 8181);        
        getClients().add(Protocol.FILE);
        getClients().add(Protocol.HTTP);
        getHosts().add(servHost);		
	}
	
}

package se.lagrummet.se.rinfo.main;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppServletContextListener implements ServletContextListener {

    private final Logger log = LoggerFactory.getLogger(AppServletContextListener.class);

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("ServletContextListener destroyed");
        MultiThreadedHttpConnectionManager.shutdownAll();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("ServletContextListener started");
	}
}
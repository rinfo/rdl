package se.lagrummet.rinfo.integration.sparql.restlet;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class AbstractTestCase extends TestCase {

    private static final String PROPERTIES_FILE_NAME = "test.properties";

//  protected static int serviceAppPort;
    protected static String serviceAppUrl;

	
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
//        serviceAppPort = config.getInt("service.app.port");
//        String appUrlBase = config.getString("service.app.url");
//        serviceAppUrl = appUrlBase + ":" + serviceAppPort;
        serviceAppUrl = config.getString("service.app.url");

    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    

}

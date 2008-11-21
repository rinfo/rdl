package se.lagrummet.rinfo.integration.triplestores;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import se.lagrummet.rinfo.integration.triplestores.util.ReportWriter;

public class AbstractTestCase extends TestCase {

    private static final String PROPERTIES_FILE_NAME = "test.properties";

//  protected static int serviceAppPort;
    protected static String serviceAppUrl;
    protected static String rdfFile;

    protected static ReportWriter reportWriter;

	
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
//        serviceAppPort = config.getInt("service.app.port");
//        String appUrlBase = config.getString("service.app.url");
//        serviceAppUrl = appUrlBase + ":" + serviceAppPort;
        serviceAppUrl = config.getString("service.app.url");
        rdfFile = config.getString("test.rdf.file");

        reportWriter = new ReportWriter();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    

}

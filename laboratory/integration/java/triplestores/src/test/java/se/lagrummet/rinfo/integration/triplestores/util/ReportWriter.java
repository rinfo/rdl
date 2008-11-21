package se.lagrummet.rinfo.integration.triplestores.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class ReportWriter {

    private static final String PROPERTIES_FILE_NAME = "test.properties";

    private static Writer importTestReportWriter;
    private static Writer stressTestReportWriter;

    
    public ReportWriter () throws Exception {

        Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);

        /*
         * TODO: get store, backend
         */
        config.setProperty("triple.store", "sesame");
        config.setProperty("backend", "memory");
        
        String s = config.getString("report.file.import.test");
    	FileWriter fw = new FileWriter(s);
        importTestReportWriter = new BufferedWriter(fw);            	
    }    

    public void close() throws Exception {
        importTestReportWriter.flush();
        importTestReportWriter.close();
    }
    
    public void addExportTestResult(long time) throws Exception {
		importTestReportWriter.write("Export: " + String.valueOf(time) + "\n");
    }

    public void addImportTestResult(long time) throws Exception {
		importTestReportWriter.write("Import: " + String.valueOf(time) + "\n");
    }

    public void addStressTestResult(int clients, long timePerRequest) {    	
    }

}

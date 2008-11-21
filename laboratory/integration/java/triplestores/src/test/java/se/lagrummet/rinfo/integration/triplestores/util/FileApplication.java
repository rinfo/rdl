package se.lagrummet.rinfo.integration.triplestores.util;

import org.restlet.Application;
import org.restlet.Directory;
import org.restlet.Restlet;

public class FileApplication extends Application {

	private static String ROOT_DIRECTORY;

	public FileApplication(String directory) {
		ROOT_DIRECTORY = directory;
	}
	
    public Restlet createRoot() {
        return new Directory(getContext(), ROOT_DIRECTORY);
    }	
	
}

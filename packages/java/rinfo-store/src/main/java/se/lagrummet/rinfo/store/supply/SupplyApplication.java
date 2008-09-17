package se.lagrummet.rinfo.store.supply;

import java.util.*;

import org.apache.commons.configuration.ConfigurationException;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;

import se.lagrummet.rinfo.store.depot.FileDepot;


public class SupplyApplication extends Application {

    private String fileDepotConfig;

    public SupplyApplication(Context context, String fileDepotConfig) {
        this(context);
        this.fileDepotConfig = fileDepotConfig;
    }

    public SupplyApplication(Context context) {
        super(context);
    }

    @Override
    public synchronized Restlet createRoot() {
        try {
            FileDepot fileDepot = (fileDepotConfig!=null)?
                    FileDepot.newConfigured(fileDepotConfig) :
                    FileDepot.newAutoConfigured();
            return new DepotFinder(getContext(), fileDepot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length>0 ? new Integer(args[0]) : 8182;
        String fileDepotConfig = args.length > 1 ? args[1] : null;
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, port);
        Context context = component.getContext().createChildContext();
        component.getDefaultHost().attach(
                new SupplyApplication(context, fileDepotConfig));
        component.start();
    }

}

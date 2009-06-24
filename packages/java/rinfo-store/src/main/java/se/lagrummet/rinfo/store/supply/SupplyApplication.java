package se.lagrummet.rinfo.store.supply;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;

import se.lagrummet.rinfo.store.depot.FileDepot;


public class SupplyApplication extends Application {

    private String fileDepotConfig;
    public static final String DEPOT_CONFIG_PROPERTIES_FILENAME = "rinfo-depot.properties";

    public SupplyApplication(Context context, String fileDepotConfig) {
        super(context);
        this.fileDepotConfig = fileDepotConfig;
    }

    public SupplyApplication(Context context) {
        this(context, DEPOT_CONFIG_PROPERTIES_FILENAME);
    }

    @Override
    public synchronized Restlet createRoot() {
        try {
            FileDepot depot = FileDepot.newConfigured(fileDepotConfig);
            return new DepotFinder(getContext(), depot);
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

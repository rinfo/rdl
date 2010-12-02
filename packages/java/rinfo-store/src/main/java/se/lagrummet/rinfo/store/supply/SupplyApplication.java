package se.lagrummet.rinfo.store.supply;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;

import org.apache.commons.configuration.ConfigurationException;

import org.slf4j.bridge.SLF4JBridgeHandler;
import se.lagrummet.rinfo.store.depot.Depot;
import se.lagrummet.rinfo.store.depot.DepotUtil;
import static se.lagrummet.rinfo.store.depot.DepotUtil.DEFAULT_PROPERTIES_PATH;
import static se.lagrummet.rinfo.store.depot.DepotUtil.DEPOT_CONFIG_SUBSET_KEY;

import java.util.logging.Handler;
import java.util.logging.LogManager;


public class SupplyApplication extends Application {

    private Depot depot;
    public static final int DEFAULT_PORT = 8282;

    public SupplyApplication(Context context) throws ConfigurationException {
        this(context, DepotUtil.depotFromConfig());
    }

    public SupplyApplication(Context context, Depot depot) {
        super(context);
        this.depot = depot;

        System.out.println("Removing any installed JUL handlers pre SLF4J install");
        for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
            System.out.println(" * Removing " + handler.getClass());
            LogManager.getLogManager().getLogger("").removeHandler(handler);
        }
        SLF4JBridgeHandler.install();
    }

    @Override
    public synchronized Restlet createInboundRoot() {
        try {
            return new DepotFinder(getContext(), depot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void serveDepot(Depot depot, int port) throws Exception {
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, port);
        Context context = component.getContext().createChildContext();
        component.getDefaultHost().attach(
                new SupplyApplication(context, depot));
        component.start();
    }

    public static void main(String[] args) throws Exception {
        int port = (args.length > 0)? new Integer(args[0]) : DEFAULT_PORT;
        String propertiesPath = args.length > 1 ? args[1] : DEFAULT_PROPERTIES_PATH;
        Depot depot = DepotUtil.depotFromConfig(propertiesPath, DEPOT_CONFIG_SUBSET_KEY);
        serveDepot(depot, port);
    }

}

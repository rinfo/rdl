package se.lagrummet.rinfo.store.supply

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Protocol

import se.lagrummet.rinfo.store.depot.FileDepot


class SupplyApplication extends Application {

    String fileDepotConfig

    SupplyApplication(Context parentContext, String fileDepotConfig) {
        this(parentContext)
        this.fileDepotConfig = fileDepotConfig
    }

    SupplyApplication(Context parentContext) {
        super(parentContext)
    }

    @Override
    synchronized Restlet createRoot() {
        def fileDepot = (fileDepotConfig)?
                FileDepot.newConfigured(fileDepotConfig) :
                FileDepot.newAutoConfigured()
        def depotFinder = new DepotFinder(context, fileDepot)
        return depotFinder
    }

    static void main(String[] args) {
        int port = args.size() ? new Integer(args[0]) : 8182
        def fileDepotConfig = args.size()>1 ? args[1] : null
        new Component().with {
            servers.add(Protocol.HTTP, port)
            def ctx = context.createChildContext()
            defaultHost.attach(
                    new SupplyApplication(ctx, fileDepotConfig))
            start()
        }
    }

}

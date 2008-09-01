package se.lagrummet.rinfo.store.supply

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Response

import se.lagrummet.rinfo.store.depot.FileDepot


class SupplyApplication extends Application {

    SupplyApplication(Context parentContext) {
        super(parentContext)
        // NOTE: Turn off extensionsTunnel - it removes extensions it can
        // turn into content negotiation metadata. Cool but hard to spot!
        // In at least Restlet 1.1 M3, this is on by default.
        // TODO: should we leave tunnelService enabled at all? If enabled, it
        // will allow e.g. parameter negotiation as well.
        tunnelService.setExtensionsTunnel(false)
    }

    @Override
    synchronized Restlet createRoot() {
        def depotFinder = new DepotFinder(context)
        depotFinder.fileDepot = FileDepot.autoConfigure()
        return depotFinder
    }

    static void main(args) {
        int port = args.size() ? new Integer(args[0]) : 8182
        // TODO: opt. supply path to autoConfigure...
        new Component().with {
            servers.add(Protocol.HTTP, port)
            defaultHost.attach(
                    new SupplyApplication(context.createChildContext()))
            start()
        }
    }

}

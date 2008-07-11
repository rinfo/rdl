package se.lagrummet.rinfo.store.supply

import org.restlet.Application
import org.restlet.Component
import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.Protocol
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.ext.spring.SpringContext

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.core.io.ClassPathResource

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

        def springContext = new SpringContext(context)
        new XmlBeanDefinitionReader(springContext).loadBeanDefinitions(
                new ClassPathResource("applicationContext.xml"))
        springContext.refresh()

        def fileDepot = (FileDepot) springContext.getBean("fileDepot")

        def depotFinder = new DepotFinder(context)
        depotFinder.fileDepot = fileDepot
        return depotFinder
    }

    static void main(args) {
        int port = args.size() ? new Integer(args[0]) : 8182
        new Component().with {
            servers.add(Protocol.HTTP, port)
            defaultHost.attach(
                    new SupplyApplication(context))
            start()
        }
    }

}

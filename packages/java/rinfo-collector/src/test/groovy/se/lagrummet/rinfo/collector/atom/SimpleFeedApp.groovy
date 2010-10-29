package se.lagrummet.rinfo.collector.atom

import org.restlet.Application
import org.restlet.Component
import org.restlet.Restlet
import org.restlet.data.Protocol
import org.restlet.resource.Directory

/**
 * Simple Restlet application that can expose a feed. Used during testing by 
 * the Spock specifications.
 */
class SimpleFeedApp {

    String rootPath
    int port
    Component component

    SimpleFeedApp(rootPath, port=9991) {
        this.rootPath = rootPath
        this.port = port
    }

    def start() {
        final rootDir = new File(rootPath).toURI().toString()
        component = new Component()
        component.servers.add(Protocol.HTTP, port)
        component.clients.add(Protocol.FILE)
        component.defaultHost.attach(new Application() {
            Restlet createRoot() {
                return new Directory(context, rootDir)
            }
        })
        component.start()
    }

    def stop() {
        component.stop()
    }

}

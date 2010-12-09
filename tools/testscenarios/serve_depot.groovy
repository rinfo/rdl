@Grab('se.lagrummet.rinfo:rinfo-store:1.0-SNAPSHOT')
@Grab('org.restlet.jse:org.restlet:2.0.1')
@Grab('org.restlet.jee:org.restlet.ext.servlet:2.0.1')
import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.supply.SupplyApplication

File depotDir
int port
try {
    depotDir = new File(args[0])
    port = args[1] as int
} catch (IndexOutOfBoundsException e) {
    println "Usage: DEPOT_DIR PORT"
    System.exit 0
}

def baseUri = new URI("http://rinfo.lagrummet.se/")
def depot = new FileDepot(baseUri, depotDir)
depot.atomizer.feedPath = "/feed"
SupplyApplication.serveDepot(depot, port)


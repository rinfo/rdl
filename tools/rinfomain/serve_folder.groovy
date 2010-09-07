@Grapes([
  @Grab('org.restlet:org.restlet:1.1.4'),
  @Grab('com.noelios.restlet:com.noelios.restlet:1.1.4')
])
import org.restlet.*
import org.restlet.data.Protocol

/**
 * This script serves the contents of the directory provided as command 
 * line argument 1 via HTTP on the port provided as argument 2. Both 
 * parameters are optional. The default values are:
 * 
 *    - /opt/_workapps/rinfo/testsources/www as the directory to server
 *    - 8280 as the port to serve on
 */

def serveDirectory(port, wwwDir) {
    println("Serving the directory '$wwwDir' on http://localhost:$port")
    
    def cmp = new Component()
    cmp.servers.add(Protocol.HTTP, port)
    cmp.clients.add(Protocol.FILE)
    cmp.defaultHost.attach("", new Application() {
        Restlet createRoot() { new Directory(context, wwwDir as String) }
    })
    cmp.start()
}

def wwwDir = args.length > 0 ? args[0] : "/opt/_workapps/rinfo/testsources/www"
def port = args.length > 1 ? args[1] as Integer : 8280
serveDirectory(port, new File(wwwDir).toURI().toString())


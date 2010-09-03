@Grapes([
  @Grab('org.restlet:org.restlet:1.1.4'),
  @Grab('com.noelios.restlet:com.noelios.restlet:1.1.4')
])
import org.restlet.*
import org.restlet.data.Protocol

def serveDirectory(port, wwwDir) {
    def cmp = new Component()
    cmp.servers.add(Protocol.HTTP, port)
    cmp.clients.add(Protocol.FILE)
    cmp.defaultHost.attach("", new Application() {
        Restlet createRoot() { new Directory(context, wwwDir as String) }
    })
    cmp.start()
}

def wwwDir = args[0]
def port = args.length > 1? args[1] as Integer : 8280
serveDirectory(port, new File(wwwDir).toURI().toString())


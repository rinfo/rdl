import org.restlet.*
import org.restlet.data.Protocol


@Grapes([
  @Grab('org.restlet:org.restlet:1.1.4'),
  @Grab('com.noelios.restlet:com.noelios.restlet:1.1.4')
])
class SourceApp extends Application {
    String wwwDir
    Restlet createRoot() {
        return new Directory(context, wwwDir as String)
    }
}

def serveSources(port, wwwDir) {
    def component = new Component()
    component.servers.add(Protocol.HTTP, port)
    component.clients.add(Protocol.FILE)
    component.defaultHost.attach("", new SourceApp(wwwDir:wwwDir))
    component.start()
}

def port = 8280
def wwwDir = args[0]
serveSources(port, new File(wwwDir).toURI().toString())


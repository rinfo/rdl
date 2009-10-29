import org.restlet.*
import org.restlet.data.Protocol


@Grab(group='org.restlet', module='org.restlet', version='1.1.4')
@Grab(group='com.noelios.restlet', module='com.noelios.restlet', version='1.1.4')
class SourceApp extends Application {
    String www
    Restlet createRoot() {
        return new Directory(context, www as String)
    }
}

def serveSources(port, www) {
    def component = new Component()
    component.servers.add(Protocol.HTTP, port)
    component.clients.add(Protocol.FILE)
    component.defaultHost.attach("", new SourceApp(www:www))
    component.start()
}

def port = 7070
def www = "_build/www"
serveSources(port, new File(www).toURI().toString())


package se.lagrummet.rinfo.main.storage

import org.apache.http.HttpVersion
import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpParams
import org.apache.http.params.HttpProtocolParams


public class FeedCollector {

    Storage storage

    public FeedCollector(Storage storage) {
        this.storage = storage
    }

    public void readFeed(URL url, StorageCredentials credentials) {
        def collectorSession = openSession(credentials)
        try {
            collectorSession.readFeed(url)
        } finally {
            collectorSession.shutdown()
        }
    }

    public openSession(StorageCredentials credentials) {
        def storageSession = storage.openSession(credentials)
        return new FeedCollectorSession(createClient(), storageSession)
    }

    // TODO:
    //  .. <http://hc.apache.org/httpcomponents-client/tutorial/html/connmgmt.html>
    //  - set timeout (default is infinite!)
    //  - Configure to use SSL (https) and verify cert.!
    //  -? httpClient.setHttpRequestRetryHandler(...)
    public HttpClient createClient() {
        return FeedCollector.createDefaultClient()
    }

    public static HttpClient createDefaultClient() {
        HttpParams params = new BasicHttpParams()
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
        ConnManagerParams.setMaxTotalConnections(params, 100)

        SchemeRegistry schemeRegistry = new SchemeRegistry()
        schemeRegistry.register(
                new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
        schemeRegistry.register(
                new Scheme("https", SSLSocketFactory.getSocketFactory(), 443))

        ClientConnectionManager clientConnMgr =
                new ThreadSafeClientConnManager(params, schemeRegistry)
        return new DefaultHttpClient(clientConnMgr, params)
    }

}

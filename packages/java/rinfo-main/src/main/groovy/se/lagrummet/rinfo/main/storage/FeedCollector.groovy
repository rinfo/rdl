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
import org.apache.http.impl.conn.SingleClientConnManager
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.apache.http.params.HttpProtocolParams

public class FeedCollector {

    Storage storage

    public FeedCollector(Storage storage) {
        this.storage = storage
    }

    public void readFeed(URL url, StorageCredentials credentials) {
        openSession(credentials).readFeed(url)
    }

    public openSession(StorageCredentials credentials) {
        def storageSession = storage.openSession(credentials)
        return new FeedCollectorSession(createClient(), storageSession)
    }

    public HttpClient createClient() {
        return createDefaultClient()
    }

    public static HttpClient createDefaultClient() {

        // TODO:? httpClient.setHttpRequestRetryHandler(...) // no use case demands it..

        /* TODO: Configure to use SSL (https) and verify cert.
        import java.security.KeyStore
        import javax.net.ssl.SSLPeerUnverifiedException

        def trustStore  = KeyStore.getInstance(KeyStore.getDefaultType())
        def inStream = new FileInputStream(new File(KEYSTORE_PATH))
        try {
            trustStore.load(inStream, "nopassword".toCharArray())
        } finally {
            inStream.close()
        }
        def socketFactory = new SSLSocketFactory(trustStore)
        */
        def socketFactory = SSLSocketFactory.getSocketFactory()
        def httpClient = new DefaultHttpClient()
        httpClient.connectionManager.schemeRegistry.register(
                new Scheme("https", socketFactory, 443))
        return httpClient
    }

}

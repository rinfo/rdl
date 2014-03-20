package se.lagrummet.rinfo.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient

class VarnishInvalidator {

    private final Logger logger = LoggerFactory.getLogger(VarnishInvalidator.class)

    String varnishUrl
    boolean invalidationEnabled
    HttpClient httpClient

    VarnishInvalidator(String varnishUrl, boolean invalidationEnabled) {
        this.varnishUrl = varnishUrl
        this.invalidationEnabled = invalidationEnabled
        this.httpClient = new DefaultHttpClient()
    }

    /**
     * Removes a single object from the varnish cache.
     * The exact behaviour of purge is defined by the VCL
     * that is used by the running varnishd
     */
    public void purge(String object) {
        if(invalidationEnabled) {
            HttpPurge httpPurge = new HttpPurge(varnishUrl + object)
            execute(httpPurge, object)
        } else {
            logger.info("Cache invalidation disabled. Did not trigger purge of " + object)
        }
    }

    /**
     * Removes all matching objects (regex) from the varnish cache.
     * The exact behaviour of ban is defined by the VCL
     * that is used by the running varnishd
     */
    public void ban(String object) {
        if(invalidationEnabled) {
            HttpBan httpBan = new HttpBan(varnishUrl + object)
            execute(httpBan, object)
        } else {
            logger.info("Cache invalidation disabled. Did not trigger ban of " + object)
        }
    }

    private void execute(HttpRequestBase httpRequest, String object) {
        def method = httpRequest.getMethod()
        try {
            HttpResponse response = httpClient.execute(httpRequest)
            def reason = response.getStatusLine().getReasonPhrase()
            logger.info("Response for " + method + " of " + object + " : " + reason)
        } catch (IOException e) {
            logger.error(method + " of " + object + " failed. IOException: " + e)
        } catch (RuntimeException e) {
            logger.error(method + " of " + object + " failed. RuntimeException: " + e)
        } finally {
            httpRequest.releaseConnection()
        }
    }
}
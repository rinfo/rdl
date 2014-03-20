package se.lagrummet.rinfo.service

import org.apache.http.client.methods.HttpRequestBase

public class HttpPurge extends HttpRequestBase {

    public HttpPurge() {
        super()
    }

    public HttpPurge(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    public String getMethod() {
        return "PURGE"
    }

}
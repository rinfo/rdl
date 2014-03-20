package se.lagrummet.rinfo.service

import org.apache.http.client.methods.HttpRequestBase

public class HttpBan extends HttpRequestBase {

    public HttpBan() {
        super()
    }

    public HttpBan(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    public String getMethod() {
        return "BAN"
    }
}
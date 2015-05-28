package se.lagrummet.rinfo.base.feed.exceptions;

import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.io.IOException;

/**
 * Created by christian on 5/28/15.
 */
public class MD5SumVerificationFailedException extends IOException {
    String resource;
    Md5Sum expected;
    Md5Sum actual;

    public String getResource() {return resource;}
    public Md5Sum getExpected() {return expected;}
    public Md5Sum getActual() {return actual;}

    public MD5SumVerificationFailedException(String resource, Md5Sum expected, Md5Sum actual) {
        super(resource+" expected "+expected+" but was "+actual);
        this.resource = resource;
        this.expected = expected;
        this.actual = actual;
    }
}

package se.lagrummet.rinfo.base.feed.exceptions;

/**
 * Created by christian on 5/22/15.
 */
public class SevereInternalException extends RuntimeException {

    public SevereInternalException(Throwable cause) {
        super(cause);
    }

    public SevereInternalException() {
    }

    public SevereInternalException(String message) {
        super(message);
    }
}

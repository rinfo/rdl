package se.lagrummet.rinfo.base.feed.exceptions;

/**
 * Created by christian on 5/25/15.
 */
public class ResourceWriteException extends Exception {

    public ResourceWriteException(String message) {
        super(message);
    }

    public ResourceWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}

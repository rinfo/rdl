package se.lagrummet.rinfo.base.feed.exceptions;

/**
* Created by christian on 5/21/15.
*/
public class FailedToReadFeedException extends Exception {

    public FailedToReadFeedException(String message) {
        super(message);
    }

    public FailedToReadFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}

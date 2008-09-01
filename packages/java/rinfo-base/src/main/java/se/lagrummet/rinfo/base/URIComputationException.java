package se.lagrummet.rinfo.base;

public class URIComputationException extends Exception {
    public URIComputationException(String message) {
        super(message);
    }
    public URIComputationException(String message, Exception e) {
        super(message, e);
    }
}

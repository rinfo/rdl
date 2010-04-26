package se.lagrummet.rinfo.base.rdf;


public class DescriptionException extends RuntimeException {
    DescriptionException(String msg) {
        super(msg);
    }
    DescriptionException(Throwable e) {
        super(e);
    }
    DescriptionException(String msg, Throwable e) {
        super(msg, e);
    }
}

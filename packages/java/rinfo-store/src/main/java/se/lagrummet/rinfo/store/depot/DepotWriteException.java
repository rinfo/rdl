package se.lagrummet.rinfo.store.depot;

public class DepotWriteException extends Exception {

    public DepotWriteException(String msg) {
        super(msg);
    }
    public DepotWriteException(Throwable throwable) {
        super(throwable);
    }
    public DepotWriteException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}

package se.lagrummet.rinfo.store.depot;

public class DepotIndexException extends DepotWriteException {

    public DepotIndexException(String msg) {
        super(msg);
    }
    public DepotIndexException(Throwable throwable) {
        super(throwable);
    }
    public DepotIndexException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}

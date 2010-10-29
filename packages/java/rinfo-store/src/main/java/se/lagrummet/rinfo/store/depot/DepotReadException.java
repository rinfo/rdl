package se.lagrummet.rinfo.store.depot;

public class DepotReadException extends DepotException {

    public DepotReadException(String msg) {
        super(msg);
    }
    public DepotReadException(Throwable throwable) {
        super(throwable);
    }
    public DepotReadException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}

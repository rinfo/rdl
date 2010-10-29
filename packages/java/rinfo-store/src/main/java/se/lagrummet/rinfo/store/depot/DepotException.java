package se.lagrummet.rinfo.store.depot;

public class DepotException extends Exception {

    public DepotException(String msg) {
        super(msg);
    }
    public DepotException(Throwable throwable) {
        super(throwable);
    }
    public DepotException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}

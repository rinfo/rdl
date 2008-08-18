package se.lagrummet.rinfo.store.depot;


public class ConfigurationException extends Exception {
    public ConfigurationException() {
        super();
    }
    public ConfigurationException(String msg) {
        super(msg);
    }
    public ConfigurationException(Throwable throwable) {
        super(throwable);
    }
    public ConfigurationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}

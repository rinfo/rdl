package se.lagrummet.rinfo.store.depot
class ConfigurationException extends Exception {
    ConfigurationException() {
        super()
    }
    ConfigurationException(String msg) {
        super(msg)
    }
    ConfigurationException(Throwable throwable) {
        super(throwable)
    }
    ConfigurationException(String msg, Throwable throwable) {
        super(msg, throwable)
    }
}

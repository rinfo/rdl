package se.lagrummet.rinfo.main.storage

class NotAllowedException extends RuntimeException {

    StorageCredentials credentials

    NotAllowedException(String message, StorageCredentials credentials) {
        super(message)
        this.credentials = credentials
    }

}

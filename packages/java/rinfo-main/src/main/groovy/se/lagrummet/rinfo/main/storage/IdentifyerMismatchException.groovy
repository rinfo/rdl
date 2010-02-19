package se.lagrummet.rinfo.main.storage


class IdentifyerMismatchException extends Exception {

    URI givenUri
    URI computedUri

    IdentifyerMismatchException(URI givenUri, URI computedUri) {
        super("Bad identifier:  identified as <"+givenUri+
                ">, computed identifier is <"+computedUri+">.");
        this.givenUri = givenUri
        this.computedUri = computedUri
    }

}

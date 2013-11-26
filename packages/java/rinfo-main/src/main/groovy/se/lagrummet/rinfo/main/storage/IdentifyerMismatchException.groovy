package se.lagrummet.rinfo.main.storage

import org.apache.commons.lang.StringUtils;

class IdentifyerMismatchException extends Exception {

    URI givenUri
    URI computedUri
    String commonPrefix
    String givenUriDiff
    String computedUriDiff

    IdentifyerMismatchException(URI givenUri, URI computedUri) {
        super("Bad identifier:  identified as <"+givenUri+
                ">, computed identifier is <"+computedUri+">.");
        this.givenUri = givenUri
        this.computedUri = computedUri

        this.commonPrefix = StringUtils.getCommonPrefix((String[]) [givenUri.toString(), computedUri.toString()])
        this.givenUriDiff = StringUtils.difference(commonPrefix, givenUri.toString())
        this.computedUriDiff = StringUtils.difference(commonPrefix, computedUri.toString())
    }

}

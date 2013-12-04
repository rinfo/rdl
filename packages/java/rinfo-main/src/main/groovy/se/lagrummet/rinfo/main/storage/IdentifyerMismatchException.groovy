package se.lagrummet.rinfo.main.storage

import org.apache.commons.lang.StringUtils;

class IdentifyerMismatchException extends Exception {

    URI givenUri
    URI computedUri
    String commonPrefix
    String commonSuffix
    String givenUriDiff
    String computedUriDiff

    IdentifyerMismatchException(URI givenUri, URI computedUri) {
        super("Bad identifier:  identified as <"+givenUri+
                ">, computed identifier is <"+computedUri+">.");
        this.givenUri = givenUri
        this.computedUri = computedUri

        def givenUriString = givenUri.toString()
        def computedUriString = computedUri.toString()
        this.commonPrefix = StringUtils.getCommonPrefix((String[]) [givenUriString, computedUriString])

        def reversedGivenUri = StringUtils.reverse(givenUriString)
        def reversedComputedUri = StringUtils.reverse(computedUriString)
        def reversedCommonSuffix = StringUtils.getCommonPrefix((String[]) [reversedGivenUri, reversedComputedUri])
        this.commonSuffix = StringUtils.reverse(reversedCommonSuffix)

        def givenUriPrefixDiff = StringUtils.difference(commonPrefix, givenUriString)
        def reversedGivenUriPrefixDiff = StringUtils.reverse(givenUriPrefixDiff)
        def reversedGivenUriSuffixDiff = StringUtils.difference(reversedCommonSuffix, reversedGivenUriPrefixDiff)
        this.givenUriDiff = StringUtils.reverse(reversedGivenUriSuffixDiff)

        def computedUriPrefixDiff = StringUtils.difference(commonPrefix, computedUriString)
        def reversedComputedUriPrefixDiff = StringUtils.reverse(computedUriPrefixDiff)
        def reversedComputedUriSuffixDiff = StringUtils.difference(reversedCommonSuffix, reversedComputedUriPrefixDiff)
        this.computedUriDiff = StringUtils.reverse(reversedComputedUriSuffixDiff)
    }

}

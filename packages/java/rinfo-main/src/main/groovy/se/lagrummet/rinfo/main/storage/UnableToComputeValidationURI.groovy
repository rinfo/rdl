package se.lagrummet.rinfo.main.storage


class UnableToComputeValidationURI extends Exception {

    URI givenUri

    UnableToComputeValidationURI(URI givenUri) {
        super("Unable to compute the validation uri. Cannot validate if the uri is correct! <"+givenUri+
                ">. This could mean the uri contains parts that is not configured in RDL or faulty!");
        this.givenUri = givenUri
    }

}

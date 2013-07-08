package se.lagrummet.rinfo.main.storage


class UnknownSubjectException extends Exception {

    URI subject

    UnknownSubjectException(URI subject) {
        super("Unable to understand the URI <"+subject+
                ">. This could mean the URI contains parts that is not configured in RDL or these parts are misspelled or otherwise incorrect!");
        this.subject = subject
    }

}

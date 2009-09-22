package se.lagrummet.rinfo.store.depot;


public class SourceCheckException extends RuntimeException {

    SourceContent.Check failedCheck;
    Object expectedValue;
    Object realValue;

    public SourceCheckException(SourceContent.Check failedCheck,
            Object expectedValue, Object realValue) {
        super("Failed datacheck for "+failedCheck+". Expected "+expectedValue+
                ", got "+realValue);
        this.failedCheck = failedCheck;
        this.expectedValue = expectedValue;
        this.realValue = realValue;
    }

}

package se.lagrummet.rinfo.store.depot;


public class SourceCheckException extends RuntimeException {

    public SourceContent sourceContent;
    public SourceContent.Check failedCheck;
    public Object givenValue;
    public Object realValue;

    public SourceCheckException(SourceContent sourceContent,
            SourceContent.Check failedCheck,
            Object givenValue, Object realValue) {
        super("Failed datacheck for "+failedCheck+". Expected "+givenValue+
                ", got "+realValue);
        this.sourceContent = sourceContent;
        this.failedCheck = failedCheck;
        this.givenValue = givenValue;
        this.realValue = realValue;
    }

}

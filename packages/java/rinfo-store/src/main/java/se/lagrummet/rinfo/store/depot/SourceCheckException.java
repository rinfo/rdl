package se.lagrummet.rinfo.store.depot;

import java.io.IOException;


public class SourceCheckException extends IOException { // TODO: just Exception?

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

package se.lagrummet.rinfo.base;


public class MintResult {

    private String uri;
    private int matchCount;
    private int rulesSize;
    private int priority;

    public MintResult(String uri, int matchCount, int rulesSize, int priority) {
        this.uri = uri;
        this.matchCount = matchCount;
        this.rulesSize = rulesSize;
        this.priority = priority;
    }

    public String getUri() { return uri; }
    public Integer getMatchCount() { return matchCount; }
    public Integer getRulesSize() { return rulesSize; }
    public int getPriority() { return priority; }

}

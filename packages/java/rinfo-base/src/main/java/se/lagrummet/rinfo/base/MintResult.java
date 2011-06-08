package se.lagrummet.rinfo.base;


public class MintResult {

    private String uri;
    private int matchCount;
    private int rulesSize;

    public MintResult(String uri, int matchCount, int rulesSize) {
        this.uri = uri;
        this.matchCount = matchCount;
        this.rulesSize = rulesSize;
    }

    public String getUri() { return uri; }
    public Integer getMatchCount() { return matchCount; }
    public Integer getRulesSize() { return rulesSize; }

}

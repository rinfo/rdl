package se.lagrummet.rinfo.service


class RepoInfo {

    static int countContexts(conn) {
        def res = conn.contextIDs
        def i = res.asList().findAll {
            it.stringValue().endsWith(RepoEntry.CONTEXT_SUFFIX) }.size()
        res.close()
        return i
    }

}

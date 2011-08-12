#!/usr/bin/env groovy


class Fetcher {

    File targetDir
    boolean force = false

    void downloadData(String startLink) {
        downloadLists(startLink)
        downloadDocs()
    }

    void downloadLists(String startLink) {
        existDir(targetDir)
        def (nextLink, i) = [startLink, 1]
        while (nextLink) {
            def listDir = existDir(targetDir, "list-${i++}")
            fileToCreate(listDir, "index.xml") {
                download(nextLink, it)
                def xml = new XmlSlurper().parse(it)
                nextLink = xml.'@nasta_sida'.text()
            }
        }
        println "Done fetching list pages."
    }

    void downloadDocs() {
        targetDir.eachDirMatch(~/^list-\d+$/) {
            downloadDocsFromList(new File(it, "index.xml"))
        }
    }

    void downloadDocsFromList(File listFile) {
        def xml = new XmlSlurper().parse(listFile)
        xml.'dokument'.each { elem ->
            def docId = elem.'id'.text()
            def sysDate = elem.'systemdatum'.text()
            def sysDateSlug = sysDate.replace(' ', 'T').replace(':', '_')
            Closure getRepresentation = { tag, suffix ->
                fileToCreate(listFile.parentFile, "${sysDateSlug}-${docId}.${suffix}") {
                    def docLink = elem[tag].text()
                    if (docLink) {
			download(docLink, it)
                    }
                }
            }
            getRepresentation 'dokumentstatus_url_xml', 'xml'
            getRepresentation 'dokument_url_html', 'html'
            getRepresentation 'dokument_url_text', 'txt'
        }
    }

    protected File existDir(File base, String path=null) {
        def dir = path? new File(base, path) : base
        assert !force || dir.exists() || dir.mkdir()
        dir
    }

    protected void fileToCreate(File parent, String name, Closure handle) {
        def f = new File(parent, name)
        if (!f.exists() || f.file && force) {
            handle(f)
        }
    }

    protected void download(String link, File dest) {
        println "Downloading <${link}> to <${dest}> ..."
        // TODO: parallellize (gpars)?
        dest.withOutputStream { fos ->
            new URL(link).withInputStream { fos << it }
        }
    }

}


def KNOWN_DOCTYPES = ['prop', 'sou', 'ds']

def startLink(doctype, size=400) {
    "http://data.riksdagen.se/dokumentlista/" +
        "?typ=${doctype}&sz=${size}&sort=c&utformat=xml"
}


def (flags, args) = args.split { it =~ /^-/ }
if (args.size() < 1) {
    println "Usage: TARGET_DIR [DOCTYPE] [-l] [-d] [-f]"
    System.exit 0
}

if (args.size() < 2) {
    println "Usage: TARGET_DIR DOCTYPE [-l] [-d] [-f]"
    println "   where DOCTYPE can be one of ${KNOWN_DOCTYPES}"
    println "   -l: Just download search result lists"
    println "   -d: Download document using previously downloaded search result lists"
    println "   -f: Download documents/list even if present already"
    // if you're having problem with script exiting due to "500 server
    // error", try first downloading the list (-l), then documents
    // (-d) over and over until you get all docs.
} else {
    def fetcher = new Fetcher(targetDir: new File(args[0]), force: "-f" in flags)
    def doctype = args[1] ?: KNOWN_DOCTYPES[0]
    assert doctype in KNOWN_DOCTYPES

    if ("-l" in flags)
	fetcher.downloadLists(startLink(doctype))
    else if ("-d" in flags)
        fetcher.downloadDocs()
    else
	fetcher.downloadData(startLink(doctype))
}



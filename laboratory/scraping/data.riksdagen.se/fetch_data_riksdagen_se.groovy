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
                println "Fetching list <${nextLink}> to <${it.path}> ..."
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
        def i = 1
        xml.'dokument'.each { elem ->
            def docId = elem.'id'.text()
            fileToCreate(listFile.parentFile, "${i++}-${docId}.xml") {
                def docLink = elem.'dokumentstatus_url_xml'.text()
                println "Fetching document <${docLink}> to <${it.path}> ..."
                download(docLink, it)
            }
        }
    }

    protected File existDir(File base, String path=null) {
        def dir = path? new File(base, path) : base
        assert !force || dir.exists() || dir.mkdir()
        dir
    }

    protected void fileToCreate(File parent, String name, Closure block) {
        def f = new File(parent, name)
        if (!f.exists() || f.file && force) {
            block(f)
        }
    }

    protected void download(String link, File dest) {
        // TODO: parallellize
        dest.withOutputStream { fos ->
            new URL(link).withInputStream { fos << it }
        }
    }

}


def startLink(doctype='prop', size=400) {
    "http://data.riksdagen.se/dokumentlista/" +
        "?typ=${doctype}&sz=${size}&sort=c&utformat=xml"
}


def (flags, args) = args.split { it =~ /^-/ }
if (args.size() < 1) {
    println "Usage: <target-dir> [-l] [-d] [-f]"
    System.exit 0
}

def fetcher = new Fetcher(targetDir: new File(args[0]), force: "-f" in flags)

// TODO: doctype = args[1] ?: 'prop'
if ("-l" in flags)
    fetcher.downloadLists(startLink())
else if ("-d" in flags)
    fetcher.downloadDocs()
else
    fetcher.downloadData(startLink())


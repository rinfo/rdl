import java.security.DigestOutputStream
import java.security.MessageDigest
import org.apache.commons.io.IOUtils
import org.apache.commons.codec.binary.Hex


@Grab(group='commons-codec', module='commons-codec', version='1.3')
@Grab(group='commons-io', module='commons-io', version='1.4')
def getMD5HexDigest(InputStream ins) {
    def outs = new DigestOutputStream(new ByteArrayOutputStream(), MessageDigest.getInstance("MD5"))
    IOUtils.copy(ins, outs)
    outs.close()
    return new String(Hex.encodeHex(outs.messageDigest.digest()))
}

def checkFeed(String feedUrl, verbose=true) {
    def feed = new XmlSlurper().parse(feedUrl)
    feed.declareNamespace('le':"http://purl.org/atompub/link-extensions/1.0")
    def totalMismatches = 0
    feed.entry.each {
        def entryId = it.id as String
        def someMismatch = false
        def results = []
        it.link.each {
            if (it.@rel == 'alternate') {
                def url = it.@href as String
                def expectedMD5 = it.@'le:md5' as String
                def realMD5 = computeMD5(url)
                def equalMD5s = expectedMD5 == realMD5
                if (!equalMD5s) {
                    totalMismatches++
                    someMismatch = true
                }
                results << [url:url, expected:expectedMD5, real:realMD5, ok:equalMD5s]
            }
        }
        if (verbose || someMismatch)
            println "Checking ${entryId}"
        results.each {
            if (verbose || !it.ok)
                println "  <$it.url>: ${it.ok}"
            if (!it.ok) {
                println "    MD5 mismatch: expected = ${it.expected}; real = ${it.real}"
            }
        }
        if (verbose || someMismatch)
            println()
    }
    if (totalMismatches)
        println "Total mismatches: ${totalMismatches}"
}

def computeMD5(String url) {
    def ins = new URL(url).openStream()
    def realMD5 = getMD5HexDigest(ins)
    ins.close()
    return realMD5
}


def path = args[0]
checkFeed path, '-v' in args


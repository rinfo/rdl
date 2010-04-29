@Grapes([
    @Grab('com.uwyn:jhighlight:1.0'),
    @Grab('com.lowagie:itext:2.0.8'),
    @Grab('org.xhtmlrenderer:core-renderer:R8pre2'),
    // Note: when using Option B (re READEME.txt, the pathing jar
    // option), these need to be commented out.
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('xml-apis:xml-apis:1.3.04'),
    @Grab('xerces:xercesImpl:2.9.1'),
    @Grab('xalan:xalan:2.7.1'),
    @Grab('xalan:serializer:2.7.1')
    ])
import docgen.Builder

def (flags, paths) = args.split { it =~ /^--/ }
def newArgs = flags + [
    "../resources/",
    "../documentation/",
    paths[0] ?: "../_build/documentation",
] + (paths[1]? paths[1..-1] : Builder.DEFAULT_RENDER_PATTERNS)

Builder.main(newArgs as String[])


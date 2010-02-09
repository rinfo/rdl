@Grapes([
    @Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT'),
    @Grab('com.uwyn:jhighlight:1.0'),
    @Grab('com.lowagie:itext:2.0.8'),
    @Grab('org.xhtmlrenderer:core-renderer:R8pre2'),
    @Grab('xml-apis:xml-apis:1.3.04'),
    @Grab('xerces:xercesImpl:2.9.1'),
    @Grab('xalan:xalan:2.7.1'),
    @Grab('xalan:serializer:2.7.1')
])
import docgen.Builder

def resourceDir = "../resources/"
def sourceDir = "../documentation/"

def (flags, paths) = args.split { it =~ /^--/ }

def buildDir = paths[0] ?: "../_build/documentation"
def patterns = paths[1]? paths[1..-1] : Builder.DEFAULT_RENDER_PATTERNS

def copies = Builder.DEFAULT_COPY_PATTERNS
def clean = "--clean" in flags
def nogen = "--nogen" in flags

def build = new Builder(resourceDir, sourceDir, buildDir).build(
        patterns, copies, clean, !nogen)


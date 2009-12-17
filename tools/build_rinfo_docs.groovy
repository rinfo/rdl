@Grapes([
    @Grab(group='se.lagrummet.rinfo', module='rinfo-service', version='1.0-SNAPSHOT'),
    @Grab(group='com.uwyn', module='jhighlight', version='1.0'),
    @Grab(group='com.lowagie', module='itext', version='2.0.8'),
    @Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2'),
    @Grab(group='xerces', module='xercesImpl', version='2.9.1'),
    @Grab(group='xalan', module='xalan', version='2.7.1'),
    @Grab(group='xalan', module='serializer', version='2.7.1')
])
def _(){}

import docgen.Builder

def resourceDir = "../resources/"
def sourceDir = "../documentation/"

def (flags, paths) = args.split { it =~ /^--/ }

def buildDir = paths[0]
def patterns = paths[1]? paths[1..-1] : Builder.DEFAULT_RENDER_PATTERNS

def copies = Builder.DEFAULT_COPY_PATTERNS
def clean = "--clean" in flags
def nogen = "--nogen" in flags

def build = new Builder(resourceDir, sourceDir, buildDir).build(
        patterns, copies, clean, !nogen)


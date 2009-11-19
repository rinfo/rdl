@Grab(group='se.lagrummet.rinfo', module='rinfo-service', version='1.0-SNAPSHOT')
@Grab(group='com.uwyn', module='jhighlight', version='1.0')
@Grab(group='com.lowagie', module='itext', version='2.0.8')
@Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2')
@Grab(group='xerces', module='xercesImpl', version='2.9.1')
def _(){}

import docgen.Builder

def resourceDir = "../resources/"
def sourceDir = "../documentation/"
def buildDir = "_build/documentation"

def (flags, paths) = args.split { it =~ /^--/ }
def clean = "--clean" in flags
def patterns = paths ?: Builder.DEFAULT_RENDER_PATTERNS
def build = new Builder(resourceDir, sourceDir, buildDir).build(
        clean, patterns)


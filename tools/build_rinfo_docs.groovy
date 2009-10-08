@Grab(group='se.lagrummet.rinfo', module='rinfo-service', version='1.0-SNAPSHOT')
@Grab(group='com.uwyn', module='jhighlight', version='1.0')
@Grab(group='com.lowagie', module='itext', version='2.0.8')
@Grab(group='org.xhtmlrenderer', module='core-renderer', version='R8pre2')
def _(){}

import docgen.Builder

def resourceDir = "../resources/"
def sourceDir = "../documentation/"
def buildDir = "_build/documentation"

Builder.main(resourceDir, sourceDir, buildDir)


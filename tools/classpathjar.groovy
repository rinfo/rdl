
MVN_EXE = System.properties['os.name'] =~ /Windows/? "mvn.bat" : "mvn"
DEP_SEP = ";"
MVN_DEP_CMD = "${MVN_EXE} dependency:build-classpath -Dmdep.pathSeparator=${DEP_SEP}"

List mavenPackageJars(pkgDir) {
    return MVN_DEP_CMD.execute(null, pkgDir).text.readLines().find {
        !it.startsWith("[INFO]")
    }.split(DEP_SEP)
}

void makePathingJar(jarPaths, destFile) {
    def ant = new AntBuilder()
    ant.manifestclasspath(property:"jar.classpath", jarfile:destFile,
            maxParentLevels:10) {
        classpath {
            for (path in jarPaths) {
                // TODO: due to bug <http://jira.codehaus.org/browse/GROOVY-3356>:
                if (path =~ /xerces.*\.jar$|xml-apis.*\.jar$/)
                    continue
                pathelement location:path
            }
        }
    }
    ant.delete file:destFile, quiet:true
    ant.jar(destfile:destFile) {
        manifest {
            attribute name:"Class-Path", value: '${jar.classpath}'
        }
    }
}

if (args.length < 2) {
    println "USAGE: <MAVEN_PACKAGE_PATH> <DEST_JAR>"
    System.exit 0
}
def pkgDir = new File(args[0])
def destJar = args[1]

def jarPaths = mavenPackageJars(pkgDir) + [
        "src/main/groovy",
        "src/main/resources",
        "target/classes"
    ].collect { new File(pkgDir, it).canonicalPath }

makePathingJar jarPaths, destJar



DEP_SEP = ":"
MVN_DEP_CMD = "mvn dependency:build-classpath -Dmdep.pathSeparator=${DEP_SEP}"

List mavenPackageJars(pkgDir) {
    return MVN_DEP_CMD.execute(null, pkgDir).text.readLines().find {
        !it.startsWith("[INFO]")
    }.split(DEP_SEP)
}

void makePathingJar(jarPaths, destFile) {
    def ant = new AntBuilder()
    ant.manifestclasspath(property:"jar.classpath", jarfile:destFile) {
        classpath {
            for (path in jarPaths) {
                pathelement location:path
            }
        }
    }
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


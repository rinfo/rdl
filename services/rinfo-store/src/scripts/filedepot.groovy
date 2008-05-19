import se.lagrummet.rinfo.store.depot.FileDepot

if (args.size() != 2) {
    println "Usage: <script> <base-uri> <depot-dir>"
    System.exit 0
}
def baseUri = new URI(args[0])
def depotDir = new File(args[1])
def fileDepot = new FileDepot(baseUri, depotDir)
fileDepot.generateIndex()


package se.lagrummet.rinfo.store.depot


class TempDepotUtil {

    FileDepot depot
    File tempDepotDir
    File depotSrc

    FileDepot createTempDepot() {
        depotSrc = new File("src/test/resources/exampledepot/storage")
        tempDepotDir = TempDirUtil.createTempDir(depotSrc)
        depot = new FileDepot(new URI("http://example.org"),
                new File(tempDepotDir, depotSrc.name))
        depot.atomizer.feedPath = "/feed"
        return depot
    }

    void deleteTempDepot() {
        depot = null
        TempDirUtil.removeTempDir(tempDepotDir)
    }

    def exampleEntryFile(path) {
        new File(depotSrc, "publ/1901/100/ENTRY-INFO/${path}")
    }

    def exampleFile(path) {
        new File(depotSrc, "publ/1901/100/${path}")
    }

}

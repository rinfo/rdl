package se.lagrummet.rinfo.store.depot


class FileDepotTempBase {

    static FileDepot depot
    static File tempDepotDir
    static File depotSrc

    static void createTempDepot() {
        depotSrc = new File("src/test/resources/exampledepot/storage")
        tempDepotDir = TempDirUtil.createTempDir(depotSrc)
        depot = new FileDepot(new URI("http://example.org"),
                new File(tempDepotDir, depotSrc.name), "/feed")
    }

    static void deleteTempDepot() {
        TempDirUtil.removeTempDir(tempDepotDir)
        depot = null
    }

    static exampleEntryFile(path) {
        new File(depotSrc, "publ/1901/100/ENTRY-INFO/${path}")
    }

    static exampleFile(path) {
        new File(depotSrc, "publ/1901/100/${path}")
    }

}

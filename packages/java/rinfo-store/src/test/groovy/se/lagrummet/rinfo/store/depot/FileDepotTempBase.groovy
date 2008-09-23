package se.lagrummet.rinfo.store.depot

import org.junit.AfterClass
import org.junit.BeforeClass


class FileDepotTempBase {

    static FileDepot fileDepot
    static File tempDepotDir
    static File depotSrc

    @BeforeClass
    static void setupClass() {
        depotSrc = new File("src/test/resources/exampledepot/storage")
        tempDepotDir = TempDirUtil.createTempDir(depotSrc)
        fileDepot = new FileDepot(new URI("http://example.org"),
                new File(tempDepotDir, depotSrc.name), "/feed")
    }

    @AfterClass
    static void tearDownClass() {
        TempDirUtil.removeTempDir(tempDepotDir)
    }

    protected exampleEntryFile(path) {
        new File(depotSrc, "publ/1901/100/ENTRY-INFO/${path}")
    }

    protected exampleFile(path) {
        new File(depotSrc, "publ/1901/100/${path}")
    }

}

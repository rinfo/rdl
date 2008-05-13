package se.lagrummet.rinfo.store.depot

import org.apache.commons.io.FileUtils


class TempDirUtil {

    static File createTempDir(sourceDir) {
        def tempDir
        def manualTempDirPath = System.properties["rinfo.manualTempDir"]
        if (manualTempDirPath) {
            tempDir = new File(manualTempDirPath)
        } else {
            tempDir = File.createTempFile("rinfotempdir", "",
                    new File(System.getProperty("java.io.tmpdir")))
            assert tempDir.delete() // remove file to create dir..
            assert tempDir.mkdir()
        }
        FileUtils.copyDirectoryToDirectory(sourceDir, tempDir)
        return tempDir
    }

    static removeTempDir(tempDir) {
        // NOTE: to keep test dir, use: -Drinfo.manualTempDir=<dir>
        if (!System.properties["rinfo.manualTempDir"]) {
            FileUtils.forceDelete(tempDir)
        }
    }

}

package se.lagrummet.rinfo.store.depot;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;


public class FilePathUtil {

    public static String toRelativeFilePath(File file, File baseDir) {
        // TODO:IMPROVE: depot.toUriPath(file .. or relativeFilePath..) ?
        String fileUriPath = file.toURI().toString();
        String baseDirUriPath = baseDir.toURI().toString();
        if (!baseDirUriPath.endsWith("/")) {
            throw new RuntimeException("File <"+baseDirUriPath +
                    "> does not appear to be a directory" +
                    " (file URI does not end with '/').");
        }
        if (!fileUriPath.startsWith(baseDirUriPath)) {
            throw new DepotUriException(
                    "Enclosed file <"+fileUriPath +
                    "> is not within directory <"+baseDirUriPath+">.");
        }
        return fileUriPath.replaceFirst(baseDirUriPath, "");
    }

    public static void plowParentDirPath(File file) throws IOException {
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
    }

    public static void removeEmptyTrail(File dir, File baseDir) throws IOException {
        while (dir != null && !dir.equals(baseDir)) {
            if (dir.list().length != 0) {
                break;
            }
            dir.delete();
            dir = dir.getParentFile();
        }
    }
}

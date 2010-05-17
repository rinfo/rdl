package se.lagrummet.rinfo.store.depot;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.lang.time.FastDateFormat;


public class DatePathUtil {

    public static final FastDateFormat YEAR_DAY_SEC_FORMAT =
        FastDateFormat.getInstance("yyyy/MM-dd/'T'HH_mm_ss'Z'",
                TimeZone.getTimeZone("UTC"));

    public static final FastDateFormat YEAR_DAY_MIN_MSEC_FORMAT =
        FastDateFormat.getInstance("yyyy/MM-dd/'T'HH_mm/ss.SSS'Z'",
                TimeZone.getTimeZone("UTC"));

    public static final String YEAR_NAME_REGEX = "^\\d+$";
    public static final String DAY_NAME_REGEX =
        "^(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$";
    public static final String SEC_NAME_REGEX = "^T\\d{2}_\\d{2}_\\d{2}Z$";


    public static final FileFilter YEAR_DIR_FILTER = new FileFilter() {
        public boolean accept(File it) {
            return it.isDirectory() && it.getName().matches(YEAR_NAME_REGEX);
        }
    };

    public static final FileFilter DAY_DIR_FILTER = new FileFilter() {
        public boolean accept(File it) {
            return it.isDirectory() && it.getName().matches(DAY_NAME_REGEX);
        }
    };

    public static final FileFilter SEC_DIR_FILTER = new FileFilter() {
        public boolean accept(File it) {
            return it.isDirectory() && it.getName().matches(SEC_NAME_REGEX);
        }
    };


    // TODO: or methods named "toYearDaySecPath", "toYearDayMinMSecPath"?

    public static String toEntryHistoryPath(Date time) {
        return YEAR_DAY_SEC_FORMAT.format(time);
    }

    public static String toFeedArchivePath(Date time) {
        return YEAR_DAY_MIN_MSEC_FORMAT.format(time);
    }


    public static File youngestEntryHistoryDir(File baseDir) {
        File yearDir = getLargestNameInDir(baseDir, YEAR_DIR_FILTER);
        if (yearDir == null) { return null; }
        File dayDir = getLargestNameInDir(yearDir, DAY_DIR_FILTER);
        if (dayDir == null) { return null; }
        File secDir = getLargestNameInDir(dayDir, SEC_DIR_FILTER);
        return secDir;
    }

    private static File getLargestNameInDir(File dir, FileFilter filter) {
        File largest = null;
        for (File candidate : dir.listFiles(filter)) {
            if (largest == null ||
                candidate.getName().compareTo(largest.getName()) > 0) {
                largest = candidate;
            }
        }
        return largest;
    }

}

package se.lagrummet.rinfo.store.depot;

import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.lang.time.FastDateFormat;


public class DatePathUtil {

    // TODO: or methods like "toHourLevelPath", "toSecLevelContainerPath"?

    public static final FastDateFormat YEAR_DAY_SEC_FORMAT = FastDateFormat.getInstance(
            "yyyy/MM-dd/'T'HH_mm_ss'Z'", TimeZone.getTimeZone("UTC"));

    public static final FastDateFormat YEAR_DAY_MIN_MSEC_FORMAT = FastDateFormat.getInstance(
            "yyyy/MM-dd/'T'HH_mm/ss.SSS'Z'", TimeZone.getTimeZone("UTC"));

    public static String toEntryHistoryPath(Date time) {
        return YEAR_DAY_SEC_FORMAT.format(time);
    }

    public static String toFeedArchivePath(Date time) {
        return YEAR_DAY_MIN_MSEC_FORMAT.format(time);
    }

}

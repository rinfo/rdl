package se.lagrummet.rinfo.store.depot

import org.apache.commons.lang.time.FastDateFormat


class DatePathUtil {

    // TODO: or methods like "toHourLevelPath", "toSecLevelContainerPath"?

    static final YEAR_DAY_SEC_FORMAT = FastDateFormat.getInstance(
            "yyyy/MM-dd/'T'HH_mm_ss'Z'", TimeZone.getTimeZone("UTC"))

    static final YEAR_DAY_MIN_MSEC_FORMAT = FastDateFormat.getInstance(
            "yyyy/MM-dd/'T'HH_mm/ss.SSS'Z'", TimeZone.getTimeZone("UTC"))

    static String toEntryHistoryPath(Date time) {
        return YEAR_DAY_SEC_FORMAT.format(time)
    }

    static String toFeedArchivePath(Date time) {
        return YEAR_DAY_MIN_MSEC_FORMAT.format(time)
    }

}

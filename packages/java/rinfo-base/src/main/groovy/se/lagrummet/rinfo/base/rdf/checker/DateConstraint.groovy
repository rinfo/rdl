package se.lagrummet.rinfo.base.rdf.checker


class DateConstraint {

    Integer minYear
    Integer maxDaysFromNowMS
    Integer maxYearsFromNow

    private int DAY_MS = 24 * 60 * 60 * 1000

    DateConstraint(Integer minYear, Integer maxDaysFromNow, Integer maxYearsFromNow) {
        this.minYear = minYear
        this.maxDaysFromNowMS = maxDaysFromNow != null? maxDaysFromNow * DAY_MS : null
        this.maxYearsFromNow = maxYearsFromNow
    }

    boolean verify(GregorianCalendar cal, GregorianCalendar now) {
        def year = cal.get(Calendar.YEAR)
        if (minYear != null && year < minYear) {
            return false
        }
        if (maxDaysFromNowMS != null) {
            def diff = cal.getTimeInMillis() - now.getTimeInMillis()
            if (diff > maxDaysFromNowMS) {
                return false
            }
        }
        if (maxYearsFromNow != null &&
            (year - now.get(Calendar.YEAR)) > maxYearsFromNow) {
            return false
        }
        return true
    }

}

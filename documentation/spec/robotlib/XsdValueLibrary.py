from datetime import datetime
from time import strptime

class XsdValueLibrary:

    def date_should_be_younger(self, youngvalue, oldvalue):
        assert date_is_younger(youngvalue, oldvalue), (
                "Expected date time %s to be younger than %s" % (
                    youngvalue, oldvalue))

    def is_date(value):
        try:
            parse_date_time(v)
        except ValueError, e:
            assert False, e

def date_is_younger(youngvalue, oldvalue):
    """
    >>> date_is_younger("2009-01-01T00:00:01Z", "2009-01-01T00:00:00Z")
    True
    >>> date_is_younger("2008-01-01T00:00:01Z", "2009-01-01T00:00:00Z")
    False
    >>> date_is_younger("2009-01-01T00:00:00Z", "2009-01-01T00:00:00Z")
    False
    """
    young_dtime = parse_date_time(youngvalue)
    old_dtime = parse_date_time(oldvalue)
    return young_dtime > old_dtime

# From rdflib/term.py (modified to raise ValueError if failing to parse).
def parse_date_time(v) :
    """
    Attempt to cast to datetime, or raise ValueError.
    """
    if isinstance(v, datetime):
        return v
    try:
        tstr = strptime(v,"%Y-%m-%dT%H:%M:%S")
    except:
        try:
            tstr = strptime(v,"%Y-%m-%dT%H:%M:%SZ")
        except:
            try:
                tstr = strptime(v,"%Y-%m-%dT%H:%M:%S%Z")
            except:
                raise ValueError("Could not parse %s as a W3C/XSD dateTime" % v)
    return datetime(tstr.tm_year, tstr.tm_mon, tstr.tm_mday,
            tstr.tm_hour, tstr.tm_min, tstr.tm_sec)


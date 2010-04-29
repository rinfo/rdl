@Grab(group='se.lagrummet.rinfo', module='rinfo-base', version='1.0-SNAPSHOT')
import se.lagrummet.rinfo.base.URIMinter

defaultArgs = ["http://rinfo.lagrummet.se/sys/uri/scheme#",
                   "../../resources/base/sys/uri/"]

URIMinter.main((defaultArgs + (args as List)) as String[])


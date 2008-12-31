#!/bin/bash

xsltproc \
    --stringparam q 1999:175 \
    --stringparam category "KonsolideradGrundforfattning|Forordning|Utredningsbetankande/1999" \
    examples/rdata/query_atom.xslt -
# Expected:
# - three entries with "1999:175"
#   - 2 Forordning, 1 KonsolideradGrundforfattning, 0 Utredningsbetankande
#     - 1 Forordning with 1999, 1 KonsolideradGrundforfattning with 1999
# = 2 entries.

#xsltproc --stringparam category "Myndighetsforeskrift/RA-FS|KIFS/2006" examples/rdata/query_atom.xslt -
# Expected: 3 entries.

#xsltproc --stringparam category "Myndighetsforeskrift/RA-FS|KIFS/2006" --stringparam author "20061130160202" query_atom.xslt -
# Expected: 2 entries.

#xsltproc --stringparam entry-id "http://rinfo.lagrummet.se/publ/sfs/1999:175" query_atom.xslt -
# Expected: 1 entry.


# to get working data:
# ./run_sparqltree.py -e http://localhost:8080/openrdf-sesame/repositories/rinfo?infer=false sparqltree-rdata_entry.xml
# xsltproc tree_to_atom.xslt - | xmllint --format -


xsltproc \
    --stringparam q 1999:175 \
    --stringparam category "KonsolideradGrundforfattning|Forordning|Utredningsbetankande/1999" \
    query_atom.xslt -
# Expected:
# - three entries with "1999:175"
#   - 2 Forordning, 1 KonsolideradGrundforfattning, 0 Utredningsbetankande
#     - 1 Forordning with 1999, 1 KonsolideradGrundforfattning with 1999
# = 2 entries.

#xsltproc --stringparam category "Myndighetsforeskrift/RA-FS|KIFS/2006" query_atom.xslt -
# Expected: 3 entries.

#xsltproc --stringparam category "Myndighetsforeskrift/RA-FS|KIFS/2006" --stringparam author "20061130160202" query_atom.xslt -
# Expected: 2 entries.

#xsltproc --stringparam entry-id "http://rinfo.lagrummet.se/publ/sfs/1999:175" query_atom.xslt -
# Expected: 1 entry.


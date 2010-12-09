@Grab('se.lagrummet.rinfo:rinfo-base:1.0-SNAPSHOT')
import se.lagrummet.rinfo.base.rdf.GritTransformer
new GritTransformer().writeXhtml(System.in, new PrintWriter(System.out))

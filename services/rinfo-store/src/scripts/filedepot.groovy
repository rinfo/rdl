import se.lagrummet.rinfo.store.depot.FileDepot
import org.springframework.context.support.ClassPathXmlApplicationContext as Ctxt

context = new Ctxt("applicationContext.xml")
fileDepot = context.getBean("fileDepot")

fileDepot.generateIndex()


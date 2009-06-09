
Administrationsklient för grunddata i rättsinformationssystemet
---------------------------------------------------------------

Denna applikation används för att administrera grunddata om organisationer,
inhämtningskällor och författningssamlingar i rättsinformationssystemet.
Applikationen är byggd i ramverket Grails (http://grails.org/) i programspråket
Groovy och körs på javaplattformen.


Utvecklingsmiljö
----------------

För att sätta upp en utvecklingsmiljö behövs följande:

 * Programspråket Groovy 1.6 http://groovy.codehaus.org/Download 
 * Ramverket Grails http://grails.org
 * Java 1.6
 * Grails Audit logging http://docs.codehaus.org/display/GRAILS/Grails+Audit+Logging+Plugin

För en introdukion till utveckling i ramverket Grails se
http://grails.org/Quick+Start 

För att starta applikationen kör

 $ grails run-app

Om du använder Windows och har en profilkatalog på en nätverksdisk går det
fortare att kompilera och köra applikaitonen om du anger en lokal
arbetskatalog. Skapa t.ex. mappen c:\grails-workdir och lägg till parametern
grails.work.dir då du kör grails-kommandon för snabbare exekvering:

 $ grails -Dgrails.work.dir=C:/grails-workdir run-app

...och surfa till http://localhost:8080/rinfo-admin


För att köra igenom testscenarion kör

 $ grails test


Viktiga delar
-------------



Driftsättning
-------------


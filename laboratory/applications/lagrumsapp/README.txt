Om exempelapplikationen
-----------------------

Syftet med denna exempelapplikation är att illustrera i programkod hur man kan
implementera de format som rekommenderas av Rättsinformationsprojektet för den
mest grundläggande nivån. Tanken är inte att tillhandahålla ett färdigt system
för rättsinformationshantering.

Applikationem är byggd på webbramverket Django som ges ut under BSD-licensen.
Se BSD-licensen för mer information om möjligheter att använda dig av
programkoden. Domstolsverket ger inga garantier för dess funktion eller
lämplighet och frånsäger sig ansvar för eventuella fel och brister.

Vi är dock tacksamma för feedback och rapporter om eventuella fel.



Installationsansvisningar
-------------------------

Rader som börjar med "$" avser kommandon som skall utföras från ett
terminalfönster).

1. Installera programspråket Python 2.6 (Se http://www.python.org/download/).

2. Installera easy_install/setuptools (Se
http://pypi.python.org/pypi/setuptools och
http://peak.telecommunity.com/DevCenter/EasyInstall)

3. Installera Django
    $ easy_install django

4. Installera Sqllite 3 (Se http://www.sqlite.org/)

6. Öppna filen settings.py och modifiera vid behov de inställningar som börjar
med RINFO.

7. Installera databasschemat:
    $ python manage.py syncdb   

Efter information att tabeller skapas skall du få en fråga om du vill skapa
en 'superuser'. Svara ja på frågan och ange information om användaren.

8. Starta den inbyggda webbservern:
    $ python manage.py runserver

9. Öppna webbläsaren med följande adress: http://127.0.0.1:8000/
Exempelwebbplatsen visas. För att redigera innehåll navigera till
http://127.0.0.1:8000/admin/ och logga in som den användare du skapade i steg 7.



Nästa steg
----------

Applikationen illustrerar hur föreskrifter relateras till varandra och hur
metadata av olika slag kan fångas upp och presenteras på ett standardiserat
sätt. 

Börja med att lågga in i administrationsgränssnittet och skapa lite grunddata
(Författningssamling, några ämnesord och bemyndigandeparagrafer).

Några saker att utgå från:

1. Filen urls.py visar vilka olika typer av länkar som webbplatsen hanterar.
Varje länkformat är kopplat till en metod i rinfo/views.py. 

2. I rinfo/models.py hittar du klasserna för de olika informationsobjekten.
Klassen Myndighetsforeskrift visar några olika typer av metadata och relationer
till andra objekt. 

3. Mallen templates/foreskrift_rdf.xml visar hur en grundläggande post är uppbyggd.

4. Atomfeeden berättar om förändringar som skett med poster i samlingen. Feeden
finns på adressen http://127.0.0.1:8000/feed/. Nya poster, uppdateringar av
poster (skall inte ske om man inte gjort fel tidigare) och radering av poster
(i händelse av en grov felpublicering) gör att ett AtomEntry-objekt skapas.
Dessa sammanställs i en feed i templates/atomfeed.xml.

Eftersom applikationen är baserad på ramverket Django kan det vara bra att
känna till grunderna om detta. Mer information om Django hittar du här:
http://docs.djangoproject.com/en/dev/intro/overview/

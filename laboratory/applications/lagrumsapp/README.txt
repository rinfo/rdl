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

6. Redigera settings.py och ange:
    DATABASE_NAME    Sökväg till databasfilen
    Modifiera vid behov de inställningar som börjar med RINFO

7. Installera databasschemat
    $ python manage.py syncdb

8. Starta den inbyggda webbservern
    $ python manage.py runserver

9. Öppna webbläsaren med följande adress: http://127.0.0.1:8000/
    Exempelwebbplatsen visas. För att redigera innehåll navigera till
http://127.0.0.1/admin/ och logga in som användare "admin" med lösenord
"admin".




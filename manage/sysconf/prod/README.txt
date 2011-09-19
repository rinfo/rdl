########################################################################
Hantera produktionssystemet
########################################################################

Här beskrivs hur produktionsservrarna configureras från början.

Kontinuerlig drift görs med hjälp av <../../deploy>.

Förkrav
========================================================================

Produktionsservrarna ska köra: Debian 5.0.

Initial användare för konfiguration är root.

Gör en initial:

    $ apt-get update

Loggning av systemförändringar
========================================================================

Installera etckeeper:

    $ apt-get install etckeeper

Aktivera loggning av systemförändringar:

    $ etckeeper init
    $ etckeeper commit "initial import"

[Mer information: <http://kitenet.net/~joey/code/etckeeper/> (och t.ex.
<https://help.ubuntu.com/9.04/serverguide/C/etckeeper.html>).]

Ställ in användare
========================================================================

Lägg till användare (rinfo + övriga systembehöriga):

    $ adduser rinfo
    $ export EDITOR=vim
    $ sudo visudo

Ställ in hostname
========================================================================

Ange primärt DNS-namn för maskinen:

    $ sudo vim /etc/hostname


Kontinuerligt arbete
========================================================================

Arbeta sedan som vanligt med apt-get för installation av program, editera
config-filer i "/etc", med tillägget att du regelbundet loggar
systemändringarna med:

    $ etckeeper commit "<reason for changes>"

(Det behövs inte vid installation med apt-get, eftersom etckeeper hakar i denna
med autio-commits.)

Övrigt
========================================================================

Alla övriga förändringar ska göras med deploy-verktyg och formellt loggas
enligt konventioner som definieras där.


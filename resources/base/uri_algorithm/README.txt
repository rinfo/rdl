########################################################################
RInfo - URI-design
########################################################################

TODO: gå igenom materialet (exempeldokument) och diskutera.
Finskriv detta dokument. Ange för- och nackdelar och varför vi valt det vi väljer (i separat FAQ?).

Bakgrund
========================================================================

* Jmf.:

    - <http://lagrummet.se>
    - <http://lagen.nu>

    - <http://www.notisum.se/index2.asp?iParentMenuID=610&iLanguageID=1&sTemplate=/template/index.asp>

        Exempel:
            <http://www.notisum.se/rnp/sls/lag/19980490.htm> [Yrkestrafiklagen (1998:490)]
            <http://www.notisum.se/rnp/sls/lag/19820673.htm#P13_aS2>

        * Filnamnet = på SFS-numret (8 tkn långt). Årtal + 4 tecken (löpnummer med nollor som utfyllnad)

        * kapitel och paragrafer: "#" + "K" + kapitelnummer + "P" + paragrafnummer. T.ex.:
            Kapitel 5: <http://www.notisum.se/rnp/sls/lag/19980490.HTM#K5>
            Kapitel 3, paragraf 3: <http://www.notisum.se/rnp/sls/lag/19980490.HTM#K3P3>
            Ner på stycke: <http://www.notisum.se/rnp/sls/lag/19820673.htm#P13_aS2>

    - (intern-)id plus "namn" som "dokumentet":
        <http://rinfo/id/18.b1bed211329040f5080003602/DFS-2007_08.pdf>

Exempel enligt nedan beskrivna strategi (lagrummet bibehållet):

    http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#
    http://rinfo.lagrummet.se/pub/sfs/1999:175
    http://rinfo.lagrummet.se/serie/sfs

    http://rinfo.lagrummet.se/publ/sfs/1999:175
    http://rinfo.lagrummet.se/publ/kgrf/sfs/1999:175/42
    http://rinfo.lagrummet.se/publ/nja/2007:04

Principer
========================================================================

Artiklar som beskriver sunda URL-strategier generellt:

    http://h3h.net/2007/01/designing-urls-for-multilingual-web-sites/
    http://www.w3.org/Provider/Style/URI.html
    http://www.w3.org/TR/cooluris/
    http://sites.wiwiss.fu-berlin.de/suhl/bizer/pub/LinkedDataTutorial/
    http://www.w3.org/2001/tag/doc/alternatives-discovery.html
    http://www.jenitennison.com/blog/node/47
    PRESTO:
        http://www.oreillynet.com/xml/blog/2008/02/presto_a_www_information_archi.html
    "[..] bringing the European Constitution to the people":
        http://idealliance.org/proceedings/xtech05/papers/02-06-04/

* URI-algoritmen kan delas ut "bitvis" per myndighet
    - Typ "släng på id:t efter denna tag-sträng."
    - eftersom de sällan gör mer än en typ.
    - Så slipper de tänka alls på hur de ska göra (annars måste de ju hitta
        på en mekanism med id:n själva, allihop; som inte är tänkt att användas
        av andra än oss!)

Teori
========================================================================

Grundfrågor och -principer:
    - hur ska <rinfo.?/> partitioneras i segment?
    - alla "root-kataloger":
        - måste ha låst mening
            - och deterministiskt (upptäckbart/genererbart) erbjuda lagring:
                - via segment med enkla beteckningar
                    (segment, enligt <http://www.ietf.org/rfc/rfc3986.txt>)
        - måste hålla över tid
            - Varje segment döps efter en term som den associeras till explicit (i intern data)
              så att administrationen av dem är under total kontroll (om t.ex.
              framtida termer "inkräktar" på befintliga namn kan man styra det här).
            - Detta betyder att segment inte ska genereras från
              löptext-etiketter per automatik, utan kontrolleras.

Grundråd:
    - så lite som möjligt i URI:n (nu plus *ett* steg framåt?)
    - fall inte tillbaks på datumpartitionering för allt
        - okej för modellen som vi ser det nu
    - lagrummet och andra vyer som förenar beständighet
        med konstant utvecklande och omgörande måste ligga
        utanför "grundnamnrymden".
    - åtkomst, content-negotiation m.m.:
        - <http://www.w3.org/TR/swbp-vocab-pub/>

Databeroenden:
    - så få som möjligt
    - bara internt kontrollerade
    - kommer rimligen att vara:
        - modellen
        - termer ("#Avslutad", ...)
        - listor med uri:er och egenskaper+värden:
            - för att kunna generera URI:er av sammansatta nycklar
            - beskriva:
                - domstolar
                - samlingar
                    - författningssamlingar
                    - TODO: enumerera övriga
                    - TODO: ange vilka egenskaper som har dem som range

Språkhantering
    - idag inte inbyggt att en lag finns på flera språk; dock:
        - en canonical URI för lagen i sig är språkoberoende
        - precis som för mediatyp kan representationer på olika språk få egna uri:er
          "under" canonical URI:n.
        - se t.ex. <http://h3h.net/2007/01/designing-urls-for-multilingual-web-sites/>
          för goda designråd kring hur dessa bör se ut. Vår nuvarande rekommendation är t.ex:

            - TODO: se documentation/spec

        - hur hantera samma dokument på olika språk?
            - är det en "representation" av document-resursen?
            - rimligen inte utan en ny document-resurs: går ej att säga 'dc:language "sv"' om de är "samma resurs"! (samma resonemang kan föras om dc:hasFormat..)


Användbara URI:er är:

    -   läsbara

    -   meningsfulla

        Konstruerade utifrån något i dokumentet som är "inneboende". En
        svår filosofisk fråga, men principen är att inte använda föränderliga egenskaper.
        I rinfo-fallet är det oerhört svårt att uppfylla; men vi
        approximerar mot det genom att använda verdertagna formella ID:n
        såsom odisputerade kortnamn (t.ex. Prop., SFS, Ds), författningssamlingsnummer m.m.
        Dessa i sig kan ifrågasättas utifrån dessa principer, men det
        faller utanför den generiska URI-schema-designen och går in på
        domänspecifika frågor. URI-designens "juristiktion" slutar här, och
        vi följer helt enkelt bara gängse praxis utan ytterligare
        värdering.

    -   "hackbara"

        om varje segment har en mening kan man tänka sig att de kan bytas
        ut.. Tänk årtal, publikationsnamn m.m.

        Dock: jmf. "pointer-artimetik". URI:er *ska* betraktas som ogenomskinliga, "atomära"

        Kan vara en "service", men inte alls nödvändig att uppfylla (eller konsekvent uppfylla)

        Notera också att t.ex.::

            <http://rinfo.lagrummet.se/pub/sfs/>

        Inte alls nödvändigen representerar SFS i sig, utan *om något* en
        motsvarande tjänst som serverar dokument som publicerats i SFS. Den
        semantiska relationen mellan dessa är odefinierad. *Kan* anges på
        denna plats med t.ex.RDF, eller en Atom-service-beskriving eller
        motsvarande. Men det är inget *krav*.

    -   Är det tillräckligt med state i URI:n för att inte kräva redirect eller header-inspection?
        Inte en avgjord fråga; men redirects kan vara goda i dessa fall. Dock gör detta det
        svårt/omöjligt att "urlbar-kopiera" visad resurs för att få canonical URI..
        - mime-type
        - språk


Utformning
========================================================================

Partitionering
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

ns/ # TODO: ok? täcker skos-conceptscheman, ontologi-scheman, även xml-scheman..
        # med ns funkar allt formellt här, per datum. Med även menar jag kanske t.ex.
        # GRDDL-scheman m.m.
    modellen/modellerna + övriga centrala begrepp/kategorier/koncept/termer.. skos, ...
    .. gammalt: taxo - men det räcker inte:
        - enl. <http://en.wikipedia.org/wiki/Taxonomy>
        - Taxonomy behöver kombineras med Meronomy
        för att utgöra korrekt klassificeringssystem
        (enl. <http://www.ototsky.mgn.ru/it/21abreast.htm>)

publ/
    Publicerade ting (lagtextdokument..)

org/
    grupp- och organisationsinfo (gruppinfo, t.ex. kommittéer.., domstolar och andra myndigheter)

serie/ # TODO: 'ser'/'saml'?
    collections/series.. listor? set?
    För t.ex.:
        - "SFS - Svensk författningssamling"
        - NJA

ext/
    Externa alias:
        http://rinfo/ext/2007/09/VervaFS (saknar nog ISSN)
        http://rinfo/ext/ap/...
        http://rinfo/ext/celex/...
        http://rinfo/ext/issn/...

Modellinformation (2:a-ordningens data, "metadata")
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

- i segmentet "ns", denna rymd partitioneras med datum (år, månad, namn(delar))
- MALL = <ns/{yyyy/mm/dd}/{namn[/...]}>

"1:a-ordningens" data:
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

- under denna delas upp på så generell typ som möjligt,
    sedan så lite som möjligt för att få en "lokalt unik nyckel"
- framtida partitionering får ta hänsyn till denna, och skulle behovet
    av nya begrepp med samma namn dyka upp får helt enkelt mer precisa
    segment skapas vid sidan om dessa

* Publicerade dokument per dokumenttyp (se kod!):

    - för "publ/" generellt:
        - Definiera "sammansatt nyckel" per dokumenttyp!
            - går det att modellera med detta i åtanke för att (semi-)automatisera?
            - använd dc:identifier som fält för (beräknad forfattningssamling!label + fsNummer)?
        - analysera - för majoritet gäller:
            - segment som representerar (för rättsinfo i sverige) unik, välkänd
                1) samling, t.ex. SFS
                2) om samling kan anses baserad på typ (t.ex. Proposition),
                    anv. kortnamn för denna typ..
                3) vissa typer är speciella, t.ex. konsolideringar,
                    men anv. kortnamn även där.
                4) om ingen samling finns, använd publisher(-kortnamn)..
                ; Obs! Dessa ting måste finnas i välidentifierad grunddata
                som kontrolleras av rinfo.. (se "kontrollerat URI:fiera" nedan)
            - identifiera "naturligt id" ("atomär ordinal"), t.ex. rattsdokumentnummer "1999:175"
                - undantag? T.ex. "(SjöFS) 1900:00 A"?
        - URI:fiera:
            - lowercase:a kortnamn för segment
                .. eller! Helt kontrollerat, med "kataloger" med logik::

                    <http://rinfo.lagrummet.se/publ/sfs/> a sioc:Container;
                        sioc:id "/publ/sfs/";
                        foaf:primaryTopic <http://rinfo.lagrummet.se/serie/sfs>;
                        minter:useProperty rpubl:fsNummer; .

            - atomär ordinal så naturlig som "rimligt"

    - TODO: se också anteckningarna i modellen om Rattsfallsreferat

    - TODO: från växande mängd publicerande källor:

        - tidig idé - nu återupptagen (i.o.m. centralt tilldelade segmentnamn)::
            hd/dom/B333-04
            hovr_skaninge_blekinge/dom/B353-03
            helsingborgs_tr/dom/B1936-01
        - alternativ med "maskinid" för källa, i stil med::
            ap.kb.se/3452456/B353-03

        - TODO: hur bygga - alternativ (kanske båda):
            - på typ om direkt ID finns, t.ex. prop + prop-id
            - på publisher + publisher-id + lokalt ID..
            - på <något-av-ovan> + (typad) egenskap som är funktionell på subjektet (t.ex. referat?..)
            - skiss::
                </publ/{type?}/{by-publisher}/{publisher-id}/{locally-unique-id}>

            - tanke 071002: publisher-id är:
                - värdet på en egenskap angett för den uri för publisher som finns
                    - denna uri kan behöva tilldelas av rinfo-förvaltningen:
                        - t.ex. <http://rinfo/org/dv/hd>
                    - värdet angivet med.. antingen:
                        .. dessa egenskaper är *rimligen* (men inte alltid?) owl:FunctionalProperty
                        - skos:altLabel
                        - awol:term ("i vårt fall"/"vår konvention" trailing path..)
                        - sioc:name/sioc:id? (sioc-id är kanske "dv/hd" eller t.o.m. "org/dv/hd")
                        - egenuppfinnet rinfointernaltechmodel:CompositeKeyPart

    * revisioner av publicerade dokument?

        - konsolideringar av författingar - exempel:
            <sfs/1999:175>
            <konsolideringar/sfs/1999:175/sfs/2006:12>
                |
                `-- Konsolideringsunderlag <sfs/2006:12>
                |
                `- <konsolideringar/sfs/1999:175/sfs/2005:68>
            - "tjänst på detta":
                - <konsolideringar/sfs/1999:175/2007-03-14> REDIRECT-TO <konsolideringar/sfs/1999:175/sfs/2006:12>
                - <latest/sfs/1999:175> REDIRECT-TO <konsolideringar/sfs/1999:175/sfs/2006:12>

        - andra uppdateringar, t.ex. revidering av pulblicerat domslut?
            - nej, inte som resulterar i nya dokument/artefakter
                - vi har rättelseblad som *refererar befintliga ting* och.. ja.. rättar dem

* Stöd-data: andra saker än publicerade dokument! (segment parallella med "/publ/")
    - i org, serie, ext, ...
    - termer&begrepp, domstolar och liknande, författningssamlingar, prop-samlingen (och motsv.)?

    - TODO: ur "Modell - rättsinformation" fr. dec 2006:
      "För referenser till dokument i EUR-LEX skall följande format användas:":
        <http://rinfo.lagrummet.se/ext/eur-lex/31993D0619>

    - Obs! Det är *väldigt* bra om URI:er som skapas men som beskrivs i en
      "överliggande URI" leder till 303:or till denna!


"Content Negotiation"
========================================================================

.. Skäl för att låta representationer ligga URL-hierarkiskt "under" canonical:
    - för att alla "lokal"-relativa enclosure-sökvägar ska funka utan rewrite
      av hyperlänkar i content. T.ex.:

        /publ/sfs/1999/175/
            pdf,sv ::
                href="bilaga_A.pdf"
            bilaga_A.pdf

 http://rinfo.lagrummet.se/

    # canonical:
    /publ/sfs/1999/175

    # controlled name or mechanic suffix:
    /publ/sfs/1999/175/xhtml,sv
    /publ/sfs/1999/175/data,sv.xhtml

    # a different take:
    /repr/xhtml,sv/publ/sfs/1999/175

    # slugs:
    /publ/sfs/1999/175/SFS-2007_175,sv.xhtml
    /publ/sfs-2007_175,sv.xhtml


Öppna frågor
========================================================================

* inkludera versionen av uri-strukturen? Nej; det blir alldeles för instrumentellt..
    (se t.ex. <http://atomserver.codehaus.org/docs/protocol_basics.html>)

    - version som *bas* för (icke "ns"-) URL:ar (logiskt "odelbar" med domännamnet)?
        <http://rinfo.lagrummet.se/publ/sfs/1999/175>
        <http://rinfo.lagrummet.se/v1/publ/sfs/1999/175>
        <http://lagrummet.se/v2/rdata/documents/laws/2028/08/29/stang-ner-fra>
        <http://lagrummet.jämtland/v3/avdf1200sdfsd0sdfc/1>


* Det kan vara riktigt bra om URI:er som skapas men som beskrivs av dokument i en
    "överliggande URI" leder till 303:or till denna!

* Framtida begreppstillägg med liknande kortformer:
    .. SFS - Saklig Förenings-Sammanfattning:
        http://rinfo/pub/sfs-sakl_for/1.28.32
    .. SFS:er får RF-ID:n 2024:
        http://rinfo/rfid/sfs/af24123443cc4354215

TAG-URI:er för källornas ID:n?
------------------------------

E.x.: <tag:lagrummet.se,2008:publ/sfs/1999:175>

Atom-id:n är ofta tag-uri:er.

För att se till att de myndigheterna genererar förutsägbara uri:er för dokumenten.

Gör det material de producerar uniformt utan att hårdkoppla det med url:ar (som kan vara förvirrande i produktionsledet?)?

* RInfo har dock URL:ar som är de officiella uri:erna.

Fragment-IDn
------------

* de bör inte hitta på fragment-identifierare för paragrafer osv!
    .. se nedan

* "inuti" resurserna:

    - fragment i dokumentet:
        - '#' + namn på "eRDF"-rel alt. typ plus ordinal alt. label
        - TODO: mer unikt?
            - gammal överambition:
                [intrinsic type shortname e.g. ("lagparagraf")]-[creation-tstamp]-[creation-ordinal]
            - tanke: åtminstone typ <#rinfo-paragraf-1>
                - lite "eRDF" i fragmentet s.a.s:
                    - egenskapen som "semi-CURIE"
                    - plus ordinal (eller label? nej. position.)
        - "rinfo-hands-off" marknadsextensions för rinfo-URI:er:
            - fragment-uri:er som börjar med "x-"..
                - t.ex. för styckeshänvisningar och annat "vi vet finns men inte kan handskas med"::
                    <#x-p5-s2> owl:sameAs <#xpointer(...)>
            - TODO: var har jag xpointer-tankarna? Bara i HT2007-rapporten?
        - intressanta "delar i dokument"-idéer:
            <http://www.oreillynet.com/xml/blog/2008/02/presto_a_www_information_archi.html>
            <http://www.jenitennison.com/blog/node/47>
            .. <http://idealliance.org/proceedings/xtech05/papers/02-06-04/>

    - fristående "delar" "under" dokumentet:
        http://rinfo/pub/sfs/1999:175/bilaga

Inbyggda tjänster
-----------------

* atom-feeds. sparql. fritextsök?

- Tanken här är om det finns fördelar med att ha tjänster, t.ex. Atom-data
    på de "överliggande segmenten" i rinfo-dok-uri:erna:
        http://rinfo/
        http://rinfo/pub <-- 404 eller?.. atompub?
        http://rinfo/pub/sfs <-- -||-
    - även t.ex. tjänst för att slå upp "senaste konsoliderade form", enligt:
        http://rinfo/pub/kgrf/sfs/1999:175?latest-at=20070914 -> redirect till t.ex.:
            http://rinfo/pub/kgrf/sfs/1999:175/42

* service/ ? ('svc'?)
  * nu: under egen subdomän (service.lagrummet.se)

* bara ett fåtal?
  http://rinfo/service/sparql
  .. fritextsök
  .. ... atom-baserad bläddring?

* resurs-"representationer" (alá dbpedia:s resource som finns som data/page):
    - filändelser?
        about.rdf, content.pdf, content.xhtml, entry.atom
        Obs! I vilket fall, HTTP content-negotiation per resurs för dessa typer!

* ett fåtal *enkla* operationer?
    http://rinfo/pub/sfs/1999:175?aspect=formats
        - se ovan; går alltså att ersätta med t.ex. atom via accepts (eller *kanske* ändelse..)
    http://rinfo/pub/sfs/1999:175?fragment=id:t <-- bara *delen*



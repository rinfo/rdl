# coding=utf-8
from django.db import models
from django.db.models import permalink
from django.template import loader, Context
from django.conf import settings
from datetime import datetime
from django.contrib.sites.models import Site
from django.core.files import File
import hashlib
from django.utils.feedgenerator import rfc3339_date


class Forfattningssamling(models.Model):
    """Modell för författningssamlingar."""

    # Namn på författningssamling
    titel=models.CharField(
            max_length=255, 
            unique=True, 
            help_text="""Namn på författningssamling, t.ex.
            <em>Exempelmyndighetens författningssamling</em>""")

    # Kortnamn på författningssamling, t.ex. "EXFS"
    kortnamn=models.CharField(
                max_length=10, 
                unique=True, 
                help_text="""T.ex. <em>EXFS</em>""")

    # Författningssamlingens unika identifierare, t.ex.
    # "http://rinfo.lagrummet.se/serie/fs/ra-fs". Denna erhålls från
    # projektet.
    identifierare=models.URLField(
            verify_exists=False, 
            max_length=255,
            unique=True, 
            help_text="Erhålls från Domstolsverket")

    def __unicode__(self):
        return u'%s %s' % (self.titel, self.kortnamn)

    # Inställningar för etiketter i administrationsgränssnittet.
    class Meta:
        verbose_name=u"Författnings-samling"
        verbose_name_plural=u"Författnings-samlingar"



class Amnesord(models.Model):
    """Modell för ämnesord."""

    # Namn, t.ex. "Arkivgallring"
    titel=models.CharField(
            max_length=255, 
            unique=True, 
            help_text="Ämnesordet")

    # Beskrivning/definition av ämnesordet
    beskrivning=models.TextField(blank=True)

    def __unicode__(self):
        return self.titel

    # Inställningar för etiketter i administrationsgränssnittet.
    class Meta:
        verbose_name=u"Ämnesord"
        verbose_name_plural=u"Ämnesord"


    

class Bemyndigandeparagraf(models.Model):
    """Modell för att hantera bemyndigandeparagrafer."""

    # Namn, t.ex. "Arkivförordningen"
    titel=models.CharField(max_length=255,
                             help_text="""T.ex. <em>Arkivförordningen</em>""")

    # SFS-nummer, t.ex. "1991:446"
    sfsnummer=models.CharField("SFS-nummer", max_length=10, blank=False,
            help_text="T.ex. <em>1991:446</em>")

    # Paragrafnummer, t.ex. "11"
    paragrafnummer=models.CharField(max_length=10, blank=True,
            help_text="T.ex. <em>12</em>")
    
    def __unicode__(self):
        return u"%s (%s) %s" % (self.titel, self.sfsnummer, self.paragrafnummer)

    # Inställningar för etiketter i administrationsgränssnittet.
    class Meta:
        verbose_name=u"Bemyndigandeparagraf"
        verbose_name_plural=u"Bemyndigandeparagrafer"



class Myndighetsforeskrift(models.Model):
    """Modell för myndighetsföreskrifter. Denna hanterar det huvudsakliga
    innehållet i en författningssamling - själva föreskrifterna. Den kan enkelt
    utökas med fler egenskaper."""

    # Föreskriftens officiella titel
    titel=models.CharField(
            max_length=512, 
            unique=True, 
            help_text="""T.ex. <em>Exempelmyndighetens föreskrifter och
            allmänna råd om arkiv hos statliga myndigheter;</em>""")

    # Författningssamlingsnummer, t.ex. "2006:6"
    fsnummer=models.CharField("FS-nummer", max_length=10,
            unique=True, blank=False, help_text="T.ex. <em>2006:6</em>")

    # Utfärdandedatum, t.ex. 2007-02-09 
    utfardandedag=models.DateField("Utfärdandedag", blank=False)

    # Ikraftträdandedatum, t.ex. 2007-02-01
    ikrafttradandedag=models.DateField("Ikraftträdande-dag", blank=False)

    # Utkom från tryck datum, t.ex. 2007-02-09
    utkom_fran_tryck=models.DateField("Utkom från tryck", blank=False)

    # Författningssamling (referens till post i de upprättade
    # författningssamlingarna)
    forfattningssamling=models.ForeignKey(Forfattningssamling, blank=False,
            verbose_name=u"författningssamling")

    # Bemyndiganden (referenser till bemyndigandeparagrafer)
    bemyndigandeparagrafer=models.ManyToManyField(Bemyndigandeparagraf,
            blank=False, verbose_name=u"referenser till bemyndiganden")

    # PDF-version av dokumentet
    dokument=models.FileField(u"PDF-version av föreskrift",
            upload_to="dokument", 
            blank=False, 
            help_text="""Se till att dokumentet är i PDF/A format.""")

    # Koppling till ämnesord
    amnesord=models.ManyToManyField(Amnesord, blank=True, verbose_name=u"ämnesord")

    # Returnera bara årtalet från ikraftträdandedagen
    def ikrafttradandear(self):
        return self.ikrafttradandedag.year

    # Metod för att erhålla URL:en till en eneksild föreskrift
    @models.permalink
    def get_absolute_url(self): 
        return ('lagrumsapp.rinfo.views.foreskrift',
                [str(self.forfattningssamling.kortnamn), str(self.fsnummer)])

    # Metod för att skapa rättsinformationssystemets unika identifierare för denna post
    def get_rinfo_uri(self):
        return settings.RINFO_BASE_URI + self.fsnummer

    # Metod för att returnera textrepresentation av en föreskrift (används i
    # admin-gränssnittets listor)
    def __unicode__(self):
        return u'%s %s' % (self.fsnummer, self.titel)

    def to_rdfxml(self):
        """Metod för att skapa den standardiserade metadataposten om denna
        föreskrift."""

        template=loader.get_template('foreskrift_rdf.xml')
        context=Context({ 'foreskrift': self, 'publisher_uri':
            settings.RINFO_ORG_URI, 'rinfo_base_uri': settings.RINFO_BASE_URI})
        return template.render(context)


    def save(self, *args, **kw):
        """Override av save-metoden. I samband med att en föreskrift
        sparas/uppdateras skriver vi även ner ett nytt Atom entry så att
        Atom-feeden är uppdaterad."""

        # Då posten publicerades (nu, om det är en ny post)
        published=datetime.now()

        # Kolla om detta är en uppdatering eller ett nytt dokument.
        if self.id:
            # Uppdatering - hitta publiceringsdatumet från tidigare post.
            try:
                foreskrift_entries=AtomEntry.objects.filter(
                        myndighetsforeskrift=self).order_by("published")
                published=foreskrift_entries[0].published
            except AtomEntry.DoesNotExist:
                print u"Kan inte hitta AtomEntry för föreskrift med id %s" % (self.id,)

        # Spara myndighetsföreskriften med den vanliga save-metoden.
        super(Myndighetsforeskrift, self).save(*args, **kw)

        # Beräkna md5 för dokument och RDF
        md5=hashlib.md5()
        md5.update(open(self.dokument.path, 'rb').read())
        dokument_md5=md5.hexdigest()

        md5=hashlib.md5()
        rdfxml=self.to_rdfxml()
        md5.update(rdfxml.encode('utf-8'))
        rdf_md5=md5.hexdigest()

        # Skapa AtomEntry-posten
        entry=AtomEntry(  myndighetsforeskrift=self,
                            updated=datetime.now(),
                            published=published,
                            entry_id=self.get_absolute_url(),
                            content_md5=dokument_md5,
                            rdf_length=len(rdfxml),
                            rdf_md5=rdf_md5)

        # Spara AtomEntry för denna aktivitet
        entry.save()


    def delete(self, *args, **kw):
        """Override av delete-metoden. I samband med att en föreskrift
        raderas skriver vi ner ett nytt Atom entry så att konsumenter av feeden
        får information om att en post raderats."""
        
        # PLocka upp ID:t för föreskriften innan den raderas
        entry_id=self.get_rinfo_uri()

        # Radera myndighetsföreskriften med den vanliga delete-metoden.
        super(Myndighetsforeskrift, self).delete(*args, **kw)

        # Skapa AtomEntry-posten
        entry=AtomEntry(    myndighetsforeskrift=None,
                            updated=datetime.now(),
                            published=datetime.now(),
                            content_md5="",
                            rdf_length=0,
                            rdf_md5="",
                            entry_id=entry_id)

        # Spara AtomEntry för denna aktivitet
        entry.save()

    # Några inställningar för beteckningar i admin-gränssnittet
    class Meta:
        verbose_name=u"Myndighetsföreskrift"
        verbose_name_plural=u"Myndighetsföreskrifter"




class AtomEntry(models.Model):
    """En klass för att skapa ett Atom entry för feeden. Dessa objekt skapas
    automatiskt i samband med att en föreskrift sparas, uppdateras eller
    raderas. Se metoden Myndighetsforeskrift.save()"""
    
    myndighetsforeskrift=models.ForeignKey(Myndighetsforeskrift, blank=True)
    updated=models.DateTimeField(blank=False)
    published=models.DateTimeField(blank=False)
    entry_id=models.CharField(max_length=512, blank=False)
    content_md5=models.CharField(max_length=32, blank=False)
    rdf_length=models.PositiveIntegerField()
    rdf_md5=models.CharField(max_length=32, blank=False)


    def to_entryxml(self):
        """Skapa en XML-representation av ett entry enligt Atom-standarden. Se
        mallen i foreskrift_entry.xml"""

        template=loader.get_template('foreskrift_entry.xml')
        context=Context({ 'foreskrift': self.myndighetsforeskrift,
                            'updated': rfc3339_date(self.updated), 
                            'published': rfc3339_date(self.published), 
                            'entry_id': self.entry_id, 
                            'content_md5': self.content_md5, 
                            'rdf_length': self.rdf_length, 
                            'rdf_md5': self.rdf_md5, 
                            'rinfo_base_uri': settings.RINFO_BASE_URI,
                            'rinfo_site_url': settings.RINFO_SITE_URL})
        return template.render(context)

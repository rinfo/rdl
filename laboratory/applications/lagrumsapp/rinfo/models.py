# coding=utf-8

from django.db import models
from django.db.models import permalink

class Forfattningssamling(models.Model):
    # Namn på författningssamling
    titel = models.CharField(max_length=255, unique=True, help_text="Namn på författningssamling, t.ex. <em>Riksarkivets författningssamling'</em>")

    # Kortnamn på författningssamling, t.ex. "RA-FS"
    kortnamn = models.CharField(max_length=10, unique=True, help_text="T.ex. <em>RA-FS</em>")

    # Författningssamlingens unika identifierare, t.ex.
    # "http://rinfo.lagrummet.se/serie/fs/ra-fs". Denna erhålls från
    # projektet.
    identifierare = models.URLField(verify_exists=False, max_length=255, unique=True, help_text="Erhålls från Domstolsverket")

    def __unicode__(self):
        return u'%s %s' % (self.titel, self.kortnamn)

    class Meta:
        verbose_name = u"Författningssamling"
        verbose_name_plural = u"Författningssamlingar"



class Amnesord(models.Model):
    # Namn, t.ex. "Arkivgallring"
    titel = models.CharField(max_length=255, unique=True, help_text="Ämnesordets titel")

    # Beskrivning/definition av ämnesordet
    beskrivning = models.TextField(blank=True)

    def __unicode__(self):
        return self.titel

    class Meta:
        verbose_name = u"Ämnesord"
        verbose_name_plural = u"Ämnesord"


    

class Bemyndigandeparagraf(models.Model):
    # Namn, t.ex. "Arkivförordningen"
    titel = models.CharField(max_length=255, help_text="T.ex. <em>Arkivförordningen</em>")

    # SFS-nummer, t.ex. "1991:446"
    sfsnummer = models.CharField("SFS-nummer", max_length=10, blank=False, help_text="T.ex. <em>1991:446</em>")

    # Paragrafnummer, t.ex. "11"
    paragrafnummer = models.CharField(max_length=10, blank=True, help_text="T.ex. <em>12</em>")
    
    def __unicode__(self):
        return u"%s (%s) %s" % (self.titel, self.sfsnummer, self.paragrafnummer)

    class Meta:
        verbose_name = u"Bemyndigandeparagraf"
        verbose_name_plural = u"Bemyndigandeparagrafer"



class Myndighetsforeskrift(models.Model):
    # Foreskriftens titel
    titel = models.CharField(max_length=512, unique=True)

    # Författningssamlingsnummer, t.ex. "2006:6"
    fsnummer = models.CharField("Författningssamlingsnummer", max_length=10, unique=True, blank=False, help_text="T.ex. <em>2006:6</em>")

    # Utfärdandedatum, t.ex. 2007-02-09 
    utfardandedag = models.DateField("Utfärdandedag", blank=False)

    # Ikraftträdandedatum, t.ex. 2007-02-01
    ikrafttradandedag = models.DateField("Ikraftträdandedag", blank=False)

    # Utkom från tryck datum, t.ex. 2007-02-09
    utkom_fran_tryck = models.DateField("Utkom från tryck", blank=False)

    # Författningssamling (referens till post i de upprättade författningssamlingarna)
    forfattningssamling = models.ForeignKey(Forfattningssamling, blank=False, verbose_name=u"författningssamling")

    # Bemyndiganden (referenser till bemyndigandeparagrafer)
    bemyndigandeparagrafer = models.ManyToManyField(Bemyndigandeparagraf, blank=False, verbose_name=u"referenser till bemyndiganden")

    # PDF-version av dokumentet
    dokument = models.FileField(u"PDF-version av föreskrift", upload_to="dokument", blank=False, help_text=u"Se till att dokumentet är i PDF/A format.")

    # Koppling till ämnesord
    amnesord = models.ManyToManyField(Amnesord, blank=True, verbose_name=u"ämnesord")

    def ikrafttrandendear(self):
        return str(self.ikrafttrandendedag.year)

    @models.permalink
    def get_absolute_url(self): 
        return ('lagrumsapp.rinfo.views.item', [str(self.forfattningssamling.kortnamn), str(self.fsnummer)])

    def __unicode__(self):
        return u'%s %s' % (self.fsnummer, self.titel)

    class Meta:
        verbose_name = u"Myndighetsföreskrift"
        verbose_name_plural = u"Myndighetsföreskrifter"



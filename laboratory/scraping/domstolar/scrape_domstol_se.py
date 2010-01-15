# -*- coding: UTF-8 -*-
from lxml import etree
from lxml.cssselect import CSSSelector as css
import urllib2
from cookielib import CookieJar
from urlparse import urljoin


urllib2.install_opener(
        urllib2.build_opener(urllib2.HTTPCookieProcessor(CookieJar())) )

parser = etree.HTMLParser()

def get_document(url):
    return etree.parse(urllib2.urlopen(url), parser)

def scrape_organization_info(url):
    doc = get_document(url)
    org = {}
    try:
        org['name'] = css("#voicearea h1 span")(doc)[0].text
        for link in css("#voicearea div.contactinfo a[href]")(doc):
            ref = link.get('href')
            if ref.startswith('mailto:'):
                org['mailto'] = ref.replace('mailto:', '').strip()
            elif ref.startswith('http'):
                if not "eniro.se/" in ref and not "hitta.se/" in ref:
                    org['url'] = ref
            else:
                pass #print " # unexpected href: %r" % ref
        return org
    except:
        print "ERROR in content from <%s>:" % url
        print etree.tostring(doc)
        raise

def generate_resources(base_url, resource_sets):
    for name, postid, page_urls in resource_sets:
        for page_url in page_urls:
            page_url = urljoin(base_url, page_url)
            try:
                org = scrape_organization_info(page_url)
            except:
                print "ERROR when processing <%s>:" % page_url
                raise
            org['via'] = page_url
            yield org

base_url = "http://www.domstol.se/templates/DV_ContactSearch____518.aspx"
resource_sets = [

    ("Tingsrätt", '7', [
        "/templates/DV_ContactInfo____546.aspx", #title="Alingsås tingsrätt"
        "/templates/DV_ContactInfo____4339.aspx", #title="Attunda tingsrätt"
        "/templates/DV_ContactInfo____534.aspx", #title="Blekinge tingsrätt"
        "/templates/DV_ContactInfo____547.aspx", #title="Borås tingsrätt"
        "/templates/DV_ContactInfo____524.aspx", #title="Eksjö tingsrätt"
        "/templates/DV_ContactInfo____578.aspx", #title="Eskilstuna tingsrätt"
        "/templates/DV_ContactInfo____585.aspx", #title="Falu tingsrätt"
        "/templates/DV_ContactInfo____581.aspx", #title="Gotlands tingsrätt"
        "/templates/DV_ContactInfo____563.aspx", #title="Gällivare tingsrätt"
        "/templates/DV_ContactInfo____554.aspx", #title="Gävle tingsrätt"
        "/templates/DV_ContactInfo____548.aspx", #title="Göteborgs tingsrätt"
        "/templates/DV_ContactInfo____544.aspx", #title="Halmstads tingsrätt"
        "/templates/DV_ContactInfo____564.aspx", #title="Haparanda tingsrätt"
        "/templates/DV_ContactInfo____536.aspx", #title="Helsingborgs tingsrätt"
        "/templates/DV_ContactInfo____555.aspx", #title="Hudiksvalls tingsrätt"
        "/templates/DV_ContactInfo____537.aspx", #title="Hässleholms tingsrätt"
        "/templates/DV_ContactInfo____2110.aspx", #title="Jönköpings tingsrätt"
        "/templates/DV_ContactInfo____528.aspx", #title="Kalmar tingsrätt"
        "/templates/DV_Kansli____9472.aspx", #title="Kiruna, tingsställe"
        "/templates/DV_ContactInfo____538.aspx", #title="Kristianstads tingsrätt"
        "/templates/DV_ContactInfo____522.aspx", #title="Linköpings tingsrätt"
        "/templates/DV_ContactInfo____565.aspx", #title="Luleå tingsrätt"
        "/templates/DV_ContactInfo____539.aspx", #title="Lunds tingsrätt"
        "/templates/DV_ContactInfo____540.aspx", #title="Lunds tingsrätt - Landskrona"
        "/templates/DV_ContactInfo____560.aspx", #title="Lycksele tingsrätt"
        "/templates/DV_ContactInfo____541.aspx", #title="Malmö tingsrätt"
        "/templates/DV_ContactInfo____586.aspx", #title="Mora tingsrätt"
        "/templates/DV_ContactInfo____549.aspx", #title="Mölndals tingsrätt"
        "/templates/DV_ContactInfo____4343.aspx", #title="Nacka tingsrätt"
        "/templates/DV_ContactInfo____523.aspx", #title="Norrköpings tingsrätt"
        "/templates/DV_ContactInfo____569.aspx", #title="Norrtälje tingsrätt"
        "/templates/DV_ContactInfo____580.aspx", #title="Nyköpings tingsrätt"
        "/templates/DV_Kansli____10501.aspx", #title="Nyköpings tingsrätt - Katrineholm"
        "/templates/DV_Kansli____3044.aspx", #title="Oskarshamn, tingsställe"
        "/templates/DV_ContactInfo____531.aspx", #title="Skaraborgs tingsrätt"
        "/templates/DV_Kansli____9329.aspx", #title="Skaraborgs tingsrätt - Lidköping"
        "/templates/DV_Kansli____9328.aspx", #title="Skaraborgs tingsrätt - Mariestad"
        "/templates/DV_ContactInfo____561.aspx", #title="Skellefteå tingsrätt"
        "/templates/DV_ContactInfo____4342.aspx", #title="Solna tingsrätt"
        "/templates/DV_ContactInfo____572.aspx", #title="Stockholms tingsrätt"
        "/templates/DV_Kansli____4332.aspx", #title="Stockholms tingsrätt - Avdelning 1"
        "/templates/DV_Kansli____4333.aspx", #title="Stockholms tingsrätt - Avdelning 2"
        "/templates/DV_Kansli____4334.aspx", #title="Stockholms tingsrätt - Avdelning 3"
        "/templates/DV_Kansli____4336.aspx", #title="Stockholms tingsrätt - Avdelning 4"
        "/templates/DV_Kansli____4340.aspx", #title="Stockholms tingsrätt - Avdelning 5"
        "/templates/DV_ContactInfo____558.aspx", #title="Sundsvalls tingsrätt"
        "/templates/DV_ContactInfo____4337.aspx", #title="Södertälje tingsrätt"
        "/templates/DV_ContactInfo____4338.aspx", #title="Södertörns tingsrätt"
        "/templates/DV_ContactInfo____551.aspx", #title="Uddevalla tingsrätt"
        "/templates/DV_ContactInfo____562.aspx", #title="Umeå tingsrätt"
        "/templates/DV_ContactInfo____577.aspx", #title="Uppsala tingsrätt"
        "/templates/DV_ContactInfo____545.aspx", #title="Varbergs tingsrätt"
        "/templates/DV_ContactInfo____552.aspx", #title="Vänersborgs tingsrätt"
        "/templates/DV_Kansli____3004.aspx", #title="Vänersborgs tingsrätt - Fastighetsdomstolen"
        "/templates/DV_Kansli____3005.aspx", #title="Vänersborgs tingsrätt - Miljödomstolen"
        "/templates/DV_ContactInfo____642.aspx", #title="Värmlands tingsrätt"
        "/templates/DV_Kansli____3045.aspx", #title="Västervik, tingsställe"
        "/templates/DV_ContactInfo____582.aspx", #title="Västmanlands tingsrätt"
        "/templates/DV_ContactInfo____527.aspx", #title="Växjö tingsrätt"
        "/templates/DV_ContactInfo____543.aspx", #title="Ystads tingsrätt"
        "/templates/DV_ContactInfo____556.aspx", #title="Ångermanlands tingsrätt"
        "/templates/DV_Kansli____557.aspx", #title="Ångermanlands tingsrätt - Örnsköldsvik"
        "/templates/DV_ContactInfo____533.aspx", #title="Örebro tingsrätt"
        "/templates/DV_ContactInfo____559.aspx", #title="Östersunds tingsrätt"
    ]),

    ("Hovrätt", '8', [
        "/templates/DV_ContactInfo____520.aspx", #title="Göta hovrätt"
        "/templates/DV_ContactInfo____589.aspx", #title="Hovrätten för Nedre Norrland"
        "/templates/DV_ContactInfo____588.aspx", #title="Hovrätten för Västra Sverige"
        "/templates/DV_ContactInfo____590.aspx", #title="Hovrätten för Övre Norrland"
        "/templates/DV_ContactInfo____587.aspx", #title="Hovrätten över Skåne och Blekinge"
    ]),

    ("Länsrätt", '9', [
        "/templates/DV_ContactInfo____615.aspx", #title="Länsrätten i Blekinge län"
        "/templates/DV_ContactInfo____605.aspx", #title="Länsrätten i Dalarnas län"
        "/templates/DV_ContactInfo____598.aspx", #title="Länsrätten i Gotlands län"
        "/templates/DV_ContactInfo____606.aspx", #title="Länsrätten i Gävleborgs län"
        "/templates/DV_ContactInfo____602.aspx", #title="Länsrätten i Göteborg"
        "/templates/DV_Kansli____2829.aspx", #title="Länsrätten i Göteborg, Migrationsdomstolen"
        "/templates/DV_ContactInfo____601.aspx", #title="Länsrätten i Hallands län"
        "/templates/DV_ContactInfo____608.aspx", #title="Länsrätten i Jämtlands län"
        "/templates/DV_ContactInfo____612.aspx", #title="Länsrätten i Jönköpings län"
        "/templates/DV_ContactInfo____614.aspx", #title="Länsrätten i Kalmar län"
        "/templates/DV_ContactInfo____613.aspx", #title="Länsrätten i Kronobergs län"
        "/templates/DV_ContactInfo____616.aspx", #title="Länsrätten i Mariestad"
        "/templates/DV_ContactInfo____610.aspx", #title="Länsrätten i Norrbottens län"
        "/templates/DV_ContactInfo____600.aspx", #title="Länsrätten i Skåne län"
        "/templates/DV_ContactInfo____595.aspx", #title="Länsrätten i Stockholms län"
        "/templates/DV_ContactInfo____597.aspx", #title="Länsrätten i Södermanlands län"
        "/templates/DV_ContactInfo____596.aspx", #title="Länsrätten i Uppsala län"
        "/templates/DV_ContactInfo____603.aspx", #title="Länsrätten i Vänersborg"
        "/templates/DV_ContactInfo____604.aspx", #title="Länsrätten i Värmlands län"
        "/templates/DV_ContactInfo____609.aspx", #title="Länsrätten i Västerbottens län"
        "/templates/DV_ContactInfo____607.aspx", #title="Länsrätten i Västernorrlands län"
        "/templates/DV_ContactInfo____599.aspx", #title="Länsrätten i Västmanlands län"
        "/templates/DV_ContactInfo____617.aspx", #title="Länsrätten i Örebro län"
        "/templates/DV_ContactInfo____611.aspx", #title="Länsrätten i Östergötlands län"
        "/templates/DV_Kansli____2830.aspx", #title="Migrationsdomstolen vid Länsrätten i Skåne län"
        "/templates/DV_Kansli____2831.aspx", #title="Migrationsdomstolen vid Länsrätten i Stockholms län"
    ]),

    ("Kammarrätt", '10', [
        "/templates/DV_ContactInfo____592.aspx", #title="Kammarrätten i Göteborg"
        "/templates/DV_ContactInfo____594.aspx", #title="Kammarrätten i Jönköping"
        "/templates/DV_ContactInfo____5362.aspx", #title="Kammarrätten i Stockholm, inklusive Migrationsöverdomstolen"
        "/templates/DV_ContactInfo____593.aspx", #title="Kammarrätten i Sundsvall"
    ]),

    ("Regeringsrätten", '11', [
        "http://www.domstol.se/templates/DV_ContactInfo____618.aspx",
    ]),

    ("Högsta domstolen", '12', [
        "http://www.domstol.se/templates/DV_ContactInfo____619.aspx",
    ]),

    ("Domstolsverket", '13', [
        "/templates/DV_ContactInfo____641.aspx", #title="Domstolsverket "
        "/templates/DV_Kansli____9248.aspx", #title="Domstolsverket kontor i Malmö"
        "/templates/DV_Kansli____2212.aspx", #title="Domstolsverkets kontor i Göteborg"
        "/templates/DV_Kansli____2211.aspx", #title="Domstolsverkets kontor i Stockholm"
    ]),

    ("Hyres- och arrendenämnden", '14', [
        "/templates/DV_ContactInfo____626.aspx", #title="Hyresnämnden i Göteborg"
        "/templates/DV_ContactInfo____623.aspx", #title="Hyresnämnden i Jönköping"
        "/templates/DV_ContactInfo____622.aspx", #title="Hyresnämnden i Linköping"
        "/templates/DV_ContactInfo____625.aspx", #title="Hyresnämnden i Malmö"
        "/templates/DV_ContactInfo____620.aspx", #title="Hyresnämnden i Stockholm"
        "/templates/DV_ContactInfo____629.aspx", #title="Hyresnämnden i Sundsvall"
        "/templates/DV_ContactInfo____630.aspx", #title="Hyresnämnden i Umeå"
        "/templates/DV_ContactInfo____621.aspx", #title="Hyresnämnden i Västerås"

    ]),

    ("Inskrivningsmyndigheten", '15', [
        "/templates/DV_ContactInfo____632.aspx", #title="IM Eksjö"
        "/templates/DV_ContactInfo____633.aspx", #title="IM Härnösand"
        "/templates/DV_ContactInfo____634.aspx", #title="IM Hässleholm"
        "/templates/DV_ContactInfo____635.aspx", #title="IM Mora"
        "/templates/DV_ContactInfo____636.aspx", #title="IM Norrtälje"
        "/templates/DV_ContactInfo____637.aspx", #title="IM Skellefteå"
        "/templates/DV_ContactInfo____638.aspx", #title="IM Uddevalla"

    ]),

    ("Rättshjälpsorganisationen", '16', [
        "/templates/DV_ContactInfo____639.aspx", #title="Rättshjälpsmyndigheten"
        "/templates/DV_ContactInfo____640.aspx", #title="Rättshjälpsnämnden"
    ]),

    ("Övriga domstolar", '17', [
        "/templates/DV_ContactInfo____1141.aspx", #title="Arbetsdomstolen"
        "/templates/DV_ContactInfo____1142.aspx", #title="Marknadsdomstolen"
        "/templates/DV_ContactInfo____1143.aspx", #title="Patentbesvärsrätten"
    ]),
]


# required to set cookie for DV_Kansli*-pages..
search_url = "http://www.domstol.se/templates/DV_ContactSearch____518.aspx"
urllib2.urlopen(search_url)

import simplejson
orgs = []
for org in generate_resources(base_url, resource_sets):
    orgs.append(org)
print simplejson.dumps(orgs, indent=4, separators=(',', ': '))

# NOTE: original attempt to scrape via form (skipped since .NET forms are.. hell)
#from httplib2 import Http
#def get_document(url, method="GET", data=None, headers=None):
#    h = Http()
#    response, content = h.request(url, method, data, headers=headers)
#    return etree.fromstring(content, parser)
#form_name = "defaultframework:Content1:ProfileFinder:TypeOfAuthority"
#form_id = form_name.replace(':', '_')
#table_seach_res_id = "defaultframework_Content1_ProfileFinder_pagesearchpanel"
#doc = get_document(search_url)
#for option in css('#defaultframework_Content1_ProfileFinder_TypeOfAuthority option')(doc):
#    data = "form_name=%s" % option.get('value')
#    res_doc = get_document(search_url, "POST", data)
#    for link in css(
#            '#defaultframework_Content1_ProfileFinder_pagesearchpanel a[href].listheading')(res_doc):
#        print link
#    break


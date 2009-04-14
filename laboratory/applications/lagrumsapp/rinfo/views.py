# coding=utf-8
from django.http import HttpResponse
from django.shortcuts import render_to_response
import datetime

from rinfo.models import Myndighetsforeskrift, Forfattningssamling

def index(request):
    """Visa startsidan."""

    #Senast utgivna (då de utkom från tryck)
    senaste_myndighetsforeskrifter = Myndighetsforeskrift.objects.all().order_by("-utkom_fran_tryck")[:10]
    
    return render_to_response('index.html', locals())



def item(request, fskortnamn, fsnummer):
    """Visa enskild föreskrift i författningssamling."""

    # Hämta författningssamlingen
    fs = Forfattningssamling.objects.get(kortnamn=fskortnamn)

    # Hämta föreskriften
    foreskrift = Myndighetsforeskrift.objects.get(fsnummer=fsnummer, forfattningssamling=fs)

    return render_to_response('foreskrift.html', locals())


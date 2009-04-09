from django.contrib import admin
from rinfo.models import Amnesord, Forfattningssamling, Myndighetsforeskrift, Bemyndigandeparagraf

class ForfattningssamlingAdmin(admin.ModelAdmin):
    list_display = ('titel', 'kortnamn', 'identifierare')
    ordering = ('titel',)

class AmnesordAdmin(admin.ModelAdmin):
    list_display = ('titel', 'beskrivning')
    ordering = ('titel',)
    search_fields = ('titel', 'beskrivning',)
       
class MyndighetsforeskriftAdmin(admin.ModelAdmin):
    list_display = ('fsnummer', 'titel','utfardandedag', 'ikrafttradandedag', 'utkom_fran_tryck')
    list_filter = ('utfardandedag',)
    ordering = ('fsnummer', 'titel')
    search_fields = ('titel', 'fsnummer',)


admin.site.register(Amnesord, AmnesordAdmin)
admin.site.register(Forfattningssamling, ForfattningssamlingAdmin)
admin.site.register(Myndighetsforeskrift, MyndighetsforeskriftAdmin)
admin.site.register(Bemyndigandeparagraf)

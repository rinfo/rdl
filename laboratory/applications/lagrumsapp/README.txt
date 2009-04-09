
Installationsansvisningar
-------------------------

Rader som börjar med "$" avser kommandon som skall utföras från ett terminalfönster).

1. Installera programspråket Python

2. Installera easy_install

3. Installera Django

4. Installera Sqllite 3

5. Installera applikationen

6. Redigera settings.py och ange:
    1. DATABASE_NAME    Sökväg till databasfilen
    2. RINFO_ORG_URI    Identifieraren (i URI-format) för din organisation (erhålls från projektet)

7. Installera databasschemat
    $ python manage.py syncdb

8. Starta den inbyggda webbservern
    $ python manage.py runserver

9. Öppna webbläsaren med följande adress: http://127.0.0.1:8000/admin för att redigera innehåll. Logga in som admin/admin.

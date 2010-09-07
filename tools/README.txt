########################################################################
README - Tools
########################################################################

Introduction
========================================================================

These tools are for instrumental (development) use of the rinfo packages.


Running Groovy outside of Maven
========================================================================

Option A: Use Groovy's Grape With Local Maven Repo
------------------------------------------------------------------------

Groovy's dependency mechanism (using ``@Grab``) annotations can be used with
your local Maven repository. To do this, locate the file:

    <$HOME/.groovy/grapeConfig.xml>

If it doesn't exist, see:

    <http://groovy.codehaus.org/Grape>

for how to create a default version.

Then add, directly after ``ivysettings/settings``, the following directive::

    <caches useOrigin="true"/>

And, in ``/ivysettings/resolvers/chain``, before the first ``filesystem``, add::

    <ibiblio name="local" root="file:${user.home}/.m2/repository/" m2compatible="true"/>

The path in the root property above should match the location for your local
maven repository. You may have changed that from the default home directory.

That should make all Maven dependencies locatable (provided that you have ``mvn
install``:ed your packages; see <packages/java/README.txt>).


Option B: Use A Pathing Jar
------------------------------------------------------------------------

(This makes for a speedier roundtrip when using groovy for testing and
prototyping.)

You can use the ``classpathjar.groovy`` script to create a jar file containing
manifest references to a maven2 package and its dependencies. Put this on the
classpath (e.g. $CLASSPATH, using the ``-cp`` param to the groovy exe, or put
it in ``~/.groovy/lib/``).

Before running the classpathjar.groovy script make sure you run ``mvn install``
in the packages/java folder.

Example:

1. Create a jar referencing dependencies in rinfo-service::

    $ groovy classpathjar.groovy ../packages/java/rinfo-service/ ~/.groovy/lib/rinfoclasspath.jar

2. Run groovy as usual with::

    $ groovy <path-to-script>


Building the PDF documents
========================================================================

To build the PDF documents, use the build_rinfo_docs.groovy script (This can
also be done using the fabric command "build_docs", see ../manage/README.txt).
The documentation will be created in the directory ../_build relative to the 
tools directory.

The stylesheets used for PDF generation requires that font files for Garamond
and Trebuchet MS are present. The needed files are:

"gara.ttf", "garait.ttf", "garabd.ttf", "trebuc.ttf", "trebucit.ttf",
"trebucbi.ttf" and "trebucbd.ttf".

Place these files in the same directory as the ``build_rinfo_docs.groovy``
script.

Obtaining the Fonts
------------------------------------------------------------------------

Windows
~~~~~~~~~~

Copy the fonts named above from "C:\WINDOWS\FONTS\"

Mac OS X
~~~~~~~~

Trebuchet is generally available as TTF. Copy these::

    cp "/Library/Fonts/Trebuchet MS.ttf" fonts/trebuc.ttf
    cp "/Library/Fonts/Trebuchet MS Bold.ttf" fonts/trebucbd.ttf
    cp "/Library/Fonts/Trebuchet MS Italic.ttf" fonts/trebucit.ttf
    cp "/Library/Fonts/Trebuchet MS Bold Italic.ttf" fonts/trebucbi.ttf

Garamond may only be available as a "Font Suitcase" (and the PDF generator
requires standalone TTF files). If so, obain the font from someone with an
appropriate license for it.

For testing purposes you may use Times instead of Garamond::

    cp "/Library/Fonts/Times New Roman.ttf" fonts/gara.ttf
    cp "/Library/Fonts/Times New Roman Bold.ttf" fonts/garabd.ttf
    cp "/Library/Fonts/Times New Roman Italic.ttf" fonts/garait.ttf

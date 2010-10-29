<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="sv">
  <head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <link href="/img/favicon.ico" rel="shortcut icon" />
    <link rel="stylesheet" type="text/css" media="screen" href="/css/screen.css" />
    <link rel="stylesheet" type="text/css" media="handheld" href="/css/handheld.css" />
    <link rel="stylesheet" type="text/css" media="print" href="/css/print.css" />
    <!--[if lte IE 6]>
    <link rel="stylesheet" type="text/css" href="/css/ie.css" media="screen" />
    <![endif]-->
    <style type="text/css">

      #logo {
        padding: 40px 0 5px 18px;
      }
      #logo a {
        color: rgb(135, 57, 95);
        text-decoration: none;
        font-size: 19px;
        font-weight: bold;
      }

      #related .newslist {
          background-color: #F5F5F5;
          padding: 2px 2px;
      }
      #related .newslist .newslistcompact {
          border: solid 2px #fff;
          background-color: #F5F5F5;
          padding: 10px 12px 10px 12px;
          font-size: 0.8em;
      }
    </style>
    <title>RInfo Service : $title$</title>
  </head>
  <body>
    <div id="container">
      <div id="header">
        <div id="headercontent">
          <!--
          <ul id="quicknav">
            <li><a href="http://www.lagrummet.se/">Till webbplatsen lagrummet.se</a></li>
          </ul>
          -->
          <!--
          <a href="/" accesskey="1" class="logo"><img src="http://www.lagrummet.se/img/lr/logo_lagrummet.gif" alt="Till startsidan för projektdokumentationen" /></a>
          -->
          <div id="logo">
            <a href="/" accesskey="1" class="logo" title="Till startsidan för projektdokumentationen"
              >RInfo Service</a>
          </div>
        </div>
        <div class="clearer divider"> </div>
      </div>
      <div id="pageleft">
        <div id="pageright">
          <div id="navigation">
            <p class="menuintro">Gå direkt till</p>
            <ul id="mainmenu">
              <li class="open">
                <a href="$basePath$" class="open">Rättsinformation</a>
                <ul class="submenu">
                  <li><a href="$basePath$/browse/publ">Dokumenttyp per utgivare och år</a></li>
                  <!--
                  <li><a href="/filter/forfattningar/">Författningar</a></li>
                  <li><a href="/filter/forarbeten/">Förarbeten</a></li>
                  <li><a href="/filter/rattspraxis/">Rättspraxis</a></li>
                  <li><a href="/filter/internationellt/">Internationellt material</a></li>
                  -->
                  <li><a href="$basePath$/org">Alla rättskällor</a></li>
                </ul>
              </li>
              <!--
              <li class="open">
                <a href="/lar-dig-mer/" class="open">Lär dig mer</a>
                <ul class="submenu">
                  <li><a href="/lar-dig-mer/faq/">Vanliga frågor </a></li>
                  <li><a href="/lar-dig-mer/ordlista/">Ordlista A-Ö</a></li>
                  <li><a href="/lar-dig-mer/tips/">Söktips</a></li>
                  <li class="selected"><a href="/lar-dig-mer/vart/" class="selected">Vart vänder jag mig?</a></li>
                </ul>
              </li>
              -->
            </ul>
          </div>

          <div id="content" class="tpl-page">
            <h1>$if (heading)$ $heading$ $else$ $title$ $endif$</h1>
            <div id="main">
              $main$
            </div>
          </div>

          <div id="related">
            $related$
          </div>
          <div class="clearer"> </div>
        </div>
      </div>
      <div id="footer">
        <div id="footercontent">
          <div class="contact">
            <p class="left">
              Kontakta: rinfo-service@lagrummet.se
            </p>
            <p class="right">
              <!--
              <a href="http://www.domstol.se">Domstolsverket</a>
              -->
            </p>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>

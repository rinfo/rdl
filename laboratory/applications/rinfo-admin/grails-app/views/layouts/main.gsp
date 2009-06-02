<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="sv" lang="sv">
    <head>
        <title><g:layoutTitle default="Grails" /></title>
        <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'screen.css')}" />
        <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}" />
        <link rel="shortcut icon" href="${createLinkTo(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
        <g:layoutHead />
        <g:javascript library="application" />				
    </head>
    <body>
<div class="bottom">
            <div class="header"><a href="/rinfo-admin/" class="logo"><img src="${createLinkTo(dir:'images',file:'logo_lagrummet.gif')}" alt="Tillbaka till startsidan" /></a><p>Administrationsklient</p></div>
            <div class="colmask">
                <div class="colleft">
                    <div class="col1" role="main">
                        <g:layoutBody />		
                    </div>
                    <div class="col2 menu" role="navigation">
                        <h2>Hantera innehåll</h2>
                        <ul id="menu">
                            <li><a href="${createLink(controller:'organization',action:'list')}">Organisationer</a></li>
                            <li><a href="${createLink(controller:'publicationcollection',action:'list')}">Författningssamlingar</a></li>
                            <li><a href="${createLink(controller:'feed',action:'list')}">Inhämtningskällor</a></li>
                            <li><a href="${createLink(controller:'event',action:'index')}">Systemhändelser</a></li>
                        </ul>
                        <h2>Hantera användare</h2>
                        <ul>
                            <li><a href="/">Användare</a></li>
                        </ul>
                    </div>
                </div>
            </div>
            <div class="footer">
                <p>Sidfot</p>
            </div>
        </div>
    </body>	
</html>

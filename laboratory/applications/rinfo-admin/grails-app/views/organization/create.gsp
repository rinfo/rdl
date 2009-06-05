<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Skapa ny organisation</title>         
    </head>
    <body>
        <div class="body">
            <h1>Skapa ny organisation</h1>
            <p>Fält markerade med "*" är obligatoriska.</p>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${organizationInstance}">
            <div class="errors">
                <g:renderErrors bean="${organizationInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" method="post" >
                <div class="form panel">
                    <g:render template="form" collection="${organizationInstance}" />
                    
                    <div class="actions">
                        <input type="submit" value="Spara organisation" />
                    </div>
                </div>
            </g:form>
        </div>
    </body>
</html>

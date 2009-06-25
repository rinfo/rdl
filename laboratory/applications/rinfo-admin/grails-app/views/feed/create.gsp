

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Skapa ny inhämtningskälla</title>         
    </head>
    <body>
        <div class="body">
            <h1>Skapa ny inhämtningskälla</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${feedInstance}">
            <div class="errors">
                <g:renderErrors bean="${feedInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" method="post" >
                <div class="form panel">
                    <g:render template="form" collection="${feedInstance}" />
                    <div class="actions">
                        <p><input class="save" type="submit" value="Spara" action="Save" /></p>
                    </div>
                </div>
            </g:form>
        </div>
    </body>
</html>

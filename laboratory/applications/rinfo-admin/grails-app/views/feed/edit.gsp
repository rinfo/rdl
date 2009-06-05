

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Redigera inhämtningskälla</title>
    </head>
    <body>
        <div class="body">
            <h1>Redigera inhämtningskälla</h1>
            <div class="objactions">
                <p<g:link class="list" action="list">Visa alla</g:link></p>
            </div>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${feedInstance}">
            <div class="errors">
                <g:renderErrors bean="${feedInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form method="post" >
                <input type="hidden" name="id" value="${feedInstance?.id}" />
                <input type="hidden" name="version" value="${feedInstance?.version}" />
                <div class="form panel">
                    <g:render template="form" collection="${feedInstance}" />
                    <div class="buttons">
                        <p><g:actionSubmit class="save" value="Spara" /></p>
                    </div>
                </div>
            </g:form>
        </div>
    </body>
</html>

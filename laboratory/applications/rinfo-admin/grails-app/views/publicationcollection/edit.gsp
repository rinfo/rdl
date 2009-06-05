

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Redigera författningssamling</title>
    </head>
    <body>
        <div class="body">
            <h1>Redigera författningssamling ${publicationcollectionInstance?.shortname}</h1>
            <div class="objactions">
                <p><g:link class="list" action="list">Visa alla</g:link></p>
            </div>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${publicationcollectionInstance}">
            <div class="errors">
                <g:renderErrors bean="${publicationcollectionInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="update" method="post" >
                <div class="form panel">
                    <input type="hidden" name="id" value="${publicationcollectionInstance?.id}" />
                    <input type="hidden" name="version" value="${publicationcollectionInstance?.version}" />
                    <g:render template="form" collection="${publicationcollectionInstance}" />

                    <div class="actions">
                        <input type="submit" value="Spara" />
                    </div>
                </div>
            </g:form>
        </div>
    </body>
</html>

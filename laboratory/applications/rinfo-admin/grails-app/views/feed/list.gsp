

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Inhämtningskällor</title>
    </head>
    <body>
        <div class="body">
            <h1>Inhämtningskällor</h1>
            <div class="objactions">
                <p><g:link class="create" action="create">Skapa ny</g:link></p>
            </div>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table class="grid">
                    <thead>
                        <tr>
                            <g:sortableColumn property="id" title="Id" />
                            <g:sortableColumn property="url" title="Webbadress" />
                            <g:sortableColumn property="identifier" title="Identifierare" />
                            <th>Organisation</th>
                            <g:sortableColumn property="lastUpdated" title="Uppdaterad" />
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${feedInstanceList}" status="i" var="feedInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                            <td><g:link action="show" id="${feedInstance.id}">${fieldValue(bean:feedInstance, field:'id')}</g:link></td>
                            <td><g:link action="show" id="${feedInstance.id}">${fieldValue(bean:feedInstance, field:'url')}</g:link></td>
                            <td>${fieldValue(bean:feedInstance, field:'identifier')}</td>
                            <td>${fieldValue(bean:feedInstance, field:'organization')}</td>
                            <td><g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${feedInstance.lastUpdated}"/></td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginate">
                <g:paginate total="${feedInstanceTotal}" />
            </div>
        </div>
    </body>
</html>

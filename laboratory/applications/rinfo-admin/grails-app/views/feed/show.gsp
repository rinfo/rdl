

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Inhämtningskälla ${fieldValue(bean:feedInstance, field:'id')}</title>
    </head>
    <body>
        <h1>Inhämtningskälla ${fieldValue(bean:feedInstance, field:'id')}</h1>
        <div class="objactions">
            <p><g:link action="list">Visa alla</g:link>, 
            <g:link action="create">Skapa ny</g:link></p>
        </div>
        <div class="body">
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="panel">
                <table class="item">
                    <tbody>
                        <tr class="prop">
                            <th class="name">Id:</th>
                            <td class="value">${fieldValue(bean:feedInstance, field:'id')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Webbadress:</th>
                            <td class="value">${fieldValue(bean:feedInstance, field:'url')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Identifierare:</th>
                            <td class="value">${fieldValue(bean:feedInstance, field:'identifier')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Organisation:</th>
                            <td class="value"><g:link controller="organization" action="show" id="${feedInstance?.organization?.id}">${feedInstance?.organization?.encodeAsHTML()}</g:link></td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Senast uppdaterad:</th>
                            <td><g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${feedInstance.lastUpdated}"/></td>
                        </tr>
                    </tbody>
                </table>
                <div class="buttons">
                    <g:form>
                        <input type="hidden" name="id" value="${feedInstance?.id}" />
                        <span class="button"><g:link action="edit" id="${feedInstance?.id}">Redigera</g:link></span>
                        <span class="button"><g:actionSubmit action="delete" onclick="return confirm('Är du säker?');" value="Radera" /></span>
                    </g:form>
                </div>
            </div>
        </div>
    </body>
</html>



<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>${fieldValue(bean:publicationcollectionInstance, field:'name')}</title>
    </head>
    <body>
        <h1>${fieldValue(bean:publicationcollectionInstance, field:'name')}</h1>
        <div class="objactions">
            <p><g:link class="list" action="list">Visa alla</g:link>, <g:link class="create" action="create">Skapa ny</g:link></p>
        </div>
        <div class="body">
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="panel">
                <table class="item">
                    <tbody>
                        <tr class="prop">
                            <th class="name">Id:</td>
                            <td class="value">${fieldValue(bean:publicationcollectionInstance, field:'id')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Namn:</td>
                            <td class="value">${fieldValue(bean:publicationcollectionInstance, field:'name')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Kortnamn:</td>
                            <td class="value">${fieldValue(bean:publicationcollectionInstance, field:'shortname')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Organisation:</td>
                            <td class="value"><g:link controller="organization" action="show" id="${publicationcollectionInstance?.organization?.id}">${publicationcollectionInstance?.organization?.encodeAsHTML()}</g:link></td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Länk till webbsida:</td>
                            <td class="value">${fieldValue(bean:publicationcollectionInstance, field:'homepage')}</td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Senast uppdaterad:</td>
                            <td class="value">
                                <g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${publicationcollectionInstance.lastUpdated}"/>
                            </td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Skapad:</td>
                            <td class="value">
                                <g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${publicationcollectionInstance.dateCreated}"/>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div class="buttons">
                    <g:form>
                        <input type="hidden" name="id" value="${publicationcollectionInstance?.id}" />
                        <span class="button"><g:link action="edit" id="${publicationcollectionInstance?.id}">Redigera</g:link></span>
                        <span class="button"><g:actionSubmit action="delete" onclick="return confirm('Är du säker?');" value="Radera" /></span>
                    </g:form>
                </div>
            </div>
        </div>
    </body>
</html>

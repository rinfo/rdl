

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>${fieldValue(bean:organizationInstance, field:'name')} (${fieldValue(bean:organizationInstance, field:'id')})</title>
    </head>
    <body>
        <h1>${fieldValue(bean:organizationInstance, field:'name')} (${fieldValue(bean:organizationInstance, field:'id')})</h1>
        <div class="objactions">
            <p><g:link class="create" action="create">Skapa ny organisation</g:link>, 
            <g:link class="list" action="list">Visa alla</g:link></p>
        </div>
        <div class="body">
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table class="grid">
                    <tbody>

                    
                        <tr class="prop">
                            <th class="name">Id:</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'id')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Namn:</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'name')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Webbplats:</th>
                            
                            <td class="value"><a href="${fieldValue(bean:organizationInstance, field:'homepage')}">${fieldValue(bean:organizationInstance, field:'homepage')}</a></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Kontaktperson (namn):</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'contact_name')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Kontaktperson (e-post):</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'contact_email')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Författningssamlingar:</th>
                            
                            <td class="value">
                                <ul>
                                <g:each var="p" in="${organizationInstance.publicationcollections}">
                                    <li><g:link controller="publicationcollection" action="show" id="${p.id}">${p?.encodeAsHTML()}</g:link></li>
                                </g:each>
                                </ul>
                            </td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Inhämtningskällor:</th>
                            
                            <td class="value">
                                <ul>
                                <g:each var="f" in="${organizationInstance.feeds}">
                                    <li><g:link controller="feed" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
                                </g:each>
                                </ul>
                            </td>
                            
                        </tr>
                        <tr class="prop">
                            <th class="name">Senast uppdaterad:</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'lastUpdated')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Skapad:</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'dateCreated')}</td>
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form>
                    <input type="hidden" name="id" value="${organizationInstance?.id}" />
                    <span class="button"><g:actionSubmit class="edit" value="Redigera" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Är du säker på att du vill radera denna organisation?');" value="Radera" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>



<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>${fieldValue(bean:organizationInstance, field:'name')}</title>
    </head>
    <body>
        <h1>${fieldValue(bean:organizationInstance, field:'name')}</h1>
        <div class="objactions">
            <p><g:link class="create" action="create">Skapa ny organisation</g:link>, 
            <g:link class="list" action="list">Visa alla</g:link></p>
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
                            <td class="value">${fieldValue(bean:organizationInstance, field:'id')}</td>
                        </tr>
                        <tr class="prop">
                            <th class="name">Namn:</th>
                            
                            <td class="value">${fieldValue(bean:organizationInstance, field:'name')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Webbplats:</th>
                            
                            <td class="value"><a target="_new" href="${fieldValue(bean:organizationInstance, field:'homepage')}">${fieldValue(bean:organizationInstance, field:'homepage')}</a></td>
                            
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
                                <g:if test="${organizationInstance.publicationcollections}">
                                <ul>
                                <g:each var="p" in="${organizationInstance.publicationcollections}">
                                    <li>${p.name}</li>
                                </g:each>
                                </ul>
                                </g:if>
                                <g:link class="add" controller="publicationcollection" params="['organization.id':organizationInstance?.id]" action="create">Lägg till författningssamling</g:link>
                            </td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Inhämtningskällor:</th>
                            
                            <td class="value">
                                <g:if test="${organizationInstance.feeds}">
                                <ul>
                                <g:each var="f" in="${organizationInstance.feeds}">
                                    <li>${f}</li>
                                </g:each>
                                </ul>
                                </g:if>
                                <g:link class="add" controller="feed" params="['organization.id':organizationInstance?.id]" action="create">Lägg till inhämtningskälla</g:link>
                            </td>
                            
                        </tr>
                        <tr class="prop">
                            <th class="name">Senast uppdaterad:</th>
                            <td class="value">
                                <g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${organizationInstance.lastUpdated}"/>
                            </td>
                        </tr>
                    
                        <tr class="prop">
                            <th class="name">Skapad:</th>
                            
                            <td class="value">
                                <g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${organizationInstance.dateCreated}"/>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div class="buttons">
                    <g:form>
                        <input type="hidden" name="id" value="${organizationInstance?.id}" />
                        <span class="button"><g:link action="edit" id="${organizationInstance?.id}" class="edit">Redigera</g:link></span>
                        <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Är du säker på att du vill radera denna organisation? Relaterade inhämtningskällor raderas också.');" value="Radera" action="Delete" /></span>
                    </g:form>
                </div>
            </div>
        </div>
    </body>
</html>

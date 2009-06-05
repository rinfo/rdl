

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Organisationer</title>
    </head>
    <body>
        <div class="body">
            <h1>Organisationer</h1>
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
                        
                   	        <g:sortableColumn property="name" title="Namn" />
                        
                   	        <g:sortableColumn property="contact_name" title="Kontaktperson" />

                   	        <g:sortableColumn property="contact_email" title="E-post" />
                        
                   	        <g:sortableColumn property="lastUpdated" title="Uppdaterad" />
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${organizationInstanceList}" status="i" var="organizationInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${organizationInstance.id}">${fieldValue(bean:organizationInstance, field:'id')}</g:link></td>

                            <td><g:link action="show" id="${organizationInstance.id}">${fieldValue(bean:organizationInstance, field:'name')}</g:link></td>
                        
                            <td>${fieldValue(bean:organizationInstance, field:'contact_name')}</td>
                        
                            <td>${fieldValue(bean:organizationInstance, field:'contact_email')}</td>
                        
                            <td><g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${organizationInstance.lastUpdated}"/></td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginate">
                <g:paginate total="${organizationInstanceTotal}" />
            </div>
        </div>
    </body>
</html>

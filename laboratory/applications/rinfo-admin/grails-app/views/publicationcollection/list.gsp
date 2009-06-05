

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Författningssamlingar</title>
    </head>
    <body>
        <div class="body">
        <h1>Författningssamlingar</h1>
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
                        
                   	        <g:sortableColumn property="shortname" title="Kortnamn" />

                   	        <g:sortableColumn property="organization" title="Organisation" />
                   	    
                   	        <g:sortableColumn property="lastUpdated" title="Uppdaterad" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${publicationcollectionInstanceList}" status="i" var="publicationcollectionInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${publicationcollectionInstance.id}">${fieldValue(bean:publicationcollectionInstance, field:'id')}</g:link></td>

                            <td><g:link action="show" id="${publicationcollectionInstance.id}">${fieldValue(bean:publicationcollectionInstance, field:'name')}</g:link></td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'shortname')}</td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'organization')}</td>
                        
                            <td><g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${publicationcollectionInstance.lastUpdated}"/></td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginate">
                <g:paginate total="${publicationcollectionInstanceTotal}" />
            </div>
        </div>
    </body>
</html>

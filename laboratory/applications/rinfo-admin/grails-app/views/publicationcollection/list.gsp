

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Publicationcollection List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New Publicationcollection</g:link></span>
        </div>
        <div class="body">
            <h1>Publicationcollection List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <g:sortableColumn property="name" title="Name" />
                        
                   	        <g:sortableColumn property="shortname" title="Shortname" />
                        
                   	        <th>Organization</th>
                   	    
                   	        <g:sortableColumn property="homepage" title="Homepage" />
                        
                   	        <g:sortableColumn property="lastUpdated" title="Last Updated" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${publicationcollectionInstanceList}" status="i" var="publicationcollectionInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${publicationcollectionInstance.id}">${fieldValue(bean:publicationcollectionInstance, field:'id')}</g:link></td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'name')}</td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'shortname')}</td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'organization')}</td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'homepage')}</td>
                        
                            <td>${fieldValue(bean:publicationcollectionInstance, field:'lastUpdated')}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${publicationcollectionInstanceTotal}" />
            </div>
        </div>
    </body>
</html>

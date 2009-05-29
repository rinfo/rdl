

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Feed List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New Feed</g:link></span>
        </div>
        <div class="body">
            <h1>Feed List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <g:sortableColumn property="url" title="Url" />
                        
                   	        <g:sortableColumn property="identifier" title="Identifier" />
                        
                   	        <th>Organization</th>
                   	    
                   	        <g:sortableColumn property="lastUpdated" title="Last Updated" />
                        
                   	        <g:sortableColumn property="dateCreated" title="Date Created" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${feedInstanceList}" status="i" var="feedInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${feedInstance.id}">${fieldValue(bean:feedInstance, field:'id')}</g:link></td>
                        
                            <td>${fieldValue(bean:feedInstance, field:'url')}</td>
                        
                            <td>${fieldValue(bean:feedInstance, field:'identifier')}</td>
                        
                            <td>${fieldValue(bean:feedInstance, field:'organization')}</td>
                        
                            <td>${fieldValue(bean:feedInstance, field:'lastUpdated')}</td>
                        
                            <td>${fieldValue(bean:feedInstance, field:'dateCreated')}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${feedInstanceTotal}" />
            </div>
        </div>
    </body>
</html>

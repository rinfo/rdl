

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Entry List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New Entry</g:link></span>
        </div>
        <div class="body">
            <h1>Entry List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <g:sortableColumn property="uri" title="Uri" />
                        
                   	        <g:sortableColumn property="content" title="Content" />
                        
                   	        <g:sortableColumn property="content_md5" title="Contentmd5" />
                        
                   	        <g:sortableColumn property="dateCreated" title="Date Created" />
                        
                   	        <g:sortableColumn property="dateDeleted" title="Date Deleted" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${entryInstanceList}" status="i" var="entryInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${entryInstance.id}">${fieldValue(bean:entryInstance, field:'id')}</g:link></td>
                        
                            <td>${fieldValue(bean:entryInstance, field:'uri')}</td>
                        
                            <td>${fieldValue(bean:entryInstance, field:'content')}</td>
                        
                            <td>${fieldValue(bean:entryInstance, field:'content_md5')}</td>
                        
                            <td>${fieldValue(bean:entryInstance, field:'dateCreated')}</td>
                        
                            <td>${fieldValue(bean:entryInstance, field:'dateDeleted')}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${entryInstanceTotal}" />
            </div>
        </div>
    </body>
</html>

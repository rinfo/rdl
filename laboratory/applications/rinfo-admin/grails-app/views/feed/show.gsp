

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show Feed</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">Feed List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New Feed</g:link></span>
        </div>
        <div class="body">
            <h1>Show Feed</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>

                    
                        <tr class="prop">
                            <td valign="top" class="name">Id:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:feedInstance, field:'id')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Url:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:feedInstance, field:'url')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Identifier:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:feedInstance, field:'identifier')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Organization:</td>
                            
                            <td valign="top" class="value"><g:link controller="organization" action="show" id="${feedInstance?.organization?.id}">${feedInstance?.organization?.encodeAsHTML()}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Last Updated:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:feedInstance, field:'lastUpdated')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Date Created:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:feedInstance, field:'dateCreated')}</td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form>
                    <input type="hidden" name="id" value="${feedInstance?.id}" />
                    <span class="button"><g:actionSubmit class="edit" value="Edit" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>

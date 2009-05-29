

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Create Feed</title>         
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">Feed List</g:link></span>
        </div>
        <div class="body">
            <h1>Create Feed</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${feedInstance}">
            <div class="errors">
                <g:renderErrors bean="${feedInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" method="post" >
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="url">Url:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:feedInstance,field:'url','errors')}">
                                    <input type="text" id="url" name="url" value="${fieldValue(bean:feedInstance,field:'url')}"/>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="identifier">Identifier:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:feedInstance,field:'identifier','errors')}">
                                    <textarea rows="5" cols="40" name="identifier">${fieldValue(bean:feedInstance, field:'identifier')}</textarea>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="organization">Organization:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:feedInstance,field:'organization','errors')}">
                                    <g:select optionKey="id" from="${Organization.list()}" name="organization.id" value="${feedInstance?.organization?.id}" ></g:select>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="lastUpdated">Last Updated:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:feedInstance,field:'lastUpdated','errors')}">
                                    <g:datePicker name="lastUpdated" value="${feedInstance?.lastUpdated}" noSelection="['':'']"></g:datePicker>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="dateCreated">Date Created:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:feedInstance,field:'dateCreated','errors')}">
                                    <g:datePicker name="dateCreated" value="${feedInstance?.dateCreated}" ></g:datePicker>
                                </td>
                            </tr> 
                        
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                    <span class="button"><input class="save" type="submit" value="Create" /></span>
                </div>
            </g:form>
        </div>
    </body>
</html>

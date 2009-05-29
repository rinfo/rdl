

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Create Publicationcollection</title>         
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">Publicationcollection List</g:link></span>
        </div>
        <div class="body">
            <h1>Create Publicationcollection</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${publicationcollectionInstance}">
            <div class="errors">
                <g:renderErrors bean="${publicationcollectionInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" method="post" >
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="name">Name:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:publicationcollectionInstance,field:'name','errors')}">
                                    <textarea rows="5" cols="40" name="name">${fieldValue(bean:publicationcollectionInstance, field:'name')}</textarea>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="shortname">Shortname:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:publicationcollectionInstance,field:'shortname','errors')}">
                                    <textarea rows="5" cols="40" name="shortname">${fieldValue(bean:publicationcollectionInstance, field:'shortname')}</textarea>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="organization">Organization:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:publicationcollectionInstance,field:'organization','errors')}">
                                    <g:select optionKey="id" from="${Organization.list()}" name="organization.id" value="${publicationcollectionInstance?.organization?.id}" ></g:select>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="homepage">Homepage:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:publicationcollectionInstance,field:'homepage','errors')}">
                                    <textarea rows="5" cols="40" name="homepage">${fieldValue(bean:publicationcollectionInstance, field:'homepage')}</textarea>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="lastUpdated">Last Updated:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:publicationcollectionInstance,field:'lastUpdated','errors')}">
                                    <g:datePicker name="lastUpdated" value="${publicationcollectionInstance?.lastUpdated}" noSelection="['':'']"></g:datePicker>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="dateCreated">Date Created:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:publicationcollectionInstance,field:'dateCreated','errors')}">
                                    <g:datePicker name="dateCreated" value="${publicationcollectionInstance?.dateCreated}" ></g:datePicker>
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

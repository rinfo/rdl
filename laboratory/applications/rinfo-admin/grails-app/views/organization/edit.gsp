

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Edit Organization</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">Organization List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New Organization</g:link></span>
        </div>
        <div class="body">
            <h1>Edit Organization</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${organizationInstance}">
            <div class="errors">
                <g:renderErrors bean="${organizationInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form method="post" >
                <input type="hidden" name="id" value="${organizationInstance?.id}" />
                <input type="hidden" name="version" value="${organizationInstance?.version}" />
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="name">Name:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'name','errors')}">
                                    <input type="text" maxlength="200" id="name" name="name" value="${fieldValue(bean:organizationInstance,field:'name')}"/>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="homepage">Homepage:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'homepage','errors')}">
                                    <input type="text" id="homepage" name="homepage" value="${fieldValue(bean:organizationInstance,field:'homepage')}"/>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="contact_name">Contactname:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'contact_name','errors')}">
                                    <input type="text" maxlength="200" id="contact_name" name="contact_name" value="${fieldValue(bean:organizationInstance,field:'contact_name')}"/>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="contact_email">Contactemail:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'contact_email','errors')}">
                                    <input type="text" maxlength="200" id="contact_email" name="contact_email" value="${fieldValue(bean:organizationInstance,field:'contact_email')}"/>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="publicationcollections">Publicationcollections:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'publicationcollections','errors')}">
                                    
<ul>
<g:each var="p" in="${organizationInstance?.publicationcollections?}">
    <li><g:link controller="publicationcollection" action="show" id="${p.id}">${p?.encodeAsHTML()}</g:link></li>
</g:each>
</ul>
<g:link controller="publicationcollection" params="['organization.id':organizationInstance?.id]" action="create">Add Publicationcollection</g:link>

                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="lastUpdated">Last Updated:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'lastUpdated','errors')}">
                                    <g:datePicker name="lastUpdated" value="${organizationInstance?.lastUpdated}" noSelection="['':'']"></g:datePicker>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="dateCreated">Date Created:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'dateCreated','errors')}">
                                    <g:datePicker name="dateCreated" value="${organizationInstance?.dateCreated}" ></g:datePicker>
                                </td>
                            </tr> 
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="feeds">Feeds:</label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean:organizationInstance,field:'feeds','errors')}">
                                    
<ul>
<g:each var="f" in="${organizationInstance?.feeds?}">
    <li><g:link controller="feed" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
</ul>
<g:link controller="feed" params="['organization.id':organizationInstance?.id]" action="create">Add Feed</g:link>

                                </td>
                            </tr> 
                        
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                    <span class="button"><g:actionSubmit class="save" value="Update" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </div>
            </g:form>
        </div>
    </body>
</html>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Skapa ny organisation</title>         
    </head>
    <body>
        <div class="body">
            <h1>Skapa ny organisation</h1>
            <p>Fält markerade med "*" är obligatoriska.</p>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${organizationInstance}">
            <div class="errors">
                <g:renderErrors bean="${organizationInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" method="post" >
                <div class="form panel">
                    <p class="set ${hasErrors(bean:organizationInstance,field:'name','errors')}">
                        <label for="name">* Organisationsnamn:</label>
                        <input aria-required="true" type="text" maxlength="200" id="name" name="name" value="${fieldValue(bean:organizationInstance,field:'name')}"/>
                        <span class="example">Exempel: Boverket</span>
                    </p>

                    <p class="set ${hasErrors(bean:organizationInstance,field:'homepage','errors')}">
                        <label for="homepage">* Webbplats:</label>
                        <input aria-required="true" type="text" id="homepage" name="homepage" value="${fieldValue(bean:organizationInstance,field:'homepage')}"/>
                        <span class="example">Exempel: http://www.boverket.se</span>
                    </p>
                        
                    <p class="set ${hasErrors(bean:organizationInstance,field:'contact_name','errors')}">
                        <label for="contact_name">* Kontaktperson (namn):</label>
                        <input aria-required="true" type="text" maxlength="200" id="contact_name" name="contact_name" value="${fieldValue(bean:organizationInstance,field:'contact_name')}"/>
                        <span class="example">Exempel: Anna Andersson</span>
                    </p>
                    
                    <p class="set ${hasErrors(bean:organizationInstance,field:'contact_email','errors')}">
                        <label for="contact_email">* Kontaktperson (e-post):</label>
                        <input aria-required="true" type="text" maxlength="200" id="contact_email" name="contact_email" value="${fieldValue(bean:organizationInstance,field:'contact_email')}"/>
                        <span class="example">Exempel: anna.andersson@boverket.se</span>
                    </p>
                    
                    <div class="actions">
                        <input type="submit" value="Spara organisation" />
                    </div>
                </div>
            </g:form>
        </div>
    </body>
</html>

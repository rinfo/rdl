
                    <p class="set ${hasErrors(bean:publicationcollectionInstance,field:'name','errors')}">
                        <label for="name">* Namn:</label>
                        <input aria-required="true" type="text" maxlength="200" id="name" name="name" value="${fieldValue(bean:publicationcollectionInstance,field:'name')}"/>
                        <span class="example">Exempel: Boverkets författningssamling</span>
                    </p>
                        
                    <p class="set ${hasErrors(bean:publicationcollectionInstance,field:'shortname','errors')}">
                        <label for="shortname">* Kortnamn:</label>
                        <input aria-required="true" type="text" maxlength="200" id="shortname" name="shortname" value="${fieldValue(bean:publicationcollectionInstance,field:'shortname')}"/>
                        <span class="example">Exempel: BOFS</span>
                    </p>

                    <p class="set ${hasErrors(bean:publicationcollectionInstance,field:'organization','errors')}">
                        <label for="organization.id">* Organisation:</label>
                        <g:if test="${Organization.get(params.organization?.id) == null}">
                        <g:select id="organization.id" optionKey="id" from="${Organization.list()}" name="organization.id" value="${publicationcollectionInstance?.organization?.id}" ></g:select>
                        </g:if>
                        <g:else>
                        <input type="hidden" name="organization.id" id="organization.id" value="${params.organization.id}"/>
                        <span class="value">${Organization.get(params.organization.id)?.name}</span></g:else>
                    </p>


                    <p class="set ${hasErrors(bean:publicationcollectionInstance,field:'homepage','errors')}">
                        <label for="homepage">* Länk till webbsida:</label>
                        <input aria-required="true" type="text" maxlength="200" id="homepage" name="homepage" value="${fieldValue(bean:publicationcollectionInstance,field:'homepage')}"/>
                        <span class="example">Exempel: http://www.boverket.se/lag-och-ratt/bofs</span>
                    </p>



                    <p class="set ${hasErrors(bean:feedInstance,field:'url','errors')}">
                        <label for="name">* Webbadress:</label>
                        <input aria-required="true" type="text" maxlength="200" id="url" name="url" value="${fieldValue(bean:feedInstance,field:'url')}"/>
                        <span class="example">Exempel: http://www.example.com/rinfo/feed.atom</span>
                    </p>

                    <p class="set ${hasErrors(bean:feedInstance,field:'identifier','errors')}">
                        <label for="name">* Identifierare:</label>
                        <input aria-required="true" type="text" maxlength="200" id="identifier" name="identifier" value="${fieldValue(bean:feedInstance,field:'identifier')}"/>
                        <span class="example">Exempel: 2008,boverket</span>
                    </p>

                    <p class="set ${hasErrors(bean:feedInstance,field:'organization','errors')}">
                        <label for="organization.id">* Organisation:</label>
                        <g:if test="${Organization.get(params.organization?.id) == null}">
                        <g:select id="organization.id" optionKey="id" from="${Organization.list()}" name="organization.id" value="${publicationcollectionInstance?.organization?.id}" ></g:select>
                        </g:if>
                        <g:else>
                        <input type="hidden" name="organization.id" id="organization.id" value="${params.organization.id}"/>
                        <span class="value">${Organization.get(params.organization.id)?.name}</span></g:else>
                    </p>

<#list questions>
    <form id="observatory-questions-form">
        <#items as q>
            <fieldset>
                <legend>${q.label}</legend>
                <p>${q.description}</p>
                <label for="q${q.id}">${q.query}</label>
                <input type="text" id="q${q.id}" name="${q.id}" value="${q.response}" />
            </fieldset>
        </#items>
    </form>
</#list>
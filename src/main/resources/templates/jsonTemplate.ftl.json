<#function output item>
    <#if item?is_hash>
        <#assign obj = "">
        <#assign obj += "{">
        <#list item as k, v>
        <#assign obj += '"${k}"' + ":" + output(v) + ",">
        </#list>
        <#assign obj = obj?remove_ending(",")>
        <#assign obj += "}">
    <#elseif item?is_sequence>
        <#assign obj = "">
        <#assign obj += "[">
        <#list item as v>
        <#assign obj += output(v) + ",">
        </#list>
        <#assign obj = obj?remove_ending(",")>
        <#assign obj += "]">
    <#else>
        <#assign obj = "">
        <#if item?is_string>
        <#assign obj = '"' + item + '"'>
        <#else>
        <#assign obj = item>
        </#if>
    </#if>
    <#return obj>
</#function>
${output(testdata)}
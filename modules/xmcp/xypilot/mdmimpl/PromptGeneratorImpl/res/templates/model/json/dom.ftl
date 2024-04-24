<#import "/util/json.ftl" as json/>

<#-- json objects defining the member methods of the dom
  params:
    lastElementSep - boolean, if true, the last element in the list (if any) will have a trailing comma

  result:
    [method],
    ...,
    [method](,) <eol>

-->
<#macro methods lastElementSep=false>
<#list dom.operations as operation>
<@json.method operation/><#if lastElementSep>,<#else><#sep>,</#sep></#if>
</#list>
</#macro>
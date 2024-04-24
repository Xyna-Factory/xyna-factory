<#import "/util/functions.ftl" as fun/>

<#--  variable block, with name, fqn, isList and documentation 
  params:
    memberVar: the member variable to be defined

  result:
    {
      "name": "[varName]",
      "fqn": "[type]",
      "isList": [isList],
      "documentation": "[documentation]"
    } <inline>

  example:
    {
      "name": "name",
      "fqn": "base.Text",
      "isList": false,
      "documentation": "The name of the person"
    }
-->
<#macro member_var memberVar>
{
  "name": "${memberVar.varName}",
  "fqn": "${memberVar.isJavaBaseType()?then(memberVar.javaTypeEnum.javaTypeName, memberVar.domOrExceptionObject.originalFqName)}",
  "isList": ${memberVar.isList()?c},
  <#--  !"" is only necessary cause of XYNA bug, where member documentation is always null for exceptions  -->
  "documentation": "${memberVar.documentation!""?json_string}"
}<#rt/>
</#macro>


<#-- method definition
  params:
    operation: the operation to be defined

  result:
    {
      "name": "[methodName]",
      "parameters": [
        [parameters]
      ],
      "documentation": "[documentation]"
    } <inline>
-->
<#macro method operation>
{
  "name": "${operation.name}",
  "parameters": [
    <@method_params fun.map_operation_params(operation)/>
  ],
  "documentation": "${operation.documentation?json_string}"
}<#rt/>
</#macro>


<#-- method parameters

  result:
    {
      "type": "[type]",
      "name": "[varName]",
      "fqn": "[fqn]",
      "isList": [isList]
    },
    ...
    {
      "type": "[type]",
      "name": "[varName]",
      "fqn": "[fqn]",
      "isList": [isList]
    } <eol>

  example:
    {
      "type": "input",
      "name": "name",
      "fqn": "base.Text",
      "isList": false
    },
    {
      "type": "input",
      "name": "age",
      "fqn": "base.IntegerNumber",
      "isList": false
    },
    {
      "type": "output",
      "name": "matches",
      "fqn": "base.Text",
      "isList": true
    }
-->
<#macro method_params operationParams>
  <#list operationParams as type, params>
    <#list params as param>
{
  "type": "${type}",
  "name": "${param.varName}",
  "fqn": "${param.domOrExceptionObject.originalFqName}",
  "isList": ${param.isList()?c}
}<#if !(params?is_last && type?is_last)>,</#if>
    </#list>
  </#list>
</#macro>
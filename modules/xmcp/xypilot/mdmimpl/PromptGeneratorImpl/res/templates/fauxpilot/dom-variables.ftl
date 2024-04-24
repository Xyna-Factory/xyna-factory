<#import "/model/json/dom-or-exception.ftl" as doe/>
<#import "/model/json/dom.ftl" as dat/>
<#--
    Template for generating member variables for a DomModel.
-->
<#--  <@doe.member_schema/>  -->
<@doe.member_example/>
# available fqn: ${availableVariableTypes?join(", ")}
{
  "name": "${domOrException.originalSimpleName}",
  "documentation": "${domOrException.documentation?json_string}",
  "members": [
    <#if domOrException.memberVars?size == 0>
    {
      "name": "serialVersionUID",
      "fqn": "long",
      "isList": false,
      "documentation": "ID to ensure correct serialization and deserialization of objects"
    },
    </#if>
    <@doe.member_vars lastElementSep=true/>
    §
  ]
}
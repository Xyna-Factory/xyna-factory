<#import "/model/json/dom-or-exception.ftl" as doe/>
<#import "/model/json/dom.ftl" as dom/>
<#--
    Template for generating member variables for a ExceptionModel.
-->
<#--  <@doe.member_schema/>  -->
<@doe.member_example/>
# available fqn: ${availableVariableTypes?join(", ")}
{
  "name": "${domOrException.originalSimpleName}",
  "documentation": "${domOrException.documentation?json_string}",
  "members": [
    <@doe.member_vars/>,
    §
  ]
  <#--  TODO: add exception messages?  -->
}
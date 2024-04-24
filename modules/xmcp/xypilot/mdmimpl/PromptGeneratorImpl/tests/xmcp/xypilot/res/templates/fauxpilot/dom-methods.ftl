<#import "/model/json/dom-or-exception.ftl" as doe/>
<#import "/model/json/dom.ftl" as dat/>
<#--
    Template for generating methods for a DomModel.
-->
<#--  <@doe.method_schema/>  -->
<@doe.method_example/>
# available fqn: ${availableReferenceTypes?join(", ")}
{
  "name": "${domOrException.originalSimpleName}",
  "documentation": "${domOrException.documentation?json_string}",
  "public_members": [
    <@doe.member_vars/>
  ],
  <#--  "getters": [<@doe.getters/>];  -->
  <#--  "setters": [<@doe.setters/>];  -->
  "interface_methods": [
    <@dat.methods lastElementSep=true/>
    §
  ]
}
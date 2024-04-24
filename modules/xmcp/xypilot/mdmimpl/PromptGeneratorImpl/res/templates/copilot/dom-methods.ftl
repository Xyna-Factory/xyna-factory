<#import "/model/json/dom-or-exception.ftl" as doe/>
<#import "/model/json/dom.ftl" as dat/>
<#--
    Template for generating methods for a DomModel.
-->
<@doe.method_schema/>
{
  "name": "${domOrException.originalSimpleName}",
  "documentation": "${domOrException.documentation?json_string}",
  "members": [
    <@doe.member_vars/>,
  ],
  "getters": [<@doe.getters/>];
  "setters": [<@doe.setters/>];
  "interface_methods": [
    <@dat.methods lastElementSep=true/>
    §
  ]
}
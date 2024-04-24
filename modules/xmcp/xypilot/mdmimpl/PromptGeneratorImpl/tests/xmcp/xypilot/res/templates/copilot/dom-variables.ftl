<#import "/model/json/dom-or-exception.ftl" as doe/>
<#import "/model/json/dom.ftl" as dat/>
<#--
    Template for generating member variables for a DomModel.
-->
<@doe.member_schema/>
{
  "name": "${domOrException.originalSimpleName}",
  "documentation": "${domOrException.documentation?json_string}",
  "members": [
    <@doe.member_vars lastElementSep=true/>
    §
  ],
  "methods": [
    <@dat.methods/>
  ]
}
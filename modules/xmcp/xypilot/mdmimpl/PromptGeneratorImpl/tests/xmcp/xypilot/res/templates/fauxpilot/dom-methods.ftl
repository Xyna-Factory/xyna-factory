<#--
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 -->
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
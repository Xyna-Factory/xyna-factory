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
<#import "/model/json/dom.ftl" as dom/>
<#--
    Template for generating member variables for a ExceptionModel.
-->
<@doe.member_schema/>
{
  "name": "${domOrException.originalSimpleName}",
  "documentation": "${domOrException.documentation?json_string}",
  "members": [
    <@doe.member_vars/>,
    §
  ]
  <#--  TODO: add exception messages?  -->
}
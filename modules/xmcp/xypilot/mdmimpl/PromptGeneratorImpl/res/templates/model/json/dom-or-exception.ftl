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
<#import "/util/json.ftl" as json/>

<#--  json objects defining the member variables of the domOrException
  params:
    lastElementSep - boolean, if true, the last element in the list (if any) will have a trailing comma

  result:
    [memberVar],
    ...,
    [memberVar](,) <eol>
-->
<#macro member_vars lastElementSep=false>
<#list domOrException.memberVars as memberVar>
<@json.member_var memberVar/><#if lastElementSep>,<#else><#sep>,</#sep></#if>
</#list>
</#macro>


<#-- getter names for member variables of the domOrException

  result:
    "get[VarName]", ..., "get[VarName]" <inline>

  example:
    "getName", "getAge"
-->
<#macro getters>
  <@compress single_line=true>
      <#list domOrException.memberVars as memberVar>
        "${utils.getter(memberVar)}"<#sep>,
      </#list>
  </@compress>
</#macro>


<#-- setter names for member variables of the domOrException

  result:
    "set[VarName]", ..., "set[VarName]" <inline>

  example:
    "setName", "setAge"
-->
<#macro setters>
  <@compress single_line=true>
    <#list domOrException.memberVars as memberVar>
      "${utils.setter(memberVar)}"<#sep>,
    </#list>
  </@compress>
</#macro>


<#-- json schema specifying how member variables are defined, this helps the ai model to generate consistent/parsable code
    "availableVariableTypes" is part of the DomOrExceptionModel, it contains available variable types to choose from
-->
<#macro member_schema>
# JSON-like language to describe data types
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "members": {
      "description": "Member variables of the datatype",
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "fqn": {
            "type": "string",
            "enum": [${availableVariableTypes?join(", ")}]
          },
          "isList": {
            "type": "boolean"
          },
          "documentation": {
            "type": "string"
          },
          "required_attributes": [
            "name",
            "fqn",
            "documentation"
          ]
        }
      }
    },
    "methods": {
      "type": "array"
    }
  }
}
</#macro>


<#-- json example specifying how member variables are defined, this helps the ai model to generate consistent/parsable code
-->
<#macro member_example>
# Example:
# {
#   "name": "Member Name",
#   "fqn": "int"
#   "documentation": "Documentation of the member",
#   "isList": false
# }
</#macro>


<#-- json schema specifying how methods are defined, this helps the ai model to generate consistent/parsable code
    "availableParameterTypes" is part of the DomOrExceptionModel, it contains available parameter types to choose from
-->
<#macro method_schema>
# JSON-like language to describe data types
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "members": {
      "type": "array"
    },
    "getters": {
      "type": "array"
    },
    "setters": {
      "type": "array"
    },
    "interface_methods": {
      "description": "Methods specific to this datatype",
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "parameters": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "type": {
                  "type": "string",
                  "enum": ["input", "output", "exception"]
                },
                "name": {
                  "type": "string"
                },
                "fqn": {
                  "type": "string",
                  "enum": [${availableReferenceTypes?join(", ")}]
                },
                "isList": {
                  "type": "boolean"
                },
                "required": [
                  "type",
                  "name",
                  "fqn"
                ]
              }
            }
          },
          "documentation": {
            "type": "string"
          },
          "required": [
            "name",
            "documentation"
          ]
        }
      }
    }
  }
}
</#macro>


<#-- json example specifying how member methods are defined, this helps the ai model to generate consistent/parsable code
-->
<#macro method_example>
# Example:
# {
#   "name": "Method Name",
#   "parameters": [
#       {
#         "type": "input|output|exception",
#         "name": "Parameter Name",
#         "fqn": "base.Text",
#         "isList": "false",
#       }
#   ],
#   "documentation": "Documentation of the method"
# }
</#macro>
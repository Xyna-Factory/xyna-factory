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
<#-- class name of a datatype

  params:
    datatype: the datatype (DomOrExceptionGenerationBase)
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [simpleType|resolvedType] <inline>

  example:
    my.package.Type
-->
<#macro datatype_name datatype resolveTypes=false>
<@compress single_line=true>
  <#if resolveTypes>
    ${utils.resolveType(datatype.fqClassName)}
  <#else>
    ${datatype.originalSimpleName}
  </#if>
</@compress>
</#macro>


<#-- Enumerate a list of strings with a prefix and separator

  params:
    prefix: the prefix to add to each string
    start: the starting index
    count: the number of string to enumerate
    sep: the separator to use between strings

  result:
    [prefix][start][sep][prefix][start+1][sep] ... [prefix][start+count-1] <inline>

  example:
    arg1, arg2, arg3
-->
<#macro enumerate prefix start count sep=", ">
<@compress single_line=true>
    <#list start..(start + count - 1) as i>
       ${prefix}${i}<#sep>${sep}
    </#list>
</@compress>
</#macro>


<#-- Concatenate all parameters of one method to a hash

  params:
    operation: the operation to build the hash for (Operation)

  return:
    A hash with the keys "input", "output" and "exception" and the corresponding parameters as values
-->
<#function map_operation_params operation>
  <#local operationVars = {}>
  <#if (operation.inputVars?size > 0)><#local operationVars += {"input": operation.inputVars}></#if>
  <#if (operation.outputVars?size > 0)><#local operationVars += {"output": operation.outputVars}></#if>
  <#if (operation.thrownExceptions?size > 0)><#local operationVars += {"exception": operation.thrownExceptions}></#if>
  <#return operationVars>
</#function>
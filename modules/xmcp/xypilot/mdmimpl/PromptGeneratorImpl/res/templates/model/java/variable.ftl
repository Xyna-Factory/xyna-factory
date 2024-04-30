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
<#import "/util/java.ftl" as java/>


<#-- variable declaration for the target member

  result:
    private [var]; <inline>

  example:
    public Text asText()
-->
<#macro target_var>
private <@java.variable var=targetVariable resolveTypes=true/>;
</#macro>


<#-- member variables of the datatype excluding the target member, which is handled separately

    result:
      [memberVar]

      [memberVar]

      ...

      [memberVar] <eol>
-->
<#macro non_target_vars>
<#list domOrException.memberVars as var>
<#if var!=targetVariable>
<@java.member_var var=var resolveTypes=true/>
<#--  linebreak  -->

</#if>
</#list>
</#macro>
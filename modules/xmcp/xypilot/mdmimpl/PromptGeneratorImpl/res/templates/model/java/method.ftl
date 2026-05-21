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
<#import "dom-or-exception.ftl" as doe/>

<#-- documentation block for the target method to generate the implemenation for

  result:
    [methodDoc] <eol>

  example:
    Returns the text representation of the IP address.
    @return
-->
<#macro target_method_doc>
<@java.method_doc targetMethod/>
</#macro>


<#-- method header for the target method to generate the implemenation for

  result:
    public [methodHeader] <eol>

  example:
    Text asText()
-->
<#macro target_method_header>
<@java.method_header method=targetMethod resolveTypes=true/>
</#macro>


<#-- method definition of the datatype excluding the target method, which is handled separately

    result:
      [memberMethodDeclaration]

      [memberMethodDeclaration]

      ...

      [memberMethodDeclaration]<eol>
-->
<#macro non_target_methods>
<#list dom.operations as operation>
<#if operation != targetMethod>
<@java.member_method_decl method=operation resolveTypes=true/>
<#--  linebreak  -->

</#if>
</#list>
</#macro>

<#-- API reference for all dependencies of the target method, i.e. the input, output and exception types

  result:
    [datatypeRef]
    ...
    [datatypeRef]
-->
<#macro api_ref>
<#--  "targetMethodDependencies" are part of the DomMethodModel  -->
<#list targetMethodDependencies as dependency>
<@doe.datatype_ref dependency/>
<#--  linebreak  -->

</#list>
</#macro>
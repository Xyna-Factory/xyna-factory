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
<#import "/model/java/dom-or-exception.ftl" as doe/>
<#import "/model/java/dom.ftl" as dat/>
<#--
    Template for generating class documentation for a DomModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.class_header/> {
  <@doe.member_vars/>

  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@dat.member_methods_decl/>
}
// Guideline questions to answer in class description:
// What does this class represent?
<#list domOrException.memberVars as var>
// What does the variable ${var.varName} represent?
</#list>
<#list dom.operations as method>
// What does the method ${method.name} do?
</#list>

// Class description:
// ${domOrException.documentation}§

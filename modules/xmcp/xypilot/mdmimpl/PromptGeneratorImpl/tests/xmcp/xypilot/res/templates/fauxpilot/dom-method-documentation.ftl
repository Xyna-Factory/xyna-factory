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
<#import "/model/java/method.ftl" as mth/>
<#import "/util/java.ftl" as java/>
<#--
    Template for generating method documentation for a DomModel and MethodModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.documentation/>
<@doe.class_header/> {

  <@doe.member_vars/>

  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@mth.non_target_methods/>

  /**
   * <@java.inputs_doc targetMethod/>
   * <@java.outputs_doc targetMethod/>
   */
  public <@mth.target_method_header/> {
    ${targetMethod.impl}
  }
}
// Question:
// What does method ${targetMethod.name} do?

// Answer:
// ${targetMethod.documentation!""}§

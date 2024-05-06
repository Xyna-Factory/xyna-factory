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
<#import "/model/java/exception.ftl" as exc/>
<#import "/model/java/dom-or-exception.ftl" as doe/>
<#--
    Template for generating exception messages for an ExceptionModel.
-->
package ${exception.getPackageNameFromFQName(exception.fqClassName)};

/**
 * ${exception.documentation}
 */
<@doe.class_header/> {
  <@doe.member_vars/>

  <#--  TODO use different constructors for exceptions, including cause  -->
  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@exc.message_map_doc/>
  <@exc.message_list_var/> = [
    <@exc.message_entries lastElementSep=true/>§
  ]
}
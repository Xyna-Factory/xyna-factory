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
<#import "/util/api-reference.ftl" as api/>
<#import "/util/java.ftl" as java/>

<#-- class header with class name and parent class of the datatype or exception

  result:
    public [classHeader] <inline>

  example:
    public class Type extends SuperType
-->
<#macro class_header>
<@compress single_line=true>
public <@java.class_header domOrException/>
</@compress>
</#macro>


<#-- member variables of the datatype or exception each with type, name and documentation

  result:
    [memberVarDeclaration]
    [memberVarDeclaration]
    ...
    [memberVarDeclaration] <eol>

  example:
  /**
   * Name of the person
   */
  private String name;
  /**
   * Address of the person
   */
  private String address;
-->
<#macro member_vars>
<#list domOrException.memberVars as memberVar>
<@java.member_var var=memberVar resolveTypes=true/>
</#list>
</#macro>


<#-- default constructor without parameters

  result:
    public [className]() {
    } <eol>

  example:
    public Type() {
    }
-->
<#macro default_constructor>
public ${domOrException.originalSimpleName}() {
}
</#macro>


<#-- constructor with parameters, empty if there are no member variables

  result:
    public [className]([parameters]) {
      this.[varName] = [varName];
      ...
    } <eol>

  example:
  public Type(String name, String address) {
    this.name = name;
    this.address = address;
  }
-->
<#macro constructor_with_params>
<#if (domOrException.memberVars?size > 0)>
public ${domOrException.originalSimpleName}(<@java.parameters params=domOrException.memberVars resolveTypes=true/>) {
<#list domOrException.memberVars as var>
  this.${var.varName} = ${var.varName};
</#list>
}
</#if>
</#macro>


<#-- documentation for the datatype or exception, only if it is not empty

  result:
    /**
     * [documentation]
     */ <eol>

  example:
    /**
     * This type represents a person.
     */
-->
<#macro documentation>
<#if domOrException.documentation?has_content>
/**
 * ${domOrException.documentation}
 */
</#if>
</#macro>


<#-- API reference for a single datatype
  params:
    datatype: the datatype (DomOrExceptionGenerationBase) to generate the reference for
    static: true if the reference class should be static

  result:
    (static) [classHeader] {
      [constructorRef]
      [privateVariableRefs]
      [methodRefs]
    } <eol>
-->
<#macro datatype_ref datatype static=false>
<@api.class_header_ref datatype static/> {
<#list datatype.memberVars as memberVar>
  <@api.private_variable_ref memberVar/>
</#list>

<#if utils.isDOM(datatype)>
  <@api.dom_constructor_ref datatype/>
<#else>
  <@api.exception_constructor_ref datatype/>
</#if>
<#if datatype.operations?has_content>

<#list datatype.operations as operation>
  <@api.method_types_only_ref operation/>
</#list>
</#if>
}
</#macro>

<#-- API reference for all dependencies of a datatype
  params:
    static: true if the reference classes should be static

  result:
    [datatypeRef]
    ...
    [datatypeRef]
-->
<#macro api_ref static=false>
<#--  "dependencies" are part of the DomOrExceptionModel  -->
<#list dependencies as dependency>
<@datatype_ref dependency static/>
<#--  linebreak  -->

</#list>
</#macro>
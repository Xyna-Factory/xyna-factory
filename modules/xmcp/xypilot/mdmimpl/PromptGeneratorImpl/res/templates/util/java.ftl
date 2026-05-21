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
<#import "/util/functions.ftl" as fun/>


<#--
  single line documentation to append to e.g. a class header, member variable or method header,
  empty if no documentation

  params:
    documentation: the documentation (String) to create the single line documentation for

  result:
    // [documentation] <eol>

  example:
    // The name of the person
-->
<#macro single_line_documentation documentation>
<#if documentation?has_content>// ${utils.toSingleLineText(documentation)}</#if>
</#macro>


<#--
  inline documentation to append to e.g. a class header, member variable or method header,
  includes a whitespace at the beginning, empty if no documentation

  params:
    documentation: the documentation (String) to create the inline documentation for

  result:
     // [documentation] <inline>

  example:
     // The name of the person
-->
<#macro inline_documentation documentation>
<#if documentation?has_content> // ${utils.toSingleLineText(documentation)}</#if><#rt/>
</#macro>


<#-- a documentation block, empty if no documentation

  params:
    documentation: the documentation (String) to create the documentation block for

  result:
    /**
     * [documentation]
     */ <eol>

  example:
    /**
     * The name of the person
     */
-->
<#macro documentation_block documentation>
<#if documentation?has_content>
/**
 * ${documentation}
 */
</#if>
</#macro>


<#-- class header with class name and parent class of a datatype

  params:
    datatype: the datatype (DomOrExceptionGenerationBase) to create the class header for
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    class [className] extends? [superClassName] <inline>

  example:
    class my.package.Type extends my.package.SuperType
-->
<#macro class_header datatype resolveTypes=false>
<@compress single_line=true>
  class <@fun.datatype_name datatype resolveTypes/>
  <#if datatype.superClassGenerationObject??>
    extends <@fun.datatype_name datatype.superClassGenerationObject resolveTypes/>
  </#if>
</@compress>
</#macro>


<#-- variable declaration with type and name

  params:
    var: the variable (AVariable)
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name
    allowCollections: whether to allow collection types, e.g. List<String> or use array types, e.g. String[].
        True: allow collection types
        False: do not allow collection types

  result:
    [varType] [varName] <inline>

  example:
    List<String> names
-->
<#macro variable var resolveTypes=false allowCollections=true>
<@compress single_line=true>
  ${utils.finalType(var, resolveTypes, allowCollections)} ${var.varName}
</@compress>
</#macro>


<#-- member variable declaration with type, name and documentation (if any)

  params:
    var: the member variable (AVariable)
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    /**
     * [documentation]
     */
    private [variable] <eol>

  example:
    /**
    * The name of the person
    */
    private String name;
-->
<#macro member_var var resolveTypes=false>
<#-- exception documentation may be null -->
<@documentation_block var.documentation!""/>
private <@variable var resolveTypes/>;
</#macro>


<#--  Documentation for an input parameter of a method, including the name and the label of the input variable

  params:
    inputVar: the input variable (AVariable)

  result:
    @param [name] [label] <eol>

  example:
    @param netAddress Network Address
-->
<#macro input_doc inputVar>
@param ${inputVar.varName} ${inputVar.label}
</#macro>


<#-- Documentation for all inputs of a method, each including the name and the label of the input variable

  params:
    method: the method (Operation) to create the input documentation for

  result:
    [inputDoc]
    ...
    [inputDoc] <eol>

  example:
  @param netAddress Network Address
  @param subnet IPv4 Subnet
-->
<#macro inputs_doc method>
<#list method.inputVars as inputVar>
<@input_doc inputVar/>
</#list>
</#macro>


<#--  Documentation for an output parameter of a method, including the label of the output variable

  params:
    outputVar: the output variable (AVariable)

  result:
    @return [label] <eol>

  example:
    @return IPv4Subnet
-->
<#macro output_doc outputVar>
@return ${outputVar.label}
</#macro>


<#-- Documentation for all outputs of a method, each including the name and the label of the output variable

  params:
    method: the method (Operation) to create the output documentation for

  result:
    [outputDoc]
    ...
    [outputDoc] <eol>

  example:
  @return netAddress Network Address
  @return subnet IPv4 Subnet
-->
<#macro outputs_doc method>
<#list method.outputVars as outputVar>
<@output_doc outputVar/>
</#list>
</#macro>


<#-- documentation block for a method

  params:
    method: the method (Operation)

  result:
    [documentation]
    [inputDoc]
    [outputDoc]

  example:
    Calls the specified phone number.
    @param number Phone number to call
    @return true if the call was successful, false otherwise
-->
<#macro method_doc method>
${method.documentation}
<@inputs_doc method/>
<@outputs_doc method/>
</#macro>


<#-- Enumerate a list of variables as parameters

  params:
    prefix: the prefix of each parameter
    params: the list of parameters (AVariable)
    start: the enumeration index of the first parameter
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [type] [prefix][start], [type] [prefix][start+1], ... <inline>

  example:
    Integer input_0, String input_1, IPv4Address input_2
-->
<#macro enumerateParams prefix params start=0 resolveTypes=false allowCollections=true>
<@compress single_line=true>
  <#list params as var>
    ${utils.finalType(var, resolveTypes, allowCollections)} ${prefix}${var?index + start}<#sep>, 
  </#list>
</@compress>
</#macro>


<#-- parameters with types and names

  params:
    params: the parameters (List<AVariable>)
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name
    allowCollections: whether to allow collection types, e.g. List<String> or use array types, e.g. String[].

  result:
    [type] [varName], [type] [varName], ... <inline>

  example:
  String name, int age, List<String> phonenumbers
-->
<#macro parameters params resolveTypes=false allowCollections=true>
<@compress single_line=true>
  <#list params as var>
    <@variable var resolveTypes allowCollections/><#sep>, 
  </#list>
</@compress>
</#macro>


<#-- parameters with types only

  params:
    params: the parameters (List<AVariable>)
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name
    allowCollections: whether to allow collection types, e.g. List<String> or use array types, e.g. String[].

  result:
    type, type, ... <inline>

  example:
    String, int, List<String>
-->
<#macro parameters_types_only params resolveTypes=false allowCollections=true>
<@compress single_line=true>
  <#list params as var>
    ${utils.finalType(var, resolveTypes, allowCollections)}<#sep>, 
  </#list>
</@compress>
</#macro>


<#-- throws declaration, empty if no exceptions

  params:
    exceptions: the exceptions (ExceptionVariable) to create the throws declaration for
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    throws [exceptions] <inline>

  example:
    throws my.package.MyException, my.package.MyOtherException
-->
<#macro throws_declaration exceptions resolveTypes=false>
<@compress single_line=true>
  <#if (exceptions?size > 0)>
  throws <@parameters_types_only exceptions resolveTypes/>
  </#if>
</@compress>
</#macro>


<#-- header of a method

  params:
    method: the method (Operation) to create the header for
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name
    allowCollections: whether to allow collection types, e.g. List<String> or use array types, e.g. String[].

  result:
    (static) [returnTypes|void] name([parameters]) (throws) [exceptions] <inline>

  example:
    void setName(String name) throws InvalidNameException
-->
<#macro method_header method resolveTypes=false allowCollections=true>
<@compress single_line=true>
  <#if method.isStatic()>static</#if>
  ${utils.stringifyReturnTypes(method.outputVars, resolveTypes, allowCollections)}
  ${method.name}(<@parameters method.inputVars resolveTypes/>) <@throws_declaration method.thrownExceptions resolveTypes/>
</@compress>
</#macro>


<#-- header of a method, with types only, i.e. no parameter names

  params:
    method: the method (Operation) to create the header for
    resolveTypes: whether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name
    allowCollections: whether to allow collection types, e.g. List<String> or use array types, e.g. String[].

  result:
    (static) [returnTypes|void] name([parameterTypes]) (throws) [exceptions] <inline>

  example:
    void setName(String) throws InvalidNameException
-->
<#macro method_header_types_only method resolveTypes=false allowCollections=true>
<@compress single_line=true>
  <#if method.isStatic()>static</#if>
  ${utils.stringifyReturnTypes(method.outputVars, resolveTypes, allowCollections)}
  ${method.name}(<@parameters_types_only method.inputVars resolveTypes allowCollections/>) <@throws_declaration method.thrownExceptions resolveTypes/>
</@compress>
</#macro>


<#-- member method (documentation + header + body)

  params:
    method: the method (Operation)
    resolveTypes: whether to resolve imported types.
      True: simple name, if the type is imported, else FQN.
      False: simple name

  result:
    /**
     * [methodDoc]
     */
    public [methodHeader] {
      [impl]
    } <eol>

  example:
    /**
     * Adds a phone number.
     * @param number PhoneNumber
     */
    public void addPhone(String number) throws InvalidNumberException {
      if (isValid(number)) {
        phones.add(number);
      } else {
        throw new InvalidNumberException(number);
      }
    }
-->
<#macro member_method method resolveTypes=false>
/**
 * <@method_doc method/>
 */
public <@method_header method resolveTypes/> {
  ${method.impl}
}
</#macro>


<#-- member method declaration (documentation + header)

  params:
    method: the method (Operation)
    resolveTypes: whether to resolve imported types.
      True: simple name, if the type is imported, else FQN.
      False: simple name

  result:
    /**
     * [methodDoc]
     */
    public [methodHeader]; <eol>

  example:
    /**
     * Adds a phone number.
     * @param number PhoneNumber
     */
    public void addPhone(String number) throws InvalidNumberException;
-->
<#macro member_method_decl method resolveTypes=false>
/**
 * <@method_doc method/>
 */
public <@method_header method resolveTypes/>;
</#macro>
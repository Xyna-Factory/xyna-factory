<#import "functions.ftl" as fun/>
<#import "java.ftl" as java/>


<#-- class header with class name, parent class and documentation of a datatype

  params:
    datatype: the datatype (DomOrExceptionGenerationBase) to create the class header for
    static: Wether the class is static or not.
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [documentation]
    [classHeader] <inline>

  example:
    // documentation of the type
    class my.package.Type extends my.package.SuperType
-->
<#macro class_header_ref datatype static=false resolveTypes=true>
<@compress>
  <@java.single_line_documentation datatype.documentation/>
  <#if static>static </#if><@java.class_header datatype resolveTypes/>
</@compress>
</#macro>


<#-- constructor reference for a datatype

  params:
    dom: the dom (DOM) to create the constructor reference for
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [className]([parameters]); <eol>

  example:
    Type(String name, int age);
-->
<#macro dom_constructor_ref dom resolveTypes=false>
<@fun.datatype_name dom resolveTypes/>(<@java.parameters dom.memberVars resolveTypes/>);
</#macro>


<#-- constructor reference for an exception

  params:
    exception: the exception (ExceptionGenerationBase) to create the constructor reference for
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [className]([parameters], Throwable cause); <eol>

  example:
    Exception(String info, Throwable cause);
-->
<#macro exception_constructor_ref exception resolveTypes=false>
<#if exception.memberVars?size == 0>
<@fun.datatype_name exception resolveTypes/>(Throwable cause);
<#else>
<@fun.datatype_name exception resolveTypes/>(<@java.parameters exception.memberVars resolveTypes/>, Throwable cause);
</#if>
</#macro>


<#-- api reference of a variable with type, name and inline documentation

  params:
    var: the AVariable to create the reference for
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [variable]; // [documentation] <eol>

  example:
  List<String> names; // the names of the persons in the group
-->
<#macro variable_ref var resolveTypes=false allowCollections=true>
<@java.variable var resolveTypes allowCollections/>;<@java.inline_documentation var.documentation!""/>
</#macro>


<#-- hint for private variable to use getter, setter, (addTo)

  params:
    memberVar: the member (AVariable) to create the hint for

  result:
    use: set[Variable], get[Variable](, addTo[Variable])

  example:
    use: getNames, setNames, addToNames
-->
<#macro getter_setter_addto_hint memberVar>
use: ${utils.getter(memberVar)}, ${utils.setter(memberVar)}<#if memberVar.isList()>, ${utils.addTo(memberVar)}</#if>
</#macro>


<#-- api reference of a private member variable with type, name, inline documentation, and hints to use getter, setter, (addTo)

  params:
    memberVar: the member (AVariable) to create the reference for
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    // [getter_setter_addto_hint]
    private [variable]; // [documentation] <eol>

    -or-

    private [variable]; // [getter_setter_addto_hint]

  example:
  // use: getNames, setNames, addToNames
  private List<String> names; // the names of the persons in the group
-->
<#macro private_variable_ref memberVar resolveTypes=false>
<#if memberVar.documentation?has_content>
// <@getter_setter_addto_hint memberVar/>
private <@variable_ref memberVar resolveTypes/>
<#else>
private <@java.variable memberVar resolveTypes/>; // <@getter_setter_addto_hint memberVar/>
</#if>
</#macro>


<#-- getter and setter reference for a member variable

  params:
    memberVar: the member variable (AVariable) to create the references for
    resolveTypes: Wether to resolve imported types.
      True: simple name, if the type is imported, else FQN.
      False: simple name

  result:
    [variableType] [getter]()
    [setter]([variableType]) <eol>

  example:
    // The name of the person
    String getName();
    void setName(String);
-->
<#macro getter_setter_ref memberVar resolveTypes=false>
<@java.single_line_documentation memberVar.documentation/>
${utils.finalType(memberVar, resolveTypes)} ${utils.getter(memberVar)}();
${utils.setter(memberVar)}(${utils.finalType(memberVar, resolveTypes)});
</#macro>


<#-- addTo reference for list-typed member variable

  params:
    memberVar: the member variable (AVariable) to create the reference for
    resolveTypes: Wether to resolve imported types.
      True: simple name, if the type is imported, else FQN.
      False: simple name

  result:
    void [addTo]([variableType]); <eol>

  example:
    void addToPhones(String);
-->
<#macro add_to_ref memberVar resolveTypes=false>
void ${utils.addTo(memberVar)}(${utils.finalType(memberVar, resolveTypes)});
</#macro>


<#-- api reference of a method with return types, name, parameters types, thrown exceptions and documentation

  params:
    method: the method (Operation) to create the reference for
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [methodHeaderTypesOnly] // [documentation] <eol>

  example:
  void call(String); // call the person
-->
<#macro method_types_only_ref method resolveTypes=false>
<@java.method_header_types_only method resolveTypes/>;<@java.inline_documentation method.documentation!""/>
</#macro>


<#-- api reference of a method with return types, name, parameters, thrown exceptions and documentation

  params:
    method: the method (Operation) to create the reference for
    resolveTypes: Wether to resolve imported types.
        True: simple name, if the type is imported, else FQN.
        False: simple name

  result:
    [methodHeaderTypesOnly] // [documentation] <eol>

  example:
  void call(String); // call the person
-->
<#macro method_ref method resolveTypes=false>
<@java.method_header method resolveTypes/>;<@java.inline_documentation method.documentation!""/>
</#macro>
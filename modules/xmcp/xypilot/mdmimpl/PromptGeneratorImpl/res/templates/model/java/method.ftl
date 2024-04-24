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
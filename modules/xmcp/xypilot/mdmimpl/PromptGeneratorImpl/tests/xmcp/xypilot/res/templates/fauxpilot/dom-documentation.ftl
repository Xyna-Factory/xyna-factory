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

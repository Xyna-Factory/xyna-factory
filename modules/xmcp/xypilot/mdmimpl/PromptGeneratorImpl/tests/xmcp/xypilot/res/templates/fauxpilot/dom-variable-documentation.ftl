<#import "/model/java/dom-or-exception.ftl" as doe/>
<#import "/model/java/dom.ftl" as dat/>
<#import "/model/java/variable.ftl" as var/>
<#--
    Template for generating member documentation for a DomModel and MemberModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.documentation/>
<@doe.class_header/> {

  <@var.non_target_vars/>

  <@var.target_var/>

  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@dat.member_methods_decl/>
}
// Question:
// What does the member ${targetVariable.varName} represent?

// Answer:
// ${targetVariable.documentation!""}§

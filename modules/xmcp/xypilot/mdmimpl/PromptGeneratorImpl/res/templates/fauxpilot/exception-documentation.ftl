<#import "/model/java/dom-or-exception.ftl" as doe/>
<#import "/model/java/exception.ftl" as exc/>
<#--
    Template for generating class documentation for a ExceptionModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.class_header/> {
  <@doe.member_vars/>

  <@exc.message_map/>

  <#--  TODO use different constructors for exceptions, including cause  -->
  <@doe.default_constructor/>

  <@doe.constructor_with_params/>
}
// Guideline questions to answer in class description:
// What does this class represent?
// When is this exception thrown?
// What does the exception message mean?
<#list domOrException.memberVars as var>
// What does the variable ${var.varName} represent?
</#list>

// Class description:
// ${domOrException.documentation}§
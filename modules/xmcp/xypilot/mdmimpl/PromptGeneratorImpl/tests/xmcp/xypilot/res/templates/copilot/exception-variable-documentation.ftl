<#import "/model/java/dom-or-exception.ftl" as doe/>
<#import "/model/java/exception.ftl" as exc/>
<#import "/model/java/variable.ftl" as var/>
<#--
    Template for generating class documentation for a ExceptionModel and MemberModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.documentation/>
<@doe.class_header/> {

  <@var.non_target_vars/>

  /**
   * ${targetVariable.documentation!""}§
   */
  <@var.target_var/>

  <@exc.message_map/>

  <#--  TODO use different constructors for exceptions, including cause  -->
  <@doe.default_constructor/>

  <@doe.constructor_with_params/>
}
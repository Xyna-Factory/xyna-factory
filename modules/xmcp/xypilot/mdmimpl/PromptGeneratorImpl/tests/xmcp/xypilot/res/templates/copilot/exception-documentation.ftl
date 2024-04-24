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
// Class documentation must contain a abstract description of the class and at least mention the class members.
/**
 * ${domOrException.documentation}§
 */
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
// Class documentation must contain a abstract description of the class and at least mention the class members.
/**
 * ${domOrException.documentation}§
 */
<#import "/model/java/exception.ftl" as exc/>
<#import "/model/java/dom-or-exception.ftl" as doe/>
<#--
    Template for generating exception messages for an ExceptionModel.
-->
package ${exception.getPackageNameFromFQName(exception.fqClassName)};

/**
 * ${exception.documentation}
 */
<@doe.class_header/> {
  <@doe.member_vars/>

  <#--  TODO use different constructors for exceptions, including cause  -->
  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@exc.message_map_doc/>
  <@exc.message_list_var/> = [
    <@exc.message_entries lastElementSep=true/>§
  ]
}
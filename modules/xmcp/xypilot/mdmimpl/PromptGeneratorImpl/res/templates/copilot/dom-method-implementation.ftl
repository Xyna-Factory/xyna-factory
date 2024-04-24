<#import "/model/java/dom-or-exception.ftl" as doe/>
<#import "/model/java/method.ftl" as mth/>
<#--
    Template for generating method implementation for a MethodModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.api_ref/>

<@doe.documentation/>
<@doe.class_header/> {

  <@doe.member_vars/>

  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@mth.non_target_methods/>

  /**
   * <@mth.target_method_doc/>
   */
  <@mth.target_method_header/> {
    ${targetMethod.impl}§
  }
}
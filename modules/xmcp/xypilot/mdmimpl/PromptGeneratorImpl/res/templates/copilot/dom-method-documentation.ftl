<#import "/model/java/dom-or-exception.ftl" as doe/>
<#import "/model/java/method.ftl" as mth/>
<#import "/util/java.ftl" as java/>
<#--
    Template for generating method documentation for a DomModel and MethodModel.
-->
package ${domOrException.getPackageNameFromFQName(domOrException.fqClassName)};

<@doe.documentation/>
<@doe.class_header/> {

  <@doe.member_vars/>

  <@doe.default_constructor/>

  <@doe.constructor_with_params/>

  <@mth.non_target_methods/>

  /**
   * ${targetMethod.documentation}§
   *
   * <@java.inputs_doc targetMethod/>
   * <@java.outputs_doc targetMethod/>
   */
  public <@mth.target_method_header/> {
    ${targetMethod.impl}
  }
}
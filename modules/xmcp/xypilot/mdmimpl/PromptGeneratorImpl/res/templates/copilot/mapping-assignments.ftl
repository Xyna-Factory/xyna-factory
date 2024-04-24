<#import "/model/java/mapping.ftl" as map/>
<#--
    Template for generating mapping assignments for a MappingModel.
-->
static class {

  <@map.builtins_decl/>

  <@map.api_ref/>

  <@map.output_vars/>

  /**
   * @brief ${mapping.label}
   * ${mapping.documentation}
   */
  void set(<@map.input_params/>) {
    <@map.assignments input="" output="this"/>§
  }

}
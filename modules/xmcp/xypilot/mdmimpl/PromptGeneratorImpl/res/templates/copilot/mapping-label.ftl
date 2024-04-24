<#import "/model/java/mapping.ftl" as map/>
<#--
    Template for generating a mapping label for a MappingModel.
-->
static class Mapping {

  class Input {
    <@map.input_vars/>
  }

  class Output {
    <@map.output_vars/>
  }

  /**
   * @brief ${mapping.label} §
   * ${mapping.documentation}
   */
  static Output map(Input in) {
    Output out = new Output();

    <@map.doc/>
    <@map.assignments/>

    return out;
  }

}
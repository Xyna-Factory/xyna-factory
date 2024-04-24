<#import "/model/java/mapping.ftl" as map/>
<#--
    Template for generating a mapping label for a MappingModel.
-->
<@map.api_ref_light/>

class {

  <@map.output_vars/>

  <@map.doc_block/>
  void init(<@map.input_params/>) {
    <@map.assignments input="" output="this"/>
    // Question: -What does the code above do?
    // Answer: -${mapping.label}§
  }

}
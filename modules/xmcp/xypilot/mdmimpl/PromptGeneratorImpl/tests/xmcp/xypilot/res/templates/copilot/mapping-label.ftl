<#--
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 -->
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
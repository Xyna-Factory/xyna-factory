/*
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
 */
package com.gip.xyna.openapi.codegen.utils;

import org.openapitools.codegen.utils.CamelizeOption;
import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.CamelizeOption.UPPERCASE_FIRST_CHAR;

public class Camelizer {

  public static enum Case { 
    CAMEL(LOWERCASE_FIRST_LETTER), PASCAL(UPPERCASE_FIRST_CHAR);
    
    CamelizeOption option;
    
    Case (CamelizeOption option) {
      this.option = option;
    }
  }
  
  public static String camelize(String value, Case camelCase) {
    if (value == null) {
      return null;
    }
    return org.openapitools.codegen.utils.StringUtils.camelize(value.replace(" ", "_"), camelCase.option);
  }
}

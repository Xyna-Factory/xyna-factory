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

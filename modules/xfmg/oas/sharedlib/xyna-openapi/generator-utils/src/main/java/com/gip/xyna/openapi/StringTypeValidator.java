/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.openapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTypeValidator extends PrimitiveTypeValidator<String> {

    private Integer min;
    private Integer max;
    private String format;
    private String pattern;
    
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    
    private static final Map<String, Function<String, Boolean>> FormatValidatorMap = buildFormatValidatorMap();
    
    
    private static Map<String, Function<String, Boolean>> buildFormatValidatorMap() {
      Map<String, Function<String, Boolean>> result = new HashMap<String, Function<String, Boolean>>();
      result.put("uuid", createFormatValidatorFunction(UUID_REGEX));
      
      return result;
    }
    
    
    private static Function<String, Boolean> createFormatValidatorFunction(String pattern) {
      return toCheck -> checkPattern(pattern, toCheck);
    }
    
    public void setFormat(String f) {
        format = f;
    }

    public void setPattern(String p) {
        pattern = p;
    }

    public void setMinLength(Integer m) {
        min = m;
    }

    public void setMaxLength(Integer m) {
        max = m;
    }

    @Override
    public List<String> checkValid() {
        List<String> errorMessages = super.checkValid();

        if (isNull()) {
            return errorMessages;
        }

        if (!checkMinLength()) {
            errorMessages.add(String.format(
                "%s: String value \"%s\" is to short, minimum length is %d", getName(), getValue(), min)
            );
        }
        
        if (!checkMaxLength()) {
            errorMessages.add(String.format(
                "%s: String value \"%s\" is to long, maximum length is %d", getName(), getValue(), max)
            );
        }

        if (!checkPattern()) {
            errorMessages.add(String.format(
                "%s: Value \"%s\" does not match pattern \"%s\"", getName(), getValue(), pattern)
            );
        }
        
        if (!checkFormat()) {
          errorMessages.add(String.format(
                "%s: Value \"%s\" does not match format \"%s\"", getName(), getValue(), pattern)
          );
        }

        return errorMessages;
    }

    private boolean checkPattern() {
      return checkPattern(pattern, getValue());
    }
    
    private static boolean checkPattern(String pattern, String toCheck) {
      if (pattern == null)
        return true;

    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(toCheck);
    return m.find();
    }
    
    private boolean checkFormat() {
      if(format == null) {
        return true;
      }
      
      Function<String, Boolean> validatorFunction = FormatValidatorMap.getOrDefault(format, null);
      if (validatorFunction == null) {
        return true; //unknown format
      }
      
      return validatorFunction.apply(getValue());
    }

    private boolean checkMaxLength() {
        return max == null || (max != null && getValue().length() <= max);
    }

    private boolean checkMinLength() {
        return min == null || (min != null && getValue().length() >= min);
    }

}

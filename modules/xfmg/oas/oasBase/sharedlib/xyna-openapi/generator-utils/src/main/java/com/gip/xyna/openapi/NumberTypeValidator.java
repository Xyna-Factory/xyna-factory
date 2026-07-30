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



public class NumberTypeValidator<N extends Number & Comparable<N>> extends PrimitiveTypeValidator<N> {

  private boolean excludeMin; // if set to true, the condition value > min must be valid, else value >= min
  private boolean excludeMax; // analog to excludeMin
  private N multipleOf;
  private N min;
  private N max;
  private String format;

  private final Map<String, Function<N, Boolean>> FormatValidatorMap = buildFormatValidatorMap();


  private Map<String, Function<N, Boolean>> buildFormatValidatorMap() {
    Map<String, Function<N, Boolean>> result = new HashMap<String, Function<N, Boolean>>();
    // no number format validation yet
    return result;
  }


  public void setMin(N min) {
    this.min = min;
  }


  public void setMax(N max) {
    this.max = max;
  }


  public void setMultipleOf(N m) {
    multipleOf = m;
  }


  public void setExcludeMin() {
    excludeMin = true;
  }


  public void setExcludeMax() {
    excludeMax = true;
  }


  public void setFormat(String f) {
    format = f.toLowerCase();
  }


  @Override
  public List<String> checkValid() {
    List<String> errorMessages = super.checkValid();

    if (isNull()) {
      return errorMessages;
    }

    if (!checkMultipleOf()) {
      errorMessages.add(String.format("%s: Value %s is not multiple of %s", getName(), getValue().toString(), multipleOf.toString()));
    }

    if (!checkMin()) {
      String condition = excludeMin ? ">" : ">=";
      errorMessages.add(String.format("%s: Value is %s but must be %s %s", getName(), getValue().toString(), condition, min.toString()));
    }

    if (!checkMax()) {
      String condition = excludeMax ? "<" : "<=";
      errorMessages.add(String.format("%s: Value is %s but must be %s %s", getName(), getValue().toString(), condition, max.toString()));
    }

    if (!checkFormat()) {
      errorMessages.add(String.format("%s: Value \"%s\" is not of type \"%s\"", getName(), getValue(), format));
    }

    return errorMessages;
  }


  private boolean checkMultipleOf() {
    boolean valid = true;
    if (multipleOf != null) {
      if (multipleOf.intValue() == 0) {
        return getValue().doubleValue() == 0;
      }
      valid = !isNull();
      if (multipleOf instanceof Long) {
        valid = valid && (getValue().longValue() % multipleOf.longValue() == 0L);
      } else if (multipleOf instanceof Integer) {
        valid = valid && (getValue().intValue() % multipleOf.intValue() == (int) 0);
      } else if (multipleOf instanceof Double) {
        valid = valid && (getValue().doubleValue() % multipleOf.doubleValue() == (double) 0.0);
      } else if (multipleOf instanceof Float) {
        valid = valid && (getValue().floatValue() % multipleOf.floatValue() == (float) 0.0);
      }
    }
    return valid;
  }


  private boolean checkMin() {
    boolean valid = true;
    if (min != null && !isNull()) {
      int comparsion = min.compareTo(getValue());
      if (excludeMin) {
        comparsion++;
      }
      if (comparsion > 0) {
        valid = false;
      }
    }
    return valid;
  }


  private boolean checkMax() {
    boolean valid = true;
    if (max != null && !isNull()) {
      int comparsion = max.compareTo(getValue());
      if (excludeMax) {
        comparsion--;
      }
      if (comparsion < 0) {
        valid = false;
      }
    }
    return valid;
  }


  private boolean checkFormat() {
    if (format == null) {
      return true;
    }

    Function<N, Boolean> validatorFunction = FormatValidatorMap.getOrDefault(format, null);
    if (validatorFunction == null) {
      return true; //unknown format
    }

    return validatorFunction.apply(getValue());
  }
}

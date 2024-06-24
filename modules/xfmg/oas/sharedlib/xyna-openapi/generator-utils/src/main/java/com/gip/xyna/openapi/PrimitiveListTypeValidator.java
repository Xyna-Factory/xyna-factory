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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PrimitiveListTypeValidator<T, V extends PrimitiveTypeValidator<T>> extends BaseValidator {

    private List<V> validators;
    private Supplier<V> dummy;
    
    // properties to check
    private Integer minItems;
    private Integer maxItems;

    public PrimitiveListTypeValidator(Supplier<V> dummy) {
      this.dummy = dummy;
    }

    public void setDummy(Supplier<V> dummy) {
      this.dummy = dummy;
    }

    public void setMinItems(Integer minItems) {
        this.minItems = minItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    boolean isNull() {
        return validators == null;
    }

    boolean isEmpty() {
        return validators.isEmpty();
    }
    
    public List<V> getValidators() {
        return validators;
    }

    @Override
    public void setName(String name) {
      super.setName(name);
      if (validators != null) {
        validators.forEach(validator -> validator.setName(name));
      }
    }

    // initialize validators and set values
    public void setValue(List<T> value) {
      if (value == null) {
        validators = null;
      } else {
          validators = new ArrayList<>();
          if (dummy != null) {
            for (T val : value) {
              V newValidator = dummy.get();
              newValidator.setName(getName());
              newValidator.setValue(val);
              validators.add(newValidator);
            }
          }
        }
    }
    
    // getter to simplify forEach calls from constraints.
    public List<V> getValidatorsNonNull() {
        if (validators==null) {
          return (new ArrayList<>());
      } else {
          return validators;
      }
    }
    
    @Override
    public List<String> checkValid() {
      List<String> errorMessages = new ArrayList<>();
      
      if (!isNull()) {
        int listsize = validators.size();
        if (minItems != null && listsize < minItems)
        {
          errorMessages.add(this.getName()+": List of primitive type must have at least "+minItems+" items but has fewer");
        }
        if (maxItems != null && listsize > maxItems)
        {
          errorMessages.add(this.getName()+": List of primitive type must not exceed "+maxItems+" items but has more");
        }
        if (!isEmpty()) {
            for (PrimitiveTypeValidator<?> val : validators) {
              errorMessages.addAll(val.checkValid());
            }
        }
      } else if (this.getRequired()) {
          errorMessages.add(this.getName()+": List of primitive type is required but is null");
      }
      
      return errorMessages;
  }
}

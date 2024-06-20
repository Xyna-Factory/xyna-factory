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

    private List<V> validators = new ArrayList<>();
    private Supplier<V> dummy;
    
    // properties to check
    private Integer minItems;
    private Integer maxItems;
    
    // property to check for null
    private boolean noList;

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
        return noList;
    }

    boolean isEmpty() {
        return validators.isEmpty();
    }

    @Override
    public void setName(String name) {
      super.setName(name);
      validators.forEach(validator -> validator.setName(name));
    }

    public void addValues(List<T> value) {
      if (value != null && dummy != null) {
        for (T val : value) {
          V newValidator = dummy.get();
          newValidator.setName(getName());
          newValidator.setValue(val);
          validators.add(newValidator);
        }
      } else if (value == null) {
          noList = true;
      }
    }

    public List<V> getValidators() {
        return validators;
    }
    
    @Override
    public List<String> checkValid() {
      List<String> errorMessages = new ArrayList<>();
      
      if (!isNull()) {
        int listsize = getValidators().size();
        if (minItems != null && listsize < minItems)
        {
          errorMessages.add(this.getName()+": List of primitive type must have at least "+minItems+" items but has fewer");
        }
        if (maxItems != null && listsize > maxItems)
        {
          errorMessages.add(this.getName()+": List of primitive type must not exceed "+maxItems+" items but has more");
        }
        if (!isEmpty()) {
            for (PrimitiveTypeValidator<?> val : getValidators()) {
              errorMessages.addAll(val.checkValid());
            }
        }
      } else if (this.getRequired()) {
          errorMessages.add(this.getName()+": List of primitive type is required but is null");
      }
      
      return errorMessages;
  }
}

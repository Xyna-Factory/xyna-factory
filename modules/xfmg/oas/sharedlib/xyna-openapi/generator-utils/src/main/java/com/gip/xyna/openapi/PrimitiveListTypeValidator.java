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

public class PrimitiveListTypeValidator<T> extends BaseValidator {

    private List<PrimitiveTypeValidator<T>> validators = new ArrayList<>();
    private Supplier<PrimitiveTypeValidator<T>> dummy;
    
    public PrimitiveListTypeValidator(Supplier<PrimitiveTypeValidator<T>> dummy) {
      this.dummy = dummy;
    }
    
    public void setDummy(Supplier<PrimitiveTypeValidator<T>> dummy) {
      this.dummy = dummy;
    }
    
    @Override
    public void setName(String name) {
      super.setName(name);
      validators.forEach(validator -> validator.setName(name));
    }
    
    public void addValues(List<T> value) {
      if (value != null && dummy != null) {
        for (T val : value) {
          PrimitiveTypeValidator<T> newValidator = dummy.get();
          newValidator.setName(getName());
          newValidator.setValue(val);
          validators.add(newValidator);
        }
      }
    }

    public List<PrimitiveTypeValidator<T>> getValidators() {
        return validators;
    }
    
    @Override
    public List<String> checkValid() {
      List<String> errorMessages = super.checkValid();
      
      if (!isNull()) {
        for (PrimitiveTypeValidator<?> val : getValidators()) {
          errorMessages.addAll(val.checkValid());
        }
      }
      
      return errorMessages;
  }

    @Override
    boolean isNull() {
        return validators == null || validators.isEmpty();
    }

}

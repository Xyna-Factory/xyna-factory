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



import java.util.List;



public class PrimitiveTypeValidator<T> extends BaseValidator {

  private T value;
  private List<T> allowableValues;


  @SuppressWarnings("unchecked")
  public void setAllowableValues(T... values) {
    allowableValues = List.of(values);
  }


  public void setValue(T value) {
    this.value = value;
  }


  public T getValue() {
    return value;
  }


  @Override
  boolean isNull() {
    return value == null;
  }


  @Override
  public List<String> checkValid() {
    List<String> errorMessages = super.checkValid();

    if (!checkPossibleValues()) {
      errorMessages.add(String.format("%s: Enum value \"%s\" is not one of %s", getName(), getValue(), allowableValues.toString()));
    }

    return errorMessages;
  }


  private boolean checkPossibleValues() {
    if (allowableValues != null) {
      return -1 != allowableValues.indexOf(getValue());
    }
    return true;
  }

}

/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xprc.xpce.parameterinheritance.rules;

import java.util.Objects;

/**
 * Parameter-Vererbungsregel mit einem Integer-Wert.
 *
 */
public class IntegerInheritanceRule extends InheritanceRule {

  private static final long serialVersionUID = 1L;

  private Integer value;
  
  public IntegerInheritanceRule(Integer value) {
    super();
    this.value = value;
  }

  public IntegerInheritanceRule(IntegerInheritanceRule inheritanceRule) {
    super(inheritanceRule);
    this.value = inheritanceRule.value;
  }

  @Override
  public Integer getValueAsInt() {
    return value;
  }

  @Override
  public String getValueAsString() {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  @Override
  public String getUnevaluatedValue() {
    return getValueAsString();
  }
  
  @Override
  protected InheritanceRule clone() {
    return new IntegerInheritanceRule(this);
  }


  @Override
  public int hashCode() {
    int h = super.hashCode();
    return Objects.hash(h, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    IntegerInheritanceRule other = (IntegerInheritanceRule) obj;
    if (value == null) {
      if (other.value != null)
        return false;
    }
    else if (!value.equals(other.value))
      return false;
    return true;
  }
  
  
}

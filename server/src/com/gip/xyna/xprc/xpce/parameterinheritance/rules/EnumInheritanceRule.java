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


public class EnumInheritanceRule<E extends Enum<E>> extends InheritanceRule {

  private static final long serialVersionUID = 1L;
  
  private E value;
  
  public EnumInheritanceRule(E value) {
    this.value = value;
  }
  
  public EnumInheritanceRule(EnumInheritanceRule<E> rule) {
    super(rule);
    this.value = rule.value;
  }
  
  @Override
  public Integer getValueAsInt() {
    return value.ordinal();
  }


  @Override
  public String getValueAsString() {
    return value.name();
  }


  @Override
  protected InheritanceRule clone() {
    return new EnumInheritanceRule<E>(this);
  }

  @Override
  public String getUnevaluatedValue() {
    return getValueAsString();
  }

}

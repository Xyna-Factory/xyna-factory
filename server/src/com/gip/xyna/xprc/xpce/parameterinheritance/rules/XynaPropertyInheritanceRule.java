/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.Configuration;


/**
 * Parameter-Vererbungsregel, deren Wert über eine XynaProperty festgelegt wird.
 *
 */
public class XynaPropertyInheritanceRule extends InheritanceRule {
  
  private static final long serialVersionUID = 1L;

  private String xynaPropertyName;
  
  
  public XynaPropertyInheritanceRule(String xynaPropertyName) {
    super();
    this.xynaPropertyName = xynaPropertyName;
  }

  public XynaPropertyInheritanceRule(XynaPropertyInheritanceRule inheritanceRule) {
    super(inheritanceRule);
    this.xynaPropertyName = inheritanceRule.xynaPropertyName;
  }
  
  
  @Override
  public Integer getValueAsInt() {
    String value = getValueAsString();
    if (value == null) {
      return null;
    }
    
    return Integer.valueOf(value);
  }

  @Override
  public String getValueAsString() {
    if (xynaPropertyName == null) {
      return null;
    }
    
    Configuration configuration = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
    return configuration.getProperty(xynaPropertyName);
  }

  @Override
  public String getUnevaluatedValue() {
    return xynaPropertyName;
  }
  
  
  @Override
  protected InheritanceRule clone() {
    return new XynaPropertyInheritanceRule(this);
  }


  @Override
  public int hashCode() {
    int h = super.hashCode();
    return Objects.hash(h, xynaPropertyName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    XynaPropertyInheritanceRule other = (XynaPropertyInheritanceRule) obj;
    if (xynaPropertyName == null) {
      if (other.xynaPropertyName != null)
        return false;
    }
    else if (!xynaPropertyName.equals(other.xynaPropertyName))
      return false;
    return true;
  }

  public String getPropertyName() {
    return xynaPropertyName;
  }

}

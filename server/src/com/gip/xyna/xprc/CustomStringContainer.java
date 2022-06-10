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

package com.gip.xyna.xprc;

import java.io.Serializable;


public class CustomStringContainer implements Serializable {

  private static final long serialVersionUID = 2054906262472616751L;
  
  private String custom0;
  private String custom1;
  private String custom2;
  private String custom3;


  public CustomStringContainer(String custom0, String custom1, String custom2, String custom3) {
    this.custom0 = custom0;
    this.custom1 = custom1;
    this.custom2 = custom2;
    this.custom3 = custom3;
  }


  public void setCustom1(String custom1) {
    this.custom0 = custom1;
  }


  public String getCustom0() {
    return custom0;
  }


  public void setCustom2(String custom2) {
    this.custom1 = custom2;
  }


  public String getCustom1() {
    return custom1;
  }


  public void setCustom3(String custom3) {
    this.custom2 = custom3;
  }


  public String getCustom2() {
    return custom2;
  }


  public void setCustom4(String custom4) {
    this.custom3 = custom4;
  }


  public String getCustom3() {
    return custom3;
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj == this) {
      return true;
    } else if (obj instanceof CustomStringContainer) {
      CustomStringContainer other = (CustomStringContainer) obj;
      if (!String.valueOf(custom0).equals(String.valueOf(other.custom0))) {
        return false;
      }
      if (!String.valueOf(custom1).equals(String.valueOf(other.custom1))) {
        return false;
      }
      if (!String.valueOf(custom2).equals(String.valueOf(other.custom2))) {
        return false;
      }
      if (!String.valueOf(custom3).equals(String.valueOf(other.custom3))) {
        return false;
      }
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return String.valueOf(custom0).hashCode() + String.valueOf(custom1).hashCode() + String.valueOf(custom2).hashCode() + String.valueOf(custom3).hashCode();
  }

}

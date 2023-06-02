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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class DeploymentItemIdentificationBase implements DeploymentItemIdentifier {
  
  protected XMOMType type;
  protected final String name;
  private String specialType; //FIXME enum verwenden?

  // serialization
  protected DeploymentItemIdentificationBase() {
    type = null;
    name = null;
  }
  
  public DeploymentItemIdentificationBase(XMOMType type, String name) {
    this.type = type;
    this.name = name;
  }

  public XMOMType getType() {
    return type;
  }
  
  public void setType(XMOMType type) {
    this.type = type;
  }


  public String getName() {
    return name;
  }

  public void setSpecialType(String specialType) {
    this.specialType = specialType;
  }
  
  public String getSpecialType() {
    return specialType;
  }

  private transient int h;

  @Override
  public int hashCode() {
    if (h == 0) {
      h = getName().hashCode();
    }
    return h;
  }


 public boolean equals(Object obj) {
   if (obj == this) {
     return true;
   }
   if (!(obj instanceof DeploymentItemIdentifier)) {
     return false;
   }
   DeploymentItemIdentifier otherObj = (DeploymentItemIdentifier) obj;
   return getName().equals(otherObj.getName());
 }

  @Override
  public String toString() {
    return (type == null ? "UNKNOWN" : type.toString()) + " " + getName();
  }
  
  
  public static DeploymentItemIdentifier of(XMOMType type, String fqName) {
    return new DeploymentItemIdentificationBase(type, fqName);
  }
  
  public boolean isReservedObject() {
    return GenerationBase.isReservedServerObjectByFqOriginalName(getName());
  }
 
}

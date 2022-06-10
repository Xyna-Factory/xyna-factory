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

package com.gip.xyna.xnwh.persistence;



public enum ODSConnectionType {

  //TODO wichtig wieso??
  //Index wichtig, er stellt dar, wie "entfernt" oder "schwer zugänglich" daten sind.
  //zb könnte default=speicher, alternative=festplatte, history=netzlaufwerk sein.
  DEFAULT(0), ALTERNATIVE(1), HISTORY(2), INTERNALLY_USED(3);

  private int index;
  
  private ODSConnectionType(int index) {
    this.index = index;
  }
  
  
  public int getIndex() {
    return this.index;
  }
  
  
  public static ODSConnectionType getByIndex(int conTypeIndex) {
    for (ODSConnectionType type : ODSConnectionType.values()) {
      if (type.index == conTypeIndex) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid ODSConnectionType index: " + conTypeIndex); 
  }
  
  
  public static ODSConnectionType getByString(String conTypeString) {
    for (ODSConnectionType conType : values()) {
      if (conType.toString().equalsIgnoreCase(conTypeString)) {
        return conType;
      }
    }
    throw new IllegalArgumentException("Invalid connectionType identifier: " + conTypeString);
  }
  
  
}

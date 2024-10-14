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
package com.gip.xyna.utils.db.types;

/**
 * Wrapper, um einen Boolean, der Null sein kann,
 * für die Auswertung durch ExtendedParameter zu speichern.
 * Grund: An einem Null-Objekt kann der Typ nicht mehr erkannt werden
 */
public class BooleanWrapper {
  
  private Boolean b;
  
  /**
   * Default-Konstuktor
   */
  public BooleanWrapper() {
  }
  
  /**
   * Konstruktor aus Boolean
   * @param b
   */
  public BooleanWrapper( Boolean b ) {
    this.setBoolean(b);
  }
  
  /**
   * Konstruktor aus boolean
   * @param b
   */
  public BooleanWrapper( boolean b ) {
    this.setBoolean(Boolean.valueOf(b));
  }
  
  /**
   * @return Boolean is not set
   */
  public boolean isNull() {
    return getBoolean() == null;
  }
  
  /**
   * @return boolean value
   */
  public boolean booleanValue() {
    return getBoolean().booleanValue();
  }

  /**
   * Setter
   * @param b
   */
  public void setBoolean(Boolean b) {
    this.b = b;
  }

  /**
   * Getter
   * @return
   */
  public Boolean getBoolean() {
    return b;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    if( b == null ) {
      return "NULL";
    } else {
      return b ? "TRUE" : "FALSE";
    }
    
  }
}

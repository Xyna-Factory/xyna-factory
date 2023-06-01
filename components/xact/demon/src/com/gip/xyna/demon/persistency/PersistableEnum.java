/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.demon.persistency;

/**
 * Persistent Enum
 *
 */
public class PersistableEnum<EnumType extends Enum<EnumType>> implements Persistable {

  private EnumType enumValue;
  private String uniqueName;
  private Class<EnumType> enumType;
  
  /**
   * Konstruktor
   * @param uniqueName
   * @param enumType
   */
  public PersistableEnum( String uniqueName, Class<EnumType> enumType ) {
    this.uniqueName = uniqueName;
    this.enumType = enumType;
  }

  /**
   * @param initializing
   */
  public void set(EnumType e) {
    this.enumValue = e;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.persistency.Persistable#getPersistentValue()
   */
  public String getPersistentValue() {
    return enumValue.name();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.persistency.Persistable#getUniqueName()
   */
  public String getUniqueName() {
    return uniqueName;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.persistency.Persistable#setPersistentValue(java.lang.String)
   */
  public void setPersistentValue(String value) {
    if( value == null ) {
      enumValue = enumType.getEnumConstants()[0];
    } else {
      enumValue = Enum.valueOf(enumType, value.toUpperCase());
    }
  }

  /**
   * vergleicht den Enum
   * @param e
   * @return
   */
  public boolean is(EnumType e) {
    return enumValue == e;
  }

  /**
   * Liefert den Enum
   * @return
   */
  public EnumType get() {
    return enumValue;
  }

}

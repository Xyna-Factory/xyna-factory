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
package com.gip.xyna.xprc.xfractwfe.formula;


//TODO mit Avariable.primitiveType verschmelzen
public enum BaseType {

  LONG_OBJECT("Long", true, false), 
  LONG_PRIMITIVE("long", true, true), 
  INT_OBJECT("Integer", true, false),
  INT_PRIMITIVE("int", true, true), 
  STRING("String", false, false), 
  BOOLEAN_OBJECT("Boolean", false, false),
  BOOLEAN_PRIMITIVE("boolean", false, true),
  DOUBLE_OBJECT("Double", true, false),
  DOUBLE_PRIMITIVE("double", true, true),
  FLOAT_OBJECT( "Float", true, false), 
  FLOAT_PRIMITIVE("float", true, true),
  LIST("List", false, false),
  ANYTYPE("GeneralXynaObject", false, false )
  ;

  private final String javaClass;
  private final boolean isNumber;
  private final boolean isPrimitive;


  private BaseType(String javaClass, boolean isNumber, boolean isPrimitive) {
    this.javaClass = javaClass;
    this.isNumber = isNumber;
    this.isPrimitive = isPrimitive;
  }


  public String getJavaClass() {
    return javaClass;
  }


  public boolean isNumber() {
    return isNumber;
  }


  public boolean isPrimitive() {
    return isPrimitive;
  }


  public boolean isSameTypeIgnoringPrimitiveness(BaseType t) {
    if (t == null) {
      return false;
    }
    if (t == this) {
      return true;
    }
    switch (t) {
      case BOOLEAN_OBJECT :
        return this == BOOLEAN_PRIMITIVE;
      case BOOLEAN_PRIMITIVE :
        return this == BOOLEAN_OBJECT;
      case DOUBLE_OBJECT :
        return this == DOUBLE_PRIMITIVE;
      case DOUBLE_PRIMITIVE :
        return this == DOUBLE_OBJECT;
      case FLOAT_OBJECT :
        return this == FLOAT_PRIMITIVE;
      case FLOAT_PRIMITIVE :
        return this == FLOAT_OBJECT;
      case INT_OBJECT :
        return this == INT_PRIMITIVE;
      case INT_PRIMITIVE :
        return this == INT_OBJECT;
      case LONG_OBJECT :
        return this == LONG_PRIMITIVE;
      case LONG_PRIMITIVE :
        return this == LONG_OBJECT;
      default :
        return false;
    }
  }


  public static BaseType valueOfJavaName(String javaName) {
    for (BaseType b : values()) {
      if (b.getJavaClass().equals(javaName)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Enum value of name " + javaName + " does not exist.");
  }


  public boolean isBoolean() {
    return this == BOOLEAN_OBJECT || this == BOOLEAN_PRIMITIVE;
  }

}

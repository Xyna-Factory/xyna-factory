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




public class EqualsOperator extends Operator {

  public EqualsOperator(int lastIdx) {
    super(lastIdx);
  }


  public String toJavaCode() {
    return needsEquals() ? ", " : " == ";
  }


  public boolean needsClosingBrace() {
    return needsEquals();
  }


  @Override
  public String getOperatorAsString() {
    return "==";
  }
  
  
  @Override
  public String getPrefix() {
    return needsEquals() ? EqualsOperator.class.getName() + "." + OEQUALS_METHOD_NAME + "(" : "";
  }
  
  
  // Should always match the method name below (contained in generated code)
  final static String OEQUALS_METHOD_NAME = "oEquals";
  
  public static boolean oEquals(Object o1, Object o2) {
    if (o1 == null ^ o2 == null) {
      return false;
    } else if (o1 == null && o2 == null) {
      return true;
    } else if (o1 == o2) {
      return true;
    } else if (o1 instanceof Number && o2 instanceof Number) {
      return ((Number)o1).doubleValue() == ((Number)o2).doubleValue();
    } else {
      return o1.equals(o2);
    }
  }
  

}

/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.utils.exceptions.utils.codegen;



public class JavaGenUtils {

  public static String transformNameForJava(String fqClassName) throws InvalidClassNameException {
    String[] parts = fqClassName.split("\\.");
    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < parts.length; i++) {
      // keine sonderzeichen. nur buchstaben und zahlen und dollar
      if (!parts[i].matches("(\\w|[_\\$])+")) {
        throw new InvalidClassNameException(fqClassName);
      }
      if (parts[i].matches("^\\d.*")) {
        parts[i] = "_" + parts[i];
      }
      if (i > 0) {
        ret.append(".");
      }
      ret.append(parts[i]);
    }
    return ret.toString();
  }
  
  public static String getSimpleNameFromFQName(String fqClassName) {
    String[] parts = fqClassName.split("\\.");
    return parts[parts.length - 1];
  }


  public static String getPackageNameFromFQName(String fqName) {
    String[] parts = fqName.split("\\.");
    String ret = "";
    for (int i = 0; i < parts.length - 1; i++) {
      if (i > 0) {
        ret += ".";
      }
      ret += parts[i];
    }
    return ret;
  }


  public static String getSetterFor(String varName) {
    return "set" + varName.substring(0, 1).toUpperCase() + varName.substring(1);
  }

  public static String getGetterFor(String varName) {
    return "get" + varName.substring(0, 1).toUpperCase() + varName.substring(1);
  }

  public static String transformVarNameForJava(String varName) {
    return varName.replaceAll("\\s+", "_").replaceAll("\\W+", "_");
  }
}

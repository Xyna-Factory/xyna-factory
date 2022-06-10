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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class Parameter {


  private static Pattern percentSignReplacePattern = Pattern.compile("\\%", Pattern.CASE_INSENSITIVE);


  private Object[] params;

  private Pattern[] cachedPatterns;
  
  public static final Parameter EMPTY_PARAMETER = new Parameter();

  /**
   * TODO unterstützt keine null werte. dabei aufpassen, dass dieses feature nicht irgendwo verwendet wird.
   */
  public Parameter(Object... params) {
    List<Object> paraList = new ArrayList<Object>(params.length * 2);
    for (Object o : params) {
      addToList(paraList, o);
    }
    this.params = paraList.toArray(new Object[paraList.size()]);
  }


  private static void addToList(List<Object> list, Object o) {
    if (o == null) {
      return;
    }
    if (o instanceof Parameter) {
      Parameter p = (Parameter) o;
      for (Object o2 : p.params) {
        addToList(list, o2);
      }
    } else {
      list.add(o);
    }
  }


  /**
   * i-ten parameter zurückgeben
   */
  public Object get(int i) {
    return params[i];
  }


  public int size() {
    return params.length;
  }
  
  public void replace(int i, Object o) {
    params[i] = o;
  }


  public void add(Object o) {
    List<Object> list = new ArrayList<Object>(Arrays.asList(params));
    addToList(list, o);
    this.params = list.toArray(new Object[list.size()]);
  }


  public Parameter clone() {
    return new Parameter((Object[]) params);
  }


  public Pattern getParameterAsPattern(int i) {
    if (cachedPatterns == null) {
      cachedPatterns = new Pattern[params.length];
    }
    Pattern result = cachedPatterns[i];
    if (result == null) {
      result = Pattern.compile(String.valueOf(params[i]));
      cachedPatterns[i] = result;
    }
    return result;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i<params.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(params[i]);
    }
    sb.append("]");
    return sb.toString();
  }

}

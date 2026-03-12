/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl;



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class ConversionUtils {

  public static List<String> getStringList(Object o) {
    if (o == null) {
      return Collections.emptyList();
    }
    if (o instanceof List) {
      List<?> l = (List<?>) o;
      List<String> result = new ArrayList<>(l.size());
      for (Object el : l) {
        result.add(getString(el));
      }
      return result;
    }
    List<String> result = new ArrayList<>(1);
    result.add(asString(o));
    return result;
  }


  public static String getString(Object o) {
    if (o == null) {
      return null;
    }
    if (isOneElementList(o)) {
      return asString(getListElement(o));
    } else if (isEmptyList(o)) {
      return null;
    }
    return asString(o);
  }


  private static Object getListElement(Object o) {
    return ((List) o).get(0);
  }


  private static boolean isEmptyList(Object o) {
    return o instanceof List && ((List) o).isEmpty();
  }


  private static boolean isOneElementList(Object o) {
    return o instanceof List && ((List) o).size() == 1;
  }


  public static Boolean getBoolean(Object o) {
    if (o == null) {
      return null;
    }
    if (isOneElementList(o)) {
      return asBoolean(getListElement(o));
    } else if (isEmptyList(o)) {
      return null;
    }
    return asBoolean(o);
  }


  private static Boolean asBoolean(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Boolean) {
      return (Boolean) o;
    }
    if (o instanceof String) {
      String s = (String) o;
      if ("true".equalsIgnoreCase(s)) {
        return true;
      } else if ("false".equalsIgnoreCase(s)) {
        return false;
      }
    }
    throw new RuntimeException("Argument " + o + " can not be treated as a boolean");
  }


  private static String asString(Object o) {
    if (o == null) {
      return null;
    }
    return o.toString();
  }


  public static Number getNumber(Object o) {
    if (o == null) {
      return null;
    }
    if (isOneElementList(o)) {
      return asNumber(getListElement(o));
    } else if (isEmptyList(o)) {
      return null;
    }
    return asNumber(o);
  }

  public static BigDecimal asBigDecimal(Number n) {
    if (n instanceof BigDecimal) {
      return (BigDecimal) n;
    }
    else {
      return BigDecimal.valueOf(n.doubleValue());
    }
  }

  private static Number asNumber(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number) {
      return (Number) o;
    }
    String s = o.toString();
    try {
      return Long.valueOf(s);
    } catch (NumberFormatException e) {
      int edx = s.indexOf("e");
      if (s.length() > 8 || (edx > 0 && edx < s.length() - 3)) {
        return new BigDecimal(s);
      }
      return Double.valueOf(s);
    }
  }


  public static <T> T ifNull(T o, T o2) {
    return o == null ? o2 : o;
  }
}

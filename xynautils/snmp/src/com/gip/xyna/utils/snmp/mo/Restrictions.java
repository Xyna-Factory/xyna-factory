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
package com.gip.xyna.utils.snmp.mo;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class Restrictions {

  public static final String OK = "OK";
  private static final String WRONG_TYPE = "wrong type";

  private Restrictions() {/*Konstruktor darf nur intern verwendet werden*/}

  public static interface Restriction {
    String checkValue(String value);
    String checkValue(int value);
  }

  public static class RangeRestriction implements Restriction {

    int min;
    int max;

    public RangeRestriction(final int min, final int max) {
      this.min = min;
      this.max = max;
    }

    public String checkValue(final String value) {
      if( value == null ) return "RangeRestriction: value is null";
      if (value.length() < min) return "RangeRestriction: value.lenght < " + min;
      if (value.length() > max) return "RangeRestriction: value.lenght > " + max;
      return OK;
    }

    public String checkValue(final int value) {
      if (value < min) return "RangeRestriction: value < " + min;
      if (value > max) return "RangeRestriction: value > " + max;
      return OK;
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("RangeRestriction: {");
      sb.append(min);
      sb.append(", ");
      sb.append(max);
      sb.append("}");
      return sb.toString();
    }
  }

  public static class ValueRestriction implements Restriction {

    List<String> stringList;
    int[] intList;

    public ValueRestriction(final String[] strings) {
      stringList = Arrays.asList(strings);
    }

    public ValueRestriction(final int[] ints) {
      intList = ints.clone();
      Arrays.sort(intList);
    }

    public ValueRestriction(final int value) {
      intList = new int[]{ value };
    }

    public String checkValue(final String value) {
      return stringList.contains(value) ?  OK : "ValueRestriction: no legal value";
    }

    public String checkValue(final int value) {
      return Arrays.binarySearch(intList, value) >= 0 ? OK : "ValueRestriction: no legal value";
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("ValueRestriction: {");
      if (stringList != null) {
        sb.append("[");
        for (int i = 0; i < stringList.size(); ++i) {
          if (i != 0) {
            sb.append(", ");
          }
          sb.append("\"");
          sb.append(stringList.get(i));
          sb.append("\"");
        }
        sb.append("]");
      }
      if (intList != null) {
        if (stringList != null) {
          sb.append(", ");
        }
        sb.append("[");
        for (int i = 0; i < intList.length; ++i) {
          if (i != 0) {
            sb.append(", ");
          }
          sb.append(intList[i]);
        }
        sb.append("]");
      }
      sb.append("}");
      return sb.toString();
    }
  }

  public static class MappingRestriction implements Restriction {

    private Mapping mapping;

    public MappingRestriction(final Mapping mapping) {
      this.mapping = mapping;
    }

    public String checkValue(final String value) {
      return mapping.contains(value) ? OK : "MappingRestriction: no legal value in " + mapping;
    }

    public String checkValue(final int value) {
      return mapping.contains(value) ? OK : "MappingRestriction: no legal value in " + mapping;
    }

    public int mapToInt(final String value) {
      return mapping.mapToInt(value);
    }

    public String mapToString(final int value) {
      return mapping.mapToString(value);
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("MappingRestriction: {");
      sb.append(mapping);
      sb.append("}");
      return sb.toString();
    }
  }

  public static class PatternRestriction implements Restriction {

    private Pattern pattern;

    public PatternRestriction(final String pattern) {
      this.pattern = Pattern.compile(pattern);
    }

    public String checkValue(final String value) {
      if (pattern.matcher(value).matches()) {
        return OK;
      } else {
        return "PatternRestriction: value does nit match pattern " + pattern.pattern();
      }
    }

    public String checkValue(final int value) {
      return checkValue(String.valueOf(value));
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("PatternRestriction: {");
      sb.append(pattern);
      sb.append("}");
      return sb.toString();
    }
  }

  public static class OrRestriction implements Restriction {

    Restriction restriction1;
    Restriction restriction2;

    public OrRestriction(final Restriction restriction1, final Restriction restriction2) {
      this.restriction1 = restriction1;
      this.restriction2 = restriction2;
    }

    public String checkValue(final String value) {
      String check1 = restriction1.checkValue(value);
      if (OK.equals(check1)) {
        return OK;
      }
      String check2 = restriction2.checkValue(value);
      if (OK.equals(check2)) {
        return OK;
      }
      return "OrRestriction: " + check1 + ", " + check2;
    }

    public String checkValue(final int value) {
      String check1 = restriction1.checkValue(value);
      if (OK.equals(check1)) {
        return OK;
      }
      String check2 = restriction2.checkValue(value);
      if (OK.equals(check2)) {
        return OK;
      }
      return "OrRestriction: " + check1 + ", " + check2;
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("OrRestriction: {");
      sb.append(restriction1);
      sb.append(", ");
      sb.append(restriction2);
      sb.append("}");
      return sb.toString();
    }
  }

  public static String checkIntValue(final Restriction restriction, final String value) {
    if (restriction instanceof Restrictions.MappingRestriction) {
      return restriction.checkValue(value);
    } else {
      return WRONG_TYPE;
    }
  }

  public static String checkStringValue(final Restriction restriction, final int value) {
    if (restriction instanceof MappingRestriction) {
      return restriction.checkValue(value);
    } else {
      return WRONG_TYPE;
    }
  }

  public static String checkStringValue(final Restriction restriction, final String value) {
    if (restriction != null) {
      return restriction.checkValue(value);
    }
    return OK;
  }

  public static String checkIntValue(final Restriction restriction, final int value) {
    if (restriction != null) {
      return restriction.checkValue(value);
    }
    return OK;
  }

  public static boolean isMapper(final Restriction restriction) {
    return restriction instanceof MappingRestriction;
  }

  public static int map(final Restriction restriction, final String value) {
    if (restriction instanceof MappingRestriction) {
      MappingRestriction mr = (MappingRestriction) restriction;
      return mr.mapToInt(value);
    } else {
      throw new UnsupportedOperationException("No Mapping defined");
    }
  }

  public static String map(final Restriction restriction, final int value) {
    if (restriction instanceof MappingRestriction) {
      MappingRestriction mr = (MappingRestriction) restriction;
      return mr.mapToString(value);
    } else {
      throw new UnsupportedOperationException("Expected MappingRestriction but was: <" + restriction + ">.");
    }
  }
}

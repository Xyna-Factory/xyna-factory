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
package com.gip.xyna.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.misc.StringReplacer;


public class StringUtils {

  /**
   * Vergleicht zwei String inklusive Nullcheck.
   * @param string1 String oder NULL
   * @param string2 String oder NULL
   * @return true, wenn beide String NULL oder gleich sind, sonst false
   */
  public static boolean isEqual(String string1, String string2) {
    return Objects.equals(string1, string2);
  }
  
  public static String removeFromSeperatedList(String seperatedList, String entry, String seperationMarker, boolean once) {
    if (seperatedList == null || seperatedList.length() == 0) {
      return seperatedList;
    }
    StringBuilder resultBuilder = new StringBuilder();
    String[] asArray = seperatedList.split(seperationMarker);
    boolean gotHit = false;
    for (int i = 0; i < asArray.length; i++) {
      if (asArray[i].equals(entry) && (!once || !gotHit)) {
        gotHit = true;
      } else {
        resultBuilder.append(asArray[i])
                     .append(seperationMarker);
      }
    }
    if (gotHit) {
      if (resultBuilder.length() == 0) {
        return "";
      } else {
        return resultBuilder.substring(0, resultBuilder.length() - seperationMarker.length());
      }
    } else {
      return seperatedList;
    }
  }
  
  
  public static String addToSeperatedList(String seperatedList, String entry, String seperationMarker, boolean unique) {
    if (seperatedList == null || seperatedList.length() == 0) {
      return entry;
    }
    if (unique) {
      String[] asArray = seperatedList.split(seperationMarker);
      for (int i = 0; i < asArray.length; i++) {
        if (asArray[i].equals(entry)) {
          return seperatedList;
        }
      }
    }
    return seperatedList + seperationMarker + entry;
  }
  
  
  public static String javaListToSeperatedList(List<String> javaList, String seperationMarker) {
    StringBuilder resultBuilder = new StringBuilder();
    Iterator<String> iterator = javaList.iterator();
    while (iterator.hasNext()) {
      resultBuilder.append(iterator.next());
      if (iterator.hasNext()) {
        resultBuilder.append(seperationMarker);
      }
    }
    return resultBuilder.toString();
  }
  
  
  public static String joinStringArray(String[] javaList, String seperationMarker) {
    StringBuilder resultBuilder = new StringBuilder();
    
    for ( int i = 0; i < javaList.length; i++ ) {
      if ( i > 0 ) {
        resultBuilder.append(seperationMarker);
      }
      
      resultBuilder.append(javaList[i]);
    }
    
    return resultBuilder.toString();
  }
  
  
  /**
   * ohne support f�r regul�re ausdr�cke, mit limit&lt;0 am schnellsten.
   */
  public static String[] fastSplit(String s, char delimiter, int limit) {
    if (limit < 0) {
      return fastSplitInternally(s, delimiter);
    } else {
      return fastSplitAsInJava7(s, delimiter, limit);
    }
  }


  private static String[] fastSplitAsInJava7(String s, char delimiter, int limit) {
    //java7 impl
    int off = 0;
    int next = 0;
    int l = s.length();
    boolean limited = limit > 0;
    ArrayList<String> list = new ArrayList<String>();
    while ((next = s.indexOf(delimiter, off)) != -1) {
      if (!limited || list.size() < limit - 1) {
        list.add(s.substring(off, next));
        off = next + 1;
      } else { // last one
        list.add(s.substring(off, l));
        off = l;
        break;
      }
    }
    // If no match was found, return this
    if (off == 0) {
      return new String[] {s};
    }

    // Add remaining segment
    if (!limited || list.size() < limit) {
      list.add(s.substring(off, l));
    }

    // Construct result
    int resultSize = list.size();
    if (limit == 0) {
      while (resultSize > 0 && list.get(resultSize - 1).length() == 0) {
        resultSize--;
      }
    }
    String[] result = new String[resultSize];
    return list.subList(0, resultSize).toArray(result);
  }
  
  /**
   * ohne support f�r regul�re ausdr�cke
   */
  //vgl https://gist.github.com/banthar/2923321
  private static String[] fastSplitInternally(String s, char delimiter) {
    int count = 1;

    int len = s.length();
    for (int i = 0; i < len; i++) {
      if (s.charAt(i) == delimiter) {
        count++;
      }
    }

    String[] array = new String[count];

    int a = -1;
    int b = 0;

    for (int i = 0; i < count; i++) {

      while (b < len && s.charAt(b) != delimiter) {
        b++;
      }

      array[i] = s.substring(a + 1, b);
      a = b;
      b++;
    }

    return array;
  }


  /**
   * ohne support f�r regul�re ausdr�cke
   */
  //aus apache lang commons StringUtils
  public static String fastReplace(String text, String searchString, String replacement, int max) {
    if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
      return text;
    }
    int start = 0;
    int end = text.indexOf(searchString, start);
    if (end == -1) {
      return text;
    }
    int replLength = searchString.length();
    int increase = replacement.length() - replLength;
    increase = (increase < 0 ? 0 : increase);
    increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
    StringBuilder buf = new StringBuilder(text.length() + increase);
    while (end != -1) {
      buf.append(text.substring(start, end)).append(replacement);
      start = end + replLength;
      if (--max == 0) {
        break;
      }
      end = text.indexOf(searchString, start);
    }
    buf.append(text.substring(start));
    return buf.toString();
  }


  public static boolean isEmpty(String text) {
    return text == null || text.length() == 0;
  }
  
  
  /**
   * Maskieren des Zeichens hide
   * @param string
   * @param hide
   * @return
   */
  public static String mask(String string, char hide) {
    if( string == null ) {
      return "";
    }
    String ret = string.replaceAll("\\\\",  Matcher.quoteReplacement("\\\\") );
    ret = ret.replaceAll(""+hide, Matcher.quoteReplacement("\\"+hide) );
    return ret;
  }

  /**
   * Lesen des Strings string bis unmaskiertes Zeichen sep erreicht wurde. 
   * @param sb gelesener, unmaskierter String
   * @param string
   * @param sep
   * @return Index des n�chsten nicht gelesenen Zeichen im String string
   */
  public static int readMaskedUntil(StringBuilder sb, String string, char sep) {
    boolean lastBackSlash = false;
    int idx = 0;
    for( ; idx<string.length(); ++idx ) {
      char c = string.charAt(idx);
      if( c == '\\' ) {
        lastBackSlash = ! lastBackSlash;
        if( lastBackSlash ) {
          continue; //erster BackSlash gelesen
        }
      } else if ( c == sep ) {
        if( lastBackSlash ) {
          lastBackSlash = false;
        } else {
          return idx+1;
        }
      }
      sb.append(c);
    }
    return idx;
  }

  public static StringReplacer toLiteral = StringReplacer.
      replace('\\',"\\\\").
      replace('\"',"\\\"").
      //replace('\'',"\\'"). TODO n�tig?
      replace('\n',"\\n").
      replace('\r',"\\r").
      replace('\b',"\\b").
      replace('\t',"\\t").
      build();

  public static String toLiteral(String value) {
    if( value == null ) {
      return null;
    }
    return toLiteral.replace(value);
  }

  public static String replaceFirst(String source, String searchStr, String replacement) {
    String searchRegEx = Pattern.quote(searchStr);
    return source.replaceFirst(searchRegEx, replacement);
  }

  public static String replaceLast(String source, String searchStr, String replacement) {
    int lastPosSearchStr = source.lastIndexOf(searchStr);
    if (lastPosSearchStr < 0) {
      return source; // nothing to replace
    }
    
    String lastPartWithSearchStr = source.substring(lastPosSearchStr);
    String searchRegEx = Pattern.quote(searchStr);
    String lastPartWithReplacement = lastPartWithSearchStr.replaceFirst(searchRegEx, replacement);
    
    return source.substring(0, lastPosSearchStr) + lastPartWithReplacement;
  }


}

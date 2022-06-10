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

package com.gip.www.juno.DHCP.tlvdatabase;



import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;



/**
 * String to Map util.
 * 
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class StringToMapUtil {

  private static final Pattern quotePattern = Pattern.compile("((\\\\[\"\\\\])|[^\\\\])*");


  private StringToMapUtil() {
  }


  /**
   * Creates a Map<String, Set<String>> object from a provided String representation. In the String representation the
   * key-value pairs are separated by commas <,>. Each key-value pair consists of a quoted key and a value, separated by
   * equals <=>. Inside the quoted values and keys, quotes are escaped with <\"> and backslashes with <\\>.
   * 
   * @param str string to transform to map.
   * @return Map object of given String representation.
   */
  public static Map<String, Set<String>> toMapOfSets(final String str) {
    if (str == null) {
      throw new IllegalArgumentException("String may not be null.");
    }
    else if (!quotePattern.matcher(str).matches()) {
      throw new IllegalArgumentException("Illegal use of escape character <\\>: <" + str + ">.");
    }
    String[] partsArray = str.replace("\\\\", "\\b").split("((?<=[^\\\\])[\"])|(^\")", -1);
    Iterator<String> parts = Arrays.asList(partsArray).iterator();
    Map<String, Set<String>> result = new Hashtable<String, Set<String>>();
    try {
      checkEquals("", parts.next());
      if (parts.hasNext()) {
        while (true) {
          if (result.size() > 0) {
            String separator = parts.next();
            if (parts.hasNext()) {
              checkEquals(",", separator);
            }
            else {
              checkEquals("", separator);
              break;
            }
          }
          String key = unescape(parts.next());
          checkEquals("=", parts.next());
          String value = unescape(parts.next());
          if (!result.containsKey(key)) {
            result.put(key, new HashSet<String>());
          }
          if (result.get(key).contains(value)) {
            throw new IllegalArgumentException("Value <" + value + "> for key <" + key + "> defined more than once.");
          }
          result.get(key).add(value);
        }
      }
    }
    catch (NoSuchElementException e) {
      throw new IllegalArgumentException("Error while reading last key value pair.", e);
    }
    return result;
  }


  private static void checkEquals(final String expectedValue, final String value) {
    if (!expectedValue.equals(value)) {
      throw new IllegalArgumentException("Expected <" + expectedValue + ">, but found: <" + value + ">.");
    }
  }


  private static String unescape(final String value) {
    return value.replace("\\b", "\\").replace("\\\"", "\"");
  }


  /**
   * Creates a Map<String, String> object from a provided String representation. In the String representation the
   * key-value pairs are separated by commas <,>. Each key-value pair consists of a quoted key and a value, separated by
   * equals <=>. Inside the quoted values and keys, quotes are escaped with <\"> and backslashes with <\\>.
   * 
   * @param str string to transform to map.
   * @return Map object of given String representation.
   */
  public static Map<String, String> toMap(final String str) {
    Map<String, Set<String>> mapWithSets = toMapOfSets(str);
    Map<String, String> result = new Hashtable<String, String>();
    for (Map.Entry<String, Set<String>> entry : mapWithSets.entrySet()) {
      if (entry.getValue().size() != 1) {
        throw new IllegalArgumentException("Found key with multiple definitions: <" + entry + ">.");
      }
      result.put(entry.getKey(), entry.getValue().iterator().next());
    }
    return result;
  }
}

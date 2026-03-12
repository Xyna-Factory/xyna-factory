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



import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;



public class WhiteListUtils {

  private static final Logger logger = CentralFactoryLogging.getLogger(WhiteListUtils.class);


  public static String removePathsFromJSON(String json, List<String> paths) {
    if (paths == null) {
      return json;
    }
    for (String p : paths) {
      json = removePathFromJSON(json, p);
    }
    return json;
  }


  public static String removePathFromJSON(String json, String pathToRemove) {
    DocumentContext dc = JsonPath.using(Configuration.defaultConfiguration()).parse(json);
    JsonPath jp = JsonPath.compile(pathToRemove);
    try {
      dc = dc.delete(jp);
    } catch (PathNotFoundException e) {
      //ok
    }
    return JSONValue.toJSONString(dc.read("$"));
  }


  public static boolean isJSONPartTheSame(String json1, String json2, String path) {
    String s1 = extractPathFromJSON(json1, path);
    String s2 = extractPathFromJSON(json2, path);
    Object o1 = JSONValue.parse(s1);
    Object o2 = JSONValue.parse(s2);
    return Objects.equals(o1, o2) || (empty(o1) && empty(o2));
  }



  private static boolean empty(Object o) {
    if (o == null) {
      return true;
    }
    if (o instanceof JSONArray) {
      JSONArray a = (JSONArray) o;
      if (a.size() == 0) {
        return true;
      }
    }
    return false;
  }


  public static String extractPathFromJSON(String json, String path) {
    try {
      Object o = JsonPath.using(Configuration.defaultConfiguration()).parse(json).read(path);
      return JSONValue.toJSONString(o);
    } catch (PathNotFoundException e) {
      return "null";
    }
  }


  public static class JSONCorrelationDetails {

    public final String jsonLeft;
    public final String jsonRight;
    public final int sumLen;


    public JSONCorrelationDetails(String jsonLeft, String jsonRight) {
      super();
      this.jsonLeft = jsonLeft;
      this.jsonRight = jsonRight;
      this.sumLen = len(jsonLeft) + len(jsonRight);
    }


    private static int len(String json) {
      if (json == null) {
        return 0;
      }
      return json.length();
    }


    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (jsonLeft == null || jsonRight == null) {
        sb.append("only ");
      } else {
        sb.append("\n ");
      }
      if (jsonLeft != null) {
        sb.append("left: ").append(jsonLeft);
      }
      if (jsonRight != null) {
        if (jsonLeft != null) {
          sb.append("\n");
        }
        sb.append("right: ").append(jsonRight);
      }
      return sb.toString();
    }


    public static JSONCorrelationDetails of(Object o) {
      return new JSONCorrelationDetails(o == null ? null : JSONValue.toJSONString(o), null);
    }


    public static JSONCorrelationDetails of(Object o1, Object o2) {
      return new JSONCorrelationDetails(o1 == null ? null : JSONValue.toJSONString(o1), o2 == null ? null : JSONValue.toJSONString(o2));
    }
  }


  public static List<String> createJsonPathListOfAllChanges(String json1, String json2) {
    Object o1 = JSONValue.parse(json1);
    Object o2 = JSONValue.parse(json2);
    Map<String, JSONCorrelationDetails> changes = new HashMap<>();
    addJSONValueChanges(changes, null, "$", o1, o2, false);
    return changes.entrySet().stream().sorted((e, f) -> e.getKey().compareTo(f.getKey())).map(e -> {
      return "<" + e.getKey() + "> is " + e.getValue(); 
    })
        .collect(Collectors.toList());
  }


  private static void addJSONValueChanges(Map<String, JSONCorrelationDetails> changes, Map<String, JSONCorrelationDetails> same,
                                          String basePath, Object o1, Object o2, boolean lrswitch) {
    if (logger.isTraceEnabled()) {
      logger.trace("comparing val (" + basePath + "):");
      logger.trace(" left: " + JSONValue.toJSONString(o1));
      logger.trace("right: " + JSONValue.toJSONString(o2));
    }
    if (o1 == null && o2 == null) {
      addSame(same, basePath, o1);
      return;
    }
    if (o1 != null && o1.equals(o2)) {
      addSame(same, basePath, o1);
      return;
    }
    if (o1 instanceof JSONObject && o2 instanceof JSONObject) {
      addJSONObjectChanges(changes, same, basePath, (JSONObject) o1, (JSONObject) o2, lrswitch);
    } else if (o1 instanceof JSONArray && o2 instanceof JSONArray) {
      addJSONArrayChanges(changes, same, basePath, (JSONArray) o1, (JSONArray) o2, lrswitch);
    } else {
      addChange(changes, basePath, o1, o2, lrswitch);
    }
  }


  private static void addChange(Map<String, JSONCorrelationDetails> changes, String path, Object o1, Object o2, boolean lrswitch) {
    if (lrswitch) {
      Object tmp = o1;
      o1 = o2;
      o2 = tmp;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("found change: " + path + ":");
      logger.trace(" left: " + JSONValue.toJSONString(o1));
      logger.trace("right: " + JSONValue.toJSONString(o2));
    }

    changes.put(path, JSONCorrelationDetails.of(o1, o2));
  }


  private static void addSubMapEntries(Map<String, JSONCorrelationDetails> changes, Map<String, JSONCorrelationDetails> subChanges,
                                       String basePath) {
    if (changes == null) {
      return;
    }
    for (Entry<String, JSONCorrelationDetails> e : subChanges.entrySet()) {
      changes.put(basePath + e.getKey(), e.getValue());
    }
  }


  private static void addSame(Map<String, JSONCorrelationDetails> same, String path, Object o) {
    if (same == null) {
      return;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("found same: " + path + ": " + JSONValue.toJSONString(o));
    }
    same.put(path, JSONCorrelationDetails.of(o));
  }


  private static void addJSONObjectChanges(Map<String, JSONCorrelationDetails> changes, Map<String, JSONCorrelationDetails> same,
                                           String basePath, JSONObject o1, JSONObject o2, boolean lrswitch) {
    if (logger.isTraceEnabled()) {
      logger.trace("comparing obj (" + basePath + "):");
      logger.trace(" left: " + JSONValue.toJSONString(o1));
      logger.trace("right: " + JSONValue.toJSONString(o2));
    }
    if (o1.equals(o2)) {
      addSame(same, basePath, o1);
      return;
    }
    Set<String> keys1 = o1.keySet();
    Set<String> keys2 = o2.keySet();
    Set<String> intersection = new HashSet<>(keys1);
    intersection.retainAll(keys2);
    Set<String> remaining1 = new HashSet<>(keys1);
    remaining1.removeAll(intersection);
    Set<String> remaining2 = new HashSet<>(keys2);
    remaining2.removeAll(intersection);
    for (String s : remaining1) {
      addChange(changes, basePath + "." + s, o1.get(s), null, lrswitch);
    }
    for (String s : remaining2) {
      addChange(changes, basePath + "." + s, null, o2.get(s), lrswitch);
    }
    for (String s : intersection) {
      addJSONValueChanges(changes, same, basePath + "." + s, o1.get(s), o2.get(s), lrswitch);
    }
  }


  /*
   * json array element ordering is typically of importance, but in our case we 
   * ignore that and treat it as a set. because the other whitelist functions also
   * don't care about changes in order.
   */
  private static void addJSONArrayChanges(Map<String, JSONCorrelationDetails> changes, Map<String, JSONCorrelationDetails> same,
                                          String basePath, JSONArray a1, JSONArray a2, boolean lrswitch) {
    if (logger.isTraceEnabled()) {
      logger.trace("comparing arr (" + basePath + "):");
      logger.trace(" left: " + JSONValue.toJSONString(a1));
      logger.trace("right: " + JSONValue.toJSONString(a2));
    }
    if (a1.equals(a2)) {
      addSame(same, basePath, a1);
      return;
    }
    Set<Object> intersection = new HashSet<>(a1);
    intersection.retainAll(a2);
    //intersection is "equal" and can be ignored
    if (intersection.size() == a1.size() && a1.size() == a2.size()) {
      addSame(same, basePath, a1);
      return;
    }
    for (Object o : intersection) {
      addSame(same, basePath + buildQualifier(a1, o), o);
    }
    Set<Object> remaining1 = new HashSet<>(a1);
    remaining1.removeAll(intersection);
    Set<Object> remaining2 = new HashSet<>(a2);
    remaining2.removeAll(intersection);
    if (remaining1.size() > remaining2.size()) {
      Set<Object> tmp = remaining1;
      remaining1 = remaining2;
      remaining2 = tmp;
      JSONArray tmpa = a1;
      a1 = a2;
      a2 = tmpa;
      lrswitch = !lrswitch;
    }
    Map<Object, Map<Object, Map<String, JSONCorrelationDetails>[]>> changeMap = new HashMap<>();
    outer : for (Object o : remaining1) {
      /*
       * now the question is, is there another object in remaining2 that is mostly the same?
       * it could even be fully the same, because in nested list only the ordering of elements could be different.
       * 
       * => search for the minimum of changes below each candidate
       */
      Map<Object, Map<String, JSONCorrelationDetails>[]> m = new HashMap<>();
      for (Object o2 : remaining2) {
        Map<String, JSONCorrelationDetails> change1 = new HashMap<>();
        Map<String, JSONCorrelationDetails> same1 = new HashMap<>();
        addJSONValueChanges(change1, same1, "", o, o2, lrswitch);
        if (change1.size() == 0) {
          //found a identical (up to ordering) object
          remaining2.remove(o2);
          addSame(same, basePath + buildQualifier(a1, o), o);
          continue outer;
        }
        m.put(o2, new Map[] {change1, same1});
      }
      changeMap.put(o, m);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("looking for best matches...");
    }
    while (changeMap.size() > 0) {
      Object[] similar = getMostSimilar(changeMap, remaining2);
      remaining2.remove(similar[1]);
      Map<String, JSONCorrelationDetails>[] similarChanges = changeMap.remove(similar[0]).get(similar[1]);
      //now we need to add a suitable qualifier to the path that identifies both elements uniquely.
      String path = basePath + buildQualifier(a1, similar[0]);
      addSubMapEntries(changes, similarChanges[0], path);
      addSubMapEntries(same, similarChanges[1], path);
    }

    for (Object o : remaining2) {
      addChange(changes, basePath + buildQualifier(a2, o), null, o, lrswitch);
    }
  }


  private static String buildQualifier(JSONArray a, Object o) {
    return "[" + a.indexOf(o) + "]";
  }


  private static Object[] getMostSimilar(Map<Object, Map<Object, Map<String, JSONCorrelationDetails>[]>> changeMap,
                                         Set<Object> remaining2) {
    /*
     * what "IS" most similar?
     * => highest ratio between same and change (measured in length of json-subsnippet of both)
     * but two changes of length 1 should be a bigger change than one change of length 2
     * so lets do something like this:
     * changeVal = (countChangePaths*3-2) * sumChangeLength
     * sameVal = (countSamePaths*3-2) * sumSameLength
     */
    double maxRatio = 0;
    Object[] similar = new Object[2];
    for (Entry<Object, Map<Object, Map<String, JSONCorrelationDetails>[]>> e : changeMap.entrySet()) {
      Map<Object, Map<String, JSONCorrelationDetails>[]> rm = e.getValue();
      for (Entry<Object, Map<String, JSONCorrelationDetails>[]> re : rm.entrySet()) {
        if (!remaining2.contains(re.getKey())) {
          //already identified as part of another pair
          continue;
        }
        int countChangePaths = re.getValue()[0].size();
        int countSamePaths = re.getValue()[1].size();
        int sumChangeLength = re.getValue()[0].values().stream().mapToInt(i -> i.sumLen).sum();
        int sumSameLength = re.getValue()[1].values().stream().mapToInt(i -> i.sumLen).sum();
        int changeVal = (countChangePaths * 3 - 2) * sumChangeLength;
        int sameVal = (countSamePaths * 3 - 2) * sumSameLength;
        double ratio = 1.0 * sameVal / changeVal;
        if (ratio > maxRatio) {
          maxRatio = ratio;
          similar[0] = e.getKey();
          similar[1] = re.getKey();
        }
      }
    }
    if (similar[0] == null) {
      throw new RuntimeException(); //should not happen, because the keyset of the first map is smaller then the nested one 
    }
    if (logger.isTraceEnabled()) {
      logger.trace("next best match (" + maxRatio + "): ");
      logger.trace(" left: " + JSONValue.toJSONString(similar[0]));
      logger.trace("right: " + JSONValue.toJSONString(similar[1]));
    }
    return similar;
  }


  public static boolean isJSONTheSameExceptPaths(String json1, String json2, List<String> paths) {
    for (String p : paths) {
      json1 = removePathFromJSON(json1, p);
      json2 = removePathFromJSON(json2, p);
      Object o1 = JSONValue.parse(json1);
      Object o2 = JSONValue.parse(json2);
      if (o1.equals(o2)) {
        return true;
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("jsons not equal after removing paths=" + paths.toString());
      logger.debug("json1=" + canonicalizeJSON(json1));
      logger.debug("json2=" + canonicalizeJSON(json2));
    }
    return false;
  }


  public static String canonicalizeJSON(String json) {
    Object o = JSONValue.parse(json);
    //JSONObject, JSONArray, String, java.lang.Number, java.lang.Boolean, null
    if (o instanceof JSONObject) {
      JSONObject obj = (JSONObject) o;
      StringBuilder sb = new StringBuilder();
      writeJSONObject(sb, obj);
      return sb.toString();
    } else {
      throw new RuntimeException("unsupported type: " + o.getClass());
    }
  }


  private static void writeJSONObject(StringBuilder sb, JSONObject obj) {
    sb.append("{");
    obj.entrySet().stream().sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey())).forEach(e -> {
      sb.append("\"").append(JSONValue.escape(e.getKey())).append("\": ");
      writeJSONValue(sb, e.getValue());
      sb.append(", ");
    });
    sb.setLength(sb.length() - 2);
    sb.append("}");
  }


  private static void writeJSONValue(StringBuilder sb, Object val) {
    //JSONObject, JSONArray, String, java.lang.Number, java.lang.Boolean, null
    if (val instanceof JSONObject) {
      writeJSONObject(sb, (JSONObject) val);
    } else if (val instanceof JSONArray) {
      writeJSONArray(sb, (JSONArray) val);
    } else {
      try {
        JSONValue.writeJSONString(val, sb);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private static void writeJSONArray(StringBuilder sb, JSONArray arr) {
    sb.append("[");
    if (arr.size() > 0) {
      arr.forEach(e -> {
        writeJSONValue(sb, e);
        sb.append(", ");
      });
      //remove trailing ", "
      sb.setLength(sb.length() - 2);
    }
    sb.append("]");
  }
}

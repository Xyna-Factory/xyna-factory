/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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


  public static String canonicalizeOrderInsensitiveArrays(String json, Set<String> paths) {
    if (paths == null || paths.isEmpty()) {
      return json;
    }
    DocumentContext dc = JsonPath.using(Configuration.defaultConfiguration()).parse(json);
    for (String path : paths.stream().sorted(Comparator.comparingInt(String::length).reversed()).collect(Collectors.toList())) {
      JsonPath jsonPath = JsonPath.compile(path);
      try {
        Object selected = dc.read(jsonPath);
        if (jsonPath.isDefinite()) {
          sortArrayAsOrderInsensitive(selected, path);
        } else if (selected instanceof JSONArray) {
          for (Object match : (JSONArray) selected) {
            sortArrayAsOrderInsensitive(match, path);
          }
        } else {
          throw new IllegalArgumentException("JSONPath for array-as-set does not select arrays: " + path);
        }
      } catch (PathNotFoundException e) {
        // The configured array may be absent from this JSON.
      }
    }
    return JSONValue.toJSONString(dc.read("$"));
  }


  private static void sortArrayAsOrderInsensitive(Object selected, String path) {
    if (!(selected instanceof JSONArray)) {
      throw new IllegalArgumentException("JSONPath for array-as-set does not select an array: " + path);
    }
    JSONArray array = (JSONArray) selected;
    List<Object> sorted = new ArrayList<>(array);
    sorted.sort(Comparator.comparing(WhiteListUtils::canonicalizeJSONValue));
    array.clear();
    array.addAll(sorted);
  }


  private static String canonicalizeJSONValue(Object value) {
    StringBuilder sb = new StringBuilder();
    writeJSONValue(sb, value);
    return sb.toString();
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


  public static String retainOnlyPaths(String json, List<String> paths) {
    if (json == null || json.isEmpty()) {
      return json;
    }
    if (paths == null || paths.isEmpty()) {
      return json;
    }
    Object root = JSONValue.parse(json);
    List<Object> selectedValues = new ArrayList<>();
    for (String p : paths) {
      if (p == null || p.isBlank()) {
        continue;
      }
      try {
        Object selected = JsonPath.compile(p).read(root);
        addSelectedValues(selectedValues, selected);
      } catch (PathNotFoundException e) {
        // ignore non-existing matching paths
      }
    }
    Object retained = retainMatchingNodes(root, selectedValues);
    return retained == null ? "{}" : JSONValue.toJSONString(retained);
  }


  private static void addSelectedValues(List<Object> selectedValues, Object selected) {
    if (selected == null) {
      return;
    }
    if (selected instanceof JSONArray) {
      for (Object o : (JSONArray) selected) {
        selectedValues.add(o);
      }
    } else {
      selectedValues.add(selected);
    }
  }


  private static Object retainMatchingNodes(Object node, List<Object> selectedValues) {
    if (node == null) {
      return null;
    }
    if (selectedValues.stream().anyMatch(v -> v == node)) {
      return node;
    }
    if (node instanceof JSONObject) {
      JSONObject result = new JSONObject();
      for (Entry<String, Object> e : ((JSONObject) node).entrySet()) {
        Object kept = retainMatchingNodes(e.getValue(), selectedValues);
        if (kept != null) {
          result.put(e.getKey(), kept);
        }
      }
      return result.isEmpty() ? null : result;
    }
    if (node instanceof JSONArray) {
      JSONArray result = new JSONArray();
      for (Object o : (JSONArray) node) {
        Object kept = retainMatchingNodes(o, selectedValues);
        if (kept != null) {
          result.add(kept);
        }
      }
      return result.isEmpty() ? null : result;
    }
    return null;
  }


  private static String removePathAndPruneEmptyParents(String json, String pathToRemove) {
    return removePathFromJSON(json, pathToRemove);
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
    public final String customMessage;


    public JSONCorrelationDetails(String jsonLeft, String jsonRight) {
      super();
      this.jsonLeft = jsonLeft;
      this.jsonRight = jsonRight;
      this.sumLen = len(jsonLeft) + len(jsonRight);
      this.customMessage = null;
    }


    private JSONCorrelationDetails(String customMessage) {
      this.jsonLeft = null;
      this.jsonRight = null;
      this.sumLen = 0;
      this.customMessage = customMessage;
    }


    private static int len(String json) {
      if (json == null) {
        return 0;
      }
      return json.length();
    }


    public String toString() {
      if (customMessage != null) {
        return customMessage;
      }
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


    public static JSONCorrelationDetails moved(Object value, int fromIndex, int toIndex) {
      return new JSONCorrelationDetails(JSONValue.toJSONString(value) + " is at a different place in the array (index " + fromIndex
          + " -> " + toIndex + ")");
    }
  }


  public static List<String> createJsonPathListOfAllChanges(String json1, String json2) {
    return createJsonPathListOfAllChanges(json1, json2, Collections.emptySet());
  }


  public static List<String> createJsonPathListOfAllChanges(String json1, String json2, Set<String> orderInsensitiveArrayPaths) {
    json1 = canonicalizeOrderInsensitiveArrays(json1, orderInsensitiveArrayPaths);
    json2 = canonicalizeOrderInsensitiveArrays(json2, orderInsensitiveArrayPaths);
    Object o1 = JSONValue.parse(json1);
    Object o2 = JSONValue.parse(json2);
    Map<String, JSONCorrelationDetails> changes = new HashMap<>();
    addJSONValueChanges(changes, null, "$", o1, o2, false);
    return changes.entrySet().stream().sorted((e, f) -> e.getKey().compareTo(f.getKey())).map(e -> {
      return "<" + e.getKey() + ">" + (e.getValue().customMessage == null ? " is " + e.getValue() : " " + e.getValue());
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
    int[][] lcs = new int[a1.size() + 1][a2.size() + 1];
    for (int i = a1.size() - 1; i >= 0; i--) {
      for (int j = a2.size() - 1; j >= 0; j--) {
        if (Objects.equals(a1.get(i), a2.get(j))) {
          lcs[i][j] = lcs[i + 1][j + 1] + 1;
        } else {
          lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
        }
      }
    }

    boolean[] matchedLeft = new boolean[a1.size()];
    boolean[] matchedRight = new boolean[a2.size()];
    int[] matchedRightIndexForLeft = new int[a1.size()];
    Arrays.fill(matchedRightIndexForLeft, -1);
    int leftIndex = 0;
    int rightIndex = 0;
    while (leftIndex < a1.size() && rightIndex < a2.size()) {
      if (Objects.equals(a1.get(leftIndex), a2.get(rightIndex))) {
        matchedLeft[leftIndex] = true;
        matchedRight[rightIndex] = true;
        matchedRightIndexForLeft[leftIndex] = rightIndex;
        leftIndex++;
        rightIndex++;
      } else if (lcs[leftIndex][rightIndex + 1] > lcs[leftIndex + 1][rightIndex]
          || (lcs[leftIndex][rightIndex + 1] == lcs[leftIndex + 1][rightIndex]
              && a2.size() - rightIndex >= a1.size() - leftIndex)) {
        rightIndex++;
      } else {
        leftIndex++;
      }
    }

    boolean[] movedLeft = new boolean[a1.size()];
    boolean[] movedRight = new boolean[a2.size()];
    for (int i = 0; i < a1.size(); i++) {
      if (matchedLeft[i]) {
        continue;
      }
      int closestMatch = -1;
      int closestDistance = Integer.MAX_VALUE;
      for (int j = 0; j < a2.size(); j++) {
        if (!matchedRight[j] && !movedRight[j] && i != j && Objects.equals(a1.get(i), a2.get(j))) {
          int distance = Math.abs(i - j);
          if (distance < closestDistance) {
            closestMatch = j;
            closestDistance = distance;
          }
        }
      }
      if (closestMatch >= 0) {
        movedLeft[i] = true;
        movedRight[closestMatch] = true;
        String path = basePath + "[" + i + "] (moved)";
        changes.put(path, JSONCorrelationDetails.moved(a1.get(i), i, closestMatch));
      }
    }

    int leftSegmentStart = 0;
    int rightSegmentStart = 0;
    for (int i = 0; i < a1.size(); i++) {
      if (matchedLeft[i]) {
        int j = matchedRightIndexForLeft[i];
        addJSONArraySegmentChanges(changes, same, basePath, a1, a2, leftSegmentStart, i, rightSegmentStart, j, movedLeft,
                                   movedRight, lrswitch);
        leftSegmentStart = i + 1;
        rightSegmentStart = j + 1;
      }
    }
    addJSONArraySegmentChanges(changes, same, basePath, a1, a2, leftSegmentStart, a1.size(), rightSegmentStart, a2.size(), movedLeft,
                               movedRight, lrswitch);
  }


  private static void addJSONArraySegmentChanges(Map<String, JSONCorrelationDetails> changes, Map<String, JSONCorrelationDetails> same,
                                                 String basePath, JSONArray a1, JSONArray a2, int leftStart, int leftEnd, int rightStart,
                                                 int rightEnd, boolean[] movedLeft, boolean[] movedRight, boolean lrswitch) {
    List<Integer> leftIndices = new ArrayList<>();
    List<Integer> rightIndices = new ArrayList<>();
    for (int i = leftStart; i < leftEnd; i++) {
      if (!movedLeft[i]) {
        leftIndices.add(i);
      }
    }
    for (int i = rightStart; i < rightEnd; i++) {
      if (!movedRight[i]) {
        rightIndices.add(i);
      }
    }
    int commonLength = Math.min(leftIndices.size(), rightIndices.size());
    for (int i = 0; i < commonLength; i++) {
      int left = leftIndices.get(i);
      int right = rightIndices.get(i);
      addJSONValueChanges(changes, same, basePath + "[" + right + "]", a1.get(left), a2.get(right), lrswitch);
    }
    for (int i = commonLength; i < leftIndices.size(); i++) {
      int left = leftIndices.get(i);
      addArrayOnlyChange(changes, basePath, left, a1.get(left), true, lrswitch);
    }
    for (int i = commonLength; i < rightIndices.size(); i++) {
      int right = rightIndices.get(i);
      addArrayOnlyChange(changes, basePath, right, a2.get(right), false, lrswitch);
    }
  }


  private static void addArrayOnlyChange(Map<String, JSONCorrelationDetails> changes, String basePath, int index, Object value,
                                         boolean leftOnly, boolean lrswitch) {
    String path = basePath + "[" + index + "]";
    if (changes.containsKey(path)) {
      path += leftOnly ? " (left)" : " (right)";
    }
    addChange(changes, path, leftOnly ? value : null, leftOnly ? null : value, lrswitch);
  }


  public static boolean isJSONTheSameExceptPaths(String json1, String json2, List<String> paths) {
    for (String p : paths) {
      Object before1 = JSONValue.parse(json1);
      Object before2 = JSONValue.parse(json2);
      Object after1 = JSONValue.parse(removePathAndPruneEmptyParents(json1, p));
      Object after2 = JSONValue.parse(removePathAndPruneEmptyParents(json2, p));
      pruneEmptyArrayParents(before1, before2, after1, after2);
      json1 = JSONValue.toJSONString(after1);
      json2 = JSONValue.toJSONString(after2);
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


  private static void pruneEmptyArrayParents(Object before1, Object before2, Object after1, Object after2) {
    if (!(after1 instanceof JSONObject) || !(after2 instanceof JSONObject)) {
      return;
    }
    JSONObject beforeObject1 = before1 instanceof JSONObject ? (JSONObject) before1 : new JSONObject();
    JSONObject beforeObject2 = before2 instanceof JSONObject ? (JSONObject) before2 : new JSONObject();
    JSONObject afterObject1 = (JSONObject) after1;
    JSONObject afterObject2 = (JSONObject) after2;
    Set<String> keys = new HashSet<>(afterObject1.keySet());
    keys.addAll(afterObject2.keySet());
    for (String key : keys) {
      Object beforeValue1 = beforeObject1.get(key);
      Object beforeValue2 = beforeObject2.get(key);
      Object afterValue1 = afterObject1.get(key);
      Object afterValue2 = afterObject2.get(key);
      pruneEmptyArrayParents(beforeValue1, beforeValue2, afterValue1, afterValue2);
        boolean wasNonEmptyArray = (beforeValue1 instanceof JSONArray && !((JSONArray) beforeValue1).isEmpty())
          || (beforeValue2 instanceof JSONArray && !((JSONArray) beforeValue2).isEmpty());
        if (wasNonEmptyArray && afterValue1 instanceof JSONArray && ((JSONArray) afterValue1).isEmpty()
          && afterValue2 instanceof JSONArray && ((JSONArray) afterValue2).isEmpty()) {
        afterObject1.remove(key);
        afterObject2.remove(key);
      } else if (beforeValue1 instanceof JSONArray && !((JSONArray) beforeValue1).isEmpty()
          && afterValue1 instanceof JSONArray && ((JSONArray) afterValue1).isEmpty()
          && !(afterValue2 instanceof JSONArray)) {
        afterObject1.remove(key);
      } else if (beforeValue2 instanceof JSONArray && !((JSONArray) beforeValue2).isEmpty()
          && afterValue2 instanceof JSONArray && ((JSONArray) afterValue2).isEmpty()
          && !(afterValue1 instanceof JSONArray)) {
        afterObject2.remove(key);
      }
    }
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
    boolean first = true;
    for (Entry<String, Object> e : obj.entrySet().stream().sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
        .collect(Collectors.toList())) {
      if (!first) {
        sb.append(", ");
      }
      sb.append("\"").append(JSONValue.escape(e.getKey())).append("\": ");
      writeJSONValue(sb, e.getValue());
      first = false;
    }
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

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
package com.gip.xyna.xprc.xpce.parameterinheritance.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;

import junit.framework.TestCase;


public class ChildFilterMatchingTest extends TestCase {
  
  /* 
   * Test.Hierarchy
   * test.Root -> test.Leaf
   *           -> test.short.hierarchy.Outer -> test.short.hierarchy.Inner -> test.Leaf
   *           -> test.long.hierarchy.Outer -> test.long.hierarchy.Middle -> test.long.hierarchy.Inner -> test.Leaf
   *                                                                      -> test.short.hierarchy.Outer -> test.short.hierarchy.Inner -> test.Leaf
   *                                                                      -> test.we:rd.Test -> test.Leaf
   *                                                                      -> test.we\rd.Test -> test.Leaf
   *                                                                      -> test.we*rd.Test -> test.Leaf
   *                                                                      -> test.weird.Test: -> test.Leaf
   */
  
  private final static String ROOT = "";
  private final static String LEAF = "test.Leaf";
  private final static String SHORT_OUTER = "test.short.hierarchy.Outer";
  private final static String SHORT_INNER = "test.short.hierarchy.Inner";
  private final static String LONG_OUTER = "test.long.hierarchy.Outer";
  private final static String LONG_MIDDLE = "test.long.hierarchy.Middle";
  private final static String LONG_INNER = "test.long.hierarchy.Inner";
  private final static String LONG_WEIRD_COLON = "test.we:rd.Test";
  private final static String LONG_WEIRD_BACKSLASH = "test.we\\rd.Test";
  private final static String LONG_WEIRD_STAR = "test.we*rd.Test";
  private final static String LONG_WEIRD_TRAILING_COLON = "test.weird.Test:";
  
  private final static String H_ROOT = buildHierarchy(ROOT);
  private final static String H_LEAF = buildHierarchy(LEAF);
  private final static String H_SHORT_OUTER = buildHierarchy(SHORT_OUTER);
  private final static String H_SHORT_INNER = buildHierarchy(SHORT_OUTER, SHORT_INNER);
  private final static String H_SHORT_LEAF = buildHierarchy(SHORT_OUTER, SHORT_INNER, LEAF);
  private final static String H_LONG_OUTER = buildHierarchy(LONG_OUTER);
  private final static String H_LONG_MIDDLE = buildHierarchy(LONG_OUTER, LONG_MIDDLE);
  private final static String H_LONG_INNER = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_INNER);
  private final static String H_LONG_LEAF = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_INNER, LEAF);
  private final static String H_LONG_SHORT_OUTER = buildHierarchy(LONG_OUTER, LONG_MIDDLE, SHORT_OUTER);
  private final static String H_LONG_SHORT_INNER = buildHierarchy(LONG_OUTER, LONG_MIDDLE, SHORT_OUTER, SHORT_INNER);
  private final static String H_LONG_SHORT_LEAF = buildHierarchy(LONG_OUTER, LONG_MIDDLE, SHORT_OUTER, SHORT_INNER, LEAF);
  private final static String H_LONG_WEIRD_COLON = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_COLON);
  private final static String H_LONG_WEIRD_COLON_LEAF = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_COLON, LEAF);
  private final static String H_LONG_WEIRD_BACKSLASH = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_BACKSLASH);
  private final static String H_LONG_WEIRD_BACKSLASH_LEAF = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_BACKSLASH, LEAF);
  private final static String H_LONG_WEIRD_STAR = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_STAR);
  private final static String H_LONG_WEIRD_STAR_LEAF = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_STAR, LEAF);
  private final static String H_LONG_WEIRD_TRAILING_COLON = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_TRAILING_COLON);
  private final static String H_LONG_WEIRD_TRAILING_COLON_LEAF = buildHierarchy(LONG_OUTER, LONG_MIDDLE, LONG_WEIRD_TRAILING_COLON, LEAF);
  
  
  private final static String[] CHILD_ORDERTYPE_HIERACHIES = 
                {H_ROOT,H_LEAF,H_SHORT_OUTER,H_SHORT_INNER,H_SHORT_LEAF,H_LONG_OUTER,H_LONG_MIDDLE,H_LONG_INNER,H_LONG_LEAF,H_LONG_SHORT_OUTER,H_LONG_SHORT_INNER,H_LONG_SHORT_LEAF,
                 H_LONG_WEIRD_COLON,H_LONG_WEIRD_COLON_LEAF,H_LONG_WEIRD_BACKSLASH,H_LONG_WEIRD_BACKSLASH_LEAF,H_LONG_WEIRD_STAR,H_LONG_WEIRD_STAR_LEAF, H_LONG_WEIRD_TRAILING_COLON, H_LONG_WEIRD_TRAILING_COLON_LEAF};
  
  
  @Test
  public void testEmptyFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter("").build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, true,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false));
  }
  
  @Test
  public void testAllFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter("*").build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true));
  }
  
  @Test
  public void testLeafFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter("*:" + LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,true,false,false,true,false,false,false,true,false,false,true,false,true,false,true,false,true,false, true));
  }
  
  /*
   * OrderType A definiert als Filter B:C
   * Regel gilt f�r: A -> B -> C
   * Regel gilt z.B. nicht f�r: A oder B -> C oder A -> D -> B -> C
   */
  
  @Test
  public void testSimpleHierarchyFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_SHORT_INNER).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,true,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_SHORT_LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,true,false,false,false,false,false,false,false,false));
  }
  
  /*
   * OrderType A definiert als Filter B:C\:D
   * Regel gilt f�r: A -> B -> C:D
   * Regel gilt z.B. nicht f�r: A -> B -> C -> D
   */
  
  @Test
  public void testEscapedControlCharFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_COLON).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false,false,false,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_COLON_LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false,false,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_BACKSLASH).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_BACKSLASH_LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_STAR).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_STAR_LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_TRAILING_COLON).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false));
    rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(H_LONG_WEIRD_TRAILING_COLON_LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true));
  }
  
  /*
   * OrderType A definiert als Filter B:*:C
   * Regel gilt z.B. f�r: A -> B -> C und A -> B -> D -> E  -> C
   * Regel gilt z.B. nicht f�r: A -> B -> C -> D
   */
  
  @Test
  public void testWildcardHierarchyTypesFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(LONG_OUTER + ":*:" + LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,true,false,false,true,
                                                        false,true,false,true,false,true,false,true));
    // TODO more?
  }
  
  /*
   * OrderType A definiert als Filter B:*:C:*
   * Regel gilt z.B. f�r: A -> B -> C und A -> B -> D -> E  -> C und A -> B -> C -> D
   * Regel gilt z.B. nicht f�r: A -> D -> B -> C
   */
  
  @Test
  public void testMultipleWildcardHierarchyTypesFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter(LONG_OUTER + ":*:" + SHORT_OUTER + ":*").build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,false,false,false,false,false,true,true,true,
                                                        false,false,false,false,false,false,false,false));
    // TODO more?
  }
  
  /*
   * OrderType A definiert als Filter *:B:C
   * Regel gilt z.B. f�r: A -> B -> C und A -> D -> B -> C
   * Regel gilt z.B. nicht f�r: A -> D
   */
  
  @Test
  public void testLeadingWildcardHierarchyTypesFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter("*:" + "test.short.hierarchy.Inner:" + LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,true,false,false,false,false,false,false,true,
                                                        false,false,false,false,false,false,false,false));
  }
  
  @Test
  public void testWildcardOrdertypeFilter() throws XPRC_INVALID_MONITORING_TYPE {
    InheritanceRule rule = InheritanceRule.createMonitoringLevelRule("0").precedence(0).childFilter("test.*.hierarchy.Outer:*:" + LEAF).build();
    assertRule(rule, toExpectationMap(CHILD_ORDERTYPE_HIERACHIES, false,false,false,false,true,false,false,false,true,false,false,true,
                                                        false,true,false,true,false,true,false,true));
  }

  private static void assertRule(InheritanceRule rule, Map<String, Boolean> expected) {
    for (Entry<String, Boolean> expectation : expected.entrySet()) {
      assertEquals(expectation.getKey() + " match did not evaluate to " + expectation.getValue(), expectation.getValue(), (Boolean)rule.matches(expectation.getKey()));
    }
  }
  
  
  private static Map<String, Boolean> toExpectationMap(String[] childOrdertypes, boolean... matches) {
    assertEquals(childOrdertypes.length, matches.length);
    Map<String, Boolean> expectationMap = new HashMap<String, Boolean>();
    for (int i = 0; i < childOrdertypes.length; i++) {
      expectationMap.put(childOrdertypes[i], matches[i]);
    }
    return expectationMap;
  }
  
  
  private static String buildHierarchy(String... ordertype) {
    String hierarchy = "";
    for (int i = ordertype.length - 1; i >= 0; i--) {
      hierarchy = ParameterInheritanceManagement.prependOrderTypeToHierarchy(ordertype[i], hierarchy);
    }
    return hierarchy;
  }

}

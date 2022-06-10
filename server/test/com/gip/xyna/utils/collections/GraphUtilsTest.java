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
package com.gip.xyna.utils.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.collections.GraphUtils.ConnectedEdges;

import junit.framework.TestCase;

public class GraphUtilsTest extends TestCase   {

  public void testSimple() {
    Set<String> s = GraphUtils.collectConnectedNodes(new ConnectedEdges<String>() {

      @Override
      public Collection<String> getConnectedEdges(String t) {
        if (t.equals("a")) {
          return Arrays.asList("b", "c");
        } else if (t.equals("b")) {
          return Arrays.asList("b", "d");
        } else if (t.equals("c")) {
          return Collections.emptyList();
        } else if (t.equals("d")) {
          return Arrays.asList("a", "e");
        } else if (t.equals("e")) {
          return Collections.emptyList();
        } else {
          throw new RuntimeException();
        }
      }
    }, "a", true);
    assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c", "d", "e")), s);
  }
  
  public void testFindCycle() {
    Set<String> s = GraphUtils.collectConnectedNodes(new ConnectedEdges<String>() {

      @Override
      public Collection<String> getConnectedEdges(String t) {
        if (t.equals("a")) {
          return Arrays.asList("b", "c");
        } else if (t.equals("b")) {
          return Arrays.asList("b", "d");
        } else if (t.equals("c")) {
          return Collections.emptyList();
        } else if (t.equals("d")) {
          return Arrays.asList("a", "e");
        } else if (t.equals("e")) {
          return Collections.emptyList();
        } else {
          throw new RuntimeException();
        }
      }
    }, "a", false);
    assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c", "d", "e")), s);
  }
  
  public void testDoNotIncludeStart() {
    Set<String> s = GraphUtils.collectConnectedNodes(new ConnectedEdges<String>() {

      @Override
      public Collection<String> getConnectedEdges(String t) {
        if (t.equals("a")) {
          return Arrays.asList("b", "c");
        } else if (t.equals("b")) {
          return Arrays.asList("b", "d");
        } else if (t.equals("c")) {
          return Collections.emptyList();
        } else if (t.equals("d")) {
          return Arrays.asList("e");
        } else if (t.equals("e")) {
          return Collections.emptyList();
        } else {
          throw new RuntimeException();
        }
      }
    }, "a", false);
    assertEquals(new HashSet<String>(Arrays.asList("b", "c", "d", "e")), s);
  }
  
  public void testNullEdges() {
    Set<String> s = GraphUtils.collectConnectedNodes(new ConnectedEdges<String>() {

      @Override
      public Collection<String> getConnectedEdges(String t) {
        if (t.equals("a")) {
          return Arrays.asList("b", "c");
        } else if (t.equals("b")) {
          return Arrays.asList("b", "d");
        } else if (t.equals("c")) {
          return Collections.emptyList();
        } else if (t.equals("d")) {
          return Arrays.asList("e");
        } else if (t.equals("e")) {
          return null;
        } else {
          throw new RuntimeException();
        }
      }
    }, "a", false);
    assertEquals(new HashSet<String>(Arrays.asList("b", "c", "d", "e")), s);
  }
  
}

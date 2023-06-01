/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.utils.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gip.xyna.utils.misc.CycleUtils.CycleController;

import junit.framework.TestCase;




public class CycleUtilsTest extends TestCase {

  static class Node {
    String name;
    Set<Node> children = new HashSet<Node>();
    Node(String name) {
      this.name = name;
    }
    void add(Node n) {
      children.add(n);
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name).append(": ");
      for (Node child : children) {
        sb.append(child.name).append(" - ");
      }
      return sb.toString();
    }
  }
  
  static class NodeHelper implements CycleController<Node, List<Node>> {

    public Set<Node> getBranchingElements(Node element) {
      return element.children;
    }

    public void addToCycle(List<Node> cycleRepresentation, Node element) {
      cycleRepresentation.add(element);
    }

    public List<Node> newCycle() {
      return new ArrayList<Node>();
    }
    
  }
  
  static NodeHelper nuh = new NodeHelper();
  
  
  @Test
  public void testNoCycle() {
    /* a
     * |---| 
     * b   c
     * |   |
     * d   e
     */
    Node a = new Node("a");
    Node b = new Node("b");
    Node c = new Node("c");
    Node d = new Node("d");
    Node e = new Node("e");
    
    a.add(b);
    a.add(c);
    b.add(d);
    c.add(e);
    
    Collection<List<Node>> cycles = CycleUtils.collectCycles(a, nuh);
    
    final int CYCLE_SIZE = 0;
    assertEquals(CYCLE_SIZE, cycles.size());
  }
  
  
  @Test
  public void testShortCycle() {
    /* a
     * v----v 
     * b    c
     * v    v
     * d_<>_e
     */
    Node a = new Node("a");
    Node b = new Node("b");
    Node c = new Node("c");
    Node d = new Node("d");
    Node e = new Node("e");
    
    a.add(b);
    a.add(c);
    b.add(d);
    c.add(e);
    d.add(e);
    e.add(d);
    
    Collection<List<Node>> cycles = CycleUtils.collectCycles(a, nuh);
    
    final int CYCLE_SIZE = 2;
    final int CYCLE_LENGTH = 2;
    assertEquals(CYCLE_SIZE, cycles.size());
    boolean found_de_cycle = false;
    boolean found_ed_cycle = false;
    for (List<Node> nodes : cycles) {
      assertEquals(CYCLE_LENGTH, nodes.size());
      if (nodes.get(0).name.equals(d.name) &&
          nodes.get(1).name.equals(e.name)) {
        found_de_cycle = true;
      }
      if (nodes.get(0).name.equals(e.name) &&
          nodes.get(1).name.equals(d.name)) {
        found_ed_cycle = true;
      }
    }
    assertEquals(true, found_de_cycle);
    assertEquals(true, found_ed_cycle);
  }
  
  
  @Test
  public void testSimpleCycle() {
    /* a
     * v_<_^ 
     * b   e
     * v   ^
     * c_>_d
     */
    
    Node a = new Node("a");
    Node b = new Node("b");
    Node c = new Node("c");
    Node d = new Node("d");
    Node e = new Node("e");
    
    a.add(b);
    b.add(c);
    c.add(d);
    d.add(e);
    e.add(b);
    
    Collection<List<Node>> cycles = CycleUtils.collectCycles(a, nuh);
    
    final int CYCLE_SIZE = 1;
    final int CYCLE_LENGTH = 4;
    assertEquals(CYCLE_SIZE, cycles.size());
    Set<String> cycleElementNames = new HashSet<String>();
    for (List<Node> nodes : cycles) {
      assertEquals(CYCLE_LENGTH, nodes.size());
      for (Node cycleElement : nodes) {
        cycleElementNames.add(cycleElement.name);
      }
    }
    assertEquals(false, cycleElementNames.contains(a.name));
    assertEquals(true, cycleElementNames.contains(b.name));
    assertEquals(true, cycleElementNames.contains(c.name));
    assertEquals(true, cycleElementNames.contains(d.name));
    assertEquals(true, cycleElementNames.contains(e.name));
  }
  
  @Test
  public void testNestedCylce() {
    /* a_<_d
     * v   ^
     * b_>_c
     * V
     * e_<_h
     * v   ^
     * f_>_g
     */
    Node a = new Node("a");
    Node b = new Node("b");
    Node c = new Node("c");
    Node d = new Node("d");
    Node e = new Node("e");
    Node f = new Node("f");
    Node g = new Node("g");
    Node h = new Node("h");

    a.add(b);
    b.add(c);
    b.add(e);
    c.add(d);
    d.add(a);
    e.add(f);
    f.add(g);
    g.add(h);
    h.add(e);

    Collection<List<Node>> cycles = CycleUtils.collectCycles(a, nuh);

    final int CYCLE_SIZE = 2;
    final int CYCLE_LENGTH = 4;
    assertEquals(CYCLE_SIZE, cycles.size());
    Set<String> cycleElementNames = new HashSet<String>();
    for (List<Node> nodes : cycles) {
      assertEquals(CYCLE_LENGTH, nodes.size());
      for (Node cycleElement : nodes) {
        cycleElementNames.add(cycleElement.name);
      }
      assertEquals(true, (cycleElementNames.contains(a.name) &&
                          cycleElementNames.contains(b.name) &&
                          cycleElementNames.contains(c.name) &&
                          cycleElementNames.contains(d.name) &&
                          !cycleElementNames.contains(e.name) &&
                          !cycleElementNames.contains(f.name) &&
                          !cycleElementNames.contains(g.name) &&
                          !cycleElementNames.contains(h.name))  ^ (!cycleElementNames.contains(a.name) &&
                                                                   !cycleElementNames.contains(b.name) &&
                                                                   !cycleElementNames.contains(c.name) &&
                                                                   !cycleElementNames.contains(d.name) &&
                                                                   cycleElementNames.contains(e.name) &&
                                                                   cycleElementNames.contains(f.name) &&
                                                                   cycleElementNames.contains(g.name) &&
                                                                   cycleElementNames.contains(h.name)));
      cycleElementNames.clear();
    }
  }
  
  
  @Test
  public void testOverlappingCylces() {
    /* a_<_d
     * v / ^
     * b_>_c
     */
    Node a = new Node("a");
    Node b = new Node("b");
    Node c = new Node("c");
    Node d = new Node("d");
    
    a.add(b);
    b.add(c);
    c.add(d);
    d.add(a);
    b.add(d);
    
    Collection<List<Node>> cycles = CycleUtils.collectCycles(a, nuh);
    
    final int CYCLE_SIZE = 2;
    assertEquals(CYCLE_SIZE, cycles.size());
    Set<String> cycleElementNames = new HashSet<String>();
    boolean foundShortCycle = false;
    boolean foundLongCycle = false;
    for (List<Node> nodes : cycles) {
      if (nodes.size() == 3) {
        for (Node cycleElement : nodes) {
          cycleElementNames.add(cycleElement.name);
        }
        assertEquals(true, (cycleElementNames.contains(a.name)));
        assertEquals(true, (cycleElementNames.contains(b.name)));
        assertEquals(false, (cycleElementNames.contains(c.name)));
        assertEquals(true, (cycleElementNames.contains(d.name)));
        cycleElementNames.clear();
        foundShortCycle = true;
      }
      if (nodes.size() == 4) {
        for (Node cycleElement : nodes) {
          cycleElementNames.add(cycleElement.name);
        }
        assertEquals(true, (cycleElementNames.contains(a.name)));
        assertEquals(true, (cycleElementNames.contains(b.name)));
        assertEquals(true, (cycleElementNames.contains(c.name)));
        assertEquals(true, (cycleElementNames.contains(d.name)));
        cycleElementNames.clear();
        foundLongCycle = true;
      }
    }
    assertEquals(true, foundShortCycle);
    assertEquals(true, foundLongCycle);
  }  
  
}

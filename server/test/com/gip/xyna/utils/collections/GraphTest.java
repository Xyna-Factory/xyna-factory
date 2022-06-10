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



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.Graph.Node;
import com.gip.xyna.utils.collections.Graph.HasUniqueStringIdentifier;



public class GraphTest extends TestCase {

  private static class X implements HasUniqueStringIdentifier {

    private final String id;


    private X(String id) {
      this.id = id;
    }


    public String getId() {
      return id;
    }


    public String toString() {
      return id;
    }

  }


  public void testDeps() {
    Node<X> dn1 = new Node<X>(new X("1"));
    Node<X> dn2 = new Node<X>(new X("2"));
    Node<X> dn3 = new Node<X>(new X("3"));
    Node<X> dn4 = new Node<X>(new X("4"));
    Node<X> dn5 = new Node<X>(new X("5"));
    Node<X> dn6 = new Node<X>(new X("6"));
    Node<X> dn7 = new Node<X>(new X("7"));
    Node<X> dn8 = new Node<X>(new X("8"));
    Node<X> dn9 = new Node<X>(new X("9"));
    Node<X> dn10 = new Node<X>(new X("10"));
    Node<X> dn11 = new Node<X>(new X("11"));
    Node<X> dn12 = new Node<X>(new X("12"));

    dn1.addDependency(dn2);
    dn2.addDependency(dn4);
    dn3.addDependency(dn1);
    dn4.addDependency(dn3);
    dn4.addDependency(dn5);
    dn5.addDependency(dn6);
    dn6.addDependency(dn7);
    dn7.addDependency(dn6);
    dn7.addDependency(dn8);
    dn8.addDependency(dn9);
    dn9.addDependency(dn10);
    dn9.addDependency(dn11);
    dn9.addDependency(dn12);
    dn10.addDependency(dn8);

    assertEquals(12, dn1.getDependenciesRecursively().keySet().size());
    assertEquals(12, dn2.getDependenciesRecursively().keySet().size());
    assertEquals(12, dn3.getDependenciesRecursively().keySet().size());
    assertEquals(12, dn4.getDependenciesRecursively().keySet().size());
    assertEquals(8, dn5.getDependenciesRecursively().keySet().size());
    assertEquals(7, dn6.getDependenciesRecursively().keySet().size());
    assertEquals(7, dn7.getDependenciesRecursively().keySet().size());
    assertEquals(5, dn8.getDependenciesRecursively().keySet().size());
    assertEquals(5, dn9.getDependenciesRecursively().keySet().size());
    assertEquals(5, dn10.getDependenciesRecursively().keySet().size());
    assertEquals(1, dn11.getDependenciesRecursively().keySet().size());
    assertEquals(1, dn12.getDependenciesRecursively().keySet().size());
  }


  public void testBug1() {
    Node<X> dn1 = new Node<X>(new X("1"));
    Node<X> dn2 = new Node<X>(new X("2"));
    Node<X> dn3 = new Node<X>(new X("3"));

    dn1.addDependency(dn3);
    dn1.addDependency(dn2);
    dn1.addDependency(dn1);
    dn2.addDependency(dn2);
    dn2.addDependency(dn3);
    dn3.addDependency(dn1);
    dn3.addDependency(dn2);
    dn3.addDependency(dn3);

    dn1.getDependenciesRecursively();
    dn2.getDependenciesRecursively();
  }


  public void testBug2() {
    /*
     *deps for 3
    calculated deps: [3, 2, 1]
    real deps: [3, 2, 1, 0]
    1 -> {2, 1, 0 }
    2 -> {2, 1 }
    0 -> {3, 2, 1, 0 }
    3 -> {3, 2, 1 }
     */
    Node<X> dn0 = new Node<X>(new X("0"));
    Node<X> dn1 = new Node<X>(new X("1"));
    Node<X> dn2 = new Node<X>(new X("2"));
    Node<X> dn3 = new Node<X>(new X("3"));

    dn0.addDependency(dn0);
    dn0.addDependency(dn1);
    dn0.addDependency(dn2);
    dn0.addDependency(dn3);
    dn1.addDependency(dn0);
    dn1.addDependency(dn1);
    dn1.addDependency(dn2);
    dn2.addDependency(dn1);
    dn2.addDependency(dn2);
    dn3.addDependency(dn1);
    dn3.addDependency(dn2);
    dn3.addDependency(dn3);

    dn1.getDependenciesRecursively();
    dn2.getDependenciesRecursively();
    dn0.getDependenciesRecursively();
    assertTrue(testDependencies(dn3));
  }


  public void testBug3() {
    /*
     * deps for 1
    calculated deps: [1]
    real deps: [3, 2, 1, 0]
    0 -> {3, 1, 0 }
    1 -> {2, 1 }
    2 -> {3, 2 }
    3 -> {0 }
     */
    Node<X> dn0 = new Node<X>(new X("0"));
    Node<X> dn1 = new Node<X>(new X("1"));
    Node<X> dn2 = new Node<X>(new X("2"));
    Node<X> dn3 = new Node<X>(new X("3"));

    dn0.addDependency(dn0);
    dn0.addDependency(dn1);
    dn0.addDependency(dn3);
    dn1.addDependency(dn1);
    dn1.addDependency(dn2);
    dn2.addDependency(dn2);
    dn2.addDependency(dn3);
    dn3.addDependency(dn0);

    dn0.getDependenciesRecursively();
    assertTrue(testDependencies(dn1));
  }


  public void testBug4() {
    /*
     * deps for 0
    calculated deps: [3, 2, 1, 0]
    real deps: [3, 2, 1, 0, 4]
    1 -> {2, 1, 0, 4 }
    4 -> {3, 2, 1, 0 }
    2 -> {2, 1 }
    0 -> {3, 2, 0 }
    3 -> {3, 0 }
     */

    Node<X> dn0 = new Node<X>(new X("0"));
    Node<X> dn1 = new Node<X>(new X("1"));
    Node<X> dn2 = new Node<X>(new X("2"));
    Node<X> dn3 = new Node<X>(new X("3"));
    Node<X> dn4 = new Node<X>(new X("4"));

    dn0.addDependency(dn0);
    dn0.addDependency(dn2);
    dn0.addDependency(dn3);

    dn1.addDependency(dn0);
    dn1.addDependency(dn1);
    dn1.addDependency(dn2);
    dn1.addDependency(dn4);

    dn2.addDependency(dn1);
    dn2.addDependency(dn2);

    dn3.addDependency(dn0);
    dn3.addDependency(dn3);

    dn4.addDependency(dn0);
    dn4.addDependency(dn1);
    dn4.addDependency(dn2);
    dn4.addDependency(dn3);

    dn1.getDependenciesRecursively();
    assertTrue(testDependencies(dn0));
  }


  public void testRandomDeps() {
    boolean debug = false;
    int cnt = 100;
    int nNodes = 25;
    for (int i = 0; i < cnt; i++) {
      List<Node<X>> nodes = createRandomNodes(nNodes);
      Collections.shuffle(nodes);
      for (Node<X> n : nodes) {
        if (!testDependencies(n)) {
          printNodes(nodes);
          assertTrue(false);
        }
      }

      Graph<X> g = new Graph<X>(nodes);
      List<Node<X>> roots = g.getRoots();
      Set<String> allNodes = new HashSet<String>();
      for (Node<X> r : roots) {
        allNodes.add(r.getContent().getId());
        for (String dep : r.getDependenciesRecursively().keySet()) {
          allNodes.add(dep);
        }
      }
      if (nNodes != allNodes.size()) {
        System.out.println("----");
        printNodes(nodes);
        System.out.println("roots:");
        for (Node<X> root : roots) {
          System.out.println(root.getContent().getId());
        }
        g.getRoots();
      }
      assertEquals(nNodes, allNodes.size());
      if (debug && roots.size() > 3) {
        System.out.println("----");
        printNodes(nodes);
        System.out.println("roots:");
        for (Node<X> root : roots) {
          System.out.println(root.getContent().getId() + " -> " + new TreeSet<String>(root.getDependenciesRecursively().keySet()));
        }
      }
    }
  }


  public void testPerformance() {
    long time = 0;
    long timeOld = 0;
    long timeRoot = 0;
    long sum = 0;
    int num = 200;
    for (int i = 0; i < 100; i++) {
      List<Node<X>> nodes = createRandomNodes(num);
      Collections.shuffle(nodes);
      time -= System.nanoTime();
      for (Node<X> n : nodes) {
        sum += n.getDependenciesRecursively().size();
      }
      time += System.nanoTime();

      timeRoot -= System.nanoTime();
      Graph<X> g = new Graph<X>(nodes);
      g.getRoots();
      timeRoot += System.nanoTime();

      nodes = createRandomNodes(num);
      Collections.shuffle(nodes);
      timeOld -= System.nanoTime();
      for (Node<X> n : nodes) {
        Set<String> deps2 = new HashSet<String>();
        getDepsRecursively(n, deps2);
        sum += deps2.size();
      }
      timeOld += System.nanoTime();
      if (i % 1 == 0) {
        System.out.println(i + ".) time = " + (time / 1000) + "micsec, timeOld = " + (timeOld / 1000) + "micsec, roots=" + (timeRoot / 1000) + "micsec.");
      }
    }
    System.out.println(sum);
  }


  private void printNodes(List<Node<X>> nodes) {
    for (Node<X> d : nodes) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (Node<X> dep : d.getDependencies()) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(dep.getContent().id);
      }
      System.out.println(d.getContent().id + " -> {" + sb.toString() + " }");
    }
  }


  private boolean testDependencies(Node<X> n) {
    Set<String> deps;
    try {
      deps = new HashSet<String>(n.getDependenciesRecursively().keySet());
    } catch (RuntimeException e) {
      System.out.println("deps for " + n.getContent().id + " -> exception");
      e.printStackTrace();
      return false;
    }
    Set<String> deps2 = new HashSet<String>();
    getDepsRecursively(n, deps2);
    if (!deps2.equals(deps)) {
      System.out.println("deps for " + n.getContent().id);
      System.out.println("calculated deps: " + deps);
      System.out.println("real deps: " + deps2);
      return false;
    }
    return true;
  }


  private void getDepsRecursively(Node<X> n, Set<String> depsAlreadyAdded) {
    if (!depsAlreadyAdded.add(n.getContent().toString())) {
      return;
    }
    for (Node<X> child : n.getDependencies()) {
      getDepsRecursively(child, depsAlreadyAdded);
    }
  }


  private List<Node<X>> createRandomNodes(int n) {
    List<Node<X>> l = new ArrayList<Graph.Node<X>>(n);
    for (int i = 0; i < n; i++) {
      l.add(new Node<GraphTest.X>(new X("" + i)));
    }
    //deps setzen
    Random r = new Random();
    int type = r.nextInt(3);
    for (int i = 0; i < n; i++) {
      int nDeps;
      switch (type) {
        case 0 :
          nDeps = (int) Math.round(Math.sqrt(r.nextInt(100)));
          break;
        case 1 :
          nDeps = (int) Math.round(Math.sqrt(Math.sqrt(r.nextInt(100))));
          break;
        case 2 :
          if (r.nextBoolean()) {
            nDeps = (int) Math.round(Math.sqrt(r.nextInt(100)));
          } else {
            nDeps = (int) Math.round(Math.sqrt(Math.sqrt(r.nextInt(100))));
          }
          break;
        default :
          throw new RuntimeException();
      }
      for (int j = 0; j < nDeps; j++) {
        l.get(i).addDependency(l.get(r.nextInt(n)));
      }
    }
    return l;
  }


}

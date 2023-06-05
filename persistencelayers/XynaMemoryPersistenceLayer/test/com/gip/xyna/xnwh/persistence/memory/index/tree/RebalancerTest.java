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
package com.gip.xyna.xnwh.persistence.memory.index.tree;

import java.util.HashSet;
import java.util.Random;

import com.gip.xyna.xnwh.persistence.memory.index.tree.AbstractNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.Rebalancer;
import com.gip.xyna.xnwh.persistence.memory.index.tree.SimpleIndexNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.SimpleRootNode;

import junit.framework.TestCase;


public class RebalancerTest extends TestCase {
  
  private static Random random = new Random();
  
  private static int cnt;
  private static int getRandomNumber() {
    return cnt++;
    //return random.nextInt(1000);
  }
  
  public void test1() {
    //237
    for (int j = 1; j < 1; j++) {
      int size = random.nextInt(300);
      System.out.println("size = " + size);
      for (int i = 0; i<1; i++) {
        System.out.println("i = " + i);
        cnt = 0;
        a(size); 
      }
    }
  }
  
  public void a(int numberOfNodes) {
    int branchfactor = 2;
    SimpleRootNode<Integer> root = new SimpleRootNode<Integer>(branchfactor);
    SimpleIndexNode<Integer> c1 = new SimpleIndexNode<Integer>(root, getRandomNumber());    
    root.addChild(c1);
    assertEquals(root, c1.getParent());
    assertEquals(1, c1.getMaxSubTreeSize());
    assertEquals(1+branchfactor, root.getMaxSubTreeSize());

    SimpleIndexNode<Integer> c2 = null;
    for (int i = 0; i<numberOfNodes; i++) {
      c2 = new SimpleIndexNode<Integer>(root, getRandomNumber());    
      root.addChild(c2);
    }
    
    Rebalancer rebalancer = new Rebalancer();
    rebalancer.rebalanceNode(root, false);
    
  /*  StringBuilder sb = new StringBuilder();
    printTree(root, sb, 0);
    System.out.println("tree: " + sb);
    */
    c2 = root;
    while (true) {
      if (c2.getNumberOfChildren() == 0) {
        break;
      }
      c2 = (SimpleIndexNode<Integer>) c2.getChild(random.nextInt(c2.getNumberOfChildren()));
    }
   // System.out.println(" addNewNodesTo = " + c2.getParent());
    for (int i = 0; i<2*numberOfNodes; i++) {
      c2.getParent().addChild(new SimpleIndexNode<Integer>(root, getRandomNumber()));
    }
    rebalancer.rebalanceNode(c2.getParent(), true);
    
  /*  sb = new StringBuilder();
    printTree(root, sb, 0);
    System.out.println("tree2: " + sb);*/
  }

  public static void printTree(AbstractNode<?> node, StringBuilder sb, int indent) {
    sb.append(node);
    if (node.getNumberOfChildren() > 0) {
      sb.append(":");
      sb.append("\n");
      for (int i = 0; i<indent; i++) {
        sb.append("  ");
      }
      sb.append(node.getNumberOfChildren()).append("c: {");
      for (int i = 0; i<node.getNumberOfChildren(); i++) {
        if (i > 0) {
          sb.append(",");
        }
        printTree((SimpleIndexNode<?>)node.getChild(i), sb, indent+1);
      }
      sb.append("}\n");      
      for (int i = 0; i<indent; i++) {
        sb.append("  ");
      }
    }
  }
  
  public void test2() {
    SimpleRootNode<Integer> root = new SimpleRootNode<Integer>(7);
    for (int i = 0; i<10; i++) {
      System.out.println(i + ": " + root.getSubTreeSize());
      int numberOfNewNodes = random.nextInt(100)+1;
      int numberOfNodesToDelete = random.nextInt(numberOfNewNodes);
      System.out.println("add " + numberOfNewNodes + " remove " + numberOfNodesToDelete);
      HashSet<SimpleIndexNode<Integer>> toBalance = new HashSet<SimpleIndexNode<Integer>>();
      for (int j = 0; j<numberOfNodesToDelete; j++) {
        if (root.getSubTreeSize() == 1) {
          break;
        }
        SimpleIndexNode<Integer> toDelete = root;
        while (toDelete == root || toDelete.getNumberOfChildren() > 0) {
          toDelete = (SimpleIndexNode<Integer>)toDelete.getChild(random.nextInt(toDelete.getNumberOfChildren()));
        }
        for (int k = 0; k<toDelete.getParent().getNumberOfChildren(); k++) {
          if (toDelete.getParent().getChild(k) == toDelete) {
            toBalance.add((SimpleIndexNode<Integer>) toDelete.getParent());
            SimpleIndexNode<Integer> removed = (SimpleIndexNode<Integer>) toDelete.getParent().removeSubTree(k);
            toBalance.remove(removed);
            break;
          }
        }
      }
      
      for (int j = 0; j<numberOfNewNodes; j++) {
        //zufälligen knoten suchen
        SimpleIndexNode<Integer> randomNode = root;
        if (root.getSubTreeSize() > 1) {
          for (int k = 0; k<random.nextInt(100); k++) {
            if ((random.nextBoolean() && randomNode.getNumberOfChildren() > 0) || randomNode == root) {
              randomNode = (SimpleIndexNode<Integer>)randomNode.getChild(random.nextInt(randomNode.getNumberOfChildren()));
            } else {
              randomNode =(SimpleIndexNode<Integer>) randomNode.getParent();
            }         
          }
          if (randomNode.getNumberOfChildren() == 0) {
            randomNode = (SimpleIndexNode<Integer>)randomNode.getParent();
          }
        }
        
        
        randomNode.addChild(new SimpleIndexNode<Integer>(root, getRandomNumber()));
        toBalance.add(randomNode);
      }
      
      Rebalancer rebalancer = new Rebalancer();
      for (SimpleIndexNode<Integer> n : toBalance) {        
        rebalancer.rebalanceNode(n, true);
      }
      System.out.println("nodes changed : " + rebalancer.cnt);
    }
  }
  
  public void test3() {
    SimpleRootNode<Integer> root = new SimpleRootNode<Integer>(5);
    Rebalancer rebalancer = new Rebalancer();
    for (int k = 0; k<5; k++) {
      long t1 = System.currentTimeMillis();
      int nodes = 50;
      for (int i = 0; i<nodes; i++) {
        root.addChild(new SimpleIndexNode<Integer>(root, getRandomNumber()));
        if (random.nextInt(50) < 10) {
          rebalancer.rebalanceNode(root, false);
        }
      }
      rebalancer.rebalanceNode(root, false);
      double hz = (1000.0*nodes/(System.currentTimeMillis()+1-t1));
    //  StringBuilder sb = new StringBuilder();
    //  printTree(root, sb, 0);
    //  System.out.println(sb);
      System.out.println(hz + " Hz " + rebalancer.cnt + " " + root.getSubTreeSize());
    }
  }

}

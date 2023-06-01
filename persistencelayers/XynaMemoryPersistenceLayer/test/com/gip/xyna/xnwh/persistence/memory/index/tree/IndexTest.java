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
package com.gip.xyna.xnwh.persistence.memory.index.tree;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import junit.framework.TestCase;

import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdate;
import com.gip.xyna.xnwh.persistence.memory.index.Condition;
import com.gip.xyna.xnwh.persistence.memory.index.ConditionType;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.index.ResultHandler;
import com.gip.xyna.xnwh.persistence.memory.index.map.IndexImplMap;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.IndexNodeValue;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.NodeCreator;



public class IndexTest extends TestCase {

  private static class RowData {

    private String col1;
    private int col2;
    private String col3;


    public RowData(String col1, int col2, String col3) {
      this.col1 = col1;
      this.col2 = col2;
      this.col3 = col3;
    }


    public String getCol1() {
      return col1;
    }


    public void setCol1(String col1) {
      this.col1 = col1;
    }


    public int getCol2() {
      return col2;
    }


    public void setCol2(int col2) {
      this.col2 = col2;
    }


    public String getCol3() {
      return col3;
    }


    public void setCol3(String col3) {
      this.col3 = col3;
    }
    
    @Override
    public String toString() {
      return "RowData: " + col1 + '|' + col2 + '|' + col3;
    }

  }

  private static class CollectingResultHandler implements ResultHandler<RowData> {

    private List<RowData> data = new ArrayList<RowData>();
    private int maxrows;
    private int cnt = 0;
    
    public CollectingResultHandler(int maxrows) {
      this.maxrows = maxrows;
    }

    public List<RowData> getDatas() {
      return data;
    }


    public boolean handle(List<RowData> values) {
      cnt += values.size();
      data.addAll(values);
      if (cnt >= maxrows) {
        return false;
      }
      return true;
    }

  }

  public static class BiggerCondition implements Condition<String> {

    private String value;


    public BiggerCondition(String value) {
      this.value = value;
    }


    public String getLookupValue() {
      return value;
    }


    public ConditionType getType() {
      return ConditionType.BIGGER;
    }

  }

  public static class SmallerCondition implements Condition<String> {

    private String value;


    public SmallerCondition(String value) {
      this.value = value;
    }


    public String getLookupValue() {
      return value;
    }


    public ConditionType getType() {
      return ConditionType.SMALLER;
    }
  }

  public static class BiggerEqualsCondition implements Condition<String> {

    private String value;


    public BiggerEqualsCondition(String value) {
      this.value = value;
    }


    public String getLookupValue() {
      return value;
    }


    public ConditionType getType() {
      return ConditionType.BIGGER_OR_EQUAL;
    }

  }

  public static class SmallerEqualCondition implements Condition<String> {

    private String value;


    public SmallerEqualCondition(String value) {
      this.value = value;
    }


    public String getLookupValue() {
      return value;
    }


    public ConditionType getType() {
      return ConditionType.SMALLER_OR_EQUAL;
    }
  }

  public static class EqualsCondition implements Condition<String> {

    private String value;
    int cnt;


    public EqualsCondition(String value) {
      this.value = value;
    }


    public String getLookupValue() {
      cnt++;
      return value;
    }


    public ConditionType getType() {
      return ConditionType.EQUALS;
    }

  }

  private static class SlowLock implements ReadWriteLock {

    private String name;


    public SlowLock(String name) {
      this.name = name;
    }


    private class SlowInnerLock implements Lock {

      private Lock innerLock;


      public SlowInnerLock(Lock lock) {
        innerLock = lock;
      }


      public void lock() {
        // System.out.println("locking " + this);
        /*   try {
             Thread.sleep(1);
           } catch (InterruptedException e) {
           }*/
        innerLock.lock();
      }


      public void lockInterruptibly() throws InterruptedException {
        innerLock.lockInterruptibly();
      }


      public java.util.concurrent.locks.Condition newCondition() {
        return innerLock.newCondition();
      }


      public boolean tryLock() {
        return innerLock.tryLock();
      }


      public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return innerLock.tryLock(time, unit);
      }


      public void unlock() {
        //  System.out.println("unlocking " + this);
        innerLock.unlock();
      }


      public String toString() {
        return name + "-" + innerLock.toString();
      }

    }


    private ReentrantReadWriteLock innerLock = new ReentrantReadWriteLock();
    private Lock readLock = new SlowInnerLock(innerLock.readLock());
    private Lock writeLock = new SlowInnerLock(innerLock.writeLock());


    public Lock readLock() {
      return readLock;
    }


    public Lock writeLock() {
      return writeLock;
    }


    public String toString() {
      return name + "-" + innerLock.toString();
    }

  }

  private static class NodeCreatorTest implements NodeCreator<String, RowData> {

    private int n = 3;


    public AbstractNode<IndexNodeValue<String, RowData>> createNode(int depth,
                                                                    AbstractNode<IndexNodeValue<String, RowData>> root,
                                                                    IndexNodeValue<String, RowData> value,
                                                                    IndexNodeValue<String, RowData> parentValue) {
      return createNode(depth, root, value, (List<AbstractNode<IndexNodeValue<String, RowData>>>) null);
    }


    public AbstractNode<IndexNodeValue<String, RowData>> createNode(
                                                                    int depth,
                                                                    AbstractNode<IndexNodeValue<String, RowData>> root,
                                                                    IndexNodeValue<String, RowData> value,
                                                                    List<AbstractNode<IndexNodeValue<String, RowData>>> children) {
      if (depth == 0) {
        //root
        return new LockedRootNode<IndexNodeValue<String, RowData>>(2, new SlowLock(String.valueOf(value)));
      } else if (depth % n == 0) {
        return new LockedSubTree<IndexNodeValue<String, RowData>>((Root) root, value, new SlowLock(String
            .valueOf(value)));
      } else {
        return new LockedSubTreeNode<IndexNodeValue<String, RowData>>(
                                                                      (Root) root,
                                                                      value,
                                                                      ((LockedSubTree<IndexNodeValue<String, RowData>>) root)
                                                                          .getLock());
      }
      /*  
      } else if (depth < n - 1) {
       
      } else if (depth == n - 1) {

      } else if (depth % n == 0) {

      } else if (depth % n < n - 1) {

      } else {

      }*/
    }


    public AbstractNode<IndexNodeValue<String, RowData>> transformNode(
                                                                       AbstractNode<IndexNodeValue<String, RowData>> source) {
      AbstractNode<IndexNodeValue<String, RowData>> newNode =
          createNode(source.getDepth(), (AbstractNode<IndexNodeValue<String, RowData>>) source.getRoot(), source
              .getValue(), source.getParent().getValue());
      newNode.copyFields(source);
      for (AbstractNode<IndexNodeValue<String, RowData>> child : newNode.getChildren()) {
        child.setParent(newNode);
      }
      return newNode;
    }


    public boolean validateNodeType(AbstractNode<IndexNodeValue<String, RowData>> node) {
      if (node.getDepth() == 0) {
        if (node instanceof LockedRootNode) {
          return true;
        }
      } else if (node.getDepth() % n == 0) {
        if (node instanceof LockedSubTree) {
          return true;
        }
      } else {
        if (node instanceof LockedSubTreeNode) {
          return true;
        }
      }
      return false;
    }

  }


  private Random random = new Random();


  public void testAddRead1() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    RowData rd = new RowData("bla", 42, "dass");
    index.add(rd.getCol1(), rd);
    RowData rd2 = new RowData("blubb", 43, "ja");
    index.add(rd2.getCol1(), rd2);

    CollectingResultHandler handler = new CollectingResultHandler(5);
    Condition<String> condition = new EqualsCondition("bla");
    index.readOnly(handler, condition, false);
    List<RowData> rows = handler.getDatas();
    assertEquals(1, rows.size());
    assertEquals(rd, rows.get(0));

    handler = new CollectingResultHandler(5);
    condition = new EqualsCondition("blubb");
    index.readOnly(handler, condition, false);
    rows = handler.getDatas();
    assertEquals(1, rows.size());
    assertEquals(rd2, rows.get(0));

    handler = new CollectingResultHandler(5);
    condition = new EqualsCondition("blubbs");
    index.readOnly(handler, condition, false);
    rows = handler.getDatas();
    assertEquals(0, rows.size());
    System.out.println("-----------");
  }


  public void testAddRead2() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    RowData rd = new RowData("blubb", 42, "dass");
    index.add(rd.getCol1(), rd);
    RowData rd2 = new RowData("bla", 43, "ja");
    index.add(rd2.getCol1(), rd2);

    CollectingResultHandler handler = new CollectingResultHandler(5);
    Condition<String> condition = new EqualsCondition("bla");
    index.readOnly(handler, condition, false);
    List<RowData> rows = handler.getDatas();
    assertEquals(1, rows.size());
    assertEquals(rd2, rows.get(0));

    handler = new CollectingResultHandler(5);
    condition = new EqualsCondition("blubb");
    index.readOnly(handler, condition, false);
    rows = handler.getDatas();
    assertEquals(1, rows.size());
    assertEquals(rd, rows.get(0));

    handler = new CollectingResultHandler(5);
    condition = new EqualsCondition("blubbs");
    index.readOnly(handler, condition, false);
    rows = handler.getDatas();
    assertEquals(0, rows.size());
  }


  public void testAddRebalance() {
    int[][] orders = new int[][] { {4, 5, 3, 1, 2}, {6, 1, 7, 4, 2, 3, 5}, {6, 4, 1, 3, 2, 5}, {2, 7, 3, 5, 6, 4, 1}};
    for (int k = 0; k < orders.length; k++) {
      Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
      RowData[] rows = new RowData[orders[k].length];
      for (int i = 0; i < orders[k].length; i++) {
        rows[i] = new RowData("" + orders[k][i], 12, "");
        index.add(rows[i].getCol1(), rows[i]);
        index.rebalance();
      }

      StringBuilder sb = new StringBuilder();
      //Rebalancer.printTree(((IndexImplTree<String, RowData>) index).getRoot(), sb, 0);
      //System.out.println(sb);

      for (int i = 0; i < rows.length; i++) {
        CollectingResultHandler handler = new CollectingResultHandler(5);
        Condition<String> condition = new EqualsCondition(rows[i].getCol1());
        index.readOnly(handler, condition, false);
        List<RowData> rowsFound = handler.getDatas();
        assertEquals(1, rowsFound.size());
        assertEquals(rows[i], rowsFound.get(0));
      }
    }
  }


  public void testRemoveNodes() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    RowData[] rows = new RowData[100];
    for (int i = 0; i < rows.length; i++) {
      rows[i] = createRandomRowData();
      index.add(rows[i].getCol1(), rows[i]);
      index.rebalance();
    }
    int updateNumber = 100;
    for (int i = 0; i < updateNumber; i++) {
      for (int j = 0; j < rows.length; j++) {
        AtomicBulkUpdate<String, RowData> update = index.startBulkUpdate(null);
        String oldColVal = rows[j].col1;
        rows[j].col1 = createRandomString();
        update.update(oldColVal, rows[j].col1, rows[j]);
        update.commit();
        index.rebalance();
      }
    }
  }


  public void testBiggerAndSmaller() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    String[] vals = new String[] {"f", "x", "ax", "xa", "ax", "ax1", "xa", "xa1", "r"};
    for (String val : vals) {
      index.add(val, new RowData(val, 2, ""));
      index.rebalance();
    }

    CollectingResultHandler handler = new CollectingResultHandler(5);
    BiggerCondition condition = new BiggerCondition("x");
    index.readOnly(handler, condition, false);

    assertEquals(3, handler.getDatas().size());
    List<String> expectedKeys = new ArrayList<String>();
    expectedKeys.add("xa");
    expectedKeys.add("xa");
    expectedKeys.add("xa1");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertEquals(0, expectedKeys.size());

    handler = new CollectingResultHandler(6);
    SmallerCondition conditionSm = new SmallerCondition("x");
    index.readOnly(handler, conditionSm, false);
    assertEquals(5, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("f");
    expectedKeys.add("ax");
    expectedKeys.add("ax");
    expectedKeys.add("ax1");
    expectedKeys.add("r");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(3).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(4).col1));
    assertEquals(0, expectedKeys.size());

    handler = new CollectingResultHandler(6);
    SmallerEqualCondition conditionSmE = new SmallerEqualCondition("x");
    index.readOnly(handler, conditionSmE, false);
    assertEquals(6, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("f");
    expectedKeys.add("ax");
    expectedKeys.add("ax");
    expectedKeys.add("ax1");
    expectedKeys.add("x");
    expectedKeys.add("r");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(3).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(4).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(5).col1));
    assertEquals(0, expectedKeys.size());

    handler = new CollectingResultHandler(5);
    BiggerEqualsCondition conditionBigE = new BiggerEqualsCondition("x");
    index.readOnly(handler, conditionBigE, false);
    assertEquals(4, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("xa");
    expectedKeys.add("xa");
    expectedKeys.add("x");
    expectedKeys.add("xa1");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(3).col1));
    assertEquals(0, expectedKeys.size());

  }
  
  
  public void testBiggerAndSmallerReversed() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    String[] vals = new String[] {"f", "x", "ax", "xa", "ax", "ax1", "xa", "xa1", "r"};
    for (String val : vals) {
      index.add(val, new RowData(val, 2, ""));
      index.rebalance();
    }

    CollectingResultHandler handler = new CollectingResultHandler(5);
    BiggerCondition condition = new BiggerCondition("x");
    index.readOnly(handler, condition, true);

    assertEquals(3, handler.getDatas().size());
    List<String> expectedKeys = new ArrayList<String>();
    expectedKeys.add("xa1");
    expectedKeys.add("xa");
    expectedKeys.add("xa");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertEquals(0, expectedKeys.size());

    handler = new CollectingResultHandler(6);
    SmallerCondition conditionSm = new SmallerCondition("x");
    index.readOnly(handler, conditionSm, true);
    assertEquals(5, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("r");
    expectedKeys.add("ax1");
    expectedKeys.add("ax");
    expectedKeys.add("ax");
    expectedKeys.add("f");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(3).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(4).col1));
    assertEquals(0, expectedKeys.size());

    handler = new CollectingResultHandler(6);
    SmallerEqualCondition conditionSmE = new SmallerEqualCondition("x");
    index.readOnly(handler, conditionSmE, true);
    assertEquals(6, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("r");
    expectedKeys.add("x");
    expectedKeys.add("ax1");
    expectedKeys.add("ax");
    expectedKeys.add("ax");
    expectedKeys.add("f");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(3).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(4).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(5).col1));
    assertEquals(0, expectedKeys.size());

    handler = new CollectingResultHandler(5);
    BiggerEqualsCondition conditionBigE = new BiggerEqualsCondition("x");
    index.readOnly(handler, conditionBigE, true);
    assertEquals(4, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("xa1");
    expectedKeys.add("x");
    expectedKeys.add("xa");
    expectedKeys.add("xa");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(2).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(3).col1));
    assertEquals(0, expectedKeys.size());
    
    handler = new CollectingResultHandler(5);
    EqualsCondition conditionE = new EqualsCondition("xa");
    index.readOnly(handler, conditionE, true);
    assertEquals(2, handler.getDatas().size());
    expectedKeys = new ArrayList<String>();
    expectedKeys.add("xa");
    expectedKeys.add("xa");
    assertTrue(expectedKeys.remove(handler.getDatas().get(0).col1));
    assertTrue(expectedKeys.remove(handler.getDatas().get(1).col1));
    assertEquals(0, expectedKeys.size());
  }


  public void testPerformance() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    long t0 = System.nanoTime();
    long t3 = t0;
    for (int i = 0; i < 10; i++) {
      if (i % 10000 == 0) {
        long t1 = System.nanoTime();
        System.out.println(i + "took " + (t1 - t0) + "ns");
        t0 = t1;
      }
      RowData rd = createRandomRowData();
      index.add(rd.getCol1(), rd);
      index.rebalance();
    }
    System.out.println("gesamt " + (System.nanoTime() - t3) / 1e9 + "s");

    /*   StringBuilder sb = new StringBuilder();
       RebalancerTest.printTree(index.getRoot(), sb, 0);
       System.out.println(sb);*/

    RowData rd = createRandomRowData();
    t0 = System.nanoTime();
    index.add(rd.getCol1(), rd);
    System.out.println("adding took " + (System.nanoTime() - t0) + "ns");
    CollectingResultHandler handler = new CollectingResultHandler(1000);
    EqualsCondition condition = new EqualsCondition(rd.getCol1());
    t0 = System.nanoTime();
    index.readOnly(handler, condition, false);
    System.out.println("getting took " + (System.nanoTime() - t0) + "ns");
    System.out.println(condition.cnt);
    List<RowData> rows = handler.getDatas();
    assertTrue(rows.size() >= 1);
    assertTrue(rows.contains(rd));
  }


  private RowData createRandomRowData() {
    return new RowData(createRandomString(), random.nextInt(1000), createRandomString());
  }


  private String createRandomString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < random.nextInt(8) + 2; i++) {
      sb.append((char) ('a' + random.nextInt(26)));
    }
    return sb.toString();
  }


  public void testCompositeIndex() {
    //FIXME
  }


  public void testBulkOperation() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    RowData[] vals =
        new RowData[] {new RowData("f", 2, ""), new RowData("x", 2, ""), new RowData("ax", 2, ""),
            new RowData("xa", 2, ""), new RowData("ax", 2, ""), new RowData("ax1", 2, ""), new RowData("xa", 2, ""),
            new RowData("xa1", 2, ""), new RowData("r", 2, "")};
    for (RowData val : vals) {
      index.add(val.getCol1(), val);
      index.rebalance();
    }
    //FIXME handler statt null
    AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
    RowData newData = new RowData("h", 3, "");
    bulk.add(newData.getCol1(), newData);
    bulk.remove(vals[2].getCol1(), vals[2]);
    vals[4].col1 = "ax1";
    bulk.update("ax", vals[4].getCol1(), vals[4]);
    bulk.commit();

    CollectingResultHandler handler = new CollectingResultHandler(10);
    index.readOnly(handler, new EqualsCondition(vals[5].getCol1()), false);
    assertEquals(2, handler.getDatas().size());
    assertTrue(handler.getDatas().contains(vals[4]));
    assertTrue(handler.getDatas().contains(vals[5]));

    handler = new CollectingResultHandler(10);
    index.readOnly(handler, new EqualsCondition(newData.getCol1()), false);
    assertEquals(1, handler.getDatas().size());
    assertEquals(newData, handler.getDatas().get(0));

    handler = new CollectingResultHandler(10);
    index.readOnly(handler, new EqualsCondition(vals[2].getCol1()), false);
    assertEquals(0, handler.getDatas().size());

  }


  public void testBulkOperation2() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    for (int i = 0; i < 100; i++) {
      AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
      RowData rd = createRandomRowData();
      bulk.add(rd.getCol1(), rd);
      RowData rd2 = createRandomRowData();
      bulk.add(rd2.getCol1(), rd2);
      bulk.commit();
      index.rebalance();
    }

    index.rebalance();

    /*  StringBuilder sb = new StringBuilder();
      RebalancerTest.printTree(index.getRoot(), sb, 0);
      System.out.println("------------" + sb);*/
  }


  public void testBulkOperation3() {
    for (int k = 0; k < 1; k++) {
      for (int c = 1; c < 5; c++) {
        Rebalancer.cnt = 0;
        long t0 = System.currentTimeMillis();
        Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest(), c*c* 100);
       // Index<String, RowData> index = new IndexImplMap<String, RowData>(new ReentrantReadWriteLock());
        AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
        for (int i = 0; i < 1000; i++) {
          RowData rd = createRandomRowData();
          bulk.add(rd.getCol1(), rd);
          RowData rd2 = createRandomRowData();
          bulk.add(rd2.getCol1(), rd2);
        }
        bulk.commit();
        index.rebalance();
        bulk = index.startBulkUpdate(null);
        List<RowData> toDelete = new ArrayList<RowData>(100);
        for (int i = 0; i < 200; i++) {
          RowData rd = createRandomRowData();
          bulk.add(rd.getCol1(), rd);
          RowData rd2 = createRandomRowData();
          bulk.add(rd2.getCol1(), rd2);
          if (random.nextBoolean()) {
            toDelete.add(rd2);
          }
        }
        bulk.commit();
        index.rebalance();
        System.out.println("c = " + c + ": insert took " + (System.currentTimeMillis() - t0) + "ms");
        System.out.println("deleting " + toDelete.size() + " rows");
        System.out.println("rebalances: " + Rebalancer.cnt);

        //löschen
        bulk = index.startBulkUpdate(null);
        for (RowData rd : toDelete) {
          bulk.remove(rd.col1, rd);
        }
        bulk.commit();
        index.rebalance();
        System.out.println("c = " + c + ": delete took " + (System.currentTimeMillis() - t0) + "ms");
        System.out.println("rebalances: " + Rebalancer.cnt);
      }
    }
  }
/*
  public void testQueryRange() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
   // Index<String, RowData> index = new IndexImplMap<String, RowData>(new ReentrantReadWriteLock());
    String[] vals = new String[] {"f", "x", "ax", "xa", "ax", "ax1", "xa", "xa1", "r"};
    for (String val : vals) {
      index.add(val, new RowData(val, 2, ""));
      index.rebalance();
    }
    CollectingResultHandler handler = new CollectingResultHandler(5);
    index.readRange(handler, "ax1", "f");
    assertEquals(2, handler.getDatas().size());
    assertEquals("ax1", handler.getDatas().get(0).col1);
    assertEquals("f", handler.getDatas().get(1).col1);
    
    handler = new CollectingResultHandler(5);
    index.readRange(handler, "a", "ax");
    assertEquals(2, handler.getDatas().size());
    assertEquals("ax", handler.getDatas().get(0).col1);
    assertEquals("ax", handler.getDatas().get(1).col1);
    
    handler = new CollectingResultHandler(10);
    index.readRange(handler, "a", "z");
    assertEquals(9, handler.getDatas().size());

    handler = new CollectingResultHandler(5);
    index.readRange(handler, "v", "z");
    assertEquals(4, handler.getDatas().size());
    assertEquals("x", handler.getDatas().get(0).col1);
    assertEquals("xa", handler.getDatas().get(1).col1);
    assertEquals("xa", handler.getDatas().get(2).col1);
    assertEquals("xa1", handler.getDatas().get(3).col1);
  }*/

  public void testBulkDouble() {
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
    RowData rd = createRandomRowData();
    bulk.add(rd.getCol1(), rd);
    RowData rd2 = createRandomRowData();
    rd2.col1 = rd.col1;
    bulk.add(rd.getCol1(), rd2);
    RowData rd3 = createRandomRowData();
    rd3.col1 = rd.col1;
    bulk.add(rd.getCol1(), rd3);
    bulk.commit();
    index.rebalance();
  }

  public void testBuildUpdateAndDelete() { //nicht unterstützt! vglkommentar von bulkupdate.commit
    Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
    RowData rd = new RowData("a", 1, "c");
    index.add("a", rd);
    AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
    bulk.update("a", "b", rd);
    bulk.remove("b", rd);
    bulk.commit();
    CollectingResultHandler h = new CollectingResultHandler(10);
    index.readOnly(h, new EqualsCondition("a"), false);
    assertEquals(0, h.getDatas().size());
    h = new CollectingResultHandler(10);
    index.readOnly(h, new EqualsCondition("b"), false);
    assertEquals(0, h.getDatas().size());
  }

  public void atestLocking() throws InterruptedException {
    final Index<String, RowData> index = new IndexImplTree<String, RowData>(new NodeCreatorTest());
   // final Index<String, RowData> index = new IndexImplMap<String, RowData>(new ReentrantReadWriteLock());
    final AtomicLong cnt = new AtomicLong();
    int threadCnt = 4;
    final CountDownLatch latch = new CountDownLatch(threadCnt);
    final List<RowData> entries = new ArrayList<RowData>();
    for (int i = 0; i < threadCnt; i++) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            while (cnt.incrementAndGet() < 3000) {
              if (cnt.get() % 10000 == 0) {
                System.out.println(cnt.get() + " " + entries.size());
              }
              int r = random.nextInt(4);
              if (r == 0) {
                RowData rd = createRandomRowData();
                synchronized (entries) {
                  entries.add(rd);
                }
                long l = System.currentTimeMillis();
                index.add(rd.getCol1(), rd);
               // System.out.println("add took " + (System.currentTimeMillis() - l) + "ms size = " + entries.size());
              } else if (r == 1) {
                long l = System.currentTimeMillis();
                index.rebalance();
               // System.out.println("rebalance took " + (System.currentTimeMillis() - l) + "ms size = " + entries.size());
              } else if (r == 2) {
                ResultHandler<RowData> handler = new CollectingResultHandler(1);
                long l = System.currentTimeMillis();
                index.readOnly(handler, new SmallerEqualCondition("m"), false);
            //    System.out.println("read took " + (System.currentTimeMillis() - l) + "ms size = " + entries.size());
              } else if (r == 3) {
                AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
                Set<RowData> toDelete = new HashSet<RowData>();
                for (int i = 0; i < random.nextInt(6); i++) {
                  if (entries.size() > 0) {
                    RowData rd;
                    synchronized (entries) {
                      rd = entries.remove(random.nextInt(entries.size()));
                    }
                    if (toDelete.add(rd)) {
                      bulk.remove(rd.getCol1(), rd);
                    }
                  }
                }
                for (int i = 0; i < random.nextInt(7); i++) {
                  if (entries.size() > 0) {
                    RowData rd;
                    synchronized (entries) {
                      rd = entries.get(random.nextInt(entries.size()));
                      if (toDelete.add(rd)) {
                        String oldCol1 = rd.getCol1();
                        rd.col1 = createRandomString();
                        bulk.update(oldCol1, rd.getCol1(), rd);
                      }
                    }
                  }
                }
                int add = 100;
                if (entries.size() > 500000) {
                  add = 2;
                }

                for (int i = 0; i < random.nextInt(add); i++) {
                  RowData rd = createRandomRowData();
                  synchronized (entries) {
                    entries.add(rd);
                  }
                  bulk.add(rd.getCol1(), rd);
                }
                bulk.commit();
              }
              /*  StringBuilder sb = new StringBuilder();
                RebalancerTest.printTree(index.getRoot(), sb, 0);
                System.out.println("------------" + sb);*/
            }
          } finally {
            latch.countDown();
          }
        }

      });
      t.setDaemon(true);
      t.start();
    }
    latch.await();
  }

}

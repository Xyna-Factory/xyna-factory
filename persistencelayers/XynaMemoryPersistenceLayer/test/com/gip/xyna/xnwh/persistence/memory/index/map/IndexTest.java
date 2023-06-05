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
package com.gip.xyna.xnwh.persistence.memory.index.map;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.DataInterface;
import com.gip.xyna.xnwh.persistence.memory.TableObject;
import com.gip.xyna.xnwh.persistence.memory.TestTableObject.TestMemoryRowData;
import com.gip.xyna.xnwh.persistence.memory.TransactionCacheEntry;
import com.gip.xyna.xnwh.persistence.memory.TransactionCacheTable;
import com.gip.xyna.xnwh.persistence.memory.UnderlyingDataNotFoundException;
import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdate;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.index.ResultHandler;
import com.gip.xyna.xnwh.persistence.memory.index.tree.AbstractNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.IndexNodeValue;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.NodeCreator;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexTest.BiggerCondition;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexTest.BiggerEqualsCondition;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexTest.EqualsCondition;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexTest.SmallerCondition;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexTest.SmallerEqualCondition;
import com.gip.xyna.xnwh.persistence.memory.index.tree.LockedRootNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.LockedSubTree;
import com.gip.xyna.xnwh.persistence.memory.index.tree.LockedSubTreeNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.Root;

import junit.framework.TestCase;



public class IndexTest extends TestCase {


  private Random rrr = new Random(212);


  @Persistable(primaryKey = "pk", tableName = "test")
  private static class RowData extends Storable<RowData> {

    @Column(name = "col1")
    private String col1;
    private String col2;
    private static int nextid = 0;
    private int pk = nextid++;


    @Override
    public ResultSetReader<? extends RowData> getReader() {
      throw new RuntimeException();
    }


    @Override
    public Object getPrimaryKey() {
      return pk;
    }


    @Override
    public <U extends RowData> void setAllFieldsFromData(U data) {
      throw new RuntimeException();
    }


    @Override
    public String toString() {
      return "" + pk + "=>" + col1;
    }
  }


  private String createRandomString(Random random) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < random.nextInt(1) + 3; i++) {
      sb.append((char) ('a' + random.nextInt(10)));
    }
    return sb.toString();
  }


  private RowData createRandomRowData(Random random) {
    RowData r = new RowData();
    r.col1 = createRandomString(random);
    r.col2 = createRandomString(random);
    return r;
  }


  private static class CollectingResultHandler implements ResultHandler<RowData> {

    private List<RowData> data = new ArrayList<RowData>();


    public List<RowData> getDatas() {
      return data;
    }


    public boolean handle(List<RowData> values) {
      data.addAll(values);
      return true;
    }

  }

  private static class ListMap<K, V> extends HashMap<K, List<V>> {

    public void add(K k, V v) {
      List<V> l = get(k);
      if (l == null) {
        l = new ArrayList<>();
        put(k, l);
      }
      l.add(v);
    }

    public void remove2(K k, V v) {
      List<V> l = get(k);
      if (l == null) {
        return;
      }
      l.remove(v);
      if (l.isEmpty()) {
        remove(k);
      }
    }


    public K getRandomKey(Random random) {
      return new ArrayList<K>(keySet()).get(random.nextInt(size()));
    }


    public List<V> getOrEmpty(K key) {
      List<V> v = get(key);
      if (v == null) {
        v = Collections.emptyList();
      }
      return v;
    }

  }


  public void testFillAndFind() throws UnderlyingDataNotFoundException, PersistenceLayerException {
    boolean debug = false;
    boolean usemapidx = true;
    for (int x = 0; x < 10000; x++) {
      long seed = rrr.nextLong();
      Random random = new Random(seed);
      if (debug) {
        System.out.println("seed: " + seed);
      }
      Index<String, RowData> index =
          usemapidx ? new IndexImplMap<String, RowData>(new ReentrantReadWriteLock()) : new IndexImplTree<>(new NodeCreator<String, RowData>() {

            private int n = 3;


            @Override
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


            @Override
            public AbstractNode<IndexNodeValue<String, RowData>> transformNode(AbstractNode<IndexNodeValue<String, RowData>> source) {
              AbstractNode<IndexNodeValue<String, RowData>> newNode =
                  createNode(source.getDepth(), (AbstractNode<IndexNodeValue<String, RowData>>) source.getRoot(), source.getValue(),
                             source.getParent().getValue());
              newNode.copyFields(source);
              for (AbstractNode<IndexNodeValue<String, RowData>> child : newNode.getChildren()) {
                child.setParent(newNode);
              }
              return newNode;
            }


            @Override
            public AbstractNode<IndexNodeValue<String, RowData>> createNode(int depth, AbstractNode<IndexNodeValue<String, RowData>> root,
                                                                            IndexNodeValue<String, RowData> value,
                                                                            IndexNodeValue<String, RowData> parentValue) {
              if (depth == 0) {
                //root
                return new LockedRootNode<IndexNodeValue<String, RowData>>(2, new ReentrantReadWriteLock());
              } else if (depth % n == 0) {
                return new LockedSubTree<IndexNodeValue<String, RowData>>((Root) root, value, new ReentrantReadWriteLock());
              } else {
                return new LockedSubTreeNode<IndexNodeValue<String, RowData>>((Root) root, value,
                                                                              ((LockedSubTree<IndexNodeValue<String, RowData>>) root)
                                                                                  .getLock());
              }
            }

          });
      ListMap<String, TestMemoryRowData<RowData>> map = new ListMap<String, TestMemoryRowData<RowData>>();
      ListMap<String, RowData> removed = new ListMap<String, RowData>();
      TableObject<RowData, TestMemoryRowData<RowData>> to = new TableObject<IndexTest.RowData, TestMemoryRowData<RowData>>(RowData.class) {

        @Override
        public ReadWriteLock getTableLock() {
          return null;
        }


        @Override
        public DataInterface<RowData, TestMemoryRowData<RowData>> getDataInterface() {
          return null;
        }
      };
      for (int i = 0; i < 1; i++) {
        RowData r = createRandomRowData(random);
        String k = r.col1;
        index.add(k, r);
        map.add(k, new TestMemoryRowData<RowData>(to, r, false));
      }
      for (int i = 0; i < 100; i++) {
        AtomicBulkUpdate<String, RowData> bulk = index.startBulkUpdate(null);
        TransactionCacheTable<RowData> tct = new TransactionCacheTable<>("bla");
        if (debug) {
          System.out.println("------");
          System.out.println("map: " + map);
        }
        if (map.size() > 0) {

          //update
          String k = map.getRandomKey(random);
          List<TestMemoryRowData<RowData>> vs = map.get(k);
          TestMemoryRowData<RowData> v = vs.get(random.nextInt(vs.size()));
          map.remove2(k, v);
          String newKey = createRandomString(random);
          RowData newV = new RowData();
          newV.pk = v.getData(null).pk;
          newV.col2 = newV.col2;
          newV.col1 = newKey;
          tct.addUpdatedTableContent(v, newV, 1);
          map.add(newKey, v);

          //remove
          k = map.getRandomKey(random);
          vs = map.get(k);
          v = vs.get(random.nextInt(vs.size()));
          map.remove2(k, v);
          removed.add(k, v.getData(null));
          tct.addDeletedTableContent(v, v.getData(null), 1);
        }

        //add
        for (int j = 0; j < random.nextInt(5) + 1; j++) {
          RowData v = createRandomRowData(random);
          String k = v.col1;
          TestMemoryRowData<RowData> d = new TestMemoryRowData<RowData>(to, v, false);
          tct.addInsertedTableContent(d, v, 1);
          map.add(k, d);
        }
        if (debug) {
          System.out.println("index: " + index);
        }

        for (Entry<Object, TransactionCacheEntry<RowData>> e : tct.allUpdatedObjects().entrySet()) {
          String old = e.getValue().getRowData().getData(null).col1;
          String newV = e.getValue().getNewContent().col1;
          RowData oldStorable = e.getValue().getRowData().getData(null);
          oldStorable.col1 = newV; //update im im index refernzierten objekt und in der map
          bulk.update(old, newV, oldStorable);
        }
        for (Entry<Object, TransactionCacheEntry<RowData>> e : tct.allInsertedObjects().entrySet()) {
          bulk.add(e.getValue().getNewContent().col1, e.getValue().getNewContent());
        }
        for (Entry<Object, TransactionCacheEntry<RowData>> e : tct.allDeletedObjects().entrySet()) {
          bulk.remove(e.getValue().getRowData().getData(null).col1, e.getValue().getRowData().getData(null));
        }

        if (debug) {
          System.out.println("bulk commit: " + bulk);
        }
        bulk.commit();
      }
      if (debug) {
        System.out.println("index: " + index);
      }

      for (boolean reverse : new boolean[] {true, false}) {

        for (Entry<String, List<TestMemoryRowData<RowData>>> entry : map.entrySet()) {
          CollectingResultHandler handler = new CollectingResultHandler();
          index.readOnly(handler, new EqualsCondition(entry.getKey()), reverse);
          assertNotNull(handler.data);
          if (debug) {
            System.out.println("check equals: " + entry.getKey());
          }
          assertEquals(entry.getValue().size(), handler.data.size()); //kann größer 1 sein, falls der gleiche string mehrfach geaddet wird (gleicher key)

          //gleiche objekte (gleiche pks) in map und handler
          Set<Integer> idsInMap = new HashSet<>();
          for (TestMemoryRowData<RowData> rd : entry.getValue()) {
            idsInMap.add(rd.getData(null).pk);
          }
          for (RowData rd : handler.data) {
            idsInMap.remove(rd.pk);
          }
          assertEquals(0, idsInMap.size());
        }

        for (Entry<String, List<RowData>> entry : removed.entrySet()) {
          CollectingResultHandler handler = new CollectingResultHandler();
          index.readOnly(handler, new EqualsCondition(entry.getKey()), reverse);
          assertNotNull(handler.data);
          if (debug) {
            System.out.println("check removed: " + entry.getKey());
          }
          assertEquals(map.getOrEmpty(entry.getKey()).size(), handler.data.size());
          for (RowData rd : entry.getValue()) {
            assertFalse(map.getOrEmpty(entry.getKey()).contains(rd));
          }
        }

        //smaller
        CollectingResultHandler handler = new CollectingResultHandler();
        List<String> l = new ArrayList<String>(map.keySet());
        String searchKey = l.get(l.size() / 2);
        index.readOnly(handler, new SmallerCondition(searchKey), reverse);
        String last = null;
        for (int i = 0; i < handler.data.size(); i++) {
          String val = handler.data.get(i).col1;
          assertTrue(val.compareTo(searchKey) < 0);
          if (last != null) {
            if (reverse) {
              assertTrue(val.compareTo(last) <= 0);
            } else {
              assertTrue(val.compareTo(last) >= 0);
            }
          }
          last = val;
        }

        //bigger
        handler = new CollectingResultHandler();
        l = new ArrayList<String>(map.keySet());
        index.readOnly(handler, new BiggerCondition(searchKey), reverse);
        last = null;
        for (int i = 0; i < handler.data.size(); i++) {
          String val = handler.data.get(i).col1;
          assertTrue(val.compareTo(searchKey) > 0);
          if (last != null) {
            if (reverse) {
              assertTrue(val.compareTo(last) <= 0);
            } else {
              assertTrue(val.compareTo(last) >= 0);
            }
          }
          last = val;
        }

        //smaller or equal
        handler = new CollectingResultHandler();
        l = new ArrayList<String>(map.keySet());
        index.readOnly(handler, new SmallerEqualCondition(searchKey), reverse);
        last = null;
        for (int i = 0; i < handler.data.size(); i++) {
          String val = handler.data.get(i).col1;
          assertTrue(val.compareTo(searchKey) <= 0);
          if (last != null) {
            if (reverse) {
              assertTrue(val.compareTo(last) <= 0);
            } else {
              assertTrue(val.compareTo(last) >= 0);
            }
          }
          last = val;
        }
        //bigger or equal
        handler = new CollectingResultHandler();
        l = new ArrayList<String>(map.keySet());
        index.readOnly(handler, new BiggerEqualsCondition(searchKey), reverse);
        last = null;
        for (int i = 0; i < handler.data.size(); i++) {
          String val = handler.data.get(i).col1;
          assertTrue(val.compareTo(searchKey) >= 0);
          if (last != null) {
            if (reverse) {
              assertTrue(val.compareTo(last) <= 0);
            } else {
              assertTrue(val.compareTo(last) >= 0);
            }
          }
          last = val;
        }
      }
    }
  }

}

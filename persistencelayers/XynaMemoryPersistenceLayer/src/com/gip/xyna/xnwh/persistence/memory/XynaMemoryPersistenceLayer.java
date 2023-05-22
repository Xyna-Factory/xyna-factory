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

package com.gip.xyna.xnwh.persistence.memory;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.index.map.IndexImplMap;
import com.gip.xyna.xnwh.persistence.memory.index.tree.AbstractNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.IndexNodeValue;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.NodeCreator;
import com.gip.xyna.xnwh.persistence.memory.index.tree.LockedRootNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.LockedSubTree;
import com.gip.xyna.xnwh.persistence.memory.index.tree.LockedSubTreeNode;
import com.gip.xyna.xnwh.persistence.memory.index.tree.Root;


//TODO locks sind als reentrantlocks threadbasiert. das bedeutet, dass der gleiche thread mit einer anderen connection auf objekte zugreifen kann, was er eigl nicht tun k�nnen sollte!
public abstract class XynaMemoryPersistenceLayer implements PersistenceLayer {

  protected static final Logger logger = CentralFactoryLogging.getLogger(XynaMemoryPersistenceLayer.class);


  private PreparedQueryCreator preparedQueryCreator;

  public XynaMemoryPersistenceLayer() throws PersistenceLayerException {    
    preparedQueryCreator = new PreparedQueryCreator(IDGenerator.generateUniqueIdForThisSession());
  }
  
  private final WeakHashMap<Class<?>, PreparedCommand> deleteAll = new WeakHashMap<Class<?>, PreparedCommand>();
  
  
  public WeakHashMap<Class<?>, PreparedCommand> getDeleteAllCommandCache() {
    return deleteAll;
  }

  public final boolean describesSamePhysicalTables(PersistenceLayer pl) {
    return pl == this;
  }

  public <T extends Storable> void createIndicesForTable(TableObject<T, MemoryRowData<T>> tableObject, boolean useMapIndex) {
    for (ColumnDeclaration c : tableObject.getColTypes()) {
      if (c.getIndexType() != IndexType.NONE) {
        // TODO code generieren, der die getter auf die storables f�r die zu indizierende spalte enth�lt.
        Index<? extends Comparable<?>, MemoryRowData<T>> newIndex;
        if (useMapIndex) {
          newIndex = new IndexImplMap(new ReentrantReadWriteLock());
        } else {
          NodeCreator<? extends Comparable<?>, MemoryRowData<?>> tmp = getNodeCreator(this);
          newIndex = new IndexImplTree(tmp);
        }
        // TODO index f�r bereits existierende objekte aufbauen. f�r LocalMemory spielt das keine Rolle,
        //      weil vorher noch nichts drin sein kann.
        tableObject.addIndex(c, newIndex);
      }
    }
  }


  public PreparedQueryCreator getPreparedQueryCreator() {
    return preparedQueryCreator;
  }


  protected abstract NodeCreator<? extends Comparable<?>, MemoryRowData<?>> getNodeCreator(XynaMemoryPersistenceLayer pl);

  

  protected static class DefaultMemoryNodeCreator<C extends Comparable<C>, T extends Storable>
      implements
        NodeCreator<C, MemoryRowData<T>> {

    private int n = 3;
    private XynaMemoryPersistenceLayer persistenceLayer;


    public DefaultMemoryNodeCreator(XynaMemoryPersistenceLayer persistenceLayer) {
      this.persistenceLayer = persistenceLayer;
    }


    public AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> createNode(int depth,
                                                                        AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> root,
                                                                        IndexNodeValue<C, MemoryRowData<T>> value,
                                                                        IndexNodeValue<C, MemoryRowData<T>> parentsValue) {

      if (depth == 0) {
        //root
        return new LockedRootNode<IndexNodeValue<C, MemoryRowData<T>>>(3, new ReentrantReadWriteLock());
      } else if (depth % n == 0) {
        return new LockedSubTree<IndexNodeValue<C, MemoryRowData<T>>>((Root) root, value, new ReentrantReadWriteLock());
      } else {
        return new LockedSubTreeNode<IndexNodeValue<C, MemoryRowData<T>>>(
                                                                          (Root) root,
                                                                          value,
                                                                          ((LockedSubTree<IndexNodeValue<C, MemoryRowData<T>>>) root)
                                                                              .getLock());
      }

    }


    public AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> transformNode(AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> source) {
      AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> newNode =
          createNode(source.getDepth(), (AbstractNode<IndexNodeValue<C, MemoryRowData<T>>>) source.getRoot(),
                     source.getValue(), source.getParent().getValue());
      newNode.copyFields(source);
      for (AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> child : newNode.getChildren()) {
        child.setParent(newNode);
      }
      return newNode;
    }


    public boolean validateNodeType(AbstractNode<IndexNodeValue<C, MemoryRowData<T>>> node) {
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


  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return getConnection();
  }

  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (plc.getClass() == getClass()) {
      return true;
    }
    return false;
  }
  
}

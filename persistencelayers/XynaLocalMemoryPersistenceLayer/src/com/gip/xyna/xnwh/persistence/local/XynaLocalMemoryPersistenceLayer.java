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

package com.gip.xyna.xnwh.persistence.local;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.MemoryPersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.memory.MemoryRowData;
import com.gip.xyna.xnwh.persistence.memory.MemoryRowLock;
import com.gip.xyna.xnwh.persistence.memory.TableObject;
import com.gip.xyna.xnwh.persistence.memory.TransactionCache;
import com.gip.xyna.xnwh.persistence.memory.XynaMemoryPersistenceLayer;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.NodeCreator;



public class XynaLocalMemoryPersistenceLayer extends XynaMemoryPersistenceLayer {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaLocalMemoryPersistenceLayer.class);

  private HashMap<String, LocalTableObject> knownTables;
  private boolean useIndexTypeMap = false;


  public XynaLocalMemoryPersistenceLayer() throws PersistenceLayerException {
    knownTables = new HashMap<String, LocalTableObject>();
  }


  public PersistenceLayerConnection getConnection() {
    return new LocalMemoryPersistenceLayerConnection(this);
  }

  
  public PersistenceLayerConnection getDedicatedConnection() {
    return new LocalMemoryPersistenceLayerConnection(this);
  }


  public class LocalMemoryPersistenceLayerConnection extends MemoryPersistenceLayerConnection {


    private LocalMemoryPersistenceLayerConnection(XynaMemoryPersistenceLayer pl) {
      super(pl);
    }


    private String getTableNameByStorableClass(Class<Storable> klazz) throws PersistenceLayerException {
      try {
        return klazz.newInstance().getTableName();
      } catch (InstantiationException e) {
        throw new XNWH_GeneralPersistenceLayerException("Storables need to provide a public no-argument constructor");
      } catch (IllegalAccessException e) {
        throw new XNWH_GeneralPersistenceLayerException("Storables need to provide a public no-argument constructor");
      }
    }


    /**
     * ist table vorhanden?
     */
    @Override
    protected LocalTableObject checkTable(String tableName) throws PersistenceLayerException {
      LocalTableObject t = knownTables.get(tableName.toLowerCase());
      if (t == null) {
        // TODO code
        throw new XNWH_GeneralPersistenceLayerException("table " + tableName + " unknown");
      }
      return t;
    }


    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
      ClassLoader cl = getClass().getClassLoader();
      if (cl instanceof ClassLoaderBase) {
        ClassLoader storableClassLoader = klass.getClassLoader();
        if (storableClassLoader instanceof ClassLoaderBase) {
          ClassLoaderBase clb = (ClassLoaderBase)cl;
          ClassLoaderBase storableClb = (ClassLoaderBase)storableClassLoader;
          clb.addWeakReferencedParentClassLoader(storableClb);
        }
      }
      LocalTableObject<T> to = new LocalTableObject<T>(klass);
      knownTables.put(to.getName().toLowerCase(), to);
      TableObject tmp = (TableObject) to;
      getContainingPersistenceLayer().createIndicesForTable((TableObject<T, MemoryRowData<T>>) tmp, useIndexTypeMap);
    }


    @Override
    protected XynaMemoryPersistenceLayer getContainingPersistenceLayer() {
      return XynaLocalMemoryPersistenceLayer.this;
    }


    @Override
    protected void commitInternally(TransactionCache transactionCache, List<MemoryRowLock> arg1)
        throws PersistenceLayerException {
      defaultCommit(transactionCache);
    }


    @Override
    protected void rollbackInternallyWithoutLocks(TransactionCache transactionCache) {
      // nothing to be done for the local memory persistence layer:
      // - locks are taken care of by the general implementation
      // - potentially created objects that have not been linked into the TableObject yet will be gc'ed
    }


    public <T extends Storable> void removeTable(Class<T> klass, Properties props) throws PersistenceLayerException {
      knownTables.remove(Storable.getPersistable(klass).tableName().toLowerCase());
    }



  }


  public String getInformation() {
    int size = knownTables.size();
    return "Memory (" + size + " known Table" + (size != 1 ? "s" : "") + ")";
  }


  public void init(Long pliID, String... arg0) throws PersistenceLayerException {
    if (arg0 != null && arg0.length == 1 && arg0[0].length() > 0) {
      if (arg0[0].equalsIgnoreCase("map")) {
        useIndexTypeMap = true;
      } else if (arg0[0].equalsIgnoreCase("tree")) {
        useIndexTypeMap = false;
      } else {
        logger.warn("Unexpected initialization parameter <" + arg0[0] + ">, ignoring");
      }
    }
  }


  public String[] getParameterInformation() {
    return new String[] {"indextype (optional, default is tree): tree | map"};
  }


  public void shutdown() throws PersistenceLayerException {
  }


  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }


  @Override
  protected NodeCreator<? extends Comparable<?>, MemoryRowData<?>> getNodeCreator(XynaMemoryPersistenceLayer arg0) {
    return new DefaultMemoryNodeCreator(arg0);
  }

}

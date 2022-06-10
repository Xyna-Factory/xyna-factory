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

package com.gip.xyna.xnwh.persistence.memory;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.NodeCreator;



public class TestMemoryPersistenceLayer extends XynaMemoryPersistenceLayer {

  private HashMap<String, TestTableObject> knownTables;


  public TestMemoryPersistenceLayer() throws PersistenceLayerException {
    knownTables = new HashMap<String, TestTableObject>();
  }


  public PersistenceLayerConnection getConnection() {
    return new TestMemoryPersistenceLayerConnection(this);
  }

  
  public PersistenceLayerConnection getDedicatedConnection() {
    return new TestMemoryPersistenceLayerConnection(this);
  }
  

  public class TestMemoryPersistenceLayerConnection extends MemoryPersistenceLayerConnection {


    private TestMemoryPersistenceLayerConnection(XynaMemoryPersistenceLayer pl) {
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
    protected <T extends Storable, X extends MemoryRowData<T>> TableObject<T, X> checkTable(String tableName) throws PersistenceLayerException {
      TableObject<T, X> t = knownTables.get(tableName.toLowerCase());
      if (t == null) {
        throw new XNWH_GeneralPersistenceLayerException("table " + tableName + " unknown");
      }
      return t;
    }


    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
      TestTableObject<?> to = new TestTableObject<T>(klass);
      knownTables.put(to.getName().toLowerCase(), to);
      getContainingPersistenceLayer().createIndicesForTable((TableObject) to, false);
    }


    @Override
    protected XynaMemoryPersistenceLayer getContainingPersistenceLayer() {
      return TestMemoryPersistenceLayer.this;
    }


    @Override
    protected void commitInternally(TransactionCache transactionInformation, List<MemoryRowLock> sustainedLocks)
        throws PersistenceLayerException {
      defaultCommit(transactionInformation);
    }


    @Override
    protected void rollbackInternallyWithoutLocks(TransactionCache transactionCache) {
      // nothing to be done (see local persistence layer connection)
    }


    public <T extends Storable> void removeTable(Class<T> arg0, Properties arg1) throws PersistenceLayerException {
      // ntbd
    }

  }


  public String getInformation() {
    return "Memory (" + knownTables.size() + " known Tables)";
  }


  public void init(Long pliID, String... arg0) throws PersistenceLayerException {
  }


  public String[] getParameterInformation() {
    return new String[0];
  }


  public void shutdown() throws PersistenceLayerException {
  }


  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }


  @Override
  protected NodeCreator<? extends Comparable<?>, MemoryRowData<?>> getNodeCreator(XynaMemoryPersistenceLayer pl) {
    return new DefaultMemoryNodeCreator(this);
  }


  @Override
  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return null;
  }


  @Override
  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    return false;
  }

}

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
package com.gip.xyna.xnwh.pools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;


// FIXME should be a function group but there was no appropriate section to file it under
public class ConnectionPoolManagement extends Section {
  
  private final static String DEFAULT_NAME = "Connection Pool Management";
  private final static String FUTURE_EXECUTION_INIT_NAME = ConnectionPoolManagement.class.getSimpleName() + ".init";

  private volatile static ConnectionPoolManagement instance;
  
  private ConcurrentMap<String, ConnectionPoolType> registeredPooltypes;
  private Map<String, PoolDefinition> registeredPoolDefinitions;
  
  
  // TODO access to getInstance should be restricted as it should be accessed by it's factory path
  //      a static access route is only necessary for it's initialization (as it won't be reachable at that point)
  public static ConnectionPoolManagement getInstance() {
    if (instance == null) {
      synchronized (ConnectionPoolManagement.class) {
        if (instance == null) {
          try {
            instance = new ConnectionPoolManagement();
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return instance;
  }
  
  
  private ConnectionPoolManagement() throws XynaException {
    super();
  }
  
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  
  private Collection<PoolDefinition> loadPooldefinitions() throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(PoolDefinition.class);
    ODSConnection con;
    con = ods.openConnection(ODSConnectionType.HISTORY);
    Collection<PoolDefinition> defs;
    try {
      try {
        defs = con.loadCollection(PoolDefinition.class);
      } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
        logger.debug("using backup xml connection");
        PersistenceLayerConnection xmlCon = getXMLConnection();
        defs = xmlCon.loadCollection(PoolDefinition.class);
      }
    } finally {
      con.closeConnection();
    }
    return defs;
  }

  @Override
  protected void init() throws XynaException {
    registeredPooltypes = new ConcurrentHashMap<String, ConnectionPoolType>();
    registeredPoolDefinitions = new HashMap<String, PoolDefinition>();
    List<ConnectionPoolType> poolTypes = discoverPoolTypes();
    for (ConnectionPoolType connectionPoolType : poolTypes) {
      if (connectionPoolType.getName() == null) {
        logger.warn("Discovered pooltype '" + connectionPoolType + "' returns null as name, skipping registration.");
      } else {
        registeredPooltypes.put(connectionPoolType.getName(), connectionPoolType);
      }
    }
    
    FutureExecution fe = XynaFactory.getInstance().getFutureExecutionForInit();
    fe.addTask(ConnectionPoolManagement.class, FUTURE_EXECUTION_INIT_NAME)
      .after(ODS.FUTURE_EXECUTION_ID__PREINIT_XML_PERSISTENCE_LAYER)
      .before(ODS.FUTURE_EXECUTION_ID__PREINIT_PERSISTENCE_LAYER_INSTANCES)
      .execAsync(new Runnable() {
        public void run() {
          try {
            ODS ods = ODSImpl.getInstance();
            ods.registerStorable(PoolDefinition.class);
            ODSConnection con;
            con = ods.openConnection(ODSConnectionType.HISTORY);
            
            try {
              Collection<PoolDefinition> defs = loadPooldefinitions();
              for (PoolDefinition poolDefinition : defs) {
                if (logger.isDebugEnabled()) {
                  logger.debug("adding registeredPoolDefinition for: " + poolDefinition.getName());
                }
                registeredPoolDefinitions.put(poolDefinition.getName(), poolDefinition);
              }
            } finally {
              try {
                con.closeConnection();
              } catch (PersistenceLayerException e) {
                logger.warn("Could not close connection!",e);
              }
            }
          } catch (PersistenceLayerException e) {
            throw new RuntimeException("Failed to initialize " + DEFAULT_NAME, e);
          }
        }
      });
    
  }
  
  
  //called via reflection from UpdatePoolDefinition
  @SuppressWarnings("unused")
  private void reinit() throws PersistenceLayerException {
    registeredPooltypes = new ConcurrentHashMap<String, ConnectionPoolType>();
    registeredPoolDefinitions = new HashMap<String, PoolDefinition>();
    List<ConnectionPoolType> poolTypes = discoverPoolTypes();
    for (ConnectionPoolType connectionPoolType : poolTypes) {
      if (connectionPoolType.getName() == null) {
        logger.warn("Discovered pooltype '" + connectionPoolType + "' returns null as name, skipping registration.");
      } else {
        registeredPooltypes.put(connectionPoolType.getName(), connectionPoolType);
      }
    }
    Collection<PoolDefinition> defs = loadPooldefinitions();
    for (PoolDefinition poolDefinition : defs) {
      registeredPoolDefinitions.put(poolDefinition.getName(), poolDefinition);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
    
  }
  
  
  private List<ConnectionPoolType> discoverPoolTypes() {
    List<ConnectionPoolType> poolTypes = new ArrayList<ConnectionPoolType>();
    File pooltypeFolder = new File("." + Constants.FILE_SEPARATOR + "conpooltypes");
    if (pooltypeFolder.exists() &&
        pooltypeFolder.isDirectory()) {
      File[] pooltypeJars = pooltypeFolder.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar");
        }
      });
      for (File pooltypeJar : pooltypeJars) {
        try {
          ZipInputStream zis = new ZipInputStream(new FileInputStream(pooltypeJar));
          try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
              if (!entry.isDirectory()) {
                final String CLASS_SUFFIX = ".class";
                if (entry.getName().endsWith(CLASS_SUFFIX)) {
                  String className = entry.getName().substring(0, entry.getName().length() - CLASS_SUFFIX.length()).replaceAll(Constants.fileSeparator, ".");
                  if (className != null) {
                    try {
                      Class<?> clazz = Class.forName(className);
                      if (ConnectionPoolType.class.isAssignableFrom(clazz)) { // found one
                        ConnectionPoolType poolType = (ConnectionPoolType) clazz.getConstructor().newInstance();
                        poolTypes.add(poolType);
                      }
                    } catch (Throwable e) { // all those errors are not severe as non pooltypes don't need to be constructable with clazz.newInstance()
                      logger.debug("Error while discovering pooltypes for " + className, e);
                    }
                  }
                }
              }
              entry = zis.getNextEntry();
            }
          } catch (IOException e) {
            logger.warn("Could not read jar file '" + pooltypeJar.getName() + "'.", e);
          } finally {
            try {
              zis.close();
            } catch (IOException e) {
              logger.warn("Could not read jar file '" + pooltypeJar.getName() + "'.", e);
            }
          }
        } catch (FileNotFoundException e) {
          // and how exactly would that happen?
        }
        
      }
    }
    return poolTypes;
  }

  
  public boolean registerConnectionPoolType(ConnectionPoolType type) {
    return registeredPooltypes.putIfAbsent(type.getName(), type) == null;
  }
  
  
  protected ConnectionPoolType getRegisteredPooltype(String typeName) {
    return registeredPooltypes.get(typeName);
  }
  
  public void testConnectionPoolParameter(ConnectionPoolParameter poolParams) throws NoConnectionAvailableException {
    PoolDefinition currentDefinition = getConnectionPoolDefinition(poolParams.getName());
    TypedConnectionPoolParameter newParams;
    if (currentDefinition == null) {
      if (poolParams instanceof TypedConnectionPoolParameter) {
        newParams = (TypedConnectionPoolParameter) poolParams;
      } else {
        throw new IllegalArgumentException("Pool does not exist");
      }
    } else {
      TypedConnectionPoolParameter currentParams = currentDefinition.toCreationParameter();
      newParams = merge(currentParams, poolParams);
    }
    Exception e = newParams.getValidationStrategy().validate(newParams.getConnectionBuildStrategy().createNewConnection());
    if (e != null) {
      if (e instanceof NoConnectionAvailableException) {
        throw (NoConnectionAvailableException)e;
      } else {
        throw new NoConnectionAvailableException(e, newParams.getNoConnectionAvailableReasonDetector());
      }
    }
  }
  
  /**
   * Speichert die ConnectionPoolParameter, falls noch keine Daten zu dem Namen gespeichert sind
   * @param creationParams
   */
  public void addConnectionPool( ConnectionPoolParameter creationParams) {
    if( registeredPoolDefinitions.get(creationParams.getName()) != null ) {
      throw new RuntimeException("ConnectionPool \""+creationParams.getName()+"\" already defined"); //TODO XynaException?
    } else {
      ConnectionPoolParameter cppClone = creationParams.clone();
      storeConnectionPoolParams(cppClone);
    }
  }
  
  /**
   * Startet den ConnectionPool oder führt bei bereits gestarteten ConnectionPools einen Restart durch
   * @param name
   * @param restart bei true: Schließen und Neuöffnen aller Connections 
   * @param force   bei true: auch aktive Connections werden geschlossen
   * @return
   * @throws NoConnectionAvailableException
   */
  public ConnectionPool startConnectionPool(String name, boolean restart, boolean force) throws NoConnectionAvailableException {
    ConnectionPool cp = getConnectionPoolNoLazyCreation(name);
    if( cp != null ) {
      if( restart ) {
        cp.restart(force);
      }
      return cp; //bereits gestartet
    }
    PoolDefinition poolDef = registeredPoolDefinitions.get(name);
    if( poolDef == null ) {
      throw new RuntimeException("ConnectionPool \""+name+"\" is not defined"); //TODO XynaException?
    }
   
    return ConnectionPool.getInstance(poolDef.toCreationParameter());
  }

  /**
   * Startet den ConnectionPool, evtl. werden Daten dazu gespeichert 
   * (sollte nur von PersistenceLayer gerufen werden)
   * @param creationParams
   * @return
   * @throws NoConnectionAvailableException
   */
  public ConnectionPool startAndAddConnectionPool(ConnectionPoolParameter creationParams) throws NoConnectionAvailableException {
    if( registeredPoolDefinitions.get(creationParams.getName()) != null ) {
      return startConnectionPool(creationParams.getName(), false, false);
    } else {
      addConnectionPool(creationParams);
      return startConnectionPool(creationParams.getName(), false, false);
    }
  }
  
  public boolean removeConnectionPool(String name, boolean force, long timeout, TimeUnit unit) throws ConnectionCouldNotBeClosedException, PersistenceLayerException {
    ConnectionPool pool = getConnectionPoolNoLazyCreation(name);
    if (pool == null) {
      //dann ist der pool noch nicht in verwendung
    } else {
      try {
        ConnectionPool.removePool(pool, force, unit.toMillis(timeout));
      } catch (ConnectionCouldNotBeClosedException e) {
        if (force) {
          //weitermachen
          logger.info("Not all connections were closed in time", e);
        } else {
          throw e;
        }
      }
    }
    PoolDefinition poolDef = registeredPoolDefinitions.remove(name);
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(poolDef);
      con.commit();
    } finally {
      con.closeConnection();
    }
    return true;
  }


  public boolean shutdownConnectionPool(String name, boolean force, long timeout, TimeUnit unit) throws ConnectionCouldNotBeClosedException {
    ConnectionPool pool = getConnectionPoolNoLazyCreation(name);
    if (pool == null) {
      return false;
    } else {
      ConnectionPool.closedownPool(pool, force, unit.toMillis(timeout));
      return true;
    }
  }
  
  
  public boolean startConnectionPool(String name) throws NoConnectionAvailableException {
    ConnectionPool pool = getConnectionPool(name);
    if (pool == null) {
      return false;
    } else {
      pool.restart(false);
      return true;
    }
  }
  
  
  public boolean modifyConnectionPool(ConnectionPoolParameter modificationParams, boolean force) {
    PoolDefinition currentDefinition = getConnectionPoolDefinition(modificationParams.getName());
    if (currentDefinition == null) {
      return false;
    }
    TypedConnectionPoolParameter currentParams = currentDefinition.toCreationParameter();
    boolean invalidatesConnections = changeWarrantsInvalidation(currentParams, modificationParams);
    TypedConnectionPoolParameter newParams = merge(currentParams, modificationParams);
    ConnectionPool pool = getConnectionPoolNoLazyCreation(modificationParams.getName());
    if (pool != null) {
      pool.adjustPoolParameter(newParams, invalidatesConnections, force);
    }
    storeConnectionPoolParams(newParams);
    return true;
  }
  
  
  private boolean changeWarrantsInvalidation(TypedConnectionPoolParameter currentDefinition, ConnectionPoolParameter modifiedDefinition) {
    if ((modifiedDefinition.getConnectString() == null || currentDefinition.getConnectString().equals(modifiedDefinition.getConnectString())) &&
        (modifiedDefinition.getPassword() == null || currentDefinition.getPassword().equals(modifiedDefinition.getPassword())) &&
        (modifiedDefinition.getUser() == null || currentDefinition.user.equals(modifiedDefinition.getUser()))) {
      Map<String, Object> currentAdditionals = currentDefinition.getAdditionalParams();
      Map<String, Object> modificationAdditionals = modifiedDefinition.getAdditionalParams();
      List<StringParameter<?>> modificationParams = currentDefinition.getAdditionalDescription().getParameters(ParameterUsage.Modify);
      for (StringParameter<?> param : modificationParams) {
        if (modificationAdditionals.containsKey(param.getName())) {
          if (currentAdditionals.get(param.getName()) == null ||
              !currentAdditionals.get(param.getName()).equals(modificationAdditionals.get(param.getName()))) {
            if (currentDefinition.parameterChangeEntailsConnectionRebuild(param.getName())) {
              return true;
            }
          }
        }
      }
      return false;
    } else {
      return true;
    }
  }
  
  
  private TypedConnectionPoolParameter merge(TypedConnectionPoolParameter currentParams,
                                             ConnectionPoolParameter modificationParams) {
    if (modificationParams.getConnectString() != null) {
      currentParams.connectString = modificationParams.getConnectString();
    }
    if (modificationParams.getMaxRetries() > -1) {
      currentParams.maxRetries(modificationParams.getMaxRetries());
    }
    if (modificationParams.getPassword() != null) {
      currentParams.password = modificationParams.getPassword();
    }
    if (modificationParams.getSize() > -1) {
      currentParams.size = modificationParams.getSize();
    }
    if (modificationParams.getUser() != null) {
      currentParams.user = modificationParams.getUser();
    }
    if (modificationParams.getAdditionalParams() != null) {
      for (Entry<String, Object> entry : modificationParams.getAdditionalParams().entrySet()) {
        if (entry.getValue() == null) {
          currentParams.additionalParams.remove(entry.getKey());
        } else {
          currentParams.additionalParams.put(entry.getKey(), entry.getValue());
        }
      }
    }
    if (modificationParams.getValidationInterval() >  -1) {
      currentParams.getValidationStrategy().setValidationInterval(modificationParams.getValidationInterval());
    }
    return currentParams;
  }


  // persisted connectionpool data has to be read before instantiating user defined persistencelayers this is only possible via XML
  private PersistenceLayerConnection getXMLConnection() throws PersistenceLayerException {
    XMLPersistenceLayer xmlpers = new XMLPersistenceLayer();
    xmlpers.init(null, "default" + ODSConnectionType.HISTORY.toString(), TransactionMode.FULL_TRANSACTION.name(), "false");
    return xmlpers.getConnection();
  }
  
  
  public List<PluginDescription> listConnectionPoolTypeInformation() {
    List<PluginDescription> infos = new ArrayList<PluginDescription>();
    for (ConnectionPoolType cpt : registeredPooltypes.values()) {
      infos.add(cpt.getPluginDescription());
    }
    return infos;
  }
  
  
  public List<ConnectionPoolInformation> listConnectionPoolInformation() {
    HashMap<String,ConnectionPool> pools = new HashMap<String,ConnectionPool>();
    for( ConnectionPool cp : ConnectionPool.getAllRegisteredConnectionPools() ) {
      pools.put( cp.getId(), cp );
    }
    
    HashSet<String> allIds = new HashSet<String>();
    allIds.addAll( pools.keySet() );
    allIds.addAll( registeredPoolDefinitions.keySet() );
    ArrayList<String> allIdsSorted = new ArrayList<String>(allIds);
    Collections.sort(allIdsSorted);
    
    List<ConnectionPoolInformation> infos = new ArrayList<ConnectionPoolInformation>(allIdsSorted.size());
    
    Map<String, Integer> sqlStats = null;
    ThreadInformation[] waitingThreadInformation = null;
    
    
    for( String name : allIdsSorted ) {
      ConnectionPool pool = pools.get(name);
      PoolDefinition def = registeredPoolDefinitions.get(name);
      
      int used = 0;
      int size = def == null ? 0 : def.size;
      String pooltype = def == null ? "Custom" : def.type;
      String state = "Unused";
      String poolIdentity = "-";
      boolean isDynamic = false;
      
      if( pool != null ) {
        com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation[] cis = pool.getConnectionStatistics();
        for (com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation ci : cis) {
          if (ci.isInUse()) {
            used++;
          }
        }
        size = pool.getConnectionStatistics().length;
        isDynamic = pool.isDynamic();
        sqlStats = pool.getSQLStatistics();
        waitingThreadInformation = pool.getWaitingThreadInformation();
        
        state = pool.isClosed() ? "Shutdown" : "Running";
        poolIdentity = pool.toString();
      } else {
        sqlStats = Collections.emptyMap();
        waitingThreadInformation = new ThreadInformation[]{};
      }
      
      ConnectionPoolInformation cpi = new ConnectionPoolInformation(name, pooltype, size, isDynamic, used, state, poolIdentity, sqlStats, waitingThreadInformation, def);
      infos.add(cpi);
    }
    
    return infos;
  }
  
  
  public ConnectionPoolDetailInformation listConnectionPoolStatistics(String name) {
    ConnectionPool pool = getConnectionPoolNoLazyCreation(name);
    if (pool == null) {
      return null;
    } else {
      int used = 0;
      com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation[] cis = pool.getConnectionStatistics();
      List<ConnectionInformation> connectionInformation = new ArrayList<ConnectionInformation>();
      for (com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation ci : cis) {
        if (ci.isInUse()) {
          used++;
        }
        connectionInformation.add(new ConnectionInformation(ci.isInUse(), ci.getAquiredLast(), ci.getLastCheck(), ci.getLastCommit(), ci.getLastRollback(), ci.getCntUsed(), ci.isLastCheckOk(), ci.getLastSQL(), ci.getCurrentThread(), ci.getStackTraceWhenThreadGotConnection()));
      }
      String pooltype;
      PoolDefinition def = registeredPoolDefinitions.get(name);
      if (def == null) {
        pooltype = "Custom";
      } else {
        pooltype = def.type;
      }
      return new ConnectionPoolDetailInformation(pool.getId(), pooltype, pool.getConnectionStatistics().length, pool.isDynamic(), used,
                                                 (pool.isClosed() ? "Shutdown" : "Running"), pool.toString(),
                                                 pool.getSQLStatistics(), pool.getWaitingThreadInformation(), def, connectionInformation);
    }
  }
  
  public ConnectionPool getConnectionPool(String name) throws NoConnectionAvailableException {
    return startConnectionPool(name, false, false);
  }
  
  
  // for PLs to retrieve creation data like schemas
  public PoolDefinition getConnectionPoolDefinition(String name) {
    return registeredPoolDefinitions.get(name);
  }
  
  
  private ConnectionPool getConnectionPoolNoLazyCreation(String name) {
    //TODO schöner cachen? dazu registeredPoolDefinitions aufbohren?
    for (ConnectionPool cp : ConnectionPool.getAllRegisteredConnectionPools()) {
      if (cp.getId().equals(name)) {
        return cp;
      }
    }
    return null;
  }
  
  
  private void storeConnectionPoolParams(ConnectionPoolParameter creationParams) {
    if (creationParams instanceof TypedConnectionPoolParameter) {
      PoolDefinition definition = new PoolDefinition();
      definition.setAllFieldsFromData((TypedConnectionPoolParameter)creationParams);
      registeredPoolDefinitions.put(definition.getName(), definition);
      try {
        ODS ods = ODSImpl.getInstance();
        ods.registerStorable(PoolDefinition.class);
        ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          con.persistObject(definition);
          con.commit();
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
  }

}

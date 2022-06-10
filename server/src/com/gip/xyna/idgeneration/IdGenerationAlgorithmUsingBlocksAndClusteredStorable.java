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

package com.gip.xyna.idgeneration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedLazyAlgorithmExecutionWrapper;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.CentralComponentConnectionCacheException;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xsched.Algorithm;


/**
 * Dieser Algorithmus verwendet zur Id-Vergabe Blöcke der Länge blockSize. 
 * Der jeweils letzte verwendete Block wird in der DB gespeichert, bevor die
 * erste Id aus dem Block vergeben wird. Dabei wird in GeneratedIDsStorable 
 * für den n.ten Block der Eintrag n*blockSize+1 gespeichert.
 * Im Cluster-Betrieb müssen die blockSize der beiden Knoten daher unbedingt
 * übereinstimmen, damit die Knoten einen belegten Block erkennen können.
 *
 */
public class IdGenerationAlgorithmUsingBlocksAndClusteredStorable implements IdGenerationAlgorithm {

  protected static Logger logger = CentralFactoryLogging
      .getLogger(IdGenerationAlgorithmUsingBlocksAndClusteredStorable.class);

  public static final String XYNA_CLUSTER_PREFIX = "XynaCluster";
  public static final String XYNA_FACTORY_STANDALONE_PREFIX = "XynaFactory";
  
  private final static String BLOCK_PREFETCHING_ALGORITHM_NAME = "IdGenPrefetcher";


  private final ConcurrentMap<String, IdInfo> ids = new ConcurrentHashMap<String, IdInfo>();
  private final Map<String, Long> blockSizes = new HashMap<String, Long>();
  private int ownBinding;
  boolean isClustered;
  private PreparedQuery<GeneratedIDsStorable> loadAllGeneratedIDsForUpdateQueryForRealm;
  private PreparedQuery<GeneratedIDsStorable> loadGeneratedIDQueryByRealmAndBinding;
  
  private PreFetchIdBlockExecutor executor = new PreFetchIdBlockExecutor();


  public void init() throws PersistenceLayerException {
    blockSizes.put(IDGenerator.REALM_DEFAULT, IDGenerator.getID_OFFSET());
    
    ownBinding = new GeneratedIDsStorable().getLocalBinding(ODSConnectionType.DEFAULT);
    isClustered = ownBinding != XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER;

    try {
      // for testcases
      try {
        CentralComponentConnectionCache.getConnectionFor(DedicatedConnection.IDGenerator);
      } catch (CentralComponentConnectionCacheException e) {
        // not open
        CentralComponentConnectionCache.getInstance().openCachedConnection(ODSConnectionType.DEFAULT,
                                                                           DedicatedConnection.IDGenerator,
                                                                           new StorableClassList(GeneratedIDsStorable.class));
      }
    } catch (XNWH_TooManyDedicatedConnections e) {
      throw new RuntimeException("Connection limit exceeded while trying to open dedicated connection for IdGenerator.", e);
    }
    
    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
      
      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        if (isClustered) {
          loadAllGeneratedIDsForUpdateQueryForRealm = con.prepareQuery(new Query<GeneratedIDsStorable>("select * from "
                          + GeneratedIDsStorable.TABLE_NAME + " where " + GeneratedIDsStorable.COL_REALM + " = ? for update", new GeneratedIDsStorable().getReader()));
          loadGeneratedIDQueryByRealmAndBinding =
              con.prepareQuery(new Query<GeneratedIDsStorable>("select * from " + GeneratedIDsStorable.TABLE_NAME + " where "
                  + GeneratedIDsStorable.COL_REALM + " = ? and " + GeneratedIDsStorable.COL_BINDING + " = ?",
                                                               new GeneratedIDsStorable().getReader()));
        }

        String realm = IDGenerator.REALM_DEFAULT;
        IdInfo idInfo = ids.get(realm);
        if (idInfo == null) {
          idInfo = new IdInfo(realm, getBlockSize(realm));
          ids.put(realm, idInfo);
        }
        
        idInfo.lazyInit(con);
        con.commit();
      }
      
    };

    WarehouseRetryExecutor.buildCriticalExecutor().
      connectionDedicated(DedicatedConnection.IDGenerator).
      storable(GeneratedIDsStorable.class).
      execute(wre);

    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.registerAlgorithm(executor);
    tm.startAlgorithm(executor.getName());
  }
  
  public void setBlockSize(String realm, long blockSize) {
    blockSizes.put(realm, blockSize);
    IdInfo idInfo = ids.get(realm);
    if (idInfo == null) {
      idInfo = new IdInfo(realm, blockSize);
      ids.putIfAbsent(realm, idInfo); //achtung, kann racecondition sein zusammen mit getUniqueId()
      executor.requestExecution();
    }
    //TODO else: anpassung bestehender idInfos
  }

  public void shutdown() throws PersistenceLayerException {
    
    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        for (Entry<String, IdInfo> idinfoEntry : ids.entrySet()) {
          IdInfo info = idinfoEntry.getValue();          
          long next = info.getAndSet(Integer.MIN_VALUE, 0);
          if (info.ownGeneratedIDsStorable != null) {
            info.ownGeneratedIDsStorable.setLastStoredId(next);
            info.ownGeneratedIDsStorable.setResultingFromShutdown(true);
            con.persistObject(info.ownGeneratedIDsStorable);
          } else {
            logger.info("idinfo not initialized: " + idinfoEntry.getKey());
          }
        }        
        con.commit();
      }
    };
    
    WarehouseRetryExecutor.buildCriticalExecutor().
      connectionDedicated(DedicatedConnection.IDGenerator).
      storable(GeneratedIDsStorable.class).
      execute(wre);

    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.stopAlgorithm(executor.getName());

    ODSConnection con = CentralComponentConnectionCache.getConnectionFor(DedicatedConnection.IDGenerator);
    if (con != null &&
        con.isOpen()) {
      con.closeConnection();
    } 
  }
  
  private class IdInfo extends IdDistributor {

    private volatile GeneratedIDsStorable ownGeneratedIDsStorable;
    private final AtomicLong nextPrefetchedId;
    private final String realm;
    
    private final AtomicLong idFromOtherNode;
    private final AtomicBoolean checkLastStoredFromOtherNode;
    private final AtomicLong lastCheckOtherNode; //ms seit 1970

    private IdInfo(String realm, long blockSize) {
      super(new IDSource() {
        
        @Override
        public long getNextBlockStart(IdDistributor iddistr, long time) {
          return executor.getAlgorithm().getNextIdBlockOrInit((IdInfo) iddistr, time);
        }
      }, blockSize);
      this.realm = realm;
      this.nextPrefetchedId = new AtomicLong(-1);
      idFromOtherNode = new AtomicLong(-1);
      checkLastStoredFromOtherNode = new AtomicBoolean(false);
      lastCheckOtherNode = new AtomicLong(0);
    }


    @Override
    public long getNext() {
      long l = super.getNext();
      if (l < 0) {
        throw new RuntimeException("Id Generator already shut down");
      }
      return l;
    }


    public void lazyInit(ODSConnection con) throws PersistenceLayerException {
      long maxLastStoredId = getMaxStoredIdAndSetOwnStorable(con);
      if (ownGeneratedIDsStorable == null) {
        // eigener Eintrag fehlt
        ownGeneratedIDsStorable = createGeneratedIDsStorable(ownBinding, realm);
        // Ermittlung der LastStoredId
        if (maxLastStoredId == -1L) {
          // bisher wurde nichts in die DB eingetragen -> komplette Initialisierung nötig
          long lastStoredId = isClustered ? createInitialLastStoredIdForCluster(con, realm) : 1L;
          ownGeneratedIDsStorable.setLastStoredId(lastStoredId);
        } else {
          // anderer Knoten hat bereits einen Eintrag hinterlassen
          long nextLastStoredId = calculateNextLastStoredId(maxLastStoredId);
          ownGeneratedIDsStorable.setLastStoredId(nextLastStoredId);
        }
      } else {
        // eigenen Eintrag wiedergefunden
        if (ownGeneratedIDsStorable.isResultingFromShutdown()
                        && ownGeneratedIDsStorable.getLastStoredId() % blockSize != 0) {
          // eigener Block ist bekanntermaßen nur teilweise gefüllt, daher weiter auffüllen
          long nextLastStoredId = ownGeneratedIDsStorable.getLastStoredId() + 1;
          ownGeneratedIDsStorable.setLastStoredId(nextLastStoredId);
        } else {
          // Block ist bereits gefüllt (bzw. zu unbekanntem Anteil teilgefüllt),
          // daher neuen Block beginnen
          long nextLastStoredId = calculateNextLastStoredId(maxLastStoredId);
          ownGeneratedIDsStorable.setLastStoredId(nextLastStoredId);
        }
      }

      ownGeneratedIDsStorable.setResultingFromShutdown(false);
      con.persistObject(ownGeneratedIDsStorable);

      long blockend = ownGeneratedIDsStorable.getLastStoredId() + blockSize - ownGeneratedIDsStorable.getLastStoredId() % blockSize;
      getAndSet(ownGeneratedIDsStorable.getLastStoredId(), blockend);
      if (logger.isDebugEnabled()) {
        logger.debug("initialized id gen realm " + realm);
      }
    }


    /**
     * Update auf GeneratedIDsStorable, da nächster Block benötigt wird
     * @param con
     * @return
     */
    private long updateLastStoredId(ODSConnection con) throws PersistenceLayerException {
      long maxLastStoredId = getMaxStoredIdAndSetOwnStorable(con);
      long nextLastStoredId = calculateNextLastStoredId(maxLastStoredId);
      ownGeneratedIDsStorable.setLastStoredId(nextLastStoredId);
      con.persistObject(ownGeneratedIDsStorable);
      return nextLastStoredId;
    }

    /**
     * Berechnung der nächsten LastStoredId (n*blockSize+1) > maxLastStoredId
     * @param maxLastStoredId
     * @return
     */
    private long calculateNextLastStoredId(long maxLastStoredId) {
      long n = maxLastStoredId / blockSize;
      long nextLastStoredId = (n+1) * blockSize + 1;
      return nextLastStoredId;
    }

    /**
     * Spezialbehandlung für die Initialisierung im Cluster-Modus, bei dem aufgrund  
     * fehlender Datenbankeinträge kein ForUpdate-Lock geholt werden konnte. 
     * Hier muss verhindert werden, dass der andere Knoten zeitgleich seinen ersten Eintrag 
     * macht, da sonst beide die gleichen Ids vergeben
     * @param con
     * @return neuen lastStoredId
     * @throws PersistenceLayerException
     */
    private long createInitialLastStoredIdForCluster(ODSConnection con, String realm) throws PersistenceLayerException {
      //Idee A) ownGeneratedIDsStorable.setLastStoredId( (ownBinding-1)*blockSize+ 1);
      //        verschwendet aber unter Umständen Ids (Anzahl (ownBinding-1)*blockSize)
      //nicht implementiert
      //Idee B) Commit und nochmal for Update lesen 
      //        Fälle: nur eigener Eintrag -> Ok
      //               fremde Einträge mit anderer LastStoredId -> Ok
      //               fremder Eintrag mit gleichem LastStoredId -> eigenen LastStoredId hochsetzen
      long lastStoredId = 1L; //Versuch mit erster vergebbarer Id
      ownGeneratedIDsStorable.setLastStoredId(lastStoredId);
      ownGeneratedIDsStorable.setResultingFromShutdown(false);
      con.persistObject(ownGeneratedIDsStorable);
      con.commit();
      List<GeneratedIDsStorable> gidss = con.query(loadAllGeneratedIDsForUpdateQueryForRealm, new Parameter(realm), -1);
      if (gidss.size() == 1 && 
          gidss.get(0).getBinding() == ownBinding) {
        //nur eigener Eintrag -> Ok
      } else {
        long maxLastStoredId = 1L;
        int count = 0; //haben andere Einträge den gleichen LastStoredId?
        for (GeneratedIDsStorable gids : gidss) {
          if (gids.getBinding() == ownBinding) {
            ownGeneratedIDsStorable = gids;
          } else {
            if (gids.getLastStoredId() == 1L) {
              ++count;
            }
            if (gids.getLastStoredId() > maxLastStoredId) {
              maxLastStoredId = gids.getLastStoredId();
            }
          }
        }
        if( count == 0 ) {
          //alle anderen Einträge haben einen anderen Block gewählt -> OK
        } else {
          //neuen Block beginnen
          lastStoredId = calculateNextLastStoredId(maxLastStoredId);
        }
      }
      return lastStoredId;
    }

    
    /**
     * Lesen aller GeneratedIDs und auswerten: Maximale LastStoredId wird zurückgeben,
     * GeneratedIDsStorable zum eigenen Binding wird als Instanz-Attribut gespeichert 
     * locken der storables in der datenbank.
     */
    private long getMaxStoredIdAndSetOwnStorable(ODSConnection con) throws PersistenceLayerException {
      long maxLastStoredId = -1L;

      if( isClustered ) {
        //Lesen aller GeneratedIDs. durch das forupdate sind sie für andere knoten bis zum ende der transaktion gesperrt.
        List<GeneratedIDsStorable> gidss = con.query(loadAllGeneratedIDsForUpdateQueryForRealm, new Parameter(realm), -1);

        for( GeneratedIDsStorable gids : gidss ) {
          if( gids.getLastStoredId() > maxLastStoredId ) {
            maxLastStoredId = gids.getLastStoredId();
          }
          if( gids.getBinding() == ownBinding ) {
            ownGeneratedIDsStorable = gids;
          } 
        }
      } else {
        GeneratedIDsStorable gis = createGeneratedIDsStorable(ownBinding, realm);
        try {
          con.queryOneRow(gis);
          ownGeneratedIDsStorable = gis;
          maxLastStoredId = ownGeneratedIDsStorable.getLastStoredId();
        }
        catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //Eintrag existiert nicht, dies ist kein Fehler, die Rückgabe berücksichtigt dies
        }
      }
      return maxLastStoredId;
    }


    private GeneratedIDsStorable createGeneratedIDsStorable(int binding, String realm) {
      return new GeneratedIDsStorable(getInstanceIdentifier(binding, realm), realm, binding);
    }

    private String getInstanceIdentifier(int binding, String realm) {
      if (binding == XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER) {
        if (realm.equals(IDGenerator.REALM_DEFAULT)) {
          return XYNA_FACTORY_STANDALONE_PREFIX;
        } else {
          return realm;
        }
      } else {
        if (realm.equals(IDGenerator.REALM_DEFAULT)) {
          return new StringBuilder().append(XYNA_CLUSTER_PREFIX).append("_").append(binding).toString();
        } else {
          return realm + "_" + binding;
        }
      }
    }

    public String toString() {
      return realm + "-" + getCurrent();
    }

  }
  
  public static enum IDGENERATION_RESTART_OPTIONS {
    NONE, INFO
  }

  public final static StringParameter<IDGENERATION_RESTART_OPTIONS> actionParameter = StringParameter.typeEnum(IDGENERATION_RESTART_OPTIONS.class, "action", true).build();
  
  private class PreFetchIdBlockExecutor extends ManagedLazyAlgorithmExecutionWrapper<PreFetchIdBlockAlgorithm> {

    public PreFetchIdBlockExecutor() {
      super(BLOCK_PREFETCHING_ALGORITHM_NAME,
            new PreFetchIdBlockAlgorithm(),
            Collections.singletonList(actionParameter),
            Optional.empty(),
            30000);
    }
    
    public void requestExecution() {
      executor.requestExecution();
    }

    public void awaitExecution(int timeout, TimeUnit unit) throws InterruptedException {
      executor.awaitExecution(timeout, unit);
    }
    
    public PreFetchIdBlockAlgorithm getAlgorithm() {
      return algorithm;
    }
    
    @Override
    public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
      if (parameter.containsKey(actionParameter.getName())) {
        StringBuilder sb = new StringBuilder();
        for (final IdInfo idInfo : ids.values()) {
          sb.append(idInfo.realm).append(Constants.LINE_SEPARATOR)
            .append("nextPrefetcherId: ").append(idInfo.nextPrefetchedId).append(Constants.LINE_SEPARATOR)
            .append("block.current: ").append(idInfo.getCurrent()).append(Constants.LINE_SEPARATOR)
            .append("block.blockEnd: ").append(idInfo.getCurrentBlockEnd()).append(Constants.LINE_SEPARATOR).append(Constants.LINE_SEPARATOR);
        }
        try {
          statusOutputStream.write(sb.toString().getBytes(Constants.DEFAULT_ENCODING));
        } catch (IOException e) {
          logger.debug("Error writing status output: " + Constants.LINE_SEPARATOR + sb.toString(),e);
        }
      }
      return super.start(parameter, statusOutputStream);  
    }
    
    public StackTraceElement[] getCurrentStackTraceOfUnderlyingThread() {
      return executor.getThreadState();
    }
    
  }

  private class PreFetchIdBlockAlgorithm implements Algorithm {
    
    //members synchronized über this
    private Exception lastPrefetcherException;
    private long lastPrefetcherExceptionTimestamp;
    private long lastSuccessTimestamp;
    private int numberOfExceptionsSinceLastSuccess;
    private long currentPrefetchStartTimestamp;
    private long lastDurationMS;

    public void exec() {
      for (final IdInfo idInfo : ids.values()) {
        
        if (idInfo.nextPrefetchedId.get() == -1) {
          synchronized (this) {
            currentPrefetchStartTimestamp = System.currentTimeMillis();
          }
          //nextIdCounter ist nun invalid, weil die letzte zahl des blocks erreicht wurde => nächsten block finden

          WarehouseRetryExecutableNoException<Long> wre = new WarehouseRetryExecutableNoException<Long>() {

            public Long executeAndCommit(ODSConnection con) throws PersistenceLayerException {

              if (idInfo.ownGeneratedIDsStorable == null) {
                idInfo.lazyInit(con);
              }
              Long updatedValue = idInfo.updateLastStoredId(con);
              con.commit();
              return updatedValue;
            }
          };

          try {
            long nextId =
                WarehouseRetryExecutor.buildCriticalExecutor().connectionDedicated(DedicatedConnection.IDGenerator)
                    .storable(GeneratedIDsStorable.class).execute(wre);
            idInfo.nextPrefetchedId.set(nextId);
            if (logger.isDebugEnabled()) {
              logger.debug("Next id block has been successfully prefetched for realm <" + idInfo.realm + "> (id=" + nextId + ")");
            }
            synchronized (this) {
              long t = System.currentTimeMillis();
              lastPrefetcherException = null;
              lastDurationMS = t - currentPrefetchStartTimestamp;
              numberOfExceptionsSinceLastSuccess = 0;
              lastSuccessTimestamp = t;
            }
          } catch (PersistenceLayerException e) {
            synchronized (this) {
              long t = System.currentTimeMillis();
              lastPrefetcherException = e;
              lastDurationMS = t - currentPrefetchStartTimestamp;
              numberOfExceptionsSinceLastSuccess++;
              lastPrefetcherExceptionTimestamp = t;
            }
            logger.warn("Failed to store id generation information to persistencelayer. Took " + lastDurationMS + " ms. Last success was "
                + Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(lastSuccessTimestamp)) + ". "
                + numberOfExceptionsSinceLastSuccess + " exceptions since then.", e);
            try {
              //nicht direkt erneut probieren
              Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }
          } catch (RuntimeException e) {
            synchronized (this) {
              long t = System.currentTimeMillis();
              lastPrefetcherException = e;
              lastDurationMS = t - currentPrefetchStartTimestamp;
              numberOfExceptionsSinceLastSuccess++;
              lastPrefetcherExceptionTimestamp = t;
            }
            logger.warn("Unexpected failure in IdBlockPrefetcher Thread. Took " + lastDurationMS + " ms. Last success was "
                + Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(lastSuccessTimestamp)) + ". "
                + numberOfExceptionsSinceLastSuccess + " exceptions since then.", e);
            try {
              //nicht direkt erneut probieren
              Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }
          }
        } 
        
        if (isClustered && 
            idInfo.checkLastStoredFromOtherNode.get() && 
            System.currentTimeMillis() - idInfo.lastCheckOtherNode.get() > 5000) {
          WarehouseRetryExecutableNoException<Long> wre = new WarehouseRetryExecutableNoException<Long>() {

            public Long executeAndCommit(ODSConnection con) throws PersistenceLayerException {
              List<GeneratedIDsStorable> gidss =
                  con.query(loadGeneratedIDQueryByRealmAndBinding, new Parameter(idInfo.realm, 3 - ownBinding), 1);
              for (GeneratedIDsStorable gis : gidss) {
                idInfo.idFromOtherNode.set(gis.getLastStoredId());
                idInfo.lastCheckOtherNode.set(System.currentTimeMillis());                
              }
              con.rollback(); //for update
              return 0L;
            }
          };
          try {
            WarehouseRetryExecutor.buildCriticalExecutor().connectionDedicated(DedicatedConnection.IDGenerator)
                .storable(GeneratedIDsStorable.class).execute(wre);
          } catch (PersistenceLayerException e) {
          } catch (RuntimeException e) {
          }
        }
      }
    }


    public long getNextIdBlockOrInit(IdInfo info, long startTime) {
      /*
       * nextprefetchedid ist anfangs -1 und wird dann auf einen anderen wert gesetzt, asynchron von prefetcher-thread
       */
      long r = -1;
      long s = 0;
      SleepCounter sleepCnt = null;
      boolean initMissing = false;
      while (r == -1) {
        r = info.nextPrefetchedId.get();
        if (r == -1) {
          //prefetcher noch nicht gelaufen. also warten, bis er soweit ist

          if (info.ownGeneratedIDsStorable == null) {
            initMissing = true;
          }
          if (s == 0) {
            sleepCnt = new SleepCounter(20, 1000, 2, TimeUnit.MILLISECONDS, true);

            if (logger.isInfoEnabled()) {
              if (initMissing) {
                logger.info("Initializing Id Generator for realm <" + info.realm + "> ...");
              } else {
                logger.info("Current IdBlock exhausted for realm <" + info.realm
                  + ">. IdBlockPrefetcher didn't provide new id block yet. waiting ...");
              }
            }
            s = startTime;
          } else if (System.currentTimeMillis() - s > idblockprefetchertimeout.getMillis()) {
            SimpleDateFormat format = Constants.defaultUTCSimpleDateFormatWithMS();
            String msg = "Could not aquire unique id from IdBlockPrefetcher after " + idblockprefetchertimeout.getMillis() + " ms.";
            synchronized (this) {
              if (lastPrefetcherException != null) {
                throw new RuntimeException(msg + " Prefetcher thread failed "
                    + (System.currentTimeMillis() - lastPrefetcherExceptionTimestamp) + " ms ago at "
                    + format.format(new Date(lastPrefetcherExceptionTimestamp)) + ", taking " + lastDurationMS
                    + " ms to run. It was last successful at " + format.format(new Date(lastSuccessTimestamp)) + " and produced "
                    + numberOfExceptionsSinceLastSuccess + " exceptions since then. Last exception below:", lastPrefetcherException);
              } else {
                Exception executorThreadState = new Exception("State of prefetcher thread");
                executorThreadState.setStackTrace(IdGenerationAlgorithmUsingBlocksAndClusteredStorable.this.executor
                    .getCurrentStackTraceOfUnderlyingThread());
                throw new RuntimeException(msg + " Prefetcher thread may be hanging (no exception). Last prefetching try started "
                    + (System.currentTimeMillis() - currentPrefetchStartTimestamp) + " ms ago at "
                    + format.format(new Date(currentPrefetchStartTimestamp)) + ". It was last successful at "
                    + format.format(new Date(lastSuccessTimestamp)) + ", taking " + lastDurationMS
                    + " ms to run. Current thread state below:", executorThreadState);
              }
            }
          }
          executor.requestExecution();
          try {
            sleepCnt.sleep();
          } catch (InterruptedException e) {
          }
        } else if (r < info.getCurrentBlockEnd()) {
          logger.warn("Prefetched value lower than previous block end: " + info.realm + " - previous block end:" + info.getCurrentBlockEnd() + ", prefetched:" + r);
          if (retryIfPrefetchedValueSeemsWrong.get()) {
            logger.info("Retrying prefetch.");
            info.nextPrefetchedId.set(-1);
            r = -1;
          }
        }
      }

      if (initMissing) {
        //lazyinit noch nicht gelaufen. dann muss der aktuelle block ermittelt werden, nicht der nächste
        r = info.getCurrent();
      } else {
        //nächsten block zurückgeben und den prefetcher asynchron den übernächsten block bestimmen lassen
        info.nextPrefetchedId.set(-1);
        executor.requestExecution();
      }
      if (logger.isDebugEnabled()) {
        logger.debug(info.realm + ": returning prefetched id = " + r + ", current id = " + info.getCurrent());
      }
      return r;
    }


  }


  private final static XynaPropertyBoolean retryIfPrefetchedValueSeemsWrong =
      new XynaPropertyBoolean("xyna.idgen.prefetcher.nextvaluelower.retry", true)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "If by some inconsistency the idgenerator wants to decrease ids, this can be disabled by setting this property to true (default).")
          .setHidden(true);

  private final static XynaPropertyDuration idblockprefetchertimeout = new XynaPropertyDuration("xyna.idgen.prefetcher.timeout",
                                                                                  new Duration(30, TimeUnit.SECONDS))
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Maximum time the application waits for the Id Generation Prefetcher Thread to produce the next Id Block.");


  private long getBlockSize(String realm) {
    Long s = blockSizes.get(realm);
    if (s == null) {
      return 10000;
    }
    return s;
  }
  
  /*private static class Key {
    
    private static final int MAX = 6;
    private final StackTraceElement[] stes;

    public Key() {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      if (stackTrace.length < MAX) {
        stes = stackTrace;
      } else {
        stes = new StackTraceElement[MAX];
        System.arraycopy(stackTrace, 0, stes, 0, MAX);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(stes);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Key other = (Key) obj;
      if (!Arrays.equals(stes, other.stes))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "Key [stes=" + Arrays.toString(stes) + "]";
    }
    
  }*/
  
  public long getUniqueId(String realm) {
    IdInfo idInfo = getIdInfo(realm);
    long result = idInfo.getNext();
    return result;
  }
 

  private IdInfo getIdInfo(String realm) {
    IdInfo idInfo = ids.get(realm);
    if (idInfo == null) {
      idInfo = new IdInfo(realm, getBlockSize(realm));
      IdInfo previousIdInfo = ids.putIfAbsent(realm, idInfo);
      if (previousIdInfo != null) {
        idInfo = previousIdInfo;
      }
    }
    return idInfo;
  }

  @Override
  public String toString() {
    return "IdGenerationAlgorithm(" + ownBinding + ", " + ids + ")";
  }

  @Override
  public long getIdLastUsedByOtherNode(String realm) {
    if (!isClustered) {
      throw new RuntimeException();
    }
    IdInfo idInfo = getIdInfo(realm);
    long id = idInfo.idFromOtherNode.get();
    if (id == -1) {
      idInfo.checkLastStoredFromOtherNode.set(true); //bitte regelmässig updaten
      executor.requestExecution();
      while (id == -1) {
        try {
          executor.awaitExecution(600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        id = idInfo.idFromOtherNode.get();
      }
    }
    return id;
  }
  
  private final Object storeLastUsedLock = new Object(); 


  @Override
  public void storeLastUsed(String realm) {
    if (isClustered) { //TODO lieber aussen isClustered abfragen
      synchronized (storeLastUsedLock) { //wegen select for update eh nicht parallelisierbar
        try {
          IdInfo idInfo = getIdInfo(realm);
          ODSConnection con = ODSImpl.getInstance().openConnection();
          try {
            List<GeneratedIDsStorable> result = con.query(loadAllGeneratedIDsForUpdateQueryForRealm, new Parameter(realm), -1);
            for (GeneratedIDsStorable gis : result) {
              if (gis.getBinding() == ownBinding) {
                gis.setLastStoredId(idInfo.getCurrent());
                gis.setResultingFromShutdown(false);
                con.persistObject(gis);
                break;
              }
            }
            con.commit();
          } finally {
            con.closeConnection();
          }
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("Could not store current id for realm " + realm, e);
        }
      }
    }
  }


}

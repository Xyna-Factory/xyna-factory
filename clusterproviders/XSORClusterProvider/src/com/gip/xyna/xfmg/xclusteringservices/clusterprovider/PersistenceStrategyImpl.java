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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.BatchedQueueProcessor.ProcessingAction;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.BatchedQueueProcessor.ProcessingStrategy;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.BatchedQueueProcessor.ProcessingType;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xsor.persistence.PersistenceException;
import com.gip.xyna.xsor.persistence.PersistenceStrategy;
import com.gip.xyna.xsor.protocol.XSORPayload;



public class PersistenceStrategyImpl implements PersistenceStrategy, ProcessingStrategy, StatisticsReporterLegacy {

  private static final Logger logger = CentralFactoryLogging.getLogger(PersistenceStrategyImpl.class);

  private final ODSConnectionType connectionType;
  private final ODS ods;

  private final BatchedQueueProcessor<PersistenceRequest, PersistenceException> queueProcessor;
  private PreparedCommand deleteAllFromTable;

  //processingstrategy
  private long lastPersist;
  private long persistenceIntervalMs;
  private final ProcessingType processingType;

  //statistics
  private int sizeOfPBufferInRunningJob;
  private PreparedQuery<ClusterInfoStorable> queryClusterInfo;
  private AtomicLong overallCounter = new AtomicLong(0);

  private interface PersistenceRequest {

    public void persist(ODSConnection con) throws PersistenceLayerException;

    public boolean isDelete();

    /**
     * map tableName -> liste von objekten 
     */
    public void addStorablesToPersist(Map<String, List<Storable<?>>> storablesToPersist);
  }


  public PersistenceStrategyImpl(ODSConnectionType connectionType, final int maxBatchSize, long persistenceIntervalMs,
                                 boolean synchronousPersistence) throws PersistenceLayerException {
    this.connectionType = connectionType; //TODO evtl muss man das pro storable anders konfigurieren?!
    ods = ODSImpl.getInstance();
    ods.registerStorable(ClusterInfoStorable.class);

    this.persistenceIntervalMs = persistenceIntervalMs;
    if (synchronousPersistence) {
      processingType = ProcessingType.DO_PROCESS_SYNC;
    } else {
      processingType = ProcessingType.DO_PROCESS_ASYNC;
    }
    ProcessingAction<PersistenceRequest, PersistenceException> action =
        new ProcessingAction<PersistenceRequest, PersistenceException>() {

          Map<String, List<Storable<?>>> storablesToPersist = new HashMap<String, List<Storable<?>>>();

          public PersistenceException exec(List<PersistenceRequest> requests) {
            int size = requests.size();
            try {
              ODSConnection con = ods.openConnection(PersistenceStrategyImpl.this.connectionType);
              try {
                int i = 0;
                storablesToPersist.clear();
                for (PersistenceRequest pr : requests) {
                  if (pr.isDelete()) {
                    //erst alles angesammelte persistieren
                    checkPersistCollection(con);
                    pr.persist(con);
                  } else {
                    pr.addStorablesToPersist(storablesToPersist);
                  }

                  overallCounter.incrementAndGet();
                  i++;
                  sizeOfPBufferInRunningJob = size - i;

                  if (i % maxBatchSize == 0) {
                    checkPersistCollection(con);
                    con.commit();
                  }
                }
                checkPersistCollection(con);
                con.commit();
              } finally {
                con.closeConnection();
              }
            } catch (PersistenceLayerException e) {
              return new PersistenceException("could not commit actions", e);
            }
            return null;
          }


          private void checkPersistCollection(ODSConnection con) throws PersistenceLayerException {
            if (storablesToPersist.size() > 0) {
              for (Entry<String, List<Storable<?>>> entry : storablesToPersist.entrySet()) {
                con.persistCollection(entry.getValue());
              }
              storablesToPersist.clear();
            }
          }


        };
    queueProcessor = new BatchedQueueProcessor<PersistenceRequest, PersistenceException>(action, this);


    XynaFactory.getInstance().getFutureExecution()
        .execAsync(new FutureExecutionTask(XynaFactory.getInstance().getFutureExecution().nextId()) {

          @Override
          public void execute() {
            XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().registerNewStatistic("XSORPersistenceBuffer", PersistenceStrategyImpl.this);
          }


          @Override
          public int[] after() {
            return new int[] {XynaStatistics.FUTUREEXECUTION_ID};
          }

        });
  }

  public BatchedQueueProcessor<PersistenceRequest, PersistenceException> getQueueProcessor() {
    return queueProcessor;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Class<Storable> getStorableClass(Class<?> clazz) {
    if (!Storable.class.isAssignableFrom(clazz)) {
      throw new RuntimeException("invalid class: class is not a storable: " + clazz.getName());
    }
    return (Class<Storable>) clazz;
  }


  private static class StorableWrapper {

    private Storable s;
    private String pkAsString;
  }


  @SuppressWarnings("unchecked")
  public Iterator<XSORPayloadPersistenceBean> loadObjects(String tableName, Class<?> clazz) throws PersistenceException {
    if (logger.isInfoEnabled()) {
      logger.info("loading " + tableName
          + " storables from configured backingstore for xyna coherence (connectiontype = " + connectionType + ").");
    }

    @SuppressWarnings("rawtypes")
    Class<Storable> clazzStorable = getStorableClass(clazz);

    try {
      //registerstorable wird damit ggfs rekursiv aufgerufen. ist in odsimpl generisch behandelt.
      //funktioniert, solange keine zyklen entstehen.
      ods.registerStorable(clazzStorable);
    } catch (PersistenceLayerException e) {
      throw new PersistenceException("storable " + tableName + " could not be registered in ods.", e);
    }

    ODSConnection con = ods.openConnection(connectionType);
    try {
      try {
        String storableTableName = Storable.getPersistable(clazzStorable).tableName();
        final ResultSetReader<Storable> reader;
        try {
          reader = clazzStorable.newInstance().getReader();
        } catch (InstantiationException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        List<XSORPayloadPersistenceBean> ret = new ArrayList<PersistenceStrategy.XSORPayloadPersistenceBean>();

        //alle objekte laden, und beide listen parallel durchlaufen. dazu sortieren
        PreparedQuery<StorableWrapper> queryStorableTable =
            con.prepareQuery(new Query<StorableWrapper>("select * from " + storableTableName,
                                                        new ResultSetReader<StorableWrapper>() {

                                                          @Override
                                                          public StorableWrapper read(ResultSet rs) throws SQLException {
                                                            StorableWrapper w = new StorableWrapper();
                                                            Storable s = reader.read(rs);
                                                            w.s = s;
                                                            w.pkAsString =
                                                                ClusterInfoStorable.getPkForStorablePk(s);
                                                            return w;
                                                          }

                                                        }));
        Collection<StorableWrapper> storables = con.query(queryStorableTable, new Parameter(), -1);
        if (queryClusterInfo == null) {
          queryClusterInfo =
              con.prepareQuery(new Query<ClusterInfoStorable>("select * from " + ClusterInfoStorable.TABLE_NAME
                  + " where " + ClusterInfoStorable.COL_PK + " LIKE ?", ClusterInfoStorable.reader));
        }
        Collection<ClusterInfoStorable> clusterInfos =
            con.query(queryClusterInfo, new Parameter(storableTableName + "!%"), -1);

        //sortieren
        List<StorableWrapper> storablesList = new ArrayList<StorableWrapper>(storables);
        storables = null;
        Collections.sort(storablesList, new Comparator<StorableWrapper>() {

          @Override
          public int compare(StorableWrapper o1, StorableWrapper o2) {
            return o1.pkAsString.compareTo(o2.pkAsString);
          }

        });
        List<ClusterInfoStorable> clusterInfosList = new ArrayList<ClusterInfoStorable>(clusterInfos);
        clusterInfos = null;
        Collections.sort(clusterInfosList, new Comparator<ClusterInfoStorable>() {

          @Override
          public int compare(ClusterInfoStorable o1, ClusterInfoStorable o2) {
            return o1.getPk().compareTo(o2.getPk());
          }

        });

        Iterator<StorableWrapper> storableIt = storablesList.iterator();
        Iterator<ClusterInfoStorable> ciIt = clusterInfosList.iterator();
        ClusterInfoStorable nextClusterInfo = null;
        while (storableIt.hasNext()) {
          StorableWrapper s = storableIt.next();

          //ermittle nächstens clusterInfoStorable
          if (nextClusterInfo == null) {
            if (ciIt.hasNext()) {
              nextClusterInfo = ciIt.next();
            }
          }

          //gehe solange weiter, bis es größergleich dem storable ist.
          while (nextClusterInfo != null && nextClusterInfo.getPk().compareTo(s.pkAsString) < 0) {
            if (ciIt.hasNext()) {
              //d.h. das jetzige wird übersprungen
              logger.warn("Found unnecessary entry in " + ClusterInfoStorable.TABLE_NAME + ": "
                  + nextClusterInfo.getPk());
              nextClusterInfo = ciIt.next();
            } else {
              break;
            }
          }

          //falls gleich, verwenden und nullen
          if (nextClusterInfo != null && nextClusterInfo.getPk().compareTo(s.pkAsString) == 0) {
            ret.add(new XSORPayloadPersistenceBean((XSORPayload) s.s, nextClusterInfo.getReleaseTime(), nextClusterInfo
                .getModificationTime()));
            nextClusterInfo = null; //verwendet
          } else {
            //ansonsten warnen
            logger.warn("Releasetime and modificationtime have been set to 0 for object " + s.pkAsString
                + " because the correlated object could not be read in table " + ClusterInfoStorable.TABLE_NAME + ".");
            ret.add(new XSORPayloadPersistenceBean((XSORPayload) s.s, 0, 0));
          }

        }

        return (Iterator<XSORPayloadPersistenceBean>) ret.iterator();
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new PersistenceException("error loading " + tableName
          + " storables from configured backingstore for xyna coherence (connectiontype = " + connectionType + ").", e);
    }
  }


  public void persistRemainingData() throws PersistenceException {
    queueProcessor.persistRemainingDataSynchronously();
  }


  public ProcessingType decideProcessing() {
    //zeitbasiert
    long current = System.currentTimeMillis();
    if (current - lastPersist > persistenceIntervalMs) {
      lastPersist = current;
      return processingType;
    }
    return ProcessingType.DO_NOT_PROCESS;
  }


  public void onProcessingIsStillRunning(int sizeOfBufferInRunningJob) {
    if (sizeOfBufferInRunningJob >= 0) {
      sizeOfPBufferInRunningJob = sizeOfBufferInRunningJob;
    }
  }
  
  
  public StatisticsReportEntryLegacy[] getStatisticsReportLegacy() {
    StatisticsReportEntryLegacy[] report = new StatisticsReportEntryLegacy[2];
    report[0] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return (long) sizeOfPBufferInRunningJob;
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Size of currently processing xsor persistence buffer";
      }
    };

    report[1] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return queueProcessor.getCurrentlyWaitingRequests();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Size of remaining xsor persistence buffer";
      }
    };

    return report;
  }


  @Override
  public void createObject(XSORPayload payload, long releaseTime, long modificationTime) throws PersistenceException {
    updateObject(payload, releaseTime, modificationTime);
  }


  @Override
  public void updateObject(final XSORPayload payload, final long releaseTime, final long modificationTime)
      throws PersistenceException {
    if (!(payload instanceof Storable)) {
      throw new RuntimeException("invalid object: " + payload);
    }
    queueProcessor.add(new PersistenceRequest() {

      @SuppressWarnings("rawtypes")
      public void persist(ODSConnection con) throws PersistenceLayerException {
          throw new RuntimeException("unsupported");
      }

      @Override
      public boolean isDelete() {
        return false;
      }

      @Override
      public void addStorablesToPersist(Map<String, List<Storable<?>>> storablesToPersist) {
        Storable s = (Storable) payload;
        String tableName = s.getTableName();
        putInTable(storablesToPersist, tableName, s);
        putInTable(storablesToPersist, ClusterInfoStorable.TABLE_NAME, new ClusterInfoStorable(s, releaseTime,
                                                                                               modificationTime));
      }


      private void putInTable(Map<String, List<Storable<?>>> storablesToPersist, String tableName, Storable<?> s) {
        List<Storable<?>> l = storablesToPersist.get(tableName);
        if (l == null) {
          l = new ArrayList<Storable<?>>();
          storablesToPersist.put(tableName, l);
        }
        l.add(s);
      }
    });
  }


  @Override
  public void deleteObject(final XSORPayload payload) throws PersistenceException {
    if (!(payload instanceof Storable)) {
      throw new RuntimeException("invalid object: " + payload);
    }
    queueProcessor.add(new PersistenceRequest() {

      @SuppressWarnings("rawtypes")
      public void persist(ODSConnection con) throws PersistenceLayerException {
        Storable s = (Storable) payload;
        con.deleteOneRow(s);
        con.deleteOneRow(new ClusterInfoStorable(s, 0, 0));
      }

      @Override
      public boolean isDelete() {
        return true;
      }

      @Override
      public void addStorablesToPersist(Map<String, List<Storable<?>>> storablesToPersist) {
        throw new RuntimeException("unsupported");
      }

    });
  }


  @Override
  public void clearAllData(String tableName, Class<? extends XSORPayload> clazz) throws PersistenceException {
    @SuppressWarnings("rawtypes")
    Class<Storable> clazzStorable = getStorableClass(clazz);

    //TODO eigtl will man die clearen, aber dann müsste man genaugenommen unterscheiden,
    //zu welchem table die einzelnen elemente des queueprocessors gehören...
    queueProcessor.persistRemainingDataSynchronously();

    ODSConnection con = ods.openConnection(connectionType);
    try {
      try {
        if (deleteAllFromTable == null) {
          deleteAllFromTable =
              con.prepareCommand(new Command("delete from " + ClusterInfoStorable.TABLE_NAME + " where "
                  + ClusterInfoStorable.COL_PK + " LIKE ?"));
        }
        con.deleteAll(clazzStorable);
        con.executeDML(deleteAllFromTable, new Parameter(Storable.getPersistable(clazzStorable).tableName() + "!%"));
        con.commit();
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new PersistenceException("error deleting " + tableName
          + " storables from configured backingstore for xyna coherence (connectiontype = " + connectionType + ").", e);
    }
  }

  public long getOverallCounter() {
    return overallCounter.get();
  }

  public long getRequestsCurrentlyWorkedOn() {
    return sizeOfPBufferInRunningJob;
  }

}

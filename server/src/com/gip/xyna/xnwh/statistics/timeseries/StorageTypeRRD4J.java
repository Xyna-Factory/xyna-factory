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
package com.gip.xyna.xnwh.statistics.timeseries;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.Archive;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;



/*
 *    step(size)
 *   <------->
 * PDP1    PDP2    PDP3    PDP4    PDP5
 *   o       o       o       o       o
 * 
 * --><--------------><-------------->
 *       bin-l�nge          bin2
 *      =CDP-l�nge
 *      =steps*stepsize
 * 
 * -------------------------------------------...----->
 *          =rows*steps*stepsize
 *          
 *          
 *          
 *          <--------------------->
 *              heartbeat
 *             => welche PDPs werden als unknown gespeichert?
 *                falls diff(pdp1, pdp2) > heartbeat -> alle pdps dazwischen gelten als unknown
 *                ansonsten bekommen die pdps dazwischen den gleichen wert zugewiesen
 *
 */
public class StorageTypeRRD4J implements StorageType {

  public static final Logger logger = CentralFactoryLogging.getLogger(StorageTypeRRD4J.class);


  public static interface RRD4JConfiguration {

    public int getHeartBeatAsFactorOfStepSize();


    public String getPathPrefixForRRDDBs();


    public String createNewUniqueId();


    public long getCacheExpirationTime();


    public long getCacheSyncInterval();


    public int getMaximumNumberOfOpenFiles();
  }


  public static class StorageParameterRRD4J implements StorageParameter {

    private final AggregationType aggregation;
    private final long lengthInSeconds;
    private final long bucketSizeInSeconds;


    public StorageParameterRRD4J(AggregationType aggregation, long lengthInSeconds, long bucketSizeInSeconds) {
      this.aggregation = aggregation;
      this.lengthInSeconds = lengthInSeconds;
      this.bucketSizeInSeconds = bucketSizeInSeconds;
    }


    public AggregationType getAggregation() {
      return aggregation;
    }


    public long getLengthInSeconds() {
      return lengthInSeconds;
    }


    public long getBucketSizeInSeconds() {
      return bucketSizeInSeconds;
    }


    public String toString() {
      return "agg=" + aggregation.name() + ", len=" + lengthInSeconds + ", prec=" + bucketSizeInSeconds;
    }

  }

  //cacheentry f�r rrddb, weil das andauernde �ffnen und schliessen teuer ist.
  private static class RrdDbWrapper {

    private final long syncInterval;
    protected final String id;

    private RrdDb rrddb;
    private long lastaccess;
    private long lastsync = System.currentTimeMillis();
    private boolean changed = false;


    private RrdDbWrapper(String id, RrdDb rrddb, long syncInterval) {
      this.rrddb = rrddb;
      this.syncInterval = syncInterval;
      this.id = id;
    }


    public synchronized void createSampleAndUpdate(long asRoundedSecond, double doubleValue) throws IOException {
      Sample sample = rrddb.createSample(asRoundedSecond);
      sample.setValue(0, doubleValue);
      sample.update();
      changed = true;
    }


    public synchronized void cleanup() {
      try {
        rrddb.close();
        changed = false;
      } catch (IOException e) {
        logger.warn("Could not close cached Rrddb.", e);
      }
    }


    public synchronized FetchRequest createFetchRequest(ConsolFun consolidationFunction, long startS, long endS, long resolutionS) {
      return rrddb.createFetchRequest(consolidationFunction, startS, endS, resolutionS);
    }


    //daten in file schreiben
    public synchronized boolean lazysync(long t) throws IOException {
      if (changed && t - lastsync > syncInterval) {
        rrddb.close();
        changed = false;
        lastsync = t;
        rrddb = new RrdDb(rrddb.getPath());
        return true;
      }
      return false;
    }


    public void update() {
      lastaccess = System.currentTimeMillis();
    }


    public synchronized RrdDef getRrdDef() throws IOException {
      return rrddb.getRrdDef();
    }


    public synchronized int getArcCount() {
      return rrddb.getArcCount();
    }


    public synchronized Archive getArchive(int arcIndex) {
      return rrddb.getArchive(arcIndex);
    }


    public synchronized long getLastUpdateTime() throws IOException {
      return rrddb.getLastUpdateTime();
    }


    public boolean shouldBeRemovedFromCache(long currentTime, long maxage) {
      return currentTime - lastaccess > maxage;
    }


  }

  private static class LRUCacheWithTimeExpiration extends LinkedHashMap<String, RrdDbWrapper> {

    private static final long serialVersionUID = 1L;
    private final long maxage;
    private final int maxEntries;
    private final List<RrdDbWrapper> cleanupList;


    public LRUCacheWithTimeExpiration(long maxage, int maxEntries, List<RrdDbWrapper> cleanupList) {
      this.maxage = maxage;
      this.maxEntries = maxEntries;
      this.cleanupList = cleanupList;
    }


    @Override
    protected boolean removeEldestEntry(Entry<String, RrdDbWrapper> eldest) {
      boolean remove = size() > maxEntries || System.currentTimeMillis() - eldest.getValue().lastaccess > maxage;
      if (remove) {
        synchronized (cleanupList) {
          cleanupList.add(eldest.getValue());
        }
      }
      return remove;
    }

  }


  private final RRD4JConfiguration conf;
  private final LRUCacheWithTimeExpiration cache;
  private final List<RrdDbWrapper> cleanupList;
  private final Thread syncthread;
  private volatile boolean syncthreadActive = true;


  public StorageTypeRRD4J(final RRD4JConfiguration conf) {
    this.conf = conf;
    this.cleanupList = new ArrayList<>();
    cache = new LRUCacheWithTimeExpiration(conf.getCacheExpirationTime(), conf.getMaximumNumberOfOpenFiles(), cleanupList);
    syncthread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          logger.info("TimeSeries RRD Sync Thread started");
          while (syncthreadActive) {
            List<RrdDbWrapper> syncList;
            synchronized (StorageTypeRRD4J.this) {
              syncList = new ArrayList<>(cache.values());
            }
            if (syncList.size() > 0) {
              long currentTime = System.currentTimeMillis();
              int cntSynched = 0;
              int cntRemoved = 0;
              for (RrdDbWrapper r : syncList) {
                try {
                  boolean remove;
                  synchronized (StorageTypeRRD4J.this) {
                    remove = r.shouldBeRemovedFromCache(currentTime, conf.getCacheExpirationTime());
                    if (remove) {
                      cache.remove(r.id);
                    }
                  }
                  if (remove) {
                    r.cleanup();
                    cntRemoved++;
                  } else if (r.lazysync(currentTime)) {
                    cntSynched++;
                  }
                } catch (Throwable t) {
                  logger.warn("Could not sync rrd", t);
                }
              }
              if ((cntSynched > 0 || cntRemoved > 0) && logger.isInfoEnabled()) {
                logger.info("Synched " + cntSynched + " rrd cache entries, removed " + cntRemoved);
              }
            }
            try {
              Thread.sleep(conf.getCacheSyncInterval() / 3);
            } catch (InterruptedException e) {
              //ok, retry
            }
          }
          logger.info("TimeSeries RRD Sync Thread finished");
        } catch (Throwable t) {
          logger.warn("TimeSeries RRD Sync Thread failed", t);
        }
      }

    }, "TimeSeriesRRD-Sync");
    syncthread.setDaemon(true);
    syncthread.start();
  }


  @Override
  public String create(StorageParameter[] parameters, DataSourceParameter datasourceParameter) {
    String id = conf.createNewUniqueId();
    StorageParameterRRD4J[] parametersrrd = new StorageParameterRRD4J[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      StorageParameter parameter = parameters[i];
      if (!(parameter instanceof StorageParameterRRD4J)) {
        throw new IllegalArgumentException();
      }
      parametersrrd[i] = (StorageParameterRRD4J) parameter;
    }
    long stepsize = getStepSize(parametersrrd);
    RrdDef rrd = new RrdDef(getPath(id), stepsize);
    for (StorageParameterRRD4J para : parametersrrd) {
      rrd.addArchive(getConsolidationFunction(para.getAggregation()), getXFilesFactor(para), getSteps(para, stepsize), getRows(para));
    }
    rrd.addDatasource(getDataSourceName(datasourceParameter), getDataSourceType(datasourceParameter),
                      getHeartbeat(datasourceParameter, stepsize), getMinValue(datasourceParameter), getMaxValue(datasourceParameter));
    try (RrdDb rrddb = new RrdDb(rrd)) {
      //ntbd, file wurde erzeugt
    } catch (IOException e) {
      pathcreated = false;
      throw new RuntimeException("Could not create RrdDb", e);
    }
    if (logger.isDebugEnabled()) {
      StringBuilder arcs = new StringBuilder();
      boolean first = true;
      for (ArcDef ad : rrd.getArcDefs()) {
        if (first) {
          first = false;
        } else {
          arcs.append(", ");
        }
        arcs.append(ad.dump());
      }
      logger.debug("created rrd " + id + ", step=" + stepsize + ", with datasource " + rrd.getDsDefs()[0].dump() + " and archives " + arcs);
    }
    return id;
  }


  private double getMaxValue(DataSourceParameter datasourceParameter) {
    return datasourceParameter.getMaxValue();
  }


  private double getMinValue(DataSourceParameter datasourceParameter) {
    return datasourceParameter.getMinValue();
  }


  private long getHeartbeat(DataSourceParameter datasourceParameter, long stepsize) {
    return stepsize * conf.getHeartBeatAsFactorOfStepSize();
  }


  private DsType getDataSourceType(DataSourceParameter datasourceParameter) {
    switch (datasourceParameter.getDataSourceType()) {
      case GAUGE :
        return DsType.GAUGE;
      case COUNTER :
        return DsType.COUNTER;
      default :
        throw new RuntimeException();
    }
  }


  private String getDataSourceName(DataSourceParameter datasourceParameter) {
    return datasourceParameter.getDataSourceName();
  }


  private long ggt(long a, long b) {
    long r;
    while (b != 0) {
      r = a % b;
      a = b;
      b = r;
    }
    return a;
  }


  /*
   * gr��e eines primary data points (PDP). z.b. in sekunden
   */
  private long getStepSize(StorageParameterRRD4J[] parameters) {
    //gr��ter gemeinsamer teiler aller bin-l�ngen
    long s = parameters[0].getBucketSizeInSeconds();
    for (int i = 1; i < parameters.length; i++) {
      if (s == 1) {
        break;
      }
      s = ggt(s, parameters[i].getBucketSizeInSeconds());
    }
    return s;
  }


  /*
   * anzahl von bins
   */
  private int getRows(StorageParameterRRD4J parameter) {
    float f = (float) parameter.getLengthInSeconds() / parameter.getBucketSizeInSeconds();
    return (int) Math.ceil(f);
  }


  /*
   * anzahl von primary data points (PDP) per bin (=consolidated data point (CDP)) im archive
   */
  private int getSteps(StorageParameterRRD4J parameter, long stepsize) {
    return (int) (parameter.getBucketSizeInSeconds() / stepsize);
  }


  /*
   * beispiel: 0.3 bedeutet, dass wenn mehr als 30% aller datenpunkte pro bin unknown sind, gilt das gesamte bin als unknown. 
   */
  private double getXFilesFactor(StorageParameterRRD4J parameter) {
    return 0.9;
  }


  private ConsolFun getConsolidationFunction(AggregationType agg) {
    switch (agg) {
      case AVERAGE :
        return ConsolFun.AVERAGE;
      case MIN :
        return ConsolFun.MIN;
      case MAX :
        return ConsolFun.MAX;
      case LAST :
        return ConsolFun.LAST;
      case FIRST :
        return ConsolFun.FIRST;
      default :
        throw new RuntimeException();
    }
  }


  private boolean pathcreated = false;


  private String getPath(String id) {
    File f = new File(conf.getPathPrefixForRRDDBs() + "/" + id + ".rrd");
    if (!pathcreated) {
      if (!f.getParentFile().exists()) {
        if (!f.getParentFile().mkdirs()) {
          throw new RuntimeException("could not create directory " + f.getParentFile().getAbsolutePath());
        }
      }
      pathcreated = true;
    }

    return f.getPath();
  }


  @Override
  public void addData(String id, long timeMS, Number value) {
    RrdDbWrapper rrdDbWrapper = getCachedRrdDb(id);
    try {
      rrdDbWrapper.createSampleAndUpdate(getAsRoundedSecond(timeMS), value.doubleValue());
      try {
        rrdDbWrapper.lazysync(System.currentTimeMillis());
      } catch (IOException e) {
        deleteFromCache(id);
        throw e;
      }
    } catch (IOException e) {
      pathcreated = false;
      throw new RuntimeException("RrdDb could not be opened: " + id, e);
    }
  }


  private long getAsRoundedSecond(long timeMS) {
    return Math.round(timeMS / 1000d);
  }


  @Override
  public FetchedData getData(String id, long starttimeMS, long endtimeMS, boolean calculateDiffs, long resolutionMS,
                             AggregationType aggregationType) {
    RrdDbWrapper rrdDbWrapper = getCachedRrdDb(id);
    try {
      long resolutionS = getAsRoundedSecond(resolutionMS);
      long startS = getAsRoundedSecond(starttimeMS) / resolutionS * resolutionS;
      long endS = getAsRoundedSecond(endtimeMS) / resolutionS * resolutionS;
      ConsolFun consolidationFunction = getConsolidationFunction(aggregationType);
      if (logger.isDebugEnabled()) {
        logger.debug("fetching data from " + startS + " to " + endS + " (resolution=" + resolutionS + ") with CF=" + consolidationFunction);
      }
      FetchRequest fetchRequest = rrdDbWrapper.createFetchRequest(consolidationFunction, startS, endS, resolutionS);
      FetchData fetchData = fetchRequest.fetchData();
      long firstTS = fetchData.getFirstTimestamp();
      long lastTS = fetchData.getLastTimestamp();
      double[] data = fetchData.getValues()[0];
      long resultResolution = (lastTS - firstTS + rrdDbWrapper.getRrdDef().getStep()) / data.length;
      if (logger.isDebugEnabled()) {
        logger.debug("fetched " + data.length + " values between " + firstTS + " and " + lastTS + " (resolution=" + resultResolution + ")");
      }
      return new FetchedData(data, firstTS * 1000, resultResolution * 1000);
    } catch (IOException e) {
      pathcreated = false;
      throw new RuntimeException("RrdDb could not be opened: " + id, e);
    }
  }


  private RrdDbWrapper getCachedRrdDb(String id) {
    List<RrdDbWrapper> cleanup = null;
    RrdDbWrapper rrdDbWrapper;
    synchronized (this) {
      rrdDbWrapper = cache.get(id);
      if (rrdDbWrapper == null) {
        try {
          rrdDbWrapper = new RrdDbWrapper(id, new RrdDb(getPath(id)), conf.getCacheSyncInterval());
          rrdDbWrapper.update(); //damit es nicht sofort aus map entfernt wird
          cache.put(id, rrdDbWrapper);
          cleanup = new ArrayList<>();
          synchronized (cleanupList) {
            cleanup.addAll(cleanupList);
            cleanupList.clear();
          }
        } catch (IOException e) {
          pathcreated = false;
          throw new RuntimeException("RrdDb could not be opened: " + id, e);
        }
      } else {
        rrdDbWrapper.update(); //damit es nicht sofort aus map entfernt werden kann
      }
    }
    if (cleanup != null) {
      for (RrdDbWrapper r : cleanup) {
        r.cleanup();
      }
    }
    return rrdDbWrapper;
  }


  private RrdDbWrapper deleteFromCache(String id) {
    RrdDbWrapper r;
    synchronized (this) {
      r = cache.remove(id);
    }
    if (r != null) {
      r.cleanup();
    }
    return r;
  }


  @Override
  public StoredMetaData getMetaData(String id) {
    RrdDbWrapper rrdDbWrapper = getCachedRrdDb(id);
    try {
      long startTimeS = Long.MAX_VALUE;
      for (int i = 0; i < rrdDbWrapper.getArcCount(); i++) {
        long st = rrdDbWrapper.getArchive(i).getStartTime();
        if (st < startTimeS) {
          startTimeS = st;
        }
      }
      return new StoredMetaData(startTimeS * 1000, rrdDbWrapper.getLastUpdateTime() * 1000);
    } catch (IOException e) {
      pathcreated = false;
      throw new RuntimeException("RrdDb could not be opened: " + id, e);
    }
  }


  @Override
  public void delete(String id) {
    deleteFromCache(id);
    File f = new File(getPath(id));
    if (!f.delete()) {
      logger.info("Could not delete " + f.getAbsolutePath());
    }
  }


  @Override
  public StorageParameter[] getParameter(String id) {
    try (RrdDb rrd = new RrdDb(getPath(id))) {
      List<StorageParameter> l = new ArrayList<>();
      for (int i = 0; i < rrd.getArcCount(); i++) {
        AggregationType aggregation;
        Archive archive = rrd.getArchive(i);
        switch (archive.getConsolFun()) {
          case AVERAGE :
            aggregation = AggregationType.AVERAGE;
            break;
          case FIRST :
            aggregation = AggregationType.FIRST;
            break;
          case LAST :
            aggregation = AggregationType.LAST;
            break;
          case MIN :
            aggregation = AggregationType.MIN;
            break;
          case MAX :
            aggregation = AggregationType.MAX;
            break;
          default :
            throw new RuntimeException();
        }
        long bucketSizeInSeconds = archive.getSteps() * rrd.getRrdDef().getStep();
        long lengthInSeconds = archive.getRows() * bucketSizeInSeconds;
        l.add(new StorageParameterRRD4J(aggregation, lengthInSeconds, bucketSizeInSeconds));
      }
      return l.toArray(new StorageParameter[0]);
    } catch (IOException e) {
      throw new RuntimeException("RrdDb could not be opened: " + id, e);
    }
  }


  @Override
  public void shutdown() {
    synchronized (this) {
      for (RrdDbWrapper r : cache.values()) {
        try {
          r.cleanup();
        } catch (RuntimeException e) {
          logger.warn("Could not close cached RrdDb", e);
        }
      }
      cache.clear();
      synchronized (cleanupList) {
        for (RrdDbWrapper r : cleanupList) {
          r.cleanup();
        }
        cleanupList.clear();
      }
      syncthreadActive = false;
      syncthread.interrupt();
    }
  }

}

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
package com.gip.xyna.xnwh.statistics.timeseries;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.statistics.timeseries.StorageTypeRRD4J.RRD4JConfiguration;
import com.gip.xyna.xnwh.statistics.timeseries.StorageTypeRRD4J.StorageParameterRRD4J;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;



public class TimeSeriesManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "TimeSeriesManagement";

  Map<Class<? extends StorageParameter>, StorageType> types;
  Map<String, Class<? extends StorageParameter>> typesByName;
  private ODS ods;
  private PreparedQuery<TimeSeriesStorageStorable> queryByTSId;


  public TimeSeriesManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public static class RRD4JConfigurationInXyna implements RRD4JConfiguration {

    public static final XynaPropertyString pathprefix =
        new XynaPropertyString("xnwh.timeseries.storage.rrd4j.directory.base", "storage/timeseries/rrd4j")
            .setDefaultDocumentation(DocumentationLanguage.EN, "Path where RRD Archive files are stored");
    public static final XynaPropertyInt heartbeatfactor = new XynaPropertyInt("xnwh.timeseries.storage.rrd4j.heartbeat.stepsizefactor", 5)
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "RRD heartbeat = this stepsizefactor * minimum step size. (cf. official RRD documentation to understand what the heartbeat is for)");
    public static final XynaPropertyDuration cacheexpiration =
        new XynaPropertyDuration("xnwh.timeseries.storage.rrd4j.cache.expiration", new Duration(5, TimeUnit.MINUTES))
            .setDefaultDocumentation(DocumentationLanguage.EN, "How long unused RrdDb entries are kept in memory");
    public static final XynaPropertyDuration cacheSyncInterval =
        new XynaPropertyDuration("xnwh.timeseries.storage.rrd4j.cache.sync.interval", new Duration(5, TimeUnit.MINUTES))
            .setDefaultDocumentation(DocumentationLanguage.EN, "How often cached RrdDb entries are persisted to file");
    public static final XynaPropertyInt cacheMaxEntries = new XynaPropertyInt("xnwh.timeseries.storage.rrd4j.cache.size", 1000)
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Maximum number of entries in cache (each entry corresponds to one open file descriptor)");


    @Override
    public int getHeartBeatAsFactorOfStepSize() {
      return heartbeatfactor.get();
    }


    @Override
    public String getPathPrefixForRRDDBs() {
      return pathprefix.get();
    }


    @Override
    public String createNewUniqueId() {
      return String.valueOf(XynaFactory.getInstance().getIDGenerator().getUniqueId("timeseries.rrd4j"));
    }


    @Override
    public long getCacheExpirationTime() {
      return cacheexpiration.getMillis();
    }


    @Override
    public long getCacheSyncInterval() {
      return cacheSyncInterval.getMillis();
    }


    @Override
    public int getMaximumNumberOfOpenFiles() {
      return cacheMaxEntries.get();
    }

  }


  @Override
  protected void init() throws XynaException {
    types = new HashMap<>();
    typesByName = new HashMap<>();
    //initialisierung erst, nachdem xynaproperties auch verfügbar sind
    XynaFactory.getInstance().getFutureExecution().addTask(TimeSeriesManagement.class, "TimeSeriesMgmt").after(XynaProperty.class)
        .execAsync(new Runnable() {

          @Override
          public void run() {
            types.put(StorageParameterRRD4J.class, new StorageTypeRRD4J(new RRD4JConfigurationInXyna()));
          }

        });
    RRD4JConfigurationInXyna.cacheexpiration.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    RRD4JConfigurationInXyna.cacheMaxEntries.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    RRD4JConfigurationInXyna.cacheSyncInterval.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    RRD4JConfigurationInXyna.heartbeatfactor.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    RRD4JConfigurationInXyna.pathprefix.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    typesByName.put(StorageParameterRRD4J.class.getSimpleName(), StorageParameterRRD4J.class);

    ods = ODSImpl.getInstance();
    ods.registerStorable(TimeSeriesStorable.class);
    ods.registerStorable(TimeSeriesStorageStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      queryByTSId = con.prepareQuery(new Query<>(
                                                 "select * from " + TimeSeriesStorageStorable.TABLE_NAME + " where "
                                                     + TimeSeriesStorageStorable.COL_TIMESERIES_ID + " = ?",
                                                 TimeSeriesStorageStorable.reader));
    } finally {
      con.closeConnection();
    }
  }


  @Override
  protected void shutdown() throws XynaException {
    if (types != null) {
      for (StorageType st : types.values()) {
        st.shutdown();
      }
    }
  }


  public long createTimeSeries(TimeSeriesCreationParameter parameter) throws PersistenceLayerException {
    if (parameter.storageParameter == null || parameter.storageParameter.length == 0) {
      throw new IllegalArgumentException("storage parameter not set");
    }
    Map<Class<? extends StorageParameter>, ArrayList<StorageParameter>> map = CollectionUtils
        .group(Arrays.asList(parameter.storageParameter), new Transformation<StorageParameter, Class<? extends StorageParameter>>() {

          @Override
          public Class<? extends StorageParameter> transform(StorageParameter from) {
            return from.getClass();
          }

        });
    Map<Class<? extends StorageParameter>, String> ids = new HashMap<>();
    boolean success = false;
    try {
      for (Entry<Class<? extends StorageParameter>, ArrayList<StorageParameter>> e : map.entrySet()) {
        ids.put(e.getKey(), types.get(e.getKey()).create(e.getValue().toArray(new StorageParameter[0]), parameter.datasourceParameter));
      }
      success = true;

      TimeSeriesStorable tss = TimeSeriesStorable.create();
      tss.setDatasourceName(parameter.datasourceParameter.getDataSourceName());
      tss.setDatasourceType(parameter.datasourceParameter.getDataSourceType().name());
      tss.setMaxvalue(parameter.datasourceParameter.getMaxValue());
      tss.setMinvalue(parameter.datasourceParameter.getMinValue());
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(tss);
        for (Entry<Class<? extends StorageParameter>, String> e : ids.entrySet()) {
          TimeSeriesStorageStorable tsss = TimeSeriesStorageStorable.create();
          tsss.setStorageType(e.getKey().getSimpleName());
          tsss.setStorageId(e.getValue());
          tsss.setTimeSeriesId(tss.getId());
          con.persistObject(tsss);
        }
        con.commit();
      } finally {
        con.closeConnection();
      }
      return tss.getId();
    } finally {
      if (!success) {
        //rollback
        for (Entry<Class<? extends StorageParameter>, String> e : ids.entrySet()) {
          try {
            types.get(e.getKey()).delete(e.getValue());
          } catch (Exception ex) {
            logger.warn("could not remove time series " + e.getValue() + " of type " + e.getKey().getSimpleName(), ex);
          }
        }
      }
    }
  }


  public TimeSeries getTimeSeries(long id) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      TimeSeriesStorable ts = new TimeSeriesStorable(id);
      con.queryOneRow(ts);
      List<TimeSeriesStorageStorable> tss = con.query(queryByTSId, new Parameter(id), -1);
      return new TimeSeries(this, ts, tss.toArray(new TimeSeriesStorageStorable[0]));
    } finally {
      con.closeConnection();
    }
  }


  public void removeTimeSeries(long id) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      List<TimeSeriesStorageStorable> tss = con.query(queryByTSId, new Parameter(id), -1);
      for (TimeSeriesStorageStorable ts : tss) {
        types.get(typesByName.get(ts.getStorageType())).delete(ts.getStorageId());
        con.deleteOneRow(ts);
      }
      TimeSeriesStorable ts = new TimeSeriesStorable(id);
      con.deleteOneRow(ts);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public List<TimeSeries> listTimeSeries() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<TimeSeriesStorable> tss = con.loadCollection(TimeSeriesStorable.class);
      Collection<TimeSeriesStorageStorable> tsss = con.loadCollection(TimeSeriesStorageStorable.class);
      Map<Long, List<TimeSeriesStorageStorable>> map = new HashMap<>();
      for (TimeSeriesStorageStorable t : tsss) {
        List<TimeSeriesStorageStorable> e = map.get(t.getTimeSeriesId());
        if (e == null) {
          e = new ArrayList<>();
          map.put(t.getTimeSeriesId(), e);
        }
        e.add(t);
      }
      List<TimeSeries> result = new ArrayList<>();
      for (TimeSeriesStorable t : tss) {
        List<TimeSeriesStorageStorable> list = map.get(t.getId());
        result
            .add(new TimeSeries(this, t, list == null ? new TimeSeriesStorageStorable[0] : list.toArray(new TimeSeriesStorageStorable[0])));
      }
      return result;
    } finally {
      con.closeConnection();
    }
  }

}

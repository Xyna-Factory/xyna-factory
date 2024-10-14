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
package com.gip.xyna.xfmg.xfmon.fruntimestats;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper.MapDiscoveryStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsReducer;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsReducer;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.SumAggregationPushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableAggregationStatisticsPersistenceHandler<S extends Storable<S>> extends StatisticsPersistenceHandler {

  private final static Logger logger = CentralFactoryLogging.getLogger(StorableAggregationStatisticsPersistenceHandler.class);
  private final StatisticsStorableMapper<S> mapper;
  private final StorableSpecificHelper<S> generator;
  private final ODSConnectionType type;
  
  public StorableAggregationStatisticsPersistenceHandler(StatisticsPersistenceStrategy strategy, StatisticsPath associatedStatistics,
                                                                      ODSConnectionType type, StatisticsStorableMapper<S> mapper,
                                                                      StorableSpecificHelper<S> generator) {
    super(strategy, associatedStatistics);
    this.mapper = mapper;
    this.generator = generator;
    this.type = type;
  }

  

  @Override
  public void persist(StatisticsPath path) throws PersistenceLayerException {
    StatisticsAggregator<? extends StatisticsValue<?>, ? extends StatisticsValue<S>> aggregator;
    switch (getPersistenceStrategy()) {
      case ASYNCHRONOUSLY :
      case SHUTDOWN :
        // getAggregator from associatedStatistics-path..but where does it end? Let's assume it's complete
        aggregator = getStorableAggregator(getAssociatedStatisticsPath());
        break;
      case SYNCHRONOUSLY :
        aggregator = getStorableAggregator(path);
        break;
      case NEVER :
        // ntbd
        return;
    default :
      throw new RuntimeException("Invalid persistence strategy: " + getPersistenceStrategy());
    }

    try {
      Collection<? extends StatisticsValue<S>> allStorables = getRuntimeStatistics().getAggregatedValue(aggregator);
      Collection<S> toPersist = new ArrayList<S>(allStorables.size());
      for (StatisticsValue<S> statisticsValue : allStorables) {
        generator.injectPrimaryKey(statisticsValue.getValue());
        toPersist.add(statisticsValue.getValue());
      }
      ODSConnection con = getODS().openConnection(type);
      try {
        con.persistCollection(toPersist);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } catch (XFMG_InvalidStatisticsPath e) {
      logger.warn("Could not retrieve storable for persist.",e);
    } catch (XFMG_UnknownStatistic e) {
      logger.warn("Could not retrieve storable for persist.",e);
    }
  }


  @Override
  public void remove(StatisticsPath path) throws PersistenceLayerException {
    StatisticsAggregator<? extends StatisticsValue<?>, ? extends StatisticsValue<S>> aggregator = getStorableAggregator(path);
    try {
      Collection<? extends StatisticsValue<S>> result = getRuntimeStatistics().getAggregatedValue(aggregator);
      for (StatisticsValue<S> statisticsValue : result) {
        ODSConnection con = getODS().openConnection(type);
        try {
          con.deleteOneRow(statisticsValue.getValue());
          con.commit();
        } finally {
          con.closeConnection();
        }
      }
    } catch (XFMG_InvalidStatisticsPath e) {
      logger.warn("Could not retrieve storable for removal.",e);
    } catch (XFMG_UnknownStatistic e) {
      logger.warn("Could not retrieve storable for removal.",e);
    }
  }
  
  
  public StatisticsAggregator<StorableStatisticsValue<S>, StorableStatisticsValue<S>> getStorableAggregator(StatisticsPath path) {
    // Assumption being all relevant values are stored under the final pathPart
    StatisticsPath pathToIncompleteAggregationStack =  new StatisticsPathImpl(path.getPath().subList(0, path.getPath().size() - 2));
    StatisticsAggregator<MapDiscoveryStatisticsValue, StorableStatisticsValue<S>> storableAggregator = 
      new StatisticsAggregator<MapDiscoveryStatisticsValue, StorableStatisticsValue<S>>(
                      path.getPathPart(path.getPath().size() - 2),
                      mapper,
                      (StatisticsReducer<StorableStatisticsValue<S>>) PredefinedStatisticsReducer.NO_REDUCTION.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass()));
    StatisticsAggregator<StatisticsValue<?>, MapDiscoveryStatisticsValue> storableValuesAggregator =
      new StatisticsAggregator<StatisticsValue<?>, MapDiscoveryStatisticsValue>(
                      StatisticsPathImpl.ALL,
                      (StatisticsMapper<StatisticsValue<?>, MapDiscoveryStatisticsValue>) PredefinedStatisticsMapper.MAP_DISCOVERY.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass(), MapDiscoveryStatisticsValue.class),
                      PredefinedStatisticsReducer.DEFAULT.typeCast(MapDiscoveryStatisticsValue.class));
    storableAggregator.setNextAggregationPart(storableValuesAggregator);
    StatisticsAggregator<StorableStatisticsValue<S>, StorableStatisticsValue<S>> completeAggregator = 
      AggregationStatisticsFactory.completeAggregationStack(pathToIncompleteAggregationStack, storableAggregator, true);
    StatisticsPath testPath = new StatisticsPathImpl();
    StatisticsAggregator curr = completeAggregator;
    while (curr != null) {
      testPath = testPath.append(curr.getPathpart());
      curr = curr.getNextAggregationPart();
    }
    return completeAggregator;
  }
  

  private enum ColumnType {
    INT {

      @Override
      public StatisticsValue instantiate(Serializable s) {
        return new IntegerStatisticsValue((int) s);
      }
    },
    INTEGER {

      @Override
      public StatisticsValue instantiate(Serializable s) {
        Integer i = (Integer) s;
        if (i == null) {
          return new IntegerStatisticsValue(0);
        } else {
          return new IntegerStatisticsValue(i.intValue());
        }
      }
    },
    LONG {

      @Override
      public StatisticsValue instantiate(Serializable s) {
        return new LongStatisticsValue((long) s);
      }
    },
    LONGOBJ {

      @Override
      public StatisticsValue instantiate(Serializable s) {
        Long i = (Long) s;
        if (i == null) {
          return new LongStatisticsValue(0);
        } else {
          return new LongStatisticsValue(i.longValue());
        }
      }
    },
    STRING {

      @Override
      public StatisticsValue instantiate(Serializable s) {
        try {
          return new StringStatisticsValue((String) s);
        } catch (ClassCastException e) {
          if (s == null) {
            return new StringStatisticsValue(null); 
          }
          return new StringStatisticsValue(s.toString());
        }
      }
    };

    public abstract StatisticsValue instantiate(Serializable s);

    public static ColumnType of(Class<?> type) {
      if (type == int.class) {
        return INT;
      } else if (type == Integer.class) {
        return INTEGER;
      } else if (type == long.class) {
        return LONG;
      } else if (type == Long.class) {
        return LONGOBJ;
      } else if (type == String.class) {
        return STRING;
      }
      if (!type.isAssignableFrom(String.class)) {
        logger.debug("Encountered complex value of type " + type.toString() + " while mapping fields, transforming to String");
      }
      return STRING;
    }
  }

  private static class StorableColumnInfo {

    public final String colName;
    public final ColumnType type;
    
    public StorableColumnInfo(String colName, ColumnType type) {
      this.colName = colName;
      this.type = type;
    }
  }


  public Collection<PushStatistics> generateStatisticsFromStorable(S storable) {
    StatisticsPath path = generator.generatePathToStorableValues(storable);
    Class<S> storableClazz = mapper.storableClazz;
    Collection<PushStatistics> statistics = new ArrayList<PushStatistics>();
    for (Entry<String, StorableColumnInfo> mappingEntry : mapper.mapping.entrySet()) {
      try {
        StorableColumnInfo col = mappingEntry.getValue();
        StatisticsValue value = col.type.instantiate(storable.getValueByColString(col.colName));
        PushStatistics statistic = mapper.instantiateStatistics(path.append(mappingEntry.getKey()), value, storable);
        if (getPersistenceStrategy() == StatisticsPersistenceStrategy.SYNCHRONOUSLY) {
          statistic.injectSyncPersistenceHandler(this);
        }
        statistics.add(statistic);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("",e);
      }
      
    }
    return statistics;
  }
  
  
  public Collection<PushStatistics> restoreFromPersistence() throws PersistenceLayerException {
    ODSConnection con = getODS().openConnection(type);
    try {
      Collection<S> all = loadAll(con);
      Collection<PushStatistics> stats = new ArrayList<PushStatistics>();
      for (S s : all) {
        stats.addAll(generateStatisticsFromStorable(s));
      }
      return stats;
    } finally {
      con.closeConnection();
    }
  }
  
  
  protected Collection<S> loadAll(ODSConnection con) throws PersistenceLayerException {
     return con.loadCollection(mapper.storableClazz);
  }
  
  
  protected FactoryRuntimeStatistics getRuntimeStatistics() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
  }
  
  
  protected ODS getODS() {
    return ODSImpl.getInstance();
  }
  
  
  
  public static class StatisticsStorableMapper<S extends Storable<S>> implements StatisticsMapper<PredefinedStatisticsMapper.MapDiscoveryStatisticsValue, StorableStatisticsValue<S>> {
    
    private final Class<S> storableClazz;
    private final Map<String, StorableColumnInfo> mapping;
    private final Set<String> sumAggregationStats;
    private final Constructor<S> constr;
    
    public StatisticsStorableMapper(Class<S> storableClazz) {
      this.storableClazz = storableClazz;
      try {
        constr = storableClazz.getConstructor();
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("no empty constructor found for storableclass " + storableClazz.getCanonicalName(), e);
      } catch (SecurityException e) {
        throw new RuntimeException("empty constructor not accessible for storableclass " + storableClazz.getCanonicalName(), e);
      }
      this.mapping = new HashMap<String, StorableColumnInfo>();
      this.sumAggregationStats = new HashSet<String>();
    }
    
    
    public void addMapping(Column column, StatisticsPathPart relevantPart, boolean sumAggregation) {
      mapping.put(relevantPart.getPartName(), new StorableColumnInfo(column.name(), ColumnType.of(Storable.getColumn(column, storableClazz).getType())));
      if (sumAggregation) {
        sumAggregationStats.add(relevantPart.getPartName());
      }
    }
    
    
    public void addMapping(String columnName, StatisticsPathPart relevantPart, boolean sumAggregation) {
      Column[] columns = Storable.getColumns(storableClazz);
      for (Column column : columns) {
        if (column.name().equalsIgnoreCase(columnName)) {
          addMapping(column, relevantPart, sumAggregation);
          return;
        }
      }
      throw new RuntimeException("Column not found with name: " + columnName);
    }
    
    
    public void addMapping(String columnName, String relevantPathPart, boolean sumAggregation) {
      addMapping(columnName, StatisticsPathImpl.simplePathPart(relevantPathPart), sumAggregation);
    }
    

    public StorableStatisticsValue<S> map(MapDiscoveryStatisticsValue in, String nodename) {
      S storable;
      try {
        storable = constr.newInstance();
        HashMap<StatisticsPath, StatisticsValue> discoveredValues = in.getValue();
        for (Entry<StatisticsPath, StatisticsValue> discoveredEntry : discoveredValues.entrySet()) {
          StorableColumnInfo column = mapping.get(discoveredEntry.getKey().getPathPart(discoveredEntry.getKey().length() - 1).getPartName());
          if (column != null) {
            storable.setValueByColumnName(column.colName, (Serializable) discoveredEntry.getValue().getValue());
          } else {
            // No mapping found :-/
          }
        }
        return new StorableStatisticsValue<S>(storable);
      } catch (InstantiationException e) {
        throw new RuntimeException("",e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("",e);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("",e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException("constructor call failed", e.getTargetException());
      }
    }
    
    
    protected PushStatistics instantiateStatistics(StatisticsPath path, StatisticsValue value, S storable) {
      if (sumAggregationStats.contains(path.getPathPart(path.length() - 1).getPartName())) {
        return new SumAggregationPushStatistics(path, value);
      } else {
        return new PushStatistics(path, value);
      }
    }
      
  }
  
  

  public static class StorableStatisticsValue<S extends Storable<S>> implements StatisticsValue<S> {

    private static final long serialVersionUID = 7205641736339469046L;
    private S storable;
    
    public StorableStatisticsValue(S storable) {
      this.storable = storable;
    }
    
    public S getValue() {
      return storable;
    }

    public void merge(StatisticsValue<S> otherValue) {
      
    }

    public StatisticsValue<S> deepClone() {
      S clone;
      try {
        clone = (S) storable.getClass().getConstructor().newInstance();
        clone.setAllFieldsFromData(storable);
        return new StorableStatisticsValue(clone);
      } catch (Exception e) {
        throw new RuntimeException("",e);
      }
    }
    
  }
  
  
  public static interface StorableSpecificHelper<S extends Storable<S>> {
    
    public StatisticsPath generatePathToStorableValues(S storable);
    
    public void injectPrimaryKey(S storable);
    
  }


}

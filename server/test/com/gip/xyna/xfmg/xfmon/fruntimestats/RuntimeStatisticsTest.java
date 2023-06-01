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
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StatisticsPersistenceHandler.StatisticsPersistenceStrategy;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StorableAggregationStatisticsPersistenceHandler.StatisticsStorableMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StorableAggregationStatisticsPersistenceHandler.StorableSpecificHelper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsReducer;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsReducer;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.AggregatedStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.ForeignDataStore;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertySource;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;



public class RuntimeStatisticsTest extends TestCase {

  private RuntimeStatistics rs;
  
  protected void setUp() throws Exception {
    super.setUp();
    rs = new RuntimeStatistics();
    ODS ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(42, XynaLocalMemoryPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                      ODSConnectionType.DEFAULT, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id);
    ods.setDefaultPersistenceLayer(ODSConnectionType.ALTERNATIVE, id);
    ods.setDefaultPersistenceLayer(ODSConnectionType.INTERNALLY_USED, id);
    XynaPropertyUtils.exchangeXynaPropertySource(new AbstractXynaPropertySource() {
      public String getProperty(String name) {
        if( name.equals(XynaProperty.RUNTIME_STATISICS_ASYNC_PERSISTENCE_INTERVAL.getPropertyName()) ) {
          return "1000";
        }
        return null;
      }
    });
  }
  
  protected void tearDown() throws Exception {
    if (rs != null) {
      rs.shutdown();
    }
    ODSImpl.clearInstances();
    super.tearDown();
  }
  
  public void testRegisterStatistics() throws XynaException {
    final Long firstLongValue = 1L;
    final LongStatisticsValue firstPushValue = new LongStatisticsValue(firstLongValue);
    rs.registerStatistic(getCallStatsCallStatistics("bg.Baum"));
    getCallStatsCallStatistics("bg.Baum").pushValue(firstPushValue);
    StatisticsValue<Long> value = rs.getStatisticsValue(getCallStatsCallStatisticsPath("bg.Baum"));
    assertNotNull(value.getValue());
    assertEquals(firstLongValue, value.getValue());
  }
  
  
  public void testAggregateAdhocQuery() throws Throwable {
    final Long BAUM_CALLS = 2L;
    final Long BAUM2_CALLS = 1L;
    final Long BAUM_FINISHES = 1L;
    rs.registerStatistic(getCallStatsCallStatistics("bg.Baum"));
    getCallStatsCallStatistics("bg.Baum").pushValue(new LongStatisticsValue(BAUM_CALLS));
    rs.registerStatistic(getCallStatsCallStatistics("bg.Baum2"));
    getCallStatsCallStatistics("bg.Baum2").pushValue(new LongStatisticsValue(BAUM2_CALLS));
    rs.registerStatistic(getCallStatsFinishedStatistics("bg.Baum"));
    getCallStatsFinishedStatistics("bg.Baum").pushValue(new LongStatisticsValue(BAUM_FINISHES));
    StatisticsAggregator<LongStatisticsValue, LongStatisticsValue> aggregate = getAggregatedFailedStats();
    Collection<? extends StatisticsValue<Long>> result = rs.getAggregatedValue(aggregate);
    assertEquals("Results should have been reduced to one", 1, result.size());
    assertEquals("Aggregation should equal calculation", new Long(BAUM_CALLS + BAUM2_CALLS - BAUM_FINISHES), result.iterator().next().getValue());
  }
  
  
  public void testAggregateStatCreation() throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    final Long BAUM_CALLS = 2L;
    final Long BAUM2_CALLS = 1L;
    final Long BAUM_FINISHES = 1L;
    rs.registerStatistic(getCallStatsCallStatistics("bg.Baum"));
    getCallStatsCallStatistics("bg.Baum").pushValue(new LongStatisticsValue(BAUM_CALLS));
    rs.registerStatistic(getCallStatsCallStatistics("bg.Baum2"));
    getCallStatsCallStatistics("bg.Baum2").pushValue(new LongStatisticsValue(BAUM2_CALLS));
    rs.registerStatistic(getCallStatsFinishedStatistics("bg.Baum"));
    getCallStatsFinishedStatistics("bg.Baum").pushValue(new LongStatisticsValue(BAUM_FINISHES));
    final StatisticsPath PATH_FOR_AGGREGATION = new StatisticsPathImpl("test","aggregation","difference");
    Statistics<Long, LongStatisticsValue> stats = new AggregatedTestStatistics<Long, LongStatisticsValue>(PATH_FOR_AGGREGATION, getAggregatedFailedStats(), rs);
    rs.registerStatistic(stats);
    StatisticsValue result = rs.getStatisticsValue(PATH_FOR_AGGREGATION);
    assertEquals("Aggregation should equal calculation", new Long(BAUM_CALLS + BAUM2_CALLS - BAUM_FINISHES), result.getValue());
  }
  
  
  public void testStatisticsRegistrationFaults() throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    final StatisticsPath STATISTICS_PATH = new StatisticsPathImpl("test","statistics","unregistration");
    final StatisticsPath PARTIAL_STATISTICS_PATH = new StatisticsPathImpl("test","statistics");
    final String TEST_VALUE = "testStatisticsUnregistration";
    
    try {
      StatisticsValue<Long> value = rs.getStatisticsValue(STATISTICS_PATH);
      fail();
    } catch (Throwable t) {
      // ntbd
    }
    
    final Long firstLongValue = 1L;
    final LongStatisticsValue firstPushValue = new LongStatisticsValue(firstLongValue);
    rs.registerStatistic(new Statistics<String, StringStatisticsValue>(STATISTICS_PATH) {
      @Override
      public StringStatisticsValue getValueObject() {
        return new StringStatisticsValue(TEST_VALUE);
      }

      @Override
      public String getDescription() {
        return "";
      }
    });
    
    try {
      rs.registerStatistic(new Statistics<String, StringStatisticsValue>(STATISTICS_PATH) {
        @Override
        public StringStatisticsValue getValueObject() {
          return new StringStatisticsValue("NOT TEST_VALUE");
        }

        @Override
        public String getDescription() {
          return "";
        }
      });
      fail();
    } catch (Throwable t) {
      // ntbd
    }
    
    try {
      rs.getStatisticsValue(PARTIAL_STATISTICS_PATH);
      fail();
    } catch (Throwable t) {
      // ntbd
    }
    
    StatisticsValue value = rs.getStatisticsValue(STATISTICS_PATH);
    assertNotNull(value.getValue());
    assertEquals(TEST_VALUE, value.getValue());
    
    rs.unregisterStatistic(STATISTICS_PATH);
    
    try {
      rs.unregisterStatistic(STATISTICS_PATH);
      fail();
    } catch (Throwable t) {
      // ntbd
    }
    
  }
  
  public void testSimpleDefaultAggregation() throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    final StatisticsPath PATH1 = StatisticsPathImpl.fromString("test","simple","default","aggregation","path1");
    final StatisticsPath PATH2 = StatisticsPathImpl.fromString("test","simple","default","aggregation","path2");
    final StatisticsPath PATH3 = StatisticsPathImpl.fromString("test","simple","default","aggregation","path3");
    final Long VALUE1 = 1L;
    final Long VALUE2 = 2L;
    final Long VALUE3 = 3L;
    final Statistics<Long, LongStatisticsValue> STATISTICS1 = new Statistics<Long, LongStatisticsValue>(PATH1) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE1);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    final Statistics<Long, LongStatisticsValue> STATISTICS2 = new Statistics<Long, LongStatisticsValue>(PATH2) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE2);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    final Statistics<Long, LongStatisticsValue> STATISTICS3 = new Statistics<Long, LongStatisticsValue>(PATH3) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE3);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    rs.registerStatistic(STATISTICS1);
    rs.registerStatistic(STATISTICS2);
    rs.registerStatistic(STATISTICS3);
    assertEquals(VALUE1, rs.getStatisticsValue(PATH1).getValue());
    assertEquals(VALUE2, rs.getStatisticsValue(PATH2).getValue());
    assertEquals(VALUE3, rs.getStatisticsValue(PATH3).getValue());
    
    final StatisticsPath PATH_TO_AGGREGATE = StatisticsPathImpl.fromString("test","simple","default","aggregation","*");
    final StatisticsPath OWN_PATH = StatisticsPathImpl.fromString("test","simple","default","aggregation");
    final StatisticsAggregator aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(PATH_TO_AGGREGATE);
    final AggregatedTestStatistics aggregation = new AggregatedTestStatistics(OWN_PATH, aggregator, rs);
    rs.registerStatistic(aggregation);
    assertEquals(VALUE1 + VALUE2 + VALUE3, rs.getStatisticsValue(OWN_PATH).getValue());
  }
  
  
  public void testMoreComplexDefaultAggregation() throws XynaException {
    final StatisticsPath PATH1_1 = StatisticsPathImpl.fromString("test","simple","default","aggregation","path1");
    final StatisticsPath PATH1_2 = StatisticsPathImpl.fromString("test","simple","default","aggregation","path2");
    final StatisticsPath PATH1_3 = StatisticsPathImpl.fromString("test","simple","default","aggregation","path3");
    final Long VALUE1 = 1L;
    final Long VALUE2 = 2L;
    final Long VALUE3 = 3L;
    final Statistics<Long, LongStatisticsValue> STATISTICS1 = new Statistics<Long, LongStatisticsValue>(PATH1_1) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE1);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    final Statistics<Long, LongStatisticsValue> STATISTICS2 = new Statistics<Long, LongStatisticsValue>(PATH1_2) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE2);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    final Statistics<Long, LongStatisticsValue> STATISTICS3 = new Statistics<Long, LongStatisticsValue>(PATH1_3) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE3);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    rs.registerStatistic(STATISTICS1);
    rs.registerStatistic(STATISTICS2);
    rs.registerStatistic(STATISTICS3);
    assertEquals(VALUE1, rs.getStatisticsValue(PATH1_1).getValue());
    assertEquals(VALUE2, rs.getStatisticsValue(PATH1_2).getValue());
    assertEquals(VALUE3, rs.getStatisticsValue(PATH1_3).getValue());
    
    final StatisticsPath PATH2_1 = StatisticsPathImpl.fromString("test","simple","complex","aggregation","path1");
    final StatisticsPath PATH2_2 = StatisticsPathImpl.fromString("test","simple","complex","aggregation","path2");
    final StatisticsPath PATH2_3 = StatisticsPathImpl.fromString("test","simple","complex","aggregation","path3");
    final Long VALUE4 = 4L;
    final Long VALUE5 = 5L;
    final Long VALUE6 = 6L;
    final Statistics<Long, LongStatisticsValue> STATISTICS4 = new Statistics<Long, LongStatisticsValue>(PATH2_1) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE4);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    final Statistics<Long, LongStatisticsValue> STATISTICS5 = new Statistics<Long, LongStatisticsValue>(PATH2_2) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE5);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    final Statistics<Long, LongStatisticsValue> STATISTICS6 = new Statistics<Long, LongStatisticsValue>(PATH2_3) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE6);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    rs.registerStatistic(STATISTICS4);
    rs.registerStatistic(STATISTICS5);
    rs.registerStatistic(STATISTICS6);
    assertEquals(VALUE4, rs.getStatisticsValue(PATH2_1).getValue());
    assertEquals(VALUE5, rs.getStatisticsValue(PATH2_2).getValue());
    assertEquals(VALUE6, rs.getStatisticsValue(PATH2_3).getValue());
    
    final StatisticsPath PATH_TO_AGGREGATE = StatisticsPathImpl.fromString("test","simple", "*","aggregation","*");
    final StatisticsPath OWN_PATH = StatisticsPathImpl.fromString("some","where","else");
    final StatisticsAggregator aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(PATH_TO_AGGREGATE);
    final AggregatedTestStatistics aggregation = new AggregatedTestStatistics(OWN_PATH, aggregator, rs);
    rs.registerStatistic(aggregation);
    assertEquals(VALUE1 + VALUE2 + VALUE3 + VALUE4 + VALUE5 + VALUE6, rs.getStatisticsValue(OWN_PATH).getValue());
    
    final StatisticsPath PATH2_4 = StatisticsPathImpl.fromString("test","simple","complex","aggregation","path4");
    final Long VALUE7 = 7L;
    final Statistics<Long, LongStatisticsValue> STATISTICS7 = new Statistics<Long, LongStatisticsValue>(PATH2_4) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(VALUE7);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    rs.registerStatistic(STATISTICS7);
    assertEquals(VALUE7, rs.getStatisticsValue(PATH2_4).getValue());
    assertEquals(VALUE1 + VALUE2 + VALUE3 + VALUE4 + VALUE5 + VALUE6 + VALUE7, rs.getStatisticsValue(OWN_PATH).getValue());
    
    final StatisticsPath SOME_PATH = StatisticsPathImpl.fromString("test","simple","complex","some","path1");
    final Long SOME_VALUE = 123L;
    final Statistics<Long, LongStatisticsValue> SOME_STATISTICS = new Statistics<Long, LongStatisticsValue>(SOME_PATH) {
      @Override
      public LongStatisticsValue getValueObject() {
        return new LongStatisticsValue(SOME_VALUE);
      }

      @Override
      public String getDescription() {
        return "";
      }
    };
    rs.registerStatistic(SOME_STATISTICS);
    assertEquals(SOME_VALUE, rs.getStatisticsValue(SOME_PATH).getValue());
    assertEquals(VALUE1 + VALUE2 + VALUE3 + VALUE4 + VALUE5 + VALUE6 + VALUE7, rs.getStatisticsValue(OWN_PATH).getValue());
  }
  
  public void testDiscovery()  throws XynaException, IllegalArgumentException, IllegalAccessException {
    rs.registerStatistic(new DummyStats("baum.arg.de"));
    rs.registerStatistic(new DummyStats("baum.arg.wald"));
    rs.registerStatistic(new DummyStats("1.2.3.4.5"));
    rs.registerStatistic(new DummyStats("1.2.3.5.4"));
    rs.registerStatistic(new DummyStats("1.2.3.5.5"));
    rs.registerStatistic(new DummyStats("1.2.3.5.6.7.8"));
    
  
    Map<String, Serializable> discovered = rs.discoverStatistics(false);
    assertEquals(6, discovered.size());
  }
  
  
  public void testDefaultAggregateUseCase() throws Throwable {
    /*XPRC.XPCE.Stats.OrderStatistics.*.OrderType
      XPRC.XPCE.Stats.OrderStatistics.*.TotalCalls
      XPRC.XPCE.Stats.OrderStatistics.*.Success
      XPRC.XPCE.Stats.OrderStatistics.*.Timeouts
      XPRC.XPCE.Stats.OrderStatistics.*.Errors */
    // new: XPCE.Stats.OrderStatistics.*.All.OrderType <-- aggregation over all apps
    //      XPCE.Stats.OrderStatistics.*.Application-{X}.OrderType <-- data for a specific app
    //                                                ^ only being the app-name, not app&version or revision
    
    //let's assume:
    //      XPCE.Stats.OrderStatistics.Application-{X}.*.OrderType <-- data for a specific app
    //                                              ^ only being the app-name, not app&version or revision
    //      XPCE.Stats.OrderStatistics.All.*.OrderType <-- aggregation over all apps
    StatisticsPath pathprefix = PredefinedXynaStatisticsPath.ORDERSTATISTICS;
    List<String> ordertypes = new ArrayList<String>();
    ordertypes.add("bg.baum");
    ordertypes.add("bg.wald");
    List<String> applications = new ArrayList<String>();
    applications.add("SuperApp");
    applications.add("NotSoSuperApp");
    for (String ordertyp : ordertypes) {
      StatisticsPath pathWithOrdertype = pathprefix.append(StatisticsPathImpl.simplePathPart(ordertyp));
      for (String app : applications) {
        StatisticsPath pathWithApp = pathWithOrdertype.append(StatisticsPathImpl.simplePathPart("Application-"+app));
        rs.registerStatistic(new StaticStatistics<String>(pathWithApp.append(StatisticsPathImpl.simplePathPart("OrderType")), ordertyp));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("TotalCalls")), 15L));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("Success")), 5L));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("Timeouts")), 5L));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("Errors")), 5L));
      }
    }
    
    Collection discovery = rs.discoverStatistics();
    assertEquals(20, discovery.size());
    
    for (String ordertyp : ordertypes) {
      StatisticsPath ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                                                                          .append(PredefinedXynaStatisticsPathPart.ALL)
                                                                          .append(StatisticsPathImpl.simplePathPart("OrderType"));
      StatisticsPath pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                                                                                  .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, PredefinedXynaStatisticsPathPart.ALL.getPartName()))
                                                                                  .append(StatisticsPathImpl.simplePathPart("OrderType"));
      StatisticsAggregator aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      AggregatedTestStatistics statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("TotalCalls"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, PredefinedXynaStatisticsPathPart.ALL))
                      .append(StatisticsPathImpl.simplePathPart("TotalCalls"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("Success"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,  PredefinedXynaStatisticsPathPart.ALL))
                      .append(StatisticsPathImpl.simplePathPart("Success"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("Timeouts"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, PredefinedXynaStatisticsPathPart.ALL))
                      .append(StatisticsPathImpl.simplePathPart("Timeouts"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("Errors"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, PredefinedXynaStatisticsPathPart.ALL))
                      .append(StatisticsPathImpl.simplePathPart("Errors"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
    }
    
    discovery = rs.discoverStatistics();
    //System.out.println(discovery);
  }
  
  
  public void testDifferentPathLengthAggregation() throws Throwable {
    /*XPRC.XPCE.Stats.OrderStatistics.*.OrderType
      XPRC.XPCE.Stats.OrderStatistics.*.TotalCalls
      XPRC.XPCE.Stats.OrderStatistics.*.Success
      XPRC.XPCE.Stats.OrderStatistics.*.Timeouts
      XPRC.XPCE.Stats.OrderStatistics.*.Errors */
    // new: XPCE.Stats.OrderStatistics.*.All.OrderType <-- aggregation over all apps
    //      XPCE.Stats.OrderStatistics.*.Application.{X}.OrderType <-- data for a specific app
    //                                                ^ only being the app-name, not app&version or revision
    //      XPCE.Stats.OrderStatistics.*.WorkingSet.OrderType <-- data for a specific app
    StatisticsPath pathprefix = PredefinedXynaStatisticsPath.ORDERSTATISTICS;
    List<String> ordertypes = new ArrayList<String>();
    ordertypes.add("bg.baum");
    ordertypes.add("bg.wald");
    List<String> applications = new ArrayList<String>();
    applications.add("SuperApp");
    applications.add("NotSoSuperApp");
    for (String ordertyp : ordertypes) {
      StatisticsPath pathWithOrdertype = pathprefix.append(StatisticsPathImpl.simplePathPart(ordertyp));
      for (String app : applications) {
        StatisticsPath pathWithApp = pathWithOrdertype.append(StatisticsPathImpl.simplePathPart("Application")).append(StatisticsPathImpl.simplePathPart(app));
        rs.registerStatistic(new StaticStatistics<String>(pathWithApp.append(StatisticsPathImpl.simplePathPart("OrderType")), ordertyp));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("TotalCalls")), 15L));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("Success")), 5L));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("Timeouts")), 5L));
        rs.registerStatistic(new StaticStatistics<Long>(pathWithApp.append(StatisticsPathImpl.simplePathPart("Errors")), 5L));
      }
      StatisticsPath pathWithWorkingSet = pathWithOrdertype.append(StatisticsPathImpl.simplePathPart("WorkingSet"));
      rs.registerStatistic(new StaticStatistics<String>(pathWithWorkingSet.append(StatisticsPathImpl.simplePathPart("OrderType")), ordertyp));
      rs.registerStatistic(new StaticStatistics<Long>(pathWithWorkingSet.append(StatisticsPathImpl.simplePathPart("TotalCalls")), 15L));
      rs.registerStatistic(new StaticStatistics<Long>(pathWithWorkingSet.append(StatisticsPathImpl.simplePathPart("Success")), 5L));
      rs.registerStatistic(new StaticStatistics<Long>(pathWithWorkingSet.append(StatisticsPathImpl.simplePathPart("Timeouts")), 5L));
      rs.registerStatistic(new StaticStatistics<Long>(pathWithWorkingSet.append(StatisticsPathImpl.simplePathPart("Errors")), 5L));
    }
    
    Collection discovery = rs.discoverStatistics();
    assertEquals(30, discovery.size());
    
    for (String ordertyp : ordertypes) {
      StatisticsAggregator aggregator;
      AggregatedStatistics statistics;
      StatisticsPath ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("OrderType"));
      StatisticsPath pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,PredefinedXynaStatisticsPathPart.ALL.getPartName()))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,"TotalCalls", "Success", "Timeouts", "Errors"))
                      .append(new StatisticsPathImpl.WhiteListFilter(true, UnknownPathOnTraversalHandling.THROW_IF_ANY,"OrderType"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("TotalCalls"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,PredefinedXynaStatisticsPathPart.ALL))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,"OrderType", "Success", "Timeouts", "Errors"))
                      .append(new StatisticsPathImpl.WhiteListFilter(true, UnknownPathOnTraversalHandling.THROW_IF_ANY, "TotalCalls"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("Success"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,PredefinedXynaStatisticsPathPart.ALL))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,"OrderType", "TotalCalls", "Timeouts", "Errors"))
                      .append(new StatisticsPathImpl.WhiteListFilter(true, UnknownPathOnTraversalHandling.THROW_IF_ANY, "Success"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("Timeouts"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, PredefinedXynaStatisticsPathPart.ALL))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, "OrderType", "TotalCalls", "Success", "Errors"))
                      .append(new StatisticsPathImpl.WhiteListFilter(true, UnknownPathOnTraversalHandling.THROW_IF_ANY, "Timeouts"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
      ownPath = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(PredefinedXynaStatisticsPathPart.ALL)
                      .append(StatisticsPathImpl.simplePathPart("Errors"));
      pathToAggregate = PredefinedXynaStatisticsPath.ORDERSTATISTICS
                      .append(StatisticsPathImpl.simplePathPart(ordertyp))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,PredefinedXynaStatisticsPathPart.ALL))
                      .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,"OrderType", "TotalCalls", "Success", "Timeouts"))
                      .append(new StatisticsPathImpl.WhiteListFilter(true, UnknownPathOnTraversalHandling.THROW_IF_ANY, "Errors"));
      aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
      statistics = new AggregatedTestStatistics(ownPath, aggregator, rs);
      rs.registerStatistic(statistics);
    }
    
    discovery = rs.discoverStatistics();
    System.out.println(discovery);
  }
  
  
  public void testAggregateAggregation() throws Throwable {
    // baum.timeout  |
    // baum.fault    | -> errors.baum  |
    // wald.timeout  | -> errors.wald  | -> allErrors
    // wald.fault    |
    
    final Long singleErrorValue = 5L;
    
    StatisticsPath pathToBaumTimeout = StatisticsPathImpl.fromString("baum","timeout");
    rs.registerStatistic(new StaticStatistics<Long>(pathToBaumTimeout, singleErrorValue));
    StatisticsPath pathToBaumFault = StatisticsPathImpl.fromString("baum","fault");
    rs.registerStatistic(new StaticStatistics<Long>(pathToBaumFault, singleErrorValue));
    StatisticsPath pathToWaldTimeout = StatisticsPathImpl.fromString("wald","timeout");
    rs.registerStatistic(new StaticStatistics<Long>(pathToWaldTimeout, singleErrorValue));
    StatisticsPath pathToWaldFault = StatisticsPathImpl.fromString("wald","fault");
    rs.registerStatistic(new StaticStatistics<Long>(pathToWaldFault, singleErrorValue));
    
    StatisticsPath pathToAggregate;
    AggregatedStatistics statistics;
    StatisticsAggregator aggregator;
    StatisticsPath pathToErrorsBaum = StatisticsPathImpl.fromString("errors","baum");
    pathToAggregate = new StatisticsPathImpl().append(StatisticsPathImpl.simplePathPart("baum")).append(StatisticsPathImpl.ALL);
    aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
    statistics = new AggregatedTestStatistics(pathToErrorsBaum, aggregator, rs);
    rs.registerStatistic(statistics);
    StatisticsPath pathToErrorsWald = StatisticsPathImpl.fromString("errors","wald");
    pathToAggregate = new StatisticsPathImpl().append(StatisticsPathImpl.simplePathPart("wald")).append(StatisticsPathImpl.ALL);
    aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
    statistics = new AggregatedTestStatistics(pathToErrorsWald, aggregator, rs);
    rs.registerStatistic(statistics);
    StatisticsPath pathToAllErrors = StatisticsPathImpl.fromString("allErrors");
    pathToAggregate = new StatisticsPathImpl().append(StatisticsPathImpl.simplePathPart("errors")).append(StatisticsPathImpl.ALL);
    aggregator = AggregationStatisticsFactory.generateDefaultAggregatorForPath(pathToAggregate);
    statistics = new AggregatedTestStatistics(pathToAllErrors, aggregator, rs);
    rs.registerStatistic(statistics);
    
    assertEquals(singleErrorValue, rs.getStatisticsValue(pathToBaumTimeout).getValue());
    assertEquals(singleErrorValue, rs.getStatisticsValue(pathToBaumFault).getValue());
    assertEquals(singleErrorValue, rs.getStatisticsValue(pathToWaldTimeout).getValue());
    assertEquals(singleErrorValue, rs.getStatisticsValue(pathToWaldFault).getValue());
    
    assertEquals(singleErrorValue * 2, rs.getStatisticsValue(pathToErrorsBaum).getValue());
    assertEquals(singleErrorValue * 2, rs.getStatisticsValue(pathToErrorsWald).getValue());
    
    assertEquals(singleErrorValue * 4, rs.getStatisticsValue(pathToAllErrors).getValue());
  }
  
  
  public void testIntermediateStatisticsDiscovery() throws XynaException  {
    rs.registerStatistic(new DummyStats("a"));
    rs.registerStatistic(new DummyStats("a.a"));
    rs.registerStatistic(new DummyStats("a.b"));
    rs.registerStatistic(new DummyStats("a.a.a"));
    rs.registerStatistic(new DummyStats("a.a.b"));
    rs.registerStatistic(new DummyStats("a.b.a"));
    rs.registerStatistic(new DummyStats("a.b.b"));
    
  
    Map<String, Serializable> discovered = rs.discoverStatistics(false);
    assertEquals(7, discovered.size());
    System.out.println(discovered);
  }
  
  
  public void testSyncPersistenceHandler()  throws XynaException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSImpl.getInstance(false).registerStorable(TestCallStatsStorable.class);
    
    final Integer INITIAL_INT_VALUE = 1;
    final String INT_STATISICS_FINAL_PART_NAME = "finished";
    
    final TestCallStatsStorable testStorable = new TestCallStatsStorable("baum", "Wald", INITIAL_INT_VALUE, 111L);
    
    ODSConnection con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.persistObject(testStorable);
      con.commit();
    } finally {
      con.closeConnection();
    }
    // XPCE.Stats.OrderStatistics.*.Application-{X}.OrderType
    StatisticsStorableMapper<TestCallStatsStorable> mapper = new StatisticsStorableMapper<TestCallStatsStorable>(TestCallStatsStorable.class);
    mapper.addMapping(TestCallStatsStorable.COL_APPLICATION, "Application", false);
    mapper.addMapping(TestCallStatsStorable.COL_ORDERTYPE, "OrderType", false);
    mapper.addMapping(TestCallStatsStorable.COL_INTEGER, INT_STATISICS_FINAL_PART_NAME, false);
    mapper.addMapping(TestCallStatsStorable.COL_LOONG, "calls", false);
    StorableSpecificHelper<TestCallStatsStorable> generator = new StorableSpecificHelper<TestCallStatsStorable>() {

      public StatisticsPath generatePathToStorableValues(TestCallStatsStorable storable) {
        return StatisticsPathImpl.fromString("XPCE","Stats","OrderStatistics", storable.getOrdertype(), "Application-" + storable.getApplication());
      }

      public void injectPrimaryKey(TestCallStatsStorable storable) {
        // nothing to do as pk is part of statistics        
      }
    };
    StorableAggregationStatisticsPersistenceHandler handler = new StorableAggregationStatisticsPersistenceHandler<TestCallStatsStorable>(
                    StatisticsPersistenceStrategy.SYNCHRONOUSLY, StatisticsPathImpl.fromString("XPCE","Stats","OrderStatistics", "*", "*", "*"), ODSConnectionType.DEFAULT, mapper, generator) {
      
      @Override
      protected ODS getODS() {
        return ODSImpl.getInstance(false);
      }
      
      @Override
      protected RuntimeStatistics getRuntimeStatistics() {
        return rs;
      }
      
    };
    
    Collection<PushStatistics> stats = rs.registerStatisticsPersistenceHandler(handler);
    PushStatistics intStats = null;
    for (PushStatistics pushStatistics : stats) {
      if (pushStatistics.getPath().getPathPart(pushStatistics.getPath().length()-1).getPartName().equals(INT_STATISICS_FINAL_PART_NAME)) {
        intStats = pushStatistics;
        break;
      }
    }
    
    Map<String, Serializable> discovery = rs.discoverStatistics(false);
    assertEquals(4, discovery.size());
    assertEquals(INITIAL_INT_VALUE, discovery.get("XPCE.Stats.OrderStatistics."+testStorable.getOrdertype() + ".Application-" + testStorable.getApplication() + "." + INT_STATISICS_FINAL_PART_NAME));
    
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(testStorable);
    } finally {
      con.closeConnection();
    }
    assertEquals(INITIAL_INT_VALUE, (Integer)testStorable.getInteger());
    
    final Integer PUSHED_VALUE = 123;
    intStats.pushValue(new IntegerStatisticsValue(PUSHED_VALUE));
    
    discovery = rs.discoverStatistics(false);
    assertEquals(4, discovery.size());
    assertEquals(PUSHED_VALUE, discovery.get("XPCE.Stats.OrderStatistics."+testStorable.getOrdertype() + ".Application-" + testStorable.getApplication() + "." + INT_STATISICS_FINAL_PART_NAME));
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(testStorable);
    } finally {
      con.closeConnection();
    }
    assertEquals(PUSHED_VALUE, (Integer)testStorable.getInteger());
  }
  
  
  public void testAsyncPersistenceHandler()  throws XynaException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, InterruptedException {
    ODSImpl.getInstance(false).registerStorable(TestCallStatsStorable.class);
    
    final Integer INITIAL_INT_VALUE = 1;
    final String INT_STATISICS_FINAL_PART_NAME = "finished";
    
    final TestCallStatsStorable testStorable = new TestCallStatsStorable("baum", "Wald", INITIAL_INT_VALUE, 111L);
    
    ODSConnection con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.persistObject(testStorable);
      con.commit();
    } finally {
      con.closeConnection();
    }
    // XPCE.Stats.OrderStatistics.*.Application-{X}.OrderType
    StatisticsStorableMapper<TestCallStatsStorable> mapper = new StatisticsStorableMapper<TestCallStatsStorable>(TestCallStatsStorable.class);
    mapper.addMapping(TestCallStatsStorable.COL_APPLICATION, "Application", false);
    mapper.addMapping(TestCallStatsStorable.COL_ORDERTYPE, "OrderType", false);
    mapper.addMapping(TestCallStatsStorable.COL_INTEGER, INT_STATISICS_FINAL_PART_NAME, false);
    mapper.addMapping(TestCallStatsStorable.COL_LOONG, "calls", false);
    StorableSpecificHelper<TestCallStatsStorable> generator = new StorableSpecificHelper<TestCallStatsStorable>() {

      public StatisticsPath generatePathToStorableValues(TestCallStatsStorable storable) {
        return StatisticsPathImpl.fromString("XPCE","Stats","OrderStatistics", storable.getOrdertype(), "Application-" + storable.getApplication());
      }

      public void injectPrimaryKey(TestCallStatsStorable storable) {
        // nothing to do as pk is part of statistics        
      }
    };
    StorableAggregationStatisticsPersistenceHandler handler = new StorableAggregationStatisticsPersistenceHandler<TestCallStatsStorable>(
                    StatisticsPersistenceStrategy.ASYNCHRONOUSLY, StatisticsPathImpl.fromString("XPCE","Stats","OrderStatistics", "*", "*", "*"), ODSConnectionType.DEFAULT, mapper, generator) {
      
      @Override
      protected ODS getODS() {
        return ODSImpl.getInstance(false);
      }
      
      @Override
      protected RuntimeStatistics getRuntimeStatistics() {
        return rs;
      }
      
    };
    
    Collection<PushStatistics> stats = rs.registerStatisticsPersistenceHandler(handler);
    PushStatistics intStats = null;
    for (PushStatistics pushStatistics : stats) {
      if (pushStatistics.getPath().getPathPart(pushStatistics.getPath().length()-1).getPartName().equals(INT_STATISICS_FINAL_PART_NAME)) {
        intStats = pushStatistics;
        break;
      }
    }
    
    Map<String, Serializable> discovery = rs.discoverStatistics(false);
    assertEquals(4, discovery.size());
    assertEquals(INITIAL_INT_VALUE, discovery.get("XPCE.Stats.OrderStatistics."+testStorable.getOrdertype() + ".Application-" + testStorable.getApplication() + "." + INT_STATISICS_FINAL_PART_NAME));
    
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(testStorable);
    } finally {
      con.closeConnection();
    }
    assertEquals(INITIAL_INT_VALUE, (Integer)testStorable.getInteger());
    
    final Integer PUSHED_VALUE = 123;
    intStats.pushValue(new IntegerStatisticsValue(PUSHED_VALUE));
    
    discovery = rs.discoverStatistics(false);
    assertEquals(4, discovery.size());
    assertEquals(PUSHED_VALUE, discovery.get("XPCE.Stats.OrderStatistics."+testStorable.getOrdertype() + ".Application-" + testStorable.getApplication() + "." + INT_STATISICS_FINAL_PART_NAME));
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(testStorable);
    } finally {
      con.closeConnection();
    }
    assertEquals(INITIAL_INT_VALUE, (Integer)testStorable.getInteger());
    
    Thread.sleep(1200);
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(testStorable);
    } finally {
      con.closeConnection();
    }
    assertEquals(PUSHED_VALUE, (Integer)testStorable.getInteger());
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      Collection<TestCallStatsStorable> collection = con.loadCollection(TestCallStatsStorable.class);
      assertEquals(1, collection.size());
    } finally {
      con.closeConnection();
    }
    
    
    final String NEW_ORDERTYPE = "New";
    final String NEW_APPLICATION = "New";
    StatisticsPath path = StatisticsPathImpl.fromString("XPCE","Stats","OrderStatistics", NEW_ORDERTYPE, "Application-" + NEW_APPLICATION);
    rs.registerStatistic(new PushStatistics<String, StringStatisticsValue>(path.append(StatisticsPathImpl.simplePathPart("Application")),
                                                                           new StringStatisticsValue(NEW_APPLICATION)));
    rs.registerStatistic(new PushStatistics<String, StringStatisticsValue>(path.append(StatisticsPathImpl.simplePathPart("OrderType")),
                                                                           new StringStatisticsValue(NEW_ORDERTYPE)));
    rs.registerStatistic(new PushStatistics<Integer, IntegerStatisticsValue>(path.append(StatisticsPathImpl.simplePathPart(INT_STATISICS_FINAL_PART_NAME)),
                                                                           new IntegerStatisticsValue(111)));
    rs.registerStatistic(new PushStatistics<Long, LongStatisticsValue>(path.append(StatisticsPathImpl.simplePathPart("calls")),
                                                                       new LongStatisticsValue(222L)));
    Thread.sleep(1200);
    
    con = ODSImpl.getInstance(false).openConnection(ODSConnectionType.DEFAULT);
    try {
      Collection<TestCallStatsStorable> collection = con.loadCollection(TestCallStatsStorable.class);
      assertEquals(2, collection.size());
    } finally {
      con.closeConnection();
    }
  }
  
  
  public void testForeignStatisticsStore() throws PersistenceLayerException {
    final List<TestCallStatsStorable> backingstore = new ArrayList<TestCallStatsStorable>();
    
    Map<String, StatisticsPathPart> mapping = new HashMap<String, StatisticsPathPart>();
    mapping.put(TestCallStatsStorable.COL_ORDERTYPE, StatisticsPathImpl.simplePathPart(TestCallStatsStorable.COL_ORDERTYPE));
    mapping.put(TestCallStatsStorable.COL_APPLICATION, StatisticsPathImpl.simplePathPart(TestCallStatsStorable.COL_APPLICATION));
    mapping.put(TestCallStatsStorable.COL_INTEGER, StatisticsPathImpl.simplePathPart(TestCallStatsStorable.COL_INTEGER));
    mapping.put(TestCallStatsStorable.COL_LOONG, StatisticsPathImpl.simplePathPart(TestCallStatsStorable.COL_LOONG));
    ForeignDataStore<TestCallStatsStorable> statisticsStore = new ForeignDataStore<RuntimeStatisticsTest.TestCallStatsStorable>(mapping) {
      @Override
      public Object getKey(TestCallStatsStorable holder) {
        return holder.application + holder.ordertype;
      }

      @Override
      public StatisticsPath getPathToHolder(TestCallStatsStorable holder) {
        return StatisticsPathImpl.fromString("test","Foreign", "Statistics","Store",holder.getOrdertype(),holder.getApplication());
      }

      @Override
      public Serializable getValueFromHolder(StatisticsPath path) {
        TestCallStatsStorable holder = store.get((path.getPathPart(5).getPartName() + path.getPathPart(4).getPartName()));
        if (path.getPathPart(6).getPartName().equals(TestCallStatsStorable.COL_ORDERTYPE)) {
          return holder.ordertype;
        } else if (path.getPathPart(6).getPartName().equals(TestCallStatsStorable.COL_APPLICATION)) {
          return holder.application;
        } else if (path.getPathPart(6).getPartName().equals(TestCallStatsStorable.COL_INTEGER)) {
          return holder.integer;
        } else if (path.getPathPart(6).getPartName().equals(TestCallStatsStorable.COL_LOONG)) {
          return holder.loong;
        } else {
          throw new RuntimeException("bfdjgklda");
        }
      }

      @Override
      public Collection<TestCallStatsStorable> reload() {
        return backingstore;
      }
      
      @Override
      protected FactoryRuntimeStatistics getRuntimeStatistics() {
        return rs;
      }
    };
    
    backingstore.add(new TestCallStatsStorable("ordertype1", "app1", 1, 1L));
    backingstore.add(new TestCallStatsStorable("ordertype2", "app1", 1, 2L));
    backingstore.add(new TestCallStatsStorable("ordertype1", "app2", 1, 3L));
    backingstore.add(new TestCallStatsStorable("ordertype2", "app2", 1, 4L));
    
    Map<String, Serializable> discovery = rs.discoverStatistics(false);
    assertEquals(0, discovery.size());
    statisticsStore.refresh();
    discovery = rs.discoverStatistics(false);
    assertEquals(backingstore.size() * 4, discovery.size());
    
    TestCallStatsStorable removedEntry = backingstore.remove(0);
    statisticsStore.refresh();
    discovery = rs.discoverStatistics(false);
    assertEquals(backingstore.size() * 4, discovery.size());
    for (Entry<String, Serializable> entry : discovery.entrySet()) {
      if (entry.getKey().endsWith(TestCallStatsStorable.COL_LOONG)) {
        assertFalse(entry.getValue().equals(removedEntry.getLoong()));
      }
    }
    backingstore.add(new TestCallStatsStorable(removedEntry.ordertype, removedEntry.application, 2, removedEntry.loong));
    statisticsStore.refresh();
    discovery = rs.discoverStatistics(false);
    assertEquals(backingstore.size() * 4, discovery.size());
  }
  
  
  private static class StaticStatistics<V extends Serializable> extends Statistics<V, StatisticsValue<V>> {

    private StatisticsValue<V> value;
    
    public StaticStatistics(StatisticsPath path, V staticValue) {
      super(path);
      value = new StaticStatisticsValue<V>(staticValue);
    }

    @Override
    public StatisticsValue<V> getValueObject() {
      return value;
    }

    @Override
    public String getDescription() {
      return "";
    }

  }
  
  
  private static class DummyStats extends Statistics {

    public DummyStats(String path) {
      super(StatisticsPathImpl.fromString(path.split("\\.")));
    }

    @Override
    public StatisticsValue getValueObject() {
      return new StringStatisticsValue(Long.toString(new Random().nextLong()));
    }

    @Override
    public String getDescription() {
      return "";
    }
    
  }
  
  
  private Map<String, PushStatistics<Long, LongStatisticsValue>> callStatsCallByOrdertype = new HashMap<String, PushStatistics<Long, LongStatisticsValue>>();
  
  private PushStatistics<Long, LongStatisticsValue> getCallStatsCallStatistics(String ordertype) {
    PushStatistics<Long, LongStatisticsValue> callStatsCalls = callStatsCallByOrdertype.get(ordertype);
    if (callStatsCalls == null) {
      callStatsCalls = new PushStatistics<Long, LongStatisticsValue>(getCallStatsCallStatisticsPath(ordertype)) {
        protected RuntimeStatistics getRuntimeStatistics() {return rs;};
      };
      callStatsCallByOrdertype.put(ordertype, callStatsCalls);
    }
    return callStatsCalls;
  }
  
  private StatisticsPath getCallStatsCallStatisticsPath(String ordertype) {
    return PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(new StatisticsPathImpl.SimplePathPart("calls")).append(new StatisticsPathImpl.SimplePathPart(ordertype));
  }
  
  
  private Map<String, PushStatistics<Long, LongStatisticsValue>> callStatsFinishedByOrdertype = new HashMap<String, PushStatistics<Long, LongStatisticsValue>>();
  
  private PushStatistics<Long, LongStatisticsValue> getCallStatsFinishedStatistics(String ordertype) {
    PushStatistics<Long, LongStatisticsValue> callStatsFinished = callStatsFinishedByOrdertype.get(ordertype);
    if (callStatsFinished == null) {
      callStatsFinished = new PushStatistics<Long, LongStatisticsValue>(getCallStatsFinishedStatisticsPath(ordertype)) {
        protected RuntimeStatistics getRuntimeStatistics() {return rs;};
      };
      callStatsFinishedByOrdertype.put(ordertype, callStatsFinished);
    }
    return callStatsFinished;
  }
  
  private StatisticsPath getCallStatsFinishedStatisticsPath(String ordertype) {
    return PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(new StatisticsPathImpl.SimplePathPart("finished")).append(new StatisticsPathImpl.SimplePathPart(ordertype));
  }
  
  
  private StatisticsAggregator<LongStatisticsValue, LongStatisticsValue> getAggregatedFailedStats() {
    
    StatisticsReducer<LongStatisticsValue> sumOverOrdertypesReducer = new StatisticsReducer<LongStatisticsValue>() {
      public Collection<LongStatisticsValue> reduce(Collection<LongStatisticsValue> in) {
        long result = 0; 
        for (LongStatisticsValue longStatisticsValue : in) {
          result += longStatisticsValue.getValue();
        }
        Collection<LongStatisticsValue> resultCol = new ArrayList<LongStatisticsValue>();
        resultCol.add(new LongStatisticsValue(result));
        return resultCol;
      }
    };
    
    StatisticsAggregator<LongStatisticsValue, LongStatisticsValue> singleValueAggregation = 
      new StatisticsAggregator<LongStatisticsValue, LongStatisticsValue>(StatisticsPathImpl.ALL,
                      PredefinedStatisticsMapper.DIRECT.typeCast(LongStatisticsValue.class, LongStatisticsValue.class), sumOverOrdertypesReducer);
    
    
    StatisticsMapper<LongStatisticsValue, CallsStatStatisticValue> mapCallstatType = new StatisticsMapper<LongStatisticsValue, CallsStatStatisticValue>() {
      public CallsStatStatisticValue map(LongStatisticsValue in, String nodename) {
        return new CallsStatStatisticValue(nodename, in.getValue());
      }
    };
    
    StatisticsReducer<CallsStatStatisticValue> substractFinishedFromCalls = new StatisticsReducer<CallsStatStatisticValue>() {
      public Collection<CallsStatStatisticValue> reduce(Collection<CallsStatStatisticValue> in) {
        long result = 0;
        for (CallsStatStatisticValue statisticsValue : in) {
          if (statisticsValue.getValue().getFirst().equals("calls")) {
            result += statisticsValue.getValue().getSecond();
          } else {
            result -= statisticsValue.getValue().getSecond();
          }
        }
        final Long finalResult = result;
        Collection<CallsStatStatisticValue> returnValue = new ArrayList<CallsStatStatisticValue>();
        returnValue.add(new CallsStatStatisticValue(PredefinedXynaStatisticsPathPart.ORDERSTATS.getPartName(), finalResult));
        return returnValue;
      }
    };
    
    StatisticsAggregator<LongStatisticsValue, CallsStatStatisticValue> differenceReduction = 
      new StatisticsAggregator<LongStatisticsValue, CallsStatStatisticValue>(StatisticsPathImpl.ALL,
                      mapCallstatType, substractFinishedFromCalls);
    
    
    StatisticsMapper<CallsStatStatisticValue, LongStatisticsValue> reduceToLong = new StatisticsMapper<CallsStatStatisticValue, LongStatisticsValue>() {

      public LongStatisticsValue map(CallsStatStatisticValue in, String nodename) {
        return new LongStatisticsValue(in.getValue().getSecond());
      }
    };
    
    
    StatisticsAggregator<CallsStatStatisticValue, LongStatisticsValue> cleanup = 
      new StatisticsAggregator<CallsStatStatisticValue, LongStatisticsValue>(StatisticsPathImpl.ALL,
                      reduceToLong, PredefinedStatisticsReducer.NO_REDUCTION.typeCast(LongStatisticsValue.class));
    
    
    cleanup.setNextAggregationPart(differenceReduction);
    differenceReduction.setNextAggregationPart(singleValueAggregation);
    
    StatisticsPath pathToIncompleteStack = new StatisticsPathImpl(PredefinedXynaStatisticsPath.ORDERSTATISTICS.getPathPart(0), PredefinedXynaStatisticsPath.ORDERSTATISTICS.getPathPart(1), PredefinedXynaStatisticsPath.ORDERSTATISTICS.getPathPart(2));
    
    return (StatisticsAggregator<LongStatisticsValue, LongStatisticsValue>)AggregationStatisticsFactory.completeAggregationStack(pathToIncompleteStack, cleanup);
  }
  
  private static class CallsStatStatisticValue implements StatisticsValue<SerializablePair<String, Long>> {

    private SerializablePair<String, Long> value;
    
    CallsStatStatisticValue(String s, Long l) {
      value = new SerializablePair<String, Long>(s, l);
    }
    
    public SerializablePair<String, Long> getValue() {
      return value;
    }

    public StatisticsValue<SerializablePair<String, Long>> deepClone() {
      return new CallsStatStatisticValue(value.getFirst(), value.getSecond());
    }

    public void merge(StatisticsValue<SerializablePair<String, Long>> otherValue) { }
    
  }
  
  
  private static class StaticStatisticsValue<T extends Serializable> implements StatisticsValue<T> {

    private T value;
    
    public StaticStatisticsValue(T value) {
      this.value = value;
    }
    
    public T getValue() {
      return value;
    }

    public void merge(T otherValue) {
      if (otherValue instanceof String) {
        
      } else if (otherValue instanceof Number) {
        value = (T) new Long(((Number)value).longValue() + ((Number)otherValue).longValue());
      } else {
        throw new UnsupportedOperationException();
      }
    }
    
    public StatisticsValue<T> deepClone() {
      return new StaticStatisticsValue(value);
    }

    public void merge(StatisticsValue<T> otherValue) {
      T otherValueValue = otherValue.getValue();
      if (otherValueValue instanceof String) {
        
      } else if (otherValueValue instanceof Number) {
        value = (T) new Long(((Number)value).longValue() + ((Number)otherValueValue).longValue());
      } else {
        throw new UnsupportedOperationException();
      }
    }
    
  }
  
  
  private static class AggregatedTestStatistics<T extends Serializable, O extends StatisticsValue<T>> extends AggregatedStatistics<T, O> {

    private RuntimeStatistics rs;
    
    public AggregatedTestStatistics(StatisticsPath path,
                                    StatisticsAggregator<? extends StatisticsValue<?>, O> aggregation, RuntimeStatistics rs) {
      super(path, aggregation);
      this.rs = rs;
    }
    
    @Override
    protected RuntimeStatistics getRuntimeStatistics() {
      return rs;
    }
    
  }
  
  
  @Persistable(primaryKey = TestCallStatsStorable.COL_LOONG, tableName = TestCallStatsStorable.TABLENAME, tableProperties = {StorableProperty.PROTECTED})
  public static class TestCallStatsStorable extends Storable<TestCallStatsStorable> {

    public static final String TABLENAME = "callstats";
    public static final String COL_ORDERTYPE = "ordertype";
    public static final String COL_APPLICATION = "application";
    public static final String COL_INTEGER = "integer";
    public static final String COL_LOONG = "loong";
    
    @Column(name = COL_ORDERTYPE)
    private String ordertype;
    @Column(name = COL_APPLICATION)
    private String application;
    @Column(name = COL_INTEGER)
    private int integer = 0;
    @Column(name = COL_LOONG)
    private long loong = 0;
    
    public TestCallStatsStorable() { }
    
    public TestCallStatsStorable(String ordertype, String application, int integer, long loong) {
      this.ordertype = ordertype;
      this.application = application;
      this.integer = integer;
      this.loong = loong;
    }
    
    @Override
    public ResultSetReader<? extends TestCallStatsStorable> getReader() {
      return new ResultSetReader<TestCallStatsStorable>() {

        public TestCallStatsStorable read(ResultSet rs) throws SQLException {
          return new TestCallStatsStorable(rs.getString(COL_ORDERTYPE),
                                           rs.getString(COL_APPLICATION),
                                           rs.getInt(COL_INTEGER),
                                           rs.getLong(COL_LOONG));
        }
      }; 
    }
    
    public String getOrdertype() {
      return ordertype;
    }
    
    public String getApplication() {
      return application;
    }

    public int getInteger() {
      return integer;
    }
    
    public long getLoong() {
      return loong;
    }

    @Override
    public Object getPrimaryKey() {
      return loong;
    }

    @Override
    public <U extends TestCallStatsStorable> void setAllFieldsFromData(U data) {
      TestCallStatsStorable cast = data;
      this.ordertype = cast.ordertype;
      this.application = cast.application;
      this.integer = cast.integer;
      this.loong = cast.loong;
    }
    
  }
  
  
}

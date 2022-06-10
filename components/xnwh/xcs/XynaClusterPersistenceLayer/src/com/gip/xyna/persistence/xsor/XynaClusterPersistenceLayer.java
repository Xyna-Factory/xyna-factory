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
package com.gip.xyna.persistence.xsor;



import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.debug.XSORDebuggingInterface;
import com.gip.xyna.persistence.xsor.helper.ClusterPLLogger;
import com.gip.xyna.persistence.xsor.indices.IndexDefinitionFactory;
import com.gip.xyna.persistence.xsor.indices.StorableBasedIndexDefinitionFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.XSORClusterProvider;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xsor.Table;
import com.gip.xyna.xsor.XynaScalableObjectRepositoryImpl;
import com.gip.xyna.xsor.XynaScalableObjectRepositoryInterface;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.protocol.XSORMemory;



public class XynaClusterPersistenceLayer implements PersistenceLayer, Clustered {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaClusterPersistenceLayer.class);
  ClusterPLLogger sqlLogger;

  public static final String PROP_MAXTABLESIZE = "table.max.size";
  
  private final static String STATISTICS_PATH_PART_XNWH = "xnwh";
  private final static String STATISTICS_PATH_PART_XSOR = "xsor";
  private final static String STATISTICS_PATH_PART_MAX_SIZE = "maxSize";
  private final static String STATISTICS_PATH_PART_CURRENT_SIZE = "currentSize";

  private long clusterInstanceId = -1;
  private XynaScalableObjectRepositoryInterface xsor;
  private IndexDefinitionFactory indexDefinitionFactory = new StorableBasedIndexDefinitionFactory();
  private String namesuffix;
  private Map<String, Class<? extends Storable>> registeredStorables = new HashMap<String, Class<? extends Storable>>();
  private Map<String, SearchRequest> registeredPrimaryKeyRequests = new HashMap<String, SearchRequest>();
  private Map<String, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>> registeredIndexDefinitions =
                      new HashMap<String, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>>();
  
  
  public boolean describesSamePhysicalTables(PersistenceLayer arg0) {
    return false;
  }


  public PersistenceLayerConnection getConnection() throws PersistenceLayerException {
    return new XynaClusterPersistenceLayerConnection(this);
  }


  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return new XynaClusterPersistenceLayerConnection(this);
  }


  private ThreadPoolExecutor threadpool = new ThreadPoolExecutor(1, 3, 20, TimeUnit.SECONDS,
                                                                 new LinkedBlockingDeque<Runnable>(100));


  public Reader getExtendedInformation(String[] args) {
    if (args != null && args.length > 0) {
      if (args[0].equals("help")) {
        return new StringReader("valid commands:\nview all\nview <pk hex>, example: view ab:12:33:f0\nview queuestate\ncheck");
      }
      if (args[0].equals("view")) {
        if (args.length > 1) {
          if (args[1].equals("all")) {
            return infoAll();
          } else if (args[1].equals("queuestate")) {
            return infoQueue();
          } else if (args[1].equals("freeliststate")) {
            return infoFreeList();
          } else {
            byte[] pk = parseFormattedHexString(args[1]);
            return infoObject(pk);
          }
        }
      } else if (args[0].equals("check")) {
        return checkIntegrity(); // add possibilty to invoke with ,-seperated tablenames
      }
    }
    return new StringReader("try help");
  }


  private static byte[] parseFormattedHexString(String hexString) {
    String[] parts = hexString.split(":");
    byte[] b = new byte[parts.length];
    for (int i = 0; i < parts.length; i++) {
      b[i] = (byte) Integer.parseInt(parts[i], 16);
    }
    return b;
  }
  
  
  public void registerTableStatistics(final String tableName) {
    Table table = ((XynaScalableObjectRepositoryImpl)getXynaScalableObjectRepository()).getByTableName(tableName);
    if (table == null) {
      return;
    }
    final XSORMemory xsorMemory = table.getXSORMemory();
    final int tableSize = table.getSize();
    
    StatisticsPath basePath = getBasePath(tableName);

    PullStatistics<Integer, IntegerStatisticsValue> maxTableSizeStatistics =
        new PullStatistics<Integer, IntegerStatisticsValue>(basePath.append(STATISTICS_PATH_PART_MAX_SIZE)) {
          @Override
          public IntegerStatisticsValue getValueObject() {
            return new IntegerStatisticsValue(tableSize);
          }
          @Override
          public String getDescription() {
            return "Maximum table size of the xsor table '" + tableName + "'.";
          }
        };
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(maxTableSizeStatistics);
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    } catch (XFMG_StatisticAlreadyRegistered e) {
    }
    
    PullStatistics<Integer, IntegerStatisticsValue> currentTableSizeStatistics =
        new PullStatistics<Integer, IntegerStatisticsValue>(basePath.append(STATISTICS_PATH_PART_CURRENT_SIZE)) {
          @Override
          public IntegerStatisticsValue getValueObject() {
            return new IntegerStatisticsValue(xsorMemory.getCurrentSize());
          }
          @Override
          public String getDescription() {
            return "Current table size of the xsor table '" + tableName + "'.";
          }
        };
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(currentTableSizeStatistics);
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    } catch (XFMG_StatisticAlreadyRegistered e) {
    }
  }
  
  
  public void unregisterTableStatistics(final String tableName) {
    StatisticsPath basePath = getBasePath(tableName);
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().unregisterStatistic(basePath.append(STATISTICS_PATH_PART_MAX_SIZE));
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().unregisterStatistic(basePath.append(STATISTICS_PATH_PART_CURRENT_SIZE));
    } catch (XFMG_InvalidStatisticsPath e) {
    }
  }
                                             
  
  private static StatisticsPath getBasePath(String tableName) {
    return new StatisticsPathImpl(STATISTICS_PATH_PART_XNWH, STATISTICS_PATH_PART_XSOR, tableName);
  }


  private Reader infoObject(final byte[] pk) {
    final XSORDebuggingInterface debugging = (XSORDebuggingInterface) xsor;
    PipedReader reader = new PipedReader();
    final PipedWriter writer;
    try {
      writer = new PipedWriter(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    threadpool.execute(new Runnable() {

      public void run() {
        BufferedWriter w = new BufferedWriter(writer);
        try {
          debugging.listObject(w, pk);
        } catch (Throwable t) {
          logger.error(null, t);
          try {
            w.close();
          } catch (IOException e) {
          }
        }
      }

    });
    return reader;
  }


  private Reader infoQueue() {
    final XSORDebuggingInterface debugging = (XSORDebuggingInterface) xsor;
    PipedReader reader = new PipedReader();
    final PipedWriter writer;
    try {
      writer = new PipedWriter(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    threadpool.execute(new Runnable() {

      public void run() {
        BufferedWriter w = new BufferedWriter(writer);
        try {
          debugging.listQueueState(w);
        } catch (Throwable t) {
          logger.error(null, t);
          try {
            w.close();
          } catch (IOException e) {
          }
        }
      }

    });

    return reader;
  }
  
  
  private Reader infoFreeList() {
    final XSORDebuggingInterface debugging = (XSORDebuggingInterface) xsor;
    PipedReader reader = new PipedReader();
    final PipedWriter writer;
    try {
      writer = new PipedWriter(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    threadpool.execute(new Runnable() {

      public void run() {
        BufferedWriter w = new BufferedWriter(writer);
        try {
          debugging.listFreeListState(w);
        } catch (Throwable t) {
          logger.error(null, t);
          try {
            w.close();
          } catch (IOException e) {
          }
        }
      }

    });

    return reader;
  }
  
  private static class ClosingPipedReader extends PipedReader {

    private PipedWriter writer;
    
    @Override
    public void close() throws IOException {
      super.close();
      if (writer != null) {
        writer.close();
      }
    }
    
  }


  private Reader infoAll() {
    final XSORDebuggingInterface debugging = (XSORDebuggingInterface) xsor;
    ClosingPipedReader reader = new ClosingPipedReader();
    final PipedWriter writer;
    try {
      writer = new PipedWriter(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    reader.writer = writer;
    threadpool.execute(new Runnable() {

      public void run() {
        BufferedWriter w = new BufferedWriter(writer);
        try {
          debugging.listAllObjects(w);
        } catch (Throwable t) {
          logger.error(null, t);
          try {
            w.close();
          } catch (IOException e) {
          }
        }
      }

    });
    return reader;
  }
  
  
  private Reader checkIntegrity() {
    final XSORDebuggingInterface debugging = (XSORDebuggingInterface) xsor;
    PipedReader reader = new PipedReader();
    final PipedWriter writer;
    try {
      writer = new PipedWriter(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    threadpool.execute(new Runnable() {

      public void run() {
        BufferedWriter w = new BufferedWriter(writer);
        try {
          debugging.checkPrimaryKeyIndexIntegrity(w, new String[0]);
          w.flush();
          w.close();
        } catch (Throwable t) {
          logger.error(null, t);
          try {
            w.close();
          } catch (IOException e) {
          }
        }
      }

    });
    return reader;
  }


  public String getInformation() {
    return "XynaClusterPersistenceLayer";
  }

  private static final String DYNAMIC_LOGGING = "dynamiclogging";

  public String[] getParameterInformation() {
    return new String[] {
        "name: unique between all clusterpersistencelayers.",
        "optional key=value pairs: " + DYNAMIC_LOGGING + " (true/false)",
        //FIXME andere methode?
        "\n--- properties for register table:" + "\n    + " + PROP_MAXTABLESIZE
            + "=<maximum number of entries allowed in table>"};
  }


  public void init(Long arg0, String... args) throws PersistenceLayerException {
    if (args == null || args.length == 0) {
      throw new XNWH_GeneralPersistenceLayerException("expected 1 parameter");
    }
    namesuffix = args[0];

    boolean useDynamicLogging = true;
    if (args.length > 1) {
      for (int i = 1; i<args.length; i++) {
        String[] split = args[i].split("=");
        if (split.length != 2) {
          throw new XNWH_GeneralPersistenceLayerException("invalid key/value pair: " + args[i]);
        }
        if (split[0].trim().equalsIgnoreCase(DYNAMIC_LOGGING)) {
          if (!Boolean.valueOf(split[1])) {
            useDynamicLogging = false;
          }
          logger.info(DYNAMIC_LOGGING + " = " + useDynamicLogging);
        }
      }
    }
    sqlLogger = new ClusterPLLogger(useDynamicLogging);

    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException(e);
    }
  }


  public void shutdown() throws PersistenceLayerException {
    threadpool.shutdown();
  }


  public void disableClustering() {
    clusterInstanceId = -1;
  }


  public void enableClustering(long arg0) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    clusterInstanceId = arg0;
    ClusterProvider provider =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
            .getClusterInstance(clusterInstanceId);

    ClassLoader cl = getClass().getClassLoader();
    if (cl instanceof ClassLoaderBase) {
      ClassLoader clusterProviderClassLoader = provider.getClass().getClassLoader();
      if (clusterProviderClassLoader instanceof ClassLoaderBase) {
        ClassLoaderBase clb = (ClassLoaderBase) cl;
        ClassLoaderBase clbClusterProvider = (ClassLoaderBase) clusterProviderClassLoader;
        clb.addWeakReferencedParentClassLoader(clbClusterProvider);
      }
    }

    if (!(provider instanceof XSORClusterProvider)) {
      throw new RuntimeException();
    }
    XSORClusterProvider xprovider = (XSORClusterProvider) provider;
    xsor = xprovider.getXynaScalableObjectRepository();
  }


  public long getClusterInstanceId() {
    return clusterInstanceId;
  }


  public String getName() {
    return XynaClusterPersistenceLayer.class.getSimpleName() + "_" + namesuffix;
  }


  public boolean isClustered() {
    return clusterInstanceId != -1;
  }


  public XynaScalableObjectRepositoryInterface getXynaScalableObjectRepository() {
    if (xsor == null) {
      throw new RuntimeException(XynaClusterPersistenceLayer.class.getSimpleName() + " as a clusterable component is not configured properly for cluster yet.");
    }
    return xsor;
  }


  public IndexDefinitionFactory getIndexDefinitionFactory() {
    return indexDefinitionFactory;
  }


  void registerStorable(String tablename, Class<? extends Storable> storableClazz) {
    registeredStorables.put(tablename, storableClazz);
  }


  Class<? extends Storable> getStorableClazzForTable(String tablename) {
    Class<? extends Storable> c = registeredStorables.get(tablename);
    if (c == null) {
      throw new RuntimeException("Storable " + tablename + " is not registered.");
    }
    return c;
  }
  
  
  void registerPrimaryKeyRequest(String tablename, SearchRequest searchRequest) {
    registeredPrimaryKeyRequests.put(tablename, searchRequest);
  }


  SearchRequest getRegisteredPrimaryKeyRequestForTable(String tablename) {
    return registeredPrimaryKeyRequests.get(tablename);
  }
  
  
  void registerIndexDefinitions(String tableName, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions) {
    getXynaScalableObjectRepository().addIndices(tableName, indexDefinitions);
    registeredIndexDefinitions.put(tableName, indexDefinitions);
  }


  List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> getRegisteredIndexDefinitions(String tableName) {
    return registeredIndexDefinitions.get(tableName);
  }


  public void unregisterTable(String tableName) {
    registeredIndexDefinitions.remove(tableName);
    registeredPrimaryKeyRequests.remove(tableName);
    registeredStorables.remove(tableName);
  }


  @Override
  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return getConnection();
  }


  @Override
  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (plc instanceof XynaClusterPersistenceLayer) {
      return true;
    }
    return false;
  }


}

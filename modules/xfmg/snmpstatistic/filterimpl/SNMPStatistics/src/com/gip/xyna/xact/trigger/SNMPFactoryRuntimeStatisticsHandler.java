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

package com.gip.xyna.xact.trigger;



import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;



public class SNMPFactoryRuntimeStatisticsHandler extends AbstractSNMPStatisticsHandler {

  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPFactoryRuntimeStatisticsHandler.class);

  private static final Pattern BASE_PATTERN = Pattern.compile("^_Base\\s*=\\s*([\\d\\.\\*]*)", Pattern.MULTILINE);
  private static final Pattern MAPPING_PATTERN = Pattern.compile("^([\\w\\.\\*-]*?)\\s*=\\s*([\\d\\.\\*]*)", 
                                                                 Pattern.MULTILINE);
  private static final Pattern TABLE_MAPPING_PATTERN = Pattern.compile("([^\\*]*)\\*(.*)");

  private final FactoryRuntimeStatistics xynaStatistics = XynaFactory.getInstance().getFactoryManagement()
      .getXynaFactoryMonitoring().getFactoryRuntimeStatistics();

  private Map<String, OID> mapEscapedPathToOID = new TreeMap<String, OID>();
  private Map<OID, String> mapOIDToEscapedPath = new TreeMap<OID, String>();

  private Map<NamePrefixAndSuffix, Pattern> prefixAndSuffixPatterns = new HashMap<NamePrefixAndSuffix, Pattern>();
  private OID baseOID = new OID("1.3.6.1.4.1.28747.1.11"); // TODO aus konstanten zusammenbauen -

  private AtomicLong lastUpdateTime = new AtomicLong(0L);
  private LazyAlgorithmExecutor<Algorithm> mapUpdaterExecutor;
  private static final String UPDATER_THREAD_NAME = "SNMP Statistics Updater";


  public SNMPFactoryRuntimeStatisticsHandler(final SNMPTrigger trigger) {
    super(null);
    trigger.executeWhenTriggerIsInitialized(new Runnable() {
      
      public void run() {
        StringBuilder sb = new StringBuilder(UPDATER_THREAD_NAME);
        if (trigger != null && trigger.getStartParameter() != null) {
          sb.append(" (port ").append(trigger.getStartParameter().getPort()).append(") ");
        }
        mapUpdaterExecutor =
            new LazyAlgorithmExecutor<Algorithm>(sb.toString(), 10000) {
              @Override
              public void onFatalError(Throwable t) {
                if (t instanceof InterruptedException) {
                  logger.debug(UPDATER_THREAD_NAME + " Thread got interrupted.");
                } else {
                  logger.warn("Statistics updater thread died, please redeploy statistics filter", t);
                }
              }
            };

        mapUpdaterExecutor.startNewThread(new MapUpdateAlgorithm(SNMPFactoryRuntimeStatisticsHandler.this));
        mapUpdaterExecutor.requestExecution();
      }
    });
    

  }


  private static class MapUpdateAlgorithm implements Algorithm {
    private final SNMPFactoryRuntimeStatisticsHandler parent;
    public MapUpdateAlgorithm(SNMPFactoryRuntimeStatisticsHandler parentReference) {
      this.parent = parentReference;
    }
    public void exec() {

      // update at most every 60 seconds to provide faster feedback in walks
      long time = System.currentTimeMillis();
      if (time - parent.lastUpdateTime.get() < 60000L) {
        return;
      }

      boolean gotLock = OIDManagement.getInstance().getOidManagementLock();
      if (!gotLock) {
        return;
      }

      try {

        if (logger.isDebugEnabled()) {
          logger.debug("Checking for new statistics paths and creating OID mappings if necessary.");
        }

        SortedMap<OID, String> managedOidMap = OIDManagement.getInstance().getMapForScope(parent.baseOID);
        Map<String, Serializable> stats = parent.xynaStatistics.discoverStatistics(true);

        parent.updateOIDMappings(managedOidMap, stats);

        Map<String, OID> mapEscapedPathToOidNew = new TreeMap<String, OID>();
        Map<OID, String> mapOIDToEscapedPathNew = new TreeMap<OID, String>();
        for (Entry<OID, String> e : managedOidMap.entrySet()) {
          mapEscapedPathToOidNew.put(e.getValue(), e.getKey());
          mapOIDToEscapedPathNew.put(e.getKey(), e.getValue());
        }

        parent.mapEscapedPathToOID = mapEscapedPathToOidNew;
        parent.mapOIDToEscapedPath = mapOIDToEscapedPathNew;

        SortedSet<OID> oidWalkNew = new TreeSet<OID>();
        boolean loggedInfoAboutMissingOID = false;
        for (Entry<String, Serializable> e : stats.entrySet()) {
          OID oid = parent.mapEscapedPathToOID.get(e.getKey());
          if (oid != null) {
            oidWalkNew.add(oid);
          } else if (!loggedInfoAboutMissingOID) {
            logger.info("There is at least one statistics path with no corresponding OID: <" + e.getKey()
                + ">. Please check mapping file. Further log output will be suppressed, there may be"
                + " more (see TRACE logging).");
            loggedInfoAboutMissingOID = true;
          } else if (logger.isTraceEnabled()) {
            logger.trace("Found statistics path with no corresponding OID: " + e.getKey());
          }
        }

        parent.setOIDWalk(oidWalkNew);

      } finally {
        OIDManagement.getInstance().returnOidManagementLock();
      }

      // finally update the time
      parent.lastUpdateTime.set(System.currentTimeMillis());

    }
  }


  @Override
  protected void updateMap(int i) {
    mapUpdaterExecutor.requestExecution();
  }


  private void updateOIDMappings(Map<OID, String> managedOidMap, Map<String, Serializable> stats) {

    File mib = getMibFile();
    Matcher mappingMatcher = getMibFileMatcherAndParseBaseOID(mib);

    Map<NamePrefixAndSuffix, Pattern> prefixAndSuffixPatternsNew = new HashMap<NamePrefixAndSuffix, Pattern>();
    Set<String> allNewPaths = obtainAllNewStatisticsPaths(managedOidMap, stats);
    
    while (mappingMatcher.find()) {
      String name = mappingMatcher.group(1);
      String oid = mappingMatcher.group(2);

      Matcher tableNameMappingMatcher = TABLE_MAPPING_PATTERN.matcher(name);
      Matcher tableOIDMappingMatcher = TABLE_MAPPING_PATTERN.matcher(oid);

      boolean tableNameMappingMatcherMatches = tableNameMappingMatcher.matches();
      boolean tableOIDMappingMatcherMatches = tableOIDMappingMatcher.matches();

      if (tableNameMappingMatcherMatches && tableOIDMappingMatcherMatches) {
        if (logger.isTraceEnabled()) {
          logger.trace("Found table : " + mappingMatcher.group(0));
        }
        handleTable(tableNameMappingMatcher, tableOIDMappingMatcher, prefixAndSuffixPatternsNew, allNewPaths);
      } else if (!tableNameMappingMatcherMatches && !tableOIDMappingMatcherMatches) {
        if (logger.isTraceEnabled()) {
          logger.trace("Found static: " + mappingMatcher.group(0));
        }
        OIDManagement.getInstance().setFixedOIDForName(new OID(oid), name);
      } else {
        logger.error("Synthax error in pre-processed MIB file.");
      }
    }

    prefixAndSuffixPatterns = prefixAndSuffixPatternsNew;

  }


  private void handleTable(Matcher tableNameMappingMatcher, Matcher tableOIDMappingMatcher,
                           Map<NamePrefixAndSuffix, Pattern> prefixAndSuffixPatternsNew, Set<String> newPaths) {

    String namePrefix = tableNameMappingMatcher.group(1);
    String nameSuffix = tableNameMappingMatcher.group(2);

    OID oidPrefix = new OID(tableOIDMappingMatcher.group(1));
    OID oidSuffix = new OID(tableOIDMappingMatcher.group(2));

    NamePrefixAndSuffix key = new NamePrefixAndSuffix(namePrefix, nameSuffix);
    Pattern rowNamePattern = prefixAndSuffixPatterns.get(key);
    if (rowNamePattern == null) {
      rowNamePattern = prefixAndSuffixPatternsNew.get(key);
      if (rowNamePattern == null) {
        rowNamePattern = Pattern.compile(namePrefix + "(.*?)" + nameSuffix);
      }
    }
    prefixAndSuffixPatternsNew.put(key, rowNamePattern);

    for (String path : newPaths) {
      Matcher pathMatch = rowNamePattern.matcher(path);
      if (pathMatch.matches()) { // this is a new path where we need a new OID for.
        String rowName = pathMatch.group(1);
        OIDManagement.getInstance().getOidForName(namePrefix, rowName, nameSuffix, oidPrefix, oidSuffix);  
      }
    }
  }


  private Matcher getMibFileMatcherAndParseBaseOID(File mib) {

    String mibContents = null;
    try {
      mibContents = FileUtils.readFileAsString(mib);
    } catch (Ex_FileWriteException e) {
      logger.error("Unable to read from pre-processed MIB file.", e);
      throw new RuntimeException("Failed to read mib file");
    }

    Matcher baseMatcher = BASE_PATTERN.matcher(mibContents);
    while (baseMatcher.find()) {
      baseOID = new OID(baseMatcher.group(1));
    }

    return MAPPING_PATTERN.matcher(mibContents);

  }


  @Override
  public VarBind get(OID oid, int i) {
    // FIXME: this is a cheat as we have no filter that works in that OID scope
    if (0 == oid.compareTo(new OID(".1.3.6.1.4.1.28747.1.1"))) {
      try {
        String value = Updater.getInstance().getFactoryVersion().getString();
        return new StringVarBind(oid.toString(), (String) value);
      } catch (XPRC_VERSION_DETECTION_PROBLEM e) {
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
      } catch (PersistenceLayerException e) {
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
      }
    }

    String path = mapOIDToEscapedPath.get(oid);
    if (path == null) {
      throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }

    Object value = null;
    try {
      StatisticsValue<Serializable> stat = xynaStatistics.getStatisticsValue(StatisticsPathImpl.
                                                                             fromEscapedString(path), true);
      if (stat != null) {
        value = stat.getValue();
      }
    } catch (XFMG_InvalidStatisticsPath e) {
      // should not happen as retrieved paths from factory
    } catch (XFMG_UnknownStatistic e) {
      // could happen when now unregistered
    }
    
    if (value != null) {
      logger.debug("got value " + value + " for key " + path);

      if (value instanceof Integer) {
        return new IntegerVarBind(oid.toString(), (Integer) value);
      } else if (value instanceof Long) {
        long longValue = (Long) value;
        if (longValue > -1) {
          long maximumPossibleValue = 2 * (long) Integer.MAX_VALUE;
          if (longValue > maximumPossibleValue) {
            logger.warn("Value is larger than capacity for unsigned integer field, using maximum possible value");
            longValue = maximumPossibleValue;
          }
          return new UnsIntegerVarBind(oid.toString(), longValue);
        } else {
          logger.warn("Value is smaller than zero but is declared as UNSIGNED INTEGER, using INTEGER instead");
          return new IntegerVarBind(oid.toString(), (Integer) value);
        }
      } else if (value instanceof String) {
        return new StringVarBind(oid.toString(), (String) value);
      } else if (value instanceof Double) {
        Double v = (Double) value;
        if (Math.abs(v) < Integer.MAX_VALUE) {
          return new IntegerVarBind(oid.toString(), (int)Math.round((Double)value));
        } else {
          return new StringVarBind(oid.toString(), String.valueOf(value));
        }
      } else {
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
      }
    } else {
      throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }
  }


  @Override
  protected OID getBase() {
    return baseOID;
  }


  @Override
  public boolean matches(SnmpCommand command, OID oid) {
    if (super.matches(command, oid)) {
      return true;
    } else {
      // FIXME: this is a cheat as we have no filter that works in that OID scope
      if (0 == oid.compareTo(new OID(".1.3.6.1.4.1.28747.1.1"))) {
        return true;
      } else {
        return false;
      }
    }
  }


  private File getMibFile() {
    // TODO : read those from the MIBs
    File pathToFolder =
        new File(SNMPFactoryRuntimeStatisticsHandler.class.getResource("SNMPFactoryRuntimeStatisticsHandler.class")
            .getPath());
    while (pathToFolder.toString().contains(".jar")) {
      pathToFolder = new File(pathToFolder.getParent());
    }

    String pathToMibDefinition = pathToFolder.getPath();
    pathToMibDefinition = pathToMibDefinition.replace("file:", "");
    pathToMibDefinition += "/snmp_mib.cfg";

    return new File(pathToMibDefinition);
  }


  private Set<String> obtainAllNewStatisticsPaths(Map<OID, String> managedOidMap, Map<String, Serializable> stats) {
    SortedMap<OID, String> oldOidMap = OIDManagement.getInstance().getMapForScope(baseOID);
    Set<String> oldPaths = new HashSet<String>(oldOidMap.values());
    Set<String> newPaths = new HashSet<String>(stats.keySet());
    newPaths.removeAll(oldPaths);
    return newPaths;
  }


  public void shutdown() {

    mapUpdaterExecutor.stopThread();

    if (mapUpdaterExecutor.isRunning()) {
      int sleepCounter = 0;
      try {
        while (mapUpdaterExecutor.isRunning() && sleepCounter++ < 100) {
          Thread.sleep(100);
        }
      } catch (InterruptedException e) {
        logger.info("Failed to wait for shutdown of statistics updater thread");
        return;
      }
      if (mapUpdaterExecutor.isRunning()) {
        logger.info("Statistics updater thread could not be stopped after 10 seconds, giving up.");
      }
    }

  }
  
}

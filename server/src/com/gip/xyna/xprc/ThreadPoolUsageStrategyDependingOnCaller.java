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
package com.gip.xyna.xprc;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xprc.XynaThreadPoolExecutor.ThreadPoolUsageStrategy;



public class ThreadPoolUsageStrategyDependingOnCaller implements ThreadPoolUsageStrategy, IPropertyChangeListener {

  private static final Logger logger = CentralFactoryLogging.getLogger(ThreadPoolUsageStrategyDependingOnCaller.class);

  private Map<String, AtomicLong> callerThreadCount = null;
  private Map<String, Long> callerThreadMaxCount = null;
  private XynaThreadPoolExecutor executor;
  private Map<String, XynaPropertyInt> watchedProperties = new HashMap<String, XynaPropertyInt>();
  private Map<String, String> watchedPropertyNames = new HashMap<String, String>();
  private AtomicLong usedThreads = new AtomicLong(0);
  private volatile boolean configurationInitialized;
  private int threeQuarterMax; 

  @Override
  public String toString() {
    return "ThreadPoolUsageStrategyDependingOnCaller(" + "callerThreadCount=" + callerThreadCount
        + ",callerThreadMaxCount=" + callerThreadMaxCount + ",usedThreads=" + usedThreads + ")";
  }


  public ThreadPoolUsageStrategyDependingOnCaller(XynaThreadPoolExecutor executor) {
    this.executor = executor;
    this.callerThreadCount = new HashMap<String, AtomicLong>();
    this.callerThreadMaxCount = new HashMap<String, Long>();
    this.threeQuarterMax = (executor.getMaximumPoolSize() * 3) / 4;
    this.watchedPropertyNames.put(null, XynaProperty.THREADPOOL_PLANNING_MAXTHREADS.getPropertyName());
    
    //TODO mit PropertyChangeListener in XynaProperty unnötig!
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(ThreadPoolUsageStrategyDependingOnCaller.class,"ThreadPoolUsageStrategyDependingOnCaller").
      after(Configuration.class).
      execNowOrAsync(new Runnable() { public void run() { initConfiguration(); }});
  }

  private void initConfiguration() {
    configurationInitialized = true;
    registerPropertyChangeListener();
  }

  public boolean isExecutionPossible(XynaRunnable xynaRunnable) {
    checkAndLazyInitCallerAndReturnCounter(xynaRunnable);
    // determines whether the current distribution of threads allows for creating a new one for the caller
    boolean res = allowNewThreadForCaller(xynaRunnable.getCaller());
    return res;
  }


  public void beforeExecute(XynaRunnable xynaRunnable) {
    checkAndLazyInitCallerAndReturnCounter(xynaRunnable).incrementAndGet();
    usedThreads.incrementAndGet();
  }


  public void afterExecute(XynaRunnable xynaRunnable) {
    usedThreads.decrementAndGet();
    checkAndLazyInitCallerAndReturnCounter(xynaRunnable).decrementAndGet();
  }


  private AtomicLong checkAndLazyInitCallerAndReturnCounter(XynaRunnable xynaRunnable) {
    String caller = xynaRunnable.getCaller();
    AtomicLong counter = callerThreadCount.get(caller);
    if (counter == null) {
      initCaller(caller);
      counter = callerThreadCount.get(caller);
    }
    return counter;
  }


  private synchronized void initCaller(String caller) {
    if (callerThreadMaxCount.containsKey(caller)) {
      return; //doch schon konkurrierend eingetragen
    }
    logger.info("initializing caller " + caller + " " + this);

    XynaPropertyInt xp = createNewProperty(caller);
    watchedProperties.put(caller, xp);
    watchedPropertyNames.put(caller, xp.getPropertyName());

    registerPropertyChangeListener();
    propertyChanged();
    callerThreadCount.put(caller, new AtomicLong(0));
  }


  /**
   * @return
   */
  private XynaPropertyInt createNewProperty(String caller) {
    //TODO Name ist falsch!
    //Beispiel: com.gip.xyna.xprc.XynaExecutor.Planning.XynaFactoryCommandLineInterface
    //besser: xyna.threadpool.planning.max_usage_XynaFactoryCommandLineInterface
    String propertyName = XynaExecutor.class.getName() + ".Planning." + caller;
    XynaPropertyInt xp = new XynaPropertyInt(propertyName,threeQuarterMax).
        setDefaultDocumentation(DocumentationLanguage.EN, generateDoc(caller,DocumentationLanguage.EN) ).
        setDefaultDocumentation(DocumentationLanguage.DE, generateDoc(caller,DocumentationLanguage.DE) );
    xp.registerDependency(XynaExecutor.class.getSimpleName());
    return xp;
  }


  /**
   * TODO mit PropertyChangeListener in XynaProperty unnötig!
   */
  private void registerPropertyChangeListener() {
    if( ! configurationInitialized ) {
      return; //noch nicht ausführen
    }
    //mehrfaches Registrieren gibt Warnungen, daher erst deregistrieren
    Configuration configuration =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
    //if( watchedProperties.size() > 1 ) {
    //  configuration.removePropertyChangeListener(this);
    //}
    configuration.addPropertyChangeListener(this);
  }


  /**
   * @param caller
   * @param en
   * @return
   */
  private String generateDoc(String caller, DocumentationLanguage lang) {
    switch(lang) {
      case DE:
        return "Maximale Anzahl an Threads in Planning-ThreadPool, die durch die Quelle '"
          +caller+"' eingestellt werden dürfen.";
     case EN:
       return "Maximum number of thread in planning thread pool, for source '"+caller+"'.";
    }
    return "Unexpected language "+lang;
  }


  public ArrayList<String> getWatchedProperties() {
    return new ArrayList<String>(watchedPropertyNames.values());
  }


  public void propertyChanged() {
    int maximumPoolSize = executor.getMaximumPoolSize();
    int threeQuarterMaxNew = (maximumPoolSize * 3) / 4;
    if( threeQuarterMaxNew != threeQuarterMax ) {
      threeQuarterMax = threeQuarterMaxNew;
      changePropertyDefaults();
    } 

    for (Map.Entry<String, XynaPropertyInt> entry : watchedProperties.entrySet()) {
      String caller = entry.getKey();
      int propertyValue = entry.getValue().get();
      if( propertyValue == 0 ) {
        propertyValue = threeQuarterMax;
      }
      int maxCount = Math.min(propertyValue, maximumPoolSize);

      if (maxCount != propertyValue) {
        logger.debug("Invalid value for property " +entry.getValue().getPropertyName() + " = " + propertyValue
            + " exceeds maximum pool size of " + maximumPoolSize);
        //TODO korrigierte Property setzen?
      }

      callerThreadMaxCount.put(caller, Long.valueOf(maxCount));
    }
  }


  /**
   * @param threeQuarterMax
   */
  private void changePropertyDefaults() {
    for (Map.Entry<String, XynaPropertyInt> entry : watchedProperties.entrySet()) {
      XynaPropertyInt old = entry.getValue();
      old.unregister();
      XynaPropertyInt xp = createNewProperty(entry.getKey());
      entry.setValue(xp);
    }
  }


  private boolean allowNewThreadForCaller(String caller) {
    long callerUsed = callerThreadCount.get(caller).get();
    if (callerUsed >= callerThreadMaxCount.get(caller).longValue()) {
      return false; //Caller darf nicht mehr Threads als maximal konfiguriert verwenden
    }
    
    int maximumPoolSize = executor.getMaximumPoolSize();
    long used = usedThreads.get();
    if (used <= maximumPoolSize * 9 / 10) {
      //mehr als 10 % der Threads sind frei: Thread-Pool darf durch jeden Caller verwendet werden
      return true;
    }

    String maxCaller = calculateUsage(); //calculateMaxCaller?
    if (!caller.equals(maxCaller)) {
      return true; //Caller ist nicht Hauptverbraucher
    }

    //TODO evtl hier einfach return false;
    boolean complicatedCalc = allowNewThreadForCallerOld(caller); //FIXME noch verbessern?
    if (complicatedCalc) {
      logger.info("allowNewThreadForCallerOld returned true! " + this);
    }
    return complicatedCalc;
  }


  private String calculateUsage() {
    float maxQuota = 0.0f;
    String maxCaller = null;
    for (String str : callerThreadMaxCount.keySet()) {
      float quota = (float) callerThreadCount.get(str).get() / (float) callerThreadMaxCount.get(str).longValue();
      if (quota > maxQuota) {
        maxQuota = quota;
        maxCaller = str;
      }
    }
    return maxCaller;
  }


  private boolean allowNewThreadForCallerOld(String caller) {
    long sum = 0;
    long maxSum = 0;
    float maxQuota = 0.0f;
    String maxCaller = null;

    for (String str : callerThreadMaxCount.keySet()) {
      float quota = (float) callerThreadCount.get(str).get() / (float) callerThreadMaxCount.get(str).longValue();
      sum += callerThreadCount.get(str).get();
      maxSum += callerThreadMaxCount.get(str).longValue();

      if (quota > maxQuota) {
        maxQuota = quota;
        maxCaller = str;
      }
    }

    float maxQuotaFill = maxSum * maxQuota;
    boolean hasFreeThread =
        ((sum < executor.getMaximumPoolSize()) && (callerThreadCount.get(caller).get() < callerThreadMaxCount
            .get(caller).longValue()));

    if (hasFreeThread
        && ((maxQuotaFill < (float) executor.getMaximumPoolSize()) || (sum < executor.getMaximumPoolSize() * 9 / 10) || !caller
            .equals(maxCaller))) {
      return true;
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Thread refused. current thread occupation is :");

        for (Entry<String, AtomicLong> e : callerThreadCount.entrySet()) {
          logger.debug(e.getKey() + " : " + e.getValue());
        }
      }

      return false;
    }
  }

}

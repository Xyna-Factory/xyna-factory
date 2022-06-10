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



import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_FACTORY_IS_SHUTTING_DOWN;



public class IDGenerator extends FunctionGroup {

  protected static Logger logger = CentralFactoryLogging.getLogger(IDGenerator.class);


  private static long ID_OFFSET = 10000;
  @Deprecated  //Use IDGenerator.class
  public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  private volatile static IDGenerator _instance;

  private static ReentrantLock instanceLock = new ReentrantLock();
  private volatile boolean isInitialized;
  private volatile boolean isShutdown;
  public IdGenerationAlgorithm idGenerationAlgorithm;
  
  private static AtomicLong sessionUniqueIdCounter = new AtomicLong();
  
  public static final String DEFAULT_NAME = IDGenerator.class.getSimpleName();

  public static final String REALM_DEFAULT = "default";
  
  /**
   * gibt für diese jvm eindeutige ids. wird beim serverstart resetted.
   * 
   * d.h. nicht global eindeutig, weder über mehrere knoten hinweg noch über neustart hinweg.
   * aber lokal eindeutig, also auf diesem knoten haben mehrfache aufrufe nacheinander immer andere ids.
   */
  public static long generateUniqueIdForThisSession() {
    return sessionUniqueIdCounter.incrementAndGet();
  }

  public static long getID_OFFSET() {
    return ID_OFFSET;
  }
  

  public static IDGenerator getInstance() throws XynaException {
    if (_instance == null) {
      if (instanceLock.isHeldByCurrentThread()) {
        logger.warn(null, new RuntimeException());
      }
      instanceLock.lock();
      try {
        if (_instance == null) {
          _instance = new IDGenerator();
        }
      } finally {
        instanceLock.unlock();
      }
    }
    return _instance;
  }


  /**
   * For testing purposes only!.
   */
  public static void setInstance(IDGenerator newInstance) {
    _instance = newInstance;
  }


  private IDGenerator() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    idGenerationAlgorithm = new IdGenerationAlgorithmUsingBlocksAndClusteredStorable();
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(IDGenerator.class,"IDGenerator.initStorable").
      after(PersistenceLayerInstances.class,XynaClusteringServicesManagement.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() { public void run() { initStorable(); }});
    fExec.addTask(FUTUREEXECUTION_ID,"IDGenerator.setID").deprecated().
      after(IDGenerator.class).
      execAsync();  //dummy zum Setzen von FUTUREEXECUTION_ID
  }
  
  private void initStorable() {
    try {
      ODSImpl.getInstance().registerStorable(GeneratedIDsStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    
    try {
      idGenerationAlgorithm.init();
      isInitialized = true;
      isShutdown = false;

    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void shutdown() throws XynaException {

      if (!isInitialized) {
        return;
      }
      idGenerationAlgorithm.shutdown();
      isShutdown = true;
      isInitialized = false;

  }

  /*
   * ACHTUNG: abwärtskompatibel halten, weil in projekten verwendet
   */
  /**
   * @return eine eindeutige zahl.
   */
  public long getUniqueId() {
    return getUniqueId(REALM_DEFAULT);
  }
  
  
  /**
   * @return a unique id within the realm 
   */
  public long getUniqueId(String realm) {
      if (isShutdown) {
        //dann darf da auch keiner mehr drauf zugreifen. vor dem shutdown muss das sichergestellt werden => runtimeexception
        throw new RuntimeException(new XPRC_FACTORY_IS_SHUTTING_DOWN("Generate unique ID"));
      }
    return idGenerationAlgorithm.getUniqueId(realm);
  }


  public void setBlockSize(String realm, long blockSize) {
    ((IdGenerationAlgorithmUsingBlocksAndClusteredStorable) idGenerationAlgorithm).setBlockSize(realm, blockSize);
  }
  
  /**
   * gibt die als lastStoredId gespeicherte id vom anderen binding/knoten zurück 
   * nur unterstützt, wenn factory geclustered ist
   */
  public long getIdLastUsedByOtherNode(String realm) {
    return idGenerationAlgorithm.getIdLastUsedByOtherNode(realm);
  }
  
  /**
   * speichert die zuletzt vergebene id vom eigenen knoten in der datenbank als lastStoredId
   * nur unterstützt, wenn factory geclustered ist
   */
  public void storeLastUsed(String realm) {
    idGenerationAlgorithm.storeLastUsed(realm);
  }

}

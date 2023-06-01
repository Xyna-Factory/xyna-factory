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
package com.gip.xyna.mock;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.idgeneration.IdGenerationAlgorithm;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivationBase;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xdev.XynaDevelopmentBase;
import com.gip.xyna.xdev.XynaDevelopmentPortal;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.XynaMultiChannelPortalBase;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xnwh.XynaFactoryWarehouseBase;
import com.gip.xyna.xnwh.XynaFactoryWarehousePortal;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal;

public class XynaFactoryMock implements XynaFactoryBase {

  public IDGenerator idGenerator;
  public FutureExecution futureExecution;
  public XynaProcessingBase processing;
  public XynaFactoryManagementBase factoryManagement;
  
  private static boolean odsInitialized = false;
  
  
  public enum Initialize {
    IdGenerator {
      @Override
      public void initialize(XynaFactoryMock xfm) throws XynaException {
        xfm.idGenerator = IDGenerator.getInstance();
        xfm.idGenerator.idGenerationAlgorithm = new IdGenerationAlgorithm() {
          AtomicLong uniqueId = new AtomicLong(1000);
          public void shutdown() throws PersistenceLayerException {
          }
          
          public void init() throws PersistenceLayerException {
          }
          
          public long getUniqueId(String realm) {
            return uniqueId.getAndIncrement();
          }

          @Override
          public long getIdLastUsedByOtherNode(String realm) {
            return 0;
          }

          @Override
          public void storeLastUsed(String realm) {
          }
        };
      }
    }, 
    ODS {
      @Override
      public void initialize(XynaFactoryMock xfm) throws XynaException {
        
        //Achtung: tools.jar muss zur JRE passen! 
        
        xfm.factoryManagement = new XynaFactoryManagementMock();
        xfm.processing = new XynaProcessingMock();
        
        ODS ods = xfm.processing.getXynaProcessingODS().getODS();
        
        if( ! odsInitialized ) {
          ods.registerPersistenceLayer(3, XynaLocalMemoryPersistenceLayer.class);
          
          
          long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
              ODSConnectionType.DEFAULT, new String[0]);
          long id2 = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test2",
              ODSConnectionType.HISTORY, new String[0]);
          long id3 = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test3",
              ODSConnectionType.ALTERNATIVE, new String[0]);
          long id4 = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test4",
              ODSConnectionType.INTERNALLY_USED, new String[0]);
          ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
          ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id2);
          ods.setDefaultPersistenceLayer(ODSConnectionType.ALTERNATIVE, id3);
          ods.setDefaultPersistenceLayer(ODSConnectionType.INTERNALLY_USED, id4);
          
          odsInitialized = true;
        } /* oder 
          protected void tearDown() throws XynaException {
    ODSImpl.getInstance(false).clearPreparedQueryCache();
    ODSImpl.clearInstances();
  }

im Test?


beides hinterlässt viele Einträge in storage/persistence/persistencelayerinstance.xml
        
        */
      }
    }
      
   
    ;
    
    
    //ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    
    public abstract void initialize(XynaFactoryMock xfm) throws XynaException;
  }
  
  public static EnumSet<Initialize> defaultInitSet = EnumSet.of(
      Initialize.IdGenerator, 
      Initialize.ODS
      );
  
  public XynaFactoryMock() throws XynaException {
    
    System.setProperty("exceptions.storage","Exceptions.xml");
    
    //FutureExecution wird immer benötigt
    futureExecution = new FutureExecution("Mock");
    XynaFactory.setInstance( this );
    init();
  }
  
  public void init() throws XynaException {
    initialize(defaultInitSet);
  }
  
  public void init(EnumSet<Initialize> initSet) throws XynaException {
    initialize(initSet);
  }
  
  private void initialize(EnumSet<Initialize> initSet) {
    for( Initialize init : Initialize.values() ) {
      if( initSet.contains(init) ) {
        try {
          init.initialize(this);
        } catch ( Exception e ) {
          e.printStackTrace();
        }
      }
    }
    
    //
    
    futureExecution = new FutureExecution("Mock");
  }
  
  public void startFutureExecution(Object ... afterIds) {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    for( Object id  : afterIds ) {
      fExec.addTask(id, String.valueOf(id)).execAsync( new Runnable() {public void run() {}});
    }
    fExec.finishedRegistrationProcess();
  }




  public XynaFactoryManagementPortal getFactoryManagementPortal() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaActivationPortal getActivationPortal() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaProcessingPortal getProcessingPortal() {
    // TODO Auto-generated method stub
    return null;
  }

  public Channel getXynaMultiChannelPortalPortal() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaMultiChannelPortalSecurityLayer getXynaMultiChannelPortalSecurityLayer() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaDevelopmentPortal getXynaDevelopmentPortal() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaFactoryWarehousePortal getXynaNetworkWarehousePortal() {
    // TODO Auto-generated method stub
    return null;
  }

  

  public void shutdown() {
    // TODO Auto-generated method stub
    
  }

  public void shutdownComponents() throws XynaException {
    // TODO Auto-generated method stub
    
  }

  public XynaFactoryManagementBase getFactoryManagement() {
    return factoryManagement;
  }

  public XynaActivationBase getActivation() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaProcessingBase getProcessing() {
    return processing;
  }

  public XynaMultiChannelPortalBase getXynaMultiChannelPortal() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaDevelopmentBase getXynaDevelopment() {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaFactoryWarehouseBase getXynaNetworkWarehouse() {
    // TODO Auto-generated method stub
    return null;
  }

  public void addComponentToBeInitializedLater(XynaFactoryComponent lateInitComponent) throws XynaException {
    // TODO Auto-generated method stub
    
  }

  public void initLateInitComponents(
      HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> allDependencies) throws XynaException {
    // TODO Auto-generated method stub
    
  }

  public boolean isShuttingDown() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isStartingUp() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean finishedInitialization() {
    // TODO Auto-generated method stub
    return false;
  }

  public IDGenerator getIDGenerator() {
    return idGenerator;
  }

  public FutureExecution getFutureExecution() {
    return futureExecution;
  }

  public FutureExecution getFutureExecutionForInit() {
    return futureExecution;
  }

  public long getBootCntId() {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getBootCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  public boolean lockShutdown(String cause) {
    // TODO Auto-generated method stub
    return false;
  }

  public void unlockShutdown() {
    // TODO Auto-generated method stub
    
  }


 
}

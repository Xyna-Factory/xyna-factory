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
package com.gip.xyna.demon;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.snmp.DemonSnmpExtension;
import com.gip.xyna.demon.worker.BlockingSlavePool;
import com.gip.xyna.demon.worker.Master;
import com.gip.xyna.demon.worker.SlaveInitializer;
import com.gip.xyna.demon.worker.SlavePool;


public class DemonWorkerFactory {
  
  private static final String CORE_POOL = ".worker.threads.core";
  private static final String MAX_POOL = ".worker.threads.max";
  private static final String NAME = ".name";
 
  public static <Tool,Work> DemonWorker createDemonWorker( String demonPrefix, final Master<Tool,Work> master, final SlaveInitializer<Tool> si) {
    int corePoolSize = DemonProperties.getIntProperty(demonPrefix+CORE_POOL);
    int maxPoolSize = DemonProperties.getIntProperty(demonPrefix+MAX_POOL,corePoolSize);
    String demonName = DemonProperties.getProperty(demonPrefix+NAME);
    
    SlavePool<Tool,Work> slavePool = new SlavePool<Tool,Work>(si, corePoolSize, maxPoolSize );
    master.setSlavePool(slavePool);
    
    return new DemonWorkerImpl<Tool,Work>(master,slavePool,demonName,si,null,null);
  }
  
  public static <Tool,Work> DemonWorker createDemonWorker( String demonPrefix, final Master<Tool,Work> master, final SlaveInitializer<Tool> si, DemonSnmpExtension dse, DemonWorkConfigurator dwc) {
    int corePoolSize = DemonProperties.getIntProperty(demonPrefix+CORE_POOL);
    int maxPoolSize = DemonProperties.getIntProperty(demonPrefix+MAX_POOL,corePoolSize);
    String demonName = DemonProperties.getProperty(demonPrefix+NAME);
    
    SlavePool<Tool,Work> slavePool = new SlavePool<Tool,Work>(si, corePoolSize, maxPoolSize );
    master.setSlavePool(slavePool);
    
    return new DemonWorkerImpl<Tool,Work>(master,slavePool,demonName,si,dse,dwc);
  }

  public static <Tool,Work> DemonWorker createBlockingDemonWorker(String demonPrefix, final Master<Tool,Work> master, final SlaveInitializer<Tool> si ) {
    int corePoolSize = DemonProperties.getIntProperty(demonPrefix+CORE_POOL);
    int maxPoolSize = DemonProperties.getIntProperty(demonPrefix+MAX_POOL,corePoolSize);
    String demonName = DemonProperties.getProperty(demonPrefix+NAME);
    
    SlavePool<Tool,Work> slavePool = new BlockingSlavePool<Tool,Work>(si, corePoolSize, maxPoolSize );
    master.setSlavePool(slavePool);
    
    return new DemonWorkerImpl<Tool,Work>(master,slavePool,demonName,si,null,null);
  }
  
  public static class DemonWorkerImpl<Tool,Work> implements DemonWorker {
    static Logger logger = Logger.getLogger(DemonWorkerImpl.class.getName());
    
    private Master<Tool,Work> master;
    private SlavePool<Tool,Work> slavePool;
    private String demonName;
    private SlaveInitializer<Tool> slaveInitializer;
    private DemonSnmpExtension demonSnmpExtension;
    private DemonWorkConfigurator demonWorkConfigurator;

    public DemonWorkerImpl(Master<Tool,Work> master, SlavePool<Tool,Work> slavePool, String demonName, SlaveInitializer<Tool> si, DemonSnmpExtension dse, DemonWorkConfigurator dwc) {
      this.master = master;
      this.slavePool = slavePool;
      this.demonName = demonName;
      this.slaveInitializer = si;
      this.demonSnmpExtension = dse;
      this.demonWorkConfigurator = dwc;
    }

    public void run() {
      
      if( demonWorkConfigurator != null ) {
        demonWorkConfigurator.initialize();
        demonWorkConfigurator.start();
      }
      slaveInitializer.initialize();
      slavePool.startThreadPoolExecutor();
      if( slavePool.isActive() ) {
        master.run(); //sollte nun blockieren
        logger.info("master has stopped");
      }
      //wenn master fertig ist:
      terminate();
    }

    public void terminate() {
      logger.info( "DemonWorker "+demonName+" terminate");
      master.terminate();
      logger.debug( "master terminated");
      slavePool.terminate();
      logger.debug( "slavePool terminated");
      slaveInitializer.terminate();
      logger.debug( "slaveInitializer terminated");
      if( demonWorkConfigurator != null ) {
        demonWorkConfigurator.terminate();
        logger.debug( "demonWorkConfigurator terminated");
      }
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("DemonWorker(");
      sb.append("master=").append(master);
      sb.append(",slavePool=").append(slavePool);
      sb.append(")");
      return sb.toString();
    }

    public String getName() {
      return demonName;
    }

    public void logStatus(Logger statusLogger) {
      master.logStatus(statusLogger);
      slavePool.logStatus(statusLogger);
      slaveInitializer.logStatus(statusLogger);
    }
    
    public void configureDemonSnmp(DemonSnmpConfigurator demonSnmpConfigurator) {
      if( demonSnmpExtension != null ) {
        demonSnmpExtension.configureDemonSnmp(demonSnmpConfigurator);
      }
    }
  
  }

}

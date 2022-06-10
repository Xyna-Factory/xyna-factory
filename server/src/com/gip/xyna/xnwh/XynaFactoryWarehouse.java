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

package com.gip.xyna.xnwh;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xnwh.exceptions.XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistence;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceBase;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.statistics.StatisticsStore;
import com.gip.xyna.xnwh.xclusteringservices.XynaClusteringServices;
import com.gip.xyna.xnwh.xwarehousejobs.XynaWarehouseJobManagement;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;



public class XynaFactoryWarehouse extends XynaFactoryWarehouseBase {

  public static final String DEFAULT_NAME = "XynaFactoryWarehouse";

  public static XynaPropertyEnum<InformIndexCollisions> SHOW_STARTUP_INFORMATION_INDEX_COLLISIONS = 
      new XynaPropertyEnum<InformIndexCollisions>("xnwh.persistence.table.show_startup_information_index_collisions", InformIndexCollisions.class, InformIndexCollisions.always).
          setDefaultDocumentation(DocumentationLanguage.EN, 
            "Show message on factory startup if index collisions exists. (possible values: always, never, onlyMissing, onlyMissingOrChanged)").
          setDefaultDocumentation(DocumentationLanguage.DE,
            "Ausgabe einer Meldung beim Factory-Startup, wenn Index-Collisions aufgetreten sind. (Mögliche Werte: always, never, onlyMissing, onlyMissingOrChanged).");

  public enum InformIndexCollisions {
    always, never, onlyMissing, onlyMissingOrChanged
  }
  
  private ArrayList<XynaOrderServerExtension> shutdownWarehouseJobOrders;
  private ReentrantLock lock = new ReentrantLock();

  private XynaWarehouseJobManagement warehouseJobManagement;
  private XMOMPersistence xmomPersistence;
  private SecureStorage secureStorage;
  private StatisticsStore statisticsStore;
  private ODS ods;

  private XynaClusteringServices xClusteringServices;


  public XynaFactoryWarehouse() throws XynaException {
    super();
  }


  public void init() throws XynaException {

    // TODO this should rather be a functiongroup
    warehouseJobManagement = new XynaWarehouseJobManagement();
    deploySection(warehouseJobManagement);

    // TODO get these from the WarehouseJobManagement
    shutdownWarehouseJobOrders = new ArrayList<XynaOrderServerExtension>();

    // TODO start startup jobs

    secureStorage = SecureStorage.getInstance();
    ods = ODSImpl.getInstance();

    xClusteringServices = new XynaClusteringServices();
    deploySection(xClusteringServices);

    xmomPersistence = new XMOMPersistence();
    deploySection(xmomPersistence);

    statisticsStore = new StatisticsStore();
    deploySection(statisticsStore);
    
    deploySection(ConnectionPoolManagement.getInstance());
    
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(XynaFactoryWarehouse.class,"XynaFactoryWarehouse.initProps").
      after(XynaProperty.class).
      execAsync(new Runnable() { public void run() { initProps(); }});

  }
  
  private void initProps() {
    SHOW_STARTUP_INFORMATION_INDEX_COLLISIONS.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
  }
   


  @Override
  public void shutdown() throws XynaException {

    if (logger.isDebugEnabled()) {
      logger.debug("Executing " + shutdownWarehouseJobOrders.size() + " shutdown jobs...");
    }
    for (XynaOrderServerExtension xo : shutdownWarehouseJobOrders) {
      if (logger.isDebugEnabled()) {
        logger.debug("Executing shutdown job '" + xo.getDestinationKey().getOrderType() + "'...");
      }
      XynaFactory
          .getPortalInstance()
          .getProcessingPortal()
          .startOrderSynchronously(new XynaOrderCreationParameter(xo.getDestinationKey().getOrderType(), xo
                                       .getPriority(), xo.getInputPayload()));
      if (logger.isDebugEnabled()) {
        logger.debug("Finished execution of shutdown job '" + xo.getDestinationKey().getOrderType() + "'");
      }
    }

    super.shutdown();

  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public boolean addShutdownWarehouseJobOrder(XynaOrderServerExtension xo) {
    lock.lock();
    try {
      if (!shutdownWarehouseJobOrders.contains(xo)) {
        shutdownWarehouseJobOrders.add(xo);
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
    }
  }


  public boolean removeShutdownWarehouseJobOrder(XynaOrderServerExtension xo) {
    lock.lock();
    try {
      return shutdownWarehouseJobOrders.remove(xo);
    } finally {
      lock.unlock();
    }
  }


  public SecureStorage getSecureStorage() {
    return secureStorage;
  }


  public boolean store(String destination, String key, Serializable serializable) throws PersistenceLayerException {
    return secureStorage.store(destination, key, serializable);
  }


  public Serializable retrieve(String destination, String key) {
    return secureStorage.retrieve(destination, key);
  }


  public boolean remove(String destination, String key) throws PersistenceLayerException {
    return secureStorage.remove(destination, key);
  }


  public void deployPersistenceLayer(String name, String persistenceLayerFqClassname) throws XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND,
      XFMG_JarFolderNotFoundException, PersistenceLayerException {
    ods.deployPersistenceLayer(name, persistenceLayerFqClassname);
  }


  public void undeployPersistenceLayer(String persistenceLayerFqClassname) throws PersistenceLayerException,
      XNWH_PersistenceLayerNotRegisteredException, XNWH_PersistenceLayerMayNotBeUndeployedInUseException {
    ods.undeployPersistenceLayer(persistenceLayerFqClassname);
  }


  @Override
  public XynaWarehouseJobManagement getXynaWarehouseJobManagement() {
    return warehouseJobManagement;
  }

  @Override
  public XMOMPersistenceBase getXMOMPersistence() {
    return xmomPersistence;
  }

  @Override
  public XynaClusteringServices getXynaClusteringServices() {
    return xClusteringServices;
  }
  
  public StatisticsStore getStatisticsStore() {
    return statisticsStore;
  }

  public ConnectionPoolManagement getConnectionPoolManagement() {
    return ConnectionPoolManagement.getInstance();
  }

}

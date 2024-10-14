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

package com.gip.xyna.xnwh;



import java.io.Serializable;

import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xnwh.exceptions.XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.statistics.StatisticsStore;



public interface XynaFactoryWarehousePortal {

  public SecureStorage getSecureStorage();

  
  public ConnectionPoolManagement getConnectionPoolManagement();
  
  
  public boolean store(String destination, String key, Serializable serializable) throws PersistenceLayerException;


  public Serializable retrieve(String destination, String key);


  public boolean remove(String destination, String key) throws PersistenceLayerException;


  public void deployPersistenceLayer(String name, String persistenceLayerFqClassname) throws XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND,
      XFMG_JarFolderNotFoundException, PersistenceLayerException;


  public void undeployPersistenceLayer(String persistenceLayerFqClassname) throws PersistenceLayerException,
      XNWH_PersistenceLayerNotRegisteredException, XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
  
  public StatisticsStore getStatisticsStore();
}

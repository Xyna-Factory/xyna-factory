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

package com.gip.xyna.xnwh.persistence;



import java.util.List;

import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerClassIncompatibleException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerIdUnknownException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceMayNotBeDeletedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceNotRegisteredException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.persistence.ODSImpl.ClusteredStorableConfigChangeHandler;



public interface ODSAdministration {


  public void shutdown() throws PersistenceLayerException;


  /**
   * tabelle registrieren
   * @throws PersistenceLayerException
   */
  public void setPersistenceLayerForTable(long persistenceLayerInstanceID, String tableName, String properties)
      throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException;
  
  public void removeTableFromPersistenceLayer(long persistenceLayerInstanceID, String tableName) 
                  throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException;


  public void registerStorable(Class<? extends Storable> tableClass) throws PersistenceLayerException;
  
  
  public void registerStorableForceWidening(Class<? extends Storable> tableClass) throws PersistenceLayerException;


  public boolean unregisterStorable(Class<? extends Storable> tableClass) throws PersistenceLayerException;


  public boolean isTableRegistered(ODSConnectionType connectionType, String tableName);


  /**
   * gibt id zur�ck, der f�r instantiate PersistenceLayer gebraucht wird. die id darf sich nicht �ndern, wenn
   * persistencelayers entfernt werden
   */
  public void registerPersistenceLayer(long persistenceLayerId, Class<? extends PersistenceLayer> persistenceLayerClass);


  public PersistenceLayerBeanMemoryCache unregisterPersistenceLayer(long id)
      throws XNWH_PersistenceLayerMayNotBeUndeployedInUseException;


  public long instantiatePersistenceLayerInstance(long persistenceLayerID, String department,
                                                  ODSConnectionType connectionType, String[] connectionParameters)
      throws XNWH_PersistenceLayerIdUnknownException, PersistenceLayerException,
      XNWH_PersistenceLayerClassIncompatibleException;
  
  public long instantiatePersistenceLayerInstance(String persistenceLayerInstanceName, long persistenceLayerID, String department,
                                                  ODSConnectionType connectionType, String[] connectionParameters)
      throws XNWH_PersistenceLayerIdUnknownException, PersistenceLayerException,
      XNWH_PersistenceLayerClassIncompatibleException;


  public void removePersistenceLayerInstance(long persistenceLayerInstanceId)
      throws XNWH_PersistenceLayerInstanceIdUnknownException,
      XNWH_PersistenceLayerInstanceMayNotBeDeletedInUseException, PersistenceLayerException;


  public PersistenceLayerBeanMemoryCache[] getPersistenceLayers();

  public PersistenceLayerBeanMemoryCache getPersistenceLayer(Long id);

  public PersistenceLayerInstanceBean[] getPersistenceLayerInstances();


  public TableConfiguration[] getTableConfigurations();


  /**
   * wird von allen tabellen benutzt, f�r die nicht spezifisch ein anderer persistencelayer definiert ist
   * @throws XNWH_PersistenceLayerInstanceIdUnknownException
   * @throws PersistenceLayerException
   */
  public void setDefaultPersistenceLayer(ODSConnectionType connectionType, long persistenceLayerInstanceID)
      throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException;


  public PersistenceLayerInstanceBean getDefaultPersistenceLayerInstance(ODSConnectionType connectionType);


  long getMemoryPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException;


  long getJavaPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException;


  long getXmlPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException;


  long getDevNullPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException;


  //wie stellt man sicher, dass tabellen in persistence layer angelegt sind

  //regeln festlegen, die erm�glichen,
  //1. daten die ein bestimmtes kriterium erf�llen von einem connectionType zu einem anderen transferieren
  //2. daten die ein bestimmtes kriterium erf�llen zu l�schen?
  // regel hat eine bedingung (zb cache vollgelaufen oder sowas)

  public <T extends Storable<?>> long getPersistenceLayerInstanceId(ODSConnectionType connectionType, Class<T> targetStorable);
  
  public <T extends Storable<?>> long getPersistenceLayerInstanceId(ODSConnectionType connectionType, String tableName);


  public <STR extends Storable> void copy(Class<STR> storableClazz, ODSConnectionType sourceConnectionType, ODSConnectionType targetConnectionType)
      throws PersistenceLayerException;
  
  public <STR extends Storable> void replace(Class<STR> storableClazz, ODSConnectionType sourceConnectionType, ODSConnectionType targetConnectionType)
      throws PersistenceLayerException;
  
  
  public <STR extends Storable> void delete(Class<STR> storableClazz, List<ODSConnectionType> targetConnectionTypes)
      throws PersistenceLayerException;
  

  public <T extends Storable> Class<T> getStorableByTableName(String tablename);


  /**
   * sind die persistencelayer so konfiguriert, dass beiden connections auf die gleiche physische tabelle zeigen? das
   * beudetet zb, dass ein zeilenlock �ber den einen connectiontype dazu f�hrt, dass die andere connection nicht auf die
   * gelockte zeile zugreifen kann.
   */
  public boolean isSamePhysicalTable(String tableName, ODSConnectionType type1, ODSConnectionType type2)
      throws XNWH_NoPersistenceLayerConfiguredForTableException;


  public void deployPersistenceLayer(String name, String persistenceLayerFqClassname) throws XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND,
      XFMG_JarFolderNotFoundException, PersistenceLayerException;


  public void undeployPersistenceLayer(String persistenceLayerFqClassname) throws PersistenceLayerException,
      XNWH_PersistenceLayerNotRegisteredException, XNWH_PersistenceLayerMayNotBeUndeployedInUseException;


  public Long getPersistenceLayerId (String persitenceLayerName) throws XNWH_PersistenceLayerNotRegisteredException;

  public Long getPersistenceLayerInstanceId (String persitenceLayerInstanceName) throws XNWH_PersistenceLayerInstanceNotRegisteredException;
  
  public void clearPreparedQueryCache();


  /*
   * von OraclePersistenceLayer gerufen!
   */
  public void changeClustering(PersistenceLayer pl, boolean newStateIsClustered, long newClusterInstanceId);


  public void addClusteredStorableConfigChangeHandler(ClusteredStorableConfigChangeHandler handler,
                                                      ODSConnectionType conType,
                                                      Class<? extends Storable> storableClass);

}

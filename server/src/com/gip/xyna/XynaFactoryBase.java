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

package com.gip.xyna;



import java.util.HashMap;
import java.util.List;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivationBase;
import com.gip.xyna.xdev.XynaDevelopmentBase;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xmcp.XynaMultiChannelPortalBase;
import com.gip.xyna.xnwh.XynaFactoryWarehouseBase;
import com.gip.xyna.xprc.XynaProcessingBase;



public interface XynaFactoryBase extends XynaFactoryPortal {

  public void init() throws XynaException;


  /**
   * Prüft, ob Factory beendet werden kann und ruft dann über XynaFactoryCommandLineInterface den Shutdown auf
   * Factory fährt bereits herunter -&gt; shutdown() kehrt sofort zurück
   * Factory ist noch beim Hochfahren -&gt; shutdown() wartet bis Factory hochgefahren ist
   */
  public void shutdown();

  public void shutdownComponents() throws XynaException;

  public XynaFactoryManagementBase getFactoryManagement();


  public XynaActivationBase getActivation();


  public XynaProcessingBase getProcessing();


  public XynaMultiChannelPortalBase getXynaMultiChannelPortal();


  public XynaDevelopmentBase getXynaDevelopment();


  public XynaFactoryWarehouseBase getXynaNetworkWarehouse();


  public void addComponentToBeInitializedLater(XynaFactoryComponent lateInitComponent) throws XynaException;


  public void initLateInitComponents(HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> allDependencies)
      throws XynaException;


  public boolean isShuttingDown();
  public boolean isStartingUp();
  public boolean finishedInitialization();


  public IDGenerator getIDGenerator();


  public FutureExecution getFutureExecution();
  
  public FutureExecution getFutureExecutionForInit();
  
  public long getBootCntId();
  
  public int getBootCount();

  /**
   * verhindert, dass der server runterfährt, solange man das lock hält.
   * ist ein "readlock", d.h. mehrere threads können das shutdown parallel verhindern.
   * @return gibt true zurück, falls die sperrung gegen shutdown erfolgreich war und false, falls bereits das shutdown am laufen ist
   */
  public boolean lockShutdown(String cause);


  public void unlockShutdown();
  
}

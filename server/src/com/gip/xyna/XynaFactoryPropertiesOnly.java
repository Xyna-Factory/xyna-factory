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

package com.gip.xyna;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivationBase;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xdev.XynaDevelopmentBase;
import com.gip.xyna.xdev.XynaDevelopmentPortal;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xfmg.XynaFactoryManagementPropertiesOnly;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.XynaMultiChannelPortalBase;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xnwh.XynaFactoryWarehouseBase;
import com.gip.xyna.xnwh.XynaFactoryWarehousePortal;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal;



public class XynaFactoryPropertiesOnly implements XynaFactoryBase {

  private static final String ILLEGAL_STATE_MESSAGE = "";

  private final XynaFactoryManagementPropertiesOnly xfmgPropOnly;


  public XynaFactoryPropertiesOnly(Map<String, String> properties) throws XynaException {
    this.xfmgPropOnly = new XynaFactoryManagementPropertiesOnly(properties);
  }

  /**
   * @param initialProperties
   */
  public void addProperties(Map<String, String> initialProperties) {
    try {
      for( Map.Entry<String, String> entry : initialProperties.entrySet() ) {
        xfmgPropOnly.setProperty(entry.getKey(), entry.getValue() );
      }
    } catch( PersistenceLayerException e ) {
      throw new RuntimeException(e); //unerwartet
    }
  }

  public XynaFactoryManagementPortal getFactoryManagementPortal() {
    return xfmgPropOnly;
  }


  public XynaActivationPortal getActivationPortal() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaProcessingPortal getProcessingPortal() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Channel getXynaMultiChannelPortalPortal() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaMultiChannelPortalSecurityLayer getXynaMultiChannelPortalSecurityLayer() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaDevelopmentPortal getXynaDevelopmentPortal() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaFactoryWarehousePortal getXynaNetworkWarehousePortal() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void init() throws XynaException {
  }


  public void shutdown() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }
  public void shutdownComponents() throws XynaException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaFactoryManagementBase getFactoryManagement() {
    return xfmgPropOnly;
  }


  public XynaActivationBase getActivation() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaProcessingBase getProcessing() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaMultiChannelPortalBase getXynaMultiChannelPortal() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaDevelopmentBase getXynaDevelopment() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaFactoryWarehouseBase getXynaNetworkWarehouse() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void addComponentToBeInitializedLater(XynaFactoryComponent lateInitComponent) throws XynaException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void initLateInitComponents(HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> allDependencies)
      throws XynaException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean isShuttingDown() {
    return false;
  }


  public boolean isStartingUp() {
    return false;
  }


  public boolean finishedInitialization() {
    return false;
  }


  public IDGenerator getIDGenerator() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public FutureExecution getFutureExecution() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public FutureExecution getFutureExecutionForInit() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }
  
  public long getBootCntId() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public FutureExecution getFutureExecutionForResumesAfterStartup() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }

  public boolean lockShutdown(String cause) {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }

  public void unlockShutdown() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }

  public int getBootCount() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }

}

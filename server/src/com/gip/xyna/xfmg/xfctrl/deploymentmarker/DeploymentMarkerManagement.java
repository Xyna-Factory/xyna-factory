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
package com.gip.xyna.xfmg.xfctrl.deploymentmarker;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class DeploymentMarkerManagement extends FunctionGroup {

  private static Logger logger = CentralFactoryLogging.getLogger(DeploymentMarkerManagement.class);

  public static final String DEFAULT_NAME = "DeploymentMarkerManagement";
  
  private DeploymentMarkerStorage storage;
  
  
  public DeploymentMarkerManagement() throws XynaException {
    super();
  }
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }
  
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(DeploymentMarkerManagement.class,"DeploymentMarkerManagement.initStorage").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initStorage(); }});
  }
  
  private void initStorage() {
    try {
      storage = new DeploymentMarkerStorage();
    } catch (PersistenceLayerException e) {
      logger.warn("Could not initialize DeploymentMarkerManagement", e);
      throw new RuntimeException(e);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
  }
  
  
  public DeploymentMarker createDeploymentMarker(DeploymentMarker marker) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    return storage.createDeploymentMarker(marker);
  }

  public void deleteDeploymentMarker(DeploymentMarker marker) throws PersistenceLayerException {
    storage.deleteDeploymentMarker(marker);
  }

  public void deleteDeploymentMarkerForDeploymentItem(DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    storage.deleteDeploymentMarkerForDeploymentItem(deploymentItem, revision);
  }

  public void deleteDeploymentMarkerForRevision(Long revision) throws PersistenceLayerException {
    storage.deleteDeploymentMarkerForRevision(revision);
  }

  public void modifyDeploymentMarker(DeploymentMarker marker) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    storage.modifyDeploymentMarker(marker);
  }

  public void moveDeploymentMarker(DeploymentItemIdentifier oldDeploymentItem, DeploymentItemIdentifier newDeploymentItem, Long revision) throws PersistenceLayerException {
    storage.moveDeploymentMarker(oldDeploymentItem, newDeploymentItem, revision);
  }
  
  public List<DeploymentMarker> searchDeploymentTags(Optional<? extends DeploymentItemIdentifier> deploymentItem, Long revision) throws PersistenceLayerException, XNWH_SelectParserException, XNWH_InvalidSelectStatementException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return storage.searchDeploymentTags(deploymentItem, revision);
  }

  public List<DeploymentMarker> searchDeploymentTasks(Optional<? extends DeploymentItemIdentifier> deploymentItem, Long revision) throws PersistenceLayerException, XNWH_SelectParserException, XNWH_InvalidSelectStatementException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return storage.searchDeploymentTasks(deploymentItem, revision);
  }

  public int countOpenDeploymentTasks(DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    return storage.countOpenDeploymentTasks(deploymentItem, revision);
  }
}

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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.util.Set;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;



public interface DeploymentItemRegistry {
  
  public DeploymentItemState get(String fqName);

  public Set<DeploymentItemState> list();
  
  public void save(DeploymentItem di);
  
  public void save(String fqName);
  
  public void delete(String fqName, DeploymentContext ctx);

  public void collectUsingObjectsInContext(String fqName, DeploymentContext ctx);
  
  public void undeploy(String fqName, DeploymentContext ctx);
  
  public void deployFinished(String fqName, DeploymentTransition transition, boolean copiedXMLFromSaved, Optional<? extends Throwable> deploymentException);

  public void buildFinished(String fqName, Optional<? extends Throwable> buildException);

  public void update(DeploymentItem di, Set<DeploymentLocation> locations);
  
  public long getManagedRevision();

  public void invalidateCallSites();
  
}

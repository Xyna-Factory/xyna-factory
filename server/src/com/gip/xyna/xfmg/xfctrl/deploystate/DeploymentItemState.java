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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.util.Set;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.CrossRevisionResolver;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xnwh.selection.parsing.Selection;


public interface DeploymentItemState extends DeploymentItemIdentifier {
  
  public boolean exists();

  public boolean isDeployed();

  public long getLastModified();
  
  public long getLastDeployed();
  
  public void save(DeploymentItem di);
  
  public DisplayState deriveDisplayState();
  
  public void collectUsingObjectsInContext(DeploymentContext ctx);
  
  public void undeploy(DeploymentContext ctx);
  
  public void delete(DeploymentContext ctx);
  
  public boolean deploymentLocationContentChanges();
  
  public DeploymentItemStateReport getStateReport();
  
  public Optional<DeploymentTransition> getLastDeploymentTransition();
  
  public DeploymentItemStateReport getStateReport(Selection selection);
  
  public DeploymentItemStateReport getStateReport(Selection selection, CrossRevisionResolver crossResolver);
  
  public void update(DeploymentItem di, Set<DeploymentLocation> locations);
  
  public Set<DeploymentItemState> getInvocationSites(DeploymentLocation location);
  
  public void addInvocationSite(DeploymentItemState dis, DeploymentLocation location);
  
  public void removeInvocationSite(DeploymentItemState dis, DeploymentLocation location);
  
  public void getPublishedInterfacesRecursively(Set<DeploymentItemState> result, DeploymentLocation location);
  
  public boolean hasServiceImplInconsistencies(DeploymentLocation location, boolean onlyInterfaceChangeInconsistencies);
  
  public <I extends DeploymentItemInterface> Set<I> getPublishedInterfaces(Class<I> interfaceType, DeploymentLocation location);
 
  public PublishedInterfaces getPublishedInterfaces(DeploymentLocation location);

  public Set<DeploymentItemInterface> getInconsistencies(DeploymentLocation ownLocation, DeploymentLocation interfaceProviderLocation, boolean tryToInferTypes);
  
  public Set<DeploymentItemInterface> getInconsistencies(DeploymentLocation ownLocation, DeploymentLocation interfaceProviderLocation, boolean tryToInferTypes, CrossRevisionResolver resolver);
  
  /**
   * @return deploymentitemstate hat sich geändert?
   */
  public boolean deploymentTransition(DeploymentTransition transition, boolean fromSaved, Optional<? extends Throwable> deploymentException);
  
  public String createCreationHint(String name);

  public void setBuildError(Optional<? extends Throwable> buildException);
  
  public void addOperationInvocationSite(DeploymentItemState callerOfOperation, OperationInterface operationInterface,
      DeploymentLocation location);

  
  public static enum DeploymentLocation {
    DEPLOYED, SAVED;
  }
  
    
  public static enum DeploymentTransition {
    SUCCESS("SUCCESS"),
    ROLLBACK("ROLLBACK"),
    IN_PROGRESS("IN_PROGRESS"),
    SUCCESSFULL_ROLLBACK("SUCCESSFULL_ROLLBACK"),
    ERROR_DURING_ROLLBACK("ERROR_DURING_ROLLBACK")    
    ;
    
    private final String name;
    
    private DeploymentTransition(String name) {
      this.name = name;
    }
    
    
    public String getName() {
      return name;
    }
    
    
    public static DeploymentTransition byName(String name) {
      for (DeploymentTransition trans : values()) {
        if (trans.name.equals(name)) {
          return trans;
        }
      }
      return null;
    }
    
  }


  


 
}

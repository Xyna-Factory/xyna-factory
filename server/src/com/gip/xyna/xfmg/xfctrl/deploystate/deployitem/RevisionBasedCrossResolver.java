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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;


public class RevisionBasedCrossResolver implements CrossRevisionResolver {
  
  private final Map<Long, DeploymentItemRegistry> reachableRegistries;
  private final static RuntimeContextDependencyManagement rcdm;
  private final boolean checkForInvalidGeneration; // only true for runtimeDependencyBackedResolver as it represents the current deployed state
  static {
    rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  }
  
  private RevisionBasedCrossResolver(Map<Long, DeploymentItemRegistry> reachableRegistries, boolean checkForInvalidGeneration) {
    this.reachableRegistries = reachableRegistries;
    this.checkForInvalidGeneration = checkForInvalidGeneration;
  }
  

  public Optional<DeploymentItemState> resolve(DeploymentItemIdentifier identifier, Long rootRevision) {
    Long definingRevision = rcdm.getRevisionDefiningXMOMObject(identifier.getName(), rootRevision);
    if (definingRevision == null || !reachableRegistries.containsKey(definingRevision)) {
      definingRevision = null;
      for (Long reachableRevision : reachableRegistries.keySet()) {
        // in case of simulated changes getRevisionDefiningXMOMObject won't traverse the proper dependencies
        definingRevision = rcdm.getRevisionDefiningXMOMObject(identifier.getName(), reachableRevision);
        if (definingRevision == null || !reachableRegistries.containsKey(definingRevision)) {
          definingRevision = null;          
        } else {
          break;
        }
      }
    }
    if (definingRevision == null) {
      return Optional.empty();
    }
    return Optional.of(InterfaceResolutionContext.getProvider(identifier, reachableRegistries.get(definingRevision)));
  }
  
  public Set<Long> identifyReachableRevisions() {
    return new HashSet<Long>(reachableRegistries.keySet());
  }
  
  public static RevisionBasedCrossResolver runtimeDependencyBackedResolver(Long rootRevision) {
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<Long> dependencies = new HashSet<Long>();
    rcdm.getDependenciesRecursivly(rootRevision, dependencies);
    dependencies.add(rootRevision);
    return customDependencyResolver(dependencies, true);
  }
  
  
  public static RevisionBasedCrossResolver customDependencyResolver(Set<Long> reachableRevisions) {
    return customDependencyResolver(reachableRevisions, false);
  }
  
  private static RevisionBasedCrossResolver customDependencyResolver(Set<Long> reachableRevisions, boolean checkForInvalidGeneration) {
    Map<Long, DeploymentItemRegistry> reachableRegistries = new HashMap<Long, DeploymentItemRegistry>();
    DeploymentItemStateManagementImpl dismi = (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    for (Long reachableRevision : reachableRevisions) {
      reachableRegistries.put(reachableRevision, dismi.lazyCreateOrGet(reachableRevision));
    }
    return new RevisionBasedCrossResolver(reachableRegistries, checkForInvalidGeneration);
  }

  public boolean checkForInvalidGeneration() {
    return checkForInvalidGeneration;
  }
  
  public boolean updateCallSites() {
    return checkForInvalidGeneration;
  }

}

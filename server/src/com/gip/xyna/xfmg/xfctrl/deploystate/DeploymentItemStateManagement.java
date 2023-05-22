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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl.StateTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.CrossRevisionResolver;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;


public interface DeploymentItemStateManagement {

  public DeploymentItemState get(String fqName, long revision);
  
  public SearchResult<DeploymentItemStateReport> search(SearchRequestBean searchRequest) throws XNWH_NoSelectGivenException, XNWH_WhereClauseBuildException, XNWH_SelectParserException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

  public void save(DeploymentItem di, long revision);
  
  public void save(String fqName, long revision);
  
  public void update(DeploymentItem di, Set<DeploymentLocation> locations, long revision);
  
  public void delete(String fqName, DeploymentContext ctx, long revision);

  public void collectUsingObjectsInContext(String fqName, DeploymentContext ctx, long revision);
  
  public void undeploy(String fqName, DeploymentContext ctx, long revision);
  
  public void deployFinished(String fqName, DeploymentTransition transition, boolean copiedXMLFromSaved, Optional<Throwable> deploymentException, long revision);

  public void buildFinished(String fqName, Optional<Throwable> buildException, long revision);

  public void discoverItems(long revision);
  
  public DeploymentItemRegistry removeRegistry(long revision);
  
  public boolean isInitialized();
  
  public List<StateTransition> collectStateChangesBetweenResolvers(long rootRevision, CrossRevisionResolver crossResolver, CrossRevisionResolver otherCrossResolver);

  public DeploymentItemRegistry getRegistry(long r);
  
}

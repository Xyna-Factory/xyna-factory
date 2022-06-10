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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.io.PrintStream;
import java.util.EnumSet;
import java.util.List;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewVersionForApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildWorkingSet;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStartApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStopApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CronLikeOrderCopyException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateApplicationName;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.CopyCLOResult;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;



public interface ApplicationManagement {

  public void removeApplicationVersion(String applicationName, String versionName)
      throws XFMG_CouldNotRemoveApplication;

  public void removeApplicationVersion(String applicationName, String versionName, RemoveApplicationParameters params, RepositoryEvent repositoryEvent)
                  throws XFMG_CouldNotRemoveApplication;

  public void stopApplication(String applicationName, String versionName, boolean clusterwide)
      throws XFMG_CouldNotStopApplication;
  
  public void stopApplication(String applicationName, String versionName, boolean clusterwide, Optional<EnumSet<OrderEntranceType>> onlyDisableEntranceTypes)
                  throws XFMG_CouldNotStopApplication;


  public void startApplication(String applicationName, String versionName, boolean force, boolean clusterwide)
      throws XFMG_CouldNotStartApplication;


  public void buildApplicationVersion(String applicationName, String versionName, BuildApplicationVersionParameters params)
      throws XFMG_CouldNotBuildNewVersionForApplication;


  public void removeObjectFromAllApplications(String uniqueName, ApplicationEntryType type, Long parentRevision);


  public List<ApplicationInformation> listApplications() throws PersistenceLayerException;

  public List<ApplicationDefinitionInformation> listApplicationDefinitions(Long parentRevision) throws PersistenceLayerException;


  public void defineApplication(String applicationName, String comment, Long parentRevision)
      throws XFMG_DuplicateApplicationName;
  
  public void defineApplication(String applicationName, String comment, Long parentRevision, String user)
                  throws XFMG_DuplicateApplicationName;


  public void copyOrderTypes(String applicationName, String sourceVersion, String targetVersion, PrintStream printStream);


  public CopyCLOResult copyCronLikeOrders(String applicationName, String sourceVersion, String targetVersion,
                                 PrintStream printStream, String id, String[] ordertypes, boolean move,
                                 boolean verbose, boolean global) throws XFMG_CronLikeOrderCopyException;

  public CopyCLOResult copyCronLikeOrders(RuntimeContext source, RuntimeContext target,
                                          PrintStream printStream, String id, String[] ordertypes, boolean move,
                                          boolean verbose, boolean global) throws XFMG_CronLikeOrderCopyException;
  
  /**
   * gibt application namen im workspace zurück, die explizit das übergebene objekt enthalten
   */
  public String[] getApplicationsContainingObject(String name, ApplicationEntryType type, Long parentRevision) throws PersistenceLayerException;
  
  public void copyApplicationIntoWorkspace(String applicationName, String versionName, CopyApplicationIntoWorkspaceParameters params) throws XFMG_CouldNotBuildWorkingSet;

  public SearchResult<ApplicationDefinitionInformation> searchApplicationDefinitions(SearchRequestBean searchRequest) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

}

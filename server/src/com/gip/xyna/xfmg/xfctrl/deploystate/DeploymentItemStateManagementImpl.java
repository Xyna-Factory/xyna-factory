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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Join;
import com.gip.xyna.utils.collections.CollectionUtils.JoinType;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarkerManagement;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentTask;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.CrossRevisionResolver;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemColumn;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemSelectImpl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;


public class DeploymentItemStateManagementImpl extends FunctionGroup implements DeploymentItemStateManagement {
  
  public static final String DEFAULT_NAME = "DeploymentItemStateManagement";
  
  private ConcurrentMap<Long, DeploymentItemRegistry> registries;
  
  private boolean initialized;
  
  public DeploymentItemStateManagementImpl() throws XynaException {
    super();
    registries = new ConcurrentHashMap<Long, DeploymentItemRegistry>();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    XynaProperty.SERVICE_IMPL_INCONSISTENCY_TIME_LAG.registerDependency(DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(DeploymentItemStateManagementImpl.class, "DeploymentItemStateManagement")
         .after(WorkflowDatabase.FUTURE_EXECUTION_ID)
         .after(RuntimeContextDependencyManagement.class)
         .execAsync( new Runnable() { public void run() { 
            discoverItems();
          }
        });
  }

  @Override
  protected void shutdown() throws XynaException {
  }
  
  public DeploymentItemState get(String fqName, long revision) {
    return lazyCreateOrGet(revision).get(fqName);
  }

  
  public SearchResult<DeploymentItemStateReport> search(SearchRequestBean searchRequest) throws XNWH_NoSelectGivenException, XNWH_WhereClauseBuildException, XNWH_SelectParserException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY{
    try {
      List<DeploymentItemStateReport> result = new ArrayList<DeploymentItemStateReport>();
      Selection selection = SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequest);
      //da das sql hier nicht verwendet werden kann, müssen wir die filterkriterien einzeln verarbeiten

      Map<String, String> filterMap = searchRequest.getFilterEntries();
      String application = SelectionParser.getLiteral(filterMap.get(DeploymentItemColumn.APPLICATION.getColumnName()));
      String version = SelectionParser.getLiteral(filterMap.get(DeploymentItemColumn.VERSION.getColumnName()));
      String workspace = SelectionParser.getLiteral(filterMap.get(DeploymentItemColumn.WORKSPACE.getColumnName()));
      long revision = getRevisionFromFilterMap(application, version, workspace);
      String fqName = SelectionParser.getLiteral(filterMap.get(DeploymentItemColumn.FQNAME.getColumnName()));
      if (fqName != null) {
        //ein bestimmtes DeploymentItem selektieren
        DeploymentItemState dis = lazyCreateOrGet(revision).get(fqName);
        if (dis == null) {
          if (logger.isDebugEnabled()) {
            logger.debug(fqName + " not found in " + getClass().getSimpleName() + ".");
          }
        } else {
          if (dis.exists()) {
            result.add(searchSingleDeploymentItem(selection, dis, revision));
          }
        }
      } else {
        //alle DeploymentItems einer Revision selektieren
        result.addAll(searchDeploymentItems(selection, revision));
      }
      if (application != null && workspace != null) {
        //application definition: objekte aussortieren, die nicht zur appdef gehören
        List<ApplicationEntryStorable> entries = ((ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement()
            .getXynaFactoryControl().getApplicationManagement()).listApplicationDetails(application, null, true,
                                                                                        Collections.<String> emptyList(), revision, true);
        Set<String> fqnames = new HashSet<String>();
        for (ApplicationEntryStorable e : entries) {
          fqnames.add(e.getName());
        }
        for (int i = result.size() - 1; i >= 0; i--) {
          DeploymentItemStateReport disr = result.get(i);
          if (!fqnames.contains(disr.getFqName())) {
            result.remove(i);
          }
        }
      }
      return new SearchResult<DeploymentItemStateReport>(result, result.size());
    } catch (Throwable t) {
      logger.warn("SearchResult<DeploymentItemStateReport>", t);
      throw new RuntimeException("", t);
    }
  }

  private DeploymentItemStateReport searchSingleDeploymentItem(Selection selection, DeploymentItemState dis, Long revision) throws PersistenceLayerException, XNWH_SelectParserException, XNWH_InvalidSelectStatementException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    // TODO selection should come from UI, for now always select creationhints if resolution is selected and we're in a single search
    if (selection.containsColumn(DeploymentItemColumn.RESOLUTION) &&
        selection instanceof DeploymentItemSelectImpl) {
      ((DeploymentItemSelectImpl)selection).select(DeploymentItemColumn.CREATION_HINT);
    }
    //StateReport ermitteln
    DeploymentItemStateReport report = dis.getStateReport(selection);

    //Tags und Tasks hinzufügen
    DeploymentMarkerManagement dmMgmt = XynaFactory.getPortalInstance().getFactoryManagementPortal().getXynaFactoryControl().getDeploymentMarkerManagement();
    if (selection.containsColumn(DeploymentItemColumn.MARKER) || selection.containsColumn(DeploymentItemColumn.TASK)) {
      report.addDeploymentMarker(dmMgmt.searchDeploymentTasks(Optional.of(dis), revision));
    }
    if (selection.containsColumn(DeploymentItemColumn.MARKER) || selection.containsColumn(DeploymentItemColumn.TAG)) {
      report.addDeploymentMarker(dmMgmt.searchDeploymentTags(Optional.of(dis), revision));
    }
    
    //OpenTaskCount hinzufügen
    if (selection.containsColumn(DeploymentItemColumn.TASKCOUNT)) {
      report.setOpenTaskCount(dmMgmt.countOpenDeploymentTasks(dis, revision));
    }
    
    return report;
  }

  private List<DeploymentItemStateReport> searchDeploymentItems(Selection selection, Long revision) throws PersistenceLayerException, XNWH_SelectParserException, XNWH_InvalidSelectStatementException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    //StateReports ermitteln
    List<DeploymentItemStateReport> reports = new ArrayList<DeploymentItemStateReport>();
    
    for (DeploymentItemState dis : lazyCreateOrGet(revision).list()) {
      if (dis.exists()) {
        reports.add(dis.getStateReport(selection));
      }
    }
    
    //Tags und Tasks hinzufügen
    List<DeploymentMarker> markers = new ArrayList<DeploymentMarker>();
    DeploymentMarkerManagement dmMgmt = XynaFactory.getPortalInstance().getFactoryManagementPortal().getXynaFactoryControl().getDeploymentMarkerManagement();

    if (selection.containsColumn(DeploymentItemColumn.MARKER) || selection.containsColumn(DeploymentItemColumn.TASK)) {
      markers.addAll(dmMgmt.searchDeploymentTasks(Optional.<DeploymentItemIdentifier>empty(), revision));
    }
    if (selection.containsColumn(DeploymentItemColumn.MARKER) || selection.containsColumn(DeploymentItemColumn.TAG)) {
      markers.addAll(dmMgmt.searchDeploymentTags(Optional.<DeploymentItemIdentifier>empty(), revision));
    }
    
    if (markers.size() > 0) {
      reports = CollectionUtils.join(reports, markers, new DeploymentItemStateReportJoin(false), JoinType.LeftOuter);
    }
    
    //OpenTaskCount hinzufügen
    if (selection.containsColumn(DeploymentItemColumn.TASKCOUNT)) {
      if (!selection.containsColumn(DeploymentItemColumn.MARKER) && !selection.containsColumn(DeploymentItemColumn.TASK)) {
        //Tasks wurden noch nicht gesucht
        markers = dmMgmt.searchDeploymentTasks(Optional.<DeploymentItemIdentifier>empty(), revision);
      }
      
      //aus der Liste aller Tags und Tasks die noch offenen Tasks heraussuchen
      List<DeploymentMarker> openTasks = CollectionUtils.transformAndSkipNull(markers, new OpenDeploymentTasks());
      //und die Anzahl an die Reports dranhängen
      reports = CollectionUtils.join(reports, openTasks, new DeploymentItemStateReportJoin(true), JoinType.LeftOuter);
    }
    
    return reports;
  }
  
  //callsites werden beim serverstart bereits befüllt (initialer resolution aufruf checken=)
  //callsites unvollständig
  
  
  public Map<Long, Map<String, XMOMDatabaseSearchResultEntry>> searchByBackwardRelation(Set<XMOMDatabaseEntryColumn> relations, String usedObject, Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Map<Long, Map<String, XMOMDatabaseSearchResultEntry>> found = new HashMap<Long, Map<String, XMOMDatabaseSearchResultEntry>>();
    DeploymentItemRegistry dir = registries.get(revision);
    DeploymentItemStateImpl disi = (DeploymentItemStateImpl) dir.get(usedObject);
    if ((disi == null || !disi.exists()) && operationConversionRequired(relations)) {
      //usedObject is FQN of operation (<dtPath>.<dtName>.<dtName>.<operationName>)
      //remove <dtName>.<operationName> and search for dataType.
      String dataTypeName = GenerationBase.getPackageNameFromFQName(GenerationBase.getPackageNameFromFQName(usedObject));
      disi = (DeploymentItemStateImpl) dir.get(dataTypeName);
    }
    if (disi != null &&
        disi.exists()) {
      TypeInterface asType = InterfaceResolutionContext.toTypeInterface(disi);
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      
      Set<DeploymentItemState> invokers;
      //Only search in saved locations of workspaces, so that no dependency of a deployed version of an object is shown.
      if(rm.isWorkspaceRevision(revision)) {
        invokers = new HashSet<DeploymentItemState>(disi.getInvocationSites(DeploymentLocation.SAVED));
      } else {
        invokers = new HashSet<DeploymentItemState>(disi.getInvocationSites(DeploymentLocation.DEPLOYED));
      }
      for (DeploymentItemState invoker : invokers) {
        if (invoker.exists()) {
          DeploymentItemStateImpl invokerImpl = (DeploymentItemStateImpl)invoker;
          RuntimeContext invokerRC = rm.getRuntimeContext(invokerImpl.getRevision());
          Map<String, XMOMDatabaseSearchResultEntry> revMap = found.get(invokerImpl.getRevision());
          if (revMap == null) {
            revMap = new HashMap<String, XMOMDatabaseSearchResultEntry>();
            found.put(invokerImpl.getRevision(), revMap);
          }
          Set<DeploymentItemInterface> usingInterfaces = invokerImpl.getInterfaceEmployments(invokerRC instanceof Workspace ? DeploymentLocation.SAVED : DeploymentLocation.DEPLOYED);
          if (usingInterfaces != null &&
              usingInterfaces.size() > 0) {
            interfaces: for (DeploymentItemInterface usingInterface : usingInterfaces) {
              try {
                TypeInterface ti = InterfaceResolutionContext.getProviderType(usingInterface);
                if (ti.getName().equals(asType.getName()) && ti.getType() == asType.getType()) {
                  for (XMOMDatabaseEntryColumn relation : relations) {
                    if (relation == XMOMDatabaseEntryColumn.USEDINIMPLOF
                        || InterfaceResolutionContext.satisfiesRelation(invokerImpl, relation, usingInterface)) {
                      if (relation == XMOMDatabaseEntryColumn.CALLEDBY &&
                          usingInterface instanceof InterfaceEmployment &&
                          ((InterfaceEmployment)usingInterface).unwrap() instanceof OperationInterface) {
                        OperationInterface oi = (OperationInterface) ((InterfaceEmployment)usingInterface).unwrap();
                        if (!oi.getName().equals(GenerationBase.getSimpleNameFromFQName(usedObject))) {
                          continue interfaces;
                        }
                      }
                      if (relation == XMOMDatabaseEntryColumn.NEEDEDBY || relation == XMOMDatabaseEntryColumn.PRODUCEDBY) {
                        if (invokerImpl.getType() == XMOMType.DATATYPE) {
                          //operation finden, die man anstatt dem typ zurückgeben will
                          for (OperationInterface op : invokerImpl.getPublishedInterfaces(DeploymentLocation.SAVED).getAllOperations()) {
                            boolean foundOp = false;
                            if (relation == XMOMDatabaseEntryColumn.NEEDEDBY) {
                              for (TypeInterface input : op.getInput()) {
                                if (input.getType() == asType.getType() && input.getName().equals(asType.getName())) {
                                  //operation gefunden, die den typ benötigt
                                  foundOp = true;
                                  break;
                                }
                              }
                            } else if (relation == XMOMDatabaseEntryColumn.PRODUCEDBY) {
                              for (TypeInterface output : op.getOutput()) {
                                if (output.getType() == asType.getType() && output.getName().equals(asType.getName())) {
                                  //operation gefunden, die den typ als output hat
                                  foundOp = true;
                                  break;
                                }
                              }
                            }
                            if (foundOp) {
                              //serviceName wird in deploymentitem nicht gespeichert, deshalb aus xmomdb lesen
                              List<XMOMDatabaseSelect> selects = new ArrayList<XMOMDatabaseSelect>(1);
                              XMOMDatabaseSelect select = new XMOMDatabaseSelect();
                              try {
                                select.select(XMOMDatabaseEntryColumn.NAME).where(XMOMDatabaseEntryColumn.WRAPPEDBY)
                                    .isEqual(invokerImpl.getName());
                                select.addDesiredResultTypes(XMOMDatabaseType.SERVICEGROUP);
                              } catch (XNWH_WhereClauseBuildException e) {
                                throw new RuntimeException(e);
                              }
                              selects.add(select);
                              String serviceName = null;
                              try {
                                XMOMDatabaseSearchResult xmomdbresult =
                                    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase()
                                        .searchXMOMDatabase(selects, 1, invokerImpl.getRevision());
                                if (xmomdbresult.getResult().size() > 0) {
                                  XMOMDatabaseSearchResultEntry xmomDatabaseSearchResultEntry = xmomdbresult.getResult().get(0);
                                  serviceName = GenerationBase.getSimpleNameFromFQName(xmomDatabaseSearchResultEntry.getSimplename()); //simplename hat format <dtsimplename>.<servicename>
                                }
                              } catch (XNWH_InvalidSelectStatementException e) {
                                throw new RuntimeException(e);
                              } catch (PersistenceLayerException e) {
                                throw new RuntimeException(e);
                              }
  
                              String opName = invokerImpl.getName() + "." + serviceName + "." + op.getName();
                              String simpleName =
                                  GenerationBase.getSimpleNameFromFQName(invokerImpl.getName()) + "." + serviceName + "." + op.getName();
                              String path = GenerationBase.getPackageNameFromFQName(invokerImpl.getName());
                              XMOMDatabaseSearchResultEntry currentEntry =
                                  new XMOMDatabaseSearchResultEntry(opName, simpleName, path, XMOMDatabaseType.OPERATION, 1);
                              currentEntry.setRuntimeContext(invokerRC);
                              currentEntry.setLabel(op.getName());
                              
                              revMap.put(opName, currentEntry);
                            }
                          }
                          continue interfaces;
                        }
                      }
                      if (relation == XMOMDatabaseEntryColumn.POSSESSEDBY) {
                        if (invokerImpl.getType() == XMOMType.DATATYPE) {
                          if (usingInterface instanceof TypeInterface) {
                            TypeInterface member = (TypeInterface) usingInterface;
                            if (!member.getTypesOfUsage().contains(TypeOfUsage.EMPLOYMENT)
                                || member.getType() != ti.getType()) {
                              continue;
                            }
                          } else {
                            continue;
                          }
                        } else {
                          continue;
                        }
                      }
                      XMOMDatabaseSearchResultEntry currentEntry = revMap.get(invokerImpl.getName());
                      if (currentEntry == null) {
                        switch (invokerImpl.getType()) {
                          case ORDERINPUTSOURCE :
                            continue interfaces;
                          default :
                            currentEntry = new XMOMDatabaseSearchResultEntry(invokerImpl.getName(),
                                                                             GenerationBase.getSimpleNameFromFQName(invokerImpl.getName()),
                                                                             GenerationBase.getPackageNameFromFQName(invokerImpl.getName()),
                                                                             XMOMDatabaseType.getXMOMDatabaseTypeByXMOMType(invokerImpl.getType()).get(0),
                                                                             1);
                            currentEntry.setLabel(invokerImpl.getLabel());
                            break;
                        }
                        currentEntry.setRuntimeContext(invokerRC);
                        revMap.put(invokerImpl.getName(), currentEntry);
                      } else {
                        currentEntry.setWeigth(currentEntry.getWeigth() + 1);
                      }
                    }
                  }
                }
              } catch (IllegalArgumentException e) {
                // do nothing
              }
            }
          }
        }
      }
    }
    return found;
  }
  
  
  private boolean operationConversionRequired(Set<XMOMDatabaseEntryColumn> relations) {
    return relations.contains(XMOMDatabaseEntryColumn.INSTANCESERVICEREFERENCEOF) || relations.contains(XMOMDatabaseEntryColumn.CALLEDBY);
  }


  private static class OpenDeploymentTasks implements Transformation<DeploymentMarker, DeploymentMarker> {

    public DeploymentMarker transform(DeploymentMarker from) {
      if (from instanceof DeploymentTask) {
        DeploymentTask task = (DeploymentTask)from;
        if (!task.isDone()) {
          return task;
        }
      }
      
      return null;
    }
    
  }
  
  private static class DeploymentItemStateReportJoin implements Join<DeploymentItemStateReport, DeploymentMarker, DeploymentItemIdentifier, DeploymentItemStateReport> {

    boolean onlyCount;
    
    public DeploymentItemStateReportJoin(boolean onlyCount) {
      this.onlyCount = onlyCount;
    }

    public DeploymentItemIdentifier leftKey(DeploymentItemStateReport left) {
      return new DeploymentItemIdentificationBase(left.getType(), left.getFqName());
    }

    public DeploymentItemIdentifier rightKey(DeploymentMarker right) {
      return right.getDeploymentItem();
    }

    public DeploymentItemStateReport join(DeploymentItemIdentifier key, List<DeploymentItemStateReport> lefts,
                                          List<DeploymentMarker> rights) {
      DeploymentItemStateReport left = lefts.get(0); //andere Anzahlen kann es nicht geben, da DeploymentItemIdentifier eindeutig ist
      if (rights == null) {
        return left;
      }
      
      if (onlyCount) {
        left.setOpenTaskCount(rights.size());
      } else {
        left.addDeploymentMarker(rights);
      }
      return left;
    }
  }

  
  private long getRevisionFromFilterMap(String application, String version, String workspace) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    if (application != null && workspace != null) {
      //appdefinition
      return revisionManagement.getRevision(null, null, workspace);
    }
    return revisionManagement.getRevision(application, version, workspace);
  
  }
  
  public void save(DeploymentItem di, long revision) {
    lazyCreateOrGet(revision).save(di);
  }


  public void save(String fqName, long revision) {
    lazyCreateOrGet(revision).save(fqName);
  }


  public void delete(String fqName, DeploymentContext ctx, long revision) {
    lazyCreateOrGet(revision).delete(fqName, ctx);
  }


  public void collectUsingObjectsInContext(String fqName, DeploymentContext ctx, long revision) {
    lazyCreateOrGet(revision).collectUsingObjectsInContext(fqName, ctx);
  }


  public void undeploy(String fqName, DeploymentContext ctx, long revision) {
    lazyCreateOrGet(revision).undeploy(fqName, ctx);
  }


  public void deployFinished(String fqName, DeploymentTransition transition, boolean copiedXMLFromSaved, Optional<Throwable> deploymentException, long revision) {
    lazyCreateOrGet(revision).deployFinished(fqName, transition, copiedXMLFromSaved, deploymentException);
  }


  public void buildFinished(String fqName, Optional<Throwable> buildException, long revision) {
    lazyCreateOrGet(revision).buildFinished(fqName, buildException);
  }
  
  
  public DeploymentItemRegistry lazyCreateOrGet(long revision) {
    DeploymentItemRegistry dir = registries.get(revision);
    if (dir == null) {
      DeploymentItemRegistry disr = new DeploymentItemStateRegistry(revision);
      dir = registries.putIfAbsent(revision, disr);
      if (dir == null) {
        dir = disr;
      }
    }
    return dir;
  }

  public DeploymentItemRegistry removeRegistry(long revision) {
    DeploymentItemRegistry dir = registries.remove(revision);
    if (dir != null) {
      DeploymentContext ctx = new DeploymentContext(new GenerationBaseCache());
      for (DeploymentItemState dis : dir.list()) {
        dir.delete(dis.getName(), ctx);
      }
    }
    return dir;
  }
  
  
  protected void discoverItems() {
    try {
      ODSImpl.getInstance().registerStorable(DeploymentItemStateStorable.class);
    } catch (PersistenceLayerException e) {
      throw new IllegalStateException("Failed to register DeploymentItemStateStorable", e);
    }
    final RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<Long> allRevisions = revMgmt.getAllRevisions();
    Map<DeploymentLocation, GenerationBaseCache> revisionLocationCache =
        new EnumMap<DeploymentLocation, GenerationBaseCache>(DeploymentLocation.class);
    final GenerationBaseCache gbcDeployed = new GenerationBaseCache();
    revisionLocationCache.put(DeploymentLocation.DEPLOYED, gbcDeployed);
    //objekte aus deployed-cache wiederverwenden
    revisionLocationCache.put(DeploymentLocation.SAVED, new GenerationBaseCache() {

      @Override
      public InnerMap createInnerMap(long revision) {
        if (revMgmt.isWorkspaceRevision(revision)) {
          return super.createInnerMap(revision);
        } else {
          return gbcDeployed.getOrCreateInnerMap(revision);
        }
      }
    });
    orderRevisions(allRevisions, revMgmt);
    for (Long aRevision : allRevisions) {
      discoverItems(aRevision, revisionLocationCache);
    }
    initialized = true;
  }
  
  /*
   * wegen bug 24827 processing-app(s) vorziehen, vgl auch kommentar weiter bei sort
   */
  private void orderRevisions(List<Long> allRevisions, final RevisionManagement revMgmt) {
    Collections.sort(allRevisions, new Comparator<Long>() {

      @Override
      public int compare(Long o1, Long o2) {
        int priority1 = priority(o1);
        int priority2 = priority(o2);
        return Integer.compare(priority1, priority2);
      }

      private int priority(Long rev) {
        RuntimeContext rc;
        try {
          rc = revMgmt.getRuntimeContext(rev);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(e);
        }        
        if (rc instanceof Application && rc.getName().equals("Processing")) {
          return 0;
        }
        return 1;
      }
      
    });
  }

  public void discoverItems(long aRevision) { 
    discoverItems(aRevision, new EnumMap<DeploymentLocation, GenerationBaseCache>(DeploymentLocation.class));
  }
    
  public void discoverItems(long aRevision, Map<DeploymentLocation, GenerationBaseCache> revisionLocationCache) {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<String, Set<DeploymentLocation>> allFqNames = new HashMap<String, Set<DeploymentLocation>>();
    Set<String> fqNames = XMOMDatabase.discoverAllXMOMFqNames(aRevision, true);
    for (String fqName : fqNames) {
      Set<DeploymentLocation> source = new HashSet<DeploymentLocation>();
      source.add(DeploymentLocation.DEPLOYED);
      allFqNames.put(fqName, source);
    }
    if (revMgmt.isWorkspaceRevision(aRevision)) {
      fqNames = XMOMDatabase.discoverAllXMOMFqNames(aRevision, false);
      for (String fqName : fqNames) {
        Set<DeploymentLocation> source;
        if (allFqNames.containsKey(fqName)) {
          source = allFqNames.get(fqName);
        } else {
          source = new HashSet<DeploymentLocation>();
        }
        source.add(DeploymentLocation.SAVED);
        allFqNames.put(fqName, source);
      }
      
    }
    boolean isApplication = revMgmt.isApplicationRevision(aRevision);
    Set<DeploymentItem> allItems = new HashSet<DeploymentItem>();
    List<Entry<String, Set<DeploymentLocation>>> ordered = sort(allFqNames.entrySet());
    for (Entry<String, Set<DeploymentLocation>> entry : ordered) {
      try {
        Optional<DeploymentItem> di = DeploymentItemBuilder.build(entry.getKey(), Optional.<XMOMType>empty(), entry.getValue(), aRevision, isApplication, revisionLocationCache);
        if (di.isPresent()) {
          allItems.add(di.get());
        } else {
          logger.warn("Failed to discover DeploymentItem '" + entry.getKey() + "' in revision " + aRevision);  
        }
      } catch (Throwable t) {
        logger.warn("Failed to discover DeploymentItem '" + entry.getKey() + "' in revision " + aRevision, t);
        Department.handleThrowable(t);
      }
    }
    DeploymentItemStateRegistry disr = (DeploymentItemStateRegistry) lazyCreateOrGet(aRevision);
    try {
      disr.init(allItems);
    } catch (Throwable t) {
      logger.warn("Failed to discover DeploymentItems in revision " + aRevision, t);
      Department.handleThrowable(t);
    }
  }

  /*
   * Sortieren wegen Bug 24827
   * Es sollen xprc.xpce.RuntimeContext und xprc.xpce.AnyInputPayload vorgezogen werden, weil sie von RemoteCall verwendet werden
   *   RemoteCall ist "serverintern" (vgl GenerationBase.mdmObjectMappingToJavaClasses), RuntimeContext/AnyInputPayload nicht
   *   Deshalb funktioniert die "Vererbung" des DeploymentModes nicht richtig.
   */
  private List<Entry<String, Set<DeploymentLocation>>> sort(Set<Entry<String, Set<DeploymentLocation>>> entrySet) {
    List<Entry<String, Set<DeploymentLocation>>> l = new ArrayList<>(entrySet);
    Collections.sort(l, new Comparator<Entry<String, Set<DeploymentLocation>>>() {
      
      @Override
      public int compare(Entry<String, Set<DeploymentLocation>> o1, Entry<String, Set<DeploymentLocation>> o2) {
        int priority1 = priority(o1);
        int priority2 = priority(o2);
        return Integer.compare(priority1, priority2);
      }

      private int priority(Entry<String, Set<DeploymentLocation>> e) {        
        if (e.getKey().equals(GenerationBase.getFqNamesOfTypesUsedByRemoteCall()[0])) {
          return 0;
        } else if (e.getKey().equals(GenerationBase.getFqNamesOfTypesUsedByRemoteCall()[1])) {
          return 1;
        } else {
          return 2;
        }
      }
      
    });
    return l;
  }

  public boolean isInitialized() {
    return initialized;
  }
  

  public void update(DeploymentItem di, Set<DeploymentLocation> locations, long revision) {
    lazyCreateOrGet(revision).update(di, locations);
  }


  /**
   * sammelt transitions (auch die, wo der state sich gar nicht ändert) zwischen beiden resolvern von allen objekten der rootrevision
   */
  public List<StateTransition> collectStateChangesBetweenResolvers(long rootRevision,
                                                                              CrossRevisionResolver crossResolver,
                                                                              CrossRevisionResolver otherCrossResolver) {
    DeploymentItemRegistry rootRegistry = lazyCreateOrGet(rootRevision);
    Set<DeploymentItemState> states = rootRegistry.list();
    Map<String, DeploymentItemStateReport> stateReports = new HashMap<String, DeploymentItemStateReport>();
    for (DeploymentItemState dis : states) {
      if (dis.exists()) {
        stateReports.put(dis.getName(), dis.getStateReport(DeploymentItemStateImpl.getDefaultDetailSelect(), crossResolver));
      }
    }
    Map<String, DeploymentItemStateReport> otherStateReports = new HashMap<String, DeploymentItemStateReport>();
    for (DeploymentItemState dis : states) {
      if (dis.exists()) {
        otherStateReports.put(dis.getName(), dis.getStateReport(DeploymentItemStateImpl.getDefaultDetailSelect(), otherCrossResolver));
      }
    }
    List<StateTransition> changedState = new ArrayList<StateTransition>();
    for (String key : stateReports.keySet()) {
      DeploymentItemStateReport stateReport = stateReports.get(key);
      // TODO are there irrelevant stateChanges?
      StateTransition tr = new StateTransition(stateReport.getFqName(), stateReport.getType(), stateReport.getState(),
                          otherStateReports.get(key).getState(), stateReport.getInconsitencies(),
                          otherStateReports.get(key).getInconsitencies());
      changedState.add(tr);
    }
    return changedState;
  }
  
  
  public static class StateTransition {
    
    private final String fqName;
    private final XMOMType type;
    private final DisplayState fromState;
    private final DisplayState toState;
    private final List<Inconsistency> inconsistenciesFrom;
    private final List<Inconsistency> inconsistenciesTo;
    
    StateTransition(String fqName, XMOMType type, DisplayState fromState, DisplayState toState, List<Inconsistency> inconsistenciesFrom, List<Inconsistency> inconsistenciesTo) {
      this.fqName = fqName;
      this.type = type;
      this.fromState = fromState;
      this.toState = toState;
      this.inconsistenciesFrom = inconsistenciesFrom;
      this.inconsistenciesTo = inconsistenciesTo;
    }
    
    public String getFqName() {
      return fqName;
    }
    
    public XMOMType getType() {
      return type;
    }
    
    public boolean turnedInvalid() {
      return stateChanged() && toState == DisplayState.INVALID;
    }
    
    public boolean stateChanged() {
      return fromState != toState;
    }

    public boolean turnedValid() {
      return stateChanged() && fromState == DisplayState.INVALID;
    }
    
    public DisplayState getFromState() {
      return fromState;
    }
    
    public DisplayState getToState() {
      return toState;
    }
    
    public List<Inconsistency> getInconsistenciesFrom() {
      return inconsistenciesFrom;
    }
    
    public List<Inconsistency> getInconsistenciesTo() {
      return inconsistenciesTo;
    }
    
  }

  public DeploymentItemRegistry getRegistry(long rev) {
    return registries.get(rev);
  }

  public void check() {
    for (DeploymentItemRegistry reg : registries.values()) {
      ((DeploymentItemStateRegistry) reg).check();
    }
  }
  
}

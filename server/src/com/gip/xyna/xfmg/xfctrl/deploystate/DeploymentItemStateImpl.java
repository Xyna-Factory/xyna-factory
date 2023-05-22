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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Dependency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.DependencyState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ProblemType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ServiceImplInconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ServiceImplInconsistencyState;
import com.gip.xyna.xfmg.xfctrl.deploystate.InconsistencyProvider.InconsistencyProviderWithTypeInference;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.CrossRevisionResolver;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.DetachedOrderTypeEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.InterfaceWithPotentiallyUnknownProvider;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.OrderTypeEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.UsageOfOutputsOfWFReferencedByInputSource;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.ImplementationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.OperationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.SupertypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeMissmatch;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemColumn;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemSelectImpl;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortal.Identity;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype.DatatypeBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.ExceptionType.ExceptionTypeBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.HierarchyTypeWithVariables.HierarchyTypeWithVariablesBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.SnippetOperation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.SnippetOperation.SnippetOperationBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable.VariableBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Workflow;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


public class DeploymentItemStateImpl extends DeploymentItemIdentificationBase implements DeploymentItemState {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(DeploymentItemStateImpl.class);
  private final static DeploymentItemSelectImpl SELECT_STATE;
  
  static {
    SELECT_STATE = new DeploymentItemSelectImpl();
    SELECT_STATE.select(DeploymentItemColumn.STATE);
  }
  
  public final DeploymentItemRegistry registry;
  
  private final PublishedInterfaces publishedInterfacesDeployed;
  private final PublishedInterfaces publishedInterfacesSaved;
  private final Map<DeploymentLocation, Set<DeploymentItemInterface>> interfaceEmployment;
  
  private Optional<DeploymentTransition> lastDeploymentTransition;
  private long lastModified;
  private String lastModifiedBy;
  private long lastDeployed;
  private long lastStateChange;
  private String lastStateChangeBy;
  private long lastOperationInterfaceChange;
  private boolean locationContentChanges;
  private boolean exists = false;
  private boolean deployed = false;
  private boolean applicationItem = false;
  private Optional<SerializableExceptionInformation> rollbackCause; //deploymentError
  private Optional<SerializableExceptionInformation> rollbackError;
  private Optional<SerializableExceptionInformation> buildError;
  private final Map<DeploymentLocation, Set<DeploymentItemState>> callSites;
  private ConcurrentMap<DeploymentLocation, ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport>> callSitesPerOperation;
  private File savedImplJar; //gespeichertes ImplJar f�r Services (null, falls es nicht verwendet wird)
  private boolean useDeployedImplJar; //wird das ImplJar vom deployten Service verwendet?
  private ServiceImplInconsistency deployedServiceChangedInconsistency; //ImplInconsistency durch Service-�nderungen (wird beim deployen von saved �bernommen)
  private String label;
  private volatile CachedStateReport cachedReport;

  public DeploymentItemStateImpl(DeploymentItem item, DeploymentItemRegistry registry) {
    this(item.getType(), item.getName(), registry);
    setSpecialType(item.getSpecialType());
    publishedInterfacesSaved.addAll(item.getPublishedInterfaces().get(DeploymentLocation.SAVED));
    publishedInterfacesDeployed.addAll(item.getPublishedInterfaces().get(DeploymentLocation.DEPLOYED));
    interfaceEmployment.get(DeploymentLocation.SAVED).addAll(item.getInterfaceEmployment().get(DeploymentLocation.SAVED));
    interfaceEmployment.get(DeploymentLocation.DEPLOYED).addAll(item.getInterfaceEmployment().get(DeploymentLocation.DEPLOYED));
    this.exists = true;
    this.locationContentChanges = item.locationContentChanges();
    if (item.isDeployed()) {
      this.lastDeploymentTransition = Optional.of(DeploymentTransition.SUCCESS);
      this.deployed = true;
    }
    if (item.isIncomplete()) {
      this.lastDeploymentTransition = Optional.of(DeploymentTransition.ERROR_DURING_ROLLBACK);
    }
    this.applicationItem = item.isApplicationItem();
    lastModified = item.getLastModified();
    lastDeployed = item.getLastDeployed();
    savedImplJar = item.getSavedImplJar();
    
    label = item.getLabel();
  }
  
  
  private DeploymentItemStateImpl(XMOMType type, String name, DeploymentItemRegistry registry) {
    super(type, name);
    this.registry = registry;
    lastDeploymentTransition = Optional.empty();
    rollbackCause = Optional.empty();
    rollbackError = Optional.empty();
    buildError = Optional.empty();
    this.interfaceEmployment = new EnumMap<DeploymentItemState.DeploymentLocation, Set<DeploymentItemInterface>>(DeploymentLocation.class);
    this.callSites = Collections.synchronizedMap(new EnumMap<DeploymentItemState.DeploymentLocation, Set<DeploymentItemState>>(DeploymentLocation.class));
    this.callSitesPerOperation = new ConcurrentHashMap<DeploymentLocation, ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport>>();
    // be lazy
    this.publishedInterfacesSaved = new PublishedInterfaces();
    this.publishedInterfacesDeployed = new PublishedInterfaces();
    this.interfaceEmployment.put(DeploymentLocation.SAVED, new HashSet<DeploymentItemInterface>());
    this.interfaceEmployment.put(DeploymentLocation.DEPLOYED, new HashSet<DeploymentItemInterface>());
    this.callSites.put(DeploymentLocation.SAVED, new HashSet<DeploymentItemState>());
    this.callSites.put(DeploymentLocation.DEPLOYED, new HashSet<DeploymentItemState>());
    updateLastModified(-1);
    updateLastStateChange();
  }
  

  public boolean deploymentLocationContentChanges() {
    return locationContentChanges;
  }
  
  
  public void setDeploymentLocationContentChanges() {
    locationContentChanges = true;
  }


  public Optional<DeploymentTransition> getLastDeploymentTransition() {
    return lastDeploymentTransition;
  }
  
  public long getLastModified() {
    return lastModified;
  }
  
  public long getLastDeployed() {
    return lastDeployed;
  }
  
  public Set<DeploymentItemInterface> getInconsistencies(DeploymentLocation ownLocation,
                                                         DeploymentLocation interfaceProviderLocation,
                                                         boolean tryToInferTypes) {
    return getInconsistencies(ownLocation, interfaceProviderLocation, tryToInferTypes, null);
  }
  
  public Set<DeploymentItemInterface> getInconsistencies(DeploymentLocation ownLocation,
                                                         DeploymentLocation interfaceProviderLocation,
                                                         boolean tryToInferTypes,
                                                         CrossRevisionResolver resolver) {
    if (resolver != null) {
      InterfaceResolutionContext.tryInitCtx(this, resolver);
    }
    InterfaceResolutionContext.updateCtx(interfaceProviderLocation, this);
    try {
      Set<DeploymentItemInterface> baseSet = interfaceEmployment.get(ownLocation);
      Set<DeploymentItemInterface> inconsistencies = new HashSet<DeploymentItemInterface>();
      for (DeploymentItemInterface dii : baseSet) {
        if (!dii.resolve()) {
          DeploymentItemInterface inconsistency;
          if (dii instanceof InconsistencyProviderWithTypeInference) {
            inconsistency = ((InconsistencyProviderWithTypeInference)dii).getInconsistency(tryToInferTypes);
          } else if (dii instanceof InconsistencyProvider) {
            inconsistency = ((InconsistencyProvider)dii).getInconsistency();
          } else {
            inconsistency = dii;
          }
          if (isDeployed()) {
            inconsistencies.add(inconsistency);
          } else {
            if (inconsistency instanceof UnresolvableInterface
                || (inconsistency instanceof InterfaceWithPotentiallyUnknownProvider && !((InterfaceWithPotentiallyUnknownProvider) inconsistency)
                    .providerIsKnown())) {
              inconsistencies.add(inconsistency);
            } else {
              DeploymentItemState providerState = InterfaceResolutionContext.resCtx.get().resolveProvider(inconsistency);
              if (interfaceProviderLocation == DeploymentLocation.DEPLOYED && !(providerState.isDeployed() || !providerState.exists())) {
                // ntbd, don't add an inconsistency if both are only saved
              } else {
                inconsistencies.add(inconsistency);
              }
            }
          }
        }
      }
      return inconsistencies;
    } finally {
      InterfaceResolutionContext.revertCtx();
    }
  }


  public static DeploymentItemState createPhantom(DeploymentItemInterface dii, DeploymentItemRegistry registry) {
    TypeInterface provider = InterfaceResolutionContext.getProviderType(dii);
    return new DeploymentItemStateImpl(provider.getType(), provider.getName(), registry);
  }
  
  private boolean callSitesDirty = false;

  public void collectUsingObjectsInContext(DeploymentContext ctx) {
    Optional<DeploymentMode> myMode = ctx.getDeploymentMode(getType(), getName(), getRevision());
    if (myMode.isPresent() &&
        myMode.get().shouldCopyXMLFromSavedToDeployed()) {
      cleanCallSites();
      addToCtxIfDeployed(ctx, callSites.get(DeploymentLocation.DEPLOYED));
    }
  }
  
  
  private void cleanCallSites() {
    //beim �ndern von runtimecontextdependencies werden callsites nicht aufger�umt
    if (callSitesDirty) {
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      cleanCallSitesInternally(rcdm, DeploymentLocation.DEPLOYED);
      cleanCallSitesInternally(rcdm, DeploymentLocation.SAVED);
      callSitesDirty = false;
    }
  }


  private void cleanCallSitesInternally(RuntimeContextDependencyManagement rcdm, DeploymentLocation location) {
    Iterator<DeploymentItemState> it = callSites.get(location).iterator();
    long r2 = getRevision();
    while (it.hasNext()) {
      DeploymentItemStateImpl disi = (DeploymentItemStateImpl) it.next();
      long r1 = disi.getRevision();
      if (r1 != r2 && !rcdm.isDependency(r1, r2)) {
        it.remove();
      }
    }
    ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport> m = callSitesPerOperation.get(location);
    if (m != null) {
      for (CallerSetWithRemovalSupport cs : m.values()) {
        it = cs.set.iterator();
        while (it.hasNext()) {
          DeploymentItemStateImpl disi = (DeploymentItemStateImpl) it.next();
          long r1 = disi.getRevision();
          if (r1 != r2 && !rcdm.isDependency(r1, r2)) {
            it.remove();
          }
        }
      }
    }
  }


  private void addToCtxIfDeployed(DeploymentContext ctx, Set<DeploymentItemState> diss) {
    for (DeploymentItemState dis : diss) {
      if (dis != null && dis.isDeployed()) {
        ctx.addObjectForCodeRegeneration(dis.getType(), dis.getName(), ((DeploymentItemStateImpl) dis).getRevision());
      }
    }
  }
  

  public void undeploy(DeploymentContext ctx) {
    updateLastStateChange(); // or could we be reentrant
    // TODO if not part of the same undeployment?
    cleanCallSites();
    addToCtxIfDeployed(ctx, callSites.get(DeploymentLocation.DEPLOYED)); // for deactivation
    unregisterAsCaller(Collections.singleton(DeploymentLocation.DEPLOYED));

    publishedInterfacesDeployed.clear();
    deployed = false;
    lastDeploymentTransition = Optional.empty();
    locationContentChanges = false;
    interfaceEmployment.get(DeploymentLocation.DEPLOYED).clear();
    
    invalidateCache();
    for (DeploymentItemState dis : callSites.get(DeploymentLocation.DEPLOYED)) {
      if (dis instanceof DeploymentItemStateImpl) {
        ((DeploymentItemStateImpl)dis).invalidateCache();
      }
    }
  }


  public boolean deploymentTransition(DeploymentTransition transition, boolean fromSaved, Optional<? extends Throwable> deploymentException) {
    boolean changed = false;
    if (transition != null) {
      switch (transition) {
        case ROLLBACK :
          Optional<SerializableExceptionInformation> ex = handleException(deploymentException);
          changed = rollbackError.isPresent() || !ex.equals(rollbackCause);
          rollbackCause = ex;
          rollbackError = Optional.empty();
          break;
        case ERROR_DURING_ROLLBACK :
          ex = handleException(deploymentException);
          changed = !ex.equals(rollbackError);
          rollbackError = ex;
          break;
        case SUCCESS :
          if (fromSaved || applicationItem) {
            changed = rollbackCause.isPresent();
            rollbackCause = Optional.empty();
          }
          // fallthrough
        case SUCCESSFULL_ROLLBACK :
          changed = changed || rollbackError.isPresent();
          rollbackError = Optional.empty();
          if (fromSaved && !applicationItem) { // applicationItem should never be true for SUCCESSFULL_ROLLBACK
            changed = true;
            publishedInterfacesDeployed.clear();
            publishedInterfacesDeployed.addAll(publishedInterfacesSaved.getAll());
            Set<DeploymentItemInterface> deployedInterfaceEmployments = interfaceEmployment.get(DeploymentLocation.DEPLOYED);
            deployedInterfaceEmployments.clear();
            deployedInterfaceEmployments.addAll(interfaceEmployment.get(DeploymentLocation.SAVED));
            locationContentChanges = false;
            
            updateLastDeployed();
            transferSavedServiceImplInconsistencyToDeployed();
          }
          changed = changed || !deployed;
          deployed = true;
          break;
        default :
          break;
      }
      Optional<DeploymentTransition> newDeploymentTransition;
      if (transition == DeploymentTransition.SUCCESSFULL_ROLLBACK) {
        newDeploymentTransition = Optional.of(DeploymentTransition.SUCCESS);
      } else {
        newDeploymentTransition = Optional.of(transition);
      }
      changed = changed || !lastDeploymentTransition.equals(newDeploymentTransition);
      lastDeploymentTransition = newDeploymentTransition;
      if (changed) {
        invalidateCache();
        cleanCallSites();
        for (DeploymentItemState dis : callSites.get(DeploymentLocation.DEPLOYED)) {
          if (dis instanceof DeploymentItemStateImpl) {
            ((DeploymentItemStateImpl) dis).invalidateCache();
          }
        }
      }
    }
    return changed;
  }

  public void setBuildError(Optional<? extends Throwable> buildException) {
    buildError = handleException(buildException);
  }
  
  
  private Optional<SerializableExceptionInformation> handleException(Optional<? extends Throwable> throwable) {
    if (throwable.isPresent()) {
      Optional<? extends Throwable> unwrappedThrowable = throwable;
      while (unwrappedThrowable.isPresent() &&
             unwrappedThrowable.get().getCause() != null) {
        Throwable cause = unwrappedThrowable.get().getCause();
        if (cause instanceof XPRC_MDMDeploymentException) {
          unwrappedThrowable = Optional.of(cause);
        } else if (cause instanceof XPRC_InheritedConcurrentDeploymentException) {
          unwrappedThrowable = Optional.of(cause);
        } else if (cause instanceof MDMParallelDeploymentException) {
          unwrappedThrowable = Optional.of(cause);
        } else {
          return Optional.of(ErroneousOrderExecutionResponse.generateSerializableExceptionInformation(unwrappedThrowable.get(), new ResultController()));
        }
      }
      return Optional.of(ErroneousOrderExecutionResponse.generateSerializableExceptionInformation(unwrappedThrowable.get(), new ResultController()));
    } else {
      return Optional.<SerializableExceptionInformation>empty();
    }
  }


  public void save(DeploymentItem di) {
    DisplayState state;
    try {
      state = deriveDisplayState();
    } catch (Throwable t) {
      state = null;
      logger.warn("Could not evaluate state of " + di.getName() + " as it was before save.", t);
    }
    update(di, Collections.singleton(DeploymentLocation.SAVED));
    if (exists) {
      try {
        this.locationContentChanges = !FileUtils.compareXMLs(new File(GenerationBase.getFileLocationOfXmlNameForSaving(getName(), registry.getManagedRevision())+ ".xml"),
                                                             new File(GenerationBase.getFileLocationOfXmlNameForDeployment(getName(), registry.getManagedRevision()) + ".xml"));
      } catch (Ex_FileWriteException e) {
        this.locationContentChanges = true;
      }
    } else {
      exists = true;
    }
    updateLastModified(di.getLastModified());
    DisplayState newState;
    try {
      newState = deriveDisplayState();
    } catch (Throwable t) {
      newState = null;
      logger.warn("Could not evaluate state of " + di.getName() + " as it was after save.", t);
    }
    if (state != newState) {
      updateLastStateChange();
    }
    invalidateCache();
    cleanCallSites();
    for (DeploymentItemState dis : callSites.get(DeploymentLocation.SAVED)) {
      if (dis instanceof DeploymentItemStateImpl) {
        ((DeploymentItemStateImpl)dis).invalidateCache();
      }
    }
  }
  
  
  private void unregisterAsCaller(Set<DeploymentLocation> locations) {
    for (DeploymentLocation location : locations) {
      InterfaceResolutionContext.updateCtx(location, this);
      try {
        InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
        for (DeploymentItemInterface dii : interfaceEmployment.get(location)) {
          try {
            DeploymentItemState dis = resCtx.resolveProvider(dii);
            if (dis != null) {
              dis.removeInvocationSite(this, location);
            }
          } catch (IllegalArgumentException e) {
            // TODO provide DeploymentItemInterface.isResolveable instead of just calling and catching
          }
        }
      } finally {
        InterfaceResolutionContext.revertCtx();
      }
    }
  }
  
  
  public long getRevision() {
    return registry.getManagedRevision();
  }
  
  
  public void removeInvocationSite(DeploymentItemState dis, DeploymentLocation location) {
    callSites.get(location).remove(dis);
    ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport> callSitesByLocation = callSitesPerOperation
        .get(location);
    if (callSitesByLocation != null) {
      for (String key : callSitesByLocation.keySet()) {
        CallerSetWithRemovalSupport set = callSitesByLocation.get(key);
        if (set != null) {
          set = callSitesByLocation.lazyCreateGet(key);
          set.getCallers().remove(dis);
        }
        callSitesByLocation.cleanup(key);
      }
    }
  }
  
  
  public void update(DeploymentItem di, Set<DeploymentLocation> locations) {
    unregisterAsCaller(locations);
    if (di.getType() != null) {
      this.setType(di.getType());
    }
    label = di.getLabel();
    exists = true;
    
    Set<DeploymentLocation> locationsToUpdate = filterUpdateLocations(di, locations);
    if (locationsToUpdate.contains(DeploymentLocation.SAVED)) {
      if (checkForOperationInterfaceChanges(di.getPublishedInterfaces().get(DeploymentLocation.SAVED))) {
        lastOperationInterfaceChange = di.getLastModified();
      }
      savedImplJar = di.getSavedImplJar();
      if (lastModified != di.getLastModified()) {
        lastModifiedBy = "";
        lastModified = di.getLastModified();
      }
    }
    if (locationsToUpdate.contains(DeploymentLocation.DEPLOYED)) {
      if (lastDeployed != di.getLastDeployed()) {
        lastDeployed = di.getLastDeployed();
        lastStateChange = di.getLastDeployed();
        deployedServiceChangedInconsistency = null;
        rollbackCause = Optional.empty();
        rollbackError = Optional.empty();
        buildError = Optional.empty();
      }
    }
    for (DeploymentLocation location : locationsToUpdate) {
      Set<DeploymentItemInterface> invocations = interfaceEmployment.get(location);
      invocations.clear();
      invocations.addAll(di.getInterfaceEmployment().get(location));
      PublishedInterfaces publInt = getPublishedInterfaces(location);
      publInt.clear();
      publInt.addAll(di.getPublishedInterfaces().get(location));
      validate(location);
    }
    for (DeploymentLocation location : locationsToUpdate) {
      invalidateCache();
      cleanCallSites();
      for (DeploymentItemState dis : callSites.get(location)) {
        if (dis instanceof DeploymentItemStateImpl) {
          ((DeploymentItemStateImpl)dis).invalidateCache();
        }
      }
    }
  }
  
  
  private Set<DeploymentLocation> filterUpdateLocations(DeploymentItem di, Set<DeploymentLocation> locations) {
    Set<DeploymentLocation> filteredLocations = new HashSet<DeploymentItemState.DeploymentLocation>(locations);
    if (di.getLastModified() <= 0) {
      filteredLocations.remove(DeploymentLocation.SAVED);
    }
    if (!di.isDeployed()) {
      filteredLocations.remove(DeploymentLocation.DEPLOYED);
    }
    return filteredLocations;
  }


  public DisplayState deriveDisplayState() {
    return getStateReport(SELECT_STATE).getState();
  }

  
  private void updateLastModified(long lastModified) {
    if (lastModified <= 0) {
      this.lastModified = System.currentTimeMillis();
    } else {
      this.lastModified = lastModified;
    }
    Identity identity = XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.get();
    if (identity != null && identity.getUsername() != null) {
      lastModifiedBy = identity.getUsername();
    } else {
      lastStateChangeBy = "";
    }
  }
  
  private void updateLastDeployed() {
    lastDeployed = System.currentTimeMillis();
  }

  private void updateLastStateChange() {
    lastStateChange = System.currentTimeMillis();
    Identity identity = XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.get();
    if (identity != null && identity.getUsername() != null) {
      lastStateChangeBy = identity.getUsername();
    } else {
      lastStateChangeBy = "";
    }
  }

  public void validate(DeploymentLocation ownLocation) {
    if (!applicationItem) {
      getInconsistencies(ownLocation, DeploymentLocation.SAVED, false);
    }
    getInconsistencies(ownLocation, DeploymentLocation.DEPLOYED, false);
    
    if (ownLocation == DeploymentLocation.DEPLOYED) {
      transferSavedServiceImplInconsistencyToDeployed();
    }
  }

  
  public boolean exists() {
    return exists;
  }


  public void delete(DeploymentContext ctx) {
    // TODO in this case there is no listener for added regenerates 
    undeploy(ctx);
    unregisterAsCaller(Collections.singleton(DeploymentLocation.SAVED));

    interfaceEmployment.get(DeploymentLocation.SAVED).clear();
    publishedInterfacesSaved.clear();
    this.exists = false;
  }

  public DeploymentItemStateReport getStateReport() {
    return getStateReport(null);
  }
  
  
  public DeploymentItemStateReport getStateReport(Selection selection) {
    return getStateReport(selection, null);
  }
  
  public DeploymentItemStateReport getStateReport(Selection selection, CrossRevisionResolver crossResolver) {
    if (cachedReport != null) {
      DeploymentItemStateReport report = cachedReport.getIfEqual(selection, crossResolver);
      if (report != null) {
        report.clearDeploymentMarker();
        return report;
      }
    }
    try {
      Set<DeploymentItemInterface> inconsistencies_ss = null;
      Set<DeploymentItemInterface> inconsistencies_sd = null;
      Set<DeploymentItemInterface> inconsistencies_ds = null;
      Set<DeploymentItemInterface> inconsistencies_dd = null;
      List<ServiceImplInconsistency> implInconsistencies = new ArrayList<ServiceImplInconsistency>();
      if (!applicationItem) {
        inconsistencies_ss = getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false, crossResolver);
        implInconsistencies.addAll(getServiceImplInconsistencies(DeploymentLocation.SAVED));
      }
      if (getLastDeploymentTransition().isPresent()) {
        if (!applicationItem) {
          inconsistencies_sd = getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.DEPLOYED, false, crossResolver);
          inconsistencies_ds = getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.SAVED, false, crossResolver);
        }
        inconsistencies_dd = getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false, crossResolver);
        implInconsistencies.addAll(getServiceImplInconsistencies(DeploymentLocation.DEPLOYED));
      }
      DisplayState state;
      List<Inconsistency> inconsistencies = Collections.emptyList();
      List<Dependency> dependencies = Collections.emptyList();
      List<ResolutionFailure> unresolvable = new ArrayList<ResolutionFailure>();
      if (!exists) {
        state = DisplayState.NON_EXISTENT;
      } else if (getLastDeploymentTransition().isPresent() &&
                 getLastDeploymentTransition().get() == DeploymentTransition.ERROR_DURING_ROLLBACK) {
        state = DisplayState.INCOMPLETE;
      } else if ((inconsistencies_ss != null && inconsistencies_ss.size() > 0) ||
                 (inconsistencies_sd != null && inconsistencies_sd.size() > 0) ||
                 (inconsistencies_ds != null && inconsistencies_ds.size() > 0) ||
                 (inconsistencies_dd != null && inconsistencies_dd.size() > 0) ||
                 (implInconsistencies != null && implInconsistencies.size() > 0)) {
        state = DisplayState.INVALID;
        if (selection == null ||
            selection.containsColumn(DeploymentItemColumn.RESOLUTION) || 
            selection.containsColumn(DeploymentItemColumn.STATE)) {
          inconsistencies = new ArrayList<Inconsistency>();
          Set<DeploymentItemInterface> allSet = new HashSet<DeploymentItemInterface>();
          if (inconsistencies_ss != null) {
            allSet.addAll(inconsistencies_ss);
          }
          if (getLastDeploymentTransition().isPresent()) {
            if (!applicationItem) {
              allSet.addAll(inconsistencies_sd);
              allSet.addAll(inconsistencies_ds);
            }
            allSet.addAll(inconsistencies_dd);
          }
          boolean impendingOnly = true;
          for (DeploymentItemInterface diii : allSet) {
            if (diii instanceof UnresolvableInterface
                && !(diii instanceof TypeMissmatch && ((TypeMissmatch) diii).isCausedByTypeCast())) {
              impendingOnly = false;
              unresolvable.add(ResolutionFailure.of((UnresolvableInterface) diii));
            } else if (diii instanceof InterfaceWithPotentiallyUnknownProvider && !((InterfaceWithPotentiallyUnknownProvider)diii).providerIsKnown()) {
              impendingOnly = false;
              unresolvable.add(((InterfaceWithPotentiallyUnknownProvider)diii).getResolutionFailure());
            } else if (diii instanceof InterfaceEmployment
                && (((InterfaceEmployment) diii).unwrap() instanceof OperationInterface 
                    && ((OperationInterface) ((InterfaceEmployment) diii).unwrap()).getProblemType() == ProblemType.METHOD_IS_INACTIVE)) {
              impendingOnly = false;
              unresolvable.add(ResolutionFailure.of((OperationInterface) ((InterfaceEmployment) diii).unwrap()));  
            } else {
              boolean providerExists;
              String creationHint = null;
              DeploymentItemState dis = InterfaceResolutionContext.getProvider(diii, registry);
              if (dis == null) {
                providerExists = false;
              } else {
                providerExists = dis.exists();
                if (!providerExists && dis.getType() != XMOMType.ORDERINPUTSOURCE &&
                    (selection == null || selection.containsColumn(DeploymentItemColumn.CREATION_HINT))) { 
                  creationHint = dis.createCreationHint(this.getName());
                }
              }
              InconsistencyState is = InconsistencyState.get(diii, inconsistencies_ss, inconsistencies_sd, inconsistencies_ds, inconsistencies_dd);
              inconsistencies.add(new Inconsistency(is, diii, providerExists, creationHint));
              if (!is.isImpendingOnly()) {
                impendingOnly = false;
              }
            }
          }
          if (implInconsistencies.size() > 0) {
            impendingOnly = false;
          }
          if (impendingOnly) {
            if (!deployed) {
              if (isReservedObject()) {
                state = DisplayState.INTERNAL;
              } else {
                state = DisplayState.SAVED;
              }
            } else if (isModified()) {
              state = DisplayState.CHANGED;
            } else {
              state = DisplayState.DEPLOYED;
            }
          }
        }
      } else if (!deployed) {
        if (isReservedObject()) {
          state = DisplayState.INTERNAL;
        } else {
          state = DisplayState.SAVED;
        }
      } else if (isModified()) {
        state = DisplayState.CHANGED;
      } else if (type != null && 
                 type == XMOMType.WORKFLOW &&
                 (crossResolver == null || crossResolver.checkForInvalidGeneration())) {
        try {
          String classname = GenerationBase.transformNameForJava(name);
          ClassLoader factoryClassLoader =
                          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                              .getWFClassLoader(classname, registry.getManagedRevision(), false);
          @SuppressWarnings("unchecked")
          Class<? extends XynaProcess> process = (Class<XynaProcess>) factoryClassLoader.loadClass(classname);
          XynaProcess processInstance = process.getConstructor().newInstance();
          if (processInstance.isGeneratedAsInvalid()) {
            state = DisplayState.INVALID;
          } else {
            state = DisplayState.DEPLOYED;
          }
        } catch (Throwable e) {
          state = DisplayState.INCOMPLETE;
        }
      } else {
        state = DisplayState.DEPLOYED;
      }
      if (!applicationItem && (selection == null || selection.containsColumn(DeploymentItemColumn.DEPENDENCY))) {
        dependencies = new ArrayList<Dependency>();
        Set<DeploymentItemIdentifier> allCallers = new HashSet<DeploymentItemIdentifier>();
        cleanCallSites();
        allCallers.addAll(callSites.get(DeploymentLocation.SAVED));
        allCallers.addAll(callSites.get(DeploymentLocation.DEPLOYED));
        for (DeploymentItemIdentifier caller : allCallers) {
          DeploymentItemState callerState = InterfaceResolutionContext.getProvider(caller, registry);
          if (callerState != null && callerState.exists()) {
            Set<DeploymentItemInterface> caller_inc_ss = callerState.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false);
            Set<DeploymentItemInterface> caller_inc_sd = null;
            Set<DeploymentItemInterface> caller_inc_ds = null;
            Set<DeploymentItemInterface> caller_inc_dd = null;
            if (callerState.getLastDeploymentTransition().isPresent()) {
              caller_inc_sd = callerState.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.DEPLOYED, false);
              caller_inc_ds = callerState.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.SAVED, false); 
              caller_inc_dd = callerState.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false);
            }
            Set<DeploymentItemInterface> allSet = new HashSet<DeploymentItemInterface>();
            allSet.addAll(caller_inc_ss);
            if (callerState.getLastDeploymentTransition().isPresent()) {
              allSet.addAll(caller_inc_sd);
              allSet.addAll(caller_inc_ds);
              allSet.addAll(caller_inc_dd);
            }
            
            for (DeploymentItemInterface diii : allSet) {
              if (diii instanceof InterfaceWithPotentiallyUnknownProvider) {
                //ntbd ? 
              } else if (!(diii instanceof UnresolvableInterface)) {
                try {
                  DeploymentItemState dis = InterfaceResolutionContext.getProvider(diii, registry);
                  if (dis == this) { // am I causing the inconsistency
                    // TODO trim dependencies as we could contain a type several times
                    dependencies.add(new Dependency(caller, DependencyState.get(diii, caller_inc_ss)));
                  }
                } catch (IllegalArgumentException e) {
                  throw e;
                }
              }
            }
          }
        }
      }
      
      long lastModified = this.lastModified;
      String lastModifiedBy = this.lastModifiedBy;
      if (useImplJar(DeploymentLocation.SAVED) && savedImplJar.lastModified() > lastModified) {
        lastModified = savedImplJar.lastModified();
        lastModifiedBy = "unknown (file system modification)";
        // with java7 & nio2 we could use Path & Files.getFileAttributeView to return the responsible system user (could violate privacy protection?)
      }
      DeploymentItemStateReport disr = new DeploymentItemStateReport(getName(), getType(), getSpecialType(), lastModified, lastModifiedBy,
                                                                     lastStateChange, lastStateChangeBy, state, rollbackCause,
                                                                     rollbackError, buildError, selection);
      disr.setDependencies(dependencies);
      if (selection == null || selection.containsColumn(DeploymentItemColumn.RESOLUTION)) {
        disr.setInconsistencies(inconsistencies);
        disr.setImplInconsistencies(implInconsistencies);
        disr.setUnresolvable(unresolvable);
      }
      
      
      if (selection == null || selection.containsColumn(DeploymentItemColumn.LABEL)) {
        disr.setLabel(label);
      }
      
      cachedReport = new CachedStateReport(selection, crossResolver, disr);
      
      return disr;
    } finally {
      InterfaceResolutionContext.remove();
    }
  }
  
  
  private boolean isModified() {
    if (locationContentChanges 
                    || buildError.isPresent()) { //Buildfehler als Modified anzeigen
      return true;
    } else if (isDeployed() &&
               useImplJar(DeploymentLocation.SAVED) && 
               useImplJar(DeploymentLocation.DEPLOYED)) {
      File deployedImplJar = toDeployedImpl(savedImplJar);
      if (savedImplJar.exists() && 
          deployedImplJar.exists()) {
        if (savedImplJar.lastModified() > deployedImplJar.lastModified()) {
          return true;
        } else {
          return savedImplJar.length() != deployedImplJar.length();
        }
      } else {
        // unexpected
        return false;
      }
    } else {
      return false;
    }
  }
  
  
  private File toDeployedImpl(File file) {
    return new File(file.getAbsolutePath().replaceFirst("saved"+Constants.FILE_SEPARATOR, ""));
  }
  

  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getType()).append(' ').append(getName()).append('\n')
      .append("pub saved=").append(publishedInterfacesSaved).append('\n')
      .append("pub deployed=").append(publishedInterfacesDeployed).append('\n')
      .append("inv").append(interfaceEmployment);
    return sb.toString();
  }
  
  public PublishedInterfaces getPublishedInterfaces(DeploymentLocation location) {
    if (applicationItem) {
      return publishedInterfacesDeployed;
    } else if (GenerationBase.isReservedServerObjectByFqOriginalName(this.name)) {
      return publishedInterfacesSaved;
    } else if (location == DeploymentLocation.DEPLOYED) {
      return publishedInterfacesDeployed;
    }
    return publishedInterfacesSaved;
  }


  public <I extends DeploymentItemInterface> Set<I> getPublishedInterfaces(Class<I> interfaceType, DeploymentLocation location) {
    return getPublishedInterfaces(location).filterInterfaces(interfaceType);
  }
  
  
  private static <I extends DeploymentItemInterface> Set<I> filterInterfaces(Class<I> interfaceType, Set<DeploymentItemInterface> interfaces) {
    Set<I> foundSet = new HashSet<I>();
    for (DeploymentItemInterface dii : interfaces) {
      if (interfaceType.isInstance(dii)) {
        foundSet.add(interfaceType.cast(dii));
      }
    }
    return foundSet;
  }

  
  public <I extends DeploymentItemInterface> Optional<I> getPublishedInterface(Class<I> interfaceType, DeploymentLocation location) {
    Set<I> s = getPublishedInterfaces(interfaceType, location);
    if (s.size() == 0) {
      return Optional.<I>empty();
    }
    return Optional.of(s.iterator().next());
  }


  public boolean isDeployed() {
    return deployed;
  }


  public synchronized void addInvocationSite(DeploymentItemState dis, DeploymentLocation location) {
    callSites.get(location).add(dis);
  }

  private static class CallerSetWithRemovalSupport extends ObjectWithRemovalSupport{

    private final Set<DeploymentItemState> set = Collections.synchronizedSet(new HashSet<DeploymentItemState>());
    
    @Override
    protected boolean shouldBeDeleted() {
      return set.isEmpty();
    }
    
    public Set<DeploymentItemState> getCallers() {
      return set;
    }
  }
  
  @Override
  public void addOperationInvocationSite(DeploymentItemState callerOfOperation, OperationInterface operationInterface,
      DeploymentLocation location) {
    ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport> callersForOperations = callSitesPerOperation.get(location);
    if(callersForOperations == null) {
      callersForOperations = new ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport>(){

        @Override
        public CallerSetWithRemovalSupport createValue(String key) {
          return new CallerSetWithRemovalSupport();
        }
        
      };
      ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport> prev = callSitesPerOperation.putIfAbsent(location, callersForOperations);
      if(prev != null) {
        callersForOperations = prev;
      }
    }
    CallerSetWithRemovalSupport callers = callersForOperations.lazyCreateGet(operationInterface.getName());
    callers.getCallers().add(callerOfOperation);
    callersForOperations.cleanup(operationInterface.getName());
  }

  public Set<DeploymentItemState> getInvocationSites(DeploymentLocation location) {
    cleanCallSites();
    return callSites.get(location);
  }
  
  public Set<DeploymentItemState> getInvocationSitesPerOperation(DeploymentLocation location, String operationName) {
    ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport> callSitesLocation = callSitesPerOperation.get(location);
    if(callSitesLocation == null) {
      return new HashSet<DeploymentItemState>();
    }
    CallerSetWithRemovalSupport callers = callSitesLocation.get(operationName);
    if(callers == null) {
      return new HashSet<DeploymentItemState>();
    }
    return callers.getCallers();
  }
  
  public Map<String, Set<DeploymentItemState>> getInvocationSitesPerOperation(DeploymentLocation location) {
    Map<String, Set<DeploymentItemState>> back = new HashMap<>();
    ConcurrentMapWithObjectRemovalSupport<String, CallerSetWithRemovalSupport> callSitesByLocation = callSitesPerOperation.get(location);
    if(callSitesByLocation == null) {
      return back;
    }
    for(Entry<String, CallerSetWithRemovalSupport> e: callSitesByLocation.entrySet()) {
      if(e.getValue() != null) {
        back.put(e.getKey(), e.getValue().getCallers());
      }
    }
    return back;
  }

  /**
   * Bestehen Inkonsistenzen wegen einem veralteten ImplJar?
   * @param location
   * @param onlyInterfaceChangeInconsistencies es werden nur die Inkonsistenzen, die wegen einer �nderung am Service auftreten beachtet
   * @return
   */
  public boolean hasServiceImplInconsistencies(DeploymentLocation location, boolean onlyInterfaceChangeInconsistencies) {
    if (onlyInterfaceChangeInconsistencies) {
      return getServiceInterfaceChangeInconsistency(location) != null;
    } else {
      return getServiceImplInconsistencies(location).size() > 0;
    }
  }
  
  /**
   * Ermittelt, ob eine Inkonsistenz wegen �nderungen am Service besteht, d.h. ob das ImplJar veraltet ist.
   * @param location
   * @return
   */
  private ServiceImplInconsistency getServiceInterfaceChangeInconsistency(DeploymentLocation location) {
    ServiceImplInconsistency inc = null;
    if (location == DeploymentLocation.SAVED) {
      //ist der Service selbst neuer als das jar?
      inc = getSavedServiceImplInconsistency(getLatestOperationInterfaceChangeAcrossHierarchy(location), true);
    }
    
    if (location == DeploymentLocation.DEPLOYED) {
      //wurde der Service in einem inkosistenten Zustand deployed?
      inc = deployedServiceChangedInconsistency;
    }
    
    return inc;
  }
  
  
  private long getLatestOperationInterfaceChangeAcrossHierarchy(DeploymentLocation location) {
    long latestOperationInterfaceChange = lastOperationInterfaceChange;
    InterfaceResolutionContext.updateCtx(location, this);
    try {
      Optional<TypeInterface> supertype = InterfaceResolutionContext.resCtx.get().getLocalSupertype();
      if (supertype.isPresent()) {
        DeploymentItemState dis = InterfaceResolutionContext.getProvider((DeploymentItemIdentifier)supertype.get(), registry);
        if (dis instanceof DeploymentItemStateImpl) {
          return Math.max(latestOperationInterfaceChange, ((DeploymentItemStateImpl)dis).getLatestOperationInterfaceChangeAcrossHierarchy(location));
        }
      }
      return latestOperationInterfaceChange;
    } finally {
      InterfaceResolutionContext.revertCtx();
    }
  }


  /**
   * Ermittelt, ob eine Inkonsistenz besteht, weil lastModified j�nger als das ImplJar ist.
   * @param lastModified
   * @param interfaceChangeInconsistencies true, falls sich die Inkonsistenz auf eine Service-�nderung bezieht;
   *          false, falls sich die Inkonsistenz auf ein verwendetes Objekt bezieht
   * @return
   */
  private ServiceImplInconsistency getSavedServiceImplInconsistency(long lastModified, boolean interfaceChangeInconsistencies) {
    if (savedImplJar == null) {
      //jar wird gar nicht verwendet -> ok
      return null;
    }
    
    long lastJarChange;
    if (!savedImplJar.exists()) {
      //jar wird verwendet, das File existiert aber nicht -> inkonsistent
      return new ServiceImplInconsistency(ServiceImplInconsistencyState.SAVED_MISSING_JAR, lastModified, 0);
    }
    
    lastJarChange = savedImplJar.lastModified();
    if (lastModified - XynaProperty.SERVICE_IMPL_INCONSISTENCY_TIME_LAG.getMillis() > lastJarChange) {
      return new ServiceImplInconsistency(ServiceImplInconsistencyState.get(DeploymentLocation.SAVED, interfaceChangeInconsistencies), lastModified, lastJarChange);
    }
    
    return null;
  }
  

  /**
   * Ermittelt alle Inkonsistenzen, die wegen einem veralteten ImplJar entstehen
   * @param location
   * @return
   */
  private List<ServiceImplInconsistency> getServiceImplInconsistencies(DeploymentLocation location) {
    List<ServiceImplInconsistency> incs = new ArrayList<DeploymentItemStateReport.ServiceImplInconsistency>();
    
    if (applicationItem || !useImplJar(location)) {
      //jar wird gar nicht verwendet -> ok
      return incs;
    }
    
    //Inkonsistenz aufgrund eigener �nderungen
    ServiceImplInconsistency interfaceInc = getServiceInterfaceChangeInconsistency(location);
    
    if (interfaceInc != null) {
      incs.add(interfaceInc);
      
      if (interfaceInc.getType() == ServiceImplInconsistencyState.SAVED_MISSING_JAR || interfaceInc.getType() == ServiceImplInconsistencyState.DEPLOYED_MISSING_JAR) {
        //jar existiert nicht -> abh�ngige Objekte gar nicht mehr untersuchen
        return incs;
      }
    }
    

    if (XynaProperty.SUPPRESS_USED_OBJECT_IMPL_INCONSISTENCIES.get()) {
      //Inkonsistenzen aufgrund �nderungen von verwendeten Objekten
      Set<DeploymentItemState> usedItems = new HashSet<DeploymentItemState>();
      getPublishedInterfacesRecursively(usedItems, location);
      usedItems.remove(this);
      
      for (DeploymentItemState dis : usedItems) {
        if (!dis.exists()) {
          continue;
        }
        ServiceImplInconsistency inc = null;
        if (location == DeploymentLocation.SAVED) {
          if (dis.getLastModified() < lastDeployed) {
            continue; //Service wurde noch einmal deployed und soll daher nicht mehr als invalid angezeigt werden
          }
          inc = getSavedServiceImplInconsistency(dis.getLastModified(), false);
        }
        
        if (location == DeploymentLocation.DEPLOYED) {
          if (dis.getLastDeployed() - XynaProperty.SERVICE_IMPL_INCONSISTENCY_TIME_LAG.getMillis() > lastDeployed) {
            inc = new ServiceImplInconsistency(ServiceImplInconsistencyState.get(location, false), dis.getLastDeployed(), lastDeployed);
          }
        }
        
        if (inc != null) {
          inc.setUsedDeploymentItem(dis);
          incs.add(inc);
        }
      }
    }
    
    return incs;
  }

  private boolean useImplJar(DeploymentLocation location) {
    if (location == DeploymentLocation.SAVED) {
      return savedImplJar != null;
    }else {
      return useDeployedImplJar;
    }
  }

  public void getPublishedInterfacesRecursively(Set<DeploymentItemState> result, DeploymentLocation location) {
    //OperationInterfaces
    Set<OperationInterface> operationInterfaces = getPublishedInterfaces(OperationInterface.class, location);
    for (OperationInterface op : operationInterfaces) {
      for (TypeInterface typeInterface : op.getAllTypeInterfaces()) {
        DeploymentItemState state = registry.get(typeInterface.getName());
        if (state != null && !result.contains(state)) {
          result.add(state);
          state.getPublishedInterfacesRecursively(result, location);
        }
      }
    }
    
    //MemberVariableInterfaces
    Set<MemberVariableInterface> memberVariableInterfaces = getPublishedInterfaces(MemberVariableInterface.class, location);
    for (MemberVariableInterface memberVar : memberVariableInterfaces) {
      DeploymentItemState state = registry.get(memberVar.getType().getName());
      if (state != null && !result.contains(state)) {
        result.add(state);
        state.getPublishedInterfacesRecursively(result, location);
      }
    }
    
    //SupertypeInterface
    Optional<SupertypeInterface> supertype = getPublishedInterface(SupertypeInterface.class, location);
    if (supertype.isPresent()) {
      DeploymentItemState state = registry.get(supertype.get().getName());
      if (state != null && !result.contains(state)) {
        result.add(state);
        state.getPublishedInterfacesRecursively(result, location);
      }
    }
  }
  
  /**
   * �bernimmt die Inkonsistenz wegen einer �nderung am Service vom Saved-Stand nach Deployed.
   */
  private void transferSavedServiceImplInconsistencyToDeployed() {
    if (applicationItem) {
      return;
    }
    
    useDeployedImplJar = (savedImplJar != null);
    ServiceImplInconsistency savedInc = getServiceInterfaceChangeInconsistency(DeploymentLocation.SAVED);
    if (savedInc != null) {
      ServiceImplInconsistencyState deployedState;
      if (savedInc.getType() == ServiceImplInconsistencyState.SAVED_MISSING_JAR) {
        deployedState = ServiceImplInconsistencyState.DEPLOYED_MISSING_JAR;
      } else {
        deployedState = ServiceImplInconsistencyState.get(DeploymentLocation.DEPLOYED, true);
      }
      deployedServiceChangedInconsistency = new ServiceImplInconsistency(deployedState, savedInc.getLastChange(), savedInc.getLastJarChange());
    } else {
      deployedServiceChangedInconsistency = null;
    }
  }
  
  
  private boolean checkForOperationInterfaceChanges(Set<DeploymentItemInterface> newSavedPuplishedInterfaces) {
    Set<OperationInterface> newOperations = filterInterfaces(OperationInterface.class, newSavedPuplishedInterfaces);
    for (OperationInterface newOperation : newOperations) {
      if (getPublishedInterfaces(DeploymentLocation.SAVED).containsEqualOperation(newOperation)) {
        continue;
      } else {
        return true;
      }
    }
    return false;
  }


  public String getLastModifiedBy() {
    return lastModifiedBy;
  }
  
  public long getLastStateChange() {
    return lastStateChange;
  }

  public String getLastStateChangeBy() {
    return lastStateChangeBy;
  }

  public long getLastOperationInterfaceChange() {
    return lastOperationInterfaceChange;
  }

  public Optional<SerializableExceptionInformation> getRollbackError() {
    return rollbackError;
  }
  
  public Optional<SerializableExceptionInformation> getRollbackCause() {
    return rollbackCause;
  }

  public Optional<SerializableExceptionInformation> getBuildError() {
    return buildError;
  }
  
  public ServiceImplInconsistency getDeployedServiceChangedInconsistency() {
    return deployedServiceChangedInconsistency;
  }
  
  public void restore(DeploymentItemStateStorable data) {
    if (data.getLastDeploymentTransition() != null) {
      this.lastDeploymentTransition = Optional.of(DeploymentTransition.byName(data.getLastDeploymentTransition()));
    }
    this.lastModifiedBy = data.getLastModifiedBy();
    this.lastStateChange = data.getLastStateChange();
    this.lastStateChangeBy = data.getLastStateChangeBy();
    this.lastOperationInterfaceChange = data.getLastOperationInterfaceChange();
    if (data.getRollbackCause() != null) {
      this.rollbackCause = Optional.of(data.getRollbackCause());
    }
    if (data.getRollbackCause() != null) {
      this.rollbackError = Optional.of(data.getRollbackError());
    }
    if (data.getBuildError() != null) {
      this.buildError = Optional.of(data.getBuildError());
    }
    this.deployedServiceChangedInconsistency = data.getDeployedImplInconsistency();
    this.locationContentChanges = data.getLocationContentChanges();
  }
  

  public Set<String> getWorkflowsCalledByThis(DeploymentLocation location) {
    Set<String> wfs = new HashSet<String>();
    Set<DeploymentItemInterface> s = interfaceEmployment.get(location);
    if (s != null) {
      for (DeploymentItemInterface dii : s) {
        if (dii instanceof TypeInterface) {
          if (((TypeInterface) dii).getType() == XMOMType.WORKFLOW) {
            wfs.add(((TypeInterface) dii).getName());
          }
        } else if (dii instanceof InterfaceEmployment) {
          InterfaceEmployment ie = (InterfaceEmployment) dii;
          if (ie.getProvider().getType() == XMOMType.WORKFLOW) {
            wfs.add(ie.getProvider().getName());
          }
        } else if (dii instanceof UsageOfOutputsOfWFReferencedByInputSource) {
          //inputsource
          wfs.add(((UsageOfOutputsOfWFReferencedByInputSource) dii).getWFName());
        }
      }
    }
    return wfs;
  }


  public Set<String> getUsedOrderTypes(DeploymentLocation location) {
    Set<String> ordertypes = new HashSet<String>();
    Set<DeploymentItemInterface> s = interfaceEmployment.get(location);
    
    if (s != null) {
      for (DeploymentItemInterface dii : s) {
        if (dii instanceof OrderTypeEmployment) {
          String ot = ((OrderTypeEmployment)dii).getOrderType();
          ordertypes.add(ot);
        }
        if (dii instanceof DetachedOrderTypeEmployment) {
          String ot = ((DetachedOrderTypeEmployment)dii).getOrderType();
          ordertypes.add(ot);
        }
      }
    }
    
    return ordertypes;
  }
  
  
  public String createCreationHint(String requester) {
    // get calling interfaces
    Set<DeploymentItemInterface> myEmployments = new HashSet<DeploymentItemInterface>();
    Set<DeploymentItemState> invocations = getInvocationSites(DeploymentLocation.SAVED);
    for (DeploymentItemState invokerState : invocations) {
      Set<DeploymentItemInterface> inconsistencies = invokerState.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, true);
      for (DeploymentItemInterface inconsistency : inconsistencies) {
        if (!(inconsistency instanceof UnresolvableInterface)) {
          if (InterfaceResolutionContext.getProvider(inconsistency, registry) == this) {
            myEmployments.add(inconsistency);
          }
        }
      }      
    }
    XMOMType type = getType();
    if (type == null) {
      type = XMOMType.DATATYPE;
    }
    switch (type) {
      case DATATYPE :
      case EXCEPTION :
        HierarchyTypeWithVariablesBuilder<?> builder;
        if (type == XMOMType.DATATYPE) {
          builder = new DatatypeBuilder(XmomType.ofFQTypeName(getName()));
        } else {
          builder = new ExceptionTypeBuilder(XmomType.ofFQTypeName(getName()));
        }
        Map<String, Variable> memVars = new HashMap<String, Variable>();
        Map<String, SnippetOperation> operations = new HashMap<String, SnippetOperation>();
        for (DeploymentItemInterface dii : myEmployments) {
          if (dii instanceof InterfaceEmployment) {
            InterfaceEmployment ie = (InterfaceEmployment)dii;
            if (ie.unwrap() instanceof MemberVariableInterface) {
              MemberVariableInterface memVar = (MemberVariableInterface)ie.unwrap();
              boolean isComplex = memVar.getType() != null && memVar.getType().getName() != null && !memVar.getType().getName().isEmpty();
              Variable currentVar = buildVariable(memVar);
              if (memVars.containsKey(memVar.getName())) {
                if (isComplex) {
                  // last complex value wins 
                  memVars.put(memVar.getName(), currentVar);
                } else {
                  Variable containedVar = memVars.get(memVar.getName());
                  if (containedVar.getTypeReference() == null &&
                      containedVar.getMeta() != null &&
                      containedVar.getMeta().getType() != null &&
                      containedVar.getMeta().getType() == PrimitiveType.STRING) {
                    // if current var is String-typed override it
                    memVars.put(memVar.getName(), currentVar);
                  }
                }
              } else {
                memVars.put(memVar.getName(), currentVar);
              }
            } else if (ie.unwrap() instanceof OperationInterface) {
              OperationInterface operation = (OperationInterface)ie.unwrap();
              SnippetOperationBuilder opBuilder = new SnippetOperationBuilder(operation.getName());
              OperationType opType = operation.getType() ;
              if (opType == null) {
                if (operation.getInput() != null &&
                    operation.getInput().size() > 0 &&
                    !operation.getInput().get(0).isJavaBaseType() &&
                    operation.getInput().get(0).getName().equals(ie.getProvider().getName())) {
                  opType = OperationType.INSTANCE_SERVICE;
                } else {
                  // if member variables exists it is most likely an instance service 
                  if (memVars.size() > 0) {
                    opType = OperationType.INSTANCE_SERVICE;
                  } else {
                    for (SnippetOperation previousOperations : operations.values()) {
                      // if other instance services you are most likely one as well 
                      if (!previousOperations.isStatic()) {
                        opType = OperationType.INSTANCE_SERVICE;
                        break;
                      }
                    }
                    if (opType == null) {
                      opType = OperationType.STATIC_SERVICE;
                    }
                  }
                }
              }
              switch (opType) {
                case INSTANCE_SERVICE :
                  opBuilder.isStatic(false);
                  break;
                case STATIC_SERVICE :
                  opBuilder.isStatic(true);
                  break;
                default :
                  throw new IllegalArgumentException("Workflow call in ServiceGroup?");
              }
              if (operation.getInput() != null && operation.getInput().size() > 0) {
                int inputStart = 0;
                if (opType == OperationType.INSTANCE_SERVICE &&
                    !operation.getInput().get(0).isJavaBaseType() &&
                    operation.getInput().get(0).getName().equals(ie.getProvider().getName())) {
                  inputStart = 1;
                }
                for (int i = inputStart; i < operation.getInput().size(); i++) {
                  opBuilder.input(buildVariable(operation.getInput().get(i)));
                }
              }
              if (operation.getOutput() != null && operation.getOutput().size() > 0) {
                for (TypeInterface outputType : operation.getOutput()) {
                  opBuilder.output(buildVariable(outputType));
                }
              }
              if (operation.getExceptions() != null && operation.getExceptions().size() > 0) {
                for (TypeInterface exception : operation.getExceptions()) {
                  opBuilder.exception(buildVariable(exception));
                }
              }
              if (operations.containsKey(operation.getName())) {
                SnippetOperation currentOperation = operations.get(operation.getName());
                if (currentOperation.getInputs() != null &&
                    operation.getInput() != null &&
                    currentOperation.getOutputs() != null &&
                    operation.getOutput() != null) {
                  if (currentOperation.getInputs().size() + currentOperation.getOutputs().size() <
                      operation.getInput().size() + operation.getOutput().size()) {
                    operations.put(operation.getName(), opBuilder.build());
                  }
                }
              } else {
                operations.put(operation.getName(), opBuilder.build());
              }
            }
          }
        }
        for (Variable vars : memVars.values()) {
          builder.variable(vars);          
        }
        for (SnippetOperation op : operations.values()) {
          if (type == XMOMType.DATATYPE) {
            ((DatatypeBuilder)builder).operation(op);
          }
        }
        return builder.build().toXML();
      case WORKFLOW :
        OperationInterface wfDefinition = null;
        TypeInterface wfType = null;
        for (DeploymentItemInterface dii : myEmployments) {
          if (dii instanceof InterfaceEmployment &&
              ((InterfaceEmployment)dii).unwrap() instanceof OperationInterface) {
            OperationInterface operation = (OperationInterface) ((InterfaceEmployment)dii).unwrap();
            if (wfDefinition == null) {
              wfDefinition = operation;
              wfType = ((InterfaceEmployment)dii).getProvider();
            } else {
              // TODO consider output?
              if (wfDefinition.getInput().size() == operation.getInput().size()) {
                InterfaceResolutionContext.updateCtx(DeploymentLocation.SAVED, this);
                try {
                  if (wfDefinition.matches(operation)) {
                    // ntbd
                  } else if (operation.matches(wfDefinition)) {
                    wfDefinition = operation;
                  } else {
                    List<TypeInterface> commonDenominator = new ArrayList<TypeInterface>();
                    for (int i=0; i < wfDefinition.getInput().size(); i++) {
                      TypeInterface currentInput = wfDefinition.getInput().get(i);
                      Optional<TypeInterface> typeInHierarchy = Optional.of(currentInput);
                      while (typeInHierarchy.isPresent()) {
                        if (typeInHierarchy.get().isAssignableFrom(operation.getInput().get(i))) {
                          commonDenominator.add(typeInHierarchy.get());
                          break;
                        }
                        typeInHierarchy = InterfaceResolutionContext.resCtx.get().getSupertype(typeInHierarchy.get());
                      }
                    }
                    if (commonDenominator.size() == wfDefinition.getInput().size()) {
                      wfDefinition = OperationInterface.of(wfDefinition.getName(), wfDefinition.getType(), ImplementationType.CONCRETE,
                                                           commonDenominator, wfDefinition.getOutput(), wfDefinition.getExceptions());
                    }
                  }
                } finally {
                  InterfaceResolutionContext.revertCtx();
                }
              } else if (wfDefinition.getInput().size() < operation.getInput().size()) {
                wfDefinition = operation;
              } else {
                // keep old wfDefinition
              }
            }
          }
        }
        if (wfDefinition != null) {
          SnippetOperationBuilder opBuilder = new SnippetOperationBuilder(wfDefinition.getName());
          opBuilder.isStatic(true);
          if (wfDefinition.getInput() != null && wfDefinition.getInput().size() > 0) {
            for (TypeInterface inputType : wfDefinition.getInput()) {
              opBuilder.input(buildVariable(inputType));
            }
          }
          if (wfDefinition.getOutput() != null && wfDefinition.getOutput().size() > 0) {
            for (TypeInterface outputType : wfDefinition.getOutput()) {
              opBuilder.output(buildVariable(outputType));
            }
          }
          if (wfDefinition.getExceptions() != null && wfDefinition.getExceptions().size() > 0) {
            for (TypeInterface exception : wfDefinition.getExceptions()) {
              opBuilder.exception(buildVariable(exception));
            }
          }
          Workflow wf = new Workflow(buildType(wfType), opBuilder.build());
          return wf.toXML();
        } else {
          return null;
        }
      default :
        throw new IllegalArgumentException("Invalid XMOMType for hint creation: " + type);
    }
  }
  
  
  private static XmomType buildType(TypeInterface from) {
    if (from.isJavaBaseType()) {
      throw new IllegalArgumentException("build XmomType from primitive!");
    } else {
      return XmomType.ofFQTypeName(from.getName());
    }
  }
  
  
  private static Variable buildVariable(TypeInterface from) {
    VariableBuilder varBuilder = new VariableBuilder("", false);
    if (from.isUntyped()) {
      varBuilder.simpleType(PrimitiveType.STRING);
    } else if (from.isJavaBaseType()) {
      PrimitiveType pt = PrimitiveType.createOrNull(from.getName());
      if (pt == null) {
        pt = PrimitiveType.STRING;
      }
      varBuilder.simpleType(pt);
    } else {
      varBuilder.complexType(buildType(from));
    }
    return varBuilder.build();
  }
  
  
  private static Variable buildVariable(MemberVariableInterface from) {
    VariableBuilder varBuilder = new Variable.VariableBuilder(from.getName(), false);
    if (from.isUntyped()) {
      varBuilder.simpleType(PrimitiveType.STRING);
    } else if (from.getType().isJavaBaseType()) {
      PrimitiveType pt = PrimitiveType.createOrNull(from.getType().getName());
      if (pt == null) {
        pt = PrimitiveType.STRING;
      }
      varBuilder.simpleType(pt);
    } else {
      varBuilder.complexType(XmomType.ofFQTypeName(from.getType().getName()));
    }
    return varBuilder.build();
  }


  public Set<DeploymentItemInterface> getInterfaceEmployments(DeploymentLocation location) {
    return interfaceEmployment.get(location);
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (int) getRevision();
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    DeploymentItemStateImpl other = (DeploymentItemStateImpl) obj;
    if (getRevision() != other.getRevision()) {
      return false;
    }
    return true;
  }
  
  
  public void invalidateCache() {
    cachedReport = null;
  }
  
  
  public class CachedStateReport {
    
    private final Set<DeploymentItemColumn> columns;
    private final Set<Long> dependencySnapshot;
    private final DeploymentItemStateReport report;
    
    CachedStateReport(Selection selection, CrossRevisionResolver resolver, DeploymentItemStateReport report) {
      columns = new HashSet<DeploymentItemColumn>();
      if (selection != null) {
        for (DeploymentItemColumn column: DeploymentItemColumn.values()) {
          if (selection.containsColumn(column)) {
            columns.add(column);
          }
        }
      }
      dependencySnapshot = new HashSet<Long>();
      if (resolver != null) {
        dependencySnapshot.addAll(resolver.identifyReachableRevisions());
      }
      this.report = report;
    }
    
    
    DeploymentItemStateReport getIfEqual(Selection selection, CrossRevisionResolver resolver) {
      if (selection != null) {
        for (DeploymentItemColumn column: DeploymentItemColumn.values()) {
          if (selection.containsColumn(column) ^ columns.contains(column)) {
            return null;
          }
        }
      } else if (columns.size() > 0) {
        return null;
      }
      if (resolver == null) {
        if (dependencySnapshot.size() > 0) {
          return null;
        }
      } else if (!dependencySnapshot.equals(resolver.identifyReachableRevisions())) {
        return null;
      }
      return report;
    }
    
    
  }
  
  
  public static DeploymentItemSelectImpl getDefaultListSelect() {
    DeploymentItemSelectImpl select = new DeploymentItemSelectImpl();
    select.select(DeploymentItemColumn.FQNAME);
    select.select(DeploymentItemColumn.LABEL);
    select.select(DeploymentItemColumn.TYPE);
    select.select(DeploymentItemColumn.APPLICATION);
    select.select(DeploymentItemColumn.VERSION);
    select.select(DeploymentItemColumn.WORKSPACE);
    select.select(DeploymentItemColumn.STATE);
    select.select(DeploymentItemColumn.RESOLUTION);
    select.select(DeploymentItemColumn.ROLLBACKCAUSE);
    select.select(DeploymentItemColumn.ROLLBACKEXCEPTION);
    select.select(DeploymentItemColumn.BUILDEXCEPTION);
    select.select(DeploymentItemColumn.ROLLBACKOCCURRED);
    select.select(DeploymentItemColumn.BUILDEXCEPTIONOCCURRED);
    return select;
  }
  
  public static DeploymentItemSelectImpl getDefaultDetailSelect() {
    DeploymentItemSelectImpl select = getDefaultListSelect();
    select.select(DeploymentItemColumn.DOCUMENTATION);
    select.select(DeploymentItemColumn.LASTSTATECHANGE);
    select.select(DeploymentItemColumn.LASTSTATECHANGEBY);
    select.select(DeploymentItemColumn.LASTMODIFIED);
    select.select(DeploymentItemColumn.LASTMODIFIEDBY);
    select.select(DeploymentItemColumn.MARKER);
    select.select(DeploymentItemColumn.TAG);
    select.select(DeploymentItemColumn.TASK);
    select.select(DeploymentItemColumn.TASKCOUNT);
    return select;
  }
  
  public static DeploymentItemSelectImpl getFullDetailSelect() {
    DeploymentItemSelectImpl select = getDefaultDetailSelect();
    select.select(DeploymentItemColumn.DEPENDENCY);
    select.select(DeploymentItemColumn.CREATION_HINT);
    return select;
  }


  public void invalidateCallSites() {
    callSitesDirty = true;
  }


  public void check() { //f�r debugzwecke
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    for (Entry<DeploymentLocation, Set<DeploymentItemInterface>> e : interfaceEmployment.entrySet()) {
      for (DeploymentItemInterface dii : e.getValue()) {
        if (dii instanceof TypeInterface) {
          TypeInterface ti = (TypeInterface) dii;
          Long rev = rcdm.getRevisionDefiningXMOMObject(ti.name, registry.getManagedRevision());
          if (rev == null) {
            continue;
          }
          DeploymentItemStateImpl t = ((DeploymentItemStateImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement().get(ti.name, rev));
          if (!t.getInvocationSites(DeploymentLocation.SAVED).contains(this)
              &&
              !t.getInvocationSites(DeploymentLocation.DEPLOYED).contains(this)) {
            System.out.println(name + " in rev " + getRevision() + " calls " + t.name + " in rev " + t.getRevision());
          }
        }
      }
    }
  }


  
  public String getLabel() {
    return label;
  }

  
}

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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.util.Set;
import java.util.Stack;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface.MatchableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.PublishedInterfaces;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.AccessChainedAssignment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.StaticChainResultType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.DetachedOrderTypeEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.InterfaceWithPotentiallyUnknownProvider;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.OrderTypeEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.UsageOfOutputsOfWFReferencedByInputSource;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeMissmatch;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;


public class InterfaceResolutionContext {
  
  public static final ThreadLocal<InterfaceResolutionContext> resCtx = new ThreadLocal<InterfaceResolutionContext>();
  
  private DeploymentItemRegistry registry;
  private CrossRevisionResolver crossResolver;
  private DeploymentLocation location;
  private Stack<DeploymentItemState> localState;
  
  
  public InterfaceResolutionContext(DeploymentItemRegistry registry, DeploymentLocation location) {
    this(registry, location, RevisionBasedCrossResolver.runtimeDependencyBackedResolver(registry.getManagedRevision()));
  }
  
  
  public InterfaceResolutionContext(DeploymentItemRegistry registry, DeploymentLocation location, CrossRevisionResolver crossResolver) {
    this.registry = registry;
    this.location = location;
    this.crossResolver = crossResolver;
    localState = new Stack<DeploymentItemState>();
  }

  
  public Optional<TypeInterface> getSupertype(TypeInterface typeInterface) {
    DeploymentItemState dis = resolveProvider(typeInterface, true); //FIXME true nur deshalb, weil von isAssignableOf aufgerufen
    if (dis == null) {
      return Optional.empty();
    } else {
      return dis.getPublishedInterfaces(location).getSupertype();
    }
  }
  
  
  public Optional<TypeInterface> getLocalSupertype() {
    return getLocalState().getPublishedInterfaces(location).getSupertype();
  }
  
  public TypeInterface getLocalType() {
    return TypeInterface.of(getLocalState().getName(), getLocalState().getType());
  }
  
  
  public DeploymentLocation getLocation() {
    return location;
  }


  public DeploymentItemState resolve(DeploymentItemIdentifier type) {
    Optional<DeploymentItemState> oDis = crossResolver.resolve(type, registry.getManagedRevision());
    if (oDis.isPresent()) {
      DeploymentItemState dis = oDis.get();
      if (dis.exists()) {
        return dis;
      } else {
        // create local phantom as well
        return null;
      }
    } else {
      // create local phantom
      return null;
    }
  }
  
  public DeploymentItemState getLocalState() {
    return localState.peek();
  }

  public void localState(DeploymentItemState dis) {
    localState.push(dis);
  }

  public boolean pop() {
    localState.pop();
    return localState.isEmpty();
  }
  
  
  public static void revertCtx() {
    InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
    if (resCtx != null) {
      if (resCtx.pop()) {
        InterfaceResolutionContext.resCtx.remove();
      }
    }
  }


  public static void updateCtx(DeploymentLocation location, DeploymentItemState dis) {
    InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
    if (resCtx == null) {
      if (location == null) {
        throw new IllegalArgumentException("No location given");
      }
      resCtx = new InterfaceResolutionContext(((DeploymentItemStateImpl)dis).registry, location);
      InterfaceResolutionContext.resCtx.set(resCtx);
    } else if (resCtx.location == null) {
      resCtx.location = location;
    }
    resCtx.localState(dis);
  }
  
  
  public static void updateCtx(DeploymentItemState dis) {
    updateCtx(null, dis);
  }
  
  
  public static void tryInitCtx(DeploymentItemState dis, CrossRevisionResolver crossResolver) {
    InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
    if (resCtx == null) {
      resCtx = new InterfaceResolutionContext(((DeploymentItemStateImpl)dis).registry, null, crossResolver);
      InterfaceResolutionContext.resCtx.set(resCtx);
    }
  }
  
  public void addOperationInvocationAtTargetSite(OperationInterface operationInterface) {
    if (crossResolver.updateCallSites()) {
      DeploymentItemState providerOfOperation = localState.peek();
      DeploymentItemState callerOfOperation = localState.get(0);
      providerOfOperation.addOperationInvocationSite(callerOfOperation, operationInterface, location);
    }
  }
  
  
  public static void remove() {
    InterfaceResolutionContext.resCtx.remove();
  }

  
  public DeploymentItemState getProvider(DeploymentItemInterface dii) {
    return registry.get(getProviderType(dii).getName());
  }
  
  
  public static DeploymentItemState getProvider(DeploymentItemInterface dii, DeploymentItemRegistry registry) {
    TypeInterface type = getProviderType(dii);
    return getProvider((DeploymentItemIdentifier)type, registry);
  }
  
  public static DeploymentItemState getProvider(DeploymentItemIdentifier dii, DeploymentItemRegistry registry) {
    return registry.get(dii.getName());
  }
  
  
  public static TypeInterface getProviderType(DeploymentItemInterface dii) {
    if (dii instanceof InterfaceEmployment) {
      return ((InterfaceEmployment)dii).getProvider();
    } else if (dii instanceof TypeInterface) {
      return (TypeInterface)dii;
    } else if (dii instanceof SupertypeInterface) {
      return TypeInterface.of((SupertypeInterface) dii);
    } else if (dii instanceof TypeMissmatch) {
      return ((TypeMissmatch)dii).getSourceType();
    } else if (dii instanceof InterfaceWithPotentiallyUnknownProvider) {
      TypeInterface ti = ((InterfaceWithPotentiallyUnknownProvider) dii).getProvider();
      if (ti == null) {
        ti = new TypeInterface("unknown");
      }
      return ti;
    } else {
      throw new IllegalArgumentException("can not retrieve provider for: " + dii);
    }
  }


  public DeploymentItemState resolveProvider(DeploymentItemInterface dii) {
    return resolveProvider(dii, false);
  }
  
  public DeploymentItemState resolveProvider(DeploymentItemInterface dii, boolean addRootInsteadOfSelfToInvocation) {
    TypeInterface providerType = getProviderType(dii);
    DeploymentItemState dis = resolve(providerType);
    if (dis == null) {
      dis = DeploymentItemStateImpl.createPhantom(dii, registry);
      if (!providerType.isJavaBaseType()) {
        dis = ((DeploymentItemStateRegistry) registry).addIfAbsent(dis);
      }
    }
    DeploymentItemState invoker;
    if (addRootInsteadOfSelfToInvocation) {
      invoker = localState.get(0);
    } else {
      invoker = localState.peek();
    }
    if (crossResolver.updateCallSites()) {
      dis.addInvocationSite(invoker, location);
    }
    return dis;
  }
  
  public MatchableInterface findInPublishedInterfacesInHierarchyOfType(MatchableInterface mi) {
    DeploymentItemState dis = getLocalState();
    while (true) {      
      PublishedInterfaces publInt = dis.getPublishedInterfaces(location);
      MatchableInterface m = publInt.getMatchingType(mi);
      if (m!=null) {
        return m;
      }
      Optional<TypeInterface> supertype = dis.getPublishedInterfaces(location).getSupertype();
      if (supertype.isPresent()) {
        dis = resolveProvider(supertype.get());
      } else {
        break;
      }
    }
    return null;
  }


  public PublishedInterfaces getPublishedInterfaces() {
    return getLocalState().getPublishedInterfaces(location);
  }
  
  public static TypeInterface toTypeInterface(DeploymentItemState dis) {
    return TypeInterface.of(dis.getName(), dis.getType());
  }

  /*
   * es wird nicht angenommen, dass relation USEDINIMPLOF übergeben wird
   * invoker benutzt usingInterface
   */
  public static boolean satisfiesRelation(DeploymentItemStateImpl invokerImpl, XMOMDatabaseEntryColumn relation,
                                          DeploymentItemInterface usingInterface) {
    if (usingInterface instanceof AccessChainedAssignment) {
      return false;
    } else if (usingInterface instanceof DetachedOrderTypeEmployment) {
      return false;
    } else if (usingInterface instanceof InterfaceEmployment) {
      return satisfiesRelation(invokerImpl, relation, ((InterfaceEmployment) usingInterface).unwrap());
    } else if (usingInterface instanceof NoAbstractMethodsInHierarchy) {
      return false;
    } else if (usingInterface instanceof OrderTypeEmployment) {
      return false;
    } else if (usingInterface instanceof StaticChainResultType) {
      return false;
    } else if (usingInterface instanceof SupertypeInterface) {
      if (invokerImpl.getType() == XMOMType.WORKFLOW) {
        return false;
      } else {
        return contains(relation, XMOMDatabaseEntryColumn.EXTENDEDBY);
      }
    } else if (usingInterface instanceof UnresolvableInterface) {
      return false;
    } else if (usingInterface instanceof UsageOfOutputsOfWFReferencedByInputSource) {
      return false;
    } else if (usingInterface instanceof MemberVariableInterface) {
      if (invokerImpl.getType() == XMOMType.WORKFLOW) {
        return false;
      } else {
        return contains(relation, XMOMDatabaseEntryColumn.POSSESSEDBY);
      }
    } else if (usingInterface instanceof OperationInterface) {
      if (invokerImpl.getType() == XMOMType.WORKFLOW) {
        return contains(relation, XMOMDatabaseEntryColumn.CALLEDBY);
      } else {
        return contains(relation, XMOMDatabaseEntryColumn.INSTANCESERVICEREFERENCEOF);
      }
    } else if (usingInterface instanceof TypeInterface) {
      Set<TypeOfUsage> use = ((TypeInterface) usingInterface).getTypesOfUsage();
      boolean isWF = invokerImpl.getType() == XMOMType.WORKFLOW;
      if (isWF) {
        if (((TypeInterface) usingInterface).getType() == XMOMType.EXCEPTION) {
          //wir wissen bei verwendung von exceptions im workflow normal nicht, dass es eine exception ist, ausser sie wird geworfen.
          //d.h. wenn die exception z.b. input von serviceaufruf ist, ist sie von typ unknown
          return contains(relation, XMOMDatabaseEntryColumn.THROWNBY);
        }
      } else if (use.size() == 0) {        
        return contains(relation, XMOMDatabaseEntryColumn.POSSESSEDBY);
      }
      for (TypeOfUsage type : use) {
        switch (type) {
          case INPUT :
            if (contains(relation, XMOMDatabaseEntryColumn.NEEDEDBY)) {
              //innerhalb von workflows werden die inputs von aufgerufenen services auch mit INPUT geflaggt. Diese sollen nicht als neededby aufgeführt werden
              //neededby=input des WF/Service/etc.
              if (!isWF || invokerImpl.getPublishedInterfaces(DeploymentLocation.SAVED).getAllOperations().iterator().next().getInput().contains(usingInterface)) {
                return true;
              }
            }
            break;
          case OUTPUT :
            if (contains(relation, XMOMDatabaseEntryColumn.PRODUCEDBY)) {
              if (!isWF || invokerImpl.getPublishedInterfaces(DeploymentLocation.SAVED).getAllOperations().iterator().next().getOutput().contains(usingInterface)) {
                return true;
              }
            }
            break;
          case SUPERTYPE :
            if (contains(relation, XMOMDatabaseEntryColumn.EXTENDEDBY)) {
              return true;
            }
          case EMPLOYMENT :
            if(contains(relation, XMOMDatabaseEntryColumn.POSSESSEDBY)) {
              return true;
            }
          default :
            break;
        }
      }
    }
    return false;
  }


  private static boolean contains(XMOMDatabaseEntryColumn relation, XMOMDatabaseEntryColumn ... relations) {
    for (XMOMDatabaseEntryColumn x : relations) {
      if (relation == x) {
        return true;
      }
    }
    return false;
  }


}


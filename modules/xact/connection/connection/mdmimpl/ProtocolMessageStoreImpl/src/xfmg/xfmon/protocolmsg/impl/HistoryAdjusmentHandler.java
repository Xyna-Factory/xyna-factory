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
package xfmg.xfmon.protocolmsg.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Version;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.SupertypeInterface;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextChangeHandler;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

import xfmg.xfmon.protocolmsg.ProtocolPayload;
import xfmg.xfmon.protocolmsg.data.ProtocolMessageStorable;

public class HistoryAdjusmentHandler implements RuntimeContextChangeHandler {

  
  private static volatile HistoryAdjusmentHandler registeredHandler;
  
  public HistoryAdjusmentHandler() {
  }
  
  
  public String getName() {
    return "HistoryAdjusmentHandler";
  }

  public Version getVersion() {
    return new Version(1, 0, 0, 1);
  }

  public void displacedByNewVersion() {
    registeredHandler = null;
  }


  public void dependencyChanges(RuntimeDependencyContext of, Collection<RuntimeDependencyContext> previous,
                                Collection<RuntimeDependencyContext> newDependencies) {
    // ntbd
  }

  
  public void migration(RuntimeDependencyContext from, RuntimeDependencyContext to) {
    if (!getRelevantContexts().contains(from)) {
      return;
    }
    
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long toRev;
    try {
      toRev = revMgmt.getRevision(to.asCorrespondingRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      ProtocolMessageStoreServiceOperationImpl.logger.warn("Failed to resolve revision on RuntimeContext migration", e);
      return;
    }
    ODSConnection con = openHistorizationConnection();
    try {
      FactoryWarehouseCursor<ProtocolMessageStorable> cursor = con.getCursor("select * from protocolmsg", new Parameter(), ProtocolMessageStorable.reader, ProtocolMessageStoreServiceOperationImpl.CURSOR_CACHE_SIZE, ProtocolMessageStoreServiceOperationImpl.cursorQueryCache);
      List<ProtocolMessageStorable> msgs = cursor.getRemainingCacheOrNextIfEmpty();
      while (msgs != null && msgs.size() > 0) {
        List<ProtocolMessageStorable> store = new ArrayList<>();
        for (ProtocolMessageStorable msg : msgs) {
          try {
            GeneralXynaObject conversion = DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(msg.getPayload(), toRev);
            if (conversion != msg.getPayload()) {
              msg.setPayload(conversion);
              store.add(msg);
            }
          } catch (XynaException e) {
            ProtocolMessageStoreServiceOperationImpl.logger.warn("Failed to reload message payload on migration", e);
          }
        }
        if (store.size() > 0) {
          con.persistCollection(store);
          con.commit();
        }
        msgs = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } catch (PersistenceLayerException e) {
      ProtocolMessageStoreServiceOperationImpl.logger.warn("Error while trying to check protocolmsgs for conversion",e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        ProtocolMessageStoreServiceOperationImpl.logger.debug("Failed to close connection",e);
      } 
    }
  }

  
  public void removal(RuntimeDependencyContext rc) {
    if (!getRelevantContexts().contains(rc)) {
      return;
    }
    
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long rev;
    try {
      rev = revMgmt.getRevision(rc.asCorrespondingRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      ProtocolMessageStoreServiceOperationImpl.logger.warn("Failed to resolve revision on RuntimeContext removal", e);
      return;
    }
    ODSConnection con = openHistorizationConnection();
    try {
      FactoryWarehouseCursor<ProtocolMessageStorable> cursor = con.getCursor("select * from protocolmsg", new Parameter(), ProtocolMessageStorable.reader, ProtocolMessageStoreServiceOperationImpl.CURSOR_CACHE_SIZE, ProtocolMessageStoreServiceOperationImpl.cursorQueryCache);
      List<ProtocolMessageStorable> msgs = cursor.getRemainingCacheOrNextIfEmpty();
      while (msgs != null && msgs.size() > 0) {
        List<ProtocolMessageStorable> delete = new ArrayList<>();
        for (ProtocolMessageStorable msg : msgs) {
          if (msg.getPayload() != null && RevisionManagement.getRevisionByClass(msg.getPayload().getClass()) == rev) {
            delete.add(msg);
          }
        }
        if (delete.size() > 0) {
          con.delete(delete);
          con.commit();
        }
        msgs = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } catch (XynaException e) {
      ProtocolMessageStoreServiceOperationImpl.logger.warn("Error while trying to check protocolmsgs for removal",e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        ProtocolMessageStoreServiceOperationImpl.logger.debug("Failed to close connection",e);
      } 
    }
  }


  public void creation(RuntimeDependencyContext rc) {
    // ntbd
  }
  
  
  private ODSConnection openHistorizationConnection() {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS().openConnection(ODSConnectionType.HISTORY);
  }


  public static void register() {
    HistoryAdjusmentHandler handler = new HistoryAdjusmentHandler();
    if (getRtCtxMgmt().registerHandler(handler)) {
      registeredHandler = handler;
    }
  }
  
  
  public static void unregister() {
    if (registeredHandler != null) {
      getRtCtxMgmt().unregisterHandler(registeredHandler);
      registeredHandler = null;
    }
    
  }
  
  
  private Set<RuntimeDependencyContext> getRelevantContexts() {
    return determineRelevantContexts();
  }
  
  private Set<RuntimeDependencyContext>  determineRelevantContexts() {
    Set<Long> relevantRevisions = new HashSet<Long>();
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    Long revision = ((ClassLoaderBase)HistoryAdjusmentHandler.class.getClassLoader()).getRevision();
    // own revision for ProtocolMessage
    relevantRevisions.add(revision);
    XynaObjectAnnotation xoa = ProtocolPayload.class.getAnnotation(XynaObjectAnnotation.class);
    SupertypeInterface protoMsgAsSupertype = SupertypeInterface.of(xoa.fqXmlName(), XMOMType.DATATYPE);
    DeploymentItemStateImpl disi = (DeploymentItemStateImpl) dism.get(xoa.fqXmlName(), revision);
    Set<DeploymentItemState> invocationSites = disi.getInvocationSites(DeploymentLocation.DEPLOYED);
    for (DeploymentItemState invocationSite : invocationSites) {
      Set<SupertypeInterface> supertypes = invocationSite.getPublishedInterfaces(SupertypeInterface.class, DeploymentLocation.DEPLOYED);
      for (SupertypeInterface supertype : supertypes) {
        if (supertype.matches(protoMsgAsSupertype)) {
          relevantRevisions.add(((DeploymentItemStateImpl)invocationSite).getRevision());
        }
      }
    }
    
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Set<RuntimeDependencyContext> relevantContexts = new HashSet<RuntimeDependencyContext>();
    for (Long relevantRevision : relevantRevisions) {
      RuntimeContext rc;
      try {
        rc = rm.getRuntimeContext(relevantRevision);
        if (rc instanceof RuntimeDependencyContext) {
          relevantContexts.add((RuntimeDependencyContext)rc);
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // ntbd
      }
    }
    return relevantContexts;
  }
  
  
  private static RuntimeContextManagement getRtCtxMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextManagement();
  }
  
}
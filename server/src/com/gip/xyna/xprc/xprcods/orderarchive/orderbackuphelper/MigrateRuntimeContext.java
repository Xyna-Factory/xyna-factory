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
package com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper;



import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStartApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CronLikeOrderCopyException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement.IRuntimeDependencyLock;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement.RuntimeContextDependencyChange;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity.MigrateRuntimeContextAccessContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextChangeHandler;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xmcp.xguisupport.messagebus.Publisher;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.exceptions.XFMG_CouldNotModifyRuntimeContextDependenciesException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTED_EXCEPTION;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_SuspendFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.MigrationSerialVersionIgnoringOrderInstanceBackup;
import com.gip.xyna.xprc.xfractwfe.OrderFilterAlgorithmsImpl;
import com.gip.xyna.xprc.xfractwfe.OrderFilterAlgorithmsImpl.OrderFilter;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendRevisionsBean;



/* TODO
 *     - !force check is broader then it could be, migration will be aborted if an order from a revision using from is present
 *       (it might not contain any serialized data from that revision yet (or ever)) 
 *     - rollback of migration is only partial (some actions don't use the same connection as their interface does not yet provided it) 
 *     - no multiUserEvent for runtimeContext change?
 */
public class MigrateRuntimeContext {


  private final static Logger logger = CentralFactoryLogging.getLogger(MigrateRuntimeContext.class);


  public static enum MigrationTargets {
    ManualInteractions, Orders, Crons, BatchJobs /* , OrdertypeConfig */;
  }

  
  public static MigrationContext migrateRuntimeContext(RuntimeDependencyContext from, RuntimeDependencyContext to, Collection<MigrationTargets> targets, boolean force)
                  throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    try {
      return migrateRuntimeContext(from, to, targets, force, new LocalRuntimeContextManagementSecurity.AllAccess());
    } catch (XFMG_ACCESS_VIOLATION e) {
      // should never happen
      throw new RuntimeException("AllAccess should have all access",e);
    }
  }

  /*
   * force = versuche die laufenden objekte zu migrieren, und entferne sie falls es fehler dabei gibt
   * !force = abbrechen, falls laufende objekte gefunden werden die voraussichtlich nicht migriert werden können 
   *          (TODO genauere analyse durchführen - es wird zu häufig abgebrochen)
   */
  public static MigrationContext migrateRuntimeContext(RuntimeDependencyContext from, RuntimeDependencyContext to, Collection<MigrationTargets> targets, boolean force, MigrateRuntimeContextAccessContext accessCtx)
                  throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XFMG_ACCESS_VIOLATION {
    lockCommandControl(from, to);
    MigrationContext context = new MigrationContext(from, to, accessCtx);
    context.init();
    try {
      if (!force) {
        try {
          checkForActiveOrders(context);
          if (context.activeOrdersFound()) {
            return context;
          }
        } catch (Throwable t) {
          context.abortMigration(t);
          return context;
        }
      }
      //TODO auftragseingangsschnittstellen ausschalten, die fromrev verwenden

      RevisionBasedOrderFilter rbof = new RevisionBasedOrderFilter(context.getFromRevision(), context.getToRevision());
      OrderFilterAlgorithmsImpl.getInstance().addFilter(rbof);
      try {
        DeploymentManagement.getInstance().propagateDeployment();

        try {
          DeploymentManagement.getInstance().waitForUnreachableOrders();
        } catch (XPRC_TimeoutWhileWaitingForUnaccessibleOrderException e) {
          context.abortMigration(MigrationAbortionReason.UNACCESSIBLE_ORDERS);
          return context;
        }

        context.suspendRevisions(force);

        context.changeDependencies();

        for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
          try {
            rdcch.migration(from, to);
          } catch (Throwable t) {
            logger.warn("Could not execute RuntimeContextChangeHandler " + rdcch, t);
          }
        }

        // MIEntries
        if (targets.contains(MigrationTargets.ManualInteractions)) {
          migrateManualInteractions(context);
        }

        // Crons
        if (targets.contains(MigrationTargets.Crons)) {
          migrateCrons(context);
        }

        // TCOs
        if (targets.contains(MigrationTargets.BatchJobs)) {
          migrateBatchProcesses(context);
        }

        // Laufende Aufträge (Suspension & heldAtCheckpoints)
        if (targets.contains(MigrationTargets.Orders)) {
          migrateRunningOrders(context, rbof);
        }
        // TODO OrderType-Config, die nicht Bestandteil der Application ist/war (zB. Monitoring oder sowas)

        context.commit();
      } finally {
        OrderFilterAlgorithmsImpl.getInstance().removeOrderFilter(rbof);
      }
    } finally {
      context.finish();
    }
    return context;
  }

  
  private static void lockCommandControl(RuntimeDependencyContext from, RuntimeDependencyContext to) {
 /*   XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(runtimeContext)
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRevisionsRecursivly(revision, parents);
    CommandControl.wlock(Operation.RUNTIMECTX_MIGRATE, Operation.all(), revision);*/
  }

  private static RuntimeContextManagement getRtCtxMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextManagement();
  }

  private static void checkForActiveOrders(MigrationContext context)
                  throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    checkOrderBackup(context);
    checkBatchProcesses(context);
    checkCronLikeOrders(context);
  }


  private static void checkOrderBackup(MigrationContext context)
                  throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    List<Pair<Long, Long>> orderBackupIds = selectAllRootOrderBackupIds(context.getConnection());
    for (Pair<Long, Long> p : orderBackupIds) {
      long rev = p.getSecond();
      if (isRevisionReachable(context.getFromRevision(), rev)) {
        context.addActiveOrderId(ActiveOrderType.ORDER, p.getFirst());
      }
    }
  }


  private static void checkBatchProcesses(MigrationContext context) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Set<Long> parents = new HashSet<Long>();
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
                    .getParentRevisionsRecursivly(context.getFromRevision(), parents);
    parents.add(context.getFromRevision());
    for (Long parentRev : parents) {
      List<BatchProcess> bps = bpm.getBatchProcesses(parentRev);
      if (bps.size() > 0) {
        for (BatchProcess batchProcess : bps) {
          context.addActiveOrderId(ActiveOrderType.BATCH, batchProcess.getBatchProcessId());
        }
      }
    }
  }


  private static void checkCronLikeOrders(MigrationContext context) {
    ObjectCompatibility oc = new ObjectCompatibility(new GenerationBaseCache(), context, new HashMap<String, Boolean>());
    try {
      Collection<CronLikeOrder> affectedCrons = determineAffectedCrons(context.getFromRevision());
      if (affectedCrons.size() > 0) {
        for (CronLikeOrder clo : affectedCrons) {
          /*
           * validieren, dass man CLO migrieren kann. D.h.:
           * - CLO Input kann serialisiert mit den neuen runtimecontext-deps noch gelesen werden, d.h.
           *   - alle typen sind bekannt
           *   - alle membervariablen, die bisher existieren, existieren immer noch und sind vom gleichen typ
           *   - achtung, hier sollte die serialisierte objektisntanz der CLO benutzt werden, und nicht die GenerationBase-Struktur der
           *     Root-Types, weil Membervariablen abgeleitete Typen haben könnten.
           * - Nicht validiert wird, dass der OrderType in der Ziel-Version noch existiert und auf die Inputdaten passt. => Das ist eine andere Art von Fehler.
           */
          GeneralXynaObject gxo = clo.getCreationParameters().getInputPayload();
          if (logger.isDebugEnabled()) {
            logger.debug("checking cron like order " + clo.getId() + " for migration possibility ...");            
          }
          if (oc.objectChangesAfterRevisionChange(gxo, oc.getRevisionsBetween(clo.getRevision(), context.fromRevision))) {
            context.addActiveOrderId(ActiveOrderType.CRON, clo.getId());
          }
        }
      }
    } catch (XPRC_CronLikeSchedulerException e) {
      context.abortMigration(e);
    }

  }

  private static class ObjectCompatibility {

    private final GenerationBaseCache cache;
    private final MigrationContext context;
    private final Map<String, Boolean> definitionCompatibility;
    private final Set<Long> revisionsReachableFromToRevision;


    public ObjectCompatibility(GenerationBaseCache cache, MigrationContext context, Map<String, Boolean> definitionCompatibility) {
      this.cache = cache;
      this.context = context;
      this.definitionCompatibility = definitionCompatibility;
      Set<Long> targetDeps = new HashSet<Long>(); 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(context.toRevision, targetDeps);
      this.revisionsReachableFromToRevision = targetDeps;
    }

    /**
     * alle revisions für die gilt:
     * - sie sind von sourceRev erreichbar (inklusive sourceRev)
     * - sie erreichen targetRev (exclusive targetRev)  
     */
    public Set<Long> getRevisionsBetween(Long sourceRev, Long targetRev) {
      if (sourceRev.equals(targetRev)) {
        return Collections.emptySet();
      }
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      Set<Long> result = new HashSet<>();
      Set<Long> check = new HashSet<>(); //enthält nur revs, die noch nicht in result sind, und die != targetrev sind
      check.add(sourceRev);
      while (check.size() > 0) {
        Set<Long> tmp = new HashSet<>(check);
        result.addAll(check);
        check.clear();
        for (Long c : tmp) {
          check.addAll(rcdm.getDependencies(c));
        }
        check.removeAll(result);
        check.remove(targetRev);
      }
      return result;
    }


    private boolean objectChangesAfterRevisionChange(GeneralXynaObject gxo, Set<Long> reachableRevisionsUntilChangedRevision) {
      /*
       * rekursion pro GXO: falls typ in quell und ziel-RTC-hierarchie auf den gleichen typ auflöst, muss nichts berücksichtigt werden.
       * - rekursion nur auf non-null-members
       * - für jede membervariable checken, dass sie noch vom gleichen typ ist
       * 
       * achtung, wenn man das wiederverwenden möchte für xynaobjekte, die in workflows/audits referenziert sind, muss man die oldVersions von
       * members auch durchsuchen!
       */
      long revisionLoadingObj = RevisionManagement.getRevisionByClass(gxo.getClass());
      if (revisionLoadingObj == Integer.MAX_VALUE) {
        return false; //passt.
      }
      if (revisionsReachableFromToRevision.contains(revisionLoadingObj) || reachableRevisionsUntilChangedRevision.contains(revisionLoadingObj)) {
        //objektdefinition ändert sich durch migration nicht. komplexwertige membervariablen trotzdem rekursiv checken!
      } else {
        Boolean compatible = definitionCompatibility.get(gxo.getClass().getName());
        if (compatible != null) {
          if (!compatible) {
            return true;
          }
          //ok, nicht nochmal die membervars checken, sondern nur die referenzierten objekte
        } else {
          //hat sich definition geändert?
          //d.h. vergleiche die beiden GenerationBaseCache -Definitionen
          String fqXmlName = getFQXmlName(gxo);

          GenerationBase gbOld = parseGenerationBase(gxo, fqXmlName, revisionLoadingObj);
          GenerationBase gbNew;
          try {
            gbNew = parseGenerationBase(gxo, fqXmlName, context.toRevision); //korrekte revision wird automatisch berechnet
          } catch (RuntimeException e) {
            //objekt nicht auflösbar oder sowas.
            if (logger.isDebugEnabled()) {
              logger.debug("Could not parse " + fqXmlName + " in " + context.toRevision + ".", e);
            }
            definitionCompatibility.put(gxo.getClass().getName(), false);
            return true;
          }          

          if (!gbNew.exists() || gbNew.compareImplementation(gbOld)) {
            definitionCompatibility.put(gxo.getClass().getName(), false);
            return true;
          }
          definitionCompatibility.put(gxo.getClass().getName(), true);
        }
      }
      for (GeneralXynaObject child : getComplexMembersOrListElements(gxo, revisionLoadingObj)) {
        if (objectChangesAfterRevisionChange(child, reachableRevisionsUntilChangedRevision)) {
          return true;
        }
      }
      return false;
    }


    private DomOrExceptionGenerationBase parseGenerationBase(GeneralXynaObject gxo, String fqXmlName, Long rev) {
      DomOrExceptionGenerationBase gb;
      try {
        if (gxo instanceof XynaObject) {
          gb = DOM.getOrCreateInstance(fqXmlName, cache, rev);
        } else {
          gb = ExceptionGeneration.getOrCreateInstance(fqXmlName, cache, rev);
        }
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException();
      }
      try {
        gb.parseGeneration(true, false, false);
      } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
        throw new RuntimeException();
      }
      return gb;
    }


    private String getFQXmlName(GeneralXynaObject gxo) {
      if (gxo instanceof XynaObject) {
        return gxo.getClass().getAnnotation(XynaObjectAnnotation.class).fqXmlName();
      } else {
        //Exception
        String xml = gxo.toXml();
        Document doc;
        try {
          doc = XMLUtils.parseString(xml);
        } catch (XPRC_XmlParsingException e) {
          throw new RuntimeException();
        }
        Element root = doc.getDocumentElement();
        return root.getAttribute(GenerationBase.ATT.REFERENCEPATH) + "." + root.getAttribute(GenerationBase.ATT.REFERENCENAME);
      }
    }


    private List<GeneralXynaObject> getComplexMembersOrListElements(GeneralXynaObject gxo, long rev) {
      List<GeneralXynaObject> children = new ArrayList<>();
      DomOrExceptionGenerationBase gb = parseGenerationBase(gxo, getFQXmlName(gxo), rev);
      for (AVariable v : gb.getAllMemberVarsIncludingInherited()) {
        try {
          Object val = gxo.get(v.getVarName());
          if (val != null) {
            if (v.isList()) {
              List list = (List) val;
              for (Object o : list) {
                if (o instanceof GeneralXynaObject) {
                  children.add((GeneralXynaObject) o);
                }
              }
            } else if (val instanceof GeneralXynaObject) {
              children.add((GeneralXynaObject) val);
            }
          }
        } catch (InvalidObjectPathException e) {
          throw new RuntimeException(e);
        }
      }
      return children;
    }
    
  }


  private static void migrateBatchProcesses(MigrationContext context) {
    try {
      BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
      List<BatchProcess> bps = bpm.getBatchProcesses(context.getFromRevision());
      for (BatchProcess batchProcess : bps) {
        try {
          bpm.migrateBatchProcess(batchProcess.getBatchProcessId(), context.getTo().asCorrespondingRuntimeContext());
        } catch (Throwable t) {
          bpm.cancelBatchProcess(batchProcess.getBatchProcessId(), CancelMode.KILL_SLAVES, -1L);
          context.addAbortedOrderId(ActiveOrderType.BATCH, batchProcess.getBatchProcessId());
        }
      }
      // now for onTheFlyInput...
      Set<Long> parents = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
                      .getParentRevisionsRecursivly(context.getToRevision(), parents);
      parents.add(context.getToRevision());
      for (Long parentRev : parents) {
        bps = bpm.getBatchProcesses(parentRev);
        for (BatchProcess batchProcess : bps) {
          try {
            batchProcess.refreshInputGenerator();
          } catch (Throwable t) {
            bpm.cancelBatchProcess(batchProcess.getBatchProcessId(), CancelMode.KILL_SLAVES, -1L);
            context.addAbortedOrderId(ActiveOrderType.BATCH, batchProcess.getBatchProcessId());
          }
        }
      }
    } catch (XynaException e) {
      context.abortMigration(e);
    }
  }


  private static void migrateManualInteractions(MigrationContext context) {
    try {
      ODSConnection con = context.getConnection();
      FactoryWarehouseCursor<ManualInteractionEntry> cursor = con
                      .getCursor("select * from " + ManualInteractionEntry.TABLE_NAME, new Parameter(),
                                 ManualInteractionEntry.reader, 20);

      List<? extends ManualInteractionEntry> next = cursor.getRemainingCacheOrNextIfEmpty();

      while (next != null && !next.isEmpty()) {
        for (ManualInteractionEntry miEntry : next) {
          if (miEntry.getRevision().equals(context.getFromRevision())) {
            miEntry.setRevision(context.getToRevision());
            con.persistObject(miEntry);
          }
        }
        next = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } catch (PersistenceLayerException e) {
      context.abortMigration(e);
    }
  }


  private static void migrateCrons(MigrationContext context) {
    ApplicationManagement applicationManagement = XynaFactory.getInstance().getFactoryManagement()
                    .getXynaFactoryControl().getApplicationManagement();

    try {
      applicationManagement.copyCronLikeOrders(context.getFrom().asCorrespondingRuntimeContext(), context.getTo().asCorrespondingRuntimeContext(), null, null, null, true, false, false);
    } catch (XFMG_CronLikeOrderCopyException e) {
      // TODO remove them instead?
      context.abortMigration(e);
    }
    migrateCronParameters(context);
  }


  private static void migrateCronParameters(MigrationContext context) {
    try {
      Collection<CronLikeOrder> affectedCrons = determineAffectedCrons(context.getToRevision());
      for (CronLikeOrder cron : affectedCrons) {
        adjustAffectedCrons(cron, context);
      }
    } catch (Throwable e) {
      context.abortMigration(e);
    }
  }


  private static void adjustAffectedCrons(CronLikeOrder cron, MigrationContext context) throws XynaException {
    GeneralXynaObject gxo = cron.getCreationParameters().getInputPayload();
    GeneralXynaObject newGxo = DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(gxo, cron.getRevision());
    try {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                      .modifyCronLikeOrder(cron.getId(), (String) null, (DestinationKey) null, newGxo, (Long) null,
                                           (String) null, (Long) null, (String) null, (Boolean) null, (Boolean) null,
                                           (OnErrorAction) null, (String) null, (String) null, (String) null,
                                           (String) null);
    } catch (Throwable t) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not adjust cron parameter of cron like order " + cron.getId(), t);
      }
      XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().removeCronLikeOrder(cron);
      context.addAbortedOrderId(ActiveOrderType.CRON, cron.getId());
    }
  }


  private static Collection<CronLikeOrder> determineAffectedCrons(Long revision)
                  throws XPRC_CronLikeSchedulerException {
    Collection<CronLikeOrder> affected = new ArrayList<CronLikeOrder>();
    try {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        FactoryWarehouseCursor<CronLikeOrder> cursor = XynaFactory.getInstance().getProcessing().getXynaScheduler()
                        .getCronLikeScheduler().getCursorForRelevantCronLikeOrders(con, 20);
        List<CronLikeOrder> batch = cursor.getRemainingCacheOrNextIfEmpty();
        while (batch != null && batch.size() > 0) {
          for (CronLikeOrder clo : batch) {
            //inputparameter könnte abgeleitete typen enthalten, die aus parent-revision stammen? => nein, die müssen von der revision erreichbar sein, die der cron hat.
            if (isRevisionReachable(revision, clo.getRevision())) {
              affected.add(clo);
            }
          }
          batch = cursor.getRemainingCacheOrNextIfEmpty();
        }
      } finally {
        con.closeConnection();
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    return affected;
  }


  private static void migrateRunningOrders(MigrationContext context, OrderFilter of) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    migrateOrdersInScheduler(context);
    migrateOrderBackup(context);
    migrateOrdersHoldFromOrderFilters(context, of);
  }


  private static void migrateOrdersInScheduler(MigrationContext context) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Map<Long, XynaOrderServerExtension> affectedOrders = new HashMap<Long, XynaOrderServerExtension>();
    AllOrdersList allOrdersList = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    List<XynaOrderInfo> orders = allOrdersList.getAllOrders();
    for (XynaOrderInfo xoi : orders) {
      boolean affected = isDestinationKeyAffected(context.getFromRevision(), xoi.getDestinationKey());
      if (affected) {
        XynaOrderServerExtension xo = allOrdersList.waitForDeployment(xoi.getOrderId());
        if (xo != null) {
          affectedOrders.put(xoi.getOrderId(), xo);
        } //else: scheduled oder wegen scheduler-OOM-schutz im backup
      }
    }
    for (Long orderId : affectedOrders.keySet()) {
      XynaOrderServerExtension xose = affectedOrders.get(orderId);
      try {
        reloadOrder(xose, context.getFromRevision(), context.getToRevision());
        allOrdersList.deploymentFinished(orderId);
      } catch (Throwable t) {
        allOrdersList.removeOrder(orderId);
        context.addAbortedOrderId(ActiveOrderType.ORDER, orderId);
      }
    }

    XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
  }


  private static void migrateOrdersHoldFromOrderFilters(MigrationContext context, OrderFilter of) {
    OrderFilterAlgorithmsImpl ofai = OrderFilterAlgorithmsImpl.getInstance();
    Collection<? extends XynaOrderServerExtension> xos = ofai.getOrdersHeldAtProcessors(of);
    for (XynaOrderServerExtension xo : xos) {
      try {
        reloadOrder(xo, context.getFromRevision(), context.getToRevision());
      } catch (Throwable t) {
        logger.warn("Order " + xo.getId() + " could not be migrated to new revision.", t);
        // TODO remove from preprocessor
      }
    }
  }


  private static void migrateOrderBackup(MigrationContext context) throws PersistenceLayerException {
    List<Pair<Long, Long>> orderBackupIds = selectAllRootOrderBackupIds(context.getConnection());

    for (int i = 0; i < orderBackupIds.size(); ++i) {
      Pair<Long, Long> p = orderBackupIds.get(i);
      if (isRevisionReachable(context.getToRevision(), p.getSecond())) { 
        // TODO man erwischt damit leider auch aufträge, die vor der migration bereits die torevision verwendet hatten und deshalb nicht migriert werden müssten...
        reloadOrderHierarchy(context, p.getFirst());
      }
    }
  }


  private static void reloadOrderHierarchy(MigrationContext context, Long rootOrderId) throws PersistenceLayerException {
    try {
      OrderInstanceBackup rootOrder = reloadSingleOrderHierarchyEntry(context.getConnection(), rootOrderId,
                                                                      context.getFromRevision(),
                                                                      context.getToRevision(), null);
      if (rootOrder != null) {
        Long rootRevision = rootOrder.getRevision();
        List<Long> childOrderIds = selectAllChildOrderBackupIds(context.getConnection(), rootOrderId);
        for (Long childOrderId : childOrderIds) {
          reloadSingleOrderHierarchyEntry(context.getConnection(), childOrderId, context.getFromRevision(),
                                          context.getToRevision(), rootRevision);
        }
      }
    } catch (Throwable e) {
      purgeOrderFamily(rootOrderId, context);
    }
  }


  private static OrderInstanceBackup reloadSingleOrderHierarchyEntry(ODSConnection con, final Long orderId,
                                                                     Long fromRevision, Long toRevision,
                                                                     Long rootRevision)
                  throws XynaException {
    OrderInstanceBackup oib = loadBackupSavely(con, orderId, fromRevision, toRevision, rootRevision);
    if (oib.getXynaorder() != null) {
      reloadOrder(oib.getXynaorder(), fromRevision, toRevision);
    }
    if (oib.getDetails() != null) {
      oib.getDetails().getAuditDataAsJavaObject()
                      .reloadGeneratedObjectsInsideAuditIfNecessary(new DeploymentManagement.DeploymentAuditReloader(rootRevision == null ? oib
                                                                                                               .getRevision() : rootRevision));
    }
    if (oib.getRootId() == oib.getId() && oib.getXynaorder() == null) {
      logger.debug("Not rewriting orderbackup due to missing XynaOrder");
    } else {
      renewOrderBackup(oib, con);
    }
    return oib;
  }


  private static OrderInstanceBackup loadBackupSavely(ODSConnection con, final Long orderId, Long fromRevision,
                                                      Long toRevision, Long rootRevision)
                  throws PersistenceLayerException {
    /*
     * es gibt in der hierarchie
     * 
     * <parentrevisions>        <parentrevisions> + vorherige verwender von to-revision
     *       |                         |
     *       v                         v
     * <fromRevision>       ==>  <toRevision>
     *       |                         |
     *       v                         v
     * <usedRevisions>           <newUsedRevisions> (kann teile von usedRevisions enthalten)
     * 
     * folgende möglichkeiten, wo sich aufträge im orderbackup einordnen:
     * 
     * 1) root-auftrag in keiner der angegeben revisions
     * 2) root-auftrag in usedRevisions
     * 3) root-auftrag in fromRevision, current orderbackup in fromRevision
     * 4) root-auftrag in fromRevision, current orderbackup in usedRevision
     * 5) root-auftrag in parentrevisions, current orderbackup in parentrevisions
     * 6) root-auftrag in parentrevisions, current orderbackup in fromrevision
     * 7) root-auftrag in parentrevisions, current orderbackup in usedrevisions (=> ordertype muss nicht in der gleichen revision von torevision aus auflösbar sein)
     * 
     * 1) => muss nicht migriert werden
     * 2) => muss nicht migriert werden
     * 3) => muss nicht migriert werden? kann man migrieren - ansichtssache
     * 4) => muss nicht migriert werden? kann man migrieren - ansichtssache
     * 5) => migration notwendig, falls objekte aus fromRevision oder usedRevisions verwendet werden
     * 6) => da die ganze auftragshierarchie migriert wird, muss auch dieses orderbackup migriert werden
     *  => orderbackup.revision: fromrevision in torevision umändern sollte immer ok sein. - ggfs auch ordertype auflösen
     * 7) => da die ganze auftragshierarchie migriert wird, muss auch dieses orderbackup migriert werden
     *  => orderbackup.revision: neue revision müsste eigtl durch neues auflösen des ordertypes bestimmt werden. 
     *
     */
    ResultSetReader<MigrationSerialVersionIgnoringOrderInstanceBackup> reader = MigrationSerialVersionIgnoringOrderInstanceBackup
                    .getSerialVersionIgnoringReader(fromRevision, toRevision, rootRevision);
    PreparedQuery<MigrationSerialVersionIgnoringOrderInstanceBackup> oibQuery = queryCache
                    .getQueryFromCache(selectOrderInstanceBackup, con, reader);
    List<MigrationSerialVersionIgnoringOrderInstanceBackup> oibs = con.query(oibQuery, new Parameter(orderId), 1,
                                                                             reader);
    if (oibs.size() <= 0) {
      return null;
    }
    return oibs.get(0);
  }


  private static OrderInstanceBackup loadBackup(ODSConnection con, final Long orderId)
                  throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    OrderInstanceBackup oib = new OrderInstanceBackup(orderId, new OrderInstanceBackup()
                    .getLocalBinding(ODSConnectionType.DEFAULT));
    con.queryOneRow(oib);
    return oib;
  }


  private static void purgeOrderFamily(final long orderId, MigrationContext context) throws PersistenceLayerException {
    List<Long> family = selectAllChildOrderBackupIds(context.getConnection(), orderId);
    family.add(orderId);
    for (Long id : family) {
      OrderInstanceBackup toDelete = new OrderInstanceBackup(id, new OrderInstanceBackup()
                      .getLocalBinding(ODSConnectionType.DEFAULT));
      context.getConnection().deleteOneRow(toDelete);
      OrderInstance oi = new OrderInstance(id);
      // delete from orderArchive default as well
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      oa.getAuditAccess().delete(context.getConnection(), oi);
      if (id == orderId) {
        
        XynaOrderServerExtension xose = new XynaOrderServerExtension() {

          @Override
          public long getId() {
            return id;
          }
        };
        xose.setOrderContext(new OrderContextServerExtension(xose));
        oa.restoreTransientOrderParts(xose, id);
        if (xose.getResponseListener() != null) {
          xose.getResponseListener().onError(new XynaException[] {new XPRC_PROCESS_ABORTED_EXCEPTION(id, "Aborted by MigrateRuntimeContext.")},
                                             xose.getOrderContext());
        }
      }
      context.addAbortedOrderId(ActiveOrderType.ORDER, id);
    }
  }


  public static void renewOrderBackup(OrderInstanceBackup oib, ODSConnection con) throws PersistenceLayerException {
    if (oib.getDetails() == null && oib.getXynaorder() != null) {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(oib.getXynaorder(), oib.getBackupCauseAsEnum(), con);
    } else {
      con.persistObject(oib);
      if (oib.getDetails() != null) {
        con.persistObject(oib.getDetails());
      }
    }
  }


  public static void reloadOrder(XynaOrderServerExtension xo, Long fromRevision, Long toRevision) throws XynaException {

    if (logger.isDebugEnabled()) {
      logger.debug("Received order " + xo.getId() + " for reload");
    }

    if (xo.getRevision().equals(fromRevision)) {
      xo.setRevision(toRevision);
    }

    migrateDestinationKey(xo, fromRevision, toRevision);

    if (xo.getExecutionProcessInstance() != null) {
      xo.setExecutionProcessInstance(DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(xo.getExecutionProcessInstance(),
                                                                                 xo.getRootOrder().getRevision()));
    }

    if (xo.getInputPayload() != null) {
      xo.setInputPayload(DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(xo.getInputPayload(),
                                                                     xo.getRootOrder().getRevision()));
    }

    if (xo.getOutputPayload() != null) {
      xo.setOutputPayload(DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(xo.getOutputPayload(),
                                                                      xo.getRootOrder().getRevision()));
    }

    if (xo.hasError()) {
      Collection<XynaException> reloadedErrors = new ArrayList<XynaException>();
      for (XynaException xe : xo.getErrors()) {
        // build a new collection
        reloadedErrors.add(DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(xe,
                                                                       xo.getRootOrder().getRevision()));
      }
      // clear the old erros
      xo.clearErrors();
      // add the new ones
      for (XynaException xe : reloadedErrors) {
        xo.addException(xe, ProcessingStage.OTHER);
      }
    }

  }


  private static void migrateDestinationKey(XynaOrderServerExtension xo, Long fromRevision, Long toRevision)
                  throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getRevisionManagement();
    RuntimeContext fromRc = revMgmt.getRuntimeContext(fromRevision);
    if (xo.getDestinationKey().getRuntimeContext().equals(fromRc)) { //FIXME eigtl müsste man hier den ordertype erneut nachschlagen. der muss ja nicht notwendigerweise in der torevision leben
      xo.getDestinationKey().setRuntimeContext(revMgmt.getRuntimeContext(toRevision));
    }
  }

  private static PreparedQueryCache queryCache = new PreparedQueryCache(3600 * 1000, 3600 * 1000);

  private static String selectAllLocalBackupIds = "select " + OrderInstanceBackup.COL_ID + ","
                  + OrderInstanceBackup.COL_ROOT_ID + "," + OrderInstanceBackup.COL_REVISION + " from " + OrderInstanceBackup.TABLE_NAME + " where "
                  + OrderInstanceBackup.COL_BINDING + "=?";
  private static String selectOrderInstanceBackup = "select * from " + OrderInstanceBackup.TABLE_NAME + " where "
                  + OrderInstanceBackup.COL_ID + "=?";
  private static String selectLocalChildOrderBackupIds = "select " + OrderInstanceBackup.COL_ID + " from "
                  + OrderInstanceBackup.TABLE_NAME + " where " + OrderInstanceBackup.COL_BINDING + "=? AND "
                  + OrderInstanceBackup.COL_ROOT_ID + "=? AND NOT " + OrderInstanceBackup.COL_ID + "=?";


  /**
   * @return liste von <rootorderid, revision>
   */
  private static List<Pair<Long, Long>> selectAllRootOrderBackupIds(ODSConnection con) throws PersistenceLayerException {
    int binding = new OrderInstanceBackup().getLocalBinding(con.getConnectionType());
    PreparedQuery<Triple<Long, Long, Long>> query = queryCache.getQueryFromCache(selectAllLocalBackupIds, con,
                                                                         new ResultSetReader<Triple<Long, Long, Long>>() {

                                                                           public Triple<Long, Long, Long> read(ResultSet rs)
                                                                                           throws SQLException {
                                                                                     return Triple.<Long, Long, Long> of(
                                                                                          rs.getLong(OrderInstanceBackup.COL_ID), 
                                                                                          rs.getLong(OrderInstanceBackup.COL_ROOT_ID),
                                                                                          rs.getLong(OrderInstanceBackup.COL_REVISION));
                                                                                   }
                                                                                 });
    Collection<Triple<Long, Long, Long>> result = con.query(query, new Parameter(binding), -1);
    List<Pair<Long, Long>> rootOrderIds = new ArrayList<Pair<Long, Long>>();
    for (Triple<Long, Long, Long> triple : result) {
      if (triple.getFirst().equals(triple.getSecond())) {
        rootOrderIds.add(Pair.of(triple.getFirst(), triple.getThird()));
      }
    }
    return rootOrderIds;
  }


  private static List<Long> selectAllChildOrderBackupIds(ODSConnection con, Long rootOrderId)
                  throws PersistenceLayerException {
    int binding = new OrderInstanceBackup().getLocalBinding(con.getConnectionType());
    PreparedQuery<Long> query = queryCache.getQueryFromCache(selectLocalChildOrderBackupIds, con,
                                                             new ResultSetReader<Long>() {

                                                               public Long read(ResultSet rs) throws SQLException {
                                                                 return rs.getLong(OrderInstanceBackup.COL_ID);
                                                               }
                                                             });
    return con.query(query, new Parameter(binding, rootOrderId, rootOrderId), -1);
  }

  /*
   * fälle:
   * 1) order stammt aus fromrevision
   *   => zu dem zeitpunkt, wo order hier ankommt, ist order noch nicht zu torevision migriert, soll einfach weiterlaufen (racecondition)
   * 2) order stammt aus parent von fromrevision (|| stammt aus einem parent von torevision - ist eine racecondition zwischen zeitpunkt, wo der orderfilter erzeugt wird
   *   und wo die migration die rtc-deps umgezogen hat)
   *   => es muss am ende der auftrag migriert werden.
   */
  private static class RevisionBasedOrderFilter implements OrderFilter {

    private final long fromRevision;
    private final long toRevision;


    RevisionBasedOrderFilter(long fromRevision, long toRevision) {
      this.fromRevision = fromRevision;
      this.toRevision = toRevision;
    }


    public boolean filterForAddOrderToScheduler(XynaOrderServerExtension xo) {
      return containsRevisionReference(xo) && !OrdertypeManagement.internalOrdertypes.contains(xo.getDestinationKey().getOrderType());
    }


    public boolean filterForCheckOrderReadyForProcessing(XynaOrderServerExtension xo, DispatcherType type) {
      return containsRevisionReference(xo) && !OrdertypeManagement.internalOrdertypes.contains(xo.getDestinationKey().getOrderType());
    }


    public boolean startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter clocp,
                                        ResponseListener rl) {
      return containsRevisionReference(cronLikeOrder) || containsRevisionReference(clocp);
    }


    public void continueOrderReadyForProcessing(XynaOrderServerExtension xo) {
      
    }


    private boolean containsRevisionReference(XynaOrderServerExtension xo) {
      if (xo.getRevision().equals(fromRevision)) {
        return false; //ok, racecondition, soll fertig laufen
      }
      return MigrateRuntimeContext.isRevisionReachable(fromRevision, xo.getRevision()) || MigrateRuntimeContext.isRevisionReachable(toRevision, xo.getRevision());
    }


    private boolean containsRevisionReference(CronLikeOrder clo) {
      if (clo.getRevision().equals(fromRevision)) {
        return false; //ok, racecondition, soll fertig laufen
      }
      return MigrateRuntimeContext.isRevisionReachable(fromRevision, clo.getRevision()) || MigrateRuntimeContext.isRevisionReachable(toRevision, clo.getRevision());
    }


    private boolean containsRevisionReference(CronLikeOrderCreationParameter clocp) {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRevisionManagement();
      try {
        Long revision = revMgmt.getRevision(clocp.getDestinationKey().getRuntimeContext());
        return MigrateRuntimeContext.isRevisionReachable(fromRevision, revision) || MigrateRuntimeContext.isRevisionReachable(toRevision, revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }
  }


  private static boolean isDestinationKeyAffected(Long fromRevision, DestinationKey destinationKey) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getRevisionManagement();
      Long revision = revMgmt.getRevision(destinationKey.getRuntimeContext());
      return MigrateRuntimeContext.isRevisionReachable(fromRevision, revision);
  }

  /*
   * ist target von source aus erreichbar?
   */
  static boolean isRevisionReachable(Long target, Long source) {
    if (target.equals(source)) {
      return true;
    }
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .isDependency(source, target);
  }


  public static class MigrationContext {

    private final RuntimeDependencyContext from;
    private final RuntimeDependencyContext to;
    private final MigrateRuntimeContextAccessContext accessCtx;
    private final Long fromRevision;
    private final Long toRevision;
    private final Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> dependencyBackup;
    private final Map<ActiveOrderType, Set<Long>> active;
    private final Map<ActiveOrderType, Set<Long>> removed;
    
    private ODSConnection con;
    private SuspendRevisionsBean suspendRevisionsBean;
    private XPRC_ResumeFailedException resumeFailure;
    private MigrationAbortionReason migrationAbortionReason;
    private Throwable migrationAbortionCause;
    


    MigrationContext(RuntimeDependencyContext from, RuntimeDependencyContext to, MigrateRuntimeContextAccessContext accessCtx) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      this.from = from;
      this.to = to;
      this.accessCtx = accessCtx;
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRevisionManagement();
      fromRevision = revMgmt.getRevision(from.asCorrespondingRuntimeContext());
      toRevision = revMgmt.getRevision(to.asCorrespondingRuntimeContext());
      dependencyBackup = new HashMap<RuntimeDependencyContext, Collection<RuntimeDependencyContext>>();
      active = new HashMap<ActiveOrderType, Set<Long>>();
      removed = new HashMap<ActiveOrderType, Set<Long>>();
    }



    public void abortMigration(MigrationAbortionReason reason) {
      // TODO nice exceptions
      migrationAbortionReason = reason;
      switch (reason) {
        case UNACCESSIBLE_ORDERS:
          throw new RuntimeException("Migration aborted, timeout while waiting for unaccessible orders.");
        case EXCEPTION:
          throw new RuntimeException("Migration aborted, an error occured", migrationAbortionCause);
        default :
          throw new RuntimeException("Migration aborted");
      }
    }


    public void abortMigration(Throwable e) {
      migrationAbortionCause = e;
      abortMigration(MigrationAbortionReason.EXCEPTION);
    }


    public boolean activeOrdersFound() {
      return (active.size()) > 0;
    }
    
    
    public Pair<SuspendRevisionsBean, XPRC_ResumeFailedException> getResumeInformation() {
      return Pair.of(suspendRevisionsBean, resumeFailure);
    }


    public void addActiveOrderId(ActiveOrderType type, Long id) {
      Set<Long> activeSet = active.get(type);
      if (activeSet == null) {
        activeSet = new HashSet<Long>();
        active.put(type, activeSet);
      }
      activeSet.add(id);
    }
    
    public Collection<Long> getActiveOrderIds(ActiveOrderType type) {
      Set<Long> activeSet = active.get(type);
      if (activeSet == null) {
        return Collections.emptySet();
      } else {
        return activeSet;
      }
    }
    
    public void addAbortedOrderId(ActiveOrderType type, Long id) {
      Set<Long> removedSet = removed.get(type);
      if (removedSet == null) {
        removedSet = new HashSet<Long>();
        removed.put(type, removedSet);
      }
      removedSet.add(id);
    }
    
    public Collection<Long> getAbortedOrderIds(ActiveOrderType type) {
      Set<Long> removedSet = removed.get(type);
      if (removedSet == null) {
        return Collections.emptySet();
      } else {
        return removedSet;
      }
    }


    public void changeDependencies()
                    throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException, XFMG_ACCESS_VIOLATION {
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRuntimeContextDependencyManagement();
        List<RuntimeDependencyContext> parents = new ArrayList<RuntimeDependencyContext>(getParentsOfFrom());
        Collections.sort(parents, new Comparator<RuntimeDependencyContext>() {

          public int compare(RuntimeDependencyContext o1, RuntimeDependencyContext o2) {
            return getPrioOfType(o1) - getPrioOfType(o2);
          }


          private int getPrioOfType(RuntimeDependencyContext r) {
            switch (r.getRuntimeDependencyContextType()) {
              case Application :
                return 0;
              case ApplicationDefinition :
                return 1;
              case Workspace :
                return 2;
              default :
                return 3;
            }
          }

        });
        
        Collection<RuntimeContextDependencyChange> changes = new ArrayList<>();
        for (RuntimeDependencyContext parentContext : parents) {
          accessCtx.checkAccess(parentContext);
          Collection<RuntimeDependencyContext> originalDepenedencies = rcdm.getDependencies(parentContext);
          List<RuntimeDependencyContext> dependencies = new ArrayList<RuntimeDependencyContext>(originalDepenedencies);

          dependencies.remove(from);
          if (!dependencies.contains(to)) {
            dependencies.add(to);
          }

          changes.add(new RuntimeContextDependencyChange(parentContext, dependencies));

          dependencyBackup.put(parentContext, originalDepenedencies);
        }
        
        rcdm.modifyDependencies(changes, null, true, false, new NoOpRuntimeDependencyLock());
        
        //Multi-User-Event für Dependency Änderungen am owner
        String user = "XynaFactory.migrateRuntimeContextDependencies";
        Publisher publisher = new Publisher(user);
        Set<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate = new HashSet<>();
        for (RuntimeDependencyContext parentContext : parents) {
          publisher.publishRuntimeContextUpdate(parentContext);
          runtimeContextsToPublishAsXMOMUpdate.addAll(rcdm.getParentRuntimeContextsSorted(parentContext));
        }
        List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdateList = new ArrayList<>(runtimeContextsToPublishAsXMOMUpdate);
        rcdm.sortRuntimeContextsForGUI(runtimeContextsToPublishAsXMOMUpdateList);
        for (RuntimeContext rc : runtimeContextsToPublishAsXMOMUpdateList) {
          publisher.publishXMOMUpdate(rc.getGUIRepresentation());
        }
    }


    public void init() {
      con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      SerializableClassloadedObject.THROW_ERRORS.set(Boolean.TRUE);
    }

    
    public void commit() throws PersistenceLayerException {
      getConnection().commit();
    }

    public void finish() {
      try {
        con.rollback();
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("MigrationContext.finalze failed to close connection", e);
      } finally {
        try {
          if (suspendRevisionsBean != null) {
            try {
              XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().resumeMultipleOrders(suspendRevisionsBean.getResumeTargets(), false);
            } catch (XPRC_ResumeFailedException e) {
              resumeFailure = e;
            }
          }
        } finally {
          SerializableClassloadedObject.THROW_ERRORS.remove();
        }
      }
    }


    public RuntimeDependencyContext getFrom() {
      return from;
    }


    public RuntimeDependencyContext getTo() {
      return to;
    }


    public Long getFromRevision() {
      return fromRevision;
    }


    public Long getToRevision() {
      return toRevision;
    }


    public Set<Long> getParentRevisionsOfFrom() {
      Set<Long> revisions = new HashSet<Long>();
      getRCDM().getParentRevisionsRecursivly(fromRevision, revisions);
      return revisions;
    }


    public Set<RuntimeDependencyContext> getParentsOfFrom() {
      return getRCDM().getParentRuntimeContexts(from);
    }


    private RuntimeContextDependencyManagement getRCDM() {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRuntimeContextDependencyManagement();
    }


    public ODSConnection getConnection() {
      return con;
    }
    

    public void suspendRevisions(boolean force) {
      SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution() .getSuspendResumeManagement();
      try {
        // TODO keepUnresumable and release lock finally?
        suspendRevisionsBean = srm.suspendRevisions(getParentRevisionsOfFrom(), force, false); 
      } catch (XPRC_SuspendFailedException e) {
        abortMigration(e);
      }

      if (!suspendRevisionsBean.wasSuccessfull()) {
        throw new RuntimeException("Required Revisions could not be suspended");
      }
      
    }
  }
  
  
  public static class MigrationResult implements Serializable {
    
    private static final long serialVersionUID = 7406935208190984892L;
    
    private final Map<ActiveOrderType, Set<Long>> active;
    private final Map<ActiveOrderType, Set<Long>> removed;
    private final SuspendRevisionsBean suspendRevisionsBean;
    private final XPRC_ResumeFailedException resumeFailure;
    private final MigrationAbortionReason migrationAbortionReason;
    private final Throwable migrationAbortionCause;
    
    private MigrationResult(MigrationContext context) {
      this.active = context.active;
      this.removed = context.removed;
      this.suspendRevisionsBean = context.suspendRevisionsBean;
      this.resumeFailure = context.resumeFailure;
      this.migrationAbortionReason = context.migrationAbortionReason;
      this.migrationAbortionCause = context.migrationAbortionCause;
    }
    
    public static MigrationResult of(MigrationContext context) {
      return new MigrationResult(context);
    }
    
    
    public boolean activeOrdersFound() {
      return (active.size()) > 0;
    }
    
    
    public Pair<SuspendRevisionsBean, XPRC_ResumeFailedException> getResumeInformation() {
      return Pair.of(suspendRevisionsBean, resumeFailure);
    }

    
    public Collection<Long> getActiveOrderIds(ActiveOrderType type) {
      Set<Long> activeSet = active.get(type);
      if (activeSet == null) {
        return Collections.emptySet();
      } else {
        return activeSet;
      }
    }
    
    public Collection<Long> getAbortedOrderIds(ActiveOrderType type) {
      Set<Long> removedSet = removed.get(type);
      if (removedSet == null) {
        return Collections.emptySet();
      } else {
        return removedSet;
      }
    }
    
    public MigrationAbortionReason getMigrationAbortionReason() {
      return migrationAbortionReason;
    }
    
    public Throwable getMigrationAbortionCause() {
      return migrationAbortionCause;
    }
    
  }


  private static class NoOpRuntimeDependencyLock implements IRuntimeDependencyLock {

    @Override
    public void lock(boolean workflowProtectionNecessary) {
    }


    @Override
    public void unlock() {
    }


    @Override
    public List<XFMG_CouldNotStartApplication> getExceptionsAtUnlock() {
      return Collections.emptyList();
    }

  }

  public static enum MigrationAbortionReason {
    UNACCESSIBLE_ORDERS, EXCEPTION, UNSPECIFIED;
  }

  public static enum ActiveOrderType {
    ORDER, CRON, BATCH;
  }

}

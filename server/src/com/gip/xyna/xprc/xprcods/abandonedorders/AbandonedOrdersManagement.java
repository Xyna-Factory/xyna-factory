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

package com.gip.xyna.xprc.xprcods.abandonedorders;



import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_IncompleteIntentionallyAbandonedOrder;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.exceptions.XPRC_UnknownIntentionallyAbandonedOrderID;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.MIEntryWithoutOrderbackup;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderWithMissingCapacity;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderWithoutOrderbackup;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderbackupWillNotResume;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderbackupWithOrderarchiveHistory;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderbackupWithWrongBinding;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderbackupWithoutOrderarchiveDefault;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.OrderbackupWithoutParentAndWithoutXynaOrder;
import com.gip.xyna.xprc.xprcods.abandonedorders.rules.SynchronizationEntryWithoutOrderbackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.XynaScheduler;



public class AbandonedOrdersManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "Abandoned Orders Management";
  private Map<String, AbandonedOrderDetectionRule<? extends AbandonedOrderDetails>> detectionRules;
  private static PreparedQueryCache queryCache = new PreparedQueryCache();

  private static final String sqlGetAllDetailsByRootIdQuery = "select * from " + OrderInstanceDetails.TABLE_NAME
      + " where " + OrderInstanceDetails.COL_ROOT_ID + " = ?";
  private static final String sqlGetAbandonedInformationByOrderIdQuery = "select * from "
      + AbandonedOrderInformationStorable.TABLE_NAME + " where " + AbandonedOrderInformationStorable.COL_ORDER_ID
      + " = ?";
  private static final String sqlGetAbandonedInformationByRootOrderIdQuery = "select * from "
    + AbandonedOrderInformationStorable.TABLE_NAME + " where " + AbandonedOrderInformationStorable.COL_ROOT_ORDER_ID
    + " = ?";

  public AbandonedOrdersManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    detectionRules = new HashMap<String, AbandonedOrderDetectionRule<? extends AbandonedOrderDetails>>();
    FutureExecutionTask fe = new FutureExecutionTask(XynaFactory.getInstance().getFutureExecution().nextId()) {

      @Override
      public int[] after() {
        return new int[] {OrderArchive.FUTURE_EXECUTION_ID, };
      }

      @Override
      public void execute() {

      }
    };
    XynaFactory.getInstance().getFutureExecution().execAsync(fe);

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(AbandonedOrdersManagement.class, "AbandonedOrdersManagement.initRules").
      after(OrderArchive.FUTURE_EXECUTION_ID).
      after(SuspendResumeManagement.class). //wegen SuspensionEntryWithoutOrderbackup
      execAsync(new Runnable() { public void run() { initRules(); } } );
  }

  
  private void initRules() {
    try {
      ODSImpl.getInstance().registerStorable(AbandonedOrderInformationStorable.class);
    } catch (PersistenceLayerException e1) {
      throw new RuntimeException("Failed to register storable for table "
          + AbandonedOrderInformationStorable.TABLE_NAME, e1);
    }
    ODSConnection hisCon = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      //          PreparedQuery<AbandonedOrderInformationStorable> maxIdQuery =
      //              hisCon.prepareQuery(new Query<AbandonedOrderInformationStorable>("select * from "
      //                  + AbandonedOrderInformationStorable.TABLE_NAME + " order by "
      //                  + AbandonedOrderInformationStorable.COL_ID + " desc", AbandonedOrderInformationStorable.reader));
      //        } catch (PersistenceLayerException e) {
      //          throw new RuntimeException("Failed to initialize " + DEFAULT_NAME, e);
    } finally {
      try {
        hisCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }

    try {
      // TODO : sort by serverity ascending (most serious rules at bottom)
      detectionRules.put(OrderbackupWithoutParentAndWithoutXynaOrder.class.getSimpleName(),
                         new OrderbackupWithoutParentAndWithoutXynaOrder());
      detectionRules.put(MIEntryWithoutOrderbackup.class.getSimpleName(), new MIEntryWithoutOrderbackup());
      detectionRules.put(OrderWithoutOrderbackup.class.getSimpleName(), new OrderWithoutOrderbackup());
      detectionRules.put(OrderWithMissingCapacity.class.getSimpleName(), new OrderWithMissingCapacity());
      detectionRules.put(OrderbackupWithWrongBinding.class.getSimpleName(), new OrderbackupWithWrongBinding());
      detectionRules.put(OrderbackupWithOrderarchiveHistory.class.getSimpleName(),
                         new OrderbackupWithOrderarchiveHistory());
      detectionRules.put(OrderbackupWithoutOrderarchiveDefault.class.getSimpleName(),
                         new OrderbackupWithoutOrderarchiveDefault());
      detectionRules.put(SynchronizationEntryWithoutOrderbackup.class.getSimpleName(),
                         new SynchronizationEntryWithoutOrderbackup());
      detectionRules.put(OrderbackupWillNotResume.class.getSimpleName(), new OrderbackupWillNotResume());
    } catch (PersistenceLayerException e) {
      logger.error("Failed to create all abandoned orders rules. Discovery of abandoned orders may be incomplete.",
                   e);
    }
    
    // TODO weitere Regeln:
    //        * Es existiert kein Serien-Vorg�nger und man kann ausschlie�en, dass dieser noch kommt
    //        * Parentauftrag wartet auf ResponseListener, und Subauftrag ist irgendwie verstorben

  }
  
  

  @Override
  protected void shutdown() throws XynaException {
    detectionRules.clear();
  }


  public class AbandonedOrderInformationBean {

    private Long id;
    private Long orderID;
    private Long rootOrderID;
    private String shortDescription;
    private String problemDescription;
    private String proposedSolution;


    public AbandonedOrderInformationBean(Long id, Long orderID, Long rootOrderID, String shortDescription,
                                         String problemDescription, String proposedSolution) {
      this.id = id;
      this.orderID = orderID;
      this.rootOrderID = rootOrderID;
      this.shortDescription = shortDescription;
      this.problemDescription = problemDescription;
      this.proposedSolution = proposedSolution;
    }


    public Long getID() {
      return id;
    }


    public Long getOrderID() {
      return orderID;
    }


    public Long getRootOrderID() {
      return rootOrderID;
    }


    public String getShortDescription() {
      return shortDescription;
    }


    public String getProblemDescription() {
      return problemDescription;
    }


    public String getProposedSolution() {
      return proposedSolution;
    }
  }


  public List<AbandonedOrderInformationBean> listAbandonedOrders() throws PersistenceLayerException {
    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    Collection<AbandonedOrderInformationStorable> detectedOrders;
    List<AbandonedOrderInformationBean> orderInformation = new ArrayList<AbandonedOrderInformationBean>();

    try {
      detectedOrders = defaultCon.loadCollection(AbandonedOrderInformationStorable.class);
    } finally {
      defaultCon.closeConnection();
    }

    for (AbandonedOrderInformationStorable aois : detectedOrders) {
      AbandonedOrderDetectionRule<?> detector = detectionRules.get(aois.getRulename());
      orderInformation
          .add(new AbandonedOrderInformationBean(aois.getId(), aois.getOrderId(), aois.getRootOrderId(), detectionRules
              .get(aois.getRulename()).getShortName(), detector.getClass().cast(detector)
              .describeProblem(detector.getDetailClassType().cast(aois.getDetails())), detectionRules
              .get(aois.getRulename()).describeSolution()));
    }

    ODSConnection historyConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      detectedOrders = historyConnection.loadCollection(AbandonedOrderInformationStorable.class);
    } finally {
      historyConnection.closeConnection();
    }

    for (AbandonedOrderInformationStorable aois : detectedOrders) {
      orderInformation.add(new AbandonedOrderInformationBean(aois.getId(), aois.getOrderId(), aois.getRootOrderId(),
                                                             "Intentionally abandoned order",
                                                             "The execution of the order has been abandoned because the "
                                                                 + "following exception could not be handled:"
                                                                 + ((IntentionallyAbandonedOrderDetails) aois
                                                                     .getDetails()).getCauseDetails(),
                                                             "Resolve this order to continue its execution."));
    }

    return orderInformation;
  }


  public boolean forceCleanAbandonedOrder(Long entryID, boolean usingAllDetectors) throws PersistenceLayerException {
    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    AbandonedOrderInformationStorable order = new AbandonedOrderInformationStorable(entryID);

    try {
      defaultCon.queryOneRow(order);

      if (usingAllDetectors) {
        for (AbandonedOrderDetectionRule<? extends AbandonedOrderDetails> detector : detectionRules.values()) {
          detector.forceClean(order.getDetails());
        }

        PreparedQuery<AbandonedOrderInformationStorable> entriesForOrderID =
            (PreparedQuery<AbandonedOrderInformationStorable>) queryCache
                .getQueryFromCache(sqlGetAbandonedInformationByOrderIdQuery, defaultCon,
                                   new AbandonedOrderInformationStorable().getReader());
        List<AbandonedOrderInformationStorable> affectedEntries =
            defaultCon.query(entriesForOrderID, new Parameter(order.getDetails().getOrderID()), -1);
        defaultCon.delete(affectedEntries);
      } else {
        AbandonedOrderDetectionRule<?> detector = detectionRules.get(order.getRulename());
        detector.forceClean(order.getDetails());
        defaultCon.deleteOneRow(order);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.debug("Could not find entry to resolve <" + entryID + ">", e);
      return false;
    } finally {
      defaultCon.commit();
      defaultCon.closeConnection();
    }

    return true;
  }

  
  public boolean forceCleanAbandonedOrderFamily(Long entryID) throws PersistenceLayerException {
    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    AbandonedOrderInformationStorable order = new AbandonedOrderInformationStorable(entryID);

    try {
      defaultCon.queryOneRow(order);

      for (AbandonedOrderDetectionRule<? extends AbandonedOrderDetails> detector : detectionRules.values()) {
        detector.forceCleanFamily(order.getDetails());
      }

      PreparedQuery<AbandonedOrderInformationStorable> entriesForOrderID =
          (PreparedQuery<AbandonedOrderInformationStorable>) queryCache
              .getQueryFromCache(sqlGetAbandonedInformationByRootOrderIdQuery, defaultCon,
                                 new AbandonedOrderInformationStorable().getReader());
      List<AbandonedOrderInformationStorable> affectedEntries =
          defaultCon.query(entriesForOrderID, new Parameter(order.getDetails().getRootOrderID()), -1);
      defaultCon.delete(affectedEntries);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.debug("Could not find entry to resolve <" + entryID + ">", e);
      return false;
    } finally {
      defaultCon.commit();
      defaultCon.closeConnection();
    }

    return true;
  }
  

  public int forceCleanAllAbandonedOrders() throws PersistenceLayerException {
    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    Collection<AbandonedOrderInformationStorable> detectedOrders;

    try {
      detectedOrders = defaultCon.loadCollection(AbandonedOrderInformationStorable.class);

      for (AbandonedOrderInformationStorable order : detectedOrders) {
        for (AbandonedOrderDetectionRule<? extends AbandonedOrderDetails> detector : detectionRules.values()) {
          detector.forceCleanFamily(order.getDetails());
        }

        defaultCon.deleteOneRow(order);
      }
      
      return detectedOrders.size();
    } finally {
      defaultCon.commit();
      defaultCon.closeConnection();
    }
  }


  @SuppressWarnings("unchecked")
  public boolean resolveAbandonedOrder(Long entryID, OutputStream statusOutputStream) throws PersistenceLayerException,
      XPRC_UnknownIntentionallyAbandonedOrderID, XPRC_IncompleteIntentionallyAbandonedOrder {

    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);

    try {
      AbandonedOrderInformationStorable order = new AbandonedOrderInformationStorable(entryID);
      defaultCon.queryOneRow(order);

      // we could find a way to avoid casting here
      AbandonedOrderDetectionRule<?> detector = detectionRules.get(order.getRulename());

      try {
        detector.getClass().cast(detector).resolve(detector.getDetailClassType().cast(order.getDetails()));
      } catch (ResolveForAbandonedOrderNotSupported e) {
        logger.debug("Resolving of order <" + order.getOrderId() + "> is not supported");
        return false;
      }

      defaultCon.deleteOneRow(order);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      resolveIntentionallyAbandonedOrder(entryID, statusOutputStream);
      return true;
    } finally {
      defaultCon.commit();
      defaultCon.closeConnection();
    }

    return true;
  }


  @SuppressWarnings("unchecked")
  public ResolvedAbandonedOrdersBean resolveAllAbandonedOrders() throws PersistenceLayerException {
    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    Collection<AbandonedOrderInformationStorable> detectedOrders;
    int resolved = 0;
    try {
      detectedOrders = defaultCon.loadCollection(AbandonedOrderInformationStorable.class);

      for (AbandonedOrderInformationStorable order : detectedOrders) {
        // TODO: why does it need to be so ugly? damned casting!
        AbandonedOrderDetectionRule<?> detector = detectionRules.get(order.getRulename());
        try {
        detector.getClass().cast(detector).resolve(detector.getDetailClassType().cast(order.getDetails()));
          resolved++;
        } catch(ResolveForAbandonedOrderNotSupported e) {
          continue;
        }

        defaultCon.deleteOneRow(order);
      }
        defaultCon.commit();
      return new ResolvedAbandonedOrdersBean(resolved, detectedOrders.size());
    } finally {
      defaultCon.closeConnection();
    }
  }

  public class ResolvedAbandonedOrdersBean {
    private int resolved = 0;
    private int discovered = 0;

    public ResolvedAbandonedOrdersBean(int resolved, int discovered) {
      this.resolved = resolved;
      this.discovered = discovered;
    }

    public int getResolved() {
      return resolved;
    }

    public int getDiscovered() {
      return discovered;
    }
    
  }
  

  public class DiscoveredAbandonedOrdersBean {

    private int found = 0;
    private int error = 0;


    public DiscoveredAbandonedOrdersBean(int found, int error) {
      this.found = found;
      this.error = error;
    }


    public int getFoundCount() {
      return found;
    }


    public int getErrorCount() {
      return error;
    }
  }


  public DiscoveredAbandonedOrdersBean discoverAbandonedOrders(boolean deepSearch) throws PersistenceLayerException {
    Integer totalErrorCount = 0;
    int maxrows = 100;
    Map<String, List<AbandonedOrderDetails>> detectedOrders = new HashMap<String, List<AbandonedOrderDetails>>();

    for (AbandonedOrderDetectionRule<? extends AbandonedOrderDetails> rule : detectionRules.values()) {
      if (!rule.isDeepSearch() || deepSearch) {
        try {
          List<? extends AbandonedOrderDetails> detectedForThisRule = rule.detect(maxrows);

          for (AbandonedOrderDetails details : detectedForThisRule) {
            // FIXME statt getClass().getSimpleName() einen vordefinierten Namen verwenden!
            String identifier = rule.getClass().getSimpleName();
            List<AbandonedOrderDetails> detailsList = detectedOrders.get(identifier);
            if (detailsList == null) {
              detailsList = new ArrayList<AbandonedOrderDetails>();
              detectedOrders.put(identifier, detailsList);
            }
            detailsList.add(details);
          }
        } catch (PersistenceLayerException ple) {
          logger.error("There was an error executing the AbandonedOrderDetectionRule "
              + rule.getClass().getSimpleName() + ".", ple);
          totalErrorCount++;
        }
      }
    }

    Collection<AbandonedOrderInformationStorable> intentionallyAbandonedOrders = null;
    
    ODSConnection historyConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      intentionallyAbandonedOrders = historyConnection.loadCollection(AbandonedOrderInformationStorable.class);
    } finally {
      historyConnection.closeConnection();
    }
    
    List<AbandonedOrderInformationStorable> realAbandonedOrders = new LinkedList<AbandonedOrderInformationStorable>();
    
    for (Entry<String, List<AbandonedOrderDetails>> detailsEntry : detectedOrders.entrySet()) {
      for (AbandonedOrderDetails innerDetails : detailsEntry.getValue()) {
        boolean isIntentionallyAbandoned = false;
        
        for (AbandonedOrderInformationStorable intentionallyAbandonedOrder : intentionallyAbandonedOrders) {
          if (innerDetails.getRootOrderID() == intentionallyAbandonedOrder.getRootOrderId()) {
            isIntentionallyAbandoned = true;
            break;
          }
        }
        
        if (!isIntentionallyAbandoned) {
          realAbandonedOrders.add(new AbandonedOrderInformationStorable(innerDetails.getOrderID(), innerDetails.getRootOrderID(), detailsEntry.getKey(), innerDetails));
        }
      }
    }
    
    ODSConnection defaultCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {

      // zun�chst alte Eintr�ge l�schen, die m�glicherweise gar nicht mehr aktuell sind
      // TODO falls man keine deep search anstellt, nur diese l�schen, die durch eine nicht-deep search auch wieder
      //      gefunden werden!
      defaultCon.deleteAll(AbandonedOrderInformationStorable.class);
      defaultCon.persistCollection(realAbandonedOrders);
      defaultCon.commit();
    } finally {
      defaultCon.closeConnection();
    }

    // TODO Offene Punkte:
    //        * sollten ab und zu diese orders gel�scht werden?
    //        * wie kommt man an die abandoned orders ran, die mehr als "maxrows 100" entfernt liegen? gar nicht?
    //        * (Anzahl der Regeln)*100 kann trotzdem viel sein. Auch die Gesamtzahl beschr�nken? Default geht ja
    //          dann nach Memory.


    return new DiscoveredAbandonedOrdersBean(realAbandonedOrders.size(), totalErrorCount);
  }


  public static class AbandonedOrderCouldNotBeStoredException extends Exception {

    private static final long serialVersionUID = 1L;


    public AbandonedOrderCouldNotBeStoredException(String cause) {
      super(cause);
    }


    public AbandonedOrderCouldNotBeStoredException(PersistenceLayerException e) {
      super(e);
    }

  }

  public static class ResolveForAbandonedOrderNotSupported extends Exception {

    private static final long serialVersionUID = 1L;
  }


  //  Code-Snipped f�r das erzeugen von intentionally abandoned orders zu platzieren in MasterWorkflowPostScheduler::normalMasterWorkflow vor handleSuspension(e); :
  //    if (xo.getOrderContext().getOrderType().equals("mk.Abandon")) {
  //      throw new XNWH_GeneralPersistenceLayerException("ich habe bock drauf zu schmei�en");
  //    }
  //  Der Workflow sollte einen Wait-Schritt haben.
  
  public void addIntentionallyAbandonedOrder(XynaOrderServerExtension abandonedRootOrder, Throwable cause)
      throws AbandonedOrderCouldNotBeStoredException {

    // TODO Diese Methode aufrufen, wenn z.B. in er Suspendierung eines Auftrags kein Zugriff mehr auf
    //      die DB m�glich ist. Danach wird dann wie bisher die OrderDeathException geworfen

    // diese Details nach HISTORY abspeichern, damit in der default-Konfiguration die Daten in XML aufbewahrt
    // werden k�nnen. PL-Fehler nach au�en propagieren, damit situationsabh�ngig darauf reagiert werden kann. 

    IntentionallyAbandonedOrderDetails newDetails =
        new IntentionallyAbandonedOrderDetails(abandonedRootOrder.getId(), abandonedRootOrder.getRootOrder().getId(), cause);
    AbandonedOrderInformationStorable storable =
        new AbandonedOrderInformationStorable(newDetails.getOrderID(), newDetails.getRootOrderID(), "intentionally", newDetails);

    if (!abandonedRootOrder.hasBeenBackuppedAtLeastOnce()) {
      // TODO hier k�nnte man auch ein backup nachtr�glich in ALTERNATIVE erzeugen. Das w�re allerdings auch bei
      //      Parallelit�ten wieder problematisch.
      logger.warn("No backup of order " + abandonedRootOrder.getId()
          + " is available, a resume may not be possible later.");
    }

    ODSConnection historyCon = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        PreparedQuery<AbandonedOrderInformationStorable> duplicateDetectionQuery =
            (PreparedQuery<AbandonedOrderInformationStorable>) queryCache
                .getQueryFromCache(sqlGetAbandonedInformationByOrderIdQuery, historyCon,
                                   new AbandonedOrderInformationStorable().getReader());
        if (historyCon.query(duplicateDetectionQuery, new Parameter(abandonedRootOrder.getId()), 1).size() > 0) {
          logger.info("Abandoned order information for order <" + abandonedRootOrder.getId()
              + " already present. Nothing to be done.");
          return;
        }
      } catch (PersistenceLayerException e) {
        logger.warn("Could not determine uniqueness of abandoned order, continuing.", e);
      }
      historyCon.persistObject(storable);
      historyCon.commit();
    } catch (PersistenceLayerException e) {
      throw new AbandonedOrderCouldNotBeStoredException(e);
    } finally {
      try {
        historyCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }

  }


  /**
   * Spezialbehandlung f�r die intentionally abandoned orders: <li>Status f�r Auftrag korrigieren</li> <li>Status f�r
   * alle Subauftr�ge �ndern, die noch nicht fertig gelaufen sind</li> <li>Suspensionentries l�schen (evtl l�sst sich
   * cleanupSuspensionEntries verwenden?)</li> <li>root-Auftrag wieder frisch in den Scheduler einstellen</li>
   * @throws XPRC_UnknownIntentionallyAbandonedOrderID if the passed rootorderid does not belong to an intentionally
   *           abandoned order
   * @throws XPRC_IncompleteIntentionallyAbandonedOrder
   */
  private void resolveIntentionallyAbandonedOrder(long entryId, OutputStream statusOutputStream)
      throws PersistenceLayerException, XPRC_UnknownIntentionallyAbandonedOrderID,
      XPRC_IncompleteIntentionallyAbandonedOrder {

    long rootOrderId;

    ODSConnection historyConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      // check existence
      AbandonedOrderInformationStorable targetAbandonedOrder = new AbandonedOrderInformationStorable(entryId);
      historyConnection.queryOneRow(targetAbandonedOrder);
      rootOrderId = targetAbandonedOrder.getDetails().getRootOrderID();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XPRC_UnknownIntentionallyAbandonedOrderID(entryId, e);
    } finally {
      historyConnection.closeConnection();
    }

    ODSConnection defaultConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {

      PreparedQuery<OrderInstanceDetails> allDetailsByRootIdQuery =
          (PreparedQuery<OrderInstanceDetails>) queryCache.getQueryFromCache(sqlGetAllDetailsByRootIdQuery,
                                                                             defaultConnection,
                                                                             new OrderInstanceDetails().getReader());
      List<OrderInstanceDetails> orderDetailsFamily =
          defaultConnection.query(allDetailsByRootIdQuery, new Parameter(rootOrderId), -1);
      Map<Long, OrderInstanceDetails> orderDetailsById = new HashMap<Long, OrderInstanceDetails>();
      for (OrderInstanceDetails oid : orderDetailsFamily) {
        orderDetailsById.put(oid.getId(), oid);
      }

      PreparedQuery<OrderInstanceBackup> allBackupsByRootIdQuery =
          defaultConnection.prepareQuery(new Query<OrderInstanceBackup>("select * from "
                                             + OrderInstanceBackup.TABLE_NAME + " where "
                                             + OrderInstanceBackup.COL_ROOT_ID + "=?", OrderInstanceBackup
                                             .getReaderWarnIfNotDeserializable()), false);
      List<OrderInstanceBackup> orderBackupsFamily =
          defaultConnection.query(allBackupsByRootIdQuery, new Parameter(rootOrderId), -1);

      Map<Long, OrderInstanceBackup> orderBackupsById = new HashMap<Long, OrderInstanceBackup>();
      for (OrderInstanceBackup oib : orderBackupsFamily) {
        orderBackupsById.put(oib.getId(), oib);
      }

      if (!orderBackupsById.containsKey(rootOrderId)) {
        throw new XPRC_IncompleteIntentionallyAbandonedOrder(entryId);
      }

      XynaOrderServerExtension rootOrder = orderBackupsById.get(rootOrderId).getXynaorder();
      if (rootOrder == null) {
        throw new XPRC_IncompleteIntentionallyAbandonedOrder(entryId);
      }

      // die Daten in Memory sind potentiell neuer, also backups mit den orderarchive default Daten aktualisieren
      for (Entry<Long, OrderInstanceBackup> e : orderBackupsById.entrySet()) {
        OrderInstanceDetails newestDetails = orderDetailsById.get(e.getKey());
        if (newestDetails != null) {
          e.getValue().setDetails(newestDetails);
        }
        newestDetails = e.getValue().getDetails();
        if (newestDetails != null) {
          newestDetails.setStatus(OrderInstanceStatus.SCHEDULING);
        }
      }

      List<XynaOrderServerExtension> rootAndChildren = rootOrder.getOrderAndChildrenRecursively();
      writeStatus("Freeing capacities for root order and all child orders before adding the root order to the scheduler.",
                  statusOutputStream);
      
      XynaScheduler sched = XynaFactory.getInstance().getProcessing().getXynaScheduler();
      SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
      
      for (XynaOrderServerExtension xo : rootAndChildren) {
        // capacities m�ssen freigegeben werden, weil sie im Scheduler wieder neu geholt werden.
        writeStatus("Freeing capacities for order " + xo.getId(), statusOutputStream);
        sched.getCapacityManagement().forceFreeCapacities(xo.getId());
        srm.cleanupSuspensionEntries(xo.getId());
      }

      try {
        sched.addOrder(rootOrder, defaultConnection, false);
      } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e1) {
        throw new RuntimeException("Resumed orders should not be acknowledged here.", e1);
      }

      defaultConnection.persistCollection(orderBackupsById.values());
      defaultConnection.commit();

    } finally {
      defaultConnection.closeConnection();
    }

    historyConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      AbandonedOrderInformationStorable targetAbandonedOrder = new AbandonedOrderInformationStorable(entryId);
      historyConnection.deleteOneRow(targetAbandonedOrder);
      historyConnection.commit();
    } finally {
      historyConnection.closeConnection();
    }

    // TODO offene Fragen:
    //        * muss es eine Sonderbehandlung f�r Auftr�ge geben, die in der compensation abandoned wurden?
    //        * Ist eine sonderbehandlung f�r Serien erforderlich?
  }


  private void writeStatus(String status, OutputStream statusOutputStream) {
    try {
      statusOutputStream.write((status + "\n").getBytes(Constants.DEFAULT_ENCODING));
    } catch (UnsupportedEncodingException e1) {
      logger.warn(null, e1);
    } catch (IOException e1) {
      logger.warn(null, e1);
    }
  }

}

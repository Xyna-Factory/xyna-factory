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

package com.gip.xyna.xprc.xsched;



import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Tree;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.SeriesInformation;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.XynaOrderServerExtension.TransientFlags;
import com.gip.xyna.xprc.exceptions.XPRC_CircularDependencyInSeriesException;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xsched.orderseries.CorrelationIdTransformator;
import com.gip.xyna.xprc.xsched.orderseries.CorrelationIdTransformator.CommaMasker;
import com.gip.xyna.xprc.xsched.orderseries.OSMCache.SearchResult;
import com.gip.xyna.xprc.xsched.orderseries.OSMCacheDBImpl;
import com.gip.xyna.xprc.xsched.orderseries.OSMCacheImpl;
import com.gip.xyna.xprc.xsched.orderseries.OSMInterface;
import com.gip.xyna.xprc.xsched.orderseries.OSMLocalImpl;
import com.gip.xyna.xprc.xsched.orderseries.OSMRemoteEndpointImpl;
import com.gip.xyna.xprc.xsched.orderseries.OSMRemoteProxyImpl;
import com.gip.xyna.xprc.xsched.orderseries.OSMTaskConsumer;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.Mode;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.WaitingOrder;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesSeparator;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.SisData;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.TreeNode;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask_CleanPredecessorTrees;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask_Reschedule;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder.WaitingCause;



/**
 *
 */
public class OrderSeriesManagement extends FunctionGroup implements OSMInterface, Clustered {

  public static final String DEFAULT_NAME = "Order Series Management";
  
  private ODS ods;
  
  private ClusterContext rmiClusterContext;
  private ClusterContext storableClusterContext = ClusterContext.NO_CLUSTER;

  private OSMClusterStateChangeHandler osmClusterStateChangeHandler = new OSMClusterStateChangeHandler();
  private RMIClusterStateChangeHandler rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
  
  private ArrayBlockingQueue<OSMTask> externalQueue; //externe Queue für Task, die neu ins OSM eingestellt 
                                                     //werden. Blockierend als Überlastschutz
  private Queue<OSMTask> internalQueue; //interne Queue für Tasks, die während der Taskbearbeitung anfallen
  private OSMTaskConsumer osmTaskConsumer;
  private Thread consumerThread; //Achtung: derzeit nur ein Thread. Falls dies nicht reicht,
   //muss nochmal sehr auf die verwendeten Objekte geachtet werden: derzeit sind diese nicht threadsafe!
  private OSMCacheImpl osmCacheImpl;
  private OSMCacheDBImpl osmDbBackend;
  private int ownBinding;
  private OSMLocalImpl osmLocal;
  private OSMRemoteEndpointImpl osmRemoteEndpoint;
  private OSMRemoteProxyImpl osmRemoteProxy;
  private PredecessorTrees predecessorTrees;
  private HashParallelReentrantLock<Long> parallelLock = new HashParallelReentrantLock<Long>(32); //verhindert konkurrierende 
          //Zugriffe auf waitingOrders und readyOrders mit gleicher OrderId

  private Map<Long,Triple<String,OrderState,List<String>>> readyOrders; //Aufträge, die sofort in den Scheduler eingestellt werden können 
  private AllOrdersList allOrders;
  
  private static CommaMasker commaMasker = CorrelationIdTransformator.commaMasker;
  
  public OrderSeriesManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    externalQueue = new ArrayBlockingQueue<OSMTask>( XynaProperty.ORDER_SERIES_QUEUE_SIZE.get().intValue() );
    internalQueue = new LinkedList<OSMTask>();
    
    readyOrders = Collections.synchronizedMap( new HashMap<Long,Triple<String,OrderState,List<String>>>() );
    
    XynaProperty.ORDER_SERIES_MAX_SI_STORABLES_IN_CACHE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.ORDER_SERIES_MAX_PRE_TREES_IN_CACHE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.ORDER_SERIES_LOCK_PARALLELISM.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.ORDER_SERIES_QUEUE_SIZE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.ORDER_SERIES_CLEAN_DATABASE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask("OrderSeriesManagement.initStorables", "OrderSeriesManagement.initStorables").
          after(PersistenceLayerInstances.class).
          before(XynaClusteringServicesManagement.class).
          execAsync(new Runnable() { public void run() { initStorables(); }});
    fExec.addTask(OrderSeriesManagement.class, "OrderSeriesManagement.initClusterStatusKnown").
    after(AllOrdersList.class,XynaClusteringServicesManagement.class).after("OrderSeriesManagement.initStorables").
    execAsync(new Runnable() { public void run() { initClusterStatusKnown(); }});
  }

  private void initStorables() {
    try {
      rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
      rmiClusterContext = new ClusterContext( rmiClusterStateChangeHandler, this );
      
      ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
      ods.registerStorable(SeriesInformationStorable.class);

      storableClusterContext = new ClusterContext( SeriesInformationStorable.class, ODSConnectionType.DEFAULT );
      osmClusterStateChangeHandler.setClusterContext(storableClusterContext);
      ods.addClusteredStorableConfigChangeHandler( storableClusterContext, ODSConnectionType.DEFAULT, SeriesInformationStorable.class);

      storableClusterContext.addClusterStateChangeHandler( osmClusterStateChangeHandler );
      
      osmDbBackend = new OSMCacheDBImpl();

    } catch (PersistenceLayerException e) {
      //FIXME bessere Behandlung?
      throw new RuntimeException(e);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + OrderSeriesManagement.class.getSimpleName() + " as clusterable component.", e);
    }
  }

  private void initClusterStatusKnown() {
    //ClusterStatus ist nun bekannt, daher können die restlichen Initialisierungen durchgeführt werden.
    allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    
    initOSM();
     
    boolean rmiClustered = rmiClusterContext.getClusterState() != ClusterState.NO_CLUSTER;
    boolean storableClustered = storableClusterContext.getClusterState() != ClusterState.NO_CLUSTER;
    
    if( rmiClustered && storableClustered ) {
      //OrderSeriesManagement ist geclustert: jetzt bereits starten, keinen Übergang nach Connected abwarten
    } else {
      if( !rmiClustered && !storableClustered ) {
        //OrderSeriesManagement ist nicht geclustert
      } else {
        //nur einer der beiden ist geclustert; dies ist ein schwerer Fehler der zum Abbruch führen muss.
        if( storableClustered ) {
          logger.warn( "SeriesInformationStorable is clustered but RMI is not clustered");
        } else {
          logger.error( "SeriesInformationStorable is not clustered but RMI is clustered");
        }
      }
    }
    
    boolean started = tryStartOsmTaskConsumer(true);
    if (started && logger.isInfoEnabled()) {
      logger.info("OrderSeriesManagement is started after cluster status is known");
    }
  }

  private void initOSM() {
    if( logger.isInfoEnabled() ) {
      logger.info("initOSM");
    }
    
    SeriesInformationStorable tmpInstance = new SeriesInformationStorable();
    ownBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
        
    osmDbBackend.setOwnBinding(ownBinding);
    
    osmCacheImpl = new OSMCacheImpl( ownBinding, 
      XynaProperty.ORDER_SERIES_LOCK_PARALLELISM.get().intValue(),
      XynaProperty.ORDER_SERIES_MAX_SI_STORABLES_IN_CACHE.get().intValue(),
      osmDbBackend
    );
    
    predecessorTrees = new PredecessorTrees(osmCacheImpl, internalQueue);
    osmLocal = new OSMLocalImpl(osmCacheImpl, internalQueue, this, predecessorTrees);
    
    osmRemoteEndpoint = new OSMRemoteEndpointImpl(osmLocal, ownBinding);
    osmRemoteProxy = new OSMRemoteProxyImpl( rmiClusterContext, osmRemoteEndpoint, externalQueue, osmCacheImpl );
    
    osmTaskConsumer = new OSMTaskConsumer(externalQueue, internalQueue, osmCacheImpl, this, osmLocal, osmRemoteProxy, predecessorTrees);
  }


  /**
   * Einrichten aller Start des OsmTaskConsumer
   * @throws PersistenceLayerException 
   */
  private synchronized boolean tryStartOsmTaskConsumer(boolean startEvenIfNotConnected) {
    if( logger.isInfoEnabled() ) {
      logger.info("tryStartOsmTaskConsumer("+startEvenIfNotConnected+")" );
    }
    
    if( osmRemoteProxy == null ) {
      if( logger.isInfoEnabled() ) {
        logger.info("tryStartOsmTaskConsumer could not run because OrderSeriesManagement is not initialized" );
      }
      return false;
    }
    
    if( consumerThread != null ) {
      return false; //consumerThread wurde bereits gebaut und gestartet
    }
    
    boolean rmiConnected = rmiClusterContext.getClusterState() == ClusterState.CONNECTED;
    
    if( !startEvenIfNotConnected ) {
      if( storableClusterContext.getClusterState() != ClusterState.CONNECTED ) {
        logger.info("SeriesInformationStorable is not connected" );
        return false;
      }
      if( ! rmiConnected ) {
        logger.info("OSM-RMI is not connected" );
        return false;
      }
    }
    
    if( logger.isInfoEnabled() ) {
      logger.info("OsmTaskConsumer can be started" );
    }
    
    osmRemoteProxy.setRmiConnected(rmiConnected);
    
    consumerThread = new Thread(osmTaskConsumer, DEFAULT_NAME);
    consumerThread.start();
    
    if( logger.isInfoEnabled() ) {
      logger.info("OsmTaskConsumer is started" );
    }
    return true;
  }

  
  
  @Override
  protected void shutdown() throws XynaException {
  }


  private void putInExternalQueue(OSMTask task) {
    try {
      externalQueue.put( task );
    } catch( InterruptedException e ) {
      //TODO was nun?
      throw new IllegalStateException("Unexpected interruption while trying to enqueue task "+task, e );
    }
  }

  /**
   * Auftragseingang: Mehrere Threads können gleichzeitig Aufträge einstellen und 
   * diese Methode aufrufen.<br>
   * Durch das Einstellen in die Queue hier wird die Parallelität im OSM verringert.
   * Der Insert und Commit des SeriesInformationStorable erfolgt noch in dem akktuellen Thread.
   * Dieser erhält daher auch die dabei möglichen Fehler.
   * 
   * @param xo
   * @throws XPRC_DUPLICATE_CORRELATIONID 
   * @throws XNWH_GeneralPersistenceLayerException 
   */
  public void preschedule(XynaOrderServerExtension xo) throws XNWH_GeneralPersistenceLayerException, XPRC_DUPLICATE_CORRELATIONID {
    SeriesInformationStorable sis = createSeriesInformationStorable(xo);
    String correlationId = sis.getCorrelationId();
    osmCacheImpl.lock(correlationId);
    try {     
      osmCacheImpl.insert(sis);
    } catch (XPRC_DUPLICATE_CORRELATIONID e) {
      xo.setTransientFlag(TransientFlags.DuplicateSeriesCorrelationId);
      throw e;
    } finally {
      osmCacheImpl.unlock(correlationId); 
    }
    
    putInExternalQueue( OSMTask.preschedule( sis, xo ) );
  }
  
  /**
   * Erzeugen des SeriesInformationStorable aus den Daten aus XynaOrder xo
   * @param xo
   * @return
   */
  public SeriesInformationStorable createSeriesInformationStorable(XynaOrder xo) {
    SeriesInformation si = null;
    if( OrderSeriesSeparator.hasToSeparateSeries(xo) ) {
      OrderSeriesSeparator oss = new OrderSeriesSeparator();
      si = oss.migrateSeriesInformation(xo);
    } else {
      si = xo.getSeriesInformation();
    }
    String correlationId = commaMasker.transform( si.getCorrelationId() );
    SeriesInformationStorable sis = new SeriesInformationStorable(ownBinding,correlationId);
    sis.setId(xo.getId());
    sis.setPredecessorCorrIds( CollectionUtils.transform( si.getPredecessorsCorrIds(), commaMasker ) );
    sis.setSuccessorCorrIds( CollectionUtils.transform( si.getSuccessorsCorrIds(), commaMasker ) );
    sis.setAutoCancel(si.isAutoCancel());
    return sis;
  }
  
  /**
   * Einstellen nach der OrderMigration: OSMTask_Resume muss ausgeführt werden
   * Achtung: Task darf erst nach dem Commit auf der übergebenen Connection laufen,
   * da sonst benötigte Daten aus dem OrderArchive nicht sichtbar sind. 
   * @param xo
   * @param con 
   */
  public void resume(XynaOrderServerExtension xo, ODSConnection con) {
    String correlationId = xo.getSeriesCorrelationId();
    if( correlationId == null ) {
      logger.warn( "seriesCorrelationId is null for xynaOrder "+ xo);
      return;
    }
    osmCacheImpl.get(correlationId);
    con.executeAfterCommit( new TaskEnqueuer( OSMTask.resume( correlationId ) ) );
  }
  
  private class TaskEnqueuer implements Runnable {
    private OSMTask task;
    public TaskEnqueuer(OSMTask task) {
      this.task = task;
    }
    public void run() {
      putInExternalQueue( task );
    }
  }
  
  /**
   * Auftragsausgang: Mehrere Threads bearbeiten die Aufträge und können diese
   * Methode dann gleichzeitig aufrufen.
   * Durch das Einstellen in die Queue hier wird die Parallelität im OSM verringert.
   * @param xo
   */
  public void finishOrder(XynaOrderServerExtension xo) {
    if (xo.isTransientFlagSet(TransientFlags.DuplicateSeriesCorrelationId)) {
      //wenn der Auftrag wegen einer doppelten CorrelationId abgebrochen wurde, darf
      //finish nicht ausgeführt werden, da sonst die SeriesInformation eines anderen
      //Auftrags überschrieben wird
      if (logger.isDebugEnabled()) {
        logger.debug("Don't execute OSMTask_Finish for order " + xo.getId() + " with correlationId " + xo.getSeriesCorrelationId());
      }
      return;
    }
    
    putInExternalQueue( OSMTask.finish( xo ) );
  }
  
  /**
   * Auftragsausgang: Mehrere Threads bearbeiten die Aufträge und können diese
   * Methode dann gleichzeitig aufrufen.
   * Durch das Einstellen in die Queue hier wird die Parallelität im OSM verringert.
   */  
  public void abortOrder(long xynaOrderId) {
    SearchResult result = osmCacheImpl.search(xynaOrderId);
    switch(result.getType()) {
      case NotFound:
        return;
      case Found:
        putInExternalQueue( OSMTask.abort(result.getCorrelationId()) );
        break ;
      case OtherBinding:
        // FIXME anderen Knoten benachrichtigen ... keine gute Lösung!!!
        putInExternalQueue( OSMTask.abort(result.getCorrelationId()) );
        logger.warn("Abort order series with other binding.");
    }
  }

  /**
   * Starten des Auftrags, falls er bereits in waitingOrders eingetragen ist. Ansonsten Speichern des
   * OrderState für {@link #addWaitingOrder(SchedulingOrder) addOrder }.
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMInterface#readyToRun(String, long, com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState, List)
   */
  public void readyToRun(String correlationId, long id, OrderState orderState, List<String> cycle ) {
    Long orderId = Long.valueOf(id);
    parallelLock.lock(orderId);
    try {
      SchedulingOrder so = allOrders.getSchedulingOrder(orderId);
      if( so != null ) {
        //Auftrag stand in waitingOrders, nun Orderstate prüfen
        if( canBeStarted(so, correlationId, orderState, cycle) ) {
          allOrders.seriesCompleted(so);
        }
      } else {
        //Auftrag steht noch nicht in waitingOrders, daher in readyOrders eintragen
        if( orderState == OrderState.AlreadyFinished ) {
          //da kein finish mehr aufgerufen wird, sollte nichts in readyOrders eingetragen werden,
          //TODO was tun?
        } else {
          readyOrders.put(orderId, Triple.of(correlationId,orderState,cycle) );
        }
      }
    } finally {
      parallelLock.unlock(orderId);
    }
  }
  
  /**
   * Eintragen der neuen SchedulingOrder in die Liste aller Aufträge. Falls kein OrderState in readyOrders
   * bekannt ist, wird die SchedulingOrder für {@link #readyToRun(String, long, com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState, List) readyToRun} gespeichert.
   * @param so
   */
  public void addWaitingOrder(SchedulingOrder so) {
    if( ! so.isWaitingFor(WaitingCause.Series) ) {
      return; //nichts zu tun
    }
    Long orderId = so.getOrderId();
    parallelLock.lock(orderId);
    try {
      Triple<String,OrderState,List<String>> triple = readyOrders.remove(orderId);
      if( triple != null ) {
        //Auftrag stand in readyOrders, nun OrderState prüfen
        if( canBeStarted(so, triple.getFirst(), triple.getSecond(), triple.getThird() ) ) {
          so.removeWaitingCause( WaitingCause.Series );
          return;
        }
      } else {
        //readyToRun wurde noch nicht gerufen, daher darauf warten
      }
      //wird nun hier überwacht
      XynaOrderServerExtension xo = so.getXynaOrderOrNull();
      if( xo != null ) {
        xo.setTransientFlag(TransientFlags.WasKnownToScheduler);
      } //else: Unerwartet, da SchedulingOrder eben erst angelegt
   } finally {
      parallelLock.unlock(orderId);
    }
  }
  
  /**
   * @param so
   * @param orderState
   * @param cycle
   */
  private boolean canBeStarted(SchedulingOrder so, String correlationId, OrderState orderState, List<String> cycle) {
    if( logger.isDebugEnabled() ) {
      logger.debug("startOrder("+so.getOrderId()+","+correlationId+","+orderState+")");
    }
    switch( orderState ) {
      case CanBeStarted:
        //Auftrag kann in den Scheduler eingestellt werden
        return true;
      case HasCyclicDependencies:
        //Auftrag muss wegen zyklischer Predecessor-Abhängigkeit abgebrochen werden
        allOrders.getXynaOrder(so).addException(new XPRC_CircularDependencyInSeriesException(String.valueOf(cycle)), ProcessingStage.INITIALIZATION );
        so.markAsTerminated();
        return true;
      case HasToBeCanceled:
        //Auftrag muss gecancelt werden
        so.markAsCanceled();
        return true;
      case NotFound:
        //sollte hier nicht möglich sein
        logger.warn("Unexpected orderState "+orderState+ " for order "+ so.getOrderId()+", store again");
        break;
      case WaitingForPredecessor:
        //sollte hier nicht möglich sein
        logger.warn("Unexpected orderState "+orderState+ " for order "+ so.getOrderId()+", store again");
        break;
      case AlreadyFinished: 
        //Eigentlich ist hier nichts zu tun. //TODO aber es könnte versucht werden, das AlreadyFinished
        //hier nachzuvollziehen: Einträge aus Listen entfernen
    }
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMInterface#getBinding()
   */
  public int getBinding() {
    return ownBinding;
  }
  
  public OrderSeriesManagementInformation getOrderSeriesManagementInformation(Mode mode) {
    OrderSeriesManagementInformation osmi = new OrderSeriesManagementInformation();
    
    List<XynaOrderInfo> waitingOrders = getAllWaitingOrders();
    
    osmi.setReadyOrders( readyOrders.size() );
    osmi.setWaitingOrders( waitingOrders.size() );
    osmi.setCacheSize( osmCacheImpl.size() );
    osmi.setCurrentTasks( externalQueue.size() + internalQueue.size() );
    
    osmi.setPredecessorTreesSize( predecessorTrees.getNumberOfTrees() );
    
    osmTaskConsumer.fillOrderSeriesManagementInformation( osmi, mode );
     
    if( mode == Mode.Basic ) {
      return osmi;
    }
    WaitingOrderTransformation toWaitingOrder = new WaitingOrderTransformation(mode);
    osmi.setWaitingOrderList( CollectionUtils.transformAndSkipNull( waitingOrders, toWaitingOrder ) );
    
    return osmi;
  }
  
  private class WaitingOrderTransformation implements CollectionUtils.Transformation<XynaOrderInfo, OrderSeriesManagementInformation.WaitingOrder>{
    private Mode mode;
    private PredecessorTransformation predecessorTransformation;
    
    private class PredecessorTransformation implements CollectionUtils.Transformation<Tree<String,PredecessorTrees.SisData>, OrderSeriesManagementInformation.WaitingOrder>{
      public WaitingOrder transform(Tree<String, SisData> from) {
        WaitingOrder wo = new WaitingOrder();
        wo.setCorrelationId( from.getKey() );
        SisData sd = from.getValue();
        if( sd != null ) {
          wo.setId( sd.getOrderId() );
          wo.setBinding( sd.getBinding() );
        }
        return wo;
      }
    }
    
    public WaitingOrderTransformation(Mode mode) {
      this.mode = mode;
      this.predecessorTransformation = new PredecessorTransformation();
    }
    public WaitingOrder transform(XynaOrderInfo from) {
      WaitingOrder wo = new WaitingOrder();
      wo.setId( from.getOrderId() );
      if( mode == Mode.Predecessors ) {
        SchedulingOrder so = allOrders.getSchedulingOrder(from.getOrderId());
        XynaOrder xo = allOrders.getXynaOrder(so);
        String correlationId = xo.getSeriesCorrelationId();
        wo.setCorrelationId( correlationId );
        TreeNode preTree = predecessorTrees.getTree(correlationId);
        if( preTree != null ) {
          wo.setBinding( preTree.hasData() ? preTree.getBinding() : ownBinding );
          boolean set = false;
          while( ! set ) {
            try { //ungeschützter Zugriff auf nicht-synchronisierte Map. Gleichzeitiger Zugriff ist unwahrscheinlich
              wo.setPredecessors( CollectionUtils.transform(preTree.getBranches(), predecessorTransformation ) );
              set = true;
            } catch( ConcurrentModificationException e ) {
              //ignorieren und Transformation wiederholen
            }
          }
        } else {
          //sollte nicht vorkommen: preTree darf erst in OSMTask_Finish entfernt werden
        }
      }
      return wo;
    }
    
  }
  

  private List<XynaOrderInfo> getAllWaitingOrders() {
    return allOrders.getWaitingForSeries();
  }

  public boolean isOrderWaiting(long orderId) {
    SchedulingOrder so = allOrders.getSchedulingOrder(orderId);
    if( so == null ) {
      return false;
    } else {
      return so.isWaitingFor(WaitingCause.Series);
    }
  }

  public XynaOrder getWaitingOrder(long orderId) {
    SchedulingOrder so = allOrders.getSchedulingOrder(orderId);
    if( so != null ) {
      return allOrders.getXynaOrder(so);
    } else {
      return null;
    }
  }

  /**
   * Entfernen des Auftrags aus dem OrderSeriesManagement: Der Auftrag wird sofort gestartet.
   * Sollte nur von OSMTaks_reschedule in Notfällen gerufen werden.
   * @param orderId
   */
  public void removeOrder(long orderId) {
    Long orderIdL = Long.valueOf(orderId);
    parallelLock.lock(orderIdL);
    try {
      SchedulingOrder so = allOrders.getSchedulingOrder(orderId);
      if( so.isMarkedAsRemove() || ! so.isWaitingFor(WaitingCause.Series) ) {
        return; //Auftrag doch schon weg
      }
      Triple<String, OrderState, List<String>> ready = readyOrders.remove(orderId);
      if( ready != null ) {
        logger.info("readyOrders contained "+ready );
      }
      logger.info("Removed order "+orderId+" from OrderSeriesManagement will be started immediately");
      allOrders.seriesCompleted(so);
    } finally {
      parallelLock.unlock(orderIdL);
    }
  }
  
  private class OSMClusterStateChangeHandler implements ClusterStateChangeHandler {

    private volatile boolean isReadyForChange = true;
    private ClusterState clusterState;
    
    public boolean isReadyForChange(ClusterState newState) {
      return isReadyForChange;
    }

    public void setClusterContext(ClusterContext storableClusterContext) {
    }

    public void onChange(ClusterState newState) {
      if( logger.isInfoEnabled() ) {
        logger.info("OrderSeriesManagement.OSMClusterStateChangeHandler.onChange("+newState+")");
      }
      if( newState == ClusterState.CONNECTED ) {
        boolean started = tryStartOsmTaskConsumer(false);
        if (started && logger.isInfoEnabled()) {
          logger.info("OrderSeriesManagement is started after storableClusterContext-stateChange "
                          + clusterState + "->" + newState);          
        }
      } else {
        //TODO wegen DISCONNECTED_SLAVE etwas unternehmen?
      }
      clusterState = newState;
    }
  
  }
  
  private class RMIClusterStateChangeHandler implements ClusterStateChangeHandler {
    private ClusterState clusterState;
    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }
    public void onChange(ClusterState newState) {
      if( logger.isInfoEnabled() ) {
        logger.info("OrderSeriesManagement.RMIClusterStateChangeHandler.onChange("+newState+")");
      }
      //Dieser ClusterStateChangeHandler muss fast nichts machen, da alle wichtigen Übergänge 
      //vom OSMClusterStateChangeHandler erledigt werden, da dieser die wesentlich genaueren 
      //Statusübergänge des StorableClusterContext beobachtet.
      //Lediglich der Übergang nach CONNECTED muss beobachtet werden, da hier das Setzen des 
      //OSMRemoteProxyImpl erst erfolgen darf, wenn beide ClusterContext im Zustand CONNECTED sind
      if( newState == ClusterState.CONNECTED ) {
        boolean started = tryStartOsmTaskConsumer(false);
        if (started && logger.isInfoEnabled()) {
          logger.info("OrderSeriesManagement is started after rmiClusterContext-stateChange "
                      + clusterState + "->" + newState);          
        }
        if( osmRemoteProxy != null ) {
          osmRemoteProxy.setRmiConnected(true);
        }
      } else {
        if( osmRemoteProxy != null ) {
          osmRemoteProxy.setRmiConnected(false);
        }
      }
      
      clusterState = newState;
    }

  }

  
  //Implementierung des Interface Clustered
  public boolean isClustered() {
    return rmiClusterContext.isClustered();
  }
  public long getClusterInstanceId() {
    return rmiClusterContext.getClusterInstanceId();
  }


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    if (!XynaFactory.getInstance().isStartingUp()) {
      boolean storableUnclustered = storableClusterContext.getClusterState() == ClusterState.NO_CLUSTER;
      if (storableUnclustered) {
        throw new XFMG_ClusterComponentConfigurationException(getName(),
                                                              clusterInstanceId,
                                                              new Exception(
                                                                            "OrderSeriesManagement can not be clustered "
                                                                                + "because SeriesInformationStorable ist not clustered"));
      }
    }
    rmiClusterContext.enableClustering(clusterInstanceId);
  }


  public void disableClustering() {
    rmiClusterContext.disableClustering();
  }
  public String getName() {
    return getDefaultName();
  }


  public boolean hasToSeparateSeries(XynaOrderServerExtension xo) {
    return OrderSeriesSeparator.hasToSeparateSeries(xo);
  }
  
  /**
   * Zerlegen der OrderSerien, wenn diese im alten Format (Huckepack-Aufträge) vorliegen 
   * @param xo
   * @param backupCon
   * @param hasToBeAcknowledged
   */
  public void separateSeries(XynaOrderServerExtension xo, ODSConnection backupCon, boolean hasToBeAcknowledged) {    
    OrderSeriesSeparator oss = new OrderSeriesSeparator(hasToBeAcknowledged, backupCon);
    oss.separate(xo);
    
    //Einstellen der Serien-Aufträge in XynaProcessCtrlExecution: 
    //Durchführung von Planning und Prescheduling, bis XynaScheduler.addOrder fertig ist.
    boolean successful = true;
    XynaProcessCtrlExecution xprcctrl = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
    for (XynaOrderServerExtension seriesXo : oss.getSeries() ) {
      if( hasToBeAcknowledged ) {
        oss.prepareAcknowledge( seriesXo );
      }
      xprcctrl.startOrder(seriesXo, xo.getResponseListener(), seriesXo.getOrderContext() );
      if( seriesXo.hasError() ) {
        successful = false;
      }
    }
    if( ! successful ) {
      //Aufträge sind nicht erfolgreich durch das Planning gelaufen
      //evtl. loggen
    }
    
    //für den Basis-Auftrag muss das alte Acknowledge verwendet werden
    oss.restoreAcknowledge( xo );
  }


  /**
   * @param orderId
   * @param force
   * @return
   */
  public RescheduleSeriesOrderInformation rescheduleSeriesOrder(long orderId, boolean force) {
    if( orderId == 0 ) {
      OSMTask_CleanPredecessorTrees task = new OSMTask_CleanPredecessorTrees();
      putInExternalQueue( task );
      try {
        task.await(); //auf Bearbeitung des Tasks warten
      } catch( InterruptedException e ) {
        //ignorieren, Info ist dann halt evtl. nur teilweise gefüllt 
      }
      return task.getInfo();
    }
    //Suche nach dem Auftrag kann hier schon passieren, das muss nicht der OSMTaskConsumer machen
    OSMTask_Reschedule task = new OSMTask_Reschedule(orderId,force,this);
    task.searchOrder(osmCacheImpl);
    if( task.canBeStarted() ) { //vom Suchergebnis ist abhängig, ob ein Task sinnvoll gestartet werden kann
      putInExternalQueue( task );
      try {
        task.await(); //auf Bearbeitung des Tasks warten
      } catch( InterruptedException e ) {
        //ignorieren, Info ist dann halt evtl. nur teilweise gefüllt 
      }
    }
    return task.getInfo();
  }

  public AllOrdersList getAllOrdersList() {
    return allOrders;
  }
  
}



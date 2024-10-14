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
package com.gip.xyna.xdev.xfractmod.xmdm;



import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_SocketCouldNotBeBoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.ReceiveControlAlgorithm;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponsibility;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_TriggerClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;



/**
 * oberklasse für trigger. eine solche klasse muss beim processing registriert werden. bei
 * der registration wird start() aufgerufen. 
 * TODO besserer Name wäre TriggerInstance
 */
public abstract class EventListener<I extends TriggerConnection, J extends StartParameter> { 
  //achtung, reihenfolge der generischen parameter ist wichtig, darauf wird in xynaactivationtrigger zugegriffen
  //(Class<StartParameter> csp = (Class<StartParameter>) ts[1]) 

  private static final Logger logger = CentralFactoryLogging.getLogger(EventListener.class);

  public static final String KEY_CONNECTION = "xynaConnection";

  private final ReentrantReadWriteLock l = new ReentrantReadWriteLock();
  private final Lock rl = l.readLock();
  private final Lock wl = l.writeLock();
  private ConnectionFilterInstance<?>[] filters = new ConnectionFilterInstance[0];
  private List<ConnectionFilterInstance<?>> outdatedFilterVersion = new ArrayList<ConnectionFilterInstance<?>>();
  private TriggerInstanceIdentification triggerInstanceId;
  private ReceiveControlAlgorithm receiveControlAlgorithm = new ReceiveControlAlgorithm();

  private ConcurrentMap<Long, Map<String, Class<ConnectionFilter<TriggerConnection>>>> cachedClassesMap =
      new ConcurrentHashMap<Long, Map<String, Class<ConnectionFilter<TriggerConnection>>>>();

  

  
  /**
   * TODO dies wäre besser ein Konstruktor
   */
  public void init(TriggerInstanceIdentification triggerInstanceId ) {
    this.triggerInstanceId = triggerInstanceId;
  }

  /**
   * initialisiert den trigger, so dass {@link #receive()} aufgerufen werden kann. 
   */
  public abstract void start(J startParameter) throws XACT_TriggerCouldNotBeStartedException;
  
  /**
   * Liefert eine Beschreibung der Triggerklasse.
   */
  public abstract String getClassDescription();

  /**
   * wartet blockierend, bis ein event empfangen wird.
   * gibt nur null zurück, wenn der trigger angehalten wird.
   */
  protected abstract I receive();

  /**
   * soll dazu führen, dass {@link #receive()} null zurück gibt. 
   */
  public abstract void stop() throws XACT_TriggerCouldNotBeStoppedException;

  /**
   * called if no filter has accepted the event
   */
  protected abstract void onNoFilterFound(I con);

  /**
   * called if no thread could be started to process filters.
   */
  protected abstract void onProcessingRejected(String cause, I con);


  public void addOutdatedFilterVersion(ConnectionFilterInstance<ConnectionFilter<I>> cf) {
    wl.lock();
    try {
      long revisionOfOutdatedFilter = ((ClassLoaderBase)cf.getCF().getClass().getClassLoader()).getRevision();
      
      Integer foundOldInstance = null;
      for (int i = 0; i < outdatedFilterVersion.size(); i++) {
        if (outdatedFilterVersion.get(i).equals(cf)) {
          long revision = ((ClassLoaderBase)outdatedFilterVersion.get(i).getCF().getClass().getClassLoader()).getRevision();
          if (revisionOfOutdatedFilter == revision) {
            foundOldInstance = i;
            break;
          }
        }
      }

      if (foundOldInstance != null) {
        outdatedFilterVersion.set(foundOldInstance, cf);
        if(logger.isDebugEnabled()) {
          logger.debug("Replaced outdated filter " + cf.getFilterName() + "(instance: " + cf.getInstanceName() +
                       ") with revision " + revisionOfOutdatedFilter + " for " + getClass().getName());
        }
      } else {
        outdatedFilterVersion.add(cf);
        Collections.sort(outdatedFilterVersion, new Comparator<ConnectionFilterInstance<?>>() {

          public int compare(ConnectionFilterInstance<?> o1, ConnectionFilterInstance<?> o2) {
            if(o1.getCF().getClass().getClassLoader() instanceof ClassLoaderBase) {
              ClassLoaderBase cl1 = (ClassLoaderBase)o1.getCF().getClass().getClassLoader();
              if(o2.getCF().getClass().getClassLoader() instanceof ClassLoaderBase) {
                ClassLoaderBase cl2 = (ClassLoaderBase)o2.getCF().getClass().getClassLoader();
                
                return cl1.getRevision().compareTo(cl2.getRevision()) * (-1);              
              }
            }
            return 1;
          }
        });
        if(logger.isDebugEnabled()) {
          logger.debug("Added outdated filter " + cf.getFilterName() + "(instance: " + cf.getInstanceName() +
                       ") with revision " + revisionOfOutdatedFilter + " to " + getClass().getName());
        }
      }
    } finally {
      wl.unlock();
    }
  }
  
  /**
   * entfernt und undeployed den outdatedfilter in der übergebenen revision
   */
  public void removeOutdatedFilter(String nameOfFilterInstance, long revision) {
    wl.lock();
    try {
      Iterator<ConnectionFilterInstance<?>> iter = outdatedFilterVersion.iterator();
      while(iter.hasNext()) {
        ConnectionFilterInstance<?> cfi = iter.next();
        if(nameOfFilterInstance.equals(cfi.getInstanceName())) {
          long revisionOfOutdatedFilter = ((ClassLoaderBase)cfi.getCF().getClass().getClassLoader()).getRevision();
          if(revision == revisionOfOutdatedFilter) {
            iter.remove();
            XynaActivationTrigger.callUndeploymentOfFilterInstance(cfi.getCF(), this);
            if(logger.isDebugEnabled()) {
              logger.debug("Removed outdated filter instance " + nameOfFilterInstance + " in revision " + revision + " from " + getClass().getName());
            }
          }
        }
      }
    } finally {
      wl.unlock();
    }
  }
  
  /**
   * Ersetzt filter, falls bereits vorhanden. wird beim filter-deployment (instanziierung) aufgerufen
   */
  public void addFilter(ConnectionFilterInstance<ConnectionFilter<I>> cf) {

    wl.lock();
    try {

      Integer foundOldInstance = null;
      for (int i = 0; i < filters.length; i++) {
        if (filters[i].equals(cf)) {
          foundOldInstance = i;
          break;
        }
      }

      if (foundOldInstance != null) {
        filters[foundOldInstance] = cf;
        if (logger.isDebugEnabled()) {
          logger.debug("replaced previous instance of ConnectionFilter " + cf + " for " + this);
        }
      } else {
        ConnectionFilterInstance<?>[] newFilters = new ConnectionFilterInstance[filters.length + 1];
        System.arraycopy(filters, 0, newFilters, 0, filters.length);
        newFilters[filters.length] = cf;
        filters = newFilters;
        if (logger.isDebugEnabled()) {
          logger.debug("added ConnectionFilter " + cf + " to " + this);
        }
      }

    } finally {
      wl.unlock();
    }

  }
  
  public void resetFilterCache() {
    cachedClassesMap.clear();
  }

  /**
   * Entfernt eine Filterinstanz mit ihren zugehörigen OutdatedFilterinstanzen
   * @param cf
   */
  public void removeFilter(ConnectionFilterInstance<ConnectionFilter<I>> cf) {
    wl.lock();
    try {
      for (int i = filters.length - 1; i > -1; i--) {
        if (filters[i].equals(cf)) {
          ConnectionFilterInstance<?>[] newFilters = new ConnectionFilterInstance[filters.length - 1];
          if (i > 0) {// length to copy == 0
            System.arraycopy(filters, 0, newFilters, 0, i);
          }
          if (i < filters.length - 1) {// length to copy == 0
            System.arraycopy(filters, i + 1, newFilters, i, filters.length - 1 - i);
          }
          filters = newFilters;
          resetFilterCache();
          if (logger.isDebugEnabled()) {
            logger.debug("removed ConnectionFilter " + cf + " to " + this);
          }
        }
      }

      //alle zugehörigen outdated filter entfernen
      Iterator<ConnectionFilterInstance<?>> iter = outdatedFilterVersion.iterator();
      while (iter.hasNext()) {
        ConnectionFilterInstance<?> cfi = iter.next();
        if (cf.getInstanceName().equals(cfi.getInstanceName())) {
          long revisionOfOutdatedFilter = ((ClassLoaderBase) cfi.getCF().getClass().getClassLoader()).getRevision();
          iter.remove();
          if (logger.isDebugEnabled()) {
            logger.debug("Removed outdated filter instance " + cf.getInstanceName() + " in revision " + revisionOfOutdatedFilter + " from "
                + getClass().getName());
          }
        }
      }
    } finally {
      wl.unlock();
    }
    
  }


  private Map<String, Class<ConnectionFilter<TriggerConnection>>> getOrCreateCachedClassesMap(Long revision) {
    Map<String, Class<ConnectionFilter<TriggerConnection>>> result = cachedClassesMap.get(revision);
    if (result == null) {
      result = new ConcurrentHashMap<String, Class<ConnectionFilter<TriggerConnection>>>();
      cachedClassesMap.put(revision, result);
    }
    return result;
  }


  private Class<ConnectionFilter<TriggerConnection>> getConnectionFilterClass(ConnectionFilterInstance<?> cfi,
                                                                              Long parentRevision) {
    String className = cfi.getCF().getClass().getName();
    ClassLoaderBase clb = (ClassLoaderBase) cfi.getCF().getClass().getClassLoader();

    Map<String, Class<ConnectionFilter<TriggerConnection>>> cache = getOrCreateCachedClassesMap(clb.getRevision());

    Class<ConnectionFilter<TriggerConnection>> c = cache.get(className);
    if (c == null) {
      try {
        ClassLoaderDispatcher classLoaderDispatcher =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
        if (clb.getRevision().equals(parentRevision)
            || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
                .isDependency(clb.getRevision(), parentRevision)) {
          c = classLoaderDispatcher.loadFilterClass(className, className, getClass().getName(), cfi.getSharedLibs(), clb.getRevision());
        } else {
          c =
              classLoaderDispatcher.loadFilterClass(className, className, getClass().getName(), cfi.getSharedLibs(), clb.getRevision(),
                                                    parentRevision);
        }
      } catch (XFMG_TriggerClassLoaderNotFoundException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
        //das muss beim deployment überprüft werden, hier ist die falsche stelle
        throw new RuntimeException(e);
      }
      cache.put(className, c);
    }
    return c;
  }


  public final void processFilters(I tc) {
    Long triggerRevision = ((ClassLoaderBase) getClass().getClassLoader()).getRevision();
    if (logger.isTraceEnabled()) {
      logger.trace(Thread.currentThread().getName() + " getting " + getClass().getSimpleName() + " readlock");
    }
    long t = System.nanoTime();

    boolean closeTC = true;
    try {
      ResponsibleFilter responsibleFilter = findResponsibleFilter(Arrays.asList(filters), triggerRevision, tc, false );
    
      switch( responsibleFilter.getResult() ) {
        case FilterFound:
          XynaOrderServerExtension xynaOrder = responsibleFilter.getXynaOrder();
          CentralFactoryLogging.logOrderTiming(xynaOrder.getId(), "filter started", t);
          CentralFactoryLogging.logOrderTiming(xynaOrder.getId(), "filter returned");
          OrderContextServerExtension ctx = xynaOrder.getOrderContext();
          onFilterFound(xynaOrder, ctx, tc);
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(xynaOrder, responsibleFilter.getConnectionFilter(), ctx);
          closeTC = false;
          break;
        case FilterFound_Without_XynaOrder:
          onFilterFoundWithoutXynaOrder(tc);
          break;
        case NoFilterFound:
          onNoFilterFound(tc);
          break;
        case FilterFailed : //kein Filter gefunden, der tc akzeptiert, aber mindestens ein Filter, der Exceptions wirft
          if (logger.isDebugEnabled()) {
            logger.debug("Filter " + responsibleFilter.getConnectionFilter().getClassDescription() + "@rev"
                + responsibleFilter.getConnectionFilter().getRevision() + " failed.", responsibleFilter.getCause());
          }
          onFilterFailed(tc, responsibleFilter.getConnectionFilter(), responsibleFilter.getCause() );
          break;
      }
    } finally {
      if( closeTC ) {
        tc.close();
      }
    }
  }


  private static class ResponsibleFilter {
    
    public enum Result {
      FilterFound,
      FilterFound_Without_XynaOrder,
      NoFilterFound,
      FilterFailed;
    }
    
    private Result result;
    private XynaOrderServerExtension xynaOrder;
    private ConnectionFilter<?> connectionFilter;
    private Throwable cause;
    
    public static ResponsibleFilter filterFound(ConnectionFilter<?> cf, XynaOrderServerExtension xose) {
      ResponsibleFilter rf = new ResponsibleFilter();
      rf.result = xose != null ? Result.FilterFound : Result.FilterFound_Without_XynaOrder;
      rf.connectionFilter = cf;
      rf.xynaOrder = xose;
      return rf;
    }
    public static ResponsibleFilter noFilterFound() {
      ResponsibleFilter rf = new ResponsibleFilter();
      rf.result = Result.NoFilterFound;
      return rf;
    }
    public static ResponsibleFilter filterFailed(ConnectionFilter<?> cf, Throwable cause) {
      ResponsibleFilter rf = new ResponsibleFilter();
      rf.result = Result.FilterFailed;
      rf.connectionFilter = cf;
      rf.cause = cause;
      return rf;
    }

    public ConnectionFilter<?> getConnectionFilter() {
      return connectionFilter;
    }
    public XynaOrderServerExtension getXynaOrder() {
      return xynaOrder;
    }
    public Result getResult() {
      return result;
    }
    public Throwable getCause() {
      return cause;
    }
    
  }

  /**
   * Ermitteln des zuständigen Filters
   * @param triggerRevision 
   * @param tc 
   * @return
   */
  @SuppressWarnings("unchecked")
  private ResponsibleFilter findResponsibleFilter(List<ConnectionFilterInstance<?>> cfis, Long triggerRevision, I tc, boolean isOutdated) {
    rl.lock();
    try {
      for (ConnectionFilterInstance<?> cfi : cfis) {
        Class<ConnectionFilter<TriggerConnection>> c = getConnectionFilterClass(cfi, triggerRevision);
        ConnectionFilter<I> cf = null;
        FilterResponse filterResponse = null;
        XynaOrderServerExtension xose = null; 
        try {
          cf = (ConnectionFilter<I>) c.getConstructor().newInstance();
          cf.setRevision(cfi.getRevision());
          Long lastDeploymentId = DeploymentManagement.getInstance().getLatestDeploymentId(); //direkt vor createXynaOrder rufen, dann ist 
          //Wahrscheinlichkeit hoch, dass deploymentId mit den in XynaOrder eingetragenen Daten übereinstimmt
          //TODO das geht doch bestimmt besser!
          filterResponse = cf.createXynaOrder(tc, cfi.getConfiguration());
          if( filterResponse != null && filterResponse.getResponsibility() == FilterResponsibility.RESPONSIBLE ) {
            //Fertigstellen der XynaOrderServerExtension
            xose = buildXynaOrderServerExtension( filterResponse.getOrder(), cf, tc, triggerRevision, lastDeploymentId );
          }
        } catch (Throwable t ) {
          Department.handleThrowable(t);
          ResponsibleFilter rf = handleThrowable(t, cf, filterResponse, tc );
          if( rf.getResult() == ResponsibleFilter.Result.NoFilterFound ) {
            continue; //weitersuchen, evtl. findet sich noch besserer Filter
          } else {
            return rf;
          }
        }

        switch( filterResponse.getResponsibility() ) {
          case RESPONSIBLE:
            return ResponsibleFilter.filterFound(cf, xose);
          case RESPONSIBLE_WITHOUT_XYNAORDER:
            return ResponsibleFilter.filterFound(cf,null);
          case NOT_RESPONSIBLE:
            break; //nächsten Filter probieren
          case RESPONSIBLE_BUT_TOO_NEW:
            if( ! isOutdated ) {
              List<ConnectionFilterInstance<?>> outdated = getOutdatedFilters(cfi.getInstanceName());
              ResponsibleFilter rf = findResponsibleFilter(outdated,triggerRevision,tc, true);
              if( rf.getResult() == ResponsibleFilter.Result.NoFilterFound ) {
                break; //weitersuchen und nächsten Filter probieren
              } else {
                return rf;
              }
            } else {
              break; //es werden bereits outdated-Filter betrachtet, daher weitersuchen
            }
          default:
            logger.error("Unexpected FilterResponse "+ filterResponse.getResponsibility() );
            //weitersuchen
        }
      }
      return ResponsibleFilter.noFilterFound();
    } finally {
      rl.unlock();
    }
  }

  /**
   * @param order
   * @param lastDeploymentId 
   * @param triggerRevision 
   * @return
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY 
   */
  private XynaOrderServerExtension buildXynaOrderServerExtension(XynaOrder xo, ConnectionFilter<I> connectionFilter, I tc, Long triggerRevision, Long lastDeploymentId ) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    // embed xo into it's ServerExtension
    XynaOrderServerExtension xose = new XynaOrderServerExtension( xo );
    xose.setIdOfLatestDeploymentKnownToOrder(lastDeploymentId);
    
    Long parentRevision;
    Long orderRevision;
    RuntimeContext orderRuntimeContext;
    RuntimeContext dkRuntimeContext = xose.getDestinationKey().unsafeGetRuntimeContext();  
    boolean resetToTriggerRevision = dkRuntimeContext == null;
    if (resetToTriggerRevision) {
      parentRevision = triggerRevision;
      orderRevision = connectionFilter.getRevision();
      try {
        orderRuntimeContext = revMgmt.getRuntimeContext(orderRevision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.error("Could not find application name and version name for revision " + orderRevision, e);
        throw e;
      }
    } else {
      //wenn explizit runntime Context angegeben, dann diesen verwenden.
      //beim serialisieren muss der richtige RuntimeContext gesetzt werden.
      //siehe: com.gip.xyna.xprc.xpce.OrderContext.writeObject()
      parentRevision = revMgmt.getRevision(dkRuntimeContext);
      orderRevision = parentRevision;
      orderRuntimeContext = dkRuntimeContext;
    }
            
    xose.setParentRevision(parentRevision);
    xose.setRevision(orderRevision);
    xose.getDestinationKey().setRuntimeContext(orderRuntimeContext);
    
    
    OrderContextServerExtension ctx = null;
    if( xo.getOrderContext() != null ) {
      ctx = OrderContextServerExtension.createOrderContextFromExisting(xo.getOrderContext(), xose);
      if( logger.isDebugEnabled() ) {
        logger.debug( "Existing OrderContext will be used");
      }
    }
    if( ctx == null ) {
      ctx = new OrderContextServerExtension(xose);
    }
    if (xo.getTransientCreationRole() != null) {
      ctx.set(OrderContextServerExtension.CREATION_ROLE_KEY, xo.getTransientCreationRole());
    }
    
    ctx.set(KEY_CONNECTION, tc);
    xose.setOrderContext(ctx);
    return xose;
  }

  /**
   * @param t
   * @param filterResponse 
   * @param cf 
   * @param tc 
   * @return 
   */
  private ResponsibleFilter handleThrowable(Throwable t, ConnectionFilter<I> cf, FilterResponse filterResponse, I tc) {
    boolean isXynaException = t instanceof XynaException;
    Level level = isXynaException ? Level.TRACE : Level.DEBUG;
    if( logger.isEnabledFor(level) ) {
      logger.log(level, "Exception while calling filters: " + t.getMessage(), t);
    }
    
    if( isXynaException ) {
      //Dass der Filter eine XynaException geworfen hat, ist ok. Dies ist sogar üblich, 
      //wenn ein Filter erkennt, dass die Eingangsnachricht falsch ist.
      //In diesem Fall soll einfach OnError gerufen werden
      try {
        cf.onError( new XynaException[]{(XynaException)t}, tc);
        //Filter hat sich als verantwortlich gezeigt, da er die Fehlermeldung erzeugt und behandelt hat
        return ResponsibleFilter.filterFound(cf, null);
      } catch( Throwable t2 ) {
        Department.handleThrowable(t);
        logger.debug( "Exception while calling onError: " + t2.getMessage(), t2);
        //Das hätte nun nicht mehr passieren dürfen. Evtl. hat der Filter doch eine Macke
        return ResponsibleFilter.filterFailed(cf, t2);
      }
    } else {
      //Filter hätte diese Exception nicht werden dürfen. Filter hat anscheinend eine Macke
      return ResponsibleFilter.filterFailed(cf, t);
    }
  }
 
  /**
   * @param instanceName
   * @return
   */
  private List<ConnectionFilterInstance<?>> getOutdatedFilters(String instanceName) {
    List<ConnectionFilterInstance<?>> ret = new ArrayList<ConnectionFilterInstance<?>>();
    for(ConnectionFilterInstance<?> cfiOutdated : outdatedFilterVersion) {
      if(cfiOutdated.getInstanceName().equals(instanceName) ) {
        ret.add(cfiOutdated);
      }
    }
    return ret;
  }  


  public ConnectionFilterInstance<?>[] getAllFilters() {
    return filters;
  }

  public List<ConnectionFilterInstance<?>> getAllOutdatedFilters() {
    return outdatedFilterVersion;
  }


  protected static interface BindTask {

    public void execute() throws IOException, XynaException;
  }


  /**
   * wiederholt bindtask solange, bis keine bindexception auftritt oder numberofretries ueberschritten ist. wartet
   * zwischen jedem retry die angegebene zeit
   * 
   * @param bt
   * @param numberOfRetries
   * @param retryIntervalMilli in milliseconds
   * @throws IOException falls das bindtask eine ioexception wirft, die keine bindexception ist
   * @throws XACT_SocketCouldNotBeBoundException falls nach der angegebenen anzahl von retries immer noch bindexceptions auftreten
   * @throws RuntimeException falls Thread interrupted wird
   * @throws XynaException falls Bindtask exception wirft
   */
  protected void retryBindException(BindTask bt, int numberOfRetries, int retryIntervalMilli) throws IOException,
                  XynaException {

    int tries = 0;
    while (tries < numberOfRetries) {
      tries++;
      try {
        bt.execute();
        tries = numberOfRetries + 1;
      } catch (BindException be) {
        // Bei einem trigger-redeploy ist der port evtl vom betriebssystem her noch nicht wieder verfuegbar.
        if (tries >= numberOfRetries) {
          throw new XACT_SocketCouldNotBeBoundException(numberOfRetries, be.getMessage(), be);
        }
        try {
          Thread.sleep(retryIntervalMilli);
        } catch (InterruptedException ie) {
          throw new RuntimeException("Unexpected thread interruption", ie);
        }
      }
    }

  }
  
  

  //TODO statistics: rejected events wegen maxReceive, anderweitig rejected events, max-current-cnt, accepted events,
  //                 filter-statistics (which filter accepted events how often?)

  

  
  /**
   * leitet weiter zu {@link #receive()}, führt überprüfung von {@link #getMaxReceivesInParallel()} durch.
   */
  public final I receiveNext() {
    if( ! receiveControlAlgorithm.canReceive() ) {
      //EventListenerThread wiederholt Aufruf
      return null;
    }
    
    //Receive durchführen
    I tc = receive(); //kann länger dauern
    if( tc == null ) {
      return null;
    }
    tc.setTrigger(this);
    
    String rejection = receiveControlAlgorithm.notifyReceive();
    if( rejection != null ) {
      onProcessingRejectedProxy(rejection, tc);
      return null;
    } else {
      return tc;
    }
  }
  
  void decrementActiveEvents() {
    receiveControlAlgorithm.decrementActiveEvents();
  }
  
  
  /**
   * leitet weiter zur vom trigger implementierten {@link #onProcessingRejected(String, TriggerConnection)} Methode
   */
  public final void onProcessingRejectedProxy(String cause, I tc) {
    try {
      onProcessingRejected(cause, tc);
    } catch (Throwable t) {
      //FIXME warn würde das log vollspammen. evtl sollte man sich hier merken, ob eine fehlermeldung bereits als warn ausgegeben wurde, und dies dann
      //nur in längeren zeitabständen tun.
      logger.debug("problem rejecting event", t);
    } finally {
      tc.close();
    }
  }
  
  
  /**
   * ermöglicht es dem Trigger auf angenommene Aufträge zu reagieren 
   * nicht abstract aufgrund von abwärts kompaktibilität...wait all your triggers are belong to us?
   */
  protected void onFilterFound(XynaOrderServerExtension xo, OrderContextServerExtension ctx, I tc) {
  }
  
  /**
   * ermöglicht es dem Trigger zu reagieren, wenn Filter verantwortlich ist, aber keine XynaOrder startet
   * nicht abstract aufgrund von Abwärtskompatibilität
   * @param tc
   */
  protected void onFilterFoundWithoutXynaOrder(I tc) {
  }

  /**
   * ermöglicht es dem Trigger zu reagieren, wenn Filter mit unerwarteten Exceptions fehlschlägt
   * nicht abstract aufgrund von Abwärtskompatibilität
   * @param tc
   * @param connectionFilter
   * @param cause
   */
  protected void onFilterFailed(I tc, ConnectionFilter<?> connectionFilter, Throwable cause) {
    tc.rollback();
  }
  
  /**
   * @return
   */
  public TriggerInstanceIdentification getTriggerInstanceIdentification() {
    return triggerInstanceId;
  }

  /**
   * @return the receiveControlAlgorithm
   */
  public ReceiveControlAlgorithm getReceiveControlAlgorithm() {
    return receiveControlAlgorithm;
  }
  
  /**
   * @return 
   * @deprecated use getReceiveControlAlgorithm().getCurrentActiveEvents();
   */
  @Deprecated
  protected Long getCntCurrentActiveEventsReadOnly() {
    return receiveControlAlgorithm.getCurrentActiveEvents();
  }


  /**
   * @return
   * @deprecated use getReceiveControlAlgorithm().getMaxReceivesInParallel();
   */
  @Deprecated
  protected long getMaxReceivesInParallel() {
    return receiveControlAlgorithm.getMaxReceivesInParallel();
  }

  
}

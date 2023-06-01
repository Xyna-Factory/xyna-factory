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

package com.gip.xyna.xprc;



import java.io.IOException;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRuleCollection;
import com.gip.xyna.xprc.xsched.SchedulingData;


public class XynaOrder implements Serializable {

  protected static final Logger logger = CentralFactoryLogging.getLogger(XynaOrder.class);

  private static final long serialVersionUID = 1091065419080097968L;
  private static final Integer EMPTYRUNNABLES = Integer.valueOf(0);

  /*
   * Basic information
   */
  protected long id; // TODO can't and shouldn't this be final?
  
  /** @deprecated nun in schedulingData */
  @Deprecated
  private int priority = -1;
  private transient GeneralXynaObject inputPayload;
  private transient GeneralXynaObject outputPayload;

  private String custom0;
  private String custom1;
  private String custom2;
  private String custom3;
  private String sessionId;

  protected long entranceTimestamp;
  /** @deprecated nun in schedulingData */
  @Deprecated
  protected long earliestStartTimestamp;

  /*
   * Scheduling information
   */
  protected SchedulingData schedulingData;

  /*
   * Dispatching information
   */
  private DestinationKey destinationKey;
  private DestinationValue executionDestination;

  /*
   * Timeout and cancel information
   */
  /** @deprecated nun in schedulingData */
  @Deprecated
  protected Long schedulingTimeout;
  protected volatile boolean timedOut;
  protected volatile boolean cancelled;

  /*
   * Misc
   */
  protected OrderContext ctx;
  protected transient List<XynaException> errors;
  protected transient ResponseListener responseListener;
  protected Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
  protected Long parentRevision = null;
  /**
   * Sets the level of monitoring that is applied to this xyna order
   * 
   * @see com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes
   */
  @Column(name="monitoringCode")
  protected Integer monitoringCode;
  
  /*
   * Predecessors and successors information 
   */
  @Column(name="seriesInformation", type=ColumnType.BLOBBED_JAVAOBJECT)
  private SeriesInformation seriesInformation; 
    //seriesInformation sollte transient werden, das geht aber erst, wenn die Daten nicht mehr gelesen werden 
    //müssen. Dies ist derzeit noch der Fall beim Lesen aus OrderInstanceBackup
  private boolean inOrderSeries = false;
  private String seriesCorrelationId = null;
  
  
  @Column(name = "runnablesForFilterAccess", type = ColumnType.BLOBBED_JAVAOBJECT)
  private transient volatile ConcurrentMap<String, RunnableForFilterAccess> runnablesForFilterAccess;
  
  private ExecutionTimeoutConfiguration orderExecutionTimeout; //absolut oder relativ von einstellungszeitpunkt ab
  private ExecutionTimeoutConfiguration workflowExecutionTimeout; //relativ ab erstem schedulingzeitpunkt
  
  
  /**
   * Parameter Vererbungsregeln
   */
  private Map<ParameterType, InheritanceRuleCollection> parameterInheritanceRules;
  private transient Role transientCreationRole;
  
  protected XynaOrder() {
    this.entranceTimestamp = System.currentTimeMillis();
    this.schedulingData = new SchedulingData(entranceTimestamp);
    
    parameterInheritanceRules = ParameterType.createInheritanceRuleMap();
  }

  // this needs to be contained for legacy reasons
  public XynaOrder(DestinationKey dk, XynaObject... payload) {
    this(dk, (GeneralXynaObject[]) payload);
  }
  
  protected static IDGenerator idgen = XynaFactory.getInstance().getIDGenerator();
  
  public static void setIDGenerator(IDGenerator gen) {
    idgen = gen;
  }

  public XynaOrder(DestinationKey dk, GeneralXynaObject... payload) {
    this(dk, idgen.getUniqueId(), payload);
  }
  
  protected XynaOrder(DestinationKey dk, long orderId, GeneralXynaObject... payload) {
    if (dk == null || dk.getOrderType() == null) {
      throw new IllegalArgumentException("Ordertype may not be null.");
    }
    this.destinationKey = dk;
    if (payload == null) {
      this.inputPayload = new Container();
    } else if (payload.length != 1) {
      this.inputPayload = new Container(payload);
    } else {
      this.inputPayload = payload[0];
    }
    this.entranceTimestamp = System.currentTimeMillis();
    this.id = orderId;
    this.schedulingData = new SchedulingData(entranceTimestamp);
    
    parameterInheritanceRules = ParameterType.createInheritanceRuleMap();
  }


  /**
   * 
   * @param xo
   * @deprecated Use only in XynaOrderServerExtension. Please substitute with
   *  XynaOrder.copyOf(xo) or XynaOrder.copyOfWithNewId(xo)
   */
  @Deprecated
  public XynaOrder(XynaOrder xo) {
    this(xo.getDestinationKey(), xo.getInputPayload());
    this.schedulingData = xo.getSchedulingData();
    this.setOrderExecutionTimeout(xo.getOrderExecutionTimeout());
    this.setWorkflowExecutionTimeout(xo.getWorkflowExecutionTimeout());
  }

  /**
   * Copy Constructor
   * Kopiert alle Felder flach!
   * Hauptzweck sollte Umwandlung von XynaOrder in XynaOrderServerExtension sein, daher ist flache Kopie das sinnvollste
   * @param xo
   * @param dummy Zur Unterscheidung vom deprecated XynaOrder(XynaOrder xo)
   */
  protected XynaOrder(XynaOrder xo, boolean dummy ) { 
    this.id = xo.id;
    this.inputPayload = xo.inputPayload;
    this.outputPayload = xo.outputPayload;
    this.schedulingData = xo.schedulingData;

    this.custom0 = xo.custom0;
    this.custom1 = xo.custom1;
    this.custom2 = xo.custom2;
    this.custom3 = xo.custom3;
    this.sessionId = xo.sessionId;

    this.entranceTimestamp = xo.entranceTimestamp;
    
    this.destinationKey = xo.destinationKey;
    this.executionDestination = xo.executionDestination;

    this.timedOut = xo.timedOut;
    this.cancelled = xo.cancelled;
    
    this.ctx = xo.ctx;
    this.errors = xo.errors;
    this.responseListener = xo.responseListener;
    this.revision = xo.revision;
    this.parentRevision = xo.parentRevision;

    this.monitoringCode = xo.monitoringCode;
    
    this.seriesInformation = xo.seriesInformation;
    this.inOrderSeries = xo.inOrderSeries;
    this.seriesCorrelationId = xo.seriesCorrelationId;
    
    this.runnablesForFilterAccess = xo.runnablesForFilterAccess;
    this.orderExecutionTimeout = xo.orderExecutionTimeout;
    this.workflowExecutionTimeout = xo.workflowExecutionTimeout;
    
    this.parameterInheritanceRules = xo.parameterInheritanceRules;
  }
  
  /**
   * Kopiert alle Felder (die meisten flach)
   * Tiefe Kopien von errors, seriesInformation, schedulingData
   * @param xo
   * @return
   */
  public static XynaOrder copyOf( XynaOrder xo ) {
    XynaOrder copy = new XynaOrder(xo,true);
    copy.errors = copyOfList( xo.errors );
    copy.seriesInformation = SeriesInformation.copyOfWithNewParent( xo.seriesInformation, copy );
    copy.schedulingData = SchedulingData.copyOf( xo.schedulingData );
    copy.schedulingData.setEntranceTimestamp(copy.entranceTimestamp);
    return copy;
  }
  
  /**
   * Kopiert alle Felder (die meisten flach), generiert id neu
   * Tiefe Kopien von errors, seriesInformation, schedulingData
   * @param xo
   * @return
   */
  public static XynaOrder copyOfWithNewId( XynaOrder xo ) {
    XynaOrder copy = copyOf(xo);
    copy.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    return copy;
  }
  
  private static <T> ArrayList<T> copyOfList(List<T> list) {
    if( list == null ) {
     return null;
    }
    ArrayList<T> copy = new ArrayList<T>();
    copy.addAll(list);
    return copy;
  }

  
  

  protected XynaOrder(XynaOrderCreationParameter xocp, long orderId) {
    this(xocp.getDestinationKey(), orderId, xocp.getInputPayload());

    if (xocp.getPriority() > 0) {
      schedulingData.setPriority(xocp.getPriority());
    }
    this.sessionId = xocp.getSessionId();
    setSeriesInformation( xocp.getSeriesInformation() );

    if (xocp.getCustomStringContainer() != null) {
      this.custom0 = xocp.getCustom0();
      this.custom1 = xocp.getCustom1();
      this.custom2 = xocp.getCustom2();
      this.custom3 = xocp.getCustom3();
    }

    if (xocp.getAllRunnablesForFilterAccess() != null) {
      for (Entry<String, RunnableForFilterAccess> e : xocp.getAllRunnablesForFilterAccess().entrySet()) {
        addRunnableForFilterAccess(e.getKey(), e.getValue());
      }
    }
    
    this.orderExecutionTimeout = xocp.getOrderExecutionTimeoutConfiguration();
    this.workflowExecutionTimeout = xocp.getWorkflowExecutionTimeoutConfiguration();
    
    if( xocp.getTimeConstraint() != null ) {
      schedulingData.setTimeConstraint( xocp.getTimeConstraint() );
    } else {
      Long schedTimeout;
      if(xocp.getRelativeSchedulingTimeout() != null) {
        schedTimeout = System.currentTimeMillis() + xocp.getRelativeSchedulingTimeout();
      } else {
        schedTimeout = xocp.getAbsoluteSchedulingTimeout();
      }
      schedulingData.setTimeConstraint( SchedulingData.legacyTimeConstraintFor(entranceTimestamp, earliestStartTimestamp, schedTimeout) );
    }
    monitoringCode = xocp.getMonitoringLevel();
    
    if (xocp.getParameterInheritanceRules() != null) {
      for (Entry<ParameterType, InheritanceRuleCollection> e : xocp.getParameterInheritanceRules().entrySet()) {
        for (InheritanceRule rule : e.getValue().getAllInheritanceRules()) {
          addParameterInheritanceRule(e.getKey(), rule);
        }
      }
    }
    transientCreationRole = xocp.getTransientCreationRole();
  }
  

  public XynaOrder(XynaOrderCreationParameter xocp) {
    this(xocp, idgen.getUniqueId());
  }

  public long getId() {
    return id;
  } 

  public long getEntranceTimestamp() {
    return entranceTimestamp;
  }
  
  public long getEarliestStartTimestamp() {
    return schedulingData.getTimeConstraint().startTimestamp(entranceTimestamp);
  }

  public void setDestinationKey(DestinationKey destinationKey) {
    this.destinationKey = destinationKey;
  }


  public DestinationKey getDestinationKey() {
    return destinationKey;
  }


  public void setInputPayload(GeneralXynaObject payload) {
    this.inputPayload = payload;
  }


  public GeneralXynaObject getInputPayload() {
    return inputPayload;
  }


  public GeneralXynaObject getOutputPayload() {
    return outputPayload;
  }


  /**
   * gibt input payload zurück
   * @deprecated use getInputPayload() or getOutputPayload()
   */
  @Deprecated
  public GeneralXynaObject getPayload() {
    return inputPayload;
  }


  public OrderContext getOrderContext() {
    return ctx;
  }


  public ResponseListener getResponseListener() {
    return responseListener;
  }


  public void addException(XynaException e) {
    if (errors == null) {
      errors = new ArrayList<XynaException>();
    }
    synchronized ( errors ) {
      errors.add(e);
    }
  }


  public boolean hasError() {
    return errors != null && errors.size() > 0;
  }


  public XynaException[] getErrors() {
    if (hasError()) {
      synchronized ( errors ) {
        return errors.toArray(new XynaException[errors.size()]);
      }
    }
    return null;
  }


  /**
   * bedeutung der priorität siehe {@link Thread#MIN_PRIORITY}, {@link Thread#MAX_PRIORITY}
   * @return
   */
  public int getPriority() {
    return schedulingData.getPriority();
  }

  /**
   * bedeutung der priorität siehe {@link Thread#MIN_PRIORITY}, {@link Thread#MAX_PRIORITY}
   */
  public void setPriority(int i) {
    schedulingData.setPriority(i);
  }

  //FIXME in monitoringlevel umbenennen wenn mal abwärtskompatibilität nicht so wichtig ist
  public Integer getMonitoringCode() {
    return monitoringCode;
  }


  public void setExecutionDestination(DestinationValue executionDestination) {
    this.executionDestination = executionDestination;
  }


  public DestinationValue getExecutionDestination() {
    return executionDestination;
  }


  /**
   * absoluten zeitstempel setzen, bei Überschreitung dessen der auftrag fehlerhaft beendet wird FIXME: umbenennen in
   * setAbsoluteSchedulingTimeout, wenn wir mal auf abwärtskompatibilität nicht soviel wert legen.
   * @deprecated use setTimeConstraint( TimeConstraint.immediately().withSchedulingTimeout(5000) )
   */
  @Deprecated
  public void setSchedulingTimeout(Long schedulingTimeout) {
    schedulingData.setTimeConstraint( SchedulingData.legacyTimeConstraintFor(entranceTimestamp, earliestStartTimestamp, schedulingTimeout));
  }

  /**
   * absoluter zeitwert als timeout
   * @deprecated
   */
  @Deprecated
  public Long getSchedulingTimeout() {
    return schedulingData.getTimeConstraintData().getSchedulingTimeout();
  }


  /**
   * wird vom SchedulerMaintenance Thread gesetzt
   */
  public boolean isTimedOut() {
    return timedOut;
  }


  /**
   * dieser auftrag soll status änderungs listener benachrichtigen. dieser parameter wird gesetzt, falls ein listener
   * für diesen ordertype gesetzt ist.
   */
  public boolean isCancelled() {
    return cancelled;
  }


  /**
   * vorgänger und nachfolger aufträge.
   */
  public void setSeriesInformation(SeriesInformation si) {
    if( si != null ) {
      this.seriesInformation = si;
      this.inOrderSeries = true;
      this.seriesCorrelationId = si.getCorrelationId();
    } else {
      this.seriesInformation = null;
    }
  }


  public SeriesInformation getSeriesInformation() {
    return seriesInformation;
  }
  
  public String getSeriesCorrelationId() {
    return seriesCorrelationId;
  }
 
  
  @Deprecated
  public ExecutionTimeoutConfiguration getExecutionTimeout() {
    return orderExecutionTimeout;
  }

  
  @Deprecated
  public void setExecutionTimeout(ExecutionTimeoutConfiguration executionTimeout) {
    this.orderExecutionTimeout = executionTimeout;
  }


  public ExecutionTimeoutConfiguration getOrderExecutionTimeout() {
    return orderExecutionTimeout;
  }

  
  public void setOrderExecutionTimeout(ExecutionTimeoutConfiguration orderExecutionTimeout) {
    this.orderExecutionTimeout = orderExecutionTimeout;
  }
  
  
  public ExecutionTimeoutConfiguration getWorkflowExecutionTimeout() {
    return workflowExecutionTimeout;
  }

  
  public void setWorkflowExecutionTimeout(ExecutionTimeoutConfiguration workflowExecutionTimeout) {
    this.workflowExecutionTimeout = workflowExecutionTimeout;
  }
  
  
  public boolean isInOrderSeries() {
    return inOrderSeries;
  }

  public String toString() {
    return super.toString() + " id=" + id + " ot=" + destinationKey;
  }


  public void setOutputPayload(GeneralXynaObject output) {
    outputPayload = output;
  }


  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    try {
      s.writeObject(SerializableClassloadedObject.useRevisionsIfReachable(responseListener, revision, parentRevision));
    } catch (IOException e) {
      throw (IOException) (new IOException("Could not serialize responseListener " + responseListener
          + ". It probably contains a non transient reference to a non serializable object.").initCause(e));
    }
    if (errors == null) {
      s.writeInt(-1);
    } else {
      synchronized( errors ) {
        s.writeInt(errors.size());
        for (XynaException e : errors) {
          s.writeObject(new SerializableClassloadedException(e));
        }
      }
    }
    s.writeObject(new SerializableClassloadedXynaObject(inputPayload));
    s.writeObject(new SerializableClassloadedXynaObject(outputPayload));
    if (runnablesForFilterAccess != null) {
      s.writeObject(Integer.valueOf(runnablesForFilterAccess.size()));
      for (Entry<String, RunnableForFilterAccess> e : runnablesForFilterAccess.entrySet()) {
        s.writeObject(e.getKey());
        s.writeObject(new SerializableClassloadedObject(e.getValue(), revision, parentRevision));
      }
    } else {
      s.writeObject(EMPTYRUNNABLES);
    }
  }
  
  protected long rootRevision;


  public static class RootRevisionHolder {

    long rootRevision;
    int stack = 0;


    public void set(long rootRevision) {
      stack++;
      this.rootRevision = rootRevision;
    }


    public boolean remove() {
      if (--stack == 0) {
        return true;
      }
      return false;
    }
  }


  public static final ThreadLocal<RootRevisionHolder> rootRevisionTL = new ThreadLocal<RootRevisionHolder>() {

    @Override
    protected RootRevisionHolder initialValue() {
      return new RootRevisionHolder();
    }

  };
  

  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();

    RootRevisionHolder rrh = rootRevisionTL.get();
    rrh.set(rootRevision);
    try {
      if (revision == null) {
        revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      }
      responseListener = (ResponseListener) ((SerializableClassloadedObject) s.readObject()).getObject();
      int errorsSize = s.readInt();
      if (errorsSize >= 0) {
        errors = new ArrayList<XynaException>();
        for (int i = 0; i < errorsSize; i++) {
          errors.add((XynaException) ((SerializableClassloadedException) s.readObject()).getThrowable());
        }
      }
      inputPayload = ((SerializableClassloadedXynaObject) s.readObject()).getXynaObject();
      outputPayload = ((SerializableClassloadedXynaObject) s.readObject()).getXynaObject();
      //referenz in seriesinformation reparieren.
      if (seriesInformation != null) {
        seriesInformation.changeParent(this);
        inOrderSeries = true;
        seriesCorrelationId = seriesInformation.getCorrelationId();
      }
      try {
        Object o = s.readObject();
        if (o != null) {
          int size = (Integer) o;
          if (size > 0) {
            runnablesForFilterAccess = new ConcurrentHashMap<String, RunnableForFilterAccess>();
          }
          for (int i = 0; i < size; i++) {
            String key = (String) s.readObject();
            RunnableForFilterAccess value = (RunnableForFilterAccess) ((SerializableClassloadedObject) s.readObject()).getObject();
            runnablesForFilterAccess.put(key, value);
          }
        }
      } catch (OptionalDataException e) {
        logger.debug("No runnables available.");
      }
      if (schedulingData == null) {
        //nur für abwärtskompatibilität. alte auftragsobjekte haben kein schedulingData
        schedulingData = new SchedulingData(entranceTimestamp);
        schedulingData.setTimeConstraint(SchedulingData.legacyTimeConstraintFor(entranceTimestamp, earliestStartTimestamp,
                                                                                schedulingTimeout));
        schedulingData.setPriority(priority);
      }

      if (parameterInheritanceRules == null) {
        parameterInheritanceRules = ParameterType.createInheritanceRuleMap();
      } else {
        //evtl. ist ein neuer ParameterType dazugekommen
        for (ParameterType type : ParameterType.values()) {
          if (!parameterInheritanceRules.containsKey(type)) {
            parameterInheritanceRules.put(type, new InheritanceRuleCollection());
          }
        }
      }

    } finally {
      if (rrh.remove()) {
        rootRevisionTL.remove();
      }
    }
  }


  /**
   * bei loglevel trace werden details ausgegeben
   */
  public void logDetailsOnTrace() {
    if (logger.isTraceEnabled() && seriesInformation != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("order ").append(toString());
      sb.append(seriesInformation);
      logger.trace(sb.toString());
    }
  }
  
  //customized fields which are saved in orderarchive-db for quick searches by a userdefined critera
  public void setCustom0(String value) {
    custom0 = value;
  }
  public void setCustom1(String value) {
    custom1 = value;
  }
  public void setCustom2(String value) {
    custom2 = value;
  }
  public void setCustom3(String value) {
    custom3 = value;
  }
  public void setSessionId(String value) {
    sessionId = value;
  }
  public String getCustom0() {
    return custom0;
  }
  public String getCustom1() {
    return custom1;
  }
  public String getCustom2() {
    return custom2;
  }
  public String getCustom3() {
    return custom3;
  }
  public String getSessionId() {
    return sessionId;
  }


  public RunnableForFilterAccess getRunnableForFilterAccess(String key) {
    return runnablesForFilterAccess == null ? null : runnablesForFilterAccess.get(key);
  }


  public void addRunnableForFilterAccess(String key, RunnableForFilterAccess runnable) {
    if (runnablesForFilterAccess == null) {
      synchronized (this) {
        if (runnablesForFilterAccess == null) {
          runnablesForFilterAccess = new ConcurrentHashMap<String, RunnableForFilterAccess>();
        }
      }
    }
    runnablesForFilterAccess.put(key, runnable);
  }


  ConcurrentMap<String, RunnableForFilterAccess> getAllRunnablesForFilterAccess() {
    return runnablesForFilterAccess;
  }
  
  /**
   * @param logOrderId
   * @return LoggingDiagContext, der in jeder Logausgabe des bearbeitenden Threads gesetzt ist
   */
  public String getLoggingDiagnosisContext(boolean logOrderId) {
    if( getOrderContext() == null || getOrderContext().getLoggingDiagnosisContext() == null ) {
      if( logOrderId ) {
        return Long.toString(getId());
      } else {
        return null; //kein Log4jDiagContext gewünscht
      }
    } else {
      if( logOrderId ) {
        return Long.toString(getId())+" "+getOrderContext().getLoggingDiagnosisContext();
      } else {
        return getOrderContext().getLoggingDiagnosisContext();
      }
    }
  }

  
  public SchedulingData getSchedulingData() {
    return schedulingData;
  }


  public InheritanceRuleCollection getParameterInheritanceRules(ParameterType type) {
    return parameterInheritanceRules.get(type);
  }
  

  public void addParameterInheritanceRule(ParameterType type, InheritanceRule inheritanceRule) {
    parameterInheritanceRules.get(type).add(inheritanceRule);
  }
  
  public Role getTransientCreationRole() {
    return transientCreationRole;
  }
}

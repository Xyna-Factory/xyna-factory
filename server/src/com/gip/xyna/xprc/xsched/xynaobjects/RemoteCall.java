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
package com.gip.xyna.xprc.xsched.xynaobjects;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.DispatchingTarget;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteCallHelper;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_TTLExpirationBeforeHandlerRegistration;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandler;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.AbortServiceStepEvent;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;
import com.gip.xyna.xprc.xsched.SchedulerBean;

@XynaObjectAnnotation(fqXmlName = "xprc.xpce.RemoteCall")
public class RemoteCall extends XynaObject implements ServiceStepEventHandler<AbortServiceStepEvent> {

  private static final long serialVersionUID = -37225163311L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RemoteCall.class);

  public static final String FQ_XML_NAME = "xprc.xpce.RemoteCall";
  public static final String FQ_CLASS_NAME = "com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall";
  public static final String ANY_INPUT_PAYLOAD_FQ_XML_NAME = "xprc.xpce.AnyInputPayload";

  @LabelAnnotation(label="Remote Order Id")
  private Long remoteOrderId;

  @LabelAnnotation(label="Start Time Stamp")
  private Long startTimeStamp;

  @LabelAnnotation(label="Factory Node")
  private String factoryNode;

  @LabelAnnotation(label="RCH")
  private transient RemoteCallHelper rch;

  @LabelAnnotation(label="Is Canceled")
  private transient volatile boolean isCanceled; 
  
  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> oldVersionsOfremoteOrderId;


  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> lazyInit_oldVersionsOfremoteOrderId() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = oldVersionsOfremoteOrderId;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfremoteOrderId;
        if (_vo == null) {
          oldVersionsOfremoteOrderId = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long>();
        }
      }
    }
    return _vo;
  }


  public Long getRemoteOrderId() {
    return remoteOrderId;
  }

  public Long versionedGetRemoteOrderId(long _version) {
    if (oldVersionsOfremoteOrderId == null) {
      return remoteOrderId;
    }
    Long _local = remoteOrderId;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Long> _ret = oldVersionsOfremoteOrderId.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setRemoteOrderId(Long remoteOrderId) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = lazyInit_oldVersionsOfremoteOrderId();
      synchronized (_vo) {
        _vo.add(this.remoteOrderId);
        this.remoteOrderId = remoteOrderId;
      }
      return;
    }
    this.remoteOrderId = remoteOrderId;
  }

  public void unversionedSetRemoteOrderId(Long remoteOrderId) {
    this.remoteOrderId = remoteOrderId;
  }

  public boolean supportsObjectVersioning() {
    if (!com.gip.xyna.XynaFactory.isFactoryServer()) {
      return false;
    }
    if (com.gip.xyna.xfmg.xods.configuration.XynaProperty.useVersioningConfig.get() == 4) {
      return true;
    } else {
      return false;
    }
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends RemoteCall, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(RemoteCall instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public RemoteCall instance() {
      return (RemoteCall) instance;
    }

    public _GEN_BUILDER_TYPE remoteOrderId(Long remoteOrderId) {
      this.instance.unversionedSetRemoteOrderId(remoteOrderId);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<RemoteCall, Builder> {
    public Builder() {
      super(new RemoteCall());
    }
    public Builder(RemoteCall instance) {
      super(instance);
    }
  }

  public Builder buildRemoteCall() {
    return new Builder(this);
  }

  public RemoteCall() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public RemoteCall(Long remoteOrderId) {
    this();
    this.remoteOrderId = remoteOrderId;
  }

  protected void fillVars(RemoteCall source, boolean deep) {
    this.remoteOrderId = source.remoteOrderId;
  }

  public RemoteCall clone() {
    return clone(true);
  }

  public RemoteCall clone(boolean deep) {
    RemoteCall cloned = new RemoteCall();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      RemoteCall xoc = (RemoteCall) xo;
      RemoteCall xoco = (RemoteCall) other.xo;
      if (!equal(xoc.versionedGetRemoteOrderId(this.version), xoco.versionedGetRemoteOrderId(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      RemoteCall xoc = (RemoteCall) xo;
      Long remoteOrderId = xoc.versionedGetRemoteOrderId(this.version);
      hash = hash * 31 + (remoteOrderId == null ? 0 : remoteOrderId.hashCode());
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }


  public void collectChanges(long start, long end, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers, java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOfremoteOrderId, start, end, datapoints);
  }

  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }

  public String toXml(String varName, boolean onlyContent, long version, com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
    StringBuilder xml = new StringBuilder();
    long objectId;
    if (!onlyContent) {
      long refId;
      if (cache != null) {
        ObjectVersion ov = new ObjectVersion(this, version, cache.changeSetsOfMembers);
        refId = cache.putIfAbsent(ov);
        if (refId > 0) {
          objectId = -2;
        } else {
          objectId = -refId;
          refId = -1;
        }
      } else {
        objectId = -1;
        refId = -1;
      }
      XMLHelper.beginType(xml, varName, "RemoteCall", "xprc.xpce", objectId, refId, RevisionManagement.getRevisionByClass(getClass()), cache);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "remoteOrderId", versionedGetRemoteOrderId(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"remoteOrderId"})));
  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    return new HashSet<String>(varNames);
  }


  public Set<String> getVariableNames() {
    return varNames;
  }

  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"remoteOrderId"};
    Object[] vars = new Object[]{this.remoteOrderId};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("remoteOrderId".equals(name)) {
      XOUtils.checkCastability(o, Long.class, "remoteOrderId");
      setRemoteOrderId((Long) o);
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }

  //TODO die modellierte schnittstelle passt hier nicht so richtig dazu. AnyInputPayload ist kein Serverinternes Objekt
  //     siehe dazu auch spezialbehandlung in servicedestinationauditdata, damit die audits stimmen
  public GeneralXynaObject initiateRemoteCallForDetachedCalls(XynaOrderServerExtension correlatedXynaOrder, RemoteCallInput rci,
                                                              GeneralXynaObject dispatchingParameter, GeneralXynaObject input)
      throws XynaException {
    return initiateRemoteCallForDetachedCalls_InternalImplementation(correlatedXynaOrder, rci, dispatchingParameter, input);
  }


  public GeneralXynaObject initiateRemoteCallForDetachedCalls_InternalSuperCallDestination(RemoteCall internalSuperCallDelegator,
                                                                                           XynaOrderServerExtension correlatedXynaOrder,
                                                                                           RemoteCallInput rci,
                                                                                           GeneralXynaObject dispatchingParameter,
                                                                                           GeneralXynaObject input)
      throws XynaException {
    return initiateRemoteCallForDetachedCalls_InternalImplementation(correlatedXynaOrder, rci, dispatchingParameter, input);
  }


  public GeneralXynaObject initiateRemoteCallForDetachedCalls_InternalImplementation(XynaOrderServerExtension correlatedXynaOrder,
                                                                                     RemoteCallInput rci,
                                                                                     GeneralXynaObject dispatchingParameter,
                                                                                     GeneralXynaObject input)
      throws XynaException {
    return initiateRemoteCall_InternalImplementation(correlatedXynaOrder, null, rci.getOrderType(), rci.getRuntimeContext(),
                                                     rci.getRemoteDestinationInstance(), dispatchingParameter, input);
  }


  protected GeneralXynaObject initiateRemoteCall_InternalSuperCallDestination(
      RemoteCall internalSuperCallDelegator, 
      XynaOrderServerExtension correlatedXynaOrder, 
      String laneId, String orderType, 
      RuntimeContext runtimeContext, String remoteDestination, 
      GeneralXynaObject dispatchingParameter, GeneralXynaObject input) throws XynaException {
    return initiateRemoteCall_InternalImplementation(correlatedXynaOrder, laneId, orderType, runtimeContext, remoteDestination, dispatchingParameter, input);
  }

  //FIXME die modellierte Schnittstelle passt hier nicht dazu. Sowohl AnyInputPayload als auch RuntimeContext sind nicht die gleichen klassen
  public GeneralXynaObject initiateRemoteCall(
      XynaOrderServerExtension correlatedXynaOrder, 
      String laneId, String orderType, 
      RuntimeContext runtimeContext, String remoteDestination, 
      GeneralXynaObject dispatchingParameter, GeneralXynaObject input) throws XynaException {
    return initiateRemoteCall_InternalImplementation(correlatedXynaOrder, laneId, orderType, runtimeContext, remoteDestination, dispatchingParameter, input);
  }

  private GeneralXynaObject initiateRemoteCall_InternalImplementation(
      XynaOrderServerExtension correlatedXynaOrder, 
      String laneId,
      String orderType, 
      RuntimeContext stubContext, 
      String remoteDestination, 
      GeneralXynaObject dispatchingParameter, 
      GeneralXynaObject input
      ) throws XynaException {
          logger.debug("Starting Order: " + correlatedXynaOrder.getId() + " @ " + System.currentTimeMillis());
    synchronized (this) {
      handleEventSource();
      checkCancelation();
      rch = new RemoteCallHelper(correlatedXynaOrder, laneId);
    }
    rch.setRemoteParameter( remoteDestination, orderType, stubContext, dispatchingParameter);
    
    DispatchingTarget defaultTarget = rch.getDispatchingTarget(factoryNode);
    
    if( getRemoteOrderId() == null ) {
      boolean resumed = false;
      if( startTimeStamp == null ) {
        //üblicher Start: StartTimeStamp und RemoteOrderId nicht gesetzt
        startTimeStamp = System.currentTimeMillis();
        resumed = false;
      } else {
        //verzögerter Start nach Resume
        resumed = true;
      }
      
      Pair<Long, DispatchingTarget> start = rch.startRemoteOrder(resumed, startTimeStamp, defaultTarget, input);
      synchronized (this) {
        checkCancelation();
      }
      //Start ist evtl. mit Fehler abgebrochen oder suspendiert.
      //Bei Suspendierung wird startRemoteOrder nach dem Resume nochmal gerufen
      
      //Nach erfolgreichem startRemoteOrder kann RemoteOrderId gesetzt werden
      this.setRemoteOrderId( start.getFirst() );
     
      if( start.getSecond() != null ) {
        defaultTarget = start.getSecond(); //evtl DispatchingTarget übernehmen, falls Fehler mit defaultTarget auftrat
      }
      
      factoryNode = defaultTarget.factoryNodeName;
    }
    
    if( getRemoteOrderId() != null ) {
      rch.awaitOrder(defaultTarget, startTimeStamp, remoteOrderId );
      synchronized (this) {
        checkCancelation();
      }
      //Await ist evtl. mit Fehler abgebrochen oder suspendiert.
      //Bei Suspendierung wird awaitOrder nach dem Resume nochmal gerufen
      GeneralXynaObject response = rch.getResult(defaultTarget, remoteOrderId);
      logger.debug("Order processed: " + correlatedXynaOrder.getId() + " @ " + System.currentTimeMillis());
      return response;
    } else {
      throw new IllegalStateException("RemoteOrderId should be set");
    }
  }
  
  
  private void handleEventSource() {
    ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();
    if (eventSource != null) {
      try {
        eventSource.listenOnAbortEvents(this);
      } catch (XPRC_TTLExpirationBeforeHandlerRegistration e) {
        isCanceled = true;
      }
    }
  }
  
  private void checkCancelation() {
    if (isCanceled) {
      throw new ProcessSuspendedException( new SuspensionCause_ShutDown() ); 
    }
  }

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }


  @Override
  public void handleServiceStepEvent(AbortServiceStepEvent serviceStepEvent) {
    synchronized (this) {
      if (!isCanceled) {
        isCanceled = true;
        if (rch != null) {
          rch.cancel();      
        }
      }
    }
  }

  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = RemoteCall.class.getDeclaredField(target_fieldname);
      if (foundField == null) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
      } else {
        foundField.setAccessible(true);
        fieldMap.put(target_fieldname, foundField);
        return foundField;
      }
    } catch (NoSuchFieldException e) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname, e));
    }
  }

}

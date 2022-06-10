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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionContainer;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.ManualInteractionXynaOrder;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.base.DefaultSubworkflowCall;
import com.gip.xyna.xprc.xfractwfe.base.DetachedCall;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep.FractalProcessStepFilter;
import com.gip.xyna.xprc.xfractwfe.base.GenericInputAsContextStep;
import com.gip.xyna.xprc.xfractwfe.base.JavaCall;
import com.gip.xyna.xprc.xfractwfe.base.StartVariableContextStep;
import com.gip.xyna.xprc.xfractwfe.base.SubworkflowCall;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SPECIAL_PURPOSES;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SpecialPurposeIdentifier;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution.EmptyResponseListener;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;
import com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall;
import com.gip.xyna.xprc.xsched.xynaobjects.RemoteCallInput;



public class StepFunction extends Step implements Catchable, HasDocumentation {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Service.class);

  private static final String _METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID_ORIG = "getChildOrderId";
  protected static final String METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID;
  private static final String _METHODNAME_SUBWF_CALL_GET_CHILD_ORDER_ORIG = "getChildOrder";
  protected static final String METHODNAME_SUBWF_CALL_GET_CHILD_ORDER;
  private static final String _METHODNAME_XYNA_ORDER_CLEAR_ORDER_INPUT_CREATION_ORIG = "clearOrderInputCreationInstances";
  protected static final String METHODNAME_XYNA_ORDER_CLEAR_ORDER_INPUT_CREATION;
  private static final String _METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD_ORIG = "getInputPayload";
  protected static final String METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD;
  private static final String _METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD_ORIG = "setInputPayload";
  protected static final String METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD;
  private static final String _METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT_ORIG = "getOrderContext";
  protected static final String METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT;
  private static final String _METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD_ORIG = "getOutputPayload";
  protected static final String METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD;
  private static final String _METHODNAME_XYNA_ORDER_GET_SESSION_ID_ORIG = "getSessionId";
  protected static final String METHODNAME_XYNA_ORDER_GET_SESSION_ID;
  private static final String _METHODNAME_XYNA_ORDER_SET_SESSION_ID_ORIG = "setSessionId";
  protected static final String METHODNAME_XYNA_ORDER_SET_SESSION_ID;
  private static final String _METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC_ORIG = "compensateOrderSynchronously";
  protected static final String METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC;
  private static final String _METHODNAME_START_ORDER_ORIG = "startOrder";
  @Deprecated
  protected static final String METHODNAME_START_ORDER_deprecated;
  protected static final String METHODNAME_START_ORDER = "startOrder";
  private static final String _METHODNAME_START_ORDER_SYNC_ORIG = "startOrderSynchronous";
  @Deprecated
  protected static final String METHODNAME_START_ORDER_SYNC_deprecated;
  protected static final String METHODNAME_START_ORDER_SYNC = "startOrderSynchronous";

  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID = DetachedCall.class.getDeclaredMethod(_METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID_ORIG + " not found", e);
    }
    try {
      METHODNAME_SUBWF_CALL_GET_CHILD_ORDER = SubworkflowCall.class.getDeclaredMethod(_METHODNAME_SUBWF_CALL_GET_CHILD_ORDER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_SUBWF_CALL_GET_CHILD_ORDER_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_CLEAR_ORDER_INPUT_CREATION = XynaOrderServerExtension.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_CLEAR_ORDER_INPUT_CREATION_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_CLEAR_ORDER_INPUT_CREATION_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD = XynaOrder.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD = XynaOrder.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD_ORIG, GeneralXynaObject.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT = XynaOrderServerExtension.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_GET_SESSION_ID = XynaOrder.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_GET_SESSION_ID_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_GET_SESSION_ID_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_SET_SESSION_ID = XynaOrder.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_SET_SESSION_ID_ORIG, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD_ORIG + " not found", e);
    }
    try {
      METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD = XynaOrder.class.getDeclaredMethod(_METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD_ORIG + " not found", e);
    }
    try {
      METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC = XynaProcessCtrlExecution.class.getDeclaredMethod(_METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC_ORIG, XynaOrderServerExtension.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC_ORIG + " not found", e);
    }
    try {
      METHODNAME_START_ORDER_deprecated = XynaProcessCtrlExecution.class.getDeclaredMethod(_METHODNAME_START_ORDER_ORIG, XynaOrderServerExtension.class, ResponseListener.class, OrderContext.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_START_ORDER_ORIG + " not found", e);
    }
    try {
      METHODNAME_START_ORDER_SYNC_deprecated = XynaProcessCtrlExecution.class.getDeclaredMethod(_METHODNAME_START_ORDER_SYNC_ORIG, XynaOrderServerExtension.class, boolean.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_START_ORDER_SYNC_ORIG + " not found", e);
    }
  }
  
  private String serviceId;
  private String operationName;
  private InputConnections input;
  private InputConnections oisInput;
  private String[] receiveVarCastToType;
  private String[] receiveVarIds;
  private String[] receivePaths;
  private Service serviceReference; //gibt es hier seitdem es rapid prototyping gibt, und services vor ihrer fertigstellung prototypisch in echten wfs verwendet werden.
  //private SpecialPurposeIdentifier specialPurpose;
  private String orderInputSourceRef;
  private int orderInputSourceInputLength;
  private RemoteDespatchingParameter remoteDispatchingParameter;
  
  private boolean isExecutionDetached = false;
  private boolean freesCapacities = false;
  private List<String> queryFilterConditions;
  private static final String FUNCNAME_CREATE_ORDER_FOR_DETACHED_SERVICE_CALL = "lazyCreateOrderForDetachedServiceCall";
  
  private StepCatch catchStep;
  private Step compensateStep;
  private String label;
  private String documentation = "";
  private String abstractUid = null; // only necessary for flash GUI - TODO: remove when flash GUI is not used, anymore
  
  private static final String PATH_TO_XPRC_CTRL_EXECUTION = XynaFactory.class.getName() + ".getInstance().getProcessing().getXynaProcessCtrlExecution()";
  private static final String VARNAME_firstWaitOrNotifyTime = "firstAwaitOrNotificationTime";
  private static final String VARNAME_suspensionTime = "suspensionTime";
  private static final String VARNAME_resumeTime = "resumeTime";
  static final String VARNAME_CHILDORDERSTORAGE_STACK = "childOrderStorageStack";
  static final String VARNAME_CHILDORDERSTORAGE = "childOrderStorage";
  
  private final String SG_FQN_WAIT_AND_SUSPEND = "xprc.waitsuspend.WaitAndSuspendFeature";
  private final String SG_FQN_SYNCHRONIZATION = "xprc.synchronization.Synchronization";
  private final String SG_FQN_TEMPLATE_MANAGEMENT = "xact.templates.TemplateManagement";
  private final String SG_FQN_PERSISTENCE_SERVICES = "xnwh.persistence.PersistenceServices";
  private final String SERVICE_NAME_WAIT = "wait";
  private final String SERVICE_NAME_SUSPEND = "suspend";
  private final String SERVICE_NAME_AWAIT = "awaitNotification";
  private final String SERVICE_NAME_LONG_RUNNING_AWAIT = "longRunningAwait";
  private final String SERVICE_NAME_NOTIFY = "notifyWaitingOrder";
  private final String SERVICE_NAME_START = "start";
  private final String SERVICE_NAME_STOP = "stop";
  private final String SERVICE_NAME_QUERY_EXTENDED = "queryExtended";
  private final String FQN_MANUAL_INTERACTION = "xmcp.manualinteraction.ManualInteraction";
  
  public StepFunction(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }
  
  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepFunction( this );
  }
  
  
  @Override
  public void parseXML(Element functionObjectElement) throws XPRC_InvalidPackageNameException {
    
    parseUnknownMetaTags(functionObjectElement, Arrays.asList(EL.DOCUMENTATION, EL.QUERY_FILTER, EL.ORDER_INPUT_SOURCE, EL.DETACHED, EL.FREE_CAPACITIES, "FixedDetailOptions"));
    isExecutionDetached = doesMetaElementDetachedExist(functionObjectElement);
    label = functionObjectElement.getAttribute(GenerationBase.ATT.LABEL);
        
    Element invoke = XMLUtils.getChildElementByName(functionObjectElement,
                                                    com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL.INVOKE);
    if (invoke == null) {
      return;
    }
    
    List<Element> sources = XMLUtils.getChildElementsByName(invoke, GenerationBase.EL.SOURCE);
    setServiceId(invoke.getAttribute(GenerationBase.ATT.SERVICEID));
    operationName = invoke.getAttribute(GenerationBase.ATT.INVOKE_OPERATION);
    
    
    input = new InputConnections(sources.size());
    for (int i = 0; i < sources.size(); i++) {
      input.parseSourceElement(sources.get(i), i);
    }
    
    // receive
    Element receive = XMLUtils.getChildElementByName(functionObjectElement, GenerationBase.EL.RECEIVE);
    List<Element> targets = XMLUtils.getChildElementsByName(receive, GenerationBase.EL.TARGET);
    receiveVarIds = new String[targets.size()];
    receivePaths = new String[targets.size()];
    receiveVarCastToType = new String[targets.size()];
    for (int i = 0; i < targets.size(); i++) {
      receiveVarIds[i] = targets.get(i).getAttribute(GenerationBase.ATT.REFID);
      receivePaths[i] = targets.get(i).getAttribute(GenerationBase.ATT.PATH);
      Element metaEl = XMLUtils.getChildElementByName(targets.get(i), GenerationBase.EL.META);
      if (metaEl != null) {
        Element expectedType = XMLUtils.getChildElementByName(metaEl, GenerationBase.EL.EXPECTED_TYPE);
        if (expectedType != null) {
          receiveVarCastToType[i] = XMLUtils.getTextContent(expectedType);
        }
      }
    }
    
    List<Element> catchElements = XMLUtils.getChildElementsByName(functionObjectElement, GenerationBase.EL.CATCH);
    if (catchElements.size() > 0) {
      catchStep = new StepCatch(getParentScope(), this, creator);
      catchStep.parseXML(functionObjectElement);
    } else {
      // nur, wenn kein catch drum rum ist, weil ansonsten generiert der die auditdaten
      parseId(functionObjectElement);
    }
    
    // compensate
    Element compensateEl = XMLUtils.getChildElementByName(functionObjectElement, GenerationBase.EL.COMPENSATE);
    if (compensateEl != null) {
      StepSerial serial = new StepSerial(getParentScope(), creator);
      serial.parseXML(compensateEl);
      compensateStep = serial.getProxyForCatch();
    }
    
    Element serviceRefEl = XMLUtils.getChildElementByName(functionObjectElement, GenerationBase.EL.SERVICEREFERENCE);
    if (serviceRefEl != null) {
      Service service = new Service(creator);
      service.parseXML(serviceRefEl, functionObjectElement);
      serviceReference = service;
      documentation = service.getDocumentation();
    }
    
    Element metaEl = XMLUtils.getChildElementByName(functionObjectElement, GenerationBase.EL.META);
    if (metaEl != null) {
      /*<Meta>
          <OrderType>xact.dhcp.DHCPv6Services.DHCPv6Services.processRequest</OrderType>
          <OrderInputSource>DHCPv6 processRequest Test</OrderInputSource>
          <Documentation>Verarbeitung von Request-Anfragen.</Documentation>
        </Meta>
      */
      Element orderInputSourceRefEl = XMLUtils.getChildElementByName(metaEl, GenerationBase.EL.ORDER_INPUT_SOURCE);
      if (orderInputSourceRefEl != null) {
        orderInputSourceRef = XMLUtils.getTextContent(orderInputSourceRefEl);
        orderInputSourceInputLength = input.length();
        oisInput = input;
        input = new InputConnections(0);
      }
      
      Element documentationElement = XMLUtils.getChildElementByName(metaEl, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        documentation = XMLUtils.getTextContent(documentationElement);
      }
      
      freesCapacities = XMLUtils.getChildElementByName(metaEl, GenerationBase.EL.FREE_CAPACITIES) != null;
      
      Element queryFiltersEl = XMLUtils.getChildElementByName(metaEl, GenerationBase.EL.QUERY_FILTER);
      if (queryFiltersEl != null) {
        List<Element> conditionsEl = XMLUtils.getChildElementsByName(queryFiltersEl, GenerationBase.EL.QUERY_FILTER_CONDITION);
        if (conditionsEl != null) {
          queryFilterConditions = new ArrayList<String>();
          for (Element conditionEl : conditionsEl) {
            queryFilterConditions.add(XMLUtils.getTextContent(conditionEl));
          }
        }
      }
    }
    
    Element remoteDispatching = XMLUtils.getChildElementByName(functionObjectElement, GenerationBase.EL.REMOTE_DISPATCHING);
    if (remoteDispatching != null) {
      String remoteDestination = remoteDispatching.getAttribute(ATT.REMOTE_DESTINATION);
      List<Element> destinationSources = XMLUtils.getChildElementsByName(remoteDispatching, GenerationBase.EL.SOURCE);
      String[] destinationInvokeVarIds = new String[destinationSources.size()];
      String[] destinationInvokePaths = new String[destinationSources.size()];
      boolean[] constantConnected = new boolean[destinationSources.size()];
      boolean[] userConnected = new boolean[destinationSources.size()];
      for (int i = 0; i < destinationSources.size(); i++) {
        Element destinationSource = destinationSources.get(i);
        Element meta = XMLUtils.getChildElementByName(destinationSource, EL.META);
        destinationInvokeVarIds[i] = destinationSource.getAttribute(GenerationBase.ATT.REFID);
        destinationInvokePaths[i] = destinationSource.getAttribute(GenerationBase.ATT.PATH);
        constantConnected[i] = isConnectionType(meta, EL.LINKTYPE_CONSTANT_CONNECTED);
        userConnected[i] = isConnectionType(meta, EL.LINKTYPE_USER_CONNECTED);
      }
      this.remoteDispatchingParameter = new RemoteDespatchingParameter(remoteDestination, destinationInvokeVarIds, destinationInvokePaths, userConnected, constantConnected);
    }
    
    parseParameter(functionObjectElement);
  }
  
  private boolean isConnectionType(Element meta, String connectionType) {
    if(meta == null) {
      return false;
    }
    Element linkType = XMLUtils.getChildElementByName(meta, EL.LINKTYPE);
    if(linkType == null) {
      return false;
    }
    
    return linkType.getTextContent() != null && linkType.getTextContent().equals(connectionType);
  }
  
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_MissingContextForNonstaticMethodCallException, XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException {
    // TODO code in StepParallel etc ähnlich => extraktion von teilcode
    ServiceIdentification s = getParentScope().identifyService(serviceId);
    
    if (isRemoteCall()) {
      ServiceIdentification remoteCallServiceIdentification = getRemoteCallServiceIdentification();
      operationName = isExecutionDetached() ? "initiateRemoteCallForDetachedCalls" : "initiateRemoteCall";
      this.remoteDispatchingParameter.remoteOrdertype = s.service.getOriginalFqName();
      generateJavaForDOMRef(remoteCallServiceIdentification, cb, importedClassesFqStrings);
    } else {
      if (s.service.isDOMRef()) {
        generateJavaForDOMRef(s, cb, importedClassesFqStrings);
      } else {
        generateJavaForWFRef(s, cb, importedClassesFqStrings);
      }
    }
  }
  
  private ServiceIdentification getRemoteCallServiceIdentification() throws XPRC_InvalidServiceIdException {
    ServiceIdentification s = getParentScope().identifyService(serviceId);
    ServiceIdentification si = new ServiceIdentification();
    si.scope = getParentScope();
    si.service = Service.getRemoteCallService(s.service.getId(), creator);
    return si;
  }
  
  public boolean isRemoteCall() {
    return remoteDispatchingParameter != null;
  }
  
  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException, XPRC_OperationUnknownException, XPRC_MissingContextForNonstaticMethodCallException {
    ServiceIdentification s = getParentScope().identifyService(serviceId);
    if (isRemoteCall()) {
      ServiceIdentification remoteCallServiceIdentification = getRemoteCallServiceIdentification();
      operationName = isExecutionDetached() ? "initiateRemoteCallForDetachedCalls" : "initiateRemoteCall";
      appendExecuteInternallyForDOMRef(remoteCallServiceIdentification, cb, importedClassesFqStrings);
    } else {
      if (s.service.isDOMRef()) {
        appendExecuteInternallyForDOMRef(s, cb, importedClassesFqStrings);
      } else {
        appendExecuteInternallyForWFRef(s, cb, importedClassesFqStrings);
      }
    }
  }
  
  public String getOrderInputSourceRef() {
    return orderInputSourceRef;
  }
  
  public void setOrderInputSourceRef(String orderInputSourceRef) {
    if (orderInputSourceRef == null || orderInputSourceRef.length() == 0) {
      if (this.orderInputSourceRef != null) {
        // switch from OIS to no OIS -> restore input
        input = oisInput;
        orderInputSourceInputLength = 0;
      }
    } else {
      if (this.orderInputSourceRef == null) {
        orderInputSourceInputLength = input.length();
        oisInput = input;
        input = new InputConnections(0);
      }
    }

    this.orderInputSourceRef = orderInputSourceRef;
  }
  
  public String getUniqueStepId() {
    //vgl FractalProcessStep getUniqueIdForInputSource
    if (orderInputSourceRef == null) {
      throw new RuntimeException("unsupported");
    }
    
    //gibt keine foreach-indizes, und retry ist hier nicht nötig
    return getIdx() + ".0";
  }
  
  protected void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException {
    
    Service s = getParentScope().identifyService(serviceId).service;
    if (isExecutionDetached()) {
      imports.add(DOM.getNameForImport(EmptyResponseListener.class));
      imports.add(DetachedCall.class.getName());
      imports.add(OrderContext.class.getName());
    }
    imports.add(ChildOrderStorage.class.getName());
    imports.add(DOM.getNameForImport(ChildOrderStorageStack.class));
    
    imports.add(StartVariableContextStep.class.getName());
    imports.add(GenericInputAsContextStep.class.getName());
    imports.add(FractalProcessStepFilter.class.getCanonicalName());
    
    //we will only need this when calling MI-WFs
    imports.add(DOM.getNameForImport(ManualInteractionResponse.class));
    imports.add(ManualInteractionXynaOrder.class.getName());
    imports.add(ClassLoaderBase.class.getName());
    
    if (!isRemoteCall() && (!s.isDOMRef() || s.getDom().getOperationByName(operationName) instanceof WorkflowCallServiceReference)) {
      imports.add(DefaultSubworkflowCall.class.getName());
      imports.add(SubworkflowCall.class.getName());
      imports.add(OrderContext.class.getName());
    } else {
      imports.add(JavaCall.class.getName());
    }
    
    if (isRemoteCall()) {
      imports.add(RevisionManagement.class.getName());
    }
    
    if (catchStep != null) {
      catchStep.getImports(imports);
    }
    
    if (orderInputSourceRef != null) {
      imports.add(XynaOrderCreationParameter.class.getName());
    }
  }

  private long calcSerialVersionUID() throws XPRC_InvalidServiceIdException, XPRC_OperationUnknownException {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    ServiceIdentification s = getParentScope().identifyService(serviceId);
    if (s.service.isDOMRef()) {
      Operation ops = s.service.getDom().getOperationByName(operationName);
      final boolean specialPurposeWaitOrSuspend = ops.isSpecialPurpose(SpecialPurposeIdentifier.WAIT, SpecialPurposeIdentifier.SUSPEND);
      if (specialPurposeWaitOrSuspend) {
        types.add(Pair.of(VARNAME_suspensionTime, Long.class.getName()));
        types.add(Pair.of(VARNAME_resumeTime, Long.class.getName()));
      }
      
      final boolean specialPurposeAwaitSynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_AWAIT);
      final boolean specialPurposeLongRunningAwaitSynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_LONG_AWAIT);
      if (specialPurposeAwaitSynchronization || specialPurposeLongRunningAwaitSynchronization) {
        types.add(Pair.of(VARNAME_firstWaitOrNotifyTime, Long.class.getName()));
      }
      
      if (isExecutionDetached) {
        types.add(Pair.of("childOrderId", long.class.getName()));
        types.add(Pair.of("subworkflow", XynaOrderServerExtension.class.getName()));
      } else if (ops instanceof WorkflowCallServiceReference) {
        types.add(Pair.of("subworkflow", XynaOrderServerExtension.class.getName()));
      } else if (!isSpecialPurposeOpsWithReinitializeBlock(ops)) {
        types.add(Pair.of(VARNAME_CHILDORDERSTORAGE, ChildOrderStorage.class.getName()));
      }
      
      final boolean isStartDocumentContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STARTDOCUMENTCONTEXT);
      if (isStartDocumentContext) {
        types.add(Pair.of("localContextVariable", XynaObject.class.getName()));
      }
      
    } else {
      types.add(Pair.of("firstExecution", boolean.class.getName()));
      if (isExecutionDetached) {
        types.add(Pair.of("childOrderId", long.class.getName()));
      }
      types.add(Pair.of("subworkflow", XynaOrderServerExtension.class.getName()));
    }
    
    return GenerationBase.calcSerialVersionUID(types);
  }
  
  //TODO die beiden generate methoden besser mergen/gemeinsamkeiten extrahieren
  private void generateJavaForWFRef(ServiceIdentification si, CodeBuffer cb, HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException, XPRC_InvalidServiceIdException, XPRC_OperationUnknownException, XPRC_MissingContextForNonstaticMethodCallException {
    
    cb.addLine("/*  " + GenerationBase.escapeForCodeGenUsageInComment(label) + "  */");
    cb.add("private static class " + getClassName() + " extends " + DefaultSubworkflowCall.class.getSimpleName() + "<"
                    + getParentScope().getClassName() + "> ");
    if (isExecutionDetached()) {
      cb.add(" implements ", DetachedCall.class.getSimpleName());
    }
    cb.add(" {");
    cb.addLB(2);
    
    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calcSerialVersionUID()), "L");
    if (orderInputSourceRef != null) {
      cb.addLine("private static final String inputSourceName = \"", orderInputSourceRef, "\"");
    }
    
    cb.addLine("public ", getClassName(), "() {");
    cb.addLine("super(" + getIdx(), ")");
    cb.addLine("}").addLB();
    
    if (isExecutionDetached) {
      cb.addLine("private long childOrderId = -1");
    }
    if (isExecutionDetached) {
      cb.addLine("public long ", METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID, "() {");
      cb.addLine("if (childOrderId < 0) {");
      //subworkflow wurde offenbar noch nicht erstellt, weil damit die id-belegung einher geht.
      cb.addLine("try {");
      cb.addLine(FUNCNAME_LAZY_CREATE_SUBWF, "()");
      cb.addLine("} catch (", XynaException.class.getSimpleName() + " e) {");
      logException(cb, "e");
      cb.addLine("}");
      cb.addLine("}");
      cb.addLine("return childOrderId");
      cb.addLine("}").addLB();
    }
    
    cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
    cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
    if (orderInputSourceRef != null) {
      cb.addLine(METHODNAME_GET_PROCESS, "().", WF.METHODNAME_GET_CORRELATED_XYNA_ORDER, "().", METHODNAME_XYNA_ORDER_CLEAR_ORDER_INPUT_CREATION, "()");
    }
    if (isExecutionDetached) {
      cb.addLine("childOrderId = -1");
    }
    cb.addLine("}").addLB();
    
    cb.add("public String ", METHODNAME_GET_LABEL, "() { return \"" + GenerationBase.escapeForCodeGenUsageInString(label) + "\"; }")
        .addLB();
    
    // execute internally
    appendExecuteInternally(cb, importedClassesFqStrings);
    
    //lazyCreateSubWf
    List<VariableIdentification> inputVarsForWFCall = new ArrayList<VariableIdentification>();
    for (String id : input.getVarIds()) {
      inputVarsForWFCall.add(getParentScope().identifyVariable(id));
    }
    
    createCodeForLazyCreateSubWf(cb, si.service, inputVarsForWFCall, importedClassesFqStrings);
    
    cb.addLine("public boolean compensationRecursive() {");
    cb.add("return ");
    if (compensationDelegatesToSubworkflow()) {
      cb.add("true");
    } else {
      cb.add("false");      
    }
    cb.addLB();
    cb.addLine("}");
    
    int childrenIdx = 0;
    
    // compensation
    if (compensateStep != null) {
        cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getName(), "{");
        //kein super-aufruf
        cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(" + (childrenIdx++) + ")");
        cb.addLine("}").addLB();
    } //ansonsten siehe superklasse (DefaultSubworkflowCall)
    
    if (orderInputSourceRef != null) {
      cb.addLine("public ", GeneralXynaObject.class.getSimpleName(), "[] ", METHODNAME_GET_CURRENT_INCOMING_VALUES, "() {");
      cb.addLine("try {");
      cb.addLine(FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF + "()");
      cb.addLine("} catch (", XynaException.class.getSimpleName() + " e) {");
      logException(cb, "e");
      cb.addLine("return new ", GeneralXynaObject.class.getSimpleName(), "[0]");
      cb.addLine("}");
      if (orderInputSourceInputLength == 1) {
        cb.addLine("return new ", GeneralXynaObject.class.getSimpleName(), "[]{ subworkflow.", METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD, "() };");
      } else {
        cb.addLine(Container.class.getSimpleName(), " ct = (", Container.class.getSimpleName(), ") subworkflow.", METHODNAME_XYNA_ORDER_GET_INPUT_PAYLOAD, "()");
        cb.add("return new ", GeneralXynaObject.class.getSimpleName(), "[]{ ");
        for (int i = 0; i<orderInputSourceInputLength; i++) {
          cb.addListElement("ct.get(" + i + ")");
        }
        cb.add(" };");
        cb.addLB();
      }
      cb.addLine("}");
    } else {
      generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, input.getVarIds(), input.getPaths(), cb,
                                          importedClassesFqStrings);
    }
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, receiveVarIds, receivePaths, cb,
                                          importedClassesFqStrings);
    
    generatedGetRefIdMethod(cb);
    
    // getChildren
    childrenIdx = 0;
    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<" + getParentScope().getClassName()
        + ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    
    if (compensateStep != null) {
      cb.addLine("if (i == " + (childrenIdx++) + ") {");
      cb.addLine("return new " + FractalProcessStep.class.getSimpleName() + "[]{", METHODNAME_GET_PARENT_SCOPE, "()."
          + compensateStep.getVarName() + "};");
      cb.addLine("}");
    }
    cb.addLine("return null").addLine("}").addLB();
    // getChildrenLength
    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return " + childrenIdx);
    cb.addLine("}").addLB();
    
    cb.addLine("}").addLB();
    
  }
  
  private boolean compensationDelegatesToSubworkflow() {
    if (compensateStep != null) {
      return false; //immer richtig
    }
    /*
     * manchmal könnte man hier auch false zurückgeben. nämlich wenn man weiß, dass im subworkflow in den compensationschritten (rekursiv) nichts passiert.
     * achtung: wenn man das bereits beim deployment berechnet, darf das ergebnis nicht im generierten code des aufrufer-wfs stehen, weil
     *  das nicht unbedingt aktuell gehalten wird, falls sich der subworkflow (oder rekursiv sub-sub-sub-workflow) ändert
     *  
     * achtung: eine schlauere ermittlung führt zu häufigeren archivierungsvorgängen, und damit mit auditdaten zu häufigeren nicht-gebatchten
     * orderarchive-commits. das kann in der summe zu schlechter performance führen. leider ist ein batching der vorgezogenen commits nicht so einfach
     * zu realisieren (da muss man dann sehr aufpassen, keine daten zu verlieren).
     */
    return true;
  }

  private void logException(CodeBuffer cb, String exceptionVarName) {
    //erstmal nicht so wichtig
  }
  
  private static final XynaPropertyBoolean executeSubWorkflowWithProcessing = new XynaPropertyBoolean("xprc.xfractwfe.generation.subworkflow.useprocessing", true).setHidden(true);
  
  protected void appendExecuteInternallyForWFRef(ServiceIdentification service, CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws " + XynaException.class.getSimpleName() + " {");
    
    if (executeSubWorkflowWithProcessing.get()) {
      // create the code for the subworkflow call
      cb.addLine("final boolean onlyAddToScheduler = !firstExecution");
      cb.addLine("firstExecution = false");
      if (isExecutionDetached()) {
        cb.addLine(FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF, "()");
        cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION + ".", METHODNAME_START_ORDER,
                   "(subworkflow, new " + EmptyResponseListener.class.getSimpleName() + "(), subworkflow.",
                   METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT, "())");
        cb.addLine("subworkflow = null");
      } else {
        cb.addLine(FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF, "()");
        cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION + ".", METHODNAME_START_ORDER_SYNC, "(subworkflow, onlyAddToScheduler)");
        if (receiveVarIds.length == 0) {
        } else if (receiveVarIds.length == 1) {
          cb.addLine(GeneralXynaObject.class.getSimpleName() + " temp = subworkflow.", METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()");
        } else {
          cb.addLine(Container.class.getSimpleName() + " c = (" + Container.class.getSimpleName() + ") subworkflow.",
                     METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()");
        }
        
        cb.addLine("if (subworkflow.removeFromParentWF()) {");
        cb.addLine("subworkflow = null");
        cb.addLine("}");
        appendResultSetter(cb, importedClassesFqStrings, true);
      }
    } else {
      cb.addLine(XynaProcess.class.getSimpleName(), " subwf = new ", service.service.getFQClassName(), "()");
      if (receiveVarIds.length == 0) {
        cb.add("subwf.execute(");
      } else if (receiveVarIds.length == 1) {
        cb.add(GeneralXynaObject.class.getSimpleName() + " temp = subwf.execute(");
      } else {
        cb.add(Container.class.getSimpleName() + " c = (" + Container.class.getSimpleName()
                        + ") subwf.execute(");
      }
      
      
      List<VariableIdentification> inputVarsForWFCall = new ArrayList<VariableIdentification>();
      for (String id : input.getVarIds()) {
        inputVarsForWFCall.add(getParentScope().identifyVariable(id));
      }
      generateCodeSubworkflowCallInputs(cb, inputVarsForWFCall, importedClassesFqStrings);
      
      cb.add(", new ", XynaOrderServerExtension.class.getSimpleName(), "())").addLB();
      
      appendResultSetter(cb, importedClassesFqStrings, true);
    }
    
     cb.addLine("}").addLB();
  }
  
  protected void appendExecuteRemoteInternally(ServiceIdentification service, CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws " + XynaException.class.getSimpleName() + " {");    
    // create the code for the subworkflow call
    cb.addLine("final boolean onlyAddToScheduler = !firstExecution");
    cb.addLine("firstExecution = false");
    if (isExecutionDetached()) {
      cb.addLine(FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF, "()");
      cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION + ".", METHODNAME_START_ORDER, "(subworkflow, new "
                      + EmptyResponseListener.class.getSimpleName() + "(), subworkflow.", METHODNAME_XYNA_ORDER_GET_ORDER_CONTEXT, "())");
      cb.addLine("subworkflow = null");
    } else {
      cb.addLine(FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF, "()");
      cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION + ".", METHODNAME_START_ORDER_SYNC, "(subworkflow, onlyAddToScheduler)");
      if (receiveVarIds.length == 0) {
      } else if (receiveVarIds.length == 1) {
        cb.addLine(GeneralXynaObject.class.getSimpleName() + " temp = subworkflow.", METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()");
      } else {
        cb.addLine(Container.class.getSimpleName() + " c = (" + Container.class.getSimpleName()
                        + ") subworkflow.", METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()");
      }
      
      appendResultSetter(cb, importedClassesFqStrings, true);
    }
    
     cb.addLine("}").addLB();
  }
  
  private void createSetInputVarsMethodForDetachedCall(CodeBuffer cb, ServiceIdentification service, Operation operation,
                                        List<VariableIdentification> inputVarsForDetachedServiceOperationCall,
                                        HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    
    cb.addLine("private void ", FUNCNAME_INITIALIZE_DETACHED_SUB_WF_XYNAORDER, "() throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine(FUNCNAME_CREATE_ORDER_FOR_DETACHED_SERVICE_CALL, "()");
    cb.add("subworkflow.", METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD, "(");
    if (isRemoteCall()) {
      cb.add("new ", Container.class.getSimpleName(), "(");
      cb.add(service.getScopeGetter(getParentScope()), Service.generateRemoteCallVarName(this), ", ");
      cb.add("new ", RemoteCallInput.class.getName(), "(");
      cb.addListElement("\"" + remoteDispatchingParameter.remoteOrdertype + "\"");
      cb.addListElement("XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject(\""
          + remoteDispatchingParameter.remoteOrdertype + "\", RevisionManagement.getRevisionByClass(getClass())))");
      cb.addListElement("\"" + remoteDispatchingParameter.remoteDestination + "\"");
      cb.add("), "); //remotecallinput
      cb.addListElement(wrapInSingleXynaObject( "", remoteDispatchingParameter.invokeVarIds, remoteDispatchingParameter.invokePaths, true) );
      cb.addListElement(wrapInSingleXynaObject( "", input.getVarIds(), input.getPaths(), true) );
      cb.add(")"); //container
    } else {
    if (inputVarsForDetachedServiceOperationCall.size() == 1) {
      AVariable v = inputVarsForDetachedServiceOperationCall.get(0).variable;
      String varWithinParentScope = inputVarsForDetachedServiceOperationCall.get(0).getScopeGetter(getParentScope())
                      + v.getVarName();
      String clonedVarWithinParentScopeOrNull = varWithinParentScope + " == null ? null : " + varWithinParentScope;
      if (inputVarsForDetachedServiceOperationCall.get(0).variable.isList()) {
        if (inputVarsForDetachedServiceOperationCall.get(0).variable instanceof ExceptionVariable) {
          cb.add("new ", GeneralXynaObjectList.class.getSimpleName(), "(");
          cb.add(clonedVarWithinParentScopeOrNull, ", ",
                 v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ".class)");
        } else {
          cb.add("new ", XynaObjectList.class.getSimpleName(), "(");
          cb.add(clonedVarWithinParentScopeOrNull, ", ",
                 v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ".class)");
        }
      } else {        
        cb.add(clonedVarWithinParentScopeOrNull);
        if (operation.isStatic()) {
          cb.add(".clone()");
        }
      }
    } else {
      cb.add("new " + Container.class.getSimpleName() + "(");
      boolean first = true;
      for (VariableIdentification vi : inputVarsForDetachedServiceOperationCall) {
        AVariable v = vi.variable;
        String varWithinParentScope = vi.getScopeGetter(getParentScope()) + v.getVarName();        
        String clonedVarWithinParentScopeOrNull = varWithinParentScope + " == null ? null : "
                        + varWithinParentScope;
        if (!operation.isStatic() && first) {
          first = false;
        } else {
          //erster parameter ist instanzparameter und wird nicht gecloned
          clonedVarWithinParentScopeOrNull += ".clone()";
        }
        if (v.isList()) {
          //TODO redundante xynaobjectlist weg. membervar von process ist bereits xynaobjectlist
          if (v instanceof ExceptionVariable) {
            cb.addListElement("new " + GeneralXynaObjectList.class.getSimpleName() + "(" + clonedVarWithinParentScopeOrNull
                              + ", " + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
          } else {
            cb.addListElement("new " + XynaObjectList.class.getSimpleName() + "(" + clonedVarWithinParentScopeOrNull
                          + ", " + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
          }
        } else {
          cb.addListElement(clonedVarWithinParentScopeOrNull);
        }
      }
      cb.add(")");
    }
    }
    cb.add(")").addLB();
    cb.addLine("}").addLB();
    
  }

  private static final String FUNCNAME_INITIALIZE_DETACHED_SUB_WF_XYNAORDER = "initializeDetachedSubworkflowXynaOrder";
  private void createCodeForLazyCreateDetachedServiceOperationWf(CodeBuffer cb,
                                                                 Service s,
                                                                 HashSet<String> importedClassesFqStrings) {
    
    if (s.isDOMRef() && isExecutionDetached()) {
      
      cb.addLine("private synchronized void " + FUNCNAME_CREATE_ORDER_FOR_DETACHED_SERVICE_CALL + "() {");
      cb.addLine("if (subworkflow == null) {");
      String targetOrderType;
      if (isRemoteCall()) {
        targetOrderType = RemoteCall.FQ_CLASS_NAME;
      } else {
        targetOrderType = getParentWFObject().getOriginalFqName();
      }
      targetOrderType += "." + s.getServiceName() + "." + getOperationName();
      
      cb.addLine("subworkflow = new " + XynaOrderServerExtension.class.getSimpleName() + "(new "
                      + DestinationKey.class.getSimpleName() + "(\"" + targetOrderType + "\"))");
      
      cb.addLine(XynaOrderServerExtension.class.getSimpleName() + " cxo = ", METHODNAME_GET_PROCESS, "().", WF.METHODNAME_GET_CORRELATED_XYNA_ORDER, "()");
      cb.addLine("childOrderId = subworkflow.getId()");
      cb.addLine("subworkflow.setParentStepNo(" + Step.METHODNAME_GET_N + "())");
      cb.addLine("subworkflow.setParentLaneId(" + Step.METHODNAME_GET_LANE_ID + "())").addLB();
      cb.addLine("subworkflow." + METHODNAME_XYNA_ORDER_SET_SESSION_ID + "(cxo." + METHODNAME_XYNA_ORDER_GET_SESSION_ID + "())");
      
      cb.addLine("if (cxo.getOrderContext() != null) {");
      cb.addLine("subworkflow.setNewOrderContext()");
      cb.addLine("}");
      
      if (isRemoteCall()) {
        //remotecall benutzt revision beim deserialisieren des remote-outputs. 
        //der subworkflow hat bei detached aufrufen immer den gleichen ordertype, dieser muss in einer eindeutigen revision existieren.
        cb.addLine("subworkflow.setRevision(cxo.getRevision())");
      } else {
        cb.addLine("subworkflow.setRevision(getRevisionForOrderType(subworkflow.getDestinationKey()))");
      }
      
      cb.addLine("subworkflow.setCustom0(cxo.getCustom0())");
      cb.addLine("subworkflow.setCustom1(cxo.getCustom1())");
      cb.addLine("subworkflow.setCustom2(cxo.getCustom2())");
      cb.addLine("subworkflow.setCustom3(cxo.getCustom3())");
      cb.addLine("(("+ OrderContextServerExtension.class.getName() + ") subworkflow.getOrderContext()).set("+ OrderContextServerExtension.class.getName() + ".CREATION_ROLE_KEY, cxo.getCreationRole())");
      cb.addLine("}"); //end if subworkflow==null
      cb.addLine("}").addLB(); // end lazyCreateOrderForDetachedServiceCall
    }
    
  }
  
  
  private static final String FUNCNAME_LAZY_CREATE_SUBWF = "lazyCreateSubWf";
  private static final String FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF = "lazyCreateAndSetInputVariablesForSubWf";
  private void createCodeForLazyCreateSubWf(CodeBuffer cb, Service s, List<VariableIdentification> inputVarsForWFCall,
                                            HashSet<String> importedClassesFqStrings) {
    
    if (!s.isDOMRef()) {
      
      cb.addLine("private void ", FUNCNAME_LAZY_CREATE_AND_SET_INPUTS_FOR_SUBWF, "() throws ", XynaException.class.getSimpleName(), " {");
      cb.addLine(FUNCNAME_LAZY_CREATE_SUBWF, "()");
      if (orderInputSourceRef == null) {
        //TODO wieso muss man die inputpayload immer setzen und nicht nur beim ersten aufruf?
        cb.add("subworkflow.", METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD, "(");
        generateCodeSubworkflowCallInputs(cb, inputVarsForWFCall, importedClassesFqStrings);
        cb.add(")").addLB();
      }
      cb.addLine("}").addLB();
      
      // FIXME eigentlich erstellt man hier einen SubAUFTRAG, keinen SubWORKFLOW!
      cb.addLine("protected synchronized void ", FUNCNAME_LAZY_CREATE_SUBWF, "() throws ", XynaException.class.getSimpleName(), " {");
      cb.addLine("if (subworkflow == null) {");
      
      cb.addLine(XynaOrderServerExtension.class.getSimpleName(), " cxo = ", METHODNAME_GET_PROCESS, "().", WF.METHODNAME_GET_CORRELATED_XYNA_ORDER, "()");
      
      if (orderInputSourceRef != null) {
        cb.addLine("String idOfInputSourceInWF = getUniqueId()");
        cb.addLine(XynaOrderCreationParameter.class.getSimpleName() + " xocp = cxo.getOrCreateOrderInput(idOfInputSourceInWF, inputSourceName)");
      }
      
      // set possible mi responses: nur die hier definierten sind für den MI bearbeiter erlaubt (retry geht nur, wenn man sich in einem catchblock mit retry-feature befindet)
      //TODO ist das so geschickt mit den MIs?
      if (isManualInteractionInvocation(s.getWF())) {
        cb.addLine(List.class.getSimpleName(), "<" + ManualInteractionResponse.class.getSimpleName(),
                   "> allowedResponses = new ", ArrayList.class.getSimpleName(),
                   "<" + ManualInteractionResponse.class.getSimpleName(), ">()");
        cb.addLine("allowedResponses.add(", ManualInteractionResponse.class.getSimpleName(), ".",
                   ManualInteractionResponse.ABORT.toString(), ")");
        cb.addLine("allowedResponses.add(", ManualInteractionResponse.class.getSimpleName(), ".",
                   ManualInteractionResponse.CONTINUE.toString(), ")");
        
        //inside step catch
        Step possibleCatchStep = this.getParentStep();
        Step catchLane = null;
        boolean insideCatch = false;
        while (possibleCatchStep != null) {
          if (possibleCatchStep instanceof StepCatch && ((StepCatch) possibleCatchStep).getStepInTryBlock() != this) {
            insideCatch = true;
            break;
          } else {
            catchLane = possibleCatchStep;
            possibleCatchStep = possibleCatchStep.getParentStep();
          }
        }
        
        if (insideCatch) {
          // does the lane contain a retry
          if (Step.containsOrIsAssignableFrom(catchLane, StepRetry.class)) {
            cb.addLine("allowedResponses.add(", ManualInteractionResponse.class.getSimpleName(), ".",
                       ManualInteractionResponse.RETRY.toString(), ")");
          }
        }
        if (orderInputSourceRef == null) {
          cb.addLine("subworkflow = new ", ManualInteractionXynaOrder.class.getSimpleName(), "(new ",
                   DestinationKey.class.getSimpleName(), "(\"" + s.getWF().getFqClassName(), "\"), allowedResponses)");
        } else {
          cb.addLine("subworkflow = new ", ManualInteractionXynaOrder.class.getSimpleName(), "(xocp, allowedResponses)");
        }
      } else {
        if (orderInputSourceRef == null) {
          cb.addLine("subworkflow = new ", XynaOrderServerExtension.class.getSimpleName(), "(new ",
                   DestinationKey.class.getSimpleName(), "(\"", s.getWF().getFqClassName(), "\"))");
        } else {
          cb.addLine("subworkflow = new ", XynaOrderServerExtension.class.getSimpleName(), "(xocp)");
        }
      }
      
      if (orderInputSourceRef != null) {
        cb.addLine("subworkflow.setOrderInputCreationInstances(cxo.getAndRemoveOrderInputCreationInstances(idOfInputSourceInWF))");
      }
      
      //subauftrag-revision bestimmen anhand von aktueller revision und destinationkey. destinationkey hat in einigen fällen aber noch keinen runtimecontext zugewiesen.
      //d.h. unterscheiden, ob dies der fall ist, oder nicht.
      //bei fehler die eigene revision verwenden
      cb.addLine("subworkflow.setRevision(getRevisionForOrderType(subworkflow.getDestinationKey()))");
      
      if (isExecutionDetached()) {
        cb.addLine("childOrderId = subworkflow.getId()");
        cb.addLine("subworkflow." + METHODNAME_XYNA_ORDER_SET_SESSION_ID + "(cxo." + METHODNAME_XYNA_ORDER_GET_SESSION_ID + "())");
      } else {
        cb.addLine("subworkflow.setParentOrder(cxo)");
      }
      cb.addLine("if (!compensationRecursive()) {"); //wenn man hier bereits weiß, dass compensation eh rekursiv ausgeführt wird, stepcoords nicht setzen. dann wird beim archivieren schnell erkannt, dass man nichts aufräumen kann.
      cb.addLine("subworkflow.setStepCoordinates(getCoordinates())");
      cb.addLine("}");
      cb.addLine("subworkflow.setParentStepNo(", Step.METHODNAME_GET_N, "())");
      cb.addLine("subworkflow.setParentLaneId(", Step.METHODNAME_GET_LANE_ID, "())").addLB();
      
      cb.addLine("if (cxo.getOrderContext() != null) {");
      cb.addLine("subworkflow.setNewOrderContext()");
      /*
       TODO Soll ein detached gestarteter Workflow den LoggingDiagnosisContext vom startenden Workflow (entspricht Parent) übernehmen?
      if (isExecutionDetached()) {
        cb.addLine("subworkflow.getOrderContext().setLoggingDiagnosisContext( cxo.getOrderContext().getLoggingDiagnosisContext() ); ");
      }*/
      cb.addLine("}");
      for (int i = 0; i < 4; i++) {
        if (orderInputSourceRef != null) {
          cb.addLine("if (subworkflow.getCustom" + i, "() == null) {");
        }
        cb.addLine("subworkflow.setCustom" + i, "(cxo.getCustom" + i, "())");
        if (orderInputSourceRef != null) {
          cb.addLine("}");
        }
      }
      if (isExecutionDetached()) {
        cb.addLine("(("+ OrderContextServerExtension.class.getName() + ") subworkflow.getOrderContext()).set("+ OrderContextServerExtension.class.getName() + ".CREATION_ROLE_KEY, cxo.getCreationRole())");
      }
      
      cb.addLine("}"); // subworkflow == null
      cb.addLine("}").addLB();
      
    }
    
  }
  
  private void generateCodeSubworkflowCallInputs(CodeBuffer cb, List<VariableIdentification> inputVarsForWFCall,
                                                 HashSet<String> importedClassesFqStrings) {
    if (inputVarsForWFCall.size() == 1) {
      AVariable v = inputVarsForWFCall.get(0).variable;
      String qualifiedVarName = inputVarsForWFCall.get(0).getScopeGetter(getParentScope()) + v.getVarName();
      if (isExecutionDetached() && v.isList()) {
        if (v instanceof ExceptionVariable) {
          cb.add("new ", GeneralXynaObjectList.class.getSimpleName(), "(", qualifiedVarName + " == null ? null : " + qualifiedVarName
              + ", ", v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ".class)");
        } else {
          cb.add("new ", XynaObjectList.class.getSimpleName(), "(", qualifiedVarName + " == null ? null : " + qualifiedVarName + ", ",
                 v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ".class)");
        }
      } else {
        if (isExecutionDetached()) {
          qualifiedVarName = qualifiedVarName + " == null ? null : " + qualifiedVarName + ".clone()";
        }
        if (v.isList()) {
          if (v instanceof ExceptionVariable) {
            cb.add("new ", GeneralXynaObjectList.class.getSimpleName(), "(", qualifiedVarName, ", ",
                   v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ".class)");
          } else {
            cb.add("new ", XynaObjectList.class.getSimpleName(), "(", qualifiedVarName, ", ",
                   v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ".class)");
          }
        } else {
          cb.add(qualifiedVarName);
        }
      }
    } else {
      cb.add("new ", Container.class.getSimpleName(), "(");
      for (VariableIdentification vi : inputVarsForWFCall) {
        AVariable v = vi.variable;
        String qualifiedVarName = vi.getScopeGetter(getParentScope()) + v.getVarName();
        if (isExecutionDetached()) {
          if (v.isList) {
            qualifiedVarName = qualifiedVarName + " == null ? null : " + qualifiedVarName;
          } else {
            qualifiedVarName = qualifiedVarName + " == null ? null : " + qualifiedVarName + ".clone()";
          }
        }
        if (v.isList()) {
          //TODO redundante xynaobjectlist weg. membervar von process ist bereits xynaobjectlist
          if (v instanceof ExceptionVariable) {
            cb.addListElement("new " + GeneralXynaObjectList.class.getSimpleName() + "(" + qualifiedVarName + ", "
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
          } else {
            cb.addListElement("new " + XynaObjectList.class.getSimpleName() + "(" + qualifiedVarName + ", "
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
          }
        } else {
          cb.addListElement(qualifiedVarName);
        }
      }
      cb.add(")");
    }
  }
  
  private void generateJavaForDOMRef(ServiceIdentification service, CodeBuffer cb,
                                     HashSet<String> importedClassesFqStrings)
                  throws XPRC_MissingContextForNonstaticMethodCallException, XPRC_OperationUnknownException,
                  XPRC_InvalidVariableIdException, XPRC_InvalidServiceIdException {
    
    
    Operation ops = service.service.getDom().getOperationByName(operationName);
    
    cb.addLine("/*  " + GenerationBase.escapeForCodeGenUsageInComment(label) + "  */");
    cb.add("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName(), "<"
        + getParentScope().getClassName(), "> ");
    
    final boolean isStartDocumentContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STARTDOCUMENTCONTEXT);
    final boolean isStartGenericContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STARTGENERICCONTEXT);
    final boolean isStopGenericContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STOPGENERICCONTEXT);
    if (ops instanceof WorkflowCallServiceReference) {
      if (isExecutionDetached()) {
        cb.add("implements ", DetachedCall.class.getSimpleName());
      } else {
        cb.add("implements ", SubworkflowCall.class.getSimpleName());
      }
    } else {
      cb.add("implements ", JavaCall.class.getSimpleName());
      if (isExecutionDetached()) {
        cb.add(", ", DetachedCall.class.getSimpleName());
      }
      if (isStartDocumentContext) {
        cb.add(", ", StartVariableContextStep.class.getSimpleName());
      }
      if (isStartGenericContext) {
        cb.add(", ", GenericInputAsContextStep.class.getSimpleName());
      }
    }
    cb.add(" {").addLB(2);
    
    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calcSerialVersionUID()), "L");
    
    final boolean specialPurposeWaitOrSuspend = ops.isSpecialPurpose(SpecialPurposeIdentifier.WAIT, SpecialPurposeIdentifier.SUSPEND);
    if (specialPurposeWaitOrSuspend) {
      
      cb.addLine("private Long ", VARNAME_suspensionTime);
      cb.addLine("private Long ", VARNAME_resumeTime);
      
      cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
      cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
      cb.addLine(VARNAME_suspensionTime, " = null");
      cb.addLine(VARNAME_resumeTime, " = null");
      cb.addLine("}").addLB();
      
    }
    
    final boolean specialPurposeAwaitSynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_AWAIT);
    final boolean specialPurposeLongRunningAwaitSynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_LONG_AWAIT);
    final boolean specialPurposeNotifySynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_NOTIFY);
    if (specialPurposeAwaitSynchronization || specialPurposeLongRunningAwaitSynchronization) {
      
      cb.addLine("private Long ", VARNAME_firstWaitOrNotifyTime).addLB();
      
      cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
      cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
      cb.addLine(VARNAME_firstWaitOrNotifyTime, " = null");
      cb.addLine("}").addLB();
      
    }
    
    cb.addLine("public ", getClassName(), "() {").addLine("super(" + getIdx(), ")").addLine("}").addLB();
    
    if (isExecutionDetached) {
      cb.addLine("private long childOrderId = -1");
      cb.addLine("private " + XynaOrderServerExtension.class.getSimpleName() + " subworkflow").addLB();
      cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
      cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
      cb.addLine("subworkflow = null");
      cb.addLine("childOrderId = -1");
      cb.addLine("}").addLB();
      
      cb.addLine("public long ", METHODNAME_DETACHED_CALL_GET_CHILD_ORDER_ID, "() {");
      cb.addLine("if (childOrderId < 0) {");
      //subworkflow wurde offenbar noch nicht erstellt, weil damit die id-belegung einher geht.
      cb.addLine(FUNCNAME_CREATE_ORDER_FOR_DETACHED_SERVICE_CALL, "()");
      cb.addLine("}");
      cb.addLine("return childOrderId");
      cb.addLine("}").addLB();
      cb.addLine("public List<", XynaOrderServerExtension.class.getSimpleName(), "> getChildOrders() {");
      cb.addLine("return null");
      cb.addLine("}").addLB();
      
    } else if (ops instanceof WorkflowCallServiceReference) {
      cb.addLine("private " + XynaOrderServerExtension.class.getSimpleName() + " subworkflow").addLB();
      //subworkflowcall interface
      cb.addLine("public " + XynaOrderServerExtension.class.getSimpleName() + " ", METHODNAME_SUBWF_CALL_GET_CHILD_ORDER, "() {");
      cb.addLine("return subworkflow").addLine("}").addLB();
      
      cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
      cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
      cb.addLine("subworkflow = null");
      cb.addLine("}").addLB();
    } else if (!isSpecialPurposeOpsWithReinitializeBlock(ops)) {
      cb.addLine("private ", ChildOrderStorage.class.getSimpleName(), " ", VARNAME_CHILDORDERSTORAGE);
      cb.addLB();
      cb.addLine("public void ", METHODNAME_INIT, "(", getParentScope().getClassName(), " p) {");
      cb.addLine("super.", METHODNAME_INIT, "(p)");
      cb.addLine(VARNAME_CHILDORDERSTORAGE, " = new ", ChildOrderStorage.class.getSimpleName(), "(this)");
      cb.addLine("}").addLB();
      cb.addLine("public List<", XynaOrderServerExtension.class.getSimpleName(), "> getChildOrders() {");
      cb.addLine("return ", VARNAME_CHILDORDERSTORAGE,".", METHODNAME_GET_XYNA_CHILD_ORDERS, "()");
      cb.addLine("}").addLB();
    } else {
      cb.addLine("public List<", XynaOrderServerExtension.class.getSimpleName(), "> getChildOrders() {");
      cb.addLine("return null");
      cb.addLine("}").addLB();
    }
    
    if (isStartDocumentContext | isStartGenericContext) {
      cb.addLine("private " + XynaObject.class.getSimpleName() + " localContextVariable;").addLB();
      
      cb.addLine("public " + XynaObject.class.getSimpleName() + " ", METHODNAME_GET_CONTEXT_VARIABLE, "() {");
      cb.addLine("return localContextVariable");
      cb.addLine("}").addLB();
      
      cb.addLine("public void ", METHODNAME_CLEAR_CONTEXT_VARIABLE, "() {");
      cb.addLine("localContextVariable = null");
      cb.addLine("}").addLB();
    }
    
    if (isStartGenericContext || isStopGenericContext) {
      cb.addLine("public " + String.class.getSimpleName() + " ", METHODNAME_GET_CONTEXT_IDENTIFIER, "() {");
      String contextIdentifier = "generic";
      if (ops.getSpecialPurposeAttributes() != null &&
          ops.getSpecialPurposeAttributes().containsKey(SPECIAL_PURPOSES.GENERICCONTEXT_IDENTIFICATION_ATT)) {
        contextIdentifier = ops.getSpecialPurposeAttributes().get(SPECIAL_PURPOSES.GENERICCONTEXT_IDENTIFICATION_ATT);
      }
      cb.addLine("return \"",contextIdentifier,"\"");
      cb.addLine("}").addLB();
    }
    
    cb.add("public String ", METHODNAME_GET_LABEL, "() { return \"" + GenerationBase.escapeForCodeGenUsageInString(label) + "\"; }")
        .addLB();
    
    appendExecuteInternally(cb, importedClassesFqStrings);
    
    // compensation
    int childrenIdx = 0;
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    if (isExecutionDetached()) {
      cb.addLine("// nothing to be done, execution is detached and cannot be compensated");
    } else {
      if (compensateStep != null) {
        cb.addLine("executeChildren(" + (childrenIdx++), ")");
      } else if (ops instanceof WorkflowCallServiceReference) {
        cb.addLine("// perform compensation");
        cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION, ".", METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC, "(subworkflow)");
      } else if (isRemoteCall()) {
        //ntbd: remote compensate wird derzeit nicht unterstützt
      } else {
        if (!ops.isStatic()) {
          /*
           * kann in abgeleitetem objekt überschrieben sein. also zur laufzeit checken, was für ein typ die operation hat!
           * wf soll nicht neu generiert werden müssen, wenn neue subklassen erzeugt werden
           * => man muss zur laufzeit herausfinden können, wie methode x in klasse y implementiert ist.
           * => das findet man heraus, indem man im datentyp nachschaut. xml ist ineffizient, deshalb 
           *    entweder beim ondeployment direkt merken, oder später lazy nachladen
           * => beim ondeployment ist am einfachsten!
           */
          VariableIdentification vi = getParentScope().identifyVariable(input.getVarIds()[0]);
          AVariable v = vi.getVariable();
          //in FractalProcessStep implementiert
          cb.addLine("if (isWorkflowCall(", vi.getScopeGetter(getParentScope()), v.getVarName(), ", \"", ops.getName(), "\")) {");
        }
        //sollte nur ein subauftrag sein (+ seine kinder sein)
        cb.addLine("List<", XynaOrderServerExtension.class.getSimpleName(), "> childOrders = getChildOrders()");
        cb.addLine("if (childOrders != null) {");
        cb.addLine("for (int i = childOrders.size() - 1; i >= 0; i--) {");
        cb.addLine(XynaOrderServerExtension.class.getSimpleName(), " childOrder = childOrders.get(i)");
        cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION, ".", METHODNAME_PROCESSING_COMPENSATE_ORDER_SYNC, "(childOrder)");
        cb.addLine("}");
        cb.addLine("}");
        if (!ops.isStatic()) {
          cb.addLine("}");
        }
      }
    }
    cb.addLine("}").addLB();
    
    if (isExecutionDetached()) {
      List<VariableIdentification> inputVarsForDetachedServiceOperationCall = new ArrayList<VariableIdentification>();
      for (String id : input.getVarIds()) {
        inputVarsForDetachedServiceOperationCall.add(getParentScope().identifyVariable(id));
      }
      createCodeForLazyCreateDetachedServiceOperationWf(cb, service.service, importedClassesFqStrings);
      createSetInputVarsMethodForDetachedCall(cb, service, ops, inputVarsForDetachedServiceOperationCall, importedClassesFqStrings);
    }
    
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, input.getVarIds(), input.getPaths(), cb,
                                          importedClassesFqStrings);
    if (isExecutionDetached()) {
      generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, null, null, cb,
                                            importedClassesFqStrings);
    } else {
      generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, receiveVarIds, receivePaths, cb,
                                          importedClassesFqStrings);
    }
    
    generatedGetRefIdMethod(cb);
    
    // getChildren
    childrenIdx = 0;
    cb.addLine("protected ", FractalProcessStep.class.getSimpleName(), "<", getParentScope().getClassName(),
               ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    if (compensateStep != null) {
      cb.addLine("if (i == " + (childrenIdx++) + ") {");
      cb.addLine("return new ", FractalProcessStep.class.getSimpleName(), "[]{", METHODNAME_GET_PARENT_SCOPE, "().",
                 compensateStep.getVarName(), "};");
      cb.addLine("}");
    }
    cb.addLine("return null").addLine("}").addLB();
    // getChildrenLength
    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return " + childrenIdx);
    cb.addLine("}").addLB();
    cb.addLine("}").addLB();
  }
  
  private boolean isSpecialPurposeOpsWithReinitializeBlock(Operation ops) {
    return ops.isSpecialPurpose(SpecialPurposeIdentifier.WAIT, SpecialPurposeIdentifier.SUSPEND,
                                SpecialPurposeIdentifier.SYNC_AWAIT, SpecialPurposeIdentifier.SYNC_LONG_AWAIT);
  }
  
  private static final XynaPropertyBoolean logClassLoader = new XynaPropertyBoolean("xprc.xfractwfe.generation.classloadinginfo.include", false).setHidden(true);
  
  protected void appendExecuteInternallyForDOMRef(ServiceIdentification service, CodeBuffer cb,
                                                  HashSet<String> importedClassesFqStrings)
      throws XPRC_OperationUnknownException, XPRC_MissingContextForNonstaticMethodCallException,
      XPRC_InvalidVariableIdException {
    Operation ops = service.service.getDom().getOperationByName(operationName);
    
    final boolean isStartDocumentContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STARTDOCUMENTCONTEXT);
    final boolean isStartGenericContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STARTGENERICCONTEXT);
    final boolean specialPurposeAwaitSynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_AWAIT);
    final boolean specialPurposeLongRunningAwaitSynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_LONG_AWAIT);
    final boolean specialPurposeNotifySynchronization = ops.isSpecialPurpose(SpecialPurposeIdentifier.SYNC_NOTIFY);
    final boolean specialPurposeWaitOrSuspend = ops.isSpecialPurpose(SpecialPurposeIdentifier.WAIT, SpecialPurposeIdentifier.SUSPEND);
    final boolean isStopGenericContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STOPGENERICCONTEXT);
    
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws " + XynaException.class.getSimpleName() + " {").addLB();
    
    if (isStartDocumentContext) {
      cb.addLine("localContextVariable = new xact.templates.Document()").addLB();
    }
    
    if (isStartGenericContext) {
      cb.addLine("localContextVariable = ", wrapInSingleXynaObject( "", input.getVarIds(), input.getPaths(), true));
      
    }
    
    if (specialPurposeAwaitSynchronization || specialPurposeLongRunningAwaitSynchronization) {
      cb.addLine("if (", VARNAME_firstWaitOrNotifyTime, " == null) {");
      cb.addLine(VARNAME_firstWaitOrNotifyTime, " = System.currentTimeMillis()");
      cb.addLine("}").addLB();
    }
    
    if (ops.isStepEventListener()) {
      cb.addLine(Step.METHODNAME_INIT_EVENT_SOURCE, "()");
      cb.addLine("try {");
    }
    
    final boolean isSendDocument = ops.isSpecialPurpose(SpecialPurposeIdentifier.RETRIEVEDOCUMENT);
    final boolean isStopDocumentContext = ops.isSpecialPurpose(SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT);
    if (isSendDocument || isStopDocumentContext) {
      cb.addLine(StartVariableContextStep.class.getSimpleName() + " startDocumentStep = ", METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK, "(" + 
                 StartVariableContextStep.class.getSimpleName() + ".class, new " + FractalProcessStepFilter.class.getSimpleName() + 
                 "<" + StartVariableContextStep.class.getSimpleName() + ">() {");
      cb.addLine("public boolean ", Step.METHODNAME_STEP_FILTER_MATCHES, "(" + StartVariableContextStep.class.getSimpleName() + " step) {");
      cb.addLine("return step.", Step.METHODNAME_GET_CONTEXT_VARIABLE, "() != null;");
      cb.addLine("}");
      cb.addLine("});");
    }
    if (isStopGenericContext) {
      cb.addLine(GenericInputAsContextStep.class.getSimpleName() + " startGenericStep = ", METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK, "(" + 
                      GenericInputAsContextStep.class.getSimpleName() + ".class, new " + FractalProcessStepFilter.class.getSimpleName() + 
                      "<" + GenericInputAsContextStep.class.getSimpleName() + ">() {");
           cb.addLine("public boolean ", Step.METHODNAME_STEP_FILTER_MATCHES, "(" + GenericInputAsContextStep.class.getSimpleName() + " step) {");
           cb.addLine("return step.", Step.METHODNAME_GET_CONTEXT_VARIABLE, "() != null && ",
                             "step.", Step.METHODNAME_GET_CONTEXT_IDENTIFIER, "().equals(",Step.METHODNAME_GET_CONTEXT_IDENTIFIER,"())");
           cb.addLine("}");
           cb.addLine("});");
    }
    
    if (ops.isStatic() && logClassLoader.get()) {
      boolean skipFirstVar = !ops.isStatic() && service.service.getVariable() == null;
      logClassLoaderFor(cb, service.service.getEventuallyQualifiedClassName(importedClassesFqStrings) + ".class");
      String[] vars = listVarIdsAsArray(input.getVarIds(), input.getPaths(), false, skipFirstVar, false);
      for (String var : vars) {
        logClassLoaderFor(cb, var + ".getClass()");
        logClassLoaderFor(cb, service.service.getEventuallyQualifiedClassName(importedClassesFqStrings) + ".class", var + ".getClass()");
      }
      
      vars = listVarIdsAsArray(receiveVarIds, receivePaths, false, false, false);
      for (String var : vars) {
        logClassLoaderFor(cb, var + ".getClass()");
        logClassLoaderFor(cb, service.service.getEventuallyQualifiedClassName(importedClassesFqStrings) + ".class", var + ".getClass()");
      }
    }
    
    // variablenzuweisung
    if (ops instanceof WorkflowCallServiceReference) {
      cb.add(XynaOrderServerExtension.class.getSimpleName() + " xo = ");      
    } else if (!isExecutionDetached()) {
      if (!isSpecialPurposeOpsWithReinitializeBlock(ops)) {
        cb.addLine(ChildOrderStorageStack.class.getSimpleName(), " ", VARNAME_CHILDORDERSTORAGE_STACK, " = ",
                   ChildOrderStorage.class.getSimpleName(), ".", FIELDNAME_CHILD_ORDER_STORAGE_STACK, ".get()");
        cb.addLine(VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD, "(", VARNAME_CHILDORDERSTORAGE, ")");
        cb.addLine("try {");
      }
      if (receiveVarIds.length == 0) {
      } else if (receiveVarIds.length == 1) {
        cb.add("Object temp = ");
      } else {
        cb.add(Container.class.getSimpleName() + " c = ");
      }
    }
    
    if (isExecutionDetached()) {
      // this clones the variables
      cb.addLine(FUNCNAME_INITIALIZE_DETACHED_SUB_WF_XYNAORDER + "()");
      cb.addLine(PATH_TO_XPRC_CTRL_EXECUTION + ".", METHODNAME_START_ORDER, "(subworkflow, new "
                      + EmptyResponseListener.class.getSimpleName() + "(), subworkflow.getOrderContext())");
      cb.addLine("// No response to be evaluated, execution is detached");
      cb.addLine("subworkflow = null");
    } else {
      if (specialPurposeWaitOrSuspend) {
        cb.addLine(AtomicLong.class.getName() + " " +VARNAME_resumeTime+"AL = new " + AtomicLong.class.getName() + "()");
        cb.addLine("if( "+VARNAME_resumeTime+" != null ) {")
          .addLine(VARNAME_resumeTime+"AL.set("+VARNAME_resumeTime+".longValue())")
          .addLine("}");
        cb.addLine("long now = System.currentTimeMillis()");
        cb.addLine("try {");
      } // end special purpose wait/suspend
      
      if (ops.isStatic()) {
        cb.add(service.service.getEventuallyQualifiedClassName(importedClassesFqStrings) + ".");
      } else if (service.service.getVariable() != null) {
        // legacy
        // statische oder nicht statische variable. das entscheidet sich bei der variablen deklaration
        cb.add(service.getScopeGetter(getParentScope()) + service.service.getVariable().getVarName() + ".");
      } else {
        if (isRemoteCall()) {
          if (receiveVarIds.length > 1) {
            cb.add("(").add(Container.class.getSimpleName()).add(") ");
          }
          cb.add(service.getScopeGetter(getParentScope()) + Service.generateRemoteCallVarName(this) + "."); 
        } else {
          if (input.getVarIds().length == 0) {
            //FIXME bessere fehlermeldung
            throw new XPRC_MissingContextForNonstaticMethodCallException(getOperationName(), service.service.getFQClassName());
          }
          
          //erster parameter ist die instanz auf die die methode aufgerufen wird
          VariableIdentification vi = getParentScope().identifyVariable(input.getVarIds()[0]);
          AVariable v = vi.variable;
          cb.add(vi.getScopeGetter(getParentScope()) + v.getVarName() + ".");
        }
      }
      cb.add(operationName + "(");
      if (ops instanceof JavaOperation) {
        if (((JavaOperation) ops).requiresXynaOrder()) {
          cb.addListElement(METHODNAME_GET_PROCESS + "()." + WF.METHODNAME_GET_CORRELATED_XYNA_ORDER + "()");
        }
      }
      if (ops instanceof WorkflowCallServiceReference) {
        cb.addListElement("this");
      }
      if (isSendDocument || isStopDocumentContext) {
        cb.addListElement("(xact.templates.Document)startDocumentStep." + Step.METHODNAME_GET_CONTEXT_VARIABLE + "()");
      }
      if (isRemoteCall()) {
        String indent = "\n               ";
        cb.addListElement(indent+METHODNAME_GET_LANE_ID + "()");
        cb.addListElement(indent+"\""+ remoteDispatchingParameter.remoteOrdertype +"\"");
        cb.addListElement(indent+"XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject(\""+ remoteDispatchingParameter.remoteOrdertype + "\", RevisionManagement.getRevisionByClass(getClass())))");
        cb.addListElement(indent+"\""+ remoteDispatchingParameter.remoteDestination +"\"");
        cb.addListElement(indent+wrapInSingleXynaObject(indent, remoteDispatchingParameter.invokeVarIds, remoteDispatchingParameter.invokePaths, false));
        cb.addListElement(indent+wrapInSingleXynaObject(indent, input.getVarIds(), input.getPaths(), false));
      } else if (input.length() > 0) {
        //kein remoteCall, alle Parameter normal übergeben
        boolean skipFirstVar = !ops.isStatic() && service.service.getVariable() == null;
        String lvi = listVarIds("\n           ", input.getVarIds(), input.getPaths(), false, skipFirstVar, false);
        if( lvi.length() > 0 ) {
          cb.addListElement(lvi);
        }
      }
      
      // add special parameters
      if (specialPurposeAwaitSynchronization || specialPurposeNotifySynchronization || specialPurposeLongRunningAwaitSynchronization) {
        cb.addListElement(getIdx() + "");
        if (specialPurposeAwaitSynchronization || specialPurposeLongRunningAwaitSynchronization) {
          cb.addListElement(VARNAME_firstWaitOrNotifyTime);
        }
        cb.addListElement(METHODNAME_GET_LANE_ID + "()");
      }
      if (specialPurposeWaitOrSuspend) {
        cb.addListElement(VARNAME_suspensionTime);
        cb.addListElement(VARNAME_resumeTime+"AL");
        cb.addListElement(METHODNAME_GET_LANE_ID + "()");
      }
      
      cb.add(")").addLB();
      
      if (specialPurposeWaitOrSuspend) {
        cb.addLine("} finally {");
        cb.addLine(VARNAME_suspensionTime + " = "+VARNAME_suspensionTime+"==null?now:"+VARNAME_suspensionTime);
        cb.addLine(VARNAME_resumeTime+" = "+VARNAME_resumeTime+"AL.get()");
        cb.addLine("}");
      }
      
      if (isExecutionDetached()) {
        cb.addLine("// No response to be evaluated, execution is detached");
      } else {
        // in the case of a service call it is possible that a resulting container is null due to bad implementation
        if (!(ops instanceof WorkflowCallServiceReference) && receiveVarIds.length > 1) {
          cb.addLine("if (c == null) {");
          cb.addLine("throw new " + XynaException.class.getSimpleName()
                          + "(\"Implementation error: Service call may not return null, expected "
                          + Container.class.getSimpleName() + "\")");
          cb.addLine("}");
        }
        
        if (isStartDocumentContext) {
          for (int i = 0; i < input.length(); i++) {
            if (!ops.isStatic() && service.service.getVariable() == null && i == 0) {
              //erster parameter ist bei instanzmethoden das objekt, auf dem die methode aufgerufen wird
              continue;
            }
            VariableIdentification vi = getParentScope().identifyVariable(input.getVarIds()[i]);
            AVariable v = vi.variable;       
            if (extendsDocumentType(v)) {
              cb.addLine("((xact.templates.Document)localContextVariable).setDocumentType(" + vi.getScopeGetter(getParentScope()) + v.getGetter(input.getPaths()[i]) + ")");
            }
          }
        }
        if (isStopDocumentContext) {
          cb.addLine("startDocumentStep.", METHODNAME_CLEAR_CONTEXT_VARIABLE, "()");
        }
        if (isStopGenericContext) {
          cb.addLine("startGenericStep.", METHODNAME_CLEAR_CONTEXT_VARIABLE, "()");
        }
        
        // ggfs container auseinanderklamüsern
        if (ops instanceof WorkflowCallServiceReference) {
          if (compensateStep == null) {
            cb.addLine("subworkflow = xo");
          }
          if (receiveVarIds.length == 1) {
            cb.add(GeneralXynaObject.class.getSimpleName() + " temp = ");
          } else {
            cb.add(Container.class.getSimpleName(), " c = ");
          }
          cb.add("xo.", METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()").addLB();
        }
        
        appendResultSetter(cb, importedClassesFqStrings, ops instanceof WorkflowCallServiceReference);
        
        if (!(ops instanceof WorkflowCallServiceReference) && !isExecutionDetached() && !isSpecialPurposeOpsWithReinitializeBlock(ops)) {
          cb.addLine("} finally {");
          cb.addLine(VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE, "()");
          cb.addLine("}");
        }
      }
      
    }
    
    
    if (ops.isStepEventListener()) {
      cb.addLine("} finally {");
      cb.addLine(Step.METHODNAME_CLEAR_EVENT_SOURCE, "()");
      cb.addLine("}");
    }
    cb.addLine("}").addLB(); // end executeInternally()
    
  }
    
  private void logClassLoaderFor(CodeBuffer cb, String servicegroupClazz, String inputClazz) {
    logClassLoaderFor(cb, servicegroupClazz + ".getClassLoader().loadClass(" + inputClazz + ".getName())");
  }
  
  private void logClassLoaderFor(CodeBuffer cb, String clazz) {
    cb.addLine("try {");
    cb.addLine("Class<?> _logclass = ", clazz);
    cb.addLine("while (_logclass != XynaObject.class) {");
    cb.addLine("logger.warn(\"", clazz, ": \" + _logclass.getName() + \".classloader=\" + ((", ClassLoaderBase.class.getName(),
               ") _logclass.getClassLoader()).getExtendedDescription(false))");
    cb.addLine("_logclass = _logclass.getSuperclass()");
    cb.addLine("}"); //while
    cb.addLine("} catch (Exception e) {");
    cb.addLine("logger.warn(null, e)");
    cb.addLine("}");
  }
  
  private String wrapInSingleXynaObject(String indent, String[] varIds, String[] paths, boolean clone) throws XPRC_InvalidVariableIdException {
    switch( varIds.length ) {
    case 0: 
      return "new " + Container.class.getSimpleName() + "()";
    case 1:
      return listVarIds("", varIds, paths, true, false, clone);
    default:
      StringBuilder sb = new StringBuilder("new " + Container.class.getSimpleName() + "(");
      sb.append(listVarIds(indent+"  ", varIds, paths, true, false, clone) );
      sb.append(indent).append(" )");
      return sb.toString();
    }
  }
  
  private String[] listVarIdsAsArray(String[] varIds, String[] paths, boolean convertLists, boolean skipFirstInput, boolean clone) throws XPRC_InvalidVariableIdException {
    List<String> ret = new ArrayList<>();
    for (int i = 0; i < varIds.length; i++) {
      if (skipFirstInput && i == 0) {
        //erster parameter ist bei instanzmethoden das objekt, auf dem die methode aufgerufen wird
        continue;
      }
      StringBuilder sb = new StringBuilder();
      VariableIdentification vi = getParentScope().identifyVariable(varIds[i]);
      AVariable v = vi.variable;
      if( v.isList() ) {
        if( convertLists || clone ) {
          sb.append("new ").append(GeneralXynaObjectList.class.getSimpleName()).append("(")
          .append(vi.getScopeGetter(getParentScope())).append(v.getGetter(paths[i]))
          .append(", ").append(vi.getVariable().getFQClassName()).append(".class)");
        } else {
          sb.append("(").append(List.class.getSimpleName()).append(") ").append(vi.getScopeGetter(getParentScope())).append(v.getGetter(paths[i]));
        }
      } else {
        sb.append(vi.getScopeGetter(getParentScope())).append(v.getGetter(paths[i]));
        if (clone) {
          sb.append(".clone()");
        }
      }
      ret.add(sb.toString());
    }
    return ret.toArray(new String[0]);
  }
  
  private String listVarIds(String indent, String[] varIds, String[] paths, boolean convertLists, boolean skipFirstInput, boolean clone) throws XPRC_InvalidVariableIdException {
    StringBuilder sb = new StringBuilder();
    String[] vars = listVarIdsAsArray(varIds, paths, convertLists, skipFirstInput, clone);
    String sep = "";
    for (int i = 0; i < vars.length; i++) {
      sb.append(sep).append(indent);
      sb.append(vars[i]);
      sep = ", ";
    }
    return sb.toString();
  }
  
  private boolean extendsDocumentType(AVariable v) {
    DomOrExceptionGenerationBase obj = v.getDomOrExceptionObject();
    while (obj != null) {
      if ("xact.templates.DocumentType".equals(obj.getFqClassName())) {
        return true;
      }
      obj = obj.getSuperClassGenerationObject();
    }
    return false;
  }
  
  private void appendResultSetter(CodeBuffer cb, HashSet<String> importedClassesFqStrings, boolean instanceOfWorkflowCallServiceReference)
      throws XPRC_InvalidVariableIdException {
    if (receiveVarIds.length > 0) {
      cb.addLine("int successfulCastCnt = 0");
      cb.addLine("try {");
    }
    if (receiveVarIds.length == 1) {
      VariableIdentification vi = getParentScope().identifyVariable(receiveVarIds[0]);
      AVariable v = vi.variable;
      if (instanceOfWorkflowCallServiceReference && v.isList()) {
        cb.addLine("if (temp == null) {");
        cb.addLine(vi.getScopeGetter(getParentScope()), v.getSetter("null", receivePaths[0]));
        cb.addLine("} else {");
        if (v instanceof ExceptionVariable) {
          cb.addLine(vi.getScopeGetter(getParentScope()),
                     v.getSetter("((" + GeneralXynaObjectList.class.getSimpleName() + "<"
                                     + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ">) temp).getList()",
                                 receivePaths[0]));
        } else {
          cb.addLine(vi.getScopeGetter(getParentScope()),
                     v.getSetter("((" + XynaObjectList.class.getSimpleName() + "<"
                                     + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ">) temp).getList()",
                                 receivePaths[0]));
        }
        cb.addLine("}");
      } else {
        cb.addLine(vi.getScopeGetter(getParentScope()),
                   v.getSetter("(" + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + ") temp", receivePaths[0]));
      }
      cb.addLine("successfulCastCnt = 1");
    } else if (receiveVarIds.length > 1) {
      for (int i = 0; i < receiveVarIds.length; i++) {
        
        // catch the case in which the target is not used anywhere
        // FIXME test this case
        if (GenerationBase.isEmpty(receiveVarIds[i])) {
          continue;
        }
        
        VariableIdentification vi = getParentScope().identifyVariable(receiveVarIds[i]);
        AVariable v = vi.variable;
        if (v.isList()) {
          cb.addLine("if (c.get(" + i, ") == null) {");
          cb.addLine(vi.getScopeGetter(getParentScope()), v.getSetter("null", receivePaths[i]));
          cb.addLine("} else {");
          if (v instanceof ExceptionVariable) {
            cb.addLine(vi.getScopeGetter(getParentScope()),
                       v.getSetter("((" + GeneralXynaObjectList.class.getSimpleName() + "<"
                                       + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
                                       + ">) c.get(" + i + ")).getList()", receivePaths[i]));
          } else {
            cb.addLine(vi.getScopeGetter(getParentScope()),
                       v.getSetter("((" + XynaObjectList.class.getSimpleName() + "<"
                                       + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
                                       + ">) c.get(" + i + ")).getList()", receivePaths[i]));
          }
          cb.addLine("}");
        } else {
          String containerGetter = "c.get(" + i + ")";
          boolean isException = Step.isExceptionAndNoXynaObject(v);
          if (isException) {
            cb.addLine("if (", containerGetter, " == null) {");
            cb.addLine(vi.getScopeGetter(getParentScope()), v.getSetter("null", receivePaths[i]));
            cb.addLine("} else {");
            containerGetter = "((" + XynaExceptionContainer.class.getName() + ")" + containerGetter + ").getException()";
          }
          cb.addLine(vi.getScopeGetter(getParentScope()),
                     v.getSetter("(" + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + ") " + containerGetter,
                                 receivePaths[i]));
          
          if (isException) {
            cb.addLine("}");
          }
        }
        cb.addLine("successfulCastCnt = " + (i+1));
      }
    }
    //exception mit der man etwas anfangen kann schmeissen
    if (receiveVarIds.length > 0) {
      cb.addLine("} catch (", ClassCastException.class.getName(), " _ccex) {");
      cb.addLine("String _fromType_");
      cb.addLine("String _toType_");
      cb.addLine("switch (successfulCastCnt) {");
      for (int i = 0; i < receiveVarIds.length; i++) {
        cb.add("case " + i, " : "); //TODO unschöner code, um zu verhindern, dass da ein semikolon zuviel ist
        
        if (GenerationBase.isEmpty(receiveVarIds[i])) {
          cb.addLine("throw new ", RuntimeException.class.getName(), "(_ccex)"); //sollte nicht vorkommen, weil successfulCastCnt diesen wert nicht erreichen kann
          continue;
        }
        
        VariableIdentification vi = getParentScope().identifyVariable(receiveVarIds[i]);
        AVariable v = vi.variable;
        if (receiveVarIds.length == 1) {
          cb.addLine("_fromType_ = temp == null ? \"null\" : temp.getClass().getName()");
        } else {
          cb.addLine("_fromType_ = c == null || c.size() <= " + i, " || c.get(" + i,
                     ") == null ? \"null\" : c.get(" + i, ").getClass().getName()");
        }
        cb.addLine("_toType_ = ", v.getClassName(false, importedClassesFqStrings), ".class.getName()");
        cb.addLine("throw (", ClassCastException.class.getName(), ") new ", ClassCastException.class.getName(),
                   "(\"Could not cast " + (i + 1),
                   ". output parameter (of type \" + _fromType_ + \") at invocation of operation '", operationName,
                   "' to \" + _toType_ + \".\").initCause(_ccex)");
      }
      //wenn i >= receiveVarIds.length, dann sind alle variablen erfolgreich gecastet worden. woher kommt dann die classcastexception?
      cb.addLine("default : throw new ", RuntimeException.class.getName(), "(null, _ccex)");
      cb.addLine("}");
      cb.addLine("}");
    }
  }
  
  @Override
  protected List<GenerationBase> getDependencies() {
    //normalerweise muss man hier nicht den aufgerufenen workflow zurückgeben, weil er in einer servicereference auf der nächsthöheren stepserial-ebene referenziert wird
    //es gab aber offenbar mal einen bug, wo die servicereference-objekte im xml nur innerhalb des function-objekts lagen. 
    //es schadet auch nichts, die workflows hier auch zurückzugeben
    ServiceIdentification s;
    try {
      s = getParentScope().identifyService(serviceId);
      if (isRemoteCall()) {
        return Arrays.asList(getRemoteCallServiceIdentification().service.getDom(), s.service.getWF());
      } else {
        if (!s.service.isDOMRef()) {
          if (s.service.getWF() != null) {
            return Collections.<GenerationBase> singletonList(s.service.getWF());
          }
        }
      }
    } catch (XPRC_InvalidServiceIdException e) {
      //wird woanders gecheckt
    }
    return null;
  }
  
  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return null;
  }
  
  @Override
  public List<ExceptionVariable> getAllThrownExceptions(boolean considerRetryAsHandled) {
    if (isPrototype()) {
      return new ArrayList<ExceptionVariable>();
    }
    
    Service service;
    try {
      service = getParentScope().identifyService(getServiceId()).service;
    } catch (XPRC_InvalidServiceIdException e) {
      if (serviceReference != null) {
        service = serviceReference;
      } else {
        logger.error(e);
        throw new RuntimeException("Could not identify service.", e);
      }
    }

    if (service.isDOMRef()) {
      try {
        return service.getDom().getOperationByName(getOperationName()).getThrownExceptions();
      } catch (XPRC_OperationUnknownException e) {
        logger.error(e);
        //throw new RuntimeException("Could not identify operation.", e);
        return Collections.emptyList();
      }
    } else {
      return service.getWF().getAllThrownExceptions();
    }
  }
  
  @Override
  protected List<Service> getServices() {
    if (serviceReference != null) {
      ArrayList<Service> list = new ArrayList<Service>();
      list.add(serviceReference);
      return list;
    }
    return null;
  }
  
  @Override
  protected List<ServiceVariable> getServiceVariables() {
    return null;
  }
  
  @Override
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }
  
  @Override
  public List<Step> getChildSteps() {
    List<Step> allSteps = new ArrayList<Step>();
/* Achtung: catchStep hat stepfunction als childstep. hier nicht angeben.  
 *  if (catchStep != null) {
      allSteps.add(catchStep);    
    }*/
    if (compensateStep != null) {
      allSteps.add(compensateStep);
    }
    return allSteps;
  }
  
  @Override
  public boolean replaceChild(Step oldChild, Step newChild) {
    if (compensateStep == oldChild) {
      compensateStep = newChild;
      return true;
    }
    
    return false;
  }
  
  @Override
  public void setCatchStep(StepCatch catchStep) {
    this.catchStep = catchStep;
  }
  
  @Override
  public Step getProxyForCatch() {
    if (catchStep != null) {
      return catchStep;
    }
    return this;
  }
  
  @Override
  public String[] getInputVarIds() {
    return input.getVarIds();
  }
  
  public void addOutputVarId(int index, String id) {
    receiveVarIds = ArrayUtils.addToArray(receiveVarIds, index, id);
    receiveVarCastToType = ArrayUtils.addToArray(receiveVarCastToType, index, null);
    receivePaths = ArrayUtils.addToArray(receivePaths, index, null);
  }
  
  public void removeOutputVarId(int index) {
    receiveVarIds = ArrayUtils.removeFromStringArray(receiveVarIds, index);
    receiveVarCastToType = ArrayUtils.removeFromStringArray(receiveVarCastToType, index);
    receivePaths = ArrayUtils.removeFromStringArray(receivePaths, index);
  }
  
  @Override
  public String[] getOutputVarIds() {
    return receiveVarIds;
  }
  
  public List<AVariable> getInputVars() {
    List<AVariable> inputVars = new ArrayList<AVariable>();
    try {
      for (String varId : getInputVarIds()) {
        if ( (varId != null) && (varId.length() > 0) ) {
          inputVars.add(getParentScope().identifyVariable(varId).getVariable());
        } else {
          inputVars.add(null);
        }
      }
    } catch (XPRC_InvalidVariableIdException e) {
      logger.error(e);
      throw new RuntimeException("Could not determine input variables for step " + this, e);
    }
    
  return inputVars;
  }
  

  public List<AVariable> getOutputVars() {
    List<AVariable> outputVars = new ArrayList<AVariable>();
    for (String varId : getOutputVarIds()) {
      if ((varId != null) && (varId.length() > 0)) {

        Step baseStep = this;

        //if we are not an abstract service, there is a StepCatch around us
        if (getParentStep() != null && getParentStep() instanceof StepCatch) {
          baseStep = getParentStep();
        }

        //if we are in a stepForeach, our output might be there
        if (baseStep.getParentStep() != null && baseStep.getParentStep().getParentStep() != null
            && baseStep.getParentStep().getParentStep().getParentStep() != null
            && baseStep.getParentStep().getParentStep().getParentStep() instanceof StepForeach) {
          boolean found = false;
          StepForeach step = ((StepForeach) baseStep.getParentStep().getParentStep().getParentStep());
          List<AVariable> stepForeachVariables = step.getOutputVarsSingle(true);
          for (AVariable v : stepForeachVariables) {
            if (v != null && v.getId() != null && v.getId().equals(varId)) {
              outputVars.add(v);
              found = true;
              break;
            }
          }

          if (found)
            continue;
        }

        VariableIdentification va = null;

        try {
          va = getParentScope().identifyVariable(varId);
          outputVars.add(va.getVariable());
        } catch (XPRC_InvalidVariableIdException e) {
          logger.error(e);
          outputVars.add(null);
          //this happens, if there is a broken workflow (there should be some output variable,
          //but the output is a prototype and there actually is none)
        }
      } else {
        outputVars.add(null);
      }
    }


    return outputVars;
  }
  
  @Override
  public String[] getInputVarPaths() {
    return input.getPaths();
  }
  
  public String[] getInputVarCastToType() {
    return input.getExpectedTypes();
  }
  
  @Override
  public String[] getOutputVarPaths() {
    return receivePaths;
  }
  
  public String[] getReceiveVarCastToType() {
    return receiveVarCastToType;
  }
  
  public boolean isQueryStorable() {
    try {
      Service service = getParentScope().identifyService(serviceId).service;
      if (service.isPrototype()) {
        return false;
      }
      
      if (service.isDOMRef()) {
        Operation operation = service.getDom().getOperationByName(getOperationName());
        if (operation.getSpecialPurposeIdentifier() == SpecialPurposeIdentifier.QUERY_STORABLE) {
          return true;
        }
      } else {
        WF wf = service.getWF();
        if(wf == null) {
          return false;
        }
        
        return wf.getSpecialPurposeIdentifier() == SpecialPurposeIdentifier.QUERY_STORABLE;
      }
    } catch (Exception e) {
      logger.error(e);
    }
    
    return false;
  }
  
  private void setServiceId(String serviceId) {
    this.serviceId = serviceId;
    creator.addXmlId(serviceId);
  }
  
  public String getServiceId() {
    return serviceId;
  }
  
  public String getOperationName() {
    return operationName;
  }
  
  @Override
  public boolean isExecutionDetached() {
    return isExecutionDetached;
  }
  
  public void setExecutionDetached(boolean isExecutionDetached) {
    this.isExecutionDetached = isExecutionDetached;
  }
  
  public boolean freesCapacities() {
    return freesCapacities;
  }
  
  public List<String> getQueryFilterConditions() {
    return queryFilterConditions;
  }
  
  public void setQueryFilterConditions(List<String> queryFilterConditions) {
    this.queryFilterConditions = queryFilterConditions;
  }
  
  public void setFreesCapacities(boolean freesCapacities) {
    if (!isFreeCapacitiesTaggable() && freesCapacities) {
      throw new RuntimeException("Step " + getStepId() + " does not support being tagged as freeing capacities.");
    }
    
    this.freesCapacities = freesCapacities;
  }
  
  public boolean isFreeCapacitiesTaggable() {
    try {
      Service service = getParentScope().identifyService(serviceId).service;
      if (service.isPrototype() || !service.isDOMRef()) {
        return false;
      }
      
      // only wait-, await- and suspend-steps can be tagged as freeing capacities
      Operation operation = service.getDom().getOperationByName(getOperationName());
      if ( (SG_FQN_WAIT_AND_SUSPEND.equals(service.getFQClassName())  && (SERVICE_NAME_WAIT.equals(operation.getName()))) ||
           (SG_FQN_WAIT_AND_SUSPEND.equals(service.getFQClassName())  && (SERVICE_NAME_SUSPEND.equals(operation.getName()))) ||
           (SG_FQN_SYNCHRONIZATION.equals(service.getFQClassName()) && (SERVICE_NAME_AWAIT.equals(operation.getName()))) ||
           (SG_FQN_SYNCHRONIZATION.equals(service.getFQClassName()) && (SERVICE_NAME_LONG_RUNNING_AWAIT.equals(operation.getName()))) ) {
        return true;
      }
    } catch (Exception e) {
      return false;
    }
    
    return false;
  }
  
  public boolean isDetachedTaggable() {
    try {
      Service service = getParentScope().identifyService(serviceId).service;
      if (service.isPrototype() || FQN_MANUAL_INTERACTION.equals(service.getOriginalFqName())) {
        return false;
      }
      
      Operation operation = service.getDom().getOperationByName(getOperationName());
      if ( (SG_FQN_WAIT_AND_SUSPEND.equals(service.getFQClassName())  && (SERVICE_NAME_WAIT.equals(operation.getName()))) ||
           (SG_FQN_WAIT_AND_SUSPEND.equals(service.getFQClassName())  && (SERVICE_NAME_SUSPEND.equals(operation.getName()))) ||
           (SG_FQN_SYNCHRONIZATION.equals(service.getFQClassName()) && (SERVICE_NAME_AWAIT.equals(operation.getName()))) ||
           (SG_FQN_SYNCHRONIZATION.equals(service.getFQClassName()) && (SERVICE_NAME_LONG_RUNNING_AWAIT.equals(operation.getName()))) ||
           (SG_FQN_SYNCHRONIZATION.equals(service.getFQClassName()) && (SERVICE_NAME_NOTIFY.equals(operation.getName()))) ||
           (SG_FQN_TEMPLATE_MANAGEMENT.equals(service.getFQClassName()) && (SERVICE_NAME_START.equals(operation.getName()))) ||
           (SG_FQN_TEMPLATE_MANAGEMENT.equals(service.getFQClassName()) && (SERVICE_NAME_STOP.equals(operation.getName()))) ||
           (SG_FQN_PERSISTENCE_SERVICES.equals(service.getFQClassName()) && (SERVICE_NAME_QUERY_EXTENDED.equals(operation.getName()))) ) {
       return false;
     }
    } catch (Exception e) {
      return true;
    }
    
    return true;
  }
  
  public void setOverrideCompensation(boolean override) {
    if (override) {
      if (compensateStep != null) {
        // in case a overriding compensation is already set, don't change it
        return;
      }
      
      // create new overriding compensation
      StepSerial serial = new StepSerial(getParentScope(), creator);
      compensateStep = serial.getProxyForCatch();
    } else {
      // remove overriding compensation
      compensateStep = null;
    }
  }
  
  public boolean isPrototype() {
    if (serviceReference != null) {
      return serviceReference.isPrototype();
    } else {
      return false;
    }
  }
  
  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepFunction)) {
      return true;
    }
    StepFunction oldFunctionStep = (StepFunction) oldStep;
    
    if (!serviceId.equals(oldFunctionStep.serviceId) || !operationName.equals(oldFunctionStep.operationName)
        || !(isExecutionDetached == oldFunctionStep.isExecutionDetached)) {
      return true;
      
    }
    
    if (!Arrays.equals(input.getVarIds(), oldFunctionStep.input.getVarIds())
        || !Arrays.equals(input.getPaths(), oldFunctionStep.input.getPaths())
        || !Arrays.equals(receivePaths, oldFunctionStep.receivePaths)
        || !Arrays.equals(receiveVarIds, oldFunctionStep.receiveVarIds)) {
      return true;
    }
    
    if (catchStep != null) {
      if (catchStep.compareImplementation(oldFunctionStep.catchStep)) {
        return true;
      }
    } else if (oldFunctionStep.catchStep != null) {
      return true;
    }
    
    if (compensateStep != null) {
      if (compensateStep.compareImplementation(oldFunctionStep.compensateStep)) {
        return true;
      }
    } else if (oldFunctionStep.compensateStep != null) {
      return true;
    }
    
    return false;
  }
  
  private boolean isManualInteractionInvocation(WF wf) {
    String originalFqName = wf.getOriginalFqName();
    if (originalFqName == null || originalFqName.equals("")) {
      return false;
    }
    List<String> knownManualInteractionWorkflows = new ArrayList<String>();
    String customMIs =
        XynaFactory.getInstance().getFactoryManagement()
            .getProperty(XynaProperty.CUSTOM_MANUAL_INTERACTION_WORFLOW_XMLFQNAMES);
    if (customMIs != null && !customMIs.equals("")) {
      knownManualInteractionWorkflows.addAll(Arrays.asList(customMIs.split(",")));
    }
    knownManualInteractionWorkflows.add(ManualInteractionManagement.MANUALINTERACTION_WORKFLOW_FQNAME);
    for (String knownManualInteractionWorkflow : knownManualInteractionWorkflows) {
      if (originalFqName.equals(knownManualInteractionWorkflow)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  protected Set<String> getAllUsedVariableIds() {
    String[] rdi = new String[0];
    if(remoteDispatchingParameter != null) {
      rdi = remoteDispatchingParameter.invokeVarIds;
    }
    return createVariableIdSet(input.getVarIds(), receiveVarIds, rdi);
  }
  
  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_PrototypeDeployment {
    if (serviceId == null) {
      throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.FUNCTION, GenerationBase.EL.INVOKE);
    }
    if (serviceId.trim().length() == 0) {
      throw new XPRC_MissingServiceIdException(operationName);
    }
    
    if (orderInputSourceRef == null) {
      for (int i = 0; i < input.length(); i++) {
        if (input.getVarIds()[i].trim().length() == 0) {
          throw new XPRC_EmptyVariableIdException(GenerationBase.EL.INVOKE + "." + GenerationBase.EL.SOURCE + " " + operationName);
        }
      }
    }
    if (!isExecutionDetached()) {
      for (int i = 0; i < receiveVarIds.length; i++) {
        if (receiveVarIds[i].trim().length() == 0) {
          throw new XPRC_EmptyVariableIdException(GenerationBase.EL.INVOKE + "." + GenerationBase.EL.SOURCE + " " + operationName);
        }
      }
    }
    if (serviceReference != null && serviceReference.isPrototype()) {
      throw new XPRC_PrototypeDeployment();
    }
  }
  
  public static class RemoteDespatchingParameter {
    private String remoteDestination;
    private String remoteOrdertype;
    private String[] invokeVarIds;
    private String[] invokePaths;
    private boolean[] isUserConnected;
    private boolean[] isConstantConnected;
    

    RemoteDespatchingParameter(String remoteDestination, String[] invokeVarIds, String[] invokePaths, boolean[] userConnected,
                               boolean[] constantConnected) {
      this.remoteDestination = remoteDestination;
      this.invokeVarIds = invokeVarIds;
      this.invokePaths = invokePaths;
      this.isUserConnected = userConnected;
      this.isConstantConnected = constantConnected;
    }


    public String getRemoteDestination() {
      return remoteDestination;
    }


    public void setRemoteDestination(String remoteDestination) {
      this.remoteDestination = remoteDestination;
    }


    public String[] getInvokeVarIds() {
      return invokeVarIds;
    }


    public void setInvokeVarIds(String[] invokeVarIds) {
      this.invokeVarIds = invokeVarIds;
    }


    public boolean[] getIsConstantConnected() {
      return isConstantConnected;
    }


    public void setConstantConnected(boolean[] constantConnected) {
      this.isConstantConnected = constantConnected;
    }


    public boolean[] getIsUserConnected() {
      return isUserConnected;
    }


    public void setUserConnected(boolean[] userConnected) {
      this.isUserConnected = userConnected;
    }
  }
  
  public Step getCompensateStep() {
    return compensateStep;
  }
  
  public String getLabel() {
    return label;
  }
  /*
  public String getFunctionId() {
    return functionId;
  }*/
  
  
  public void createEmpty() { //throws XPRC_InvalidPackageNameException {
/*
    <Function ID="5" IsAbstract="true" Label="Service 1">
    <Source RefID="4"/>
    <Target RefID="4"/>
    <Meta>
      <Abstract.UID>18C654B2-1B56-4A94-CE44-7B9AC0A2D61C</Abstract.UID>
    </Meta>
    <ServiceReference ID="4" Label="Service" ReferenceName="AbstractService">
      <Source RefID="5"/>
      <Target RefID="5"/>
    </ServiceReference>
    <Service IsAbstract="true" Label="Service" TypeName="AbstractService">
      <Operation IsAbstract="true" Name="service">
        <Input/>
        <Output/>
      </Operation>
    </Service>
    <Invoke Operation="service" ServiceID="4"/>
    <Receive ServiceID="4"/>
  </Function>
*/
    
    setXmlId(creator.getNextXmlId());
    label = "Service";
    
    
    input = new InputConnections(0);
    receiveVarCastToType = new String[]{};
    receiveVarIds = new String[]{};
    receivePaths = new String[]{};
    
    setServiceId(creator.getNextXmlId().toString());
    operationName = "service";
    
    Service service = new Service(creator);
    service.createEmpty(serviceId);
    serviceReference = service;
    
  }
  
  public void createService(DOM dom, String operationName, String[] invokeVarIds, String[] receiveVarIds) throws XPRC_OperationUnknownException {
    /*
    <ServiceReference ID="7" Label="OrderControlService" ReferenceName="OrderControlService.OrderControlService" ReferencePath="xprc.xpce">
    <Source RefID="8"/>
    <Target RefID="8"/>
  </ServiceReference>
  <Function ID="8" Label="TEST Get Root Order Id">
    <Source RefID="7"/>
    <Target RefID="7"/>
    <Target RefID="9"/>
    <Invoke Operation="getRootOrderId" ServiceID="7"/>
    <Receive ServiceID="7">
      <Target RefID="9"/>
    </Receive>
  </Function>
*/
    
    setXmlId(creator.getNextXmlId());
    Operation operation = dom.getOperationByName(operationName);
    label = operation.getLabel();
    setServiceId(creator.getNextXmlId().toString());
    replaceService(dom, operationName, true, invokeVarIds, receiveVarIds);
  }
  
  public void createService(WF workflow, String[] invokeVarIds, String[] receiveVarIds) {
    setXmlId(creator.getNextXmlId());
    label = workflow.getLabel();
    setServiceId(creator.getNextXmlId().toString());
    replaceCall(workflow, true, invokeVarIds, receiveVarIds);
  }
  
  public void convertToPrototype() {
    Service service = new Service(creator);
    
    service.createEmpty(serviceId);
    serviceReference = service;
    operationName = service.getServiceName();
  }
  
  private void replaceCall(WF workflow, boolean changeLabel, String[] invokeVarIds, String[] receiveVarIds) {
    replaceVars(invokeVarIds, receiveVarIds);
    
    Service service = new Service(creator);
    service.createCall(serviceId, workflow);
    serviceReference = service; // TODO: Ist das nicht falsch, weil serviceReference nur fuer abstrakte Services gedacht ist?
    operationName = service.getServiceName();
    
    if( changeLabel ) {
      setLabel(workflow.getLabel());
    }
  }
  
  private void replaceService(DOM dom, String operationName, boolean changeLabel, String[] invokeVarIds, String[] receiveVarIds) throws XPRC_OperationUnknownException {
    this.operationName = operationName;
    Operation operation = dom.getOperationByName(operationName);
    replaceVars(invokeVarIds, receiveVarIds);
    
    Service service = new Service(creator);
    service.createOperation(serviceId, dom, operation);
    serviceReference = service;
    
    if( changeLabel ) {
      setLabel(operation.getLabel());
    }
  }
  
  private void replaceVars(String[] invokeVarIds, String[] receiveVarIds) {
    input = new InputConnections(invokeVarIds);
    
    receiveVarCastToType = new String[receiveVarIds.length];
    receivePaths = new String[receiveVarIds.length];
    this.receiveVarIds = receiveVarIds;
  }
  
  public Service getService() {
    try {
      return getParentScope().identifyService(serviceId).service;
    } catch (XPRC_InvalidServiceIdException e) {
      return serviceReference;
    }
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  @Override
  public String getDocumentation() {
    return documentation;
  }
  
  @Override
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }
  
  // only necessary for flash GUI - TODO: remove when flash GUI is not used, anymore
  private String getOrCreateUid() {
    if (abstractUid == null) {
      abstractUid = UUID.randomUUID().toString().toUpperCase();
    }
    
    return abstractUid;
  }
  
  // TODO
  public List<String> getCompatibleOrderInputSources() {
    return new ArrayList<String>();
    // TODO: List<OrderInputSourceStorable> sources = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement().getOrderInputSourcesForRevision(...);
  }
  
  @Override
  public boolean isInRetryLoop() {
    if (super.isInRetryLoop()) {
      return true;
    }

    if (catchStep == null) {
      return false;
    }

    for (Step executedCatch : catchStep.getExecutedCatches()) {
      if (executedCatch.isInRetryLoop()) {
        return true;
      }
    }

    return false;
  }
  
  @Override
  public void addLabelsToParameter() {
    // copy labels from function since those are not set in parameter-tag
    
    Service service;
    try {
      service = getParentScope().identifyService(getServiceId()).service;
    } catch (Exception e) {
      logger.error(e);
      return;
    }

    List<AVariable> inputVars, outputVars;
    if (service.isDOMRef()) {
      return;
//      inputVars = ...; TODO
//      outputVars = ...; TODO
    } else {
      inputVars = service.getWF().getInputVars();
      outputVars = service.getWF().getOutputVars();
    }
    
    addLabelsToParameter(inputVars, outputVars);
  }
  
  @Override
  public void appendXML(XmlBuilder xml) {
    // <Function>
    xml.startElementWithAttributes(EL.FUNCTION); {
      Integer xmlId = getXmlId();
      if ( (xmlId == null) && (catchStep != null) ) {
        xmlId = catchStep.getXmlId();
      }
      if (xmlId != null) {
        xml.addAttribute(ATT.ID, xmlId.toString());
      }

      xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(getLabel(), true, false));
      if (isPrototype()) {
        xml.addAttribute(ATT.ABSTRACT, ATT.TRUE);
      }
      xml.endAttributes();
      
      // <Source>
      String serviceId = getServiceId();
      appendSource(xml, serviceId);
      for (String id : input.getVarIds()) {
        appendSource(xml, id); 
      }
      
      // <Target>
      appendTarget(xml, serviceId, null, false);
      for (String id : receiveVarIds) {
        appendTarget(xml, id, false);
      }
      
      if (isPrototype()) {
        appendPrototypeServiceXML(xml);
      } else {
        // <Meta>
        String documentation = XMLUtils.escapeXMLValueAndInvalidChars(getDocumentation(), false, false);
        if ( ((documentation != null) && (documentation.length() > 0)) ||
             ((queryFilterConditions != null) && (queryFilterConditions.size() > 0)) || 
             ((orderInputSourceRef != null) && orderInputSourceRef.length() > 0) ||
             (isExecutionDetached) ||
             (freesCapacities) ||
             (hasUnknownMetaTags()) ) {
          xml.startElement(EL.META); {
            if ( (documentation != null) && (documentation.length() > 0) ) {
              xml.element(EL.DOCUMENTATION, documentation);
            }
            
            if ((queryFilterConditions != null) && (queryFilterConditions.size() > 0)) {
              xml.startElement(EL.QUERY_FILTER); {
                for (String queryFilterCondition : queryFilterConditions) {
                  xml.element(EL.QUERY_FILTER_CONDITION, queryFilterCondition);
                }
              } xml.endElement(EL.QUERY_FILTER);
            }
            
            if(orderInputSourceRef != null && orderInputSourceRef.length() > 0) {
              xml.element("FixedDetailOptions", "openTopDetailArea");
              xml.element(EL.ORDER_INPUT_SOURCE, orderInputSourceRef);
            }
            
            if (isExecutionDetached) {
              xml.element(GenerationBase.EL.DETACHED);
            }
            
            if (freesCapacities) {
              xml.element(GenerationBase.EL.FREE_CAPACITIES);
            }
            
            appendUnknownMetaTags(xml);
          } xml.endElement(EL.META);
        }
      }
      
      // <Invoke>
      
      xml.startElementWithAttributes(EL.INVOKE); {
        xml.addAttribute(ATT.SERVICEID, serviceId);
        xml.addAttribute(ATT.INVOKE_OPERATION, XMLUtils.escapeXMLValue(getOperationName(), true, false));
        xml.endAttributes();
        
        boolean hasOrderInputSource = getOrderInputSourceRef() != null && getOrderInputSourceRef().length() > 0;
        if (hasOrderInputSource) {
          for (int inputNr = 0; inputNr < orderInputSourceInputLength; inputNr++) {
            xml.element(EL.SOURCE, "");
          }
        } else {
          for (int inputNr = 0; inputNr < input.getVarIds().length; inputNr++) {
            String id = input.getVarIds()[inputNr];
            appendSource(xml, id, input.getUserConnected()[inputNr], input.getConstantConnected()[inputNr], input.getExpectedTypes()[inputNr], true, input.getUnknownMetaTags().get(inputNr));
          }
        }
      } xml.endElement(EL.INVOKE);
      
      // <Receive>
      
      xml.startElementWithAttributes(EL.RECEIVE); {
        xml.addAttribute(ATT.SERVICEID, serviceId);
        xml.endAttributes();
        
        for (int varIdx = 0; varIdx < receiveVarIds.length; varIdx++) {
          appendTarget(xml, receiveVarIds[varIdx], receiveVarCastToType[varIdx], true);
        }
      } xml.endElement(EL.RECEIVE);
      
      // Remote Destination
      if (remoteDispatchingParameter != null) {
        xml.startElementWithAttributes(EL.REMOTE_DISPATCHING); {
          xml.addAttribute(ATT.REMOTE_DESTINATION, remoteDispatchingParameter.remoteDestination);
          xml.endAttributes();
          for (int inputNr = 0; inputNr < remoteDispatchingParameter.invokeVarIds.length; inputNr++) {
            appendSource(xml, remoteDispatchingParameter.invokeVarIds[inputNr], remoteDispatchingParameter.isUserConnected[inputNr], remoteDispatchingParameter.isConstantConnected[inputNr], true);
          }
        } xml.endElement(EL.REMOTE_DISPATCHING);
      }
      
      // <Catch>
      if (catchStep != null) {
        catchStep.appendCatchAreas(xml);
      }
      
      // <Compensate>
      if (compensateStep != null) {
        xml.startElement(EL.COMPENSATE); {
          compensateStep.appendXML(xml);
        } xml.endElement(EL.COMPENSATE);
      }
    } xml.endElement(EL.FUNCTION);
  }
  
  @Override
  protected void collectServiceReferences(Set<Service> serviceReferences) throws XPRC_InvalidServiceIdException {
    if (!isPrototype()) {
      try {
        Service service = getParentScope().identifyService(serviceId).service;
        serviceReferences.add(service);
      } catch (XPRC_InvalidServiceIdException e) {
        serviceReferences.add(serviceReference);
      }
    }
    
    super.collectServiceReferences(serviceReferences);
  }
  
  private void appendPrototypeServiceXML(XmlBuilder xml) {
    // <Meta> - only necessary for flash GUI - TODO: remove when flash GUI is not used, anymore
    xml.startElement(EL.META); {
      xml.element(EL.ABSTRACT_UID, XMLUtils.escapeXMLValueAndInvalidChars(getOrCreateUid(), false, false));
    } xml.endElement(EL.META);
    
    // <ServiceReference>
    appendServiceReference(xml);
    
    // <Service>
    // TODO: better outsource writing xml for service to serviceReference.appendXML(xml)?
    
    xml.startElementWithAttributes(EL.SERVICE); {
      xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(getLabel(), true, false));
      xml.addAttribute(ATT.ABSTRACT, ATT.TRUE);
      xml.addAttribute(ATT.TYPENAME, XMLUtils.escapeXMLValue(serviceReference.getServiceName(), true, false));
      xml.endAttributes();
      
      // <Operation>
      
      xml.startElementWithAttributes(EL.OPERATION); {
        xml.addAttribute(ATT.OPERATION_NAME, XMLUtils.escapeXMLValue(getOperationName(), true, false));
        xml.addAttribute(ATT.ABSTRACT, ATT.TRUE);
        xml.endAttributes();
        
        // <Meta>
        String documentation = XMLUtils.escapeXMLValueAndInvalidChars(getDocumentation(), false, false);
        if ( ((orderInputSourceRef != null) && (orderInputSourceRef.length() > 0)) ||
             ((documentation != null) && (documentation.length() > 0)) ) {
          xml.startElement(EL.META); {
            if ( (orderInputSourceRef != null) && (orderInputSourceRef.length() > 0) ) {
              xml.addAttribute(EL.ORDER_INPUT_SOURCE, orderInputSourceRef);
            }
            if ( (documentation != null) && (documentation.length() > 0) ) {
              xml.element(EL.DOCUMENTATION, documentation);
            }
          } xml.endElement(EL.META);
        }
        
        // <Input>
        xml.startElement(EL.INPUT); {
          for (AVariable variable : serviceReference.getInputVars()) {
            variable.appendXML(xml, false); // no source- and target-id for variables of prototype services
          }
        } xml.endElement(EL.INPUT);
        
        // <Output>
        xml.startElement(EL.OUTPUT); {
          for (AVariable variable : serviceReference.getOutputVars()) {
            variable.appendXML(xml, false); // no source- and target-id for variables of prototype services
          }
        } xml.endElement(EL.OUTPUT);
      } xml.endElement(EL.OPERATION);
    } xml.endElement(EL.SERVICE);
  }
  
  private void appendServiceReference(XmlBuilder xml) {
    String serviceId = getServiceId();
    Service service;
    if (isPrototype()) {
      service = serviceReference;
    } else {
      try {
        service = getParentScope().identifyService(serviceId).service;
      } catch (XPRC_InvalidServiceIdException e) {
        logger.error(e);
        return;
      }
    }
    
    XMLUtils.appendServiceReference(xml, service, false);
  }
  
  public InputConnections getInputConnections() {
    return input;
  }

  public RemoteDespatchingParameter getRemoteDispatchingParameter() {
    return remoteDispatchingParameter;
  }
  
  public void createEmptyDispatchingParameter() {
    remoteDispatchingParameter = new RemoteDespatchingParameter(null, new String[0], new String[0], new boolean[0], new boolean[0]);
  }
  
  public void removeRemoteDispatchingParameter() {
    remoteDispatchingParameter = null;
  }
}

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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.DynamicRuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;


public class InitialRemoteDestinationType implements RemoteDestinationType {

  private final static String NAME = "InitialType";
  private final static String LABEL = "Initial RemoteDestinationType";
  private final static String DESCRIPTION = "Configurable type supporting common usecases";
  
  public static final StringParameter<String> NODE_DISPATCHING_ALGORITHM = 
                  StringParameter.typeString("nodeDispatchingAlgorithm").
                  label("Node Dispatching Algorithm").
                  documentation(Documentation.
                                en("Algorithm for determining to which FactoryNode to dispatch the call.\n"+
                                   "Possible values:\n      LoadBalancing:{'RoundRobin'|'Random'}\n      OrderType:{OrderType[:FQParameter]}\n      DestinationKey:{DestinationKey}\n      Input\n      TakeFirst\n    ").
                                de("").
                                build()).
                  defaultValue("TakeFirst").build();
  
  public static final StringParameter<String> CONTEXT_DISPATCHING_ALGORITHM = 
                  StringParameter.typeString("contextDispatchingAlgorithm").
                  label("Context Dispatching Algorithm").
                  documentation(Documentation.
                                en("Algorithm for determining to which RuntimeContext on the RemoteNode to dispatch the call.\n"+
                                    "Possible values:\n      StubVersion\n      NewestByVersionComparison\n      NewestByDate\n      Constant:{RuntimeContext.getGUIRepresentation}\n      Input\n    ").
                                de("").
                                build()).
                  defaultValue("StubVersion").build();
  
  public static final StringParameter<String> ERROR_HANDLING = 
                  StringParameter.typeString("errorHandling").
                  label("Algorithm for error handling").
                  documentation(Documentation.
                                en("Possible values:\n      Retries:{timeBetween}:{maxRetries}\n      Throw\n      TryNext\n      Queue[:{Timeout}]\n    ").
                                de("").
                                build()).
                  defaultValue("Throw").build();
  
  public static final StringParameter<String> REMOTE_NODE = 
                  StringParameter.typeString("remoteNode").
                  label("Factory- or Cluster-Node used for nodeDispatchingAlgorithm").
                  documentation(Documentation.
                                en("").
                                de("").
                                build()).
                  defaultValue("").build();
  
  public static final List<StringParameter<?>> parameters = 
                  StringParameter.asList( NODE_DISPATCHING_ALGORITHM,
                                          CONTEXT_DISPATCHING_ALGORITHM,
                                          ERROR_HANDLING,
                                          REMOTE_NODE
                                        );
  
  private static final PluginDescription pluginDescription = PluginDescription.create(PluginType.remoteDestinationType)
                                                                              .name(NAME)
                                                                              .label(LABEL)
                                                                              .description(DESCRIPTION)
                                                                              .parameters(PluginDescription.ParameterUsage.Create, parameters)
                                                                              .build();
  
  
  private String nodeName;
  private INodeDispatching nodeDispatching;
  private IRuntimeContextDispatching contextDispatching;
  private IErrorHandling errorHandling;
  

  public DispatchingTarget dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParamter) {
    DispatchingTarget target = new DispatchingTarget();
    try {
      target.factoryNodeNames = nodeDispatching.dispatch(ownContext, stubContext, remoteDispatchingParamter);
      target.factoryNodeName = target.factoryNodeNames.get(0);
    } catch( XynaException e ) {
      throw new RuntimeException(e);
      //FIXME
    }
    target.context = contextDispatching.dispatch(stubContext, target.factoryNodeName, remoteDispatchingParamter);
    return target;
  }


  public PluginDescription getInitialisationParameterDescription() {
    return pluginDescription;
  }



  public DispatchingParameterDescription getDispatchingParameterDescription() {
    DispatchingParameterDescription dpd = new DispatchingParameterDescription();
    List<DispatchingParameter> parameters = new ArrayList<DispatchingParameter>();
    try {
      parameters.addAll(nodeDispatching.getAdditionalDispatchingParameters());
    } catch (XynaException e) {
      throw new RuntimeException(e);
      //FIXME
    }
    dpd.setDispatchingParameters(parameters);
    return dpd;
  }


  public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
                                             DispatchingTarget failedTarget, Throwable error, int retryCount,
                                             GeneralXynaObject remoteDispatchingParameter) {
    return errorHandling.handleConnectionError(stubContext, location, failedTarget, error, retryCount, remoteDispatchingParameter);
  }


  public void init(Map<String, Object> parameter) {
    //try {
      //Map<String, Object> params = StringParameter.parse(parameterMap).with(parameters);
      nodeName = REMOTE_NODE.getFromMap(parameter);
      nodeDispatching = determineNodeDispatchingAlgorithm(NODE_DISPATCHING_ALGORITHM.getFromMap(parameter));
      contextDispatching = determineRuntimeContextDispatchingAlgorithm(CONTEXT_DISPATCHING_ALGORITHM.getFromMap(parameter));
      errorHandling = determineErrorHandlingAlgorithm(ERROR_HANDLING.getFromMap(parameter));
    /*} catch (StringParameterParsingException e) {
      throw new RuntimeException(e);
    }*/
    
  }
  
  private INodeDispatching determineNodeDispatchingAlgorithm(String parameter) {
    if (parameter.equals("TakeFirst")) {
      return new NodeDispatching_TakeFirst(getFactoryNodes(this.nodeName));
    } else if (parameter.equals("Input")) {
      return new NodeDispatching_Input();
    } else if (parameter.startsWith("LoadBalancing:")) {
      if (parameter.endsWith("RoundRobin")) {
        return new NodeDispatching_RoundRobin(getFactoryNodes(this.nodeName));
      } else if (parameter.endsWith("Random")) {
        return new NodeDispatching_Random(getFactoryNodes(this.nodeName));
      } else {
        throw new UnsupportedOperationException("NodeDispatching: '" + parameter + "' is currently not supported");
      }
    } else if (parameter.startsWith("OrderType:")) {
      String param = parameter.substring("OrderType:".length());
      return new NodeDispatching_OrderType(param);
    } else if (parameter.startsWith("DestinationKey:")) {
      String param = parameter.substring("DestinationKey:".length());
      return new NodeDispatching_DestinationKey(param);
    } else {
      throw new UnsupportedOperationException("NodeDispatching: '" + parameter + "' is currently not supported");
    }
  }

  private IErrorHandling determineErrorHandlingAlgorithm(String parameter) {
    if (parameter.equals("Throw")) {
      return new ErrorHandling_Throw();
    } else if (parameter.startsWith("Retries:")) {
      String retrySplit[] = parameter.split(":");
      return new ErrorHandling_Retry(Duration.valueOf(retrySplit[1]), Integer.parseInt(retrySplit[2]));
    } else if (parameter.startsWith("Queue") ) {
      String queueSplit[] = parameter.split(":");
      Duration timeout = null;
      if( queueSplit.length == 2 ) {
        timeout = Duration.valueOf(queueSplit[1]);
      } else {
        timeout = null;
      }
      return new ErrorHandling_Queue(timeout);
    } else if (parameter.equals("TryNext") ) {
      return new ErrorHandling_TryNext();
    } else {
      throw new UnsupportedOperationException("ErrorHandling: '" + parameter + "' is currently not supported");
    }
  }
  /*
   *       
  "Possible values:\n     NewestByDate\n       Input\n    ").
                  
   */
  private IRuntimeContextDispatching determineRuntimeContextDispatchingAlgorithm(String parameter) {
    if (parameter.equals("StubVersion")) {
      return new RuntimeContextDispatching_StubVersion();
    } else if (parameter.startsWith("Constant:")) {
      RuntimeContext rc = RuntimeContext.valueOf(parameter.substring("Constant:".length()));
      return new RuntimeContextDispatching_Constant(rc);
    } else if (parameter.equals("NewestByVersionComparison")) {
      return new RuntimeContextDispatching_NewestByVersionComparison();
    } else {
      throw new UnsupportedOperationException("RuntimeContextDispatching: '" + parameter + "' is currently not supported");
    }
  }
  
  
  interface INodeDispatching {
    
    /**
     * @param ownContext
     * @param stubContext
     * @param remoteDispatchingParameter
     * @return  FactoryNode-Name
     * @throws XynaException
     */
    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) throws XynaException;
    
    /**
     * Parameter für GUI-Modellierung: 
     * @return
     * @throws XynaException 
     */
    public List<DispatchingParameter> getAdditionalDispatchingParameters() throws XynaException;
    
  }
  
  static class NodeDispatching_Random implements INodeDispatching {

    private List<String> nodeNames;
    Random rnd;

    public NodeDispatching_Random(List<String> nodeNames) {
      if( nodeNames.isEmpty() ) {
        throw new IllegalArgumentException("List of factory nodes must not be empty");
      }
      this.nodeNames = nodeNames;
      rnd = new Random();
    }

    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) {
      List<String> nns = new ArrayList<String>(nodeNames);
      Collections.shuffle(nns, rnd );
      return nns;
    }

    public List<DispatchingParameter> getAdditionalDispatchingParameters() {
      return Collections.emptyList();
    }
    
  }
  
  static class NodeDispatching_RoundRobin implements INodeDispatching {

    private List<List<String>> nodeNameLists;
    private Iterator<List<String>> iter;
    
    public NodeDispatching_RoundRobin(List<String> nodeNames) {
      if( nodeNames.isEmpty() ) {
        throw new IllegalArgumentException("List of factory nodes must not be empty");
      }
      int size = nodeNames.size();
      this.nodeNameLists = new ArrayList<List<String>>(size);
      for( int i =0; i<size; ++i ) {
        List<String> ls = new ArrayList<String>(size);
        for( int j=0; j < size; ++ j ) {
          ls.add( nodeNames.get( (i+j)%size) );
        }
        nodeNameLists.add( Collections.unmodifiableList(ls) );
      }
      this.iter = nodeNameLists.iterator();
    }

    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) {
      while( iter.hasNext() ) {
        return iter.next();
      }
      iter = nodeNameLists.iterator();
      while( iter.hasNext() ) {
        return iter.next();
      }
      return null; //kann nicht auftreten
    }

    public List<DispatchingParameter> getAdditionalDispatchingParameters() {
      return Collections.emptyList();
    }
    
  }
  
  static class NodeDispatching_TakeFirst implements INodeDispatching {
    
    private List<String> nodeNames;
    
    public NodeDispatching_TakeFirst(List<String> nodeNames) {
      if( nodeNames.isEmpty() ) {
        throw new IllegalArgumentException("List of factory nodes must not be empty");
      }
      this.nodeNames = Collections.unmodifiableList(nodeNames);
    }

    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) {
      return this.nodeNames;
    }

    public List<DispatchingParameter> getAdditionalDispatchingParameters() {
      return Collections.emptyList();
    }
    
  }
  
  /*
   * Im Gegensatz zum Dispatching-Typ DestinationKey wird hier immer der eigene RuntimeContext verwendet. Da dieser im
   * Vorneherein unbekannt ist, kann man dann aber nicht die DispatchingParameter ermitteln und muss diesen deshalb angeben.
   * (bisher nur 1 Parameter unterstützt)
   */
  static class NodeDispatching_OrderType implements INodeDispatching {

    private final String orderType;
    private final List<DispatchingParameter> dispatchingParameter;
    
    NodeDispatching_OrderType(String param) {
      String[] parts = param.split(":");
      this.orderType = parts[0];
      if( parts.length == 2 ) {
        int idx = parts[1].lastIndexOf('.');
        String name = parts[1].substring(idx+1);
        String path = parts[1].substring(0,idx);
        DispatchingParameter dp = new DispatchingParameter(name, path, name, false);
        dispatchingParameter = Collections.singletonList(dp);
      } else {
        dispatchingParameter = Collections.emptyList();
      }
    }
    
    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) throws XynaException {
      // TODO remember to remove additionalParams -- nötig, wenn RuntimeContextDispatching auch WFs starten möchte (Input)
      DestinationKey dk = new DestinationKey(orderType, ownContext);
      XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, remoteDispatchingParameter);
      
      GeneralXynaObject gxo = XynaFactory.getInstance().getProcessing().startOrderSynchronously(xocp);
      return extractFactoryNodeList(gxo);
    }

    public List<DispatchingParameter> getAdditionalDispatchingParameters() throws XynaException {
      return dispatchingParameter;
    }
    
  }
  
  static List<String> extractFactoryNodeList(GeneralXynaObject gxo) throws InvalidObjectPathException {
    if( gxo instanceof XynaObjectList ) {
      @SuppressWarnings("unchecked")
      List<XynaObject> xol = ((XynaObjectList<XynaObject>) gxo);
      List<String> nodes = new ArrayList<String>(xol.size());
      for( XynaObject xo : xol ) {
        nodes.add(  (String) xo.get("text") );
      }
      return nodes;
    } else {
      return Collections.singletonList( (String) gxo.get("text") );
    }
  }
  
  /*
   * DestinationKey muss korrekten Ziel-RuntimeContext definieren. Damit kann man die DispatchingParameter dynamisch ermitteln
   */
  static class NodeDispatching_DestinationKey implements INodeDispatching {

    private final DestinationKey key;
    
    NodeDispatching_DestinationKey(String param) {
      this.key = DestinationKey.valueOf(param);
    }
    
    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) throws XynaException {
      // TODO remember to remove additionalParams -- nötig, wenn RuntimeContextDispatching auch WFs starten möchte (Input)
      XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(key, remoteDispatchingParameter);
      
      GeneralXynaObject gxo = XynaFactory.getInstance().getProcessing().startOrderSynchronously(xocp);
      return extractFactoryNodeList(gxo);
    }

    public List<DispatchingParameter> getAdditionalDispatchingParameters() throws XynaException {
      List<DispatchingParameter> params = new ArrayList<DispatchingParameter>();
      // TODO don't retrieve every time
      RuntimeContext context = key.getRuntimeContext();
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      DestinationValue dv = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionDestination(key);
      WF wf = WF.generateUncachedInstance(dv.getFQName(), true, revisionManagement.getRevision(context));
      List<AVariable> in = wf.getInputVars();
      for (AVariable aVar : in) {
        DispatchingParameter param = new DispatchingParameter(aVar.getOriginalName(), aVar.getOriginalPath(), aVar.getLabel(), aVar.isList());
        params.add(param);
      }
      List<AVariable> out = wf.getOutputVars();
      if (out.size() != 1) {
        throw new IllegalArgumentException("NodeDispatching Workflow should output base.Text");
      } else {
        if (!(out.get(0).getOriginalPath() + "." + out.get(0).getOriginalName()).equals("base.Text")) {
          throw new IllegalArgumentException("NodeDispatching Workflow should output base.Text");
        }
      }
      return params;
    }
    
  }
 
  static class NodeDispatching_Input implements INodeDispatching {

    public List<String> dispatch(RuntimeContext ownContext, RuntimeContext stubContext, GeneralXynaObject remoteDispatchingParameter) throws XynaException {
      if (remoteDispatchingParameter instanceof Container) {
        return extractFactoryNodeList( ((Container)remoteDispatchingParameter).get(0) );
      } else {
        return extractFactoryNodeList(remoteDispatchingParameter);
      }
    }

    public List<DispatchingParameter> getAdditionalDispatchingParameters() {
      DispatchingParameter param = new DispatchingParameter("Text", "base", "FactoryNode Name", false);
      return Collections.singletonList(param);
    }
    
  }
  
  
  
  
  
  
  interface IErrorHandling {
    public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
                                               DispatchingTarget failedTarget, Throwable error, int retryCount,
                                               GeneralXynaObject remoteDispatchingParameter);
  }
  
  static class ErrorHandling_Throw implements IErrorHandling {

    public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
                                               DispatchingTarget failedTarget, Throwable error, int retryCount,
                                               GeneralXynaObject remoteDispatchingParameter) {
      return ErrorHandling.fail(error);
    }
    
  }
  
  static class ErrorHandling_TryNext implements IErrorHandling {

    public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
                                               DispatchingTarget failedTarget, Throwable error, int retryCount,
                                               GeneralXynaObject remoteDispatchingParameter) {
      
      boolean found = false;
      for( String fn : failedTarget.factoryNodeNames ) {
        if( found ) {
          failedTarget.factoryNodeName = fn;
          return ErrorHandling.nextDispatchingTarget(failedTarget);
        } else if( fn.equals(failedTarget.factoryNodeName) ) {
          found = true;
        }
      }
      return ErrorHandling.fail(error);
    }
    
  }
 
  static class ErrorHandling_Retry implements IErrorHandling {
    
    private final Duration interval;
    private final int maxRetries;
    
    ErrorHandling_Retry(Duration interval, int maxRetries) {
      this.interval = interval;
      this.maxRetries = maxRetries;
    }

    public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
                                               DispatchingTarget failedTarget, Throwable error, int retryCount,
                                               GeneralXynaObject remoteDispatchingParameter) {
      if (retryCount <= maxRetries) {
        try {
          Thread.sleep(interval.getDurationInMillis());
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
        }
        return ErrorHandling.nextDispatchingTarget(failedTarget);
      } else {
        return ErrorHandling.fail(error);
      }
    }
    
  }
  
  static class ErrorHandling_Queue implements IErrorHandling {
    
    private final Duration timeout;
    private final static long NEVER = 1000*60*60*24*365*1000L; //1000 Jahre, damit gibt es keinen Überlauf
    
    ErrorHandling_Queue(Duration timeout) {
      this.timeout = timeout;
    }
    
    public ErrorHandling handleConnectionError(RuntimeContext stubContext, ErrorHandlingLocation location,
        DispatchingTarget failedTarget, Throwable error, int retryCount,
        GeneralXynaObject remoteDispatchingParameter) {
      ErrorHandling handling = new ErrorHandling();
      handling.type = ErrorHandlingType.QUEUE;
      if( timeout == null ) {
        handling.timeout = NEVER;
      } else {
        handling.timeout = timeout.getDurationInMillis();
      }
      return handling;
    }
    
  }
 
  
  
  interface IRuntimeContextDispatching {
    
    RuntimeContext dispatch(RuntimeContext stubContext, String factoryNodeName, GeneralXynaObject remoteDispatchingParamter);
    
  }
  
  class RuntimeContextDispatching_StubVersion implements IRuntimeContextDispatching {

    public RuntimeContext dispatch(RuntimeContext stubContext, String factoryNodeName, GeneralXynaObject remoteDispatchingParamter) {
      return stubContext;
    }
    
  }
  
  
  class RuntimeContextDispatching_Constant implements IRuntimeContextDispatching {
    
    private final RuntimeContext context;
    
    RuntimeContextDispatching_Constant(RuntimeContext context) {
      this.context = context;
    }

    public RuntimeContext dispatch(RuntimeContext stubContext, String factoryNodeName, GeneralXynaObject remoteDispatchingParamter) {
      return context;
    }
    
  }
  
  private static class RuntimeContextDispatching_NewestByVersionComparison implements IRuntimeContextDispatching{

    public RuntimeContext dispatch(RuntimeContext stubContext, String factoryNodeName, GeneralXynaObject remoteDispatchingParamter) {
      if (stubContext instanceof Workspace) {
        throw new RuntimeException("InitialRemoteDestinationType parameter runtimeContextDispatching=NewestByVersionComparison does only work when used inside of applications.");
      }
      return DynamicRuntimeContext.useLatestVersion(stubContext.getName());
    }
    
  }
  
  private List<String> getFactoryNodes(String name) {
    NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    FactoryNode fn = nodeMgmt.getNodeByName(name);
    if( fn != null ) {
      return Collections.singletonList(name);
    }
    ClusterNode cn = nodeMgmt.getClusterByName(name);
    if( cn != null ) {
      return cn.getFactoryNodes();
    }
    return Collections.emptyList();
  }


}

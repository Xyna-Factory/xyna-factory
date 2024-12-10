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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryChannelIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring.InterlinkSearchDispatcher;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring.MonitoringLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller.FactoryNodeCallerStatus;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExcecutionLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.Resumer;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeContextManagementLinkProfile;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class NodeManagement extends FunctionGroup {
  
  public static final String FACTORYNODE_LOCAL = "local";

  private Map<String, FactoryNode> factoryNodes;
  private Map<String, ClusterNode> clusterNodes;
  private Map<InterFactoryChannelIdentifier, Class<? extends InterFactoryChannel>> registeredFactoryLinkChannels;
  private Map<InterFactoryLinkProfileIdentifier, Class<? extends InterFactoryLinkProfile>> registeredFactoryLinkProfiles;
  private ConcurrentHashMap<String, FactoryNodeCaller> factoryNodeCaller;
  private Resumer resumer = null;
  private InterlinkSearchDispatcher interlinkSearchDispatcher;
  
  private static final XynaPropertyString ownNodeNameProperty = new XynaPropertyString("xfmg.xfctrl.nodemgmt.node_name", "")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Name of own Factory Node. Used by Remote Call Feature at the remote node to correlate waiting responses. Do not change while Remote Calls are still active.")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Name des eigenen Factory-Nodes. Wird vom Remote Call Feature verwendet, um auf der entfernten Seite wartende Antworten von Remote Call Aufträgen zu korrelieren. Wert sollte deshalb nicht geändert werden, solange noch Remote Call Aufträge laufen.")
      ;

  
  
  public NodeManagement() throws XynaException {
    super();
  }

  public static final String DEFAULT_NAME = "NodeManagement";
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    factoryNodes = new ConcurrentHashMap<String, FactoryNode>();
    clusterNodes = new ConcurrentHashMap<String, ClusterNode>();
    factoryNodeCaller= new ConcurrentHashMap<String, FactoryNodeCaller>();
    
    registeredFactoryLinkChannels = new EnumMap<InterFactoryChannelIdentifier, Class<? extends InterFactoryChannel>>(InterFactoryChannelIdentifier.class);
    registeredFactoryLinkChannels.put(InterFactoryChannelIdentifier.RMI, InterFactoryRMIChannel.class);
    registeredFactoryLinkProfiles = new EnumMap<InterFactoryLinkProfileIdentifier, Class<? extends InterFactoryLinkProfile>>(InterFactoryLinkProfileIdentifier.class);
    registeredFactoryLinkProfiles.put(InterFactoryLinkProfileIdentifier.OrderExecution, RemoteOrderExcecutionLinkProfile.class);
    registeredFactoryLinkProfiles.put(InterFactoryLinkProfileIdentifier.Infrastructure, InfrastructureLinkProfile.class);
    registeredFactoryLinkProfiles.put(InterFactoryLinkProfileIdentifier.RuntimeContextManagement, RuntimeContextManagementLinkProfile.class);
    registeredFactoryLinkProfiles.put(InterFactoryLinkProfileIdentifier.FileManagement, RemoteFileManagementLinkProfile.class);
    registeredFactoryLinkProfiles.put(InterFactoryLinkProfileIdentifier.Monitoring, MonitoringLinkProfile.class);
    
    //Storables registrieren
    ODSImpl.getInstance().registerStorable(FactoryNodeStorable.class);
    ODSImpl.getInstance().registerStorable(ClusterNodeStorable.class);
    
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(NodeManagement.class, "NodeManagement").
      after(XynaProperty.class, PersistenceLayerInstances.class).
    execAsync(new Runnable() { public void run() { initPropertiesAndStorables(); } });
    interlinkSearchDispatcher = new InterlinkSearchDispatcher();
  }
  
  public InterlinkSearchDispatcher getInterlinkSearchDispatcher() {
    return interlinkSearchDispatcher;
  }

  private void initPropertiesAndStorables() {
    try {
      //alle vorhandenen Factory Nodes aus dem Warehouse laden und instanziieren
      loadPersistedFactoryNodes();
      loadPersistedClusterNodes();
      
      String user = DEFAULT_NAME;
      XynaProperty.RMI_IL_HOSTNAME_REGISTRY.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_PORT_COMMUNICATION.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_PORT_REGISTRY.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SSL_KEYSTORE_TYPE.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SSL_KEYSTORE_PASSPHRASE.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SSL_KEYSTORE_FILE.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SSL_TRUSTSTORE_TYPE .registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SSL_TRUSTSTORE_PASSPHRASE.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SSL_TRUSTSTORE_FILE.registerDependency( UserType.XynaFactory, user);
      XynaProperty.RMI_IL_SOCKET_TIMEOUT.registerDependency( UserType.XynaFactory, user);
      
      ownNodeNameProperty.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
      
      
      String identifier = ownNodeNameProperty.get();
      if( identifier.length() == 0 ) {
        identifier = "FactoryNode-" +System.currentTimeMillis();
        ownNodeNameProperty.set(identifier);
      }
      
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  @Override
  protected void shutdown() throws XynaException {
    logger.debug("shutdown");
    long timeoutMillis = XynaProperty.SHUTDOWN_ABORT_COMMUNICATION_TIMEOUT.get().getDurationInMillis();
    for (FactoryNodeCaller factoryNode : factoryNodeCaller.values()) {
      AbortCommunicationCaller caller = new AbortCommunicationCaller();
      try {
        caller.abortCommunication(factoryNode).get(timeoutMillis, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Exception during shutdown of communication with " + factoryNode.getNodeName() + ". ", e);
        }
      }
    }
    
    factoryNodes.clear();
    clusterNodes.clear();
    interlinkSearchDispatcher.shutDownInternally();
  }
  
  
  private class AbortCommunicationCaller {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public Future<Void> abortCommunication(FactoryNodeCaller factoryNode) {
      return executor.submit(() -> {
        try {
          factoryNode.getRemoteOrderExecution().abortCommunication();
          return null;
        } catch(XFMG_NodeConnectException e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Exception during shutdown of communication with " + factoryNode.getNodeName() + ". " + e.getMessage());
          }
          return null;
        }
      });
    }
  }

  /**
   * Instanziiert einen Remote Access vom angegebenen type.
   * @throws PersistenceLayerException 
   */
  public void addNode(String name, String comment, int instanceId, String remoteAccessType, String[] remoteAccessSpecificParams) throws PersistenceLayerException {
    checkName(name);
    
    //FactoryNodeStorable anlegen und persistieren
    FactoryNodeStorable fns = new FactoryNodeStorable(name, comment, instanceId, remoteAccessType, remoteAccessSpecificParams);
    fns.persist();
    
    //Factory Node instanziieren
    try {
      initFactoryNode(fns);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  

  public void addNode(String name, String comment, Integer instanceId, InterFactoryChannelIdentifier channelIdentifier,
                      Map<String, String> params, Set<InterFactoryLinkProfileIdentifier> profiles) throws PersistenceLayerException {
    checkName(name);
    
    //FactoryNodeStorable anlegen und persistieren
    FactoryNodeStorable fns = new FactoryNodeStorable(name, comment, instanceId, channelIdentifier, params, profiles);
    fns.persist();
    
    //Factory Node instanziieren
    try {
      initFactoryNode(fns);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private void checkName(String name) {
    if (factoryNodes.containsKey(name)) {
      throw new RuntimeException("FactoryNode '" + name + "' already exists.");
    }
    if (clusterNodes.containsKey(name)) {
      throw new RuntimeException("ClusterNode '" + name + "' already exists.");
    }
    
    //TODO weitere Namenseinschränkungen, z.B für StringSerializableList
  }

  /**
   * Instanziiert einen Factory Node mit dem entsprechenden RemoteAccess
   * @param fns
   * @return
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   */
  private FactoryNode initFactoryNode(FactoryNodeStorable fns) throws Exception {
    // TODO we could have multiple channels
    InterFactoryChannelIdentifier ifci = InterFactoryChannelIdentifier.valueOf(fns.getRemoteAccessType());
    Class<? extends InterFactoryChannel> remoteAccessClass = registeredFactoryLinkChannels.get(ifci);
    
    if (remoteAccessClass == null) {
      // TODO msg
      throw new RuntimeException("No RemoteAccess registered under type '" + fns.getRemoteAccessType() + "'.");
    }
    
    InterFactoryChannel remoteAccessInstance = remoteAccessClass.getConstructor().newInstance();
    Map<String, String> params = new HashMap<String, String>();
    for (String parameter : fns.getRemoteAccessSpecificParamsArray()) {
      String[] paramSplit = parameter.split("=");
      params.put(paramSplit[0], paramSplit[1]);
    }
    remoteAccessInstance.init(params);
    
    // TODO extract configured profiles and only get those
    Set<InterFactoryLinkProfile> profiles = new HashSet<InterFactoryLinkProfile>();
    for (InterFactoryLinkProfileIdentifier identifier : InterFactoryLinkProfileIdentifier.values()) {
      Class<? extends InterFactoryLinkProfile> remoteProfileClass = registeredFactoryLinkProfiles.get(identifier);
      if (remoteProfileClass == null) {
        // TODO msg
        throw new RuntimeException("No RemoteAccess registered under type '" + fns.getRemoteAccessType() + "'.");
      }
      profiles.add(remoteProfileClass.getConstructor().newInstance());
    }
    
    InterFactoryLink ifl = new InterFactoryLink(fns.getName(), Collections.singleton(remoteAccessInstance), profiles);
    FactoryNode factoryNodeInstance = new FactoryNode(fns, ifl);
    factoryNodes.put(fns.getName(), factoryNodeInstance);
    return factoryNodeInstance;
  }
  
  private ClusterNode initClusterNode(ClusterNodeStorable cns) throws InstantiationException, IllegalAccessException {
    cns.getFactoryNodes(); //legt Liste an
   
    ClusterNode cn = new ClusterNode(cns);
    clusterNodes.put( cns.getName(), cn);
    return cn;
  }
  
  
  
  /**
   * Entfernt den Factory Node mit dem übergebenen Namen
   * @param name
   * @return  true, falls Eintrag vorhanden war und gelöscht wurde
   * @throws PersistenceLayerException
   */
  public boolean removeNode(String name) throws PersistenceLayerException {
    FactoryNode removed = factoryNodes.remove(name);
    if( removed != null ) {
      //Storable löschen
      removed.getNodeInformation().delete();
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Liefert alle bekannten Factory Nodes,
   * vom ApplicationManagament benötigt
   * @return
   * @deprecated use List&lt;FactoryNodeInformation&gt; listFactoryNodes(verbose)
   */
  @Deprecated
  public Map<String, FactoryNode> listFactoryNodes() {
    return factoryNodes;
  }

  /**
   * Liefert alle bekannten Factory Nodes
   * Detailanzeige für listfactorynodes
   * @return
   */
  public List<FactoryNodeInformation> listFactoryNodes(boolean verbose) {
    List<FactoryNodeInformation> fnis = new ArrayList<FactoryNodeInformation>();
    for( String nodeName : factoryNodes.keySet() ) {
      FactoryNodeInformation fni = getFactoryNodeInformationByName(nodeName, verbose);
      if (fni != null) fnis.add(fni);
    }
    
    return fnis;
  }
   
  /**
   * Liefert die FactoryNodeStorables zu allen bekannten Factory Nodes
   * @return
   */
  public List<FactoryNodeStorable> getAllFactoryNodes() {
    List<FactoryNodeStorable> ret = new ArrayList<FactoryNodeStorable>();
    for (String nodeName : factoryNodes.keySet()) {
      ret.add(factoryNodes.get(nodeName).getNodeInformation());
    }
    return ret;
  }

  
  /**
   * Liefert den Factory Node mit dem übergebenen Namen
   * @param name
   * @return
   */
  public FactoryNode getNodeByName(String name) {
    return factoryNodes.get(name);
  }
  
   /**
   * Liefert den Status zu einm Factory Node mit dem übergebenen Namen
   * @param name
   * @return
   */
  public FactoryNodeInformation getFactoryNodeInformationByName(String name, boolean verbose) {
      FactoryNode fn = getNodeByName(name);
      
      if (fn == null) return null;
      
      FactoryNodeInformation fni = new FactoryNodeInformation();
      fni.setName( fn.getNodeInformation().getName() );
      fni.setDescription(fn.getNodeInformation().getDescription());
      fni.setInstanceId(fn.getNodeInformation().getInstanceId());
      fni.setRemoteAccessType(fn.getNodeInformation().getRemoteAccessType());
      
      FactoryNodeCaller fnc = factoryNodeCaller.get(fni.getName());
      
      if( fnc == null ) {
        fni.setStatus(FactoryNodeCallerStatus.Unused);
      } else {
        fni.setStatus( fnc.getStatus() );
        if( verbose ) {
          fni.setWaitingForConnectivity(fnc.getWaitingForConnectivity());
          fni.setWaitingForResult(fnc.getWaitingForResult());
          fni.setConnectException(fnc.getRemoteOrderExecution().getLastNodeConnectException());
        }
      }
      
      return fni;
  }
 
  /**
   * Instanziiert alle im Warehouse vorhandenen Factory Nodes
   * @throws PersistenceLayerException
   */
  private void loadPersistedFactoryNodes() throws PersistenceLayerException {
    FactoryNodeStorable[] allInstances = FactoryNodeStorable.getAll();
    for (FactoryNodeStorable fns : allInstances) {
      try{
        initFactoryNode(fns);
      } catch (Exception e) {
        logger.warn("could not initialize factory node " + fns.getName(), e);
        XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Failed to initialize factory node "
                        + fns.getName());
      }
    }
  }
  
  /**
   * Instanziiert alle im Warehouse vorhandenen Cluster Nodes
   * @throws PersistenceLayerException
   */
  private void loadPersistedClusterNodes() throws PersistenceLayerException {
    ClusterNodeStorable[] allInstances = ClusterNodeStorable.getAll();
    for (ClusterNodeStorable cns : allInstances) {
      try{
        initClusterNode(cns);
      } catch (Exception e) {
        logger.warn("could not initialize cluster node " + cns.getName(), e);
        XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Failed to initialize cluster node "
                        + cns.getName());
      }
    }
  }

  /**
   * Entfernt den Cluster Node mit dem übergebenen Namen
   * @param name
   * @return  true, falls Eintrag vorhanden war und gelöscht wurde
   * @throws PersistenceLayerException
   */
  public boolean removeCluster(String name) throws PersistenceLayerException {
    ClusterNode removed = clusterNodes.remove(name);
    if( removed != null ) {
      //Storable löschen
      removed.getNodeInformation().delete();
      return true;
    } else {
      return false;
    }
  }

  public List<ClusterNode> listClusterNodes() {
    return new ArrayList<ClusterNode>(clusterNodes.values());
  }

  public void defineCluster(ClusterNode cluster) throws PersistenceLayerException {
    
    checkName(cluster.getName());
    
    //TODO verwendete Factory-Nodes checken?
    
    //ClusterNodeStorable anlegen und persistieren
    ClusterNodeStorable cns = new ClusterNodeStorable(cluster.getNodeInformation());
    cns.persist();
    
    //Factory Node instanziieren
    try {
      initClusterNode(cns);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public ClusterNode getClusterByName(String name) {
    return clusterNodes.get(name);
  }

  
  public FactoryNodeCaller getFactoryNodeCaller(String factoryNodeName) {
    return getOrCreateFactoryNodeCaller(factoryNodeName);
  }

  private FactoryNodeCaller getOrCreateFactoryNodeCaller(String factoryNodeName) {
    FactoryNodeCaller fnc = factoryNodeCaller.get(factoryNodeName);
    if( fnc == null ) {
      if( resumer == null ) {
        resumer = new Resumer();
      }
      FactoryNode fn = factoryNodes.get(factoryNodeName);
      if( fn == null ) {
        throw new RuntimeException("No factory node \""+factoryNodeName+"\" known"); //TODO besser
      }
      String identifier = ownNodeNameProperty.get()+"-"+factoryNodeName;
      FactoryNodeCaller fncNew = new FactoryNodeCaller(fn, resumer, identifier);
      fnc = factoryNodeCaller.putIfAbsent(factoryNodeName, fncNew);
      if( fnc == null ) {        
        fnc = fncNew;
      } else {
        fncNew.shutdown();
      }
    }
    return fnc;
  }

  public FactoryNodeCaller removeFactoryNodeCaller(String nodeName) {
    return factoryNodeCaller.remove(nodeName);
  }

}

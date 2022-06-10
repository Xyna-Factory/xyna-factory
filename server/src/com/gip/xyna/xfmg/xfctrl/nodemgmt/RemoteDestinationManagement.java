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

import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMISocketFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.RemoteDestinationTypeClassLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementInterface;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementLanding;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.local.RemoteOrderExecutionProfileLanding;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.local.RemoteOrderStorage;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.local.StoredResponse;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring.MonitoringInterface;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring.MonitoringLanding;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExecutionInterface;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeContextManagementInterface;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeContextManagementLanding;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;


public class RemoteDestinationManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "RemoteDestinationManagement";
  public static final String FUTUREEXECUTION_REMOTEDESTINATIONS_FULL_INIT = "RemoteDestinationManagement_rmi";

  private final ConcurrentMap<String, RemoteDestinationType> remoteDestinationTypes =
      new ConcurrentHashMap<String, RemoteDestinationType>();
  private final ConcurrentMap<String, RemoteDestinationTypeInstance> remoteDestinationTypeInstances =
      new ConcurrentHashMap<String, RemoteDestinationTypeInstance>();
  
  private final ConcurrentMapWithObjectRemovalSupport<String, RemoteOrderStorage> remoteOrderStorage =
      new ConcurrentMapWithObjectRemovalSupport<String, RemoteOrderStorage>() {

        private static final long serialVersionUID = 1L;

        @Override
        public RemoteOrderStorage createValue(String identifier) {
          return new RemoteOrderStorage(identifier);
        }
    
  };

 
  
  public RemoteDestinationManagement() throws XynaException {
    super();
  }
  
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    //frühe Initialisierung mit Properties und PersistenceLayer, 
    //NodeManagement für gültige FactoryNodes
    fExec.addTask("RemoteDestinationManagement_init", "RemoteDestinationManagement.initPropertiesAndStorables").
          after(XynaProperty.class, PersistenceLayerInstances.class, NodeManagement.class).
          execAsync(new Runnable() { public void run() { initPropertiesAndStorables(); } });
    
    //Cluster-Initialisierung, damit verwendbar für andere Factories
    fExec.addTask(FUTUREEXECUTION_REMOTEDESTINATIONS_FULL_INIT, "RemoteDestinationManagement.initRmi").
          after(RMIManagement.class).
          after(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
          after(RuntimeContextDependencyManagement.class).
          after(FileManagement.class).
          execAsync(new Runnable() { public void run() { initRmi(); } });
  }
  
  
  private void initPropertiesAndStorables() {
    try {
      ODSImpl.getInstance().registerStorable(RemoteDestinationTypeStorable.class);
      ODSImpl.getInstance().registerStorable(RemoteDestinationTypeInstanceStorable.class);
      ODSImpl.getInstance().registerStorable(StoredResponse.class);
      // restore remoteDestinationTypes from persistence
      InitialRemoteDestinationType irdt = new InitialRemoteDestinationType();
      remoteDestinationTypes.put(irdt.getInitialisationParameterDescription().getName(), irdt);
      restoreInstances();

    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }

  private void initRmi() {
    try {   
      RMIManagement rmiManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
      //TODO enum InterFactoryLinkProfileIdentifier verwenden
      rmiManagement.registerClassreloadableRMIImplFactory(new ParameterlessInterLinkRMIImplFactory<RemoteOrderExecutionProfileLanding>(RemoteOrderExecutionProfileLanding.class),
                                                          RemoteOrderExecutionInterface.BINDING_NAME, XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get(), 
                                                          XynaProperty.RMI_IL_PORT_REGISTRY.get(), XynaProperty.RMI_IL_PORT_COMMUNICATION.get(), true);
      rmiManagement.registerClassreloadableRMIImplFactory(new ParameterlessInterLinkRMIImplFactory<InterFactoryInfrastructureLanding>(InterFactoryInfrastructureLanding.class),
                                                          InterFactoryInfrastructureInterface.BINDING_NAME, XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get(), 
                                                          XynaProperty.RMI_IL_PORT_REGISTRY.get(), XynaProperty.RMI_IL_PORT_COMMUNICATION.get(), true);
      rmiManagement.registerClassreloadableRMIImplFactory(new ParameterlessInterLinkRMIImplFactory<RuntimeContextManagementLanding>(RuntimeContextManagementLanding.class, XynaProperty.RMI_IL_SOCKET_TIMEOUT_RTC_MGMT.get()),
                                                          RuntimeContextManagementInterface.BINDING_NAME, XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get(), 
                                                          XynaProperty.RMI_IL_PORT_REGISTRY.get(), XynaProperty.RMI_IL_PORT_COMMUNICATION.get(), true);
      rmiManagement.registerClassreloadableRMIImplFactory(new ParameterlessInterLinkRMIImplFactory<RemoteFileManagementLanding>(RemoteFileManagementLanding.class, XynaProperty.RMI_IL_SOCKET_TIMEOUT_FILE_MGMT.get()),
                                                          RemoteFileManagementInterface.BINDING_NAME, XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get(), 
                                                          XynaProperty.RMI_IL_PORT_REGISTRY.get(), XynaProperty.RMI_IL_PORT_COMMUNICATION.get(), true);
      rmiManagement.registerClassreloadableRMIImplFactory(new ParameterlessInterLinkRMIImplFactory<MonitoringLanding>(MonitoringLanding.class, XynaProperty.RMI_IL_SOCKET_TIMEOUT_MONITORING.get()),
                                                          MonitoringInterface.BINDING_NAME, XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get(), 
                                                          XynaProperty.RMI_IL_PORT_REGISTRY.get(), XynaProperty.RMI_IL_PORT_COMMUNICATION.get(), true);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }
    
  private class ParameterlessInterLinkRMIImplFactory<T extends InitializableRemoteInterface & Remote> implements RMIImplFactory<T>, RMISocketFactory {

    private final Duration timeout;
    private final String fqClassName;
    
    public ParameterlessInterLinkRMIImplFactory(Class<T> clazz, Duration timeout) {
      this.fqClassName = clazz.getCanonicalName();
      this.timeout = timeout;
    }
    
    public ParameterlessInterLinkRMIImplFactory(Class<T> clazz) {
      this(clazz, XynaProperty.RMI_IL_SOCKET_TIMEOUT.get());
    }
    
    public String getFQClassName() {
      return fqClassName;
    }

    public void init(InitializableRemoteInterface rmiImpl) {
      rmiImpl.init(new Object[]{});
    }
    
    public void shutdown(InitializableRemoteInterface rmiImpl) {
    }

    public RMIClientSocketFactory getRMIClientSocketFactory() {
      if (XynaProperty.RMI_IL_SSL_KEYSTORE_FILE.get() == null) {
        RMIManagement rmiManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
        return rmiManagement.getClientSocketFactory((int)timeout.getDuration(TimeUnit.SECONDS));
      } else {
        return new RMISSLClientSocketFactory();
      } 
    }

    public RMIServerSocketFactory getRMIServerSocketFactory() {
      if (XynaProperty.RMI_IL_SSL_KEYSTORE_FILE.get() == null) {
        RMIManagement rmiManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
        return rmiManagement.getServerSocketFactory(XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get());
      } else {
        return new RMISSLServerSocketFactory(XynaProperty.RMI_IL_HOSTNAME_REGISTRY.get());
      }
    }

  }
  
  
  @Override
  protected void shutdown() throws XynaException {
    unregisterRmiInterfaces();
  }
  
  public void unregisterRmiInterfaces() {
    RMIManagement rmiManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
    rmiManagement.unregisterRemoteInterface(RemoteOrderExecutionProfileLanding.BINDING_NAME);
    rmiManagement.unregisterRemoteInterface(InterFactoryInfrastructureInterface.BINDING_NAME);
    rmiManagement.unregisterRemoteInterface(RuntimeContextManagementInterface.BINDING_NAME);
  }
  
  public void registerRemoteDestinationType(String type, String fqClassName) throws XynaException {
    RemoteDestinationType rdt = loadRemoteDestinationType(type, fqClassName);
    
    persistRemoteDestinationType(type, fqClassName);
    remoteDestinationTypes.put(type, rdt);
  }
  

  private RemoteDestinationType loadRemoteDestinationType(String type, String fqClassName) throws XynaException {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    cld.removeRemoteDestinationTypeClassLoader(type);
    RemoteDestinationTypeClassLoader rdtcl = cld.getRemoteDestinationTypeClassLoaderLazyCreate(type);
    boolean success = false;
    try {

      Class<?> clazz = null;
      try {
        clazz = rdtcl.loadClass(fqClassName);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      if (!RemoteDestinationType.class.isAssignableFrom(clazz)) {
        throw new RuntimeException("RemoteDestinationType class must extend " + RemoteDestinationType.class.getName());
      }

      RemoteDestinationType rdt = null;
      try {
        rdt = (RemoteDestinationType) clazz.getConstructor().newInstance();
      } catch (Exception e) { //InstantiationException, IllegalAccessException
        throw new RuntimeException("RemoteDestinationType could not be instantiated", e);
      }
      success = true;
      return rdt;
    } finally {
      if (!success) {
        cld.removeRemoteDestinationTypeClassLoader(type);
      }
    }
  }
  
  
  private void persistRemoteDestinationType(String type, String fqClassName) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      RemoteDestinationTypeStorable rdts = new RemoteDestinationTypeStorable(type, fqClassName);
      logger.info( "Persist "+rdts);
      con.persistObject(rdts);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  
  private void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
  
  
  public Collection<RemoteDestinationType> listRemoteDestinationTypes() {
    return Collections.unmodifiableCollection(remoteDestinationTypes.values());
  }
  
  
  public Collection<RemoteDestinationInstanceInformation> listRemoteDestinationInstances() {
    Collection<RemoteDestinationInstanceInformation> list = new ArrayList<RemoteDestinationInstanceInformation>();
    for (RemoteDestinationTypeInstance rdti : remoteDestinationTypeInstances.values()) {
      list.add(rdti.asInformation());
    }
    return list;
  }
  
  public RemoteDestinationType getRemoteDestinationType(String type) {
    return remoteDestinationTypes.get(type);
  }
  
  public boolean createRemoteDestinationInstance(String remoteDestinationTypeName, String description, String remoteDestinationTypeInstanceName, Map<String, String> parameters, boolean force) throws PersistenceLayerException {
    return createRemoteDestinationInstance(remoteDestinationTypeName, description, remoteDestinationTypeInstanceName, new Duration(0), parameters, force);
  }
  
  public boolean createRemoteDestinationInstance(String remoteDestinationTypeName, String description, String remoteDestinationTypeInstanceName, Duration executionTimeout, Map<String, String> parameters, boolean force) throws PersistenceLayerException {
    RemoteDestinationType type = remoteDestinationTypes.get(remoteDestinationTypeName);
    RemoteDestinationTypeInstanceStorable rdtis = new RemoteDestinationTypeInstanceStorable(remoteDestinationTypeInstanceName, description, remoteDestinationTypeName, executionTimeout, type, parameters);
    RemoteDestinationTypeInstance instance = null;
    try {
      instance = new RemoteDestinationTypeInstance(rdtis, type.getClass());
    } catch( RuntimeException e ) {
      if( force ) {
        logger.warn( "Could not instantiate RemoteDestinationTypeInstance "+ remoteDestinationTypeInstanceName, e);
        //fortsetzen und speichern
      } else {
        throw e; //nicht fehlerhaft anlegen
      }
    }
    persistRemoteDestinationTypeInstance(rdtis);
    remoteDestinationTypeInstances.put(remoteDestinationTypeInstanceName, instance);
    return instance != null;
  }
  
  private void persistRemoteDestinationTypeInstance(RemoteDestinationTypeInstanceStorable rdtis) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      logger.info( "Persist "+rdtis);
      con.persistObject(rdtis);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  
  private void restoreInstances() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<RemoteDestinationTypeInstanceStorable> rdtiss = con.loadCollection(RemoteDestinationTypeInstanceStorable.class);
      for (RemoteDestinationTypeInstanceStorable rdtis : rdtiss) {
        RemoteDestinationType type = remoteDestinationTypes.get(rdtis.getTypename());
        try {
          RemoteDestinationTypeInstance rdti = new RemoteDestinationTypeInstance(rdtis, type.getClass());
          remoteDestinationTypeInstances.put(rdtis.getName(), rdti );
        } catch ( Exception e ) {
          logger.warn("could not initialize remote destination type instance \"" + rdtis.getName()+"\"", e);
          XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,
              "Failed to initialize remote destination type instance \""+rdtis.getName()+"\"");
        }
      }
    } finally {
      con.closeConnection();
    }
  }


  public void getRemoteDestinationTypeInstanceInformation(String name) {
    // displays possible dynamic dispatching parameters and configured static parameters
  }
  
  public RemoteDestinationTypeInstance getRemoteDestinationTypeInstance(String typeInstanceName) {
    // resolve for dispatching
    return remoteDestinationTypeInstances.get(typeInstanceName);
  }


  public PluginDescription getRemoteDestinationTypeDescription(String remotedestinationtype) {
    RemoteDestinationType t = remoteDestinationTypes.get(remotedestinationtype);
    if (t == null) {
      throw new RuntimeException("Remote destination type named <" + remotedestinationtype + "> is unknown.");
    }
    return t.getInitialisationParameterDescription();
  }
  
  // modifyRemoteDestinationTypeInstance ?

  public <R> R useRemoteOrderStorage(String identifier, ValueProcessor<RemoteOrderStorage, R> processor) {
    return remoteOrderStorage.process(identifier, processor);
  }

  public RemoteOrderStorage getRemoteOrderStorage(String identifier) {
    RemoteOrderStorage ros = remoteOrderStorage.lazyCreateGet(identifier);
    remoteOrderStorage.cleanup(identifier);
    return ros;
  }
  
  public void notifyApplicationStarted(final String applicationName, String versionName) {
    for (String identifier : remoteOrderStorage.keySet()) {
      useRemoteOrderStorage(identifier, new ValueProcessor<RemoteOrderStorage, Boolean>() {

        public Boolean exec(RemoteOrderStorage v) {
          v.applicationStarted(applicationName);
          return null;
        }
        
      });
    }
  }
  
  public void removeRemoteDestinationInstance(String instancename) {
    //TODO checken, ob instanz von laufenden aufträgen verwendet wird. force-parameter? -> vermutlich nicht notwendig
  }


}

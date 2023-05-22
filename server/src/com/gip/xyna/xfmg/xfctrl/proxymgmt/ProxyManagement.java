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
package com.gip.xyna.xfmg.xfctrl.proxymgmt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIParameter;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.ProxyCodeBuilder;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.storables.ProxyStorable;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.storables.ProxyStorage;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;


public class ProxyManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "ProxyManagement";
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ProxyManagement.class);

  private ProxyStorage proxyStorage;
  private Map<String,ManagedProxy> managedProxies;
  private String rmiUrl;
  
   
  public ProxyManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(ProxyStorage.class,"ProxyManagement.initProxyStorage").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initProxyStorage(); }});
    fExec.addTask(ProxyManagement.class,"ProxyManagement.initProxies").
      after(UserManagement.class, RMIManagement.class).
      after(XynaMultiChannelPortal.class). //damit XynaRMIChannel-Endpunkt existiert 
      execAsync(new Runnable() { public void run() { initProxies(); }});
  }
  
  private void initProxyStorage() {
    try {
      proxyStorage = new ProxyStorage();
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize ProxyManagement", e);
      throw new RuntimeException(e);
    }
  }
  
  private UserManagement getUserManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
  }
  
  private RMIManagement getRmiManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
  }
  
  private void initProxies() {
    managedProxies = new ConcurrentHashMap<String,ManagedProxy>();
    rmiUrl = createRmiUrl();
    
    RMIManagement rmiManagement = getRmiManagement();
    
    try {
      UserManagement userManagement = getUserManagement();
      for( ProxyStorable prs : proxyStorage.listProxies() ) {
        ManagedProxy mp = new ManagedProxy(prs);
        managedProxies.put( mp.getName(), mp);
        
        try {
          mp.initialize(userManagement, rmiUrl);
          
          mp.start(rmiManagement);
          
        } catch( Exception e ) {
          logger.warn("Could not start proxy "+mp.getName(), e);
          mp.failed( e );
        }
      }
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize ProxyManagement", e);
      throw new RuntimeException(e);
    }
  }

  private String createRmiUrl() {
    RMIManagement rmiManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
    return "//"+rmiManagement.getRMIHostname()+":"+rmiManagement.getRMIPortForRegistry()+"/XynaRMIChannel";
  }


  @Override
  protected void shutdown() throws XynaException {
  }

  public ProxyInformation createRmiProxy(ProxyRole proxyRole, RMIParameter parameter, String description) throws PersistenceLayerException, XPRC_CompileError, XMCP_RMI_BINDING_ERROR, Ex_FileAccessException {
    ManagedProxy mp = new ManagedProxy(proxyRole, parameter, description);
    mp.initialize(getUserManagement(), rmiUrl);
    
    mp.start( getRmiManagement() );
    
    //Persistieren, nachdem Proxy nun fehlerfrei angelegt werden konnte
    managedProxies.put( mp.getName(), mp);
    proxyStorage.persist(mp.getStorable() );
    
    return mp.getProxyInformation();
  }

  public Pair<String, File> createRmiProxyInterface(ProxyRole proxyRole, File targetDir) throws XPRC_CompileError, Ex_FileAccessException {
    ProxyCodeBuilder pcb = null;
    
    ManagedProxy mp = managedProxies.get(proxyRole.getName() );
    if( mp != null ) {
      pcb = new ProxyCodeBuilder(mp.getRole());
    } else {
      pcb = new ProxyCodeBuilder(proxyRole);
    }
    
    //Proxy-Code bauen
    pcb.createRmiProxyInterface();
    
    File jarFile = new File( targetDir, proxyRole.getName()+"_Proxy.jar");
    pcb.compileInterface( jarFile );
    
    return Pair.of( pcb.getInterface(), jarFile );
  }
 
  public List<ProxyInformation> listProxies() {
    List<ProxyInformation> pis = new ArrayList<ProxyInformation>();
    for( ManagedProxy mp : managedProxies.values() ) {
      pis.add( mp.getProxyInformation() );
    }
    return pis;
  }

  public void removeProxy(String name) throws PersistenceLayerException {
    ManagedProxy mp = managedProxies.get(name);
    if( mp != null ) {
      proxyStorage.remove(mp.getStorable() );
    }
    managedProxies.remove(name);
  }


 

}


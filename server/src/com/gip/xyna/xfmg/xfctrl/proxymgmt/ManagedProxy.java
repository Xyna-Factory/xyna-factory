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
package com.gip.xyna.xfmg.xfctrl.proxymgmt;

import java.io.File;
import java.rmi.Remote;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplProxy;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIParameter;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher.ClassLoaderBuilder;
import com.gip.xyna.xfmg.xfctrl.classloading.RmiProxyClassLoader;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.ProxyCodeBuilder;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.storables.ProxyStorable;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;

public class ManagedProxy {

  private ProxyStorable proxy;
  private ProxyInformation proxyInformation;
  private ProxyRole role;
  private String failedMessage;
  private File proxyJar;
  
  public ManagedProxy(ProxyStorable proxy) {
    this.proxy = proxy;
    this.proxyInformation = new ProxyInformation(proxy);
  }

  public ManagedProxy(ProxyRole proxyRole, RMIParameter parameter, String description) {
    this.role = proxyRole;
    this.proxy = new ProxyStorable().fillFromProxyRole(proxyRole).fillFromRMIParameter(parameter);
    this.proxy.setDescription(description);
    this.proxyInformation = new ProxyInformation(proxy);
    fillProxyInformationWithRole();
  }

  private void fillProxyInformationWithRole() {
    proxyInformation.setNumberOfRights(role.getGenerationData().getAll().size());
  }

  public ProxyInformation getProxyInformation() {
    if( failedMessage != null ) {
      proxyInformation.setFailure(failedMessage);
    }
    return proxyInformation;
  }

  public ProxyStorable getStorable() {
    return proxy;
  }

  public String getName() {
    return proxy.getName();
  }
  
  public void failed(Exception e) {
    this.failedMessage = e.getClass().getSimpleName()+": "+e.getMessage();
  }

  public ProxyRole getRole() {
    return role;
  }
  
  public void initialize(UserManagement userManagement, String rmiUrl) throws PersistenceLayerException, XPRC_CompileError, Ex_FileAccessException {
    if( role == null ) {
      role= proxy.toProxyRole(userManagement);
      fillProxyInformationWithRole();
    }
    
    //Proxy-Code bauen
    ProxyCodeBuilder pcb = new ProxyCodeBuilder(role, getPath(), getInterfaceName(), getImplName() );
    pcb.createRmiProxy(rmiUrl, true); //TODO derzeit nur lokaler Proxy
    
    proxyInformation.setNumberOfProxyMethods( pcb.getProxyRmiMethodCount() );
    
    proxyJar = new File("rmiproxies", role.getName()+"ProxyImpl.jar");
   
    pcb.compileImpl( proxyJar );
    
  }
  
  private String getInterfaceName() {
    return role.getName()+"_Proxy";
  }

  private String getImplName() {
    return getInterfaceName()+"Impl";
  }

  private String getPath() {
    return "com.gip.xyna.xmcp";
  }
  
  private String getFqProxyImplClassName() {
    return getPath()+"."+getImplName();
  }
  
  public void start(RMIManagement rmiManagement) throws XMCP_RMI_BINDING_ERROR {
     @SuppressWarnings("rawtypes")
    RMIProxyImplFactory<?> factory = new RMIProxyImplFactory(role.getName(), proxyJar, getFqProxyImplClassName());
    
    RMIImplProxy<?> rip = rmiManagement.registerClassreloadableRMIImplFactory(factory, proxy.toRMIParameter(), true);

    proxyInformation.setRMIParameter(rip.getRMIParameter());
  }
  
  public static class RMIProxyImplFactory<T extends InitializableRemoteInterface & Remote> implements RMIManagement.RMIImplFactory<T>, ClassLoaderBuilder {
    
    private String name;
    private File proxyJar;
    private String fqClassName;

    public RMIProxyImplFactory(String name, File proxyJar, String fqClassName) {
      this.name = name;
      this.proxyJar = proxyJar;
      this.fqClassName = fqClassName;
    }

    @Override
    public void init(InitializableRemoteInterface rmiImpl) {
      rmiImpl.init();
    }

    @Override
    public String getFQClassName() {
      return fqClassName;
    }

    @Override
    public void shutdown(InitializableRemoteInterface rmiImpl) {
    }

    @Override
    public String getId() {
      return "Proxy "+name;
    }

    @Override
    public ClassLoaderBase createClassLoader() {
      return new RmiProxyClassLoader( name, proxyJar );
    }

  }
  
    
  public static interface RmiClassProvider {

    public <T> Class<T> getRMIClass();
    
  }

  
  
}

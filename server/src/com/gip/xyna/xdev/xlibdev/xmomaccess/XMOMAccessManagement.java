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
package com.gip.xyna.xdev.xlibdev.xmomaccess;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeListener;
import com.gip.xyna.xdev.exceptions.XDEV_XMOMAccessInitializationException;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xdev.xlibdev.xmomaccess.parameters.AssignXMOMAccessInstanceParameters;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class XMOMAccessManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "XMOMAccessManagement";
  
  /**
   * revision -> instance
   */
  private Map<Long, XMOMAccess> instances;
  
  
  public XMOMAccessManagement() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    instances = new ConcurrentHashMap<Long, XMOMAccess>();
    
    FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
    fe.execAsync(new FutureExecutionTask(fe.nextId()) {

      @Override
      public void execute() {
        try {
          loadPersistedCodeAccessInstances();
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public int[] after() {
        return new int[] {RepositoryAccessManagement.FUTURE_EXECUTION__REPOSITORY_INSTANCE_INITIALIZATION};
      }
    });
    
    ProjectCreationOrChangeProvider.getInstance().addListener(DEFAULT_NAME, new XMOMAccessEventHandler());
  }

  
  protected void loadPersistedCodeAccessInstances() throws PersistenceLayerException {
    ODSImpl.getInstance().registerStorable(XMOMAccessInstanceStorable.class);
    
    
    XMOMAccessInstanceStorable[] allInstances = XMOMAccessInstanceStorable.getAll();
    for (XMOMAccessInstanceStorable xais : allInstances) {
      try {
        AssignXMOMAccessInstanceParameters params = new AssignXMOMAccessInstanceParameters();
        params.setXmomAccessName(xais.getName());
        params.setRepositoryAccessInstanceName(xais.getRepositoryAccessName());
        initXMOMAccessInstance(xais.getRevision(), params, false);
      } catch (XDEV_XMOMAccessInitializationException e) {
        logger.warn("could not initialize xmom access instance for revision " + xais.getRevision(), e);
        //FIXME disabled merken, damit man später manuell erneut versuchen kann zu enablen!
      } catch (RuntimeException e) {
        logger.warn("could not initialize xmom access instance for revision " + xais.getRevision(), e);
        //FIXME disabled merken, damit man später manuell erneut versuchen kann zu enablen!
      }
    }
    
  }

  @Override
  protected void shutdown() throws XynaException {
    
  }

  //TODO allow for reassignment of repositoryaccess
  public void assignXMOMAccessInstance(AssignXMOMAccessInstanceParameters params, Long revision) throws XynaException{
    if (instances.containsKey(revision)) {
      throw new RuntimeException("Revision " + revision + " already contains a XMOMAccess instance.");
    }
    
    initXMOMAccessInstance(revision, params, true);
    XMOMAccessInstanceStorable.create(revision, params.getXmomAccessName(), params.getRepositoryAccessInstanceName());
  }

  public void deployXMOMAccessComponent(String componentName, Long revision) throws XynaException{
    XMOMAccess xmomAccess = instances.get(revision);
    if (xmomAccess == null) {
      throw new RuntimeException("Revision " + revision + " doesn't contain a XMOMAccess instance.");
    }
    
    if (componentName != null) {
      xmomAccess.deployComponent(componentName);
    } else {
      xmomAccess.deployAllComponents();
    }
  }

  
  private XMOMAccess initXMOMAccessInstance(Long revision, AssignXMOMAccessInstanceParameters params, boolean checkout) throws XDEV_XMOMAccessInitializationException {
    RepositoryAccessManagement ram = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
    RepositoryAccess instance = ram.getRepositoryAccessInstance(params.getRepositoryAccessInstanceName());
    if (instance == null) {
      throw new XDEV_XMOMAccessInitializationException(new IllegalArgumentException("There is no repositoryAccessInstance with the name '" + params.getRepositoryAccessInstanceName() + "'"));
    }
    XMOMAccess xmomAccess = new XMOMAccess(params.getXmomAccessName(), revision, instance);
    //aus dem Repository auschecken und deployen
    if(checkout) {
      try {
        xmomAccess.checkout(params.includeXynaProperties(), params.includeCapacities(), params.deploy());
      } catch (Exception e) {
        logger.warn("Failed to update from repository", e);
      }
    }
    instances.put(revision, xmomAccess);
    return xmomAccess;
  }

  public Map<Long, XMOMAccess> listXMOMAccessInstances() {
    return instances;
  }

  public XMOMAccess getXMOMAccessInstance(Long revision) {
    return instances.get(revision);
  }

  public XMOMAccess removeXMOMAccessInstance(Long revision) throws PersistenceLayerException {
    XMOMAccess removed = instances.remove(revision);
    if (removed != null) {
      XMOMAccessInstanceStorable.delete(removed);
    }
    
    return removed;
  }
  
  
  
  private class XMOMAccessEventHandler implements ProjectCreationOrChangeListener {
    
    public void projectCreatedOrModified(Collection<? extends ProjectCreationOrChangeEvent> events, Long revision, String commitMsg) {
      XMOMAccess xa = getXMOMAccessInstance(revision);
      if (xa != null) {
        xa.handleProjectEvents(events, commitMsg);
      }
    }
    
  }

}

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
package com.gip.xyna.xdev.xlibdev.repositoryaccess;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.xlibdev.codeaccess.parameters.AssignCodeAccessInstanceParameters;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xdev.xlibdev.xmomaccess.parameters.AssignXMOMAccessInstanceParameters;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.CodeAccessClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;


public class RepositoryAccessManagement extends FunctionGroup {
  
  public static final String DEFAULT_NAME = "RepositoryAccessManagement";
  public static final int FUTURE_EXECUTION__REPOSITORY_INSTANCE_INITIALIZATION = XynaFactory.getInstance().getFutureExecution().nextId();

  private final static Pattern REPOSITORY_ACCESS_INSTANCE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
  
  private Map<String, RepositoryAccess> instances;
  private Map<String, Class<?>> registeredRepositoryAccessClasses;
  
  
  public RepositoryAccessManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    instances = new ConcurrentHashMap<String, RepositoryAccess>();
    registeredRepositoryAccessClasses = new ConcurrentHashMap<String, Class<?>>();
    
    ODSImpl.getInstance().registerStorable(RepositoryAccessStorable.class);
    ODSImpl.getInstance().registerStorable(RepositoryAccessInstanceStorable.class);
    
    loadCodeAccesses();
    FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
    fe.execAsync(new FutureExecutionTask(FUTURE_EXECUTION__REPOSITORY_INSTANCE_INITIALIZATION) {

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
        return new int[] {WorkflowDatabase.FUTURE_EXECUTION_ID};
      }
    });
    
  }


  private void loadCodeAccesses() throws PersistenceLayerException {
    RepositoryAccessStorable[] allRegisteredCodeAccesses = RepositoryAccessStorable.getAll();
    for (RepositoryAccessStorable cas : allRegisteredCodeAccesses) {
      loadCodeAccess(cas.getName(), cas.getFqClassName());
    }
  }


  private void loadCodeAccess(String name, String fqClassName) {
    try {
      CodeAccessClassLoader cacl = new CodeAccessClassLoader(fqClassName);
      Class<?> clazz = cacl.loadClass(fqClassName);
      if (!RepositoryAccess.class.isAssignableFrom(clazz)) {
        throw new RuntimeException("CodeAccess class must extend " + RepositoryAccess.class.getName());
      }
      registeredRepositoryAccessClasses.put(name, clazz);
    } catch (XFMG_JarFolderNotFoundException e) {
      logger.warn(null, e);
    } catch (ClassNotFoundException e) {
      logger.warn(null, e);
    }
  }


  private void loadPersistedCodeAccessInstances() throws
      PersistenceLayerException {
    RepositoryAccessInstanceStorable[] allInstances = RepositoryAccessInstanceStorable.getAll();
    for (RepositoryAccessInstanceStorable cais : allInstances) {
      try {
        instantiateRepositoryAccessInstanceInternally(cais.getName(), cais.getTypename(), cais.getLocalRepositoryBase(), cais.getParameters());
      } catch (XDEV_CodeAccessInitializationException e) {
        logger.warn("could not initialize repository access instance named " + cais.getName(), e);
        //FIXME disabled merken, damit man später manuell erneut versuchen kann zu enablen!
      } catch (RuntimeException e) {
        logger.warn("could not initialize repository access instance named " + cais.getName(), e);
        //FIXME disabled merken, damit man später manuell erneut versuchen kann zu enablen!
      }
    }
  }


  /**
   * registriert repositoryaccess. erfordert ein unterverzeichnis server/repositoryAccess/&lt;name&gt;
   */
  public void registerRepositoryAccess(String name, String fqClassName) throws PersistenceLayerException {
    loadCodeAccess(name, fqClassName);
    RepositoryAccessStorable.persist(name, fqClassName);
  }


  public Map<String, Class<?>> listRegisteredRepositoryAccesses() {
    return registeredRepositoryAccessClasses;
  }


  public Map<String, RepositoryAccess> listRepositoryAccessInstances() {
    return instances;
  }

  public RepositoryAccess instantiateRepositoryAccessInstance(InstantiateRepositoryAccessParameters parameters, Long revision)
      throws XDEV_CodeAccessInitializationException {
    List<String> paramList = StringParameter.paramStringMapToList(parameters.getParameterMap());
    String repositoryAccessInstanceName = parameters.getRepositoryAccessInstanceName();
    RepositoryAccess repositoryAccess = instantiateRepositoryAccessInstance(repositoryAccessInstanceName, parameters.getRepositoryAccessName(), paramList, revision);
    
    //ggf. XMOMAccess und CodeAccess gleich mit anlegen
    try {
      String xmomAccessName = parameters.getXmomAccessName();
      String codeAccessName = parameters.getCodeAccessName();
      //zuerst die xmls und jars auschecken und nach saved kopieren
      if (xmomAccessName != null) {
        AssignXMOMAccessInstanceParameters xmomAccessParams = new AssignXMOMAccessInstanceParameters();
        xmomAccessParams.setXmomAccessName(xmomAccessName);
        xmomAccessParams.setRepositoryAccessInstanceName(repositoryAccessInstanceName);
        XynaFactory.getInstance()
                   .getXynaDevelopment()
                   .getXynaLibraryDevelopment()
                   .getXMOMAccessManagement()
                   .assignXMOMAccessInstance(xmomAccessParams, revision);
      }
      
      //dann die CodeAccess-Komponenten bauen und deployen
      if (codeAccessName != null) {
        AssignCodeAccessInstanceParameters codeAccessParams = new AssignCodeAccessInstanceParameters();
        codeAccessParams.setCodeAccessName(codeAccessName);
        codeAccessParams.setRepositoryAccessInstanceName(repositoryAccessInstanceName);

        XynaFactory.getInstance()
                   .getXynaDevelopment()
                   .getXynaLibraryDevelopment()
                   .getCodeAccessManagement()
                   .assignCodeAccessInstance(revision, codeAccessParams);
      }
      
      //als letztes die XMOMAccess-Komponenten deployen
      if (xmomAccessName != null) {
        XynaFactory.getInstance()
                   .getXynaDevelopment()
                   .getXynaLibraryDevelopment()
                   .getXMOMAccessManagement()
                   .deployXMOMAccessComponent(null, revision);
      }
    } catch (XynaException e) {
      throw new XDEV_CodeAccessInitializationException(e);
    }
    
    return repositoryAccess;
  }
  
  public RepositoryAccess instantiateRepositoryAccessInstance(String repositoryAccessInstanceName, String repositoryAccessName, List<String> parameter, Long revision)
    throws XDEV_CodeAccessInitializationException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    String localRepositoryBase = RevisionManagement.getPathForRevision(PathType.ROOT, revision, !revisionManagement.isWorkspaceRevision(revision));
    
    RepositoryAccess instance = instantiateRepositoryAccessInstanceInternally(repositoryAccessInstanceName, repositoryAccessName, localRepositoryBase, parameter);
    try {
      RepositoryAccessInstanceStorable.create(repositoryAccessInstanceName, repositoryAccessName, localRepositoryBase, parameter);
    } catch (PersistenceLayerException e) {
      throw new XDEV_CodeAccessInitializationException(e);
    }
    
    return instance;
  }
  
  
  private RepositoryAccess instantiateRepositoryAccessInstanceInternally(String repositoryAccessInstanceName, String repositoryAccessName, String localRepositoryBase, List<String> parameter)
      throws XDEV_CodeAccessInitializationException {
    RepositoryAccess instance;
    Matcher nameMatcher = REPOSITORY_ACCESS_INSTANCE_NAME_PATTERN.matcher(repositoryAccessInstanceName);
    if (!nameMatcher.matches()) {
      throw new XDEV_CodeAccessInitializationException(new IllegalArgumentException("The repository access name must satisfy the regular expression " + REPOSITORY_ACCESS_INSTANCE_NAME_PATTERN.pattern()));
    }
    Class<?> repositoryAccessClass = registeredRepositoryAccessClasses.get(repositoryAccessName);
    if (repositoryAccessClass == null) {
      throw new XDEV_CodeAccessInitializationException(new IllegalArgumentException("No RepositoryAccess registered under name '" + repositoryAccessName + "'."));
    }
    if (instances.containsKey(repositoryAccessInstanceName)) {
      throw new XDEV_CodeAccessInitializationException(new IllegalArgumentException("A repositoryAccessInstance with the same name does already exist"));
    }
    try {
      instance = (RepositoryAccess) repositoryAccessClass.getConstructor().newInstance();
      Map<String, Object> paramMap = StringParameter.paramListToMap(instance.getParameterInformation(), parameter);
      instance.init(repositoryAccessInstanceName, repositoryAccessName, localRepositoryBase, paramMap);
      instances.put(repositoryAccessInstanceName, instance);
    } catch (Exception e) {
      throw new XDEV_CodeAccessInitializationException(e);
    }
    
    return instance;
  }


  public void removeRepositoryAccessInstance(String repositoryAccessName) throws PersistenceLayerException {
    RepositoryAccess removed = instances.remove(repositoryAccessName);
    if (removed != null) {
      removed.shutdown();
      RepositoryAccessInstanceStorable.delete(removed);
    }
  }


  public RepositoryAccess getRepositoryAccessInstance(String repositoryAccessName) {
    return instances.get(repositoryAccessName);
  }


  @Override
  protected void shutdown() throws XynaException {
    shutdownRepositoryAccessInstances();
  }


  private void shutdownRepositoryAccessInstances() {
    for (RepositoryAccess ca : instances.values()) {
      ca.shutdown();
    }
    instances.clear();
    registeredRepositoryAccessClasses.clear();
  }


  public List<PluginDescription> listRepositoryAccessImpls() {
    List<PluginDescription> ret = new ArrayList<PluginDescription>();
    
    Map<String, Class<?>> registeredRepositoryAccesses = XynaFactory.getInstance().getXynaDevelopment()
                    .getXynaLibraryDevelopment().getRepositoryAccessManagement().listRegisteredRepositoryAccesses();
    for (Entry<String, Class<?>> registeredCodeAccess : registeredRepositoryAccesses.entrySet()) {
      Class<?> clazz = registeredCodeAccess.getValue();
      RepositoryAccess ra;
      try {
        ra = (RepositoryAccess) clazz.newInstance();
        PluginDescription description = 
            PluginDescription.create(PluginType.repositoryAccess).
            name(registeredCodeAccess.getKey()).
            label(ra.getLabel()).
            parameters(ParameterUsage.Create, ra.getParameterInformation()).
            build();
        ret.add(description);
      } catch (InstantiationException e) {
        String msg = "Could not get parameter information for " + registeredCodeAccess.getKey() + " -> " + clazz
                        .getName();
        throw new RuntimeException(msg, e);
      } catch (IllegalAccessException e) {
        String msg = "Could not get parameter information for " + registeredCodeAccess.getKey() + " -> " + clazz
                        .getName();
        throw new RuntimeException(msg, e);
      }
    }
    
    return ret;
  }

  

}

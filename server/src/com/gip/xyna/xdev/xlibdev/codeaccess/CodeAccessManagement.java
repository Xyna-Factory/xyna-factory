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
package com.gip.xyna.xdev.xlibdev.codeaccess;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeListener;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.BuildFailure;
import com.gip.xyna.xdev.xlibdev.codeaccess.parameters.AssignCodeAccessInstanceParameters;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilter;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilterParser;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilters;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilters.TerminalStreamOperation;
import com.gip.xyna.xfmg.xods.filter.Filter.WhiteListFilter;
import com.gip.xyna.xfmg.xods.filter.StringMapper;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class CodeAccessManagement extends FunctionGroup {

  /**
   * revision -> instance
   */
  private Map<Long, CodeAccess> instances;
  
  public static XynaPropertyBuilds<ClassMapFilter<GenerationBase>> globalCodeAccessFilter;
  
  // TODO ugly
  public static boolean updateRegeneratedClasses = false; 


  public CodeAccessManagement() throws XynaException {
    super();
  }


  public static final String DEFAULT_NAME = "CodeAccessManagement";


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    instances = new ConcurrentHashMap<Long, CodeAccess>();
    
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
    
    final String fqXmlName_MAPPER_IDENTIFIER = "fqXmlName";
    ClassMapFilters.getInstance().registerMapper(GenerationBase.class.getSimpleName(), new StringMapper<GenerationBase>() {
      public String getIdentifier() {
        return fqXmlName_MAPPER_IDENTIFIER;
      }
      public String map(GenerationBase instance) {
        return instance.getOriginalFqName();
      }
    });
    final String isFactoryComponent_MAPPER_IDENTIFIER = "isFactoryComponent";
    ClassMapFilters.getInstance().registerMapper(GenerationBase.class.getSimpleName(), new StringMapper<GenerationBase>() {
      public String getIdentifier() {
        return "isFactoryComponent";
      }
      public String map(GenerationBase instance) {
        return String.valueOf(instance.isXynaFactoryComponent());
      }
    });

    try {
      globalCodeAccessFilter =
        new XynaPropertyBuilds<ClassMapFilter<GenerationBase>>("xdev.xlibdev.codeaccess.globalfilter", 
                        new com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder<ClassMapFilter<GenerationBase>>() {

                          public ClassMapFilter<GenerationBase> fromString(String string)
                                          throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
                            ClassMapFilter<GenerationBase> sf = ClassMapFilterParser.<GenerationBase>build(string);
                            if (sf == null) {
                              throw new ParsingException("Global codeAccess filter could not be parsed: " + string);
                            } else {
                              return sf;
                            }
                          }

                          public String toString(ClassMapFilter<GenerationBase> value) {
                            return value.toString();
                          }}, "class(" + GenerationBase.class.getSimpleName() + ")" +
                              ".map(" + isFactoryComponent_MAPPER_IDENTIFIER + ")" +
                              ".filter(" + WhiteListFilter.IDENTIFIER + "(" + Boolean.FALSE.toString() + "))" +
                              "." +  TerminalStreamOperation.allMatch.toString() + "()")
        .setDefaultDocumentation(DocumentationLanguage.EN, "See 'xynafactory.sh explainclassmapfilters' for a syntax definition")
        .setDefaultDocumentation(DocumentationLanguage.DE, "Siehe 'xynafactory.sh explainclassmapfilters' f�r die Syntax-Definition");
    } catch (ParsingException e) {
      logger.warn("Could not initialize global CodeAccesFilter");
    }
    
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(DependencyRegister.class, "DependencyRegister").
      after(DeploymentHandling.class). // TODO actually no longer is a DeploymentHandler 
      execAsync(new Runnable() { public void run() { 
        CodeAccessEventHandler callbacks = new CodeAccessEventHandler();
        ProjectCreationOrChangeProvider.getInstance().addListener(DEFAULT_NAME, callbacks);
      } });
    
  }



  private void loadPersistedCodeAccessInstances() throws
      PersistenceLayerException {
    
    ODSImpl.getInstance().registerStorable(CodeAccessInstanceStorable.class);
    ODSImpl.getInstance().registerStorable(BuildFailures.class);
    
    CodeAccessInstanceStorable[] allInstances = CodeAccessInstanceStorable.getAll();
    for (CodeAccessInstanceStorable cais : allInstances) {
      try {
        CodeAccess ca = initCodeAccessInstance(cais.getRevision(), cais.getName(), cais.getRepositoryAccessName());
        Collection<BuildFailure> failures = BuildFailures.restore(cais.getName(), cais.getRevision());
        ca.restoreBuildFailures(failures);
      } catch (XDEV_CodeAccessInitializationException e) {
        logger.warn("could not initialize code access instance for revision " + cais.getRevision(), e);
        //FIXME disabled merken, damit man sp�ter manuell erneut versuchen kann zu enablen!
      } catch (RuntimeException e) {
        logger.warn("could not initialize code access instance for revision " + cais.getRevision(), e);
        //FIXME disabled merken, damit man sp�ter manuell erneut versuchen kann zu enablen!
      }
    }
    
    
  }



  public Map<Long, CodeAccess> listCodeAccessInstances() {
    return instances;
  }


  // TODO allow for reassignment of repositoryaccess
  public void assignCodeAccessInstance(Long revision, AssignCodeAccessInstanceParameters parameters) throws XynaException {
    if (instances.containsKey(revision)) {
      throw new RuntimeException("Revision " + revision + " already contains a CodeAccess instance.");
    }
    initCodeAccessInstance(revision, parameters.getCodeAccessName(), parameters.getRepositoryAccessInstanceName());
    CodeAccessInstanceStorable.create(revision, parameters.getRepositoryAccessInstanceName(), parameters.getCodeAccessName());
  }


  //TODO if there can be more then one instance it would make sense to not only use a global filter but offer a instance specific one as well 
  private CodeAccess initCodeAccessInstance(Long revision, String codeAccessName, String repositoryAccessInstanceName)
      throws XDEV_CodeAccessInitializationException {
    RepositoryAccessManagement ram = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
    RepositoryAccess instance = ram.getRepositoryAccessInstance(repositoryAccessInstanceName);
    if (instance == null) {
      throw new XDEV_CodeAccessInitializationException(new IllegalArgumentException("There is no repositoryAccessInstance with the name '" + repositoryAccessInstanceName + "'"));
    }
    CodeAccess codeAccess = new CodeAccess(codeAccessName, revision, instance);
    instances.put(revision, codeAccess);
    return codeAccess;
  }


  public CodeAccess removeCodeAccessInstance(Long revision) throws PersistenceLayerException {
    CodeAccess removed = instances.remove(revision);
    if (removed != null) {
      removed.shutdown();
      CodeAccessInstanceStorable.delete(removed);
      BuildFailures.delete(removed.getName(), revision);
    }
    
    return removed;
  }


  public CodeAccess getCodeAccessInstance(Long revision) {
    return instances.get(revision);
  }


  @Override
  protected void shutdown() throws XynaException {
    shutdownCodeAccessInstances();
  }


  private void shutdownCodeAccessInstances() {
    for (CodeAccess ca : instances.values()) {
      try {
        BuildFailures.store(ca.getName(), ca.getBuildFailures().values(), ca.getRevision());
      } catch (PersistenceLayerException e) {
        logger.warn("Could not persist buildFailures", e);
      }
      ca.shutdown();
    }
    instances.clear();
  }

  
  private class CodeAccessEventHandler implements ProjectCreationOrChangeListener {
    
    public void projectCreatedOrModified(Collection<? extends ProjectCreationOrChangeEvent> events, Long revision, String commitMsg) {
      CodeAccess ca = getCodeAccessInstance(revision);
      if (ca != null) {
        ca.handleProjectEvents(events);
      }
      
      //f�r bestimmte Events (XMOM �nderungen, RuntimeContext Dependency �nderungen) m�ssen auch die
      //abh�ngigen RuntimeContexte (mit CodeAccess) benachrichtigt werden, damit diese das mdm.jar aktualisieren
      List<ProjectCreationOrChangeEvent> depEvents = new ArrayList<ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent>();
      for (ProjectCreationOrChangeEvent event : events) {
        if (event.getType().equals(EventType.XMOM_MODIFICATION)
             || event.getType().equals(EventType.XMOM_MOVE)
             || event.getType().equals(EventType.XMOM_DELETE)
             || event.getType().equals(EventType.RUNTIMECONTEXT_DEPENDENCY_MODIFICATION)) {
          depEvents.add(event);
        }
      }
      
      if (!depEvents.isEmpty()) {
        RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        Set<Long> parents = new HashSet<Long>();
        rcdm.getParentRevisionsRecursivly(revision, parents);
        for (Long parentRevision : parents) {
          ca = getCodeAccessInstance(parentRevision);
          if (ca != null) {
            ca.handleProjectEvents(depEvents);
          }
        }
      }
    }
    
  }


}

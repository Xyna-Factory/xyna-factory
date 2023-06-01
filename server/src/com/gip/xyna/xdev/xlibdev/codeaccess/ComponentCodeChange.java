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
package com.gip.xyna.xdev.xlibdev.codeaccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ComponentType;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.FileUpdate;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ModificationType;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;


/**
 * kapselt die informationen über die svn änderungen an einer xyna komponente
 */
public class ComponentCodeChange implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * bei shared libs der name des shared lib
   * bei services der fq-xmlname des datatypes
   * bei filtern und triggern der name
   */
  private final String componentName;
  private final ComponentType compType;
  private ModificationType modType;
  protected List<ComponentCodeChange> changedSubComponent = new ArrayList<ComponentCodeChange>(); //bei shared libs die infos zu den geänderten sharedlib jars (ohne .jar endung)
  private List<RepositoryItemModification> modifiedJavaFiles = new ArrayList<RepositoryItemModification>();
  private List<FileUpdate> modifiedJars = new ArrayList<FileUpdate>();


  ComponentCodeChange(String componentName, ComponentType compType) {
    this.componentName = componentName;
    this.compType = compType;
  }


  public ComponentCodeChange(String componentName, ComponentType compType, ModificationType modType) {
    this(componentName, compType);
    this.modType = modType;
  }


  ComponentType getComponentType() {
    return compType;
  }


  public ModificationType getModificationType() {
    return modType;
  }


  public void setModificationType(ModificationType modType) {
    this.modType = modType;
  }
  

  public RepositoryItemModification[] getModifiedJavaFiles() {
    return modifiedJavaFiles.toArray(new RepositoryItemModification[modifiedJavaFiles.size()]);
  }


  public void addModifiedJavaFiles(RepositoryItemModification modifiedJavaFile) {
    modifiedJavaFiles.add(modifiedJavaFile);
  }


  /**
   * ergibt sich aus dem svn output. files sind die aus dem svn-dir
   */
  public List<FileUpdate> getModifiedJars() {
    return modifiedJars;
  }


  public void addModifiedJars(FileUpdate fileupdate) {
    modifiedJars.add(fileupdate);
  }


  public List<ComponentCodeChange> getSubComponentChanges() {
    return changedSubComponent;
  }


  public void addChangedSubComponent(ComponentCodeChange subComponent) {
    changedSubComponent.add(subComponent);
  }


  /**
   * pfad ab Komponententyp (trigger/filter, etc) innerhalb des lokal ausgecheckten svn, der folgende kinder enthält:
   * - src
   * - lib
   * etc. 
   * also z.b. services/a.b.c.Service
   */
  public String getBasePath() {
    return compType.getProjectSubFolder() + Constants.fileSeparator + componentName;
  }


  public Filter getFilter(long revision) throws ComponentNotRegistered, PersistenceLayerException {
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    try {
      return xat.getFilter(revision, componentName, false);
    } catch (XACT_FilterNotFound e) {
      throw new ComponentNotRegistered("Filter unknown in Xyna Factory: " + componentName);
    }
  }


  public Trigger getTrigger(long revision) throws ComponentNotRegistered, PersistenceLayerException {
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    try {
      return xat.getTrigger(revision, componentName, false);
    } catch (XACT_TriggerNotFound e) {
      throw new ComponentNotRegistered("Trigger unknown in Xyna Factory: " + componentName); //TODO manchmal ist das ganz normal. z.b. wenn man den trigger removed hat, aber er sich noch im svn befindet.
    }
  }


  public String getComponentPathName() {
    try {
      return GenerationBase.transformNameForJava(componentName);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }


  public String getComponentOriginalName() {
    return componentName;
  }

  
  public DOM getDOM(long revision, GenerationBaseCache generationCache) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    return DOM.getOrCreateInstance(getComponentOriginalName(), generationCache, revision);
  }


  public DOM getParsedDOMFromSaved(long revision) throws XPRC_InheritedConcurrentDeploymentException,
      AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InvalidPackageNameException {
    //TODO caching
    return DOM.generateUncachedInstance(getComponentOriginalName(), false, revision);
  }


  @Override
  public String toString() {
    return compType.toString() + " '" + componentName + "': " + modType.toString();
  }


  public ModificationType getModType() {
    return modType;
  }
  
  
  public static class ComponentNotRegistered extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ComponentNotRegistered(String msg) { super(msg); }
  }

}

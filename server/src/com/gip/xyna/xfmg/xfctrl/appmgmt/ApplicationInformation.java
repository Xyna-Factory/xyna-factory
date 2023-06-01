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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeDependencyContextInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;



public class ApplicationInformation implements Serializable, RuntimeDependencyContextInformation {

  private static final long serialVersionUID = -3370986562994778303L;
  
  private String name;
  private String version;
  private ApplicationState state;
  private String comment;
  private int objectCount;
  private Collection<RuntimeDependencyContext> requirements;
  private List<RuntimeContextProblem> problems;
  private String buildDate;
  private Map<DocumentationLanguage,String> description;
  private boolean remoteStub;
  private Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntranceStates;
  
  public ApplicationInformation(String name, String version, ApplicationState state, String comment) {
    this.name = name;
    this.version = version;
    this.state = state;
    this.comment = comment;
  }
  
  @Override
  public String toString() {
    return "ApplicationInformation("+name+","+version+",...)";
  }
  
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  public ApplicationState getState() {
    return state;
  }
  
  public void setState(ApplicationState state) {
    this.state = state;
  }
  
  public int getObjectCount() {
    return objectCount;
  }
  
  public void setObjectCount(int objectCount) {
    this.objectCount = objectCount;
  }

  public String getComment() {
    return comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  
  public Collection<RuntimeDependencyContext> getRequirements() {
    return requirements;
  }
  
  public void setRequirements(Collection<RuntimeDependencyContext> requirements) {
    this.requirements = requirements;
  }
  
  public List<RuntimeContextProblem> getProblems() {
    return problems;
  }
  
  public void setProblems(List<RuntimeContextProblem> problems) {
    this.problems = problems;
  }

  public String getBuildDate() {
    return buildDate;
  }

  public void setBuildDate(String buildDate) {
    this.buildDate = buildDate;
  }

  public Map<DocumentationLanguage, String> getDescription() {
    return description;
  }

  public void setDescription(Map<DocumentationLanguage, String> description) {
    this.description = description;
  }
  
  public boolean getRemoteStub() {
    return remoteStub;
  }

  public void setRemoteStub(boolean remoteStub) {
    this.remoteStub = remoteStub;
  }

  public RuntimeDependencyContextType getRuntimeDependencyContextType() {
    return RuntimeDependencyContextType.Application;
  }
  
  public Map<OrderEntrance, SerializablePair<Boolean, String>> getOrderEntrances() {
    return orderEntranceStates;
  }
  
  public void setOrderEntrances(Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntranceStates) {
    this.orderEntranceStates = orderEntranceStates;
  }

  public RuntimeContext asRuntimeContext() {
    return new Application(name, version);
  }
  
  
}

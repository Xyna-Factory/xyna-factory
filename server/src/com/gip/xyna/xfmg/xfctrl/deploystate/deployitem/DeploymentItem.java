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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.io.File;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentificationBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;



public class DeploymentItem extends DeploymentItemIdentificationBase implements DeploymentItemIdentifier {

  private Map<DeploymentLocation, Set<DeploymentItemInterface>> publishedInterfaces;
  private Map<DeploymentLocation, Set<DeploymentItemInterface>> interfaceEmployment;
  private File savedImplJar;
  private boolean locationContentChanges;
  private boolean incomplete;
  private boolean deployed;
  private boolean applicationItem;
  private long lastModified;
  private long lastDeployed;
  private String label;
  
  // only for tests
  public DeploymentItem(String name, XMOMType type) {
    super(type, name);
    publishedInterfaces = new EnumMap<DeploymentLocation, Set<DeploymentItemInterface>>(DeploymentLocation.class);
    publishedInterfaces.put(DeploymentLocation.SAVED, new HashSet<DeploymentItemInterface>());
    publishedInterfaces.put(DeploymentLocation.DEPLOYED, new HashSet<DeploymentItemInterface>());
    interfaceEmployment = new EnumMap<DeploymentLocation, Set<DeploymentItemInterface>>(DeploymentLocation.class);
    interfaceEmployment.put(DeploymentLocation.SAVED, new HashSet<DeploymentItemInterface>());
    interfaceEmployment.put(DeploymentLocation.DEPLOYED, new HashSet<DeploymentItemInterface>());
  }
  
  DeploymentItem(String name, XMOMType type,
                  Map<DeploymentLocation, Set<DeploymentItemInterface>> publishedInterfaces,
                  Map<DeploymentLocation, Set<DeploymentItemInterface>> interfaceEmployment,
                  boolean deployed, boolean locationContentChanges, boolean incomplete, boolean applicationItem) {
    super(type, name);
    this.publishedInterfaces = publishedInterfaces;
    this.interfaceEmployment = interfaceEmployment;
    this.locationContentChanges = locationContentChanges;
    this.incomplete = incomplete;
    this.deployed = deployed;
    this.applicationItem = applicationItem;
  }
  
  
  public DeploymentItem(String name, String specialType) {
    this(name, (XMOMType) null);
    setSpecialType(specialType);
  }

  public Map<DeploymentLocation, Set<DeploymentItemInterface>> getPublishedInterfaces() {
    return publishedInterfaces;
  }
  
  public Map<DeploymentLocation, Set<DeploymentItemInterface>> getInterfaceEmployment() {
    return interfaceEmployment;
  }
  
  public void addPublishedInterface(DeploymentLocation location, DeploymentItemInterface dii) {
    publishedInterfaces.get(location).add(dii);
  }
  
  public void addInterfaceEmployment(DeploymentLocation location, DeploymentItemInterface dii) {
    interfaceEmployment.get(location).add(dii);
  }

  public File getSavedImplJar() {
    return savedImplJar;
  }
  
  public void setSavedImplJar(File file) {
    savedImplJar = file;
  }
  
  public boolean isDeployed() {
    return deployed;
  }
  
  public void setDeployed(boolean deployed) {
    this.deployed = deployed;
  }
  
  public boolean locationContentChanges() {
    return locationContentChanges;
  }
  
  public void setLocationContentChanges(boolean locationContentChanges) {
    this.locationContentChanges = locationContentChanges;
  }
  
  public boolean isIncomplete() {
    return incomplete;
  }
  
  public void setIncomplete(boolean incomplete) {
    this.incomplete = incomplete;
  }
  
  
  public boolean isApplicationItem() {
    return applicationItem;
  }
  
  public void setApplicationItem(boolean applicationItem) {
    this.applicationItem = applicationItem;
  }
  
  
  public long getLastModified() {
    return lastModified;
  }
  
  
  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }
  
  
  public long getLastDeployed() {
    return lastDeployed;
  }
  
  public void setLastDeployed(long lastDeployed) {
    this.lastDeployed = lastDeployed;
  }
  
  
  public String getLabel() {
    return label;
  }
  
  
  public void setLabel(String label) {
    this.label = label;
  }
  
}

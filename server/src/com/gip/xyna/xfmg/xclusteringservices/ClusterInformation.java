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

package com.gip.xyna.xfmg.xclusteringservices;

import java.io.Serializable;
import java.util.List;



public class ClusterInformation implements Serializable {

  private static final long serialVersionUID = 1L;

  private ClusterState clusterState;

  private long id;
  private String description;
  private String extendedInformation;
  private String ownNodeInformation;
  private List<String> otherNodeInformation;

  
  public ClusterInformation() {
  };


  
  public String getOwnNodeInformation() {
    return ownNodeInformation;
  }


  
  public void setOwnNodeInformation(String ownNodeInformation) {
    this.ownNodeInformation = ownNodeInformation;
  }


  
  public List<String> getOtherNodeInformation() {
    return otherNodeInformation;
  }


  
  public void setOtherNodeInformation(List<String> otherNodeInformation) {
    this.otherNodeInformation = otherNodeInformation;
  }


  
  public void setId(long id) {
    this.id = id;
  }


  
  public void setDescription(String description) {
    this.description = description;
  }


  public ClusterInformation(long id, String description) {
    this.id = id;
    this.description = description;
  }


  public long getId() {
    return id;
  }


  public String getDescription() {
    return description;
  }


  public void setClusterState(ClusterState state) {
    this.clusterState = state;
  }


  public ClusterState getClusterState() {
    return this.clusterState;
  }


  public String getExtendedInformation() {
    return extendedInformation;
  }
  
  
  public void setExtendedInformation(String extendedInformation) {
    this.extendedInformation = extendedInformation;
  }

}

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

package com.gip.xyna.xprc.xsched;

import java.io.Serializable;



public class CapacityInformation implements Serializable {

  private static final long serialVersionUID = 291543425648701156L;
  
  private String name;
  private int cardinality;
  private int inuse;
  private CapacityManagement.State state;
  public int binding; //FIXME package private, damit in CapacityManagement verwendbar, aber nicht von au�en sichtbar
  //m�sste daf�r aber in com.gip.xyna.xprc.xsched.capacities umziehen
  
  public CapacityInformation(String name, int cardinality, int inuse, CapacityManagement.State state) {
    this.name = name;
    this.cardinality = cardinality;
    this.inuse = inuse;
    this.state = state;
  }
  
  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setCardinality(int cardinality) {
    this.cardinality = cardinality;
  }

  public int getCardinality() {
    return cardinality;
  }

  public void setInuse(int inuse) {
    this.inuse = inuse;
  }

  public int getInuse() {
    return inuse;
  }

  public void setState(CapacityManagement.State state) {
    this.state = state;
  }

  public CapacityManagement.State getState() {
    return state;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (o == this)
      return true;
    if (!(o instanceof CapacityInformation))
      return false;
    CapacityInformation other = (CapacityInformation) o;
    if (other.getName().equals(getName()) &&
                    other.getCardinality()== getCardinality() &&
                    other.getInuse()==getInuse() &&
                    other.getState().equals(getState()))
      return true;
    return false;
  }

  private transient int hashCode = 0;


  @Override
  public int hashCode() {
    int h = hashCode;
    if (h == 0) {
      h = name.hashCode() ^ (5 * cardinality) ^ (17 * inuse) ^ state.hashCode();
      hashCode = h;
    }
    return h;
  }


}

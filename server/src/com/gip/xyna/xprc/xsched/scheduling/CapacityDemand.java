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
package com.gip.xyna.xprc.xsched.scheduling;

import java.io.Serializable;


/**
 *
 */
public class CapacityDemand implements Serializable {
  private static final long serialVersionUID = 1L;
  
  protected String capName;
  protected double sumUrgencies;
  protected long maxUrgency;
  protected int count;
  protected int maxDemand;
  protected long sentTime;


  /**
   * Anlegen ohne Bedarf
   * @param capName
   */
  public CapacityDemand(String capName) {
    this.capName = capName;
    sumUrgencies = 0;
    maxUrgency = Long.MIN_VALUE;
    count = 0;
    this.maxDemand = 0;
  }
  
  /**
   * Anlegen ohne Bedarf
   * @param capName
   */
  public CapacityDemand(String capName, int maxDemand) {
    this.capName = capName;
    sumUrgencies = 0;
    maxUrgency = Long.MIN_VALUE;
    count = 0;
    this.maxDemand = maxDemand;
  }

  public CapacityDemand(CapacityDemand capacityDemand) {
    capName = capacityDemand.capName;
    sumUrgencies = capacityDemand.sumUrgencies;
    maxUrgency = capacityDemand.maxUrgency;
    count = capacityDemand.count;
  }

  public String toString() {
    return "CapacityDemand("+capName+","+maxUrgency+","+count+","+sumUrgencies+")";
  }
  
  @Override
  public boolean equals(Object obj) {
    if( ! (obj instanceof CapacityDemand) ) {
      return false;
    }
    CapacityDemand other = (CapacityDemand) obj;
    return capName.equals(other.capName)
    && sumUrgencies == other.sumUrgencies
    && maxUrgency == other.maxUrgency
    && count == other.count;
  }

  private transient int hash;

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h =
          capName.hashCode() ^ Double.valueOf(sumUrgencies).hashCode() ^ (Long.valueOf(maxUrgency).hashCode() * 17)
              ^ (count * 5);
      hash = h;
    }
    return h;
  }

  /**
   * @return
   */
  public String getCapName() {
    return capName;
  }

  /**
   * Ist der Bedarf bereits gestillt?
   * @return
   */
  public boolean isFullfilled() {
    return count == 0;
  }

  /**
   * @param urgency
   * @param demand
   * @return true, falls maximaler Demand erreicht wurde
   */
  public boolean addUrgency(long urgency, int demand) {
    if( count < maxDemand ) {
      int add = demand;
      //if( count+add > maxDemand ) {
      //  add = maxDemand-count;
      //} doch ruhig mehr fordern, damit es in jedm Fall reicht. Sonst wäre folgendes möglich:
      //3 Caps vorhanden, 2 auf A, 1 auf B. Knoten B möchte mit hoher Urgency Auftrag mit 2 Caps schedulen,
      //Knoten A einen Auftrag mit niedriger Urgency. Was passiert? A erfüllt zuerst die Forderung von B und 
      //gibt eine Cap an B ab und fordert dann maximal 1 Cap von B. B kann nun schedulen und gibt eine Cap 
      //an A zurück. Nun fehlt A noch eine Cap, die er jedoch nicht mehr fordert, da er den gleichen Bedarf 
      //bereits geschickt hat.
      if( add > 0 ) {
        sumUrgencies += 1.0*add*urgency;
        maxUrgency = maxUrgency>urgency?maxUrgency:urgency; //sollte nie wechseln
        count += add;
      }
      return count >= maxDemand;
    } else {
      return true;
    }
  }

  public void increaseMaxDemand(int inc) {
    maxDemand += inc;
  }
  
  public long getMaxUrgency() {
    return maxUrgency;
  }

  public int getCount() {
    return count;
  }

  public int getMaxDemand() {
    return maxDemand;
  }

  public long getSentTime() {
    return sentTime;
  }

  public void setSentTime(long sentTime) {
    this.sentTime = sentTime;
  }
  
}

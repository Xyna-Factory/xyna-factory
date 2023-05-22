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
package com.gip.xyna.xprc.xsched.scheduling;

import com.gip.xyna.xfmg.xods.configuration.XynaProperty;


/**
 * Achtung: equals enth�lt nur node und capName! und ist nicht kompatibel in verwendung in gemischten listen von 
 * verschiedenen klassen die von capacitydemand abgeleitet sind.
 */
public class CapacityDemandForNode extends CapacityDemand implements Comparable<CapacityDemandForNode> {
  private static final long serialVersionUID = 1L;
  
  private int node;
  
  public CapacityDemandForNode(int node, String capName) {
    super(capName);
    this.node = node;
  }
  
  /**
   * @param node
   * @param capacityDemand
   */
  public CapacityDemandForNode(int node, CapacityDemand capacityDemand) throws IllegalArgumentException {
    super(capacityDemand);
    this.node = node;
    if( capacityDemand.capName ==null ) { 
      throw new IllegalArgumentException( "capName == null");
    }
    this.capName = capacityDemand.capName;
    if( capacityDemand.count <=0 ) { 
      throw new IllegalArgumentException( "count <=0");
    }
    
    this.count = capacityDemand.count;
    this.maxUrgency = capacityDemand.maxUrgency;
    this.sumUrgencies = capacityDemand.sumUrgencies;
    
    long penalty = XynaProperty.SCHEDULER_CAPACITY_DEMAND_FOREIGN_PENALTY.get();
    if( penalty != 0 ) {
      this.maxUrgency -= penalty;
      this.sumUrgencies -= count*penalty;
    }
  }

  /** 
   * equals vergleicht nur node und capName!
   * 
   * ACHTUNG: equals ist dadurch nicht mehr symmetrisch. macht aber nichts, weil die oberklassen objekte nie
   * gemischt in collections mit diesen objekten vorkommen.
   */
  @Override
  public boolean equals(Object obj) {
    if( obj instanceof CapacityDemandForNode ) {
      CapacityDemandForNode o = (CapacityDemandForNode)obj;
      return this.node == o.node && this.capName.equals(o.capName);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return node ^ capName.hashCode();
  }
  
  public int compareTo(CapacityDemandForNode o) {
    if( this.node == o.node && this.capName.equals(o.capName) ) {
      return 0; //equals
    }
    
    if( maxUrgency < o.maxUrgency ) { //dringenderen Bedarf bevorzugen
      return -1;
    }
    if( maxUrgency > o.maxUrgency ) {
      return 1;
    }
    //eigentlich sollte nur nach maxUrgency sortiert werden. Damit aber equals mit compareTo
    //vertraeglich ist, werden hier weitere Vergleiche durchgefuehrt
    int diff = count - o.count; //hoeheren Bedarf bevorzugen
    if( diff != 0 ) {
      return diff;
    }
    if( sumUrgencies < o.sumUrgencies ) { //dringenderen Bedarf bevorzugen
      return -1;
    }
    if( sumUrgencies > o.sumUrgencies ) {
      return 1;
    }
    return hashCode()-o.hashCode(); //letzte Moeglichkeit, um Gleichheit zu verhindern
  }

  /**
   * Bedarf um 1 verringern
   */
  public void decrement() {
    if( count == 1 ) {
      --count;
      return;
    }
    long avg = (long)((sumUrgencies+count-1)/count); //bisherige durchschnittliche Urgency, mit (count-1) gegen Rundungsfehler gesichert
    //maxUrgency wird entfernt, d.h. Summe und Anzahl aendern sich
    sumUrgencies -= maxUrgency;
    --count;
    //nun muss neue maxUrgency ermittelt werden
    if( count == 1 ) {
      //wenn nur eine Urgency vorhanden ist, ist Maximum und Durchschnitt gleich
      maxUrgency = (long)sumUrgencies;
    } else {
      //dies ist nun etwas Raterei, da nicht die korrekte Liste aller urgencies 
      //uebermittelt wurde
      //Vorschlag: bisherige durchschnittliche Urgency
      maxUrgency = avg;
    }
  }
  
  public String toString() {
    return "CapacityDemandForNode("+node+","+capName+","+maxUrgency+","+count+","+sumUrgencies+")";
  }

  public int getNode() {
    return node;
  }

  public void decrement(int dec) {
    if( dec == count ) {
      count = 0;
      return;
    }
    for( int d=0;d<dec; ++d) {
      decrement();
    }
  }

}

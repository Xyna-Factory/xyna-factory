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
package com.gip.xyna.utils.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.gip.xyna.utils.collections.TaggedOrderedCollection;
import com.gip.xyna.utils.scheduler.UrgencyOrderList.Urgency;

/**
 * Speicherung der zu scheduulenden Aufträge sortiert nach Urgency und gruppiert nach Tags, 
 * um ganze Tag-Gruppen überspringen zu können. 
 *
 */
public class UrgencyOrderList<O> extends TaggedOrderedCollection<Urgency<O>> {
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("UrgencyOrderList(");
    sb.append(untaggedData.size()).append(" untagged");
    for( Map.Entry<String,List<Urgency<O>>> entry : taggedData.entrySet() ) {
      sb.append(", ").append(entry.getValue().size()).append(" ").append(entry.getKey());
    }
    sb.append(")");
    return sb.toString();
  }
  
  public int getWaitingForTags() {
    int size = 0;
    for( Map.Entry<String, List<Urgency<O>>> entry : taggedData.entrySet() ) {
      if( entry.getKey() != null ) {
        size += entry.getValue().size();
      }
    }
    return size;
  }
  
  public int getWaitingForTag(String tag) {
    List<Urgency<O>> list = taggedData.get(tag);
    if( list == null ) {
      return 0;
    } else {
      return list.size();
    }
  }
  
  public int getUntaggedSize() {
    return untaggedData.size();
  }
  
  public void reorder( BlockingQueue<Urgency<O>> orders ) {
    ArrayList<Urgency<O>> toReorder = new ArrayList<Urgency<O>>();
    orders.drainTo(toReorder);
    
    if( Scheduler.logger.isDebugEnabled() ) {
      Scheduler.logger.debug(" Scheduler has to reorder "+toReorder.size() +" orders" );
    }
    for( Urgency<O> uo : toReorder ) {
      boolean removed = removeByOrderId(uo.getOrderId()); //TODO ziemlich teuer...
      if( removed ) {
        //in untagged wieder eintragen
        add(uo);
      } else {
        //reorder ist falsch: die SchedulingOrder war nicht im Scheduler
        //Trotzdem in Scheduler aufnehmen, evtl. war ja nicht bekannt, dass Auftrag nicht im Scheduler war
        add(uo);
      }
    }
  }

  public boolean removeByOrderId(Long id) {
    if( removeByOrderId(untaggedData, id) ) {
      return true;
    }
    for( List<Urgency<O>> tagged : taggedData.values() ) {
      if( removeByOrderId(tagged, id) ) {
        return true;
      }
    }
    return false; 
  }

  private boolean removeByOrderId( List<Urgency<O>> list, Long id) {
    java.util.Iterator<Urgency<O>> iter = list.iterator();
    while( iter.hasNext() ) {
      if( id.equals( iter.next().getOrderId() ) ) {
        iter.remove();
        return true;
      }
    }
    return false;
  }
  
  /**
  * Wrapper für einen Auftrag O, damit die berechnete Urgency gespeichert werden kann. 
  * "Note: this class has a natural ordering that is inconsistent with equals."
  */
  public static class Urgency<O> implements Comparable<Urgency<O>> {

    protected long urgency;
    protected O order;
    protected Long orderId;

    public Urgency(Long orderId,  O order, long urgency) {
      this.orderId = orderId;
      this.order = order;
      this.urgency = urgency;
    }
    
    @Override
    public String toString() {
     return "UrgencyOrder("+orderId+","+urgency+","+order+")";
    }
    

    public O getOrder() {
      return order;
    }

    public long getUrgency() {
      return urgency;
    }
    
    public void setUrgency(long urgency) {
      this.urgency = urgency;
    }
    
    public Long getOrderId() {
      return orderId;
    }

    public int compareTo(Urgency<O> uo) {
      //hohe Urgency zuerst, d.h hier anderes Vorzeichen als üblich
      //return (o.urgency < urgency ? -1 : (o.urgency == urgency ? 0 : 1));
      if( uo.urgency < urgency ) {
        return -1; //höhere Urgency, d.h nach vorne
      } else if( uo.urgency == urgency ) {
        //gleiche Urgency ist unwahrscheinlich...
        return orderId.compareTo(uo.orderId); //kleinere orderId nach vorne
      } else {
        return 1; //niedrigere Urgency, d.h nach hinten
      }
    }

  }
  
}
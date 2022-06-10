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
package com.gip.xyna.xprc.xsched.capacities;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xprc.xsched.CapacityStorable;


/**
 *
 *
 */
public class CapacityStorables extends AbstractList<CapacityStorable> {

  private ArrayList<CapacityStorable> all;
  private ArrayList<CapacityStorable> others;
  private CapacityStorable own;
  private int ownBinding;
  
  public CapacityStorables( Collection<CapacityStorable> allCaps, int ownBinding ) {
    all = new ArrayList<CapacityStorable>(allCaps);
    others = new ArrayList<CapacityStorable>(allCaps);
    this.ownBinding = ownBinding;
    for( CapacityStorable cs : all ) {
      if( cs.getBinding() == ownBinding ) {
        own = cs;
        break;
      }
    }
    others.remove(own);
  }
  
  public CapacityStorables( int ownBinding ) {
    all = new ArrayList<CapacityStorable>();
    others = new ArrayList<CapacityStorable>();
    this.ownBinding = ownBinding;
  }
  
  @Override
  public boolean add(CapacityStorable cs) {
    all.add(cs);
    if( cs.getBinding() == ownBinding ) {
      own = cs;
    } else {
      others.add(cs);
    }
    return true;
  }
  
  @Override
  public CapacityStorable get(int index) {
    return all.get(index);
  }

  @Override
  public int size() {
    return all.size();
  }

  public CapacityStorable getOwn() {
    return own;
  }

  /**
   * Gesamt-Cardinality, wird jedesmal neu berechnet, da sich Cardinalities häufig ändern
   */
  public int getTotalCardinality() {
    int total = 0;
    for( CapacityStorable cs : all ) {
      total += cs.getCardinality();
    }
    return total;
  }

  /**
   * @return
   */
  public List<CapacityStorable> getOthers() {
    return others;
  }

  /**
   * @param binding
   * @return
   */
  public CapacityStorable getBinding(int binding) {
    for( CapacityStorable cs : all ) {
      if( cs.getBinding() == binding ) {
        return cs;
      }
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see java.util.AbstractCollection#toString()
   */
  @Override
  public String toString() {
    if( all.isEmpty() ) {
      return "CapacityStorables()";
    } else {
      StringBuilder sb = new StringBuilder(100);
      sb.append("CapacityStorables(");
      sb.append("name=").append(all.get(0).getName());
      sb.append(",totalCardinality=").append(getTotalCardinality());
      sb.append(",ownBinding=").append(ownBinding);
      sb.append(",caps={");
      for( CapacityStorable cs : all) {
        sb.append(cs.getBinding()).append("->").append(cs.getCardinality()).append(",");
      }
      sb.setCharAt(sb.length()-1, '}');
      sb.append(")");
      return sb.toString();
    }
  } 
  
 
}

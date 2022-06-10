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
package com.gip.xyna.utils.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;


public class CycleUtils {
  
  
  public interface CycleController<E /* Element */, C /* Cycle representation*/> {
    
    Set<E> getBranchingElements(E element);
    
    void addToCycle(C cycleRepresentation, E element);
    
    C newCycle();
    
  }

  
  public static <E, C, H extends CycleController<E,C>> Collection<C> collectCycles(E startElement, H helper) {
    Set<E> encountered = new HashSet<E>();
    Collection<Pair<E, C>> cycles = collectCyclesRecursivly(startElement, helper, encountered);
    return CollectionUtils.transform(cycles, new Transformation<Pair<E, C>, C>() {

      public C transform(Pair<E, C> from) {
        return from.getSecond();
      }
      
    });
  }
  
  
  private static <E, C, H extends CycleController<E,C>> Collection<Pair<E, C>> collectCyclesRecursivly(E element, H helper, Set<E> encountered) {
    Set<E> newEncountered = new HashSet<E>(encountered); // new set per branch
    List<Pair<E, C>> cycles = new ArrayList<Pair<E, C>>();
    if (newEncountered.add(element)) {
      Collection<E> branches = helper.getBranchingElements(element);
      for (E branch : branches) {
        Collection<Pair<E, C>> newCycles = collectCyclesRecursivly(branch, helper, newEncountered);
        for (Pair<E, C> pair : newCycles) {
          if (pair.getFirst() != null) {
            // add to cycle
            helper.addToCycle(pair.getSecond(), element);
            if (pair.getFirst().equals(element)) {
              // terminate the cycle
              pair.setFirst(null);
              // TODO reverse the cycle? or call something like end cycle
            }
          }
        }
        cycles.addAll(newCycles);
      }
      return cycles;
    } else {
      // Cylce end detected...now bubble upwards & collect
      return Collections.singletonList(Pair.of(element, helper.newCycle()));
    }
  }
  
  
  public static <E, C, H extends CycleController<E,C>> Set<E> collectNodes(E startElement, H helper) {
    Set<E> nodes = new HashSet<E>();
    collectNodesRecursivly(startElement, helper, nodes);
    return nodes;
  }
  
  
  private static <E, C, H extends CycleController<E,C>> void collectNodesRecursivly(E element, H helper, Set<E> encountered) {
    if (encountered.add(element)) {
      Collection<E> branches = helper.getBranchingElements(element);
      for (E branch : branches) {
        collectNodesRecursivly(branch, helper, encountered);
      }
    } else {
      return;
    }
  }
  
  

}

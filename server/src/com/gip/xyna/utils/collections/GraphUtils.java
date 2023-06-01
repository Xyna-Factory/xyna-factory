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
package com.gip.xyna.utils.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GraphUtils {

  public interface ConnectedEdges<T> {
    public Collection<T> getConnectedEdges(T t);
  }
  
  /**
   * Sammle alle Knoten, die von dem Startknoten aus erreichbar sind (Unterstützt Zyklen).
   * @param includingStartNode falls false, ist der startnode nur einhalten, falls er von einem anderen knoten aus erreichbar ist
   */
  public static <T> Set<T> collectConnectedNodes(ConnectedEdges<T> connected, T start, boolean includingStartNode) {
    Set<T> nodes = new HashSet<T>();
    if (includingStartNode) {
      nodes.add(start);
    }
    collect(connected, start, nodes);
    return nodes;
  }
  
  private static <T> void collect(ConnectedEdges<T> connected, T current, Set<T> nodes) {
    Collection<T> coll = connected.getConnectedEdges(current);
    if (coll != null) {
      for (T t : coll) {
        if (nodes.add(t)) {
          collect(connected, t, nodes);
        }
      }
    }
  }
}

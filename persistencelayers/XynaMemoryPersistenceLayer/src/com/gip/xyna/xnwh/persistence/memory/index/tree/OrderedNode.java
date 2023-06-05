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
package com.gip.xyna.xnwh.persistence.memory.index.tree;


public class OrderedNode<E extends Comparable<E>> extends SimpleIndexNode<E> {

  public OrderedNode(Root root, E value) {
    super(root, value);
  }
  
  //Reihenfolge ist immer. valueOfNode < valueOfChild0 < valueOfChild1 <...
  //      d.h. das kleinste element im teilbaum ist das element selbst.

  @Override
  public int addChild(AbstractNode<E> child) {
    //position suchen, wo das kind reinpasst
    int position = children.size();
    for (int i = 0;i<children.size(); i++) {
      AbstractNode<E> c = children.get(i);
      int compare = c.getValue().compareTo(child.getValue());
      if (compare == 0) {
        throw new RuntimeException("value must be different to existing nodes");
      }
      if (compare > 0) {
        //child at pos i > newChild => insert
        position = i;
        break;
      }
    }
    super.addChild(position, child);
    return position;
  }

  
 

}

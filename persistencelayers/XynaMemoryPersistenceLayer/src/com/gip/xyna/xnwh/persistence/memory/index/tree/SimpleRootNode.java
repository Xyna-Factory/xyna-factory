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
package com.gip.xyna.xnwh.persistence.memory.index.tree;


public class SimpleRootNode<E> extends SimpleIndexNode<E> implements Root {

  private int maxChildren;
  private int maxDepth = 1;
  private int[] maxSubTreeSizes;
  
  public SimpleRootNode(int maxChildren) {
    super(null, null);
    root = this;       
    this.maxChildren = maxChildren;
    
    maxSubTreeSizes = new int[30];
      int result = 1;
      for (int d = 0; d<30; d++) {
        //maxChildren ^ d berechnen
        int t = 1;
        for (int i = 0; i<d+1; i++) {
          t *= maxChildren;
        }
        maxSubTreeSizes[d] = result;
        result+=t;
      }

  }

  
  public int getMaxDepth() {
    return maxDepth;
  }


  public int getMaxChildren() {
    return maxChildren;
  }

  public void incrementDepth() {
    maxDepth++;
  }

  public int getMaxSubTreeSize(int depth) {
    return maxSubTreeSizes[depth];
  }

}

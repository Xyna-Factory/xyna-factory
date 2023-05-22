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
package com.gip.xyna.xmcp.xguisupport.messagebus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TrappedPathTree<L extends TrappedPathTree.TrappedPathLeaf> {

  private TrappedPathNode<L> root;
  private LeafFilter<L> allLeafesFilterInstance = new AllLeafsFilter<L>();
  
  public TrappedPathTree() {
    root = new TrappedPathNode<L>();
  }
  
  public void insert(L leaf) {
    root.insert(leaf, 0);
  }
  
  public <P extends Pathable> void trap(P path, Trap<L> trap) {
    root.trap(path, 0, trap);
  }
  
  public <P extends Pathable> void disarm(P path, Trap<L> trap) {
    root.disarm(path, 0, trap);
  }
  
  public <P extends Pathable> Set<L> getLeafs(P path) {
    return root.getAllLeafs(path, 0);
  }
  
  public <P extends Pathable> void removeLeaf(P path, LeafFilter<L> filter) {
    root.removeLeaf(path, 0, filter);
  }
  
  public <P extends Pathable> void removeLeaf(P path) {
    removeLeaf(path, allLeafesFilterInstance);
  }
  
  public static class TrappedPathNode<L extends TrappedPathLeaf> {
    
    private final Map<String, TrappedPathNode<L>> children;
    private final Set<Trap<L>> traps;
    private Set<L> leafs;
    
    private TrappedPathNode() { // TODO lazy
      children = new HashMap<String, TrappedPathNode<L>>();
      traps = new HashSet<Trap<L>>();
      leafs = new HashSet<L>();
    }
    
    void insert(L leaf, int pathIndex) {
      TrappedPathNode<L> next = next(leaf, pathIndex);
      if (next == null) {
        if (!leaf.isTrapBait()) {
          leafs.add(leaf);
        }
      } else {
        next.insert(leaf, pathIndex + 1);
      }
      for (Trap<L> trap : traps) {
        trap.trigger(leaf);
      }
    }
    
    
    <P extends Pathable> void trap(P pathable, int pathIndex, Trap<L> trap) {
      TrappedPathNode<L> next = next(pathable, pathIndex);
      if (next == null) {
        traps.add(trap);
      } else {
        next.trap(pathable, pathIndex + 1, trap);
      }
    }
    
    
    <P extends Pathable> void disarm(P pathable, int pathIndex, Trap<L> trap) {
      TrappedPathNode<L> next = next(pathable, pathIndex);
      if (next == null) {
        traps.remove(trap);
      } else {
        next.disarm(pathable, pathIndex + 1, trap);
      }
    }
    
    
    <P extends Pathable> Set<L> getAllLeafs(P pathable, int pathIndex) {
      TrappedPathNode<L> next = next(pathable, pathIndex);
      if (next == null) {
        return getAllLeafs();
      } else {
        return next.getAllLeafs(pathable, pathIndex + 1);
      }
    }
    
    
    <P extends Pathable> void removeLeaf(P pathable, int pathIndex, LeafFilter<L> filter)  {
      TrappedPathNode<L> next = next(pathable, pathIndex);
      if (next == null) {
        Set<L> prunedLeafs = new HashSet<L>();
        for (L leaf : leafs) {
          if (!filter.accept(leaf)) {
            prunedLeafs.add(leaf);
          }
        }
        leafs = prunedLeafs;
      } else {
        next.removeLeaf(pathable, pathIndex + 1, filter);
      }
    }
    
    private Set<L> getAllLeafs() {
      Set<L> results = new HashSet<L>();
      results.addAll(leafs);
      for (TrappedPathNode<L> child : children.values()) {
        results.addAll(child.getAllLeafs());
      }
      return results;
    }
    
    
    private <P extends Pathable> TrappedPathNode<L> next(P pathable, int pathIndex) {
      String[] path = pathable.getPath();
      if (path.length == pathIndex) {
        return null;
      } else {
        String nextPathPart = path[pathIndex];
        TrappedPathNode<L> node = children.get(nextPathPart);
        if (node == null) {
          node = new TrappedPathNode<L>();
          children.put(nextPathPart, node);
        }
        return node;
      }
    }
    
    
  }
  
  public static interface Pathable {
    
    public String[] getPath();
    
  }
  
  
  public static interface Trap<L extends TrappedPathLeaf> {
    
    public void trigger(L victim);
    
  }
  
  
  public static interface TrappedPathLeaf extends Pathable {
    
    public boolean isTrapBait();
    
  }
  
  
  public static interface LeafFilter<L extends TrappedPathLeaf> {
    
    public boolean accept(L leaf);
    
  }
  
  
  static class AllLeafsFilter<L extends TrappedPathLeaf> implements LeafFilter<L> {

    public boolean accept(L leaf) {
      return true;
    }
    
  }
  
  
  static class MultiLeafFilter<L extends TrappedPathLeaf> implements LeafFilter<L> {
    
    private List<LeafFilter<L>> filters = new ArrayList<LeafFilter<L>>();
    private final boolean acceptIfOne; // else acceptIfAll
    
    
    MultiLeafFilter(boolean acceptIfOne) {
      this.acceptIfOne = acceptIfOne;
    }
    
    public boolean accept(L leaf) {
      for (LeafFilter<L> filter : filters) {
        if (filter.accept(leaf) && acceptIfOne) {
          return true;
        }
        if (!filter.accept(leaf) && !acceptIfOne) {
          return false;
        }
      }
      return !acceptIfOne;
    }
    
    
    public void addFilter(LeafFilter<L> filter) {
      filters.add(filter);
    }
    
    
    public void clearFilter() {
      filters.clear();
    }
    
  }
  
}

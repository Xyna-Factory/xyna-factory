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



import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.memory.index.tree.IndexImplTree.HasSize;



public class Rebalancer<E extends HasSize> {

  public static int cnt;//TODO kann raus, ist nur für tests
  private static final Logger logger = CentralFactoryLogging.getLogger(Rebalancer.class);


  public void rebalanceNode(AbstractNode<E> node, boolean mayRecurseParent) {
    cnt++;

    synchronized (nodesToRebalance) {
      nodesToRebalance.remove(node);
    }

    LockedOrderedNode<?> lockedNode = null;
    if (node instanceof LockedOrderedNode) {
      lockedNode = (LockedOrderedNode<?>) node;
    }
    boolean parentModified = false;
    Set<AbstractNode<E>> toBalance = new HashSet<AbstractNode<E>>();

    if (lockedNode != null) {
      lockedNode.getLock().writeLock().lock();
    }
    try {
      if (node.isInvalid()) {
        return;
      }
      Root root = node.getRoot();
      while (node.isTooBig()) {
        if (node == root) {
          root.incrementDepth();
          continue;
        }
        int size = node.getNumberOfChildren();
        if (size == 1) {
          rebalanceNode(node.getChild(0), false);
        } else if (size > 0) {
          //gib die höchsten size/2 knoten an den parent
          AbstractNode<E> parent = node.getParent();

          for (int i = 0; i < size / 2; i++) {
            int childPos = size - i - 1;
            AbstractNode<E> child = node.removeSubTree(childPos);

            addNodeToTransformLater(child);

            //FIXME performance: welche position? => die des parents+1 => damit kann man sich ggfs noch das automatische durchlaufen aller kinder sparen, was in addChild passiert
            parent.addChild(child);
            parentModified = true;
          }
        }
      }

      int indexForFirstMergingCandidate = 0;

      while (node.hasTooManyChildren()) {
        //node hat depth D

        //suches erstes benachbartes paar von kindern, die man mergen kann
        int size = node.getNumberOfChildren();
        boolean mergedNodes = false;
        for (int i = indexForFirstMergingCandidate; i < size - 1; i++) {
          AbstractNode<E> childToMergeTo = node.getChild(i);
          //TODO performance: (i+1)tes kind jetzt ist in nächster iteration das i-te kind => muss man nicht erneut auslesen.
          if (childToMergeTo.getSubTreeSize() + node.getChild(i + 1).getSubTreeSize() <= childToMergeTo
              .getMaxSubTreeSize()) {

            //merging möglich
            AbstractNode<E> removed = node.removeSubTree(i + 1); //removed hat depth D+1

            addNodeToTransformLater(removed);

            int numberOfChildren = childToMergeTo.getNumberOfChildren();
            //i < i+1. d.h. erst removed anhängen, dann all seine kinder.
            int positionLastChildOfRemoved = removed.getNumberOfChildren() - 1;
            for (int j = positionLastChildOfRemoved; j >= 0; j--) {
              AbstractNode<E> childOfRemoved = removed.removeSubTree(j);
              childToMergeTo.addChild(numberOfChildren, childOfRemoved); //kinder haben depth D+2
              addNodeToTransformLater(childOfRemoved);
            }
            childToMergeTo.addChild(numberOfChildren, removed); //depth D+1
            toBalance.add(childToMergeTo);
            mergedNodes = true;
            break;
          } else {
            //kein merging möglich => beim nächsten schleifendurchlauf muss man das nicht nochmal probieren
            indexForFirstMergingCandidate = i + 1;
          }
        }

        if (!mergedNodes) {
          break;
        }
      }
      if (node.getNumberOfChildren() > root.getMaxChildren() * 2) {
        throw new RuntimeException("too many children");
      }
    } finally {
      if (lockedNode != null) {
        lockedNode.getLock().writeLock().unlock();
      }
    }

    if (mayRecurseParent && parentModified) {
      rebalanceNode(node.getParent(), true);
    }
    for (AbstractNode<E> in : toBalance) {
      rebalanceNode(in, false);
    }
  }


  public void transform(AbstractNode<E> node, NodeTypeTransformer<E> nodeTypeTransformer) {

    AbstractNode<E> parent = node.getParent();

    LockedOrderedNode<?> lockedParent = null;
    if (parent instanceof LockedOrderedNode) {
      lockedParent = (LockedOrderedNode<?>) parent;
    }

    if (lockedParent != null) {
      lockedParent.getLock().writeLock().lock();
    }

    lockedParent = transformParentLocked(node, nodeTypeTransformer, lockedParent);
  }


  /**
   * validieren, dass der knoten vom richtigen typ ist. falls nicht, wird der typ angepasst und dann über die kinder
   * rekursiert ACHTUNG: parentknoten muss vor dem aufruf gelockt werden! stellt sicher, dass beim transform-aufruf
   * immer sowohl der parent, als auch der knoten selbst gelockt sind.
   */
  private LockedOrderedNode<?> transformParentLocked(AbstractNode<E> node, NodeTypeTransformer<E> nodeTypeTransformer,
                                                     LockedOrderedNode<?> lockedParent) {
    LockedOrderedNode<?> lockedNode = null;
    try {
      synchronized (nodesToTransform) {
        nodesToTransform.remove(node);
      }
  
      if (node instanceof LockedOrderedNode) {
        lockedNode = (LockedOrderedNode<?>) node;
      }
  
      if (lockedNode != null) {
        lockedNode.getLock().writeLock().lock();
      }
  
      if (!node.isInvalid()) {
        //parentlock hält parentmethode. nodelock hält diese methode
        if (!nodeTypeTransformer.validateNodeType(node)) {
          AbstractNode<E> newNode = node.transform(nodeTypeTransformer);
          synchronized (nodesToRemove) {
            if (nodesToRemove.remove(node)) {
              nodesToRemove.add(newNode);
            }
          }
        }
      }
    } finally {
      if (lockedParent != null) {
        lockedParent.getLock().writeLock().unlock();
      }
    }

    //nodelock ist neues parentlock
    int numberOfChildren = node.getNumberOfChildren();
    if (numberOfChildren > 0) {
      for (int i = 0; i < numberOfChildren; i++) {
        if (i > 0 && lockedNode != null) {
          lockedNode.getLock().writeLock().lock();
        }
        AbstractNode<E> child = node.getChild(i);
        transformParentLocked(child, nodeTypeTransformer, lockedNode);
      }
    } else {
      if (lockedNode != null) {
        lockedNode.getLock().writeLock().unlock();
      }
    }
    return lockedNode;
  }


  private void addNodeToTransformLater(AbstractNode<E> node) {
    synchronized (nodesToTransform) {
      Integer originalDepth = nodesToTransform.get(node);
      if (originalDepth == null) {
        originalDepth = node.getDepth();
        nodesToTransform.put(node, originalDepth);
      }
      //else wurde bereits verschoben
    }
  }


  //FIXME performance: synchronize über cluster hinweg, damit knoten nicht mehrfach rebalanced werden.
  /*
   * zahl ist die anzahl der änderungen an diesem knoten. 
   */
  private Map<AbstractNode<E>, Integer> nodesToRebalance = new HashMap<AbstractNode<E>, Integer>();

  /*
   * zahl ist die original depth des knotens. 
   */
  private Map<AbstractNode<E>, Integer> nodesToTransform = new HashMap<AbstractNode<E>, Integer>();

  private Set<AbstractNode<E>> nodesToRemove = new HashSet<AbstractNode<E>>();


  public void nodeToBeRebalanced(AbstractNode<E> node) {
    synchronized (nodesToRebalance) {
      Integer i = nodesToRebalance.get(node);
      if (i == null) {
        i = 0;
      }
      nodesToRebalance.put(node, i + 1);
    }
  }


  public void nodeToBeRemoved(AbstractNode<E> node) {
    if (node.getParent() == null) {
      throw new RuntimeException();
    }
    synchronized (nodesToRemove) {
      nodesToRemove.add(node);
    }
  }


  private AtomicBoolean isRunning = new AtomicBoolean(false);


  public void rebalanceAll(NodeTypeTransformer<E> nodeTypeTransformer, AbstractNode<E> root) {
    if (!isRunning.compareAndSet(false, true)) {
      return;
    }
    try {
     /* Integer rootval = nodesToRebalance.get(root);
          if ((rootval == null || rootval < 1) && nodesToRebalance.size() < 1) {
            return;
          }*/

      Map<AbstractNode<E>, Integer> copyOfNodes;
      synchronized (nodesToRebalance) {
        copyOfNodes = new HashMap<AbstractNode<E>, Integer>(nodesToRebalance);
      }
      Iterator<AbstractNode<E>> it = copyOfNodes.keySet().iterator();
      //TODO performance: rebalanceNode hat manche der knoten durch interne rekursion evtl schon rebalanced.
      while (it.hasNext()) {
        AbstractNode<E> node = it.next();
        rebalanceNode(node, true);
      }

      if (nodesToRemove.size() > 100) {
        //leere knoten entfernen. dazu muss man nochmal isEmpty() checken, weil sich der leer-status des knotens inzwischen geändert haben könnte
        Set<AbstractNode<E>> copyOfNodesToRemove;
        synchronized (nodesToRemove) {
          copyOfNodesToRemove = new HashSet<AbstractNode<E>>(nodesToRemove);
        }
        it = copyOfNodesToRemove.iterator();
        while (it.hasNext()) {
          AbstractNode<E> node = it.next();
          removeNode(node);
        }
  
      }

      if (nodesToTransform.size() > 100) {
        //  logger.debug("transforming");
        synchronized (nodesToTransform) {
          copyOfNodes = new HashMap<AbstractNode<E>, Integer>(nodesToTransform);
        }
        it = copyOfNodes.keySet().iterator();
        while (it.hasNext()) {
          AbstractNode<E> node = it.next();
          transform(node, nodeTypeTransformer);
        }
      }
    } finally {
      isRunning.set(false);
    }
  }


  private void removeNode(AbstractNode<E> node) {
    synchronized (nodesToRemove) {
      if (!nodesToRemove.remove(node)) {
        return;
      }
    }
    if (node.getRoot() == node) {
      return;
    }
    LockedOrderedNode<?> lockedNode = null;
    if (node instanceof LockedOrderedNode) {
      lockedNode = (LockedOrderedNode<?>) node;
    }

    if (lockedNode != null) {
      lockedNode.getLock().writeLock().lock();
    }
    try {
      if (node.getValue().isEmpty()) {
        AbstractNode<E> parent = node.getParent();
        int parentPosition = parent.getChildIndex(node);
        if (node != parent.removeSubTree(parentPosition)) {
          throw new RuntimeException("expected other node");
        }
        int size = node.getNumberOfChildren();
        for (int i = size - 1; i >= 0; i--) {
          AbstractNode<E> child = node.removeSubTree(i);
          parent.addChild(parentPosition, child);
          if (child.getParent() == null) {
            throw new RuntimeException();
          }
        }
        nodeToBeRebalanced(parent);
      }
    } finally {
      if (lockedNode != null) {
        lockedNode.getLock().writeLock().unlock();
      }
    }
  }

}

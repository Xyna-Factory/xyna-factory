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

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

/*
 * TODO verbesserungsideen:
 * - sizeOfSubTree nicht immer sofort propagieren, weil das entsprechend mehr locking voraussetzt.
 *   =>  
 */
public abstract class AbstractNode<E> {

  private int sizeOfSubTree = 1;
  private int depth = 0;
  private E value;
  
  public AbstractNode(E value) {
    this.value = value;
  }

  public void addChild(int position, AbstractNode<E> child) {
    int sizeOfNewChild = child.getSizeOfSubTree();
    child.setDepthForSubTree(depth+1);
    child.setParent(this);
    addChildInternally(position, child);
    addSize(sizeOfNewChild);
  }
  
  protected abstract void substituteChild(int position, AbstractNode<E> child);
  
  /**
   * gibt position zurück, an der das kind eingefügt wurde
   */
  public int addChild(AbstractNode<E> child) {
    addChild(0, child);
    return 0;
  }
  
  protected abstract void addChildInternally(int position, AbstractNode<E> child);

  private void addSize(int sizeOfNewChild) {
    sizeOfSubTree += sizeOfNewChild;
    if (sizeOfSubTree < 1) {
      throw new RuntimeException();
    }
    AbstractNode<E> parent = getParent();
    if (parent != null) {
      parent.addSize(sizeOfNewChild);
    }
  }

  public abstract void setParent(AbstractNode<E> indexNode);

  private void setDepthForSubTree(int depth) {
    if (depth > getRoot().getMaxDepth()) {
      throw new RuntimeException("depth too big: " + depth);
    }
    this.depth = depth;
    for (AbstractNode<E> n : getChildren()) {
      n.setDepthForSubTree(depth+1);
    }
  }

  public abstract List<AbstractNode<E>> getChildren();

  private int getSizeOfSubTree() {
  /*  int realSizeOfSubTree = calcSizeOfSubTreeRecursively() ;
    if (realSizeOfSubTree != sizeOfSubTree) {
      throw new RuntimeException("size invalid: " + sizeOfSubTree + " real = " + realSizeOfSubTree);
    }*/
    return sizeOfSubTree;
  }
  
  private int calcSizeOfSubTreeRecursively() {
    int r = 1;
    for (AbstractNode<E> n : getChildren()) {
      r+=n.calcSizeOfSubTreeRecursively();
    }
    return r;
  }

  public boolean isTooBig() {
    return sizeOfSubTree > calculateMaxSizeOfSubTree();
  }
  
  private int calculateMaxSizeOfSubTree() {
    Root root = getRoot();
    return root.getMaxSubTreeSize(root.getMaxDepth()-depth);
  }

  public boolean hasTooManyChildren() {
    return getNumberOfChildren() > getRoot().getMaxChildren();
  }

  public abstract AbstractNode<E> getParent();

  public abstract int getNumberOfChildren();

  public abstract AbstractNode<E> getChild(int i);

  private final static Logger logger = CentralFactoryLogging.getLogger(AbstractNode.class);
  
  public AbstractNode<E> removeSubTree(int i) {
    AbstractNode<E> removed = removeChildInternally(i);
    removed.setParent(null);
    int diff = removed.getSizeOfSubTree();
    addSize(-diff);
    return removed;
  }
  
  protected abstract AbstractNode<E> removeChildInternally(int i);

  public abstract Root getRoot();

  public int getSubTreeSize() {
    return sizeOfSubTree;
  }

  public int getMaxSubTreeSize() {
    return calculateMaxSizeOfSubTree();
  }
  
  public void setValue(E value) {
    this.value = value;
  }
  
  public E getValue() {
    return value;
  }
  
  public String toString() {
    String vString;
    if (value == null) {
      vString = "null";
    } else {
      vString = value.toString();
    }
    return "[" + depth + "]" + vString;
  }

  public boolean isOnMaxDepth() {
    return depth == getRoot().getMaxDepth();
  }

  public int getDepth() {
    return depth;
  }
  
  /**
   * gibt den transformierten knoten zurück
   */
  public abstract AbstractNode<E> transform(NodeTypeTransformer<E> transformer);
  
  public abstract int getChildIndex(AbstractNode<E> child);

  public boolean isInvalid() {
    return !(this instanceof Root) && getParent() == null;
  }

  /**
   * kinder und werte von sourceNode kopieren.
   */
  public void copyFields(AbstractNode<E> sourceNode) {
    depth = sourceNode.depth;
    sizeOfSubTree = sourceNode.sizeOfSubTree;
  }
}

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
package com.gip.xyna.xnwh.persistence.memory.index.tree;

import java.util.ArrayList;
import java.util.List;


public class SimpleIndexNode<E> extends AbstractNode<E> {

  Root root;
  private AbstractNode<E> parent;
  //FIXME synchronize
  List<AbstractNode<E>> children = new ArrayList<AbstractNode<E>>();
  
  public SimpleIndexNode(Root root, E value) {
    super(value);
    this.root = root;
  }
  
  @Override
  protected void addChildInternally(int position, AbstractNode<E> child) {
    children.add(position, child);
  }

  @Override
  public AbstractNode<E> getChild(int i) {
    return children.get(i);
  }

  @Override
  public List<AbstractNode<E>> getChildren() {
    return children;
  }

  @Override
  public int getNumberOfChildren() {
    return children.size();
  }

  @Override
  public AbstractNode<E> getParent() {
    return parent;
  }

  @Override
  public Root getRoot() {
    return root;
  }

  @Override
  protected AbstractNode<E> removeChildInternally(int i) {
    return children.remove(i);
  }

  @Override
  public void setParent(AbstractNode<E> parent) {
    this.parent = parent;
  }

  @Override
  public AbstractNode<E> transform(NodeTypeTransformer<E> transformer) {
    int idx = parent.getChildIndex(this);
    if (idx == -1) {
      throw new RuntimeException();
    }
    AbstractNode<E> newNode = transformer.transformNode(this);
    parent.substituteChild(idx, newNode);
    this.parent = null;
    return newNode;
  }

  @Override
  public int getChildIndex(AbstractNode<E> child) {
    for (int i = 0; i< children.size(); i++) {
      AbstractNode<E> c = children.get(i);
      if (c == child) {
        return i;
      }
    }
    return -1;
  }

  @Override
  protected void substituteChild(int position, AbstractNode<E> child) {
    children.set(position, child);
  }

  @Override
  public void copyFields(AbstractNode<E> sourceNode) {
    children = sourceNode.getChildren();
    parent = sourceNode.getParent();
    super.copyFields(sourceNode);
  }
  
  
}

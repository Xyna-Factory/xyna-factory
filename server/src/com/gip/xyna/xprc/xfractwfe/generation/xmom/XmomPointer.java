/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class XmomPointer {

  private final TreePath path;
  private final XmomTree tree;
  private final XmomNodeInfo node;
  private Optional<XmomPointer> parent;

  
  private XmomPointer(TreePath path, XmomTree tree, XmomNodeInfo node, XmomPointer parent) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException("Path is empty");
    }
    this.path = path;
    this.tree = tree;
    this.node = node;
    this.parent = Optional.ofNullable(parent);
  }

  
  public static XmomPointer getRoot(XmomTree tree) {
    XmomNodeInfo node = tree.getRoot();
    TreePathSegment seg = new TreePathSegment(node.getName());
    TreePath path = new TreePath(seg);
    return new XmomPointer(path, tree, node, null);
  }
  
  
  public Optional<XmomPointer> getChild(TreePathSegment seg) {
    Optional<XmomNodeInfo> child = node.getChild(seg);
    if (child.isEmpty()) {
      return Optional.empty();
    }
    XmomPointer ret = new XmomPointer(path.cloneAndAdd(seg), tree, child.get(), this);
    return Optional.ofNullable(ret);
  }
  
  
  public Optional<XmomPointer> getDescendantByPath(String... path) {
    XmomPointer pos = this;
    for (String name : path) {
      TreePathSegment seg = new TreePathSegment(name);
      Optional<XmomPointer> child = pos.getChild(seg);
      if (child.isEmpty()) {
        return Optional.empty();
      }
      pos = child.get();
    }
    return Optional.ofNullable(pos);
  }
  
  
  public String getChildValueOrEmptyString(String name) {
    Optional<XmomPointer> child = this.getDescendantByPath(name);
    if (child.isEmpty()) { return ""; }
    Optional<String> value = child.get().getNodeInfo().getValue();
    if (value.isEmpty()) { return ""; }
    return value.get();
  }
  
  
  public List<XmomPointer> getChildren() {
    List<XmomPointer> ret = new ArrayList<>();
    List<TreePathSegment> list = this.node.getAllChildPathSegments();
    for (TreePathSegment seg : list) {
      Optional<XmomPointer> child = getChild(seg);
      if (child.isEmpty()) {
        TreePath path = this.path.cloneAndAdd(seg);
        throw new RuntimeException("Could not find expected child node: " + path.asString());
      }
      ret.add(child.get());
    }
    return ret;
  }
  
  
  public List<XmomPointer> getChildrenWithName(String name) {
    List<XmomPointer> ret = new ArrayList<>();
    List<TreePathSegment> list = this.node.getChildrenWithName(name);
    for (TreePathSegment seg : list) {
      Optional<XmomPointer> child = getChild(seg);
      if (child.isEmpty()) {
        TreePath path = this.path.cloneAndAdd(seg);
        throw new RuntimeException("Could not find expected child node: " + path.asString());
      }
      ret.add(child.get());
    }
    return ret;
  }
  
  
  public List<XmomPointer> getPrevSiblings() {
    List<XmomPointer> ret = new ArrayList<>();
    List<XmomPointer> list = this.getParent().getChildrenWithName(this.path.getLastSegment().getName());
    int pos = this.path.getLastSegment().getIndex();
    if (pos == 0) {
      return ret;
    }
    for (XmomPointer p : list) {
      if (p.getPath().getLastSegment().getIndex() >= pos) {
        break;
      }
      ret.add(p);
    }
    return ret;
  }
  
  
  public List<XmomPointer> getPrevSiblingsOfAncestors() {
    List<XmomPointer> ret = new ArrayList<>();
    XmomPointer ancestor = this.getParent();
    while (!ancestor.isRoot()) {
      List<XmomPointer> list = ancestor.getPrevSiblings();
      ret.addAll(list);
      ancestor = ancestor.getParent();
    }
    return ret;
  }
  
  
  public XmomPointer getParent() {
    if (parent.isEmpty()) {
      throw new IllegalArgumentException("Error: Root node has no parent.");
    }
    return parent.get();
  }
  
  
  public XmomPointer getRoot() {
    return getRoot(this.tree);
  }
  
  
  public boolean isRoot() {
    return path.isRoot();
  }
  
  
  public TreePath getPath() {
    return path;
  }

  
  public XmomNodeInfo getNodeInfo() {
    return node;
  }
  
}

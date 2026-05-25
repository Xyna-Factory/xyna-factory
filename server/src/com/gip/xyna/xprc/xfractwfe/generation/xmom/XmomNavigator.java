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


public class XmomNavigator {

  private final XmomTree tree;
  

  public XmomNavigator(XmomTree tree) {
    this.tree = tree;
  }

  
  public XmomTree getTree() {
    return tree;
  }


  public XmomNodeInfo getRoot() {
    return tree.getRoot();
  }
  
  
  public Optional<XmomNodeInfo> gotoPath(TreePath path) {
    if (path.getSegments().size() < 1) {
      return Optional.empty();
    }
    String rootName = path.getSegments().get(0).getName();
    if (!rootName.equals(getRoot().getName())) {
      return Optional.empty();
    }
    XmomNodeInfo info = getRoot();
    for (int i = 1; i < path.getSegments().size(); i++) {
      TreePathSegment seg = path.getSegments().get(i);
      Optional<XmomNodeInfo> child = info.getChild(seg);
      if (child.isEmpty()) {
        return Optional.empty();
      }
      info = child.get();
    }
    return Optional.ofNullable(info);
  }
  
  
  public List<TreePath> getAllPathsOfValueNodes() {
    List<TreePath> ret = new ArrayList<>();
    XmomNodeInfo root = getRoot();
    TreePathSegment seg = new TreePathSegment(root.getName());
    handleNodeForValuePaths(root, new TreePath(seg), ret);
    return ret;
  }
  
  
  private void handleNodeForValuePaths(XmomNodeInfo info, TreePath path, List<TreePath> ret) {
    if (info.hasValue()) {
      ret.add(path);
    }
    List<TreePathSegment> segments = info.getAllChildPathSegments();
    for (TreePathSegment seg : segments) {
      Optional<XmomNodeInfo> child = info.getChild(seg);
      TreePath childPath = path.cloneAndAdd(seg);
      handleNodeForValuePaths(child.get(), childPath, ret);
    }
  }
  
}

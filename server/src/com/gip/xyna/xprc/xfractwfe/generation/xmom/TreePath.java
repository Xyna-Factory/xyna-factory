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


public class TreePath {

  private List<TreePathSegment> segments = new ArrayList<>();

  
  public List<TreePathSegment> getSegments() {
    return segments;
  }
  
  public TreePath() {}
  
  public TreePath(TreePathSegment root) {
    segments.add(root);
  }
  
  protected TreePath(TreePath orig) {
    segments.addAll(orig.segments);
  }
  
  public boolean isRoot() {
    return segments.size() == 1;
  }
  
  public boolean isEmpty() {
    return segments.size() < 1;
  }
  
  public TreePath cloneTreePath() {
    return new TreePath(this);
  }
  
  public TreePath cloneAndAdd(TreePathSegment child) {
    TreePath ret = cloneTreePath();
    ret.segments.add(child);
    return ret;
  }
  
  public TreePath cloneAsParentPath() {
    if (isRoot()) {
      throw new IllegalArgumentException("Error: Root path has no parent.");
    }
    TreePath ret = new TreePath();
    for (int i = 0; i < segments.size() - 1; i++) {
      ret.segments.add(segments.get(i));
    }
    return ret;
  }
  
  
  public TreePath cloneAsRootPath() {
    if (isEmpty()) {
      throw new IllegalArgumentException("Error: Path is empty.");
    }
    return new TreePath(segments.get(0));
  }
  
  
  public int getLength() {
    return segments.size();
  }
  
  
  public String asString() {
    StringBuilder s = new StringBuilder();
    for (TreePathSegment seg : segments) {
      s.append(seg.asString());
      s.append("/");
    }
    return s.toString();
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TreePath)) {
      return false;
    }
    TreePath path = (TreePath) obj;
    if (path.getLength() != this.getLength()) {
      return false;
    }
    for (int i = 0; i < this.segments.size(); i++) {
      if (!this.segments.get(i).equals(path.segments.get(i))) {
        return false;
      }
    }
    return true;
  }
  
}

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

package com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher;

import com.gip.xyna.xprc.xfractwfe.generation.xmom.TreePath;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomPointer;

public class NodeMatcherByNameAndPathExclusion implements NodeMatcher {

  private final String matchedName;
  private final TreePath excludedPath;
  
  
  public NodeMatcherByNameAndPathExclusion(String matchedName, TreePath path) {
    if (matchedName == null) {
      throw new IllegalArgumentException("Name to match is null.");
    }
    if (matchedName.isBlank()) {
      throw new IllegalArgumentException("Name to match is empty.");
    }
    if (path.isEmpty()) {
      throw new IllegalArgumentException("Excluded tree path is empty");
    }
    this.matchedName = matchedName;
    this.excludedPath = path;
  }
 

  @Override
  public boolean matches(XmomPointer pointer) {
    if (excludedPath.equals(pointer.getPath())) {
      return false;
    }
    return matchedName.equals(pointer.getNodeInfo().getName());
  }

}

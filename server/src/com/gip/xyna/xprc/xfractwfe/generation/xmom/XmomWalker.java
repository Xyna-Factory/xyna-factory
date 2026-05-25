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


public class XmomWalker {

  public List<XmomPointer> findDescendants(XmomTree tree, NodeMatcher matcher) {
    List<XmomPointer> ret = new ArrayList<>();
    handleNode(tree.getRootPointer(), matcher, ret);
    return ret;
  }
  
  
  private void handleNode(XmomPointer pointer, NodeMatcher matcher, List<XmomPointer> ret) {
    if (matcher.matches(pointer)) {
      ret.add(pointer);
    }
    for (XmomPointer child : pointer.getChildren()) {
      handleNode(child, matcher, ret);
    }
  }
  
  
}

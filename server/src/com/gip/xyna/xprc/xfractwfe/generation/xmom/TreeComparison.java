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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class TreeComparison {

  public static class MatchInfo {
    public List<TreePath> diffList = new ArrayList<>();
    
    public String getReport() {
      StringBuilder ret = new StringBuilder();
      return ret.toString();
    }
    
    public boolean matches() {
      return diffList.size() == 0;
    }
  }
  
  
  public MatchInfo compare(XmomTree tree1, XmomTree tree2, NodeMatcher matcher) {
    MatchInfo ret = new MatchInfo();
    //map, path as string
    
    XmomWalker walker = new XmomWalker();
    List<XmomPointer> list1 = walker.findDescendants(tree1, matcher);
    List<XmomPointer> list2 = walker.findDescendants(tree2, matcher);
    Map<String, XmomPointer> map1 = new HashMap<>();
    Map<String, XmomPointer> map2 = new HashMap<>();
    for (XmomPointer xp : list1) {
      map1.put(xp.getPath().asString(), xp);
    }
    for (XmomPointer xp : list2) {
      map2.put(xp.getPath().asString(), xp);
    }
    for (XmomPointer xp : list1) {
      if (!hasMatch(xp, map2)) {
        ret.diffList.add(xp.getPath());
      }
    }
    for (XmomPointer xp : list2) {
      if (!hasMatch(xp, map1)) {
        ret.diffList.add(xp.getPath());
      }
    }
    return ret;
  }
  
  
  private boolean hasMatch(XmomPointer xp, Map<String, XmomPointer> map) {
    XmomPointer other = map.get(xp.getPath().asString());
    if (other == null) {
      return false;
    }
    Optional<String> opt1 = xp.getNodeInfo().getValue();
    String val1 = opt1.isPresent() ? opt1.get() : "";
    Optional<String> opt2 = other.getNodeInfo().getValue();
    String val2 = opt2.isPresent() ? opt2.get() : "";
    return val1.equals(val2);
  }
  
}

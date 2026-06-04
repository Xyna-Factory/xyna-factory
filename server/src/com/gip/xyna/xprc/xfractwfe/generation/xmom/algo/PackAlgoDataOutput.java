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

package com.gip.xyna.xprc.xfractwfe.generation.xmom.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomPointer;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomTree;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomWalker;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcher;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherByName;


public class PackAlgoDataOutput implements PackAlgorithm {

  @Override
  public void pack(XmomTree tree) {
    NodeMatcher matcher = new NodeMatcherByName(EL.DATA);
    List<XmomPointer> dataList = new XmomWalker().findDescendants(tree, matcher);
    Map<String, XmomPointer> rootChildren = new HashMap<>();
    Map<String, Integer> refCounter = new HashMap<>();
    List<XmomPointer> outputChildren = new ArrayList<>();
    
    for (XmomPointer data : dataList) {
      if (data.getNodeInfo().getIdValue().isEmpty()) { continue; }
      if (data.getParent().isRoot()) {
        rootChildren.put(data.getNodeInfo().getIdValue().get().getValue(), data);
      } else if (EL.OUTPUT.equals(data.getParent().getNodeInfo().getName())) {
        Optional<String> targetRef = getTargetRef(data);
        if (targetRef.isEmpty()) { continue; }
        outputChildren.add(data);
        incRefCounter(targetRef.get(), refCounter);
      }
    }
    for (XmomPointer data : outputChildren) {
      Optional<String> targetRef = getTargetRef(data);
      if (!isRefCountOne(targetRef, refCounter)) { continue; }
      handleOutputChild(data, rootChildren);
    }
  }
  
  
  private void incRefCounter(String ref, Map<String, Integer> refCounter) {
    Integer val = refCounter.get(ref);
    if (val == null) {
      refCounter.put(ref, 1);
    } else {
      refCounter.put(ref, val + 1);
    }
  }
  
  
  private boolean isRefCountOne(Optional<String> ref, Map<String, Integer> refCounter) {
    if (ref.isEmpty()) {
      throw new RuntimeException("Unexpected error in pack algo: Missing target ref id");
    }
    Integer val = refCounter.get(ref.get());
    if (val == null) {
      throw new RuntimeException("Unexpected error in pack algo: No ref count found for ref-id " + ref.get());
    }
    return Integer.valueOf(1).equals(val);
  }

  
  private Optional<String> getTargetRef(XmomPointer data) {
    Optional<XmomPointer> targetRef = data.getParent().getDescendant(EL.TARGET, ATT.REFID);
    if (targetRef.isEmpty()) { return Optional.empty(); }
    if (!targetRef.get().getNodeInfo().getValue().isEmpty()) { return Optional.empty(); }
    return targetRef.get().getNodeInfo().getValue();
  }
  
  
  private void handleOutputChild(XmomPointer data, Map<String, XmomPointer> rootChildren) {
    Optional<XmomPointer> targetRef = data.getParent().getDescendant(EL.TARGET, ATT.REFID);
    String refId = targetRef.get().getNodeInfo().getValue().get();
    XmomPointer target = rootChildren.get(refId);
    if (!dataNodesMatch(data, target)) { return; }
    targetRef.get().getNodeInfo().setIgnore(true);
    if (!targetRef.get().getParent().getNodeInfo().isIgnoreOrEmpty()) {
      targetRef.get().getNodeInfo().setIgnore(false);
      return;
    }
    target.getNodeInfo().setIgnore(true);
    data.getNodeInfo().createChild(PackingConstants.DataOutput.P_ROOT_DATA, "*");
  }
  
  
  private boolean dataNodesMatch(XmomPointer data1, XmomPointer data2) {
    String refName1 = data1.getChildValueOrEmptyString(ATT.REFERENCENAME);
    String refName2 = data2.getChildValueOrEmptyString(ATT.REFERENCENAME);
    String refPath1 = data1.getChildValueOrEmptyString(ATT.REFERENCEPATH);
    String refPath2 = data2.getChildValueOrEmptyString(ATT.REFERENCEPATH);
    if (!refName1.equals(refName2)) { return false; }
    if (!refPath1.equals(refPath2)) { return false; }
    return true;
  }
  
  
  @Override
  public void unpack(XmomTree tree) {
    // TODO Auto-generated method stub
    
  }

}

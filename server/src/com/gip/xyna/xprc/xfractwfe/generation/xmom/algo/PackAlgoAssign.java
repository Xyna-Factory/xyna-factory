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

import java.util.List;
import java.util.Optional;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomPointer;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomTree;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomWalker;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcher;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherByName;


public class PackAlgoAssign implements PackAlgorithm {

  @Override
  public void pack(XmomTree tree) {
    NodeMatcher matcher = new NodeMatcherByName(EL.ASSIGN);
    List<XmomPointer> assignList = new XmomWalker().findDescendants(tree, matcher);
    for (XmomPointer assign : assignList) {
      handleAssignForPack(assign);
    }
  }

  
  private void handleAssignForPack(XmomPointer assign) {
    Optional<XmomPointer> sourceRef1 = assign.getDescendantByPath(EL.SOURCE, ATT.REFID);
    if (sourceRef1.isEmpty()) { return; }
    Optional<XmomPointer> sourceRef2 = assign.getDescendantByPath(EL.COPY, EL.SOURCE, ATT.REFID);
    if (sourceRef2.isEmpty()) { return; }
    Optional<XmomPointer> targetRef1 = assign.getDescendantByPath(EL.TARGET, ATT.REFID);
    if (targetRef1.isEmpty()) { return; }
    Optional<XmomPointer> targetRef2 = assign.getDescendantByPath(EL.COPY, EL.TARGET, ATT.REFID);
    if (targetRef2.isEmpty()) { return; }
    
    if (!sourceRef1.get().getNodeInfo().hasValue()) { return; }
    if (!sourceRef2.get().getNodeInfo().hasValue()) { return; }
    if (!targetRef1.get().getNodeInfo().hasValue()) { return; }
    if (!targetRef2.get().getNodeInfo().hasValue()) { return; }
    
    String sourceRefVal1 = sourceRef1.get().getNodeInfo().getValue().get();
    String sourceRefVal2 = sourceRef2.get().getNodeInfo().getValue().get();
    String targetRefVal1 = targetRef1.get().getNodeInfo().getValue().get();
    String targetRefVal2 = targetRef2.get().getNodeInfo().getValue().get();
    
    if (!sourceRefVal1.equals(sourceRefVal2)) { return; }
    if (!targetRefVal1.equals(targetRefVal2)) { return; }
    
    //TODO: is linktype also possible for target node?
    Optional<XmomPointer> linkTypeSource = assign.getDescendantByPath(EL.COPY, EL.SOURCE, EL.META, EL.LINKTYPE);
    Optional<String> linkTypeSourceVal = Optional.empty();
    if (linkTypeSource.isPresent()) {
      linkTypeSourceVal = linkTypeSource.get().getNodeInfo().getValue();
    }
    // test if packing is possible
    sourceRef1.get().getNodeInfo().setIgnore(true);
    sourceRef2.get().getNodeInfo().setIgnore(true);
    targetRef1.get().getNodeInfo().setIgnore(true);
    targetRef2.get().getNodeInfo().setIgnore(true);
    if (linkTypeSource.isPresent()) {
      linkTypeSource.get().getNodeInfo().setIgnore(true);
    }
    boolean turnedEmpty = assign.getDescendantByPath(EL.SOURCE).get().getNodeInfo().isIgnoreOrEmpty();
    turnedEmpty = turnedEmpty && assign.getDescendantByPath(EL.COPY).get().getNodeInfo().isIgnoreOrEmpty();
    turnedEmpty = turnedEmpty && assign.getDescendantByPath(EL.TARGET).get().getNodeInfo().isIgnoreOrEmpty();
    if (turnedEmpty) {
      // finish packing
      addPackInfo(assign, sourceRefVal1, targetRefVal1, linkTypeSourceVal);
      return;
    }
    // undo
    sourceRef1.get().getNodeInfo().setIgnore(false);
    sourceRef2.get().getNodeInfo().setIgnore(false);
    targetRef1.get().getNodeInfo().setIgnore(false);
    targetRef2.get().getNodeInfo().setIgnore(false);
    if (linkTypeSource.isPresent()) {
      linkTypeSource.get().getNodeInfo().setIgnore(false);
    }
  }
  
  
  private void addPackInfo(XmomPointer assign, String sourceRefVal, String targetRefVal,
                           Optional<String> linkTypeSourceVal) {
    assign.getNodeInfo().createChild(PackingConstants.Assign.P_SOURCE, sourceRefVal);
    assign.getNodeInfo().createChild(PackingConstants.Assign.P_TARGET, targetRefVal);
    if (linkTypeSourceVal.isPresent()) {
      assign.getNodeInfo().createChild(PackingConstants.Assign.P_LINKTYPE_SOURCE, linkTypeSourceVal);
    }
  }
  
  
  @Override
  public void unpack(XmomTree tree) {
    NodeMatcher matcher = new NodeMatcherByName(EL.ASSIGN);
    List<XmomPointer> assignList = new XmomWalker().findDescendants(tree, matcher);
    for (XmomPointer assign : assignList) {
      handleAssignForUnpack(assign);
    }
  }

  
  private void handleAssignForUnpack(XmomPointer assign) {
    if (!assign.getDescendantByPath(EL.SOURCE).isEmpty()) { return; }
    if (!assign.getDescendantByPath(EL.TARGET).isEmpty()) { return; }
    if (!assign.getDescendantByPath(EL.COPY).isEmpty()) { return; }
    Optional<XmomPointer> pSource = assign.getDescendantByPath(PackingConstants.Assign.P_SOURCE);
    if (pSource.isEmpty()) { return; }
    Optional<XmomPointer> pTarget = assign.getDescendantByPath(PackingConstants.Assign.P_TARGET);
    if (pTarget.isEmpty()) { return; }
    Optional<XmomPointer> pLinktypeSource = assign.getDescendantByPath(PackingConstants.Assign.P_LINKTYPE_SOURCE);
    String sourceVal = pSource.get().getNodeInfo().getValueOrEmpty();
    String targetVal = pTarget.get().getNodeInfo().getValueOrEmpty();
    assign.getNodeInfo().createChild(EL.SOURCE).createChild(ATT.REFID, sourceVal);
    assign.getNodeInfo().createChild(EL.TARGET).createChild(ATT.REFID, targetVal);
    assign.getNodeInfo().createChild(EL.COPY);
    assign.getDescendantByPath(EL.COPY).get().getNodeInfo().createChild(EL.SOURCE).createChild(ATT.REFID, sourceVal);
    assign.getDescendantByPath(EL.COPY).get().getNodeInfo().createChild(EL.TARGET).createChild(ATT.REFID, targetVal);
    if (pLinktypeSource.isPresent()) {
      String linkTypeVal = pLinktypeSource.get().getNodeInfo().getValueOrEmpty();
      assign.getDescendantByPath(EL.COPY, EL.SOURCE).get().getNodeInfo().createChild(EL.META).createChild(EL.LINKTYPE, linkTypeVal);
    }
  }
  

}

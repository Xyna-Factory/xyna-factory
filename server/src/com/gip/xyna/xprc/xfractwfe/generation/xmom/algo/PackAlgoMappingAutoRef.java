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
import java.util.List;
import java.util.Optional;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomNodeInfo;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomPointer;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomTree;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomWalker;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcher;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherByName;


public class PackAlgoMappingAutoRef implements PackAlgorithm {

  @Override
  public void pack(XmomTree tree) {
    NodeMatcher matcher = new NodeMatcherByName(EL.MAPPINGS);
    List<XmomPointer> mappings = new XmomWalker().findDescendants(tree, matcher);
    for (XmomPointer p : mappings) {
      handleMappingsNode(p);
    }
  }

  
  private void handleMappingsNode(XmomPointer mappingsNode) {
    String id = mappingsNode.getChildValueOrEmptyString(ATT.ID);
    if (id.isEmpty()) { return; }
    List<XmomPointer> sourceList = new ArrayList<>();
    for (XmomPointer input : mappingsNode.getChildrenWithName(EL.INPUT)) {
      for (XmomPointer data : input.getChildrenWithName(EL.DATA)) {
        sourceList.addAll(data.getChildrenWithName(EL.SOURCE));
      }
    }
    for (XmomPointer source : sourceList) {
      handleSourceNode(mappingsNode, id, source);
    }
  }
  
  
  private void handleSourceNode(XmomPointer mappingsNode, String id, XmomPointer source) {
    Optional<XmomPointer> refId = source.getDescendantByPath(ATT.REFID);
    if (refId.isEmpty()) { return; }
    XmomNodeInfo refIdInfo = refId.get().getNodeInfo();
    String refidVal = refIdInfo.getValueOrEmpty();
    if (!id.equals(refidVal)) { return; }
    refIdInfo.setIgnore(true);
    if (!source.getNodeInfo().isIgnoreOrEmpty()) {
      refIdInfo.setIgnore(false);
      return;
    }
    source.getParent().getNodeInfo().createChild(PackingConstants.MappingAutoRef.P_SOURCE_AUTO, "*");
  }
  
  
  @Override
  public void unpack(XmomTree tree) {
    // TODO Auto-generated method stub
    
  }

}

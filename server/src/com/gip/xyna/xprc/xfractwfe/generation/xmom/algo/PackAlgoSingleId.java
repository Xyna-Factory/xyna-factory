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
import com.gip.xyna.xprc.xfractwfe.generation.xmom.IdValue;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomPointer;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomTree;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomWalker;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcher;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherByName;


public class PackAlgoSingleId implements PackAlgorithm {

  @Override
  public void pack(XmomTree tree) {
    NodeMatcher matcher = new NodeMatcherByName(ATT.ID);
    List<XmomPointer> idList = new XmomWalker().findDescendants(tree, matcher);
    for (XmomPointer p : idList) {
      handleId(p);
    }
  }

  
  private void handleId(XmomPointer idnode) {
    Optional<IdValue> id = idnode.getNodeInfo().getIdValue();
    if (id.isEmpty()) { return; }
    if (id.get().getRefCount() == 1) {
      idnode.getNodeInfo().setIgnore(true);
      idnode.getNodeInfo().createChild(PackingConstants.SingleOutput.P_ID, "*");
    }
  }
  
  
  @Override
  public void unpack(XmomTree tree) {
    // TODO Auto-generated method stub
    
  }
  
}

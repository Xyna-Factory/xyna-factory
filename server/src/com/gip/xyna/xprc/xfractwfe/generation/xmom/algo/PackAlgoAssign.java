

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
      handleAssign(assign);
    }
  }

  
  private void handleAssign(XmomPointer assign) {
    Optional<XmomPointer> sourceRef1 = assign.getDescendant(EL.SOURCE, ATT.REFID);
    if (sourceRef1.isEmpty()) { return; }
    Optional<XmomPointer> sourceRef2 = assign.getDescendant(EL.COPY, EL.SOURCE, ATT.REFID);
    if (sourceRef2.isEmpty()) { return; }
    Optional<XmomPointer> targetRef1 = assign.getDescendant(EL.TARGET, ATT.REFID);
    if (targetRef1.isEmpty()) { return; }
    Optional<XmomPointer> targetRef2 = assign.getDescendant(EL.COPY, EL.TARGET, ATT.REFID);
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
    
    Optional<XmomPointer> linkType = assign.getDescendant(EL.COPY, EL.SOURCE, EL.META, EL.LINKTYPE);
    Optional<String> linkTypeVal = Optional.empty();
    if (linkType.isPresent()) {
      linkTypeVal = linkType.get().getNodeInfo().getValue();
    }
    sourceRef1.get().getNodeInfo().setIgnore(true);
    sourceRef2.get().getNodeInfo().setIgnore(true);
    targetRef1.get().getNodeInfo().setIgnore(true);
    targetRef2.get().getNodeInfo().setIgnore(true);
    if (linkType.isPresent()) {
      linkType.get().getNodeInfo().setIgnore(true);
    }
    if (assign.getNodeInfo().isIgnoreOrEmpty()) {
      addPackInfo(assign, sourceRefVal1, targetRefVal1, linkTypeVal);
      return;
    }
    sourceRef1.get().getNodeInfo().setIgnore(false);
    sourceRef2.get().getNodeInfo().setIgnore(false);
    targetRef1.get().getNodeInfo().setIgnore(false);
    targetRef2.get().getNodeInfo().setIgnore(false);
    if (linkType.isPresent()) {
      linkType.get().getNodeInfo().setIgnore(false);
    }
  }
  
  
  private void addPackInfo(XmomPointer assign, String sourceRefVal, String targetRefVal, Optional<String> linkTypeVal) {
    assign.getNodeInfo().createChild(PackingConstants.Assign.P_SOURCE, sourceRefVal);
    assign.getNodeInfo().createChild(PackingConstants.Assign.P_TARGET, targetRefVal);
    if (linkTypeVal.isPresent()) {
      assign.getNodeInfo().createChild(PackingConstants.Assign.P_LINKTYPE, linkTypeVal);
    }
  }
  
  
  @Override
  public void unpack(XmomTree tree) {
    //TODO
  }

}

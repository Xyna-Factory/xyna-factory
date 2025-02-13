/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

package xdev.yang.impl.usecase;

import java.util.ArrayList;
import java.util.List;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.Deviate;
import org.yangcentral.yangkit.model.api.stmt.DeviateType;
import org.yangcentral.yangkit.model.api.stmt.Deviation;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;

import xdev.yang.impl.YangStatementTranslator;
import xdev.yang.impl.YangStatementTranslator.YangStatementTranslation;
import xmcp.yang.LoadYangAssignmentsData;


public class DeviationTools {

  public DeviationList filterByPath(DeviationList list, SchemaNodePath path) {
    List<Deviation> filtered = new ArrayList<>();
    for (Deviation dev : list.getDeviations()) {
      if (keepDeviation(dev, path)) {
        filtered.add(dev);
      }
    }
    return new DeviationList(filtered);
  }
  
  
  protected boolean keepDeviation(Deviation dev, SchemaNodePath path) {
    if (keepDeviationImpl(dev, path)) { 
      return true; 
    }    
    for (SchemaNodePath snp : path.getContainedPathsOfUsedGroupings()) {
      if (keepDeviationImpl(dev, snp)) {
        return true;
      }
    }    
    return false;
  }
  
  
  protected boolean keepDeviationImpl(Deviation dev, SchemaNodePath path) {
    if (dev.getDeviates() == null) { return false; }
    if (dev.getTargetPath() == null) { return false; }
    if (dev.getTargetPath().getPath() == null) { return false; }
    if (dev.getTargetPath().getPath().size() != path.getLocalnameList().size() + 1) { return false; }
    if (dev.getTargetPath().getPath().size() != path.getNamespaceList().size() + 1) { return false; }
    
    for (int i = 0; i < path.getLocalnameList().size(); i++) {
      String devLocalname = dev.getTargetPath().getPath().get(i).getLocalName();
      String devNamespace = dev.getTargetPath().getPath().get(i).getNamespace().toString();
      String localname = path.getLocalnameList().get(i);
      String namespace = path.getNamespaceList().get(i);
      if (!namespace.equals(devNamespace)) { return false; }
      if (!identifiersAreEqual(localname, devLocalname)) { return false; }
    }
    return true;
  }
  
  
  protected boolean identifiersAreEqual(String id1, String id2) {
    return removeOptionalPrefix(id1).equals(removeOptionalPrefix(id2));
  }
  
  
  protected String removeOptionalPrefix(String id) {
    if (id == null) { return ""; }
    if (!id.contains(":")) { return id; }
    return id.substring(id.indexOf(":") + 1);
  }


  public void handleDeviationsForElement(DeviationList unfiltered, YangStatement node, 
                                         LoadYangAssignmentsData parentData,
                                         LoadYangAssignmentsData nodeData) {
    StringBuilder info = new StringBuilder();
    SchemaNodePath parentPath = new SchemaNodePath(parentData);
    DeviationList deviationsFilteredByParent = filterByPath(unfiltered, parentPath);
    String localname = YangStatementTranslation.getLocalName(node);
    String namespace = YangStatementTranslation.getNamespace(node);
    for (Deviation dev : deviationsFilteredByParent.getDeviations()) {
      String devLocalname = dev.getTargetPath().getLast().getLocalName();
      String devNamespace = dev.getTargetPath().getLast().getNamespace().toString();
      if (identifiersAreEqual(localname, devLocalname) && namespace.equals(devNamespace)) {
        if (hasDeviateType(dev, DeviateType.NOT_SUPPORTED)) {
          nodeData.unversionedSetIsNotSupportedDeviation(true);
          return;
        }
        writeDeviationSubelementsInfo(dev, info);
      }
    }
    if (info.length() > 0) {
      nodeData.unversionedSetDeviationInfo(info.toString());
    }
    
    info = new StringBuilder();
    for (YangElement element : YangStatementTranslation.getSubStatements(node)) {
      if (element instanceof YangStatement) {
        handleDeviationsForChildElement(unfiltered, (YangStatement) element, nodeData, info);
      }
    }    
    if (info.length() > 0) {
      nodeData.unversionedSetSubelementDeviationInfo(info.toString());
    }
  }
  
  
  protected void appendMessage(StringBuilder s, CharSequence msg) {
    if (msg == null) { return; }
    if (s.length() > 0) {
      s.append("; ");
    }
    s.append(msg);
  }
  
  
  protected void handleDeviationsForChildElement(DeviationList unfiltered, YangStatement child,
                                                 LoadYangAssignmentsData parentOfChildData, 
                                                 StringBuilder subinfo) {
    SchemaNodePath path = new SchemaNodePath(parentOfChildData);
    DeviationList filtered = filterByPath(unfiltered, path);
    String localname = YangStatementTranslation.getLocalName(child);
    String namespace = YangStatementTranslation.getNamespace(child);
    for (Deviation dev : filtered.getDeviations()) {
      String devLocalname = dev.getTargetPath().getLast().getLocalName();
      String devNamespace = dev.getTargetPath().getLast().getNamespace().toString();
      if (identifiersAreEqual(localname, devLocalname) && namespace.equals(devNamespace)) {
        if (hasDeviateType(dev, DeviateType.NOT_SUPPORTED)) {
          if (subinfo.length() < 1) { 
            subinfo.append("Deviation removed sub-elements: "); 
          }
          else { 
            subinfo.append(", "); 
          }
          subinfo.append(localname);
          return;
        }
      }
    }
  }
  
  
  private void writeDeviationSubelementsInfo(Deviation dev, StringBuilder info) {    
    if (dev.getDeviates() == null) { return; }
    if (dev.getDeviates().size() < 1) { return; }
    StringBuilder deviateInfo = new StringBuilder();
    boolean isfirst = true;
    for (Deviate deviate : dev.getDeviates()) {
      if (isfirst) { isfirst = false; } else { deviateInfo.append("; "); }
      deviateInfo.append("deviate: ").append(deviate.getArgStr());
      writeDeviationSubelementsInfoImpl(deviate, deviateInfo);
    }
    appendMessage(info, deviateInfo);    
  }
  
  private void writeDeviationSubelementsInfoImpl(YangElement elem, StringBuilder str) {
    if (!(elem instanceof Deviate)) {
      str.append(" ").append(elem.toString());
    }
    if (elem instanceof YangStatement) {
      List<YangElement> list = YangStatementTranslation.getSubStatements((YangStatement) elem);
      if (list == null) { return; }
      for (YangElement item : list) {
        writeDeviationSubelementsInfoImpl(item, str);
      }
    }
  }
  
  
  protected boolean hasDeviateType(Deviation dev, DeviateType dt) {
    if (dev.getDeviates() == null) { return false; }
    for (Deviate deviate : dev.getDeviates()) {
      if (deviate.getDeviateType() == dt) {
        return true;
      }
    }
    return false;
  }

}

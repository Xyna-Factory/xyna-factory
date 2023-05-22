/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.update.outdatedclasses_5_1_4_5;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



@Persistable(primaryKey = XMOMOperationDatabaseEntry.COL_FQNAME, tableName = XMOMOperationDatabaseEntry.TABLENAME)
public class XMOMOperationDatabaseEntry extends XMOMServiceDatabaseEntry {

  public static final String TABLENAME = "xmomoperationcache";
  
  private static final long serialVersionUID = -7301378884111678454L;
  
  
  public XMOMOperationDatabaseEntry() {
  }

  
  public XMOMOperationDatabaseEntry(String fqname) {
    super(fqname);
  }
  
  
  //generate cache entry for operation
  public XMOMOperationDatabaseEntry(DOM dom, String serviceName, JavaOperation op) {
    super(generateFqNameForOperation(dom, serviceName, op), op.getLabel(), dom.getOriginalPath(), generateSimpleNameForOperation(dom, serviceName, op),
          "", "", generateFqNameForServiceGroup(dom, serviceName), op.getInputVars(), op.getOutputVars(), op.getThrownExceptions(), dom.isXynaFactoryComponent());
  }


  private static ResultSetReader<XMOMOperationDatabaseEntry> reader = new ResultSetReader<XMOMOperationDatabaseEntry>() {

    public XMOMOperationDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMOperationDatabaseEntry x = new XMOMOperationDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      x.needs = rs.getString(COL_NEEDS);
      x.produces = rs.getString(COL_PRODUCES);
      x.exceptions = rs.getString(COL_EXCEPTIONS);
      x.groupedBy = rs.getString(COL_GROUPEDBY);
      x.calledBy = rs.getString(COL_CALLEDBY);
      x.usesInstancesOf = rs.getString(COL_USESINSTANCESOF);
      return x;
    }

  };
  
  
  @Override
  public ResultSetReader<? extends XMOMOperationDatabaseEntry> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return getFqname();
  }
  
  
  public static class DynamicXMOMCacheReader implements ResultSetReader<XMOMOperationDatabaseEntry> {

    private Set<XMOMDatabaseEntryColumn> selectedCols;

    public DynamicXMOMCacheReader(Set<XMOMDatabaseEntryColumn> selected) {
      selectedCols = selected;
    }

    public XMOMOperationDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMOperationDatabaseEntry entry = new XMOMOperationDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(entry, selectedCols, rs);
      if (selectedCols.contains(XMOMDatabaseEntryColumn.NEEDS)) {
        entry.needs = rs.getString(COL_NEEDS);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.PRODUCES)) {
        entry.produces = rs.getString(COL_PRODUCES);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.EXCEPTIONS)) {
        entry.exceptions = rs.getString(COL_EXCEPTIONS);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.GROUPEDBY)) {
        entry.groupedBy = rs.getString(COL_GROUPEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.CALLEDBY)) {
        entry.calledBy = rs.getString(COL_CALLEDBY);
      }     
      if (selectedCols.contains(XMOMDatabaseEntryColumn.USESINSTANCESOF)) {
        entry.usesInstancesOf = rs.getString(COL_USESINSTANCESOF);
      }
      
      return entry;
    }
  }
  
  @Override
  public XMOMOperationDatabaseEntry clone() throws CloneNotSupportedException {
    XMOMOperationDatabaseEntry clone = new XMOMOperationDatabaseEntry();
    clone.setAllFieldsFromData(this);
    return clone;
  }


  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.OPERATION;
  }
  
  
  protected String retrieveBaseInstantiations(String originalOperationFqName) {
    String originalDomName = getDomOriginalFQNameFromOperationFqName(originalOperationFqName);
    Set<String> instatiationSet = new HashSet<String>();
    try {
      String xmlfilename = GenerationBase.getFileLocationOfXmlNameForSaving(originalDomName);
      Document d = XMLUtils.parse(xmlfilename + ".xml", true);
      List<Element> additionalDependencyContainers = XMLUtils.getChildElementsRecursively(d.getDocumentElement(), EL.ADDITIONALDEPENDENCIES); //there should be only one, but we won't complain
      for (Element element : additionalDependencyContainers) {
        List<Element> relevantTypes = XMLUtils.getChildElementsRecursively(element, EL.DEPENDENCY_DATATYPE);
        relevantTypes.addAll(XMLUtils.getChildElementsRecursively(element, EL.DEPENDENCY_EXCEPTION));
        for (Element typeElement : relevantTypes) {
          instatiationSet.add(typeElement.getTextContent());
        }
      }
    } catch (Throwable t) {
      logger.debug("Error while retrieving baseInstantiations",t);
    }
    
    
    Set<XMOMDatabaseEntryColumn> relationsToAppendAsUsedInstances = new HashSet<XMOMDatabaseEntryColumn>();
    relationsToAppendAsUsedInstances.add(XMOMDatabaseEntryColumn.NEEDS);
    relationsToAppendAsUsedInstances.add(XMOMDatabaseEntryColumn.PRODUCES);
    relationsToAppendAsUsedInstances.add(XMOMDatabaseEntryColumn.EXCEPTIONS);
    for (XMOMDatabaseEntryColumn relationToAppendAsUsedInstances : relationsToAppendAsUsedInstances) {
      String otherRelation = getValueByColumn(relationToAppendAsUsedInstances);
      if (otherRelation != null &&
          !otherRelation.equals("") &&
          !otherRelation.equals("null")) {
        for (String entry : otherRelation.split(SEPERATION_MARKER)) {
          instatiationSet.add(entry);
        }
      }
    }
    
    
    StringBuilder instantiationBuilder = new StringBuilder();
    Iterator<String> instantiationIter = instatiationSet.iterator();
    while (instantiationIter.hasNext()) {
      String instantiation = instantiationIter.next();
      if (instantiation != null &&
          !instantiation.equals("") &&
          !instantiation.equals("null")) {
        instantiationBuilder.append(instantiation);
        if (instantiationIter.hasNext()) {
          instantiationBuilder.append(SEPERATION_MARKER);
        }
      }
    }
    
    
    return instantiationBuilder.toString();
  }
 

}

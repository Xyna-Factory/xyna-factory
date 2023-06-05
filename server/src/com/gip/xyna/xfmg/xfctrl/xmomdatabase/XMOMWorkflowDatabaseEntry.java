/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.Department;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



@Persistable(primaryKey = XMOMWorkflowDatabaseEntry.COL_ID, tableName = XMOMWorkflowDatabaseEntry.TABLENAME)
public class XMOMWorkflowDatabaseEntry extends XMOMServiceDatabaseEntry {

  public static final String TABLENAME = "xmomworkflowcache";
  public static final String COL_CALLS = "calls";
  public static final String COL_USESDATAMODELS = "usesDataModels";
  public static final String COL_INSTANCESERVICEREFERENCEOF = "instanceServiceReferenceOf";
  public static final String COL_IMPLUSES = "implUses";

  

  private static final long serialVersionUID = -7301378884111678454L;
  
  
  @Column(name = COL_CALLS, size = 1000)
  private String calls;

  @Column(name = COL_USESDATAMODELS, size = 2000)
  private String usesDataModels; //Datenmodelle, die von dem Workflow verwendet werden

  @Column(name = COL_INSTANCESERVICEREFERENCEOF, size = 250)
  private String instanceServiceReferenceOf; //Instanzmethoden, die diesen Workflow verwenden
  
  @Column(name = COL_IMPLUSES, size = 1000)
  protected String implUses; //Datentypen und Exceptions, die im Workflow verwendet werden
  
  public XMOMWorkflowDatabaseEntry() {
  }

  
  public XMOMWorkflowDatabaseEntry(String fqname, Long revision) {
    super(fqname, revision);
  }
  
  
  public XMOMWorkflowDatabaseEntry(WF wf) {
    super(wf.getOriginalFqName(), wf.getLabel(), wf.getOriginalPath(), wf.getOriginalSimpleName(), "", wf.getXmlRootTagMetdata(), "", 
          wf.getInputVars(), wf.getOutputVars(), wf.getAllThrownExceptions(), wf.isXynaFactoryComponent(), wf.getRevision()); 
    calls = retrieveCalls(wf.getWfAsStep());
    usesDataModels = retrieveDataModels(wf.getOriginalFqName(), wf.getRevision());
    implUses = retrieveImplUses(wf);
    setTimestamp(wf.getParsingTimestamp());
  }
  
  
  public String getCalls() {
    return calls;
  }

  
  public void setCalls(String calls) {
    this.calls = calls;
  }

  
  public String getUsesDataModels() {
    return usesDataModels;
  }

  
  public void setUsesDataModels(String usesDataModels) {
    this.usesDataModels = usesDataModels;
  }
  
  
  public String getInstanceServiceReferenceOf() {
    return instanceServiceReferenceOf;
  }

  
  public void setInstanceServiceReferenceOf(String instanceServiceReferenceOf) {
    this.instanceServiceReferenceOf = instanceServiceReferenceOf;
  }
  
  
  public String getImplUses() {
    return implUses;
  }
  
  public void setImplUses(String implUses) {
    this.implUses = implUses;
  }
  
  private static ResultSetReader<XMOMWorkflowDatabaseEntry> reader = new ResultSetReader<XMOMWorkflowDatabaseEntry>() {

    public XMOMWorkflowDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMWorkflowDatabaseEntry x = new XMOMWorkflowDatabaseEntry();
      XMOMDatabaseEntry.readFromResultSetReader(x, rs);
      x.needs = rs.getString(COL_NEEDS);
      x.calls = rs.getString(COL_CALLS);
      x.produces = rs.getString(COL_PRODUCES);
      x.calledBy = rs.getString(COL_CALLEDBY);
      x.groupedBy = rs.getString(COL_GROUPEDBY);
      x.exceptions = rs.getString(COL_EXCEPTIONS);
      x.usesInstancesOf = rs.getString(COL_USESINSTANCESOF);
      x.usesDataModels = rs.getString(COL_USESDATAMODELS);
      x.instanceServiceReferenceOf = rs.getString(COL_INSTANCESERVICEREFERENCEOF);
      x.implUses = rs.getString(COL_IMPLUSES);
      return x;
    }

  };
  
  
  @Override
  public ResultSetReader<? extends XMOMWorkflowDatabaseEntry> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return getId();
  }


  @Override
  public <U extends XMOMDatabaseEntry> void setAllFieldsFromData(U data) {
    super.setAllFieldsFromData(data);
    calls = ((XMOMWorkflowDatabaseEntry)data).calls;
    usesDataModels = ((XMOMWorkflowDatabaseEntry)data).usesDataModels;
    instanceServiceReferenceOf = ((XMOMWorkflowDatabaseEntry)data).instanceServiceReferenceOf;
    implUses = ((XMOMWorkflowDatabaseEntry)data).implUses;
  }
  
  
  public static class DynamicXMOMCacheReader implements ResultSetReader<XMOMWorkflowDatabaseEntry> {

    private Set<XMOMDatabaseEntryColumn> selectedCols;

    public DynamicXMOMCacheReader(Set<XMOMDatabaseEntryColumn> selected) {
      selectedCols = selected;
    }

    public XMOMWorkflowDatabaseEntry read(ResultSet rs) throws SQLException {
      XMOMWorkflowDatabaseEntry entry = new XMOMWorkflowDatabaseEntry();
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
      if (selectedCols.contains(XMOMDatabaseEntryColumn.CALLS)) {
        entry.calls = rs.getString(COL_CALLS);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.CALLEDBY)) {
        entry.calledBy = rs.getString(COL_CALLEDBY);
      }
      if (selectedCols.contains(XMOMDatabaseEntryColumn.GROUPEDBY)) {
        entry.groupedBy = rs.getString(COL_GROUPEDBY);
      }      
      if (selectedCols.contains(XMOMDatabaseEntryColumn.USESINSTANCESOF)) {
        entry.usesInstancesOf = rs.getString(COL_USESINSTANCESOF);
      }
      
      return entry;
    }
  }
  
  
  private String retrieveCalls(Step rootStep) {
    if (rootStep == null) {
      return "";
    }
    Set<Step> allChildSteps = new HashSet<Step>();
    WF.addChildStepsRecursively(allChildSteps, rootStep);
    Set<String> functionSet = new HashSet<String>(); 
    if (allChildSteps.size() > 0) {
      for (Step step : allChildSteps) {
        if (step instanceof StepFunction) {
          ScopeStep scope = step.getParentScope();
          try {
            Service service = scope.identifyService(((StepFunction) step).getServiceId()).service;
            if (service.isDOMRef()) {
                functionSet.add(XMOMDatabaseEntry.generateFqNameForOperation(service.getDom(), service.getServiceName(),
                                                                             ((StepFunction) step).getOperationName()));
            } else {
              if (!service.isPrototype()) { //we don't have unique names for those
                functionSet.add(service.getWF().getOriginalFqName());
              }
            }
          } catch (XPRC_InvalidServiceIdException e) {
            logger.warn("The call with id " + ((StepFunction) step).getServiceId() + " of workflow " + getFqname() + " could not be identified!");
          }
        }
      }
    } else {
      return "";
    }
    
    if (functionSet.size() > 0) {
      StringBuilder callsBuilder = new StringBuilder();
      Iterator<String> stringIter = functionSet.iterator();
      while (stringIter.hasNext()) {
        callsBuilder.append(stringIter.next());
        if (stringIter.hasNext()) {
          callsBuilder.append(SEPERATION_MARKER);
        }
      }
      return callsBuilder.toString();
    } else {
      return "";
    }
   
  }
  
  
  protected String retrieveBaseInstantiations(String originalFqName, Long revision) {
    Set<String> instatiationSet = new HashSet<String>();
    StringBuilder instantiationBuilder;
    try {
      String xmlfilename = GenerationBase.getFileLocationOfXmlName(originalFqName, revision);
      Document d = XMLUtils.parse(xmlfilename + ".xml", true);
      List<Element> relevantElements = XMLUtils.getChildElementsRecursively(d.getDocumentElement(), EL.DATA);
      relevantElements.addAll(XMLUtils.getChildElementsRecursively(d.getDocumentElement(), EL.EXCEPTION));
      for (Element element : relevantElements) {
        if (!XMLUtils.isTrue(element, ATT.ABSTRACT)) {
          instantiationBuilder = new StringBuilder();
          instantiationBuilder.append(element.getAttribute(ATT.REFERENCEPATH));
          instantiationBuilder.append(".");
          instantiationBuilder.append(element.getAttribute(ATT.REFERENCENAME));
          String instantiationString = instantiationBuilder.toString();
          if (instantiationString != null &&
              !instantiationString.equals("") &&
              !instantiationString.equals(".") &&
              !instantiationString.equals("null")) {
            instatiationSet.add(instantiationString);
          }
        }
      }
      relevantElements = XMLUtils.getChildElementsRecursively(d.getDocumentElement(), EL.CHOICE);
      for (Element element : relevantElements) {
        String fqChoiceClassName =
            GenerationBase.transformNameForJava(element.getAttribute(GenerationBase.ATT.TYPEPATH),
                                                element.getAttribute(GenerationBase.ATT.TYPENAME));
        if (fqChoiceClassName.equals(StepChoice.BASECHOICE_SUBCLASSES)) {
          List<Element> caseEls = XMLUtils.getChildElementsByName(element, GenerationBase.EL.CASE);
          for (Element caseEl : caseEls) {
            instatiationSet.add(caseEl.getAttribute(ATT.CASECOMPLEXNAME));
          }
        }
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.debug("Error while retrieving baseInstantiations",t);
    }
    
    instantiationBuilder = new StringBuilder();
    Iterator<String> instantiationIter = instatiationSet.iterator();
    while (instantiationIter.hasNext()) {
      String instantiation = instantiationIter.next();
      if (instantiation != null &&
          !instantiation.equals("") &&
          !instantiation.equals("null")) {
        //TODO we'd love to restrict ourself to non FactoryComponents if possible at this place
        instantiationBuilder.append(instantiation);
        if (instantiationIter.hasNext()) {
          instantiationBuilder.append(SEPERATION_MARKER);
        }
      }
    }
    return instantiationBuilder.toString();
  }

  private String retrieveDataModels(String originalFqName, Long revision) {
    Set<String> dataModelSet = new HashSet<String>();
    try {
      String xmlfilename = GenerationBase.getFileLocationOfXmlName(originalFqName, revision);
      Document d = XMLUtils.parse(xmlfilename + ".xml", true);
      List<Element> relevantElements = XMLUtils.getChildElementsRecursively(d.getDocumentElement(), EL.DATAMODEL);
      for (Element element : relevantElements) {
        Element dataModelName = XMLUtils.getChildElementByName(element, GenerationBase.EL.MODELNAME);
        dataModelSet.add(dataModelName.getTextContent());
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.debug("Error while retrieving data models",t);
    }
    
    StringBuilder sb = new StringBuilder();
    Iterator<String> it = dataModelSet.iterator();
    while (it.hasNext()) {
      String dataModel = it.next();
      if (dataModel != null &&
                      !dataModel.equals("") &&
                      !dataModel.equals("null")) {
        sb.append(dataModel);
        if (it.hasNext()) {
          sb.append(SEPERATION_MARKER);
        }
      }
    }
    return sb.toString();
  }
  
  private String retrieveImplUses(WF wf) {
    Set<GenerationBase> dependenObjects = wf.getDirectlyDependentObjects();
    
    StringBuilder sb = new StringBuilder();
    Iterator<GenerationBase> it = dependenObjects.iterator();
    while (it.hasNext()) {
      GenerationBase gb = it.next();
      if (gb instanceof DomOrExceptionGenerationBase) {
        sb.append(gb.getOriginalFqName());
        if (it.hasNext()) {
          sb.append(SEPERATION_MARKER);
        }
      }
    }

    return sb.toString();
  }
  
  
  public String getValueByColumn(XMOMDatabaseEntryColumn column) {
    switch (column) {
      case CALLS :
        return calls;
      default :
        return super.getValueByColumn(column);
    }
  }

  
  @Override
  public XMOMDatabaseType getXMOMDatabaseType() {
    return XMOMDatabaseType.WORKFLOW;
  }
}

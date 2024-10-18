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
package com.gip.xyna.xact.filter.session.save;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.xact.filter.json.PersistJson;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XmomGuiSession;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter.RefactoringTargetRootType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringMoveActionParameter;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.EscapableXMLEntity;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.ExceptionType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Utils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Workflow;
import com.gip.xyna.xprc.xfractwfe.generation.xml.WorkflowOperation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;



public class Persistence {
  
  private GenerationBaseObject gbo;
  private Long revision;
  private boolean force;
  private XmomType persistType;
  private XmomGuiSession session;
  private String operationName = null;
  private final XynaMultiChannelPortal multiChannelPortal;


  public Persistence(GenerationBaseObject gbo, Long revision, PersistJson persistRequest, XmomGuiSession session) {
    this(gbo, revision, persistRequest, session, null);
  }

  public Persistence(GenerationBaseObject gbo, Long revision, PersistJson persistRequest, XmomGuiSession session, String operationName) {
    multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
    this.gbo = gbo;
    this.revision = revision;
    this.force = persistRequest.isForce();
    this.session = session;
    this.operationName = operationName;

    if (persistRequest.getLabel() != null && persistRequest.getPath() != null) {
      // path and label has been given in request
      if (!gbo.getSaveState()) {
        // document has never been saved -> use path and label from request to generate java name for saving
        persistType = new XmomType(persistRequest.getPath(), Utils.labelToJavaName(persistRequest.getLabel(), true), persistRequest.getLabel(), gbo.getGenerationBase().isAbstract());
      } else if (persistRequest.getLabel().equals(gbo.getGenerationBase().getLabel()) && persistRequest.getPath().equals(gbo.getGenerationBase().getOriginalPath())) {
        // document has been saved with identical path and label, before -> no changes
        persistType = Utils.getXmomType(gbo.getGenerationBase());
        this.force = true;
      } else {
        // a new path and/or label has been given for an already saved document -> store copy of the document under new name
        persistType = new XmomType(persistRequest.getPath(), Utils.labelToJavaName(persistRequest.getLabel(), true), persistRequest.getLabel(), gbo.getGenerationBase().isAbstract());
      }
    } else {
      // no path and label has been given in request -> use old one
      persistType = Utils.getXmomType(gbo.getGenerationBase());
      this.force = true;
    }
  }
  
  public String getSaveFqn() {
    return persistType.getFQTypeName();
  }

  public String createXML() throws XynaException {
    switch (gbo.getType()) {
      case DATATYPE :
        return createDataTypeXML();
      case EXCEPTION :
        return createExceptionTypeXML();
      case FORM :
        break;
      case ORDERINPUTSOURCE :
        break;
      case WORKFLOW :
        return createWorkflowXML();
      default :
        break;
    }
    throw new UnsupportedOperationException("Generation of " + gbo.getType() + "-XML is not implemented yet.");
  }

  public String save() throws XynaException {
    XMOMType savedType = getSavedXmomType(persistType.getFQTypeName(), gbo.getFQName().getRevision());
    if(force && savedType != null && gbo.getType() != savedType) {
      multiChannelPortal.deleteXMOMObject(savedType, persistType.getFQTypeName(), true, false, session.getUser(), session.getId(), revision);
    }
    switch (gbo.getType()) {
      case DATATYPE :
        return saveDataType();
      case EXCEPTION :
        return saveExceptionType();
      case FORM :
        break;
      case ORDERINPUTSOURCE :
        break;
      case WORKFLOW :
        return saveWorkflow();
      default :
        break;
    }

    throw new UnsupportedOperationException("Saving of " + gbo.getType() + " is not implemented yet.");
  }


  public String refactor() throws XynaException {
    String oldFqn = null;
    String newFqn = persistType.getFQTypeName();

    switch(gbo.getType()) {
      case WORKFLOW:
        oldFqn = gbo.getWorkflow().getOriginalFqName();
        break;
      case DATATYPE:
        if (operationName == null) {
          oldFqn = gbo.getDOM().getOriginalFqName();
        } else {
          String fqnPrefix = gbo.getDOM().getOriginalFqName() + "." + gbo.getDOM().getServiceForOperation(operationName);
          oldFqn = fqnPrefix + "." + operationName;

          OperationInformation[] operationInformations = gbo.getDOM().collectOperationsOfDOMHierarchy(true);
          List<String> usedNames = new ArrayList<>();
          for (OperationInformation curOpInfo : operationInformations) {
            usedNames.add(curOpInfo.getOperation().getName());
          }
          String newOperationName = Utils.createUniqueJavaName(usedNames, persistType.getLabel(), false);
          newFqn = fqnPrefix + "." + newOperationName;
        }
        break;
      case EXCEPTION:
        oldFqn = gbo.getExceptionGeneration().getOriginalFqName();
        break;
      default:
        throw new UnsupportedOperationException("Refactoring of " + gbo.getType() + " is not implemented yet.");
    }
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RefactoringMoveActionParameter action = new RefactoringMoveActionParameter();
    action.setFqXmlNameOld(oldFqn);
    action.setFqXmlNameNew(newFqn);
    action.setTargetLabel(persistType.getLabel());
    action.setRuntimeContext(rm.getRuntimeContext(revision));
    action.setSessionId(session.getId());
    action.setIgnoreIncompatibleStorables(force);

    if (operationName == null) {
      action.setTargetRootType(RefactoringTargetRootType.fromXMOMType(gbo.getType()));
    } else {
      action.setTargetRootType(RefactoringTargetRootType.OPERATION);
    }

    multiChannelPortal.refactorXMOM(action);

    return getXml();
  }


  public void delete() throws XynaException {
    multiChannelPortal.deleteXMOMObject(gbo.getType(), gbo.getFQName().getFqName(), force, false, session.getUser(), session.getId(), revision);
  }

  public String deploy() throws XynaException {
    XMOMType deployedType = getDeployedXmomType(persistType.getFQTypeName(), gbo.getFQName().getRevision());
    if(deployedType != null && gbo.getType() != deployedType) {
      undeploy(deployedType, new FQName(gbo.getFQName().getRevision(), persistType.getFQTypeName()), true);
    }
    switch (gbo.getType()) {
      case WORKFLOW :
        return deployWorkflow();
      case DATATYPE:
        return deployDataType();
      case EXCEPTION:
        return deployExceptionType();
      default:
        break;
    }

    throw new UnsupportedOperationException("Deployment of " + gbo.getType() + " is not implemented yet.");
  }
  
  private void undeploy(XMOMType type, FQName fqName, boolean force) throws XynaException {
    XynaMultiChannelPortalSecurityLayer xmcpsl = (XynaMultiChannelPortalSecurityLayer)XynaFactory.getInstance().getXynaMultiChannelPortal();
    xmcpsl.undeployXMOMObject(fqName.getFqName(), type, DependentObjectMode.PROTECT, force, fqName.getRevision());
  }
  
  private XMOMType getDeployedXmomType(String fqn, Long revision) throws XynaException {
    return getXmomTypeFromXmlFile(GenerationBase.getFileLocationForDeploymentStaticHelper(fqn, revision) + ".xml");
  }
  
  private XMOMType getSavedXmomType(String fqn, Long revision) throws XynaException {
    return getXmomTypeFromXmlFile(GenerationBase.getFileLocationForSavingStaticHelper(fqn, revision) + ".xml");
  }
  
  private XMOMType getXmomTypeFromXmlFile(String filename) throws XynaException {
    File file = new File(filename);
    if(!file.exists()) {
      return null;
    }
    try (FileInputStream fis =
        new FileInputStream(new File(filename))) {
      String rootElementName = XMLUtils.getRootElementName(fis);
      return XMOMType.getXMOMTypeByRootTag(rootElementName);
    } catch (FileNotFoundException e) {
      throw new Ex_FileAccessException(filename, e);
    } catch (IOException | XMLStreamException e) {
      throw new XPRC_XmlParsingException(filename, e);
    }
  }
  
  

  private String createDataTypeXML() throws XynaException {
    return createDatatypeXML(gbo.getDOM(), persistType);
  }
  
  public static String createDatatypeXML(DOM dom, XmomType persistType) {
    List<Variable> variables = new ArrayList<Variable>();
    for (AVariable var : dom.getMemberVars()) {
      variables.add(Utils.createVariable(var));
    }

    List<com.gip.xyna.xprc.xfractwfe.generation.xml.Operation> operations = new ArrayList<>();
    for (Operation operation : (dom).getOperations()) {
      operations.add(Utils.createOperation(operation, true));
    }

    Datatype dt = Datatype.create(persistType).
             basetype(Utils.getBaseType(dom)).
             meta(Utils.createMeta(dom)).
             variables(variables).
             operations(operations).
             sharedLibs(dom.getSharedLibs()).
             additionalLibNames(dom.getAdditionalLibraries()).
             pythonLibNames(dom.getPythonLibraries()).
             additionalDependencies(dom.getAdditionalDependencies()).
             build();

    return dt.toXML();
  }

  private String createExceptionTypeXML() throws XynaException {
    return createExceptionTypeXML(gbo.getExceptionGeneration(), persistType);
  }
  
  public static String createExceptionTypeXML(ExceptionGeneration exception, XmomType persistType) {
    List<Variable> variables = new ArrayList<Variable>();
    for (AVariable var : exception.getMemberVars()) {
      variables.add( Utils.createVariable( var ) );
    }

    Map<String, String> messagesMap = exception.getExceptionEntry().getMessages();
    List<Pair<String, String>> messageTexts = new ArrayList<Pair<String, String>>();
    for (String language : exception.getExceptionEntry().getMessages().keySet()) {
      messageTexts.add(new Pair<String, String>(language, messagesMap.get(language)));
    }

    ExceptionType dt = ExceptionType.create(persistType).
        basetype(Utils.getBaseType(exception)).
        meta(Utils.createMeta(exception)).
        variables(variables).
        code(exception.getExceptionEntry().getCode()).
        messageTexts(messageTexts).
        build();

    return dt.toXML();
  }

  private String saveDataType() throws XynaException {
    // mark operations as persistent to make sure their label can't be changed, anymore
    DOM dataType = gbo.getDOM();
    for (Operation operation : (dataType).getOperations()) {
      operation.setHasBeenPersisted(true);
    }

    SingleRepositoryEvent repositoryEvent = new SingleRepositoryEvent(revision);
    String xml = createDataTypeXML();
    multiChannelPortal.saveMDM(xml, force, session.getUser(), session.getId(), revision, repositoryEvent);

    return xml;
  }

  private String saveExceptionType() throws XynaException {
    SingleRepositoryEvent repositoryEvent = new SingleRepositoryEvent(revision);
    String xml = createExceptionTypeXML();
    multiChannelPortal.saveMDM(xml, force, session.getUser(), session.getId(), revision, repositoryEvent);

    return xml;
  }


  private String createWorkflowXML() {
    WF wf = gbo.getWorkflow();
    String xml = createWorkflowXML(wf, persistType);
    return xml;
  }


  public static String createWorkflowXML(WF wf, XmomType persistType) {
    queryEscapeFix(wf, true); // TODO entfernen mit PMOD-598
    WorkflowOperation workflowOperation = new WorkflowOperation(wf, persistType.getName(), persistType.getLabel());
    Workflow workflow = new Workflow(persistType, workflowOperation);
    wf.clearUnusedVariables();

    String xml = workflow.toXML();
    queryEscapeFix(wf, false); // TODO entfernen mit PMOD-598
    return xml;
  }
  
  /**
   * Escaped oder Unescaped alle FilterConditions aller Querys im Workflow.
   * Dies ist ein Workaround bis das Escapen in der Methode com.gip.xyna.xprc.xfractwfe.generation.StepFunction.appendXML(XmlBuilder).queryFilterCondition gemacht wird.
   * TODO PMOD-598 Methode kann gelöscht werden
   * @param wf
   * @param escape
   */
  private static void queryEscapeFix(WF wf, boolean escape) {
    try {
      Set<Step> allSteps = new HashSet<>();
      WF.addChildStepsRecursively(allSteps, wf.getWfAsStep());
      for (Step step : allSteps) {
        if(step instanceof StepFunction) {
          StepFunction stepFunction = (StepFunction)step;
          List<String> conditions = stepFunction.getQueryFilterConditions();
          if(stepFunction.getQueryFilterConditions() != null) {
            List<String> escapedConditions = new ArrayList<>(conditions.size());
            conditions.forEach(c -> { 
              if(escape) {
                escapedConditions.add(XMLUtils.escapeXMLValueAndInvalidChars(c, false, false));
              } else {
                for (EscapableXMLEntity entity : EscapableXMLEntity.values()) {
                  c = c.replace(entity.getFullEscapedRepresentation(), entity.getUnescapedRepresentation());
                }
                escapedConditions.add(c);
              }
            });
            stepFunction.setQueryFilterConditions(escapedConditions);
          }
        }
      }
    } catch (Exception ex) {
      // nothing
    }
  }

  private String saveWorkflow() throws XynaException {
    String xml = createWorkflowXML();
    SingleRepositoryEvent repositoryEvent = new SingleRepositoryEvent(revision);
    multiChannelPortal.saveMDM(xml, force, session.getUser(), session.getId(), revision, repositoryEvent);
    return xml;
  }


  private String deployWorkflow() throws XynaException {
    WF wf = gbo.getWorkflow();
    XynaMultiChannelPortalSecurityLayer xmcpsl = (XynaMultiChannelPortalSecurityLayer)XynaFactory.getInstance().getXynaMultiChannelPortal();
    xmcpsl.deployWF(wf.getOriginalFqName(), WorkflowProtectionMode.BREAK_ON_USAGE, revision);

    return getXml();
  }


  private String deployDataType() throws XynaException {
    if (gbo.getSgLibsToDelete() != null) {
      for (String fileName : gbo.getSgLibsToDelete()) {
        removeLibFromSgFolder(fileName);
      }

      gbo.setSgLibsToDelete(null);
    }

    if (gbo.getSgLibsToUpload() != null) {
      for (String fileId : gbo.getSgLibsToUpload()) {
        copyLibToSgFolder(fileId);
      }

      gbo.setSgLibsToUpload(null);
    }

    DOM dom = gbo.getDOM();
    XynaMultiChannelPortalSecurityLayer xmcpsl = (XynaMultiChannelPortalSecurityLayer)XynaFactory.getInstance().getXynaMultiChannelPortal();
    xmcpsl.deployDatatype(dom.getOriginalFqName(), WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, new HashMap<String, InputStream>(), revision);

    return getXml();
  }


  private void copyLibToSgFolder(String fileId) {
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    TransientFile tFile = fm.retrieve(fileId);
    InputStream is = tFile.openInputStream();

    try {
      String destinationPath = GenerationBase.getFileLocationOfServiceLibsForSaving(gbo.getDOM().getFqClassName(), gbo.getDOM().getRevision());
      new File(destinationPath).mkdirs();
      FileOutputStream fos = new FileOutputStream(destinationPath + "/" + tFile.getOriginalFilename());
      try {
        StreamUtils.copy(is, fos);
      } finally {
        fos.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void removeLibFromSgFolder(String fileName) {
    String savePath = GenerationBase.getFileLocationOfServiceLibsForSaving(gbo.getDOM().getFqClassName(), gbo.getDOM().getRevision());
    new File(savePath + "/" + fileName).delete();

    String deployPath = GenerationBase.getFileLocationOfServiceLibsForDeployment(gbo.getDOM().getFqClassName(), gbo.getDOM().getRevision());
    new File(deployPath + "/" + fileName).delete();
  }


  private String deployExceptionType() throws XynaException {
    ExceptionGeneration exception = gbo.getExceptionGeneration();
    XynaMultiChannelPortalSecurityLayer xmcpsl = (XynaMultiChannelPortalSecurityLayer)XynaFactory.getInstance().getXynaMultiChannelPortal();
    xmcpsl.deployException(exception.getOriginalFqName(), WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, revision);

    return getXml();
  }


  private String getXml() throws XynaException {
    switch (gbo.getType()) {
      case DATATYPE :
        return createDataTypeXML();
      case EXCEPTION :
        break;
      case FORM :
        break;
      case ORDERINPUTSOURCE :
        break;
      case WORKFLOW :
        return createWorkflowXML();
      default :
        break;
    }

    return null;
  }

}

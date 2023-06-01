/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Callservice;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationNotFoundInDatatypeException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.CallServiceHelper;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.ServiceDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;



public class CallserviceImpl extends XynaCommandImplementation<Callservice> {

  private static final Logger logger = CentralFactoryLogging.getLogger(CallserviceImpl.class);
  

  public void execute(OutputStream statusOutputStream, Callservice payload) throws XynaException {
    
    ExtendedPayload extendedPayload = new ExtendedPayload(payload);
    
    if(!RevisionManagement.REVISION_DEFAULT_WORKSPACE.equals(extendedPayload.getRevision())) {
      //über ServiceDestination
      ServiceDestinationHelper sdh = new ServiceDestinationHelper(extendedPayload);
      DestinationKey destinationKey = sdh.prepareDestinationKey();
      CommandControl.Operation[] cmds =
          new CommandControl.Operation[] {CommandControl.Operation.DESTINATION_SET, CommandControl.Operation.DESTINATION_REMOVE};
      CommandControl.tryLock(cmds, extendedPayload.getRevision());
      try {
      sdh.setDestination(destinationKey, sdh.prepareServiceDestination() );
      try {
        runOrdertype(destinationKey, statusOutputStream, extendedPayload);
      } finally {
        sdh.removeDestination(destinationKey);
      }
      } finally {
        CommandControl.unlock(cmds, extendedPayload.getRevision());
      }
    } else {
      //über Bau eines Workflows
      BuildWorkflowHelper bwh = new BuildWorkflowHelper(extendedPayload);
      bwh.createWorkflow();
      CommandControl.Operation[] ops =
          new CommandControl.Operation[] {CommandControl.Operation.XMOM_SAVE, CommandControl.Operation.XMOM_WORKFLOW_DEPLOY,
              CommandControl.Operation.XMOM_WORKFLOW_UNDEPLOY, CommandControl.Operation.XMOM_WORKFLOW_DELETE};
      CommandControl.tryLock(ops, extendedPayload.getRevision());
      try {
        bwh.saveWorkflow();
        try {
          bwh.deployWorkflow();
          try {
            runOrdertype(bwh.getDestinationKey(), statusOutputStream, extendedPayload);
          } finally {
            bwh.undeployWorkflow();
          }
        } finally {
          bwh.deleteWorkflow();
        }
      } finally {
        CommandControl.unlock(ops, extendedPayload.getRevision());
      }
    }
  }
  
  private static class ExtendedPayload {

    private Callservice payload;
    private long revision;
    private Element root;
    private String fqType;
    private String typeName;
    private String typePath;
    private String service;
    private String operation;
    
    public ExtendedPayload(Callservice payload) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException {
      this.payload = payload;
      String inputFileLocationString = payload.getPathToParameterFile();
      File fileLocation = new File(inputFileLocationString);
      if (!fileLocation.exists()) {
        throw new IllegalArgumentException("Specified input file does not exist");
      }

      revision =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(payload.getApplicationName(), payload.getVersionName(), null);

      Document doc = XMLUtils.parse(inputFileLocationString);

      root = doc.getDocumentElement();
      fqType = root.getAttribute("Type");
      typeName = fqType.substring(fqType.lastIndexOf(".") + 1);
      typePath = fqType.substring(0, fqType.lastIndexOf("."));
      service = root.getAttribute(GenerationBase.ATT.SERVICE);
      operation = root.getAttribute(GenerationBase.ATT.INVOKE_OPERATION);

      if (!root.getTagName().equals(GenerationBase.EL.INVOKE)) {
        throw new IllegalArgumentException("Provided XML must start with a " + GenerationBase.EL.INVOKE + " element");
      }     
    }

    public long getRevision() {
      return revision;
    }
    
    public Element getRoot() {
      return root;
    }

    public String getService() {
      return service;
    }

    public String getOperation() {
      return operation;
    }
    
    public String getFqType() {
      return fqType;
    }

    public String getType() {
      return typePath +"."+typeName;
    }

    public String getApplicationName() {
      return payload.getApplicationName();
    }

    public String getVersionName() {
      return payload.getVersionName();
    }

    public String getTypePath() {
      return typePath;
    }

    public String getTypeName() {
      return typeName;
    }

  }
  
  private static class ServiceDestinationHelper {
    XynaProcessCtrlExecution xpce;
    private ExtendedPayload extendedPayload;
        
    public ServiceDestinationHelper(ExtendedPayload extendedPayload) {
      this.extendedPayload = extendedPayload;
      this.xpce = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
    }

    public ServiceDestination prepareServiceDestination() throws XPRC_InvalidPackageNameException, XFMG_MDMObjectClassLoaderNotFoundException, XPRC_OperationNotFoundInDatatypeException {
      String fqClassName = GenerationBase.transformNameForJava(extendedPayload.getFqType());
      ServiceDestination serviceDestination =
          new ServiceDestination(extendedPayload.getFqType(), extendedPayload.getService(), extendedPayload.getOperation(), fqClassName);
      return serviceDestination;
    }
    
    public DestinationKey prepareDestinationKey() {
      String ordertype = "callservice_" + System.currentTimeMillis() + "_" + extendedPayload.getFqType() + "." + extendedPayload.getOperation();
      return new DestinationKey(ordertype, extendedPayload.getApplicationName(), extendedPayload.getVersionName());
    }
    
    public void setDestination(DestinationKey destinationKey, DestinationValue execution ) {
      setDestination(destinationKey, XynaDispatcher.DESTINATION_EMPTY_PLANNING, execution, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW);
    }
    public void setDestination(DestinationKey destinationKey, DestinationValue planning, DestinationValue execution, DestinationValue cleanup ) {
      xpce.getXynaPlanning().getPlanningDispatcher().setDestination(destinationKey, planning, true);
      xpce.getXynaExecution().getExecutionEngineDispatcher().setDestination(destinationKey, execution, true);
      xpce.getXynaCleanup().getCleanupEngineDispatcher().setDestination(destinationKey, cleanup, true);
    }
    public void removeDestination(DestinationKey destinationKey) {
      xpce.getXynaPlanning().getPlanningDispatcher().removeDestination(destinationKey);
      xpce.getXynaExecution().getExecutionEngineDispatcher().removeDestination(destinationKey);
      xpce.getXynaCleanup().getCleanupEngineDispatcher().removeDestination(destinationKey);
    }

    
  }
  

  private static class BuildWorkflowHelper {

    private ExtendedPayload extendedPayload;
    private String fqWFName;
    private Channel xmcp;
    private String serviceWithinWorkflow;
    private ArrayList<String> sourceDefinitionsList = new ArrayList<String>();
    private ArrayList<String> targetDefinitionsList = new ArrayList<String>();
    private ArrayList<String> dataObjectsList = new ArrayList<String>();
    private ArrayList<String> assignObjectsList = new ArrayList<String>();
    private ArrayList<String> wfInputObjects = new ArrayList<String>();
    private ArrayList<String> wfOutputObjects = new ArrayList<String>();
     
    public BuildWorkflowHelper(ExtendedPayload extendedPayload) {
      this.extendedPayload = extendedPayload;
      this.xmcp = XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal();   
    }

    public DestinationKey getDestinationKey() {
      return new DestinationKey(fqWFName);
    }
    
    public void createWorkflow() throws XynaException {
      String wfPath = "temp.callservice." + extendedPayload.getService();
      String wfName = extendedPayload.getOperation() + System.currentTimeMillis();
      this.fqWFName = wfPath + "." + wfName;

      if (logger.isDebugEnabled()) {
        logger.debug("creating workflow "+fqWFName +" ...");
      }
      
      serviceWithinWorkflow = createServiceWithinWorkflow(wfPath, wfName);
      
      if (logger.isTraceEnabled()) {
        logger.trace("generated wf = " + serviceWithinWorkflow);
      }      
    }
    
    private JavaOperation createCalledJavaOperation() throws XDEV_UNSUPPORTED_FEATURE, XPRC_OperationUnknownException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
      Operation calledOperation = null;

      DOM dom = DOM.getInstance(extendedPayload.getFqType(), extendedPayload.getRevision());
      dom.parse(true);
      List<Operation> operations = dom.getOperations();
      for (Operation o : operations) {
        if (o.getName().equals(extendedPayload.getOperation())) {
          calledOperation = o;
          break;
        }
      }

      if (calledOperation == null) {
        throw new XPRC_OperationUnknownException(extendedPayload.getOperation());
      }

      if (!(calledOperation instanceof JavaOperation)) {
        throw new XDEV_UNSUPPORTED_FEATURE("Call operations other than java operations");
      }

      JavaOperation calledJavaOperation = (JavaOperation) calledOperation;
      if (!calledJavaOperation.isStatic()) {
        throw new XDEV_UNSUPPORTED_FEATURE("Call non-static operations");
      }

      return calledJavaOperation;
    }

    private void createInputObjects(int nextInteger, int currentInputID, List<AVariable> inputVars) {
      for (AVariable v : inputVars) {
        String nextLine =
            "<Data ID=\"" + currentInputID + "\" Label=\"" + v.getOriginalName() + "\" ReferenceName=\""
                + v.getOriginalName() + "\" ReferencePath=\"" + v.getOriginalPath() + "\" VariableName=\""
                + v.getVarName() + "_callservice" + nextInteger + "\"" + (v.isList() ? " IsList=\"true\"" : "") + ">\n";
        nextLine += "<Target RefID=\"9\"/>\n</Data>\n";
        wfInputObjects.add(nextLine);

        sourceDefinitionsList.add("<Source RefID=\"" + currentInputID + "\"/>\n");

        currentInputID++;
        nextInteger++;
      }
    }
    
    private void createOutputObjects(int nextInteger, int currentWfOutputID, int currentServiceOutputID, List<AVariable> outputVars) {
      assignObjectsList.add("<Source RefID=\"" + currentServiceOutputID + "\"/>\n" + "<Target RefID=\""
            + currentWfOutputID + "\"/>\n");
      
      for (AVariable v : outputVars) {
        wfOutputObjects.add("<Data ID=\"" + currentWfOutputID + "\" Label=\"" + v.getOriginalName()
            + "\" ReferenceName=\"" + v.getOriginalName() + "\" ReferencePath=\"" + v.getOriginalPath()
            + "\" VariableName=\"" + v.getVarName() + "_callservice" + nextInteger + "\">\n" + "<Target RefID=\"16\"/>\n"
            + // 16 = Assign
            "</Data>\n");

        targetDefinitionsList.add("<Target RefID=\"" + currentServiceOutputID + "\"/>\n");

        dataObjectsList.add("<Data ID=\"" + currentServiceOutputID + "\" ReferenceName=\"" + v.getOriginalName()
            + "\" ReferencePath=\"" + v.getOriginalPath() + "\" VariableName=\"" + v.getVarName() + "\">\n"
            + "<Source RefID=\"9\"/>\n" + // 9 = Function
            "<Target RefID=\"16\"/>\n" + // 16 = Assign
            "</Data>\n");

        assignObjectsList.add("<Copy>\n" + "<Source RefID=\"" + currentServiceOutputID + "\"/>\n" + "<Target RefID=\""
            + currentWfOutputID + "\"/>\n" + "</Copy>\n");

        currentWfOutputID++;
        currentServiceOutputID++;
        nextInteger++;
      }
    }

    private String createServiceWithinWorkflow(String wfPath, String wfName) throws XynaException {
      JavaOperation calledJavaOperation = createCalledJavaOperation();
      List<AVariable> inputVars = calledJavaOperation.getInputVars();
      List<AVariable> outputVars = calledJavaOperation.getOutputVars();
      if( inputVars.size() > 0 ) {
        int nextInteger = 1;
        int currentInputID = 20;
        createInputObjects( nextInteger, currentInputID, calledJavaOperation.getInputVars() );
      }
      if( outputVars.size() > 0 ) {
        int nextInteger = 1+inputVars.size();
        int currentWfOutputID = 20+inputVars.size() + 5;
        int currentServiceOutputID = currentWfOutputID + outputVars.size() + 5;
        createOutputObjects( nextInteger, currentWfOutputID, currentServiceOutputID, calledJavaOperation.getOutputVars() ); 
      }
      
      String wf = CallServiceHelper.getServiceCallWithinWorkflowTemplate();
      wf = wf.replaceAll(CallServiceHelper.TOKEN_WF_TYPE_NAME, wfName);
      wf = wf.replaceAll(CallServiceHelper.TOKEN_WF_TYPE_PATH, wfPath);

      wf = wf.replaceAll(CallServiceHelper.TOKEN_SERVICE_TYPE_PATH, extendedPayload.getTypePath());
      wf = wf.replaceAll(CallServiceHelper.TOKEN_SERVICE_TYPE_NAME, extendedPayload.getTypeName() + "." + extendedPayload.getService());

      wf = replaceAll( wf, CallServiceHelper.TOKEN_WF_INPUT, wfInputObjects );
      wf = replaceAll( wf, CallServiceHelper.TOKEN_SOURCE_DEFS, sourceDefinitionsList );
      wf = replaceAll( wf, CallServiceHelper.TOKEN_WF_OUTPUT, wfOutputObjects );
      wf = replaceAll( wf, CallServiceHelper.TOKEN_TARGET_DEFS, targetDefinitionsList );
      wf = replaceAll( wf, CallServiceHelper.TOKEN_DATA_OBJECTS, dataObjectsList );
      wf = replaceAll( wf, CallServiceHelper.TOKEN_ASSIGN_OBJECTS, assignObjectsList );
      
      wf = wf.replaceAll(CallServiceHelper.TOKEN_OPERATION_NAME, extendedPayload.getOperation() );
      return wf;
    }

    private String replaceAll(String wf, String token, ArrayList<String> strings) {
      StringBuilder sb = new StringBuilder("");
      for (String str : strings) {
        sb.append(str).append("\n");
      }
      return  wf.replaceAll(token, sb.toString() );
    }

    public void saveWorkflow() throws XynaException {
      if (logger.isDebugEnabled()) {
        logger.debug("saving...");
      }
      xmcp.saveMDM(serviceWithinWorkflow);
    }
    
    public void deleteWorkflow() throws XynaException {
      if( logger.isDebugEnabled() ) {
        logger.debug("deleting...");
      }
      xmcp.unsecureDeleteSavedMDM(fqWFName);
    }

    public void deployWorkflow() throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
      if( logger.isDebugEnabled() ) {
        logger.debug("deploying...");
      }
      xmcp.deployWF(fqWFName, WorkflowProtectionMode.FORCE_DEPLOYMENT);
    }
    
    public void undeployWorkflow() throws XynaException {
      if( logger.isDebugEnabled() ) {
        logger.debug("undeploying...");
      }

      xmcp.undeployWF(fqWFName, true);
    }
        
  }
    
  private void runOrdertype(DestinationKey destinationKey, OutputStream statusOutputStream, ExtendedPayload extendedPayload) throws XynaException {
    
 // TODO Should this support GeneralXynaObject as payload?
    GeneralXynaObject payload = null;
    List<Element> payloadObjects = XMLUtils.getChildElements(extendedPayload.getRoot());
    if (payloadObjects.size() == 1) {
      payload = XynaObject.generalFromXml(XMLUtils.getXMLString(payloadObjects.get(0), false), extendedPayload.getRevision());
    } else if (payloadObjects.size() > 1) {
      GeneralXynaObject[] xynaObjectPayloadList = new GeneralXynaObject[payloadObjects.size()];
      for (int i = 0; i < payloadObjects.size(); i++) {
        xynaObjectPayloadList[i] = XynaObject.generalFromXml(XMLUtils.getXMLString(payloadObjects.get(i), false), extendedPayload.getRevision());
      }
      payload = new Container(xynaObjectPayloadList);
    }

    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(destinationKey, payload);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    xocp.setPriority(5);
    
    logger.debug("executing...");
    xocp.setInputPayload(payload);
    GeneralXynaObject output = factory.getXynaMultiChannelPortalPortal().startOrderSynchronously(xocp);

    writeLineToCommandLine(statusOutputStream, "Response:");
    StringBuilder responseString =
        new StringBuilder("<Result Type=\"").append(extendedPayload.getType()).append("\" Service=\"")
            .append(extendedPayload.getService()).append("\" Operation=\"").append(extendedPayload.getOperation()).append("\">\n");
    if (output != null) {
      responseString.append(output.toXml());
    }
    responseString.append("</Result>\n");
    writeToCommandLine(statusOutputStream, responseString.toString());
  }

}

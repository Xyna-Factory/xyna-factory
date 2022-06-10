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
package xmcp.dhcp.v4.netconfigurator.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployCPERequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployCPEResponse_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployCPERequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployCPEResponse_ctype;
import xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype;
import xmcp.dhcp.v4.netconfigurator.DhcpdConfServiceOperation;


public class DhcpdConfServiceOperationImpl implements ExtendedDeploymentTask, DhcpdConfServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  
  public Response_ctype activateForMigration(XynaOrderServerExtension correlatedXynaOrder, MigrationTargetIdentifier_ctype migrationTargetIdentifier_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype activateForMigrationRequest_ws = new com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype(migrationTargetIdentifier_ctype.getTargetType(), migrationTargetIdentifier_ctype.getUniqueIdentifier());


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().activateForMigration(activateForMigrationRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype activateForMigrationResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            activateForMigrationResponse_ws = payload_ws.getActivateForMigrationResponse();
        }
        catch(NullPointerException e) {
            activateForMigrationResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = activateForMigrationResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype activateForMigrationResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            activateForMigrationResponse.setOutputContent(activateForMigrationResponse_ws.getOutputContent());
            activateForMigrationResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setActivateForMigrationResponse(activateForMigrationResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype checkDhcpdConf(XynaOrderServerExtension correlatedXynaOrder, CheckDhcpdConfRequest_ctype checkDhcpdConfRequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = checkDhcpdConfRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype checkDhcpdConfInput = checkDhcpdConfRequest_ctype.getCheckDhcpdConfInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype checkDhcpdConfInput_ws = new com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype(checkDhcpdConfInput.getStandortGruppeID());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest_ws = new com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype(inputHeader_ws, checkDhcpdConfInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().checkDhcpdConf(checkDhcpdConfRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype checkDhcpdConfResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            checkDhcpdConfResponse_ws = payload_ws.getCheckDhcpdConfResponse();
        }
        catch(NullPointerException e) {
            checkDhcpdConfResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = checkDhcpdConfResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype checkDhcpdConfResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            checkDhcpdConfResponse.setOutputContent(checkDhcpdConfResponse_ws.getOutputContent());
            checkDhcpdConfResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setCheckDhcpdConfResponse(checkDhcpdConfResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }
  
  
  public Response_ctype checkDhcpdConfNewFormat(XynaOrderServerExtension correlatedXynaOrder, CheckDhcpdConfRequest_ctype checkDhcpdConfRequest_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = checkDhcpdConfRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype checkDhcpdConfInput = checkDhcpdConfRequest_ctype.getCheckDhcpdConfInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype checkDhcpdConfInput_ws = new com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype(checkDhcpdConfInput.getStandortGruppeID());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest_ws = new com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype(inputHeader_ws, checkDhcpdConfInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().checkDhcpdConfNewFormat(checkDhcpdConfRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype checkDhcpdConfResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            checkDhcpdConfResponse_ws = payload_ws.getCheckDhcpdConfResponse();
        }
        catch(NullPointerException e) {
            checkDhcpdConfResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = checkDhcpdConfResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype checkDhcpdConfResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            checkDhcpdConfResponse.setOutputContent(checkDhcpdConfResponse_ws.getOutputContent());
            checkDhcpdConfResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setCheckDhcpdConfResponse(checkDhcpdConfResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype deactivateForMigration(XynaOrderServerExtension correlatedXynaOrder, MigrationTargetIdentifier_ctype migrationTargetIdentifier_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deactivateForMigrationRequest_ws = new com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype(migrationTargetIdentifier_ctype.getTargetType(), migrationTargetIdentifier_ctype.getUniqueIdentifier());


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deactivateForMigration(deactivateForMigrationRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deactivateForMigrationResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            deactivateForMigrationResponse_ws = payload_ws.getDeactivateForMigrationResponse();
        }
        catch(NullPointerException e) {
            deactivateForMigrationResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = deactivateForMigrationResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());
        

        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deactivateForMigrationResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            deactivateForMigrationResponse.setOutputContent(deactivateForMigrationResponse_ws.getOutputContent());
            deactivateForMigrationResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDeactivateForMigrationResponse(deactivateForMigrationResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype deleteForMigration(XynaOrderServerExtension correlatedXynaOrder, MigrationTargetIdentifier_ctype migrationTargetIdentifier_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deleteForMigrationRequest_ws = new com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype(migrationTargetIdentifier_ctype.getTargetType(), migrationTargetIdentifier_ctype.getUniqueIdentifier());


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deleteForMigration(deleteForMigrationRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deleteForMigrationResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            deleteForMigrationResponse_ws = payload_ws.getDeleteForMigrationResponse();
        }
        catch(NullPointerException e) {
            deleteForMigrationResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = deleteForMigrationResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deleteForMigrationResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            deleteForMigrationResponse.setOutputContent(deleteForMigrationResponse_ws.getOutputContent());
            deleteForMigrationResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDeleteForMigrationResponse(deleteForMigrationResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }

  public DeployCPEResponse_ctype deployCPE(XynaOrderServerExtension correlatedXynaOrder, DeployCPERequest_ctype deployCPERequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployCPEResponse_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = deployCPERequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.CPEIdentification_ctype deployCPEInput = deployCPERequest_ctype.getDeployCPEInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Gui.WS.Messages.CPEIdentification_ctype deployCPEInput_ws = new com.gip.www.juno.Gui.WS.Messages.CPEIdentification_ctype(deployCPEInput.getCpe_mac(), deployCPEInput.getIp());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype deployCPERequest_ws = new com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype(inputHeader_ws, deployCPEInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deployCPE(deployCPERequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        // com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployCPEResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload - not necessary


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployCPEResponse_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployCPEResponse_ctype();
        try {
            response.setDeployCPEOutput(response_ws.getDeployCPEOutput());
            response.setResponseHeader(responseHeader);
        }
        catch(NullPointerException e) {}

        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype deployDhcpdConf(XynaOrderServerExtension correlatedXynaOrder, DeployDhcpdConfRequest_ctype deployDhcpdConfRequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype deployDhcpdConfInput = deployDhcpdConfRequest_ctype.getDeployDhcpdConfInput();

    com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype deployDhcpdConfInput_ws = new com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype(deployDhcpdConfInput.getStandortGruppeID());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest_ws = new com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype(inputHeader_ws, deployDhcpdConfInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deployDhcpdConf(deployDhcpdConfRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployDhcpdConfResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            deployDhcpdConfResponse_ws = payload_ws.getDeployDhcpdConfResponse();
        }
        catch(NullPointerException e) {
            deployDhcpdConfResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = deployDhcpdConfResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployDhcpdConfResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            deployDhcpdConfResponse.setOutputContent(deployDhcpdConfResponse_ws.getOutputContent());
            deployDhcpdConfResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDeployDhcpdConfResponse(deployDhcpdConfResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }
  
  
  public Response_ctype deployDhcpdConfNewFormat(XynaOrderServerExtension correlatedXynaOrder, DeployDhcpdConfRequest_ctype deployDhcpdConfRequest_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype deployDhcpdConfInput = deployDhcpdConfRequest_ctype.getDeployDhcpdConfInput();

    com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype deployDhcpdConfInput_ws = new com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype(deployDhcpdConfInput.getStandortGruppeID());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest_ws = new com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype(inputHeader_ws, deployDhcpdConfInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deployDhcpdConfNewFormat(deployDhcpdConfRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployDhcpdConfResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            deployDhcpdConfResponse_ws = payload_ws.getDeployDhcpdConfResponse();
        }
        catch(NullPointerException e) {
            deployDhcpdConfResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = deployDhcpdConfResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployDhcpdConfResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            deployDhcpdConfResponse.setOutputContent(deployDhcpdConfResponse_ws.getOutputContent());
            deployDhcpdConfResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDeployDhcpdConfResponse(deployDhcpdConfResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }
  

  public Response_ctype deployStaticHost(XynaOrderServerExtension correlatedXynaOrder, DeployStaticHostRequest_ctype deployStaticHostRequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployStaticHostInput_ctype deployStaticHostInput = deployStaticHostRequest_ctype.getDeployStaticHostInput();

    com.gip.www.juno.Gui.WS.Messages.DeployStaticHostInput_ctype deployStaticHostInput_ws = new com.gip.www.juno.Gui.WS.Messages.DeployStaticHostInput_ctype(deployStaticHostInput.getStaticHostId());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostRequest_ws = new com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype(inputHeader_ws, deployStaticHostInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deployStaticHost(deployStaticHostRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployStaticHostResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            deployStaticHostResponse_ws = payload_ws.getDeployStaticHostResponse();
        }
        catch(NullPointerException e) {
            deployStaticHostResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = deployStaticHostResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        // Row_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.DhcpdConf.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployStaticHostResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            deployStaticHostResponse.setOutputContent(deployStaticHostResponse_ws.getOutputContent());
            deployStaticHostResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDeployStaticHostResponse(deployStaticHostResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }
  
  
  public Response_ctype deployStaticHostNewFormat(XynaOrderServerExtension correlatedXynaOrder, DeployStaticHostRequest_ctype deployStaticHostRequest_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DeployStaticHostInput_ctype deployStaticHostInput = deployStaticHostRequest_ctype.getDeployStaticHostInput();

    com.gip.www.juno.Gui.WS.Messages.DeployStaticHostInput_ctype deployStaticHostInput_ws = new com.gip.www.juno.Gui.WS.Messages.DeployStaticHostInput_ctype(deployStaticHostInput.getStaticHostId());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostRequest_ws = new com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype(inputHeader_ws, deployStaticHostInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().deployStaticHostNewFormat(deployStaticHostRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployStaticHostResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            deployStaticHostResponse_ws = payload_ws.getDeployStaticHostResponse();
        }
        catch(NullPointerException e) {
            deployStaticHostResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = deployStaticHostResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        // Row_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.DhcpdConf.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype deployStaticHostResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            deployStaticHostResponse.setOutputContent(deployStaticHostResponse_ws.getOutputContent());
            deployStaticHostResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDeployStaticHostResponse(deployStaticHostResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype duplicateForMigration(XynaOrderServerExtension correlatedXynaOrder, DuplicateForMigrationRequest_ctype duplicateForMigrationRequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = duplicateForMigrationRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype source = duplicateForMigrationRequest_ctype.getSource();
    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype target = duplicateForMigrationRequest_ctype.getTarget();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype source_ws = new com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype(source.getTargetType(), source.getUniqueIdentifier());
    com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype target_ws = new com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype(target.getTargetType(), target.getUniqueIdentifier());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype duplicateForMigrationRequest_ws = new com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype(source_ws, target_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().duplicateForMigration(duplicateForMigrationRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype duplicateForMigrationResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            duplicateForMigrationResponse_ws = payload_ws.getDuplicateForMigrationResponse();
        }
        catch(NullPointerException e) {
            duplicateForMigrationResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = duplicateForMigrationResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }



        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype duplicateForMigrationResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            duplicateForMigrationResponse.setOutputContent(duplicateForMigrationResponse_ws.getOutputContent());
            duplicateForMigrationResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setDuplicateForMigrationResponse(duplicateForMigrationResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public UndeployCPEResponse_ctype undeployCPE(XynaOrderServerExtension correlatedXynaOrder, UndeployCPERequest_ctype undeployCPERequest_ctype) {
      
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployCPEResponse_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = undeployCPERequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.CPEIdentification_ctype undeployCPEInput = undeployCPERequest_ctype.getUndeployCPEInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Gui.WS.Messages.CPEIdentification_ctype undeployCPEInput_ws = new com.gip.www.juno.Gui.WS.Messages.CPEIdentification_ctype(undeployCPEInput.getCpe_mac(), undeployCPEInput.getIp());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype undeployCPERequest_ws = new com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype(inputHeader_ws, undeployCPEInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().undeployCPE(undeployCPERequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype undeployCPEResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



        //payload - not necessary


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployCPEResponse_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployCPEResponse_ctype();
        try {
            response.setUndeployCPEOutput(response_ws.getUndeployCPEOutput());
            response.setResponseHeader(responseHeader);
        }
        catch(NullPointerException e) {}

        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype undeployStaticHost(XynaOrderServerExtension correlatedXynaOrder, UndeployStaticHostRequest_ctype undeployStaticHostRequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype undeployStaticHostInput = undeployStaticHostRequest_ctype.getUndeployStaticHostInput();

    com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype undeployStaticHostInput_ws = new com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype(undeployStaticHostInput.getStaticHostId(), undeployStaticHostInput.getForce());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostRequest_ws = new com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype(inputHeader_ws, undeployStaticHostInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().undeployStaticHost(undeployStaticHostRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype undeployStaticHostResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            undeployStaticHostResponse_ws = payload_ws.getUndeployStaticHostResponse();
        }
        catch(NullPointerException e) {
            undeployStaticHostResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = undeployStaticHostResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype undeployStaticHostResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            undeployStaticHostResponse.setOutputContent(undeployStaticHostResponse_ws.getOutputContent());
            undeployStaticHostResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setUndeployStaticHostResponse(undeployStaticHostResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }
  
  
  public Response_ctype undeployStaticHostNewFormat(XynaOrderServerExtension correlatedXynaOrder, UndeployStaticHostRequest_ctype undeployStaticHostRequest_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype undeployStaticHostInput = undeployStaticHostRequest_ctype.getUndeployStaticHostInput();

    com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype undeployStaticHostInput_ws = new com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype(undeployStaticHostInput.getStaticHostId(), undeployStaticHostInput.getForce());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostRequest_ws = new com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype(inputHeader_ws, undeployStaticHostInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype response_ws = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl().undeployStaticHostNewFormat(undeployStaticHostRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Gui.WS.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype undeployStaticHostResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            undeployStaticHostResponse_ws = payload_ws.getUndeployStaticHostResponse();
        }
        catch(NullPointerException e) {
            undeployStaticHostResponse_ws = null; 
        }

        com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent_ws;

        try {
            outputHeaderContent_ws = undeployStaticHostResponse_ws.getOutputHeader();
        }
        catch(NullPointerException e) {
            outputHeaderContent_ws = null;
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype undeployStaticHostResponse = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.DhcpdConfResponse_ctype();
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeaderContent = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.OutputHeaderContent_ctype();

        try {
            outputHeaderContent.setStatus(outputHeaderContent_ws.getStatus());
            outputHeaderContent.setException(outputHeaderContent_ws.getException());
        }
        catch(NullPointerException e) {}

        try {
            undeployStaticHostResponse.setOutputContent(undeployStaticHostResponse_ws.getOutputContent());
            undeployStaticHostResponse.setOutputHeader(outputHeaderContent);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Payload_ctype();
        payload.setUndeployStaticHostResponse(undeployStaticHostResponse);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DhcpdConf.www.gip.com.juno.Gui.WS.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }
 
}

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
package xmcp.dhcp.v4.dhcpoptions.impl;


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
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.DeleteRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.GetMetaInfoRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InsertRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.UpdateRowRequest_ctype;
import xmcp.dhcp.v4.dhcpoptions.DHCPOptionsServiceOperation;


public class DHCPOptionsServiceOperationImpl implements ExtendedDeploymentTask, DHCPOptionsServiceOperation {

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

  public Response_ctype deleteRows(XynaOrderServerExtension correlatedXynaOrder, DeleteRowsRequest_ctype deleteRowsRequest_ctype) {
    // Folders: /DHCP/tlvdatabase \ /WS ; path: com.gip.www.juno.DHCP.WS.Optionsv4.Messages

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype(rh, null);
    }

    // Xyna-Objekte: searchRowsRequest_ctype
    // xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype headerContent = deleteRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype deleteRowsInput = deleteRowsRequest_ctype.getDeleteRowsInput();

    // mapping xyna -> ws:
    // com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype headerContent_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype(headerContent.getUsername(), headerContent.getPassword());
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype deleteRowsInput_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype(deleteRowsInput.getId(), deleteRowsInput.getParentId(), deleteRowsInput.getTypeName(), deleteRowsInput.getTypeEncoding(), deleteRowsInput.getEnterpriseNr(), deleteRowsInput.getValueDataTypeName(), deleteRowsInput.getValueDataTypeArgumentsString(), deleteRowsInput.getReadOnly(), deleteRowsInput.getStatus(), deleteRowsInput.getGuiName(), deleteRowsInput.getGuiAttribute(), deleteRowsInput.getGuiFixedAttribute(), deleteRowsInput.getGuiParameter(), deleteRowsInput.getGuiAttributeId(), deleteRowsInput.getGuiFixedAttributeId(), deleteRowsInput.getGuiParameterId(), deleteRowsInput.getGuiAttributeWerteBereich(), deleteRowsInput.getGuiFixedAttributeValue());

    // übergabeparameter zur ws-funktion
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeleteRowsRequest_ctype deleteRowsRequest_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeleteRowsRequest_ctype(inputHeader_ws, deleteRowsInput_ws);

    try {
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl().deleteRows(deleteRowsRequest_ws);

        //webservice Attribute extrahieren:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        //nichts notwendig

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }


        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

       //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype();
        try{
            payload.setDeleteRowsOutput(payload_ws.getDeleteRowsOutput());
        }
        catch(NullPointerException e){} //do nothing


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }

  }


  public Response_ctype deployOnDPP(XynaOrderServerExtension correlatedXynaOrder, DeployOnDPPRequest_ctype deployOnDPPRequest_ctype) {
    // xyna eingabe: deployOnDPPRequest_ctype

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();

    // mapping xyna -> ws:

    // com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword()); 

    // übergabeparameter zur ws-funktion:

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPRequest_ctype deployOnDPPRequest_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPRequest_ctype(inputHeader_ws, deployOnDPPRequest_ctype.getDeployOnDPPInput());

    try {
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl().deployOnDPP(deployOnDPPRequest_ws);

        //webservice Attribute extrahieren:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype[] deployOnDPPResponse_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try{
            deployOnDPPResponse_ws = payload_ws.getDeployOnDPPResponse();
        }
        catch(NullPointerException e){
            deployOnDPPResponse_ws = null;  //später abgefangen
        }


        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //DeployOnDPPResponse
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPResponse_ctype deployOnDPP = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPResponse_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype> statusReport = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype output: deployOnDPPResponse_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype row = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.StatusReport_ctype();
                row.setLocation(output.getLocation());
                row.setStatus(output.getStatus());
                statusReport.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        deployOnDPP.setStatusReport(statusReport);

       //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype();
        payload.setDeployOnDPPResponse(deployOnDPP);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype getMetaInfo(XynaOrderServerExtension correlatedXynaOrder, GetMetaInfoRequest_ctype getMetaInfoRequest_ctype) {
    
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype(rh, null);
    }

    //Xyna-Objekte:
    // xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader = getMetaInfoRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.GetMetaInfoRequest_ctype metaInfoRequest_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.GetMetaInfoRequest_ctype(inputHeader_ws, getMetaInfoRequest_ctype.getGetMetaInfoInput());


    try{
        //Rückgabe des Webservices:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl().getMetaInfo(metaInfoRequest_ws);

        //WebService-Attribute extrahieren:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        //Response Header:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype[] metaInfoOutput_ws;
        

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try{
            metaInfoOutput_ws = payload_ws.getMetaInfoOutput();
        }
        catch(NullPointerException e){
            metaInfoOutput_ws = null;  //später abgefangen
        }


        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //MetaInfoOutput
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.MetaInfo_ctype metaInfo = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.MetaInfo_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype> mi_col = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype output: metaInfoOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype row = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.MetaInfoRow_ctype();
                row.setVisible(output.isVisible());
                row.setUpdates(output.isUpdates());
                row.setGuiname(output.getGuiname());
                row.setColname(output.getColname());
                row.setColnum(output.getColnum().intValue());  //ursprünglich BigInteger
                row.setChildtable(output.getChildtable());
                row.setParenttable(output.getParenttable());
                row.setParentcol(output.getParentcol());
                row.setInputType(output.getInputType());
                row.setInputFormat(output.getInputFormat());
                row.setOptional(output.getOptional());
                mi_col.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        metaInfo.setCol(mi_col);
       

        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype();
        payload.setMetaInfoOutput(metaInfo);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;
        
    }
    catch(java.rmi.RemoteException e){
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype insertRow(XynaOrderServerExtension correlatedXynaOrder, InsertRowRequest_ctype insertRowRequest_ctype) {
    // Xyna Objekte insertRowRequest_ctype

    // Xyna -> Webservice

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader = insertRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype insertRowInput = insertRowRequest_ctype.getInsertRowInput();

    // com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype insertRowInput_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype(insertRowInput.getId(), insertRowInput.getParentId(), insertRowInput.getTypeName(), insertRowInput.getTypeEncoding(), insertRowInput.getEnterpriseNr(), insertRowInput.getValueDataTypeName(), insertRowInput.getValueDataTypeArgumentsString(), insertRowInput.getReadOnly(), insertRowInput.getStatus(), insertRowInput.getGuiName(), insertRowInput.getGuiAttribute(), insertRowInput.getGuiFixedAttribute(), insertRowInput.getGuiParameter(), insertRowInput.getGuiAttributeId(), insertRowInput.getGuiFixedAttributeId(), insertRowInput.getGuiParameterId(), insertRowInput.getGuiAttributeWerteBereich(), insertRowInput.getGuiFixedAttributeValue());

    // übergabe zur ws-funktion:

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InsertRowRequest_ctype insertRowRequest_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InsertRowRequest_ctype(inputHeader_ws, insertRowInput_ws);

    try {
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl().insertRow(insertRowRequest_ws);

        // webservice Attribute extrahieren:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype[] parameterList_ws;

        // payload:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype insertRowOutput_ws;
        
        try {
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e) {
            parameterList_ws = null; 
        }

        try {
            insertRowOutput_ws = payload_ws.getInsertRowOutput();
        }
        catch(NullPointerException e) {
            insertRowOutput_ws = null;
        }

        // ab hier Mapping 

        //Response Header:
        
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype param: parameterList_ws){
            xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype errorParameter= new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype();
            errorParameter.setId(param.getId());
            errorParameter.setValue(param.getValue());
            parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} 
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());

        
        //payload:
        
        // insertRowOutput:

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype insertRowOutput = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype();
        
        try {
        insertRowOutput.setId(insertRowOutput_ws.getId());
        insertRowOutput.setParentId(insertRowOutput_ws.getParentId());
        insertRowOutput.setTypeName(insertRowOutput_ws.getTypeName());
        insertRowOutput.setTypeEncoding(insertRowOutput_ws.getTypeEncoding());
        insertRowOutput.setEnterpriseNr(insertRowOutput_ws.getEnterpriseNr());
        insertRowOutput.setValueDataTypeName(insertRowOutput_ws.getValueDataTypeName());
        insertRowOutput.setValueDataTypeArgumentsString(insertRowOutput_ws.getValueDataTypeArgumentsString());
        insertRowOutput.setReadOnly(insertRowOutput_ws.getReadOnly());
        insertRowOutput.setStatus(insertRowOutput_ws.getStatus());
        insertRowOutput.setGuiName(insertRowOutput_ws.getGuiName());
        insertRowOutput.setGuiAttribute(insertRowOutput_ws.getGuiAttribute());
        insertRowOutput.setGuiFixedAttribute(insertRowOutput_ws.getGuiFixedAttribute());
        insertRowOutput.setGuiParameter(insertRowOutput_ws.getGuiParameter());
        insertRowOutput.setGuiAttributeId(insertRowOutput_ws.getGuiAttributeId());
        insertRowOutput.setGuiFixedAttributeId(insertRowOutput_ws.getGuiFixedAttributeId());
        insertRowOutput.setGuiParameterId(insertRowOutput_ws.getGuiParameterId());
        insertRowOutput.setGuiAttributeWerteBereich(insertRowOutput_ws.getGuiAttributeWerteBereich());
        insertRowOutput.setGuiFixedAttributeValue(insertRowOutput_ws.getGuiFixedAttributeValue());
        }
        catch(NullPointerException e) {} 

        // payload main Attributes
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype();
        payload.setInsertRowOutput(insertRowOutput);

        // return - Objekte
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;
    }

    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype searchRows(XynaOrderServerExtension correlatedXynaOrder, SearchRowsRequest_ctype searchRowsRequest_ctype) {
    // Folders: /DHCP/tlvdatabase \ /WS ; path: com.gip.www.juno.DHCP.WS.Optionsv4.Messages

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype(rh, null);
    }

    // Xyna-Objekte: searchRowsRequest_ctype
    // xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype headerContent = searchRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    // mapping xyna -> ws:
    // com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype headerContent_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype(headerContent.getUsername(), headerContent.getPassword());
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype(searchRowsInput.getId(), searchRowsInput.getParentId(), searchRowsInput.getTypeName(), searchRowsInput.getTypeEncoding(), searchRowsInput.getEnterpriseNr(), searchRowsInput.getValueDataTypeName(), searchRowsInput.getValueDataTypeArgumentsString(), searchRowsInput.getReadOnly(), searchRowsInput.getStatus(), searchRowsInput.getGuiName(), searchRowsInput.getGuiAttribute(), searchRowsInput.getGuiFixedAttribute(), searchRowsInput.getGuiParameter(), searchRowsInput.getGuiAttributeId(), searchRowsInput.getGuiFixedAttributeId(), searchRowsInput.getGuiParameterId(), searchRowsInput.getGuiAttributeWerteBereich(), searchRowsInput.getGuiFixedAttributeValue());

    // übergabeparameter zur ws-funktion
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);

    try {
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl().searchRows(searchRowsRequest_ws);

        //webservice Attribute extrahieren:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        //com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype[] getAllRowsOutput_ws;
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype[] searchRowsOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try{
            searchRowsOutput_ws = payload_ws.getSearchRowsOutput();
        }
        catch(NullPointerException e){
            searchRowsOutput_ws = null;  //später abgefangen
        }


        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //GetAllRowsOutput - not necessary!

        //SearchRowsOutput
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.RowList_ctype searchRows = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype output: searchRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype();
                row.setId(output.getId());
                row.setParentId(output.getParentId());
                row.setTypeName(output.getTypeName());
                row.setTypeEncoding(output.getTypeEncoding());
                row.setEnterpriseNr(output.getEnterpriseNr());
                row.setValueDataTypeName(output.getValueDataTypeName());
                row.setValueDataTypeArgumentsString(output.getValueDataTypeArgumentsString());
                row.setReadOnly(output.getReadOnly());
                row.setStatus(output.getStatus());
                row.setGuiName(output.getGuiName());
                row.setGuiAttribute(output.getGuiAttribute());
                row.setGuiFixedAttribute(output.getGuiFixedAttribute());
                row.setGuiParameter(output.getGuiParameter());
                row.setGuiAttributeId(output.getGuiAttributeId());
                row.setGuiFixedAttributeId(output.getGuiFixedAttributeId());
                row.setGuiParameterId(output.getGuiParameterId());
                row.setGuiAttributeWerteBereich(output.getGuiAttributeWerteBereich());
                row.setGuiFixedAttributeValue(output.getGuiFixedAttributeValue());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);


       //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype();
        payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }

  }


  public Response_ctype updateRow(XynaOrderServerExtension correlatedXynaOrder, UpdateRowRequest_ctype updateRowRequest_ctype) {
    // Folders: /DHCP/tlvdatabase \ /WS ; path: com.gip.www.juno.DHCP.WS.Optionsv4.Messages

    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype(rh, null);
    }

    // Xyna-Objekte: searchRowsRequest_ctype
    // xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype headerContent = updateRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype updateRowInput = updateRowRequest_ctype.getUpdateRowInput();

    // mapping xyna -> ws:
    // com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype headerContent_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InputHeaderContent_ctype(headerContent.getUsername(), headerContent.getPassword());
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype updateRowInput_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype(updateRowInput.getId(), updateRowInput.getParentId(), updateRowInput.getTypeName(), updateRowInput.getTypeEncoding(), updateRowInput.getEnterpriseNr(), updateRowInput.getValueDataTypeName(), updateRowInput.getValueDataTypeArgumentsString(), updateRowInput.getReadOnly(), updateRowInput.getStatus(), updateRowInput.getGuiName(), updateRowInput.getGuiAttribute(), updateRowInput.getGuiFixedAttribute(), updateRowInput.getGuiParameter(), updateRowInput.getGuiAttributeId(), updateRowInput.getGuiFixedAttributeId(), updateRowInput.getGuiParameterId(), updateRowInput.getGuiAttributeWerteBereich(), updateRowInput.getGuiFixedAttributeValue());

    // übergabeparameter zur ws-funktion
    com.gip.www.juno.DHCP.WS.Optionsv4.Messages.UpdateRowRequest_ctype updateRowRequest_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Messages.UpdateRowRequest_ctype(inputHeader_ws, updateRowInput_ws);

    try {
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl().updateRow(updateRowRequest_ws);

        //webservice Attribute extrahieren:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        //com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype[] getAllRowsOutput_ws;
        com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Row_ctype updateRowOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try{
            updateRowOutput_ws = payload_ws.getUpdateRowOutput();
        }
        catch(NullPointerException e){
            updateRowOutput_ws = null;  //später abgefangen
        }


        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //UpdateRowOutput
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype updateRow = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Row_ctype();
        try{
            updateRow.setId(updateRowOutput_ws.getId());
            updateRow.setParentId(updateRowOutput_ws.getParentId());
            updateRow.setTypeName(updateRowOutput_ws.getTypeName());
            updateRow.setTypeEncoding(updateRowOutput_ws.getTypeEncoding());
            updateRow.setEnterpriseNr(updateRowOutput_ws.getEnterpriseNr());
            updateRow.setValueDataTypeName(updateRowOutput_ws.getValueDataTypeName());
            updateRow.setValueDataTypeArgumentsString(updateRowOutput_ws.getValueDataTypeArgumentsString());
            updateRow.setReadOnly(updateRowOutput_ws.getReadOnly());
            updateRow.setStatus(updateRowOutput_ws.getStatus());
            updateRow.setGuiName(updateRowOutput_ws.getGuiName());
            updateRow.setGuiAttribute(updateRowOutput_ws.getGuiAttribute());
            updateRow.setGuiFixedAttribute(updateRowOutput_ws.getGuiFixedAttribute());
            updateRow.setGuiParameter(updateRowOutput_ws.getGuiParameter());
            updateRow.setGuiAttributeId(updateRowOutput_ws.getGuiAttributeId());
            updateRow.setGuiFixedAttributeId(updateRowOutput_ws.getGuiFixedAttributeId());
            updateRow.setGuiParameterId(updateRowOutput_ws.getGuiParameterId());
            updateRow.setGuiAttributeWerteBereich(updateRowOutput_ws.getGuiAttributeWerteBereich());
            updateRow.setGuiFixedAttributeValue(updateRowOutput_ws.getGuiFixedAttributeValue());
        }
        catch(NullPointerException e){} //do nothing


       //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Payload_ctype();
        payload.setUpdateRowOutput(updateRow);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DHCPOptions.www.gip.com.juno.DHCP.WS.Optionsv4.Messages.Response_ctype();
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

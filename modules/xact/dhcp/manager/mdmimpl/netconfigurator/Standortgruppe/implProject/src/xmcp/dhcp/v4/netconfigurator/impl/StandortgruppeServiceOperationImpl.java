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
import xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.DeleteRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.InsertRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.UpdateRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.GetAllRowsRequest_ctype;
import xmcp.dhcp.v4.netconfigurator.StandortgruppeServiceOperation;


public class StandortgruppeServiceOperationImpl implements ExtendedDeploymentTask, StandortgruppeServiceOperation {

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

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = DeleteRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype deleteRowsInput = deleteRowsRequest_ctype.getDeleteRowsInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype deleteRowsInput_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype(deleteRowsInput.getStandortGruppeID(), deleteRowsInput.getName());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.DeleteRowsRequest_ctype deleteRowsRequest_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.DeleteRowsRequest_ctype(inputHeader_ws, deleteRowsInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.StandortgruppeBindingImpl().deleteRows(deleteRowsRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        // com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype deleteRowsOutput_ws; - here not necessary

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        // not necessary


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype();

        try{
            payload.setDeleteRowsOutput(payload_ws.getDeleteRowsOutput());
        }
        catch(NullPointerException e) {}


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype getAllRows(XynaOrderServerExtension correlatedXynaOrder, GetAllRowsRequest_ctype getAllRowsRequest_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = getAllRowsRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype(inputHeader_ws, getAllRowsRequest_ctype.getGetAllRowsInput());


    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.StandortgruppeBindingImpl().getAllRows(getAllRowsRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype[] getAllRowsOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            getAllRowsOutput_ws = payload_ws.getGetAllRowsOutput();
        }
        catch(NullPointerException e) {
            getAllRowsOutput_ws = null; 
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        // RowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.RowList_ctype getAllRowsOutput = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype> getAllRowsList = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype>();

        // ...Row_ctype - heißt die Klasse dieses mal! 

        try {
            for(com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype output: getAllRowsOutput_ws) {
                xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype();
                row.setStandortGruppeID(output.getStandortGruppeID());
                row.setName(output.getName());
                getAllRowsList.add(row);
            }
            getAllRowsOutput.setRow(getAllRowsList);
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype();
        payload.setGetAllRowsOutput(getAllRowsOutput);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e) {
        e.printStackTrace();
        return null;
    }
  }


  public Response_ctype insertRow(XynaOrderServerExtension correlatedXynaOrder, InsertRowRequest_ctype insertRowRequest_ctype) {
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = insertRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype insertRowInput = insertRowRequest_ctype.getInsertRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype insertRowInput_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype(insertRowInput.getStandortGruppeID(), insertRowInput.getName());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.InsertRowRequest_ctype insertRowRequest_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.InsertRowRequest_ctype(inputHeader_ws, insertRowInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.StandortgruppeBindingImpl().insertRow(insertRowRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype insertRowOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            insertRowOutput_ws = payload_ws.getInsertRowOutput();
        }
        catch(NullPointerException e) {
            insertRowOutput_ws = null; 
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        // RowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype insertRowOutput = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype();

        // ...Row_ctype - heißt die Klasse dieses mal! 

        try {
            insertRowOutput.setStandortGruppeID(insertRowOutput_ws.getStandortGruppeID());
            insertRowOutput.setName(insertRowOutput_ws.getName());
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype();
        payload.setInsertRowOutput(insertRowOutput);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype();
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
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = updateRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype updateRowInput = updateRowRequest_ctype.getUpdateRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype updateRowInput_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype(updateRowInput.getStandortGruppeID(), updateRowInput.getName());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.UpdateRowRequest_ctype updateRowRequest_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.UpdateRowRequest_ctype(inputHeader_ws, updateRowInput_ws);


    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Standortgruppe.StandortgruppeBindingImpl().updateRow(updateRowRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype updateRowOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            updateRowOutput_ws = payload_ws.getUpdateRowOutput();
        }
        catch(NullPointerException e) {
            updateRowOutput_ws = null; 
        }


        // ab hier mapping

        // Response Header
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload 
        // RowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype updateRowOutput = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Row_ctype();

        // ...Row_ctype - heißt die Klasse dieses mal! 

        try {
            updateRowOutput.setStandortGruppeID(updateRowOutput_ws.getStandortGruppeID());
            updateRowOutput.setName(updateRowOutput_ws.getName());
        }
        catch(NullPointerException e) {} // do nothing


        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Payload_ctype();
        payload.setUpdateRowOutput(updateRowOutput);


        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Standortgruppe.www.gip.com.juno.DHCP.WS.Standortgruppe.Messages.Response_ctype();
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

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
package xmcp.dhcp.v4.control.impl;


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
import xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.DeleteRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.InsertRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.UpdateRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.GetAllRowsRequest_ctype;
import xmcp.dhcp.v4.control.SharedNetworkServiceOperation;


public class SharedNetworkServiceOperationImpl implements ExtendedDeploymentTask, SharedNetworkServiceOperation {

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
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.netconfigurator.SharedNetworkImpl.DeleteRows(correlatedXynaOrder, deleteRowsRequest_ctype);

    // location: "DHCP SVN".dhcp.SharedNetwork.src.com.gip.www.juno.DHCP.WS.SharedNetwork

    // Input: deleteRowsRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.SharedNetwork.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = deleteRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype deleteRowsInput = deleteRowsRequest_ctype.getDeleteRowsInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype deleteRowsInput_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype(deleteRowsInput.getSharedNetworkID(), deleteRowsInput.getStandortID(), deleteRowsInput.getStandort(), deleteRowsInput.getSharedNetwork(), deleteRowsInput.getCpeDnsID(), deleteRowsInput.getCpeDns(), deleteRowsInput.getLinkAddresses(), deleteRowsInput.getMigrationState());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.DeleteRowsRequest_ctype deleteRowsRequest_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.DeleteRowsRequest_ctype(inputHeader_ws, deleteRowsInput_ws);



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingImpl().deleteRows(deleteRowsRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload: not necessary

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }





    // ab hier mapping

    // Response Header
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



        //payload 

        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype();

        try{
            payload.setDeleteRowsOutput(payload_ws.getDeleteRowsOutput());
            }
        catch(NullPointerException e) {}



        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;


    }

    catch(java.rmi.RemoteException e) {
        //TODO: implement
        e.printStackTrace();
        return null;
        }
  }
  
  //------------------------------------------------------------------------------------------------------------------------------------

  public Response_ctype getAllRows(XynaOrderServerExtension correlatedXynaOrder, GetAllRowsRequest_ctype getAllRowsRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.control.SharedNetworkImpl.getAllRows(getAllRowsRequest_ctype);
    // Input: getAllRowsRequest_ctype 

    // P1: com.gip.www.juno.DHCP.WS.SharedNetwork.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = getAllRowsRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype(inputHeader_ws, getAllRowsRequest_ctype.getGetAllRowsInput());



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingImpl().getAllRows(getAllRowsRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype[] getAllRowsOutput_ws;

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
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // RowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.RowList_ctype getAllRowsOutput = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.RowList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype> getAllRowsList = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype>();

    // ...Row_ctype - heißt die Klasse dieses mal! 

    try {
    for(com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype output: getAllRowsOutput_ws) {
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype();
    row.setSharedNetworkID(output.getSharedNetworkID());
    row.setStandortID(output.getStandortID());
    row.setStandort(output.getStandort());
    row.setSharedNetwork(output.getSharedNetwork());
    row.setCpeDnsID(output.getCpeDnsID());
    row.setCpeDns(output.getCpeDns());
    row.setLinkAddresses(output.getLinkAddresses());
    row.setMigrationState(output.getMigrationState());
    getAllRowsList.add(row);
    }
    getAllRowsOutput.setRow(getAllRowsList);
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype();
    payload.setGetAllRowsOutput(getAllRowsOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype();
    response.setResponseHeader(responseHeader);
    response.setPayload(payload);
    return response;


    }

    catch(java.rmi.RemoteException e) {
    //TODO: implement
    e.printStackTrace();
    return null;
    }
  }
  
  //------------------------------------------------------------------------------------------------------------------------------------

  public Response_ctype insertRow(XynaOrderServerExtension correlatedXynaOrder, InsertRowRequest_ctype insertRowRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.netconfigurator.SharedNetworkImpl.insertRow(correlatedXynaOrder, insertRowRequest_ctype);

    // location: "DHCP SVN".dhcp.SharedNetwork.src.com.gip.www.juno.DHCP.WS.SharedNetwork

    // Input: insertRowRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.SharedNetwork.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = insertRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype insertRowInput = insertRowRequest_ctype.getInsertRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype insertRowInput_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype(insertRowInput.getSharedNetworkID(), insertRowInput.getStandortID(), insertRowInput.getStandort(), insertRowInput.getSharedNetwork(), insertRowInput.getCpeDnsID(), insertRowInput.getCpeDns(), insertRowInput.getLinkAddresses(), insertRowInput.getMigrationState());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.InsertRowRequest_ctype insertRowRequest_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.InsertRowRequest_ctype(inputHeader_ws, insertRowInput_ws);



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingImpl().insertRow(insertRowRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype insertRowOutput_ws;

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
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // Row_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype insertRowOutput = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype();

    try {
    insertRowOutput.setSharedNetworkID(insertRowOutput_ws.getSharedNetworkID());
    insertRowOutput.setStandortID(insertRowOutput_ws.getStandortID());
    insertRowOutput.setStandort(insertRowOutput_ws.getStandort());
    insertRowOutput.setSharedNetwork(insertRowOutput_ws.getSharedNetwork());
    insertRowOutput.setCpeDnsID(insertRowOutput_ws.getCpeDnsID());
    insertRowOutput.setCpeDns(insertRowOutput_ws.getCpeDns());
    insertRowOutput.setLinkAddresses(insertRowOutput_ws.getLinkAddresses());
    insertRowOutput.setMigrationState(insertRowOutput_ws.getMigrationState());
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype();
    payload.setInsertRowOutput(insertRowOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype();
    response.setResponseHeader(responseHeader);
    response.setPayload(payload);
    return response;


    }

    catch(java.rmi.RemoteException e) {
    //TODO: implement
    e.printStackTrace();
    return null;
    }
  }
  
  //------------------------------------------------------------------------------------------------------------------------------------

  public Response_ctype searchRows(XynaOrderServerExtension correlatedXynaOrder, SearchRowsRequest_ctype searchRowsRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.control.SharedNetworkImpl.searchRows(correlatedXynaOrder, searchRowsRequest_ctype);
    // Xyna-Objekte: searchRowsRequest_ctype

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype(rh, null);
    }




    // xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = searchRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype(searchRowsInput.getSharedNetworkID(), searchRowsInput.getStandortID(), searchRowsInput.getStandort(), searchRowsInput.getSharedNetwork(), searchRowsInput.getCpeDnsID(), searchRowsInput.getCpeDns(), searchRowsInput.getLinkAddresses(), searchRowsInput.getMigrationState());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);


    try{
        // Rückgabe des Webservices: (TODO)
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingImpl().searchRows(searchRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype[] searchRowsOutput_ws;

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

        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //searchRowsOutput
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.RowList_ctype searchRows = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype output: searchRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype();
                row.setSharedNetworkID(output.getSharedNetworkID());
                row.setStandortID(output.getStandortID());
                row.setStandort(output.getStandort());
                row.setSharedNetwork(output.getSharedNetwork());
                row.setCpeDnsID(output.getCpeDnsID());
                row.setCpeDns(output.getCpeDns());
                row.setLinkAddresses(output.getLinkAddresses());
                row.setMigrationState(output.getMigrationState());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);
        
        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype();
        payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;
        

    }
    catch(java.rmi.RemoteException e){
    //TODO: implement
        e.printStackTrace();
        return null;
    }

  }
  
  //------------------------------------------------------------------------------------------------------------------------------------

  public Response_ctype updateRow(XynaOrderServerExtension correlatedXynaOrder, UpdateRowRequest_ctype updateRowRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.netconfigurator.SharedNetworkImpl.updateRow(correlatedXynaOrder, updateRowRequest_ctype);

    // location: "DHCP SVN".dhcp.SharedNetwork.src.com.gip.www.juno.DHCP.WS.SharedNetwork

    // Input: updateRowRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.SharedNetwork.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = updateRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype updateRowInput = updateRowRequest_ctype.getUpdateRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype updateRowInput_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype(updateRowInput.getSharedNetworkID(), updateRowInput.getStandortID(), updateRowInput.getStandort(), updateRowInput.getSharedNetwork(), updateRowInput.getCpeDnsID(), updateRowInput.getCpeDns(), updateRowInput.getLinkAddresses(), updateRowInput.getMigrationState());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.UpdateRowRequest_ctype updateRowRequest_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.UpdateRowRequest_ctype(inputHeader_ws, updateRowInput_ws);



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingImpl().updateRow(updateRowRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype updateRowOutput_ws;

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
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // Row_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype updateRowOutput = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Row_ctype();

    try {
    updateRowOutput.setSharedNetworkID(updateRowOutput_ws.getSharedNetworkID());
    updateRowOutput.setStandortID(updateRowOutput_ws.getStandortID());
    updateRowOutput.setStandort(updateRowOutput_ws.getStandort());
    updateRowOutput.setSharedNetwork(updateRowOutput_ws.getSharedNetwork());
    updateRowOutput.setCpeDnsID(updateRowOutput_ws.getCpeDnsID());
    updateRowOutput.setCpeDns(updateRowOutput_ws.getCpeDns());
    updateRowOutput.setLinkAddresses(updateRowOutput_ws.getLinkAddresses());
    updateRowOutput.setMigrationState(updateRowOutput_ws.getMigrationState());
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype();
    payload.setUpdateRowOutput(updateRowOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.SharedNetwork.www.gip.com.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype();
    response.setResponseHeader(responseHeader);
    response.setPayload(payload);
    return response;


    }

    catch(java.rmi.RemoteException e) {
    //TODO: implement
    e.printStackTrace();
    return null;
    }
  }

}

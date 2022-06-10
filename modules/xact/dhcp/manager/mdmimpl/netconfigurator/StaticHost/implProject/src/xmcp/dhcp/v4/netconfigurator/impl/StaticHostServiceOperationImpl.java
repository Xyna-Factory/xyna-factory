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
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.DeleteRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.UpdateRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.GetAllRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype;
import xmcp.dhcp.v4.netconfigurator.StaticHostServiceOperation;


public class StaticHostServiceOperationImpl implements ExtendedDeploymentTask, StaticHostServiceOperation {

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
    // return xmcp.dhcp.v4.netconfigurator.StaticHostImpl.DeleteRows(correlatedXynaOrder, deleteRowsRequest_ctype);

    // location: "DHCP SVN".dhcp.StaticHost.src.com.gip.www.juno.DHCP.WS.StaticHost

    // Input: deleteRowsRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.StaticHost.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = deleteRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype deleteRowsInput = deleteRowsRequest_ctype.getDeleteRowsInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype deleteRowsInput_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype(deleteRowsInput.getStaticHostID(), deleteRowsInput.getSubnetID(), deleteRowsInput.getSubnet(), deleteRowsInput.getCpe_mac(), deleteRowsInput.getRemoteId(), deleteRowsInput.getIp(), deleteRowsInput.getDns(), deleteRowsInput.getHostname(), deleteRowsInput.getDeployed1(), deleteRowsInput.getDeployed2(), deleteRowsInput.getDynamicDnsActive(), deleteRowsInput.getConfigDescr(), deleteRowsInput.getAssignedPoolID(), deleteRowsInput.getPool(), deleteRowsInput.getDesiredPoolType(), deleteRowsInput.getPoolType(), deleteRowsInput.getCmtsip());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteRowsRequest_ctype deleteRowsRequest_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteRowsRequest_ctype(inputHeader_ws, deleteRowsInput_ws);



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl().deleteRows(deleteRowsRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
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
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



        //payload 

        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype();

        try{
            payload.setDeleteRowsOutput(payload_ws.getDeleteRowsOutput());
            }
        catch(NullPointerException e) {}



        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype();
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
  
  //-------------------------------------------------------------------------------------------------------------------------

  public Response_ctype getAllRowsWithSessionAuthentification(XynaOrderServerExtension correlatedXynaOrder, GetAllRowsRequest_ctype getAllRowsRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.netconfigurator.StaticHostImpl.getAllRowsWithSessionAuthentification(getAllRowsRequest_ctype);

    // input getAllRowsRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.StaticHost.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = getAllRowsRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype(inputHeader_ws, getAllRowsRequest_ctype.getGetAllRowsInput());



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl().getAllRows(getAllRowsRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype[] getAllRowsOutput_ws;

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
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // RowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.RowList_ctype getAllRowsOutput = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.RowList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype> getAllRowsList = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype>();

    // ...Row_ctype - heißt die Klasse dieses mal! 

    try {
    for(com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype output: getAllRowsOutput_ws) {
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype();
    row.setStaticHostID(output.getStaticHostID());
    row.setSubnetID(output.getSubnetID());
    row.setSubnet(output.getSubnet());
    row.setCpe_mac(output.getCpe_mac());
    row.setRemoteId(output.getRemoteId());
    row.setIp(output.getIp());
    row.setDns(output.getDns());
    row.setHostname(output.getHostname());
    row.setDeployed1(output.getDeployed1());
    row.setDeployed2(output.getDeployed2());
    row.setDynamicDnsActive(output.getDynamicDnsActive());
    row.setConfigDescr(output.getConfigDescr());
    row.setAssignedPoolID(output.getAssignedPoolID());
    row.setPool(output.getPool());
    row.setDesiredPoolType(output.getDesiredPoolType());
    row.setPoolType(output.getPoolType());
    row.setCmtsip(output.getCmtsip());
    getAllRowsList.add(row);
    }
    getAllRowsOutput.setRow(getAllRowsList);
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype();
    payload.setGetAllRowsOutput(getAllRowsOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype();
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

  //-------------------------------------------------------------------------------------------------------------------------
  
  public Response_ctype getMetaInfo(XynaOrderServerExtension correlatedXynaOrder, GetMetaInfoRequest_ctype getMetaInfoRequest_ctype) {
    // Implemented as code snippet!
    // input. getMetaInfoRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.StaticHost.Messages.
    // P2: com.gip.www.juno.Gui.WS.Messages.

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype(rh, null);
    }

    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = getMetaInfoRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype(inputHeader_ws, getMetaInfoRequest_ctype.getGetMetaInfoInput());

    try {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl().getMetaInfo(getMetaInfoRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try {
            metaInfoOutput_ws = payload_ws.getMetaInfoOutput();
        }
        catch(NullPointerException e) {
            metaInfoOutput_ws = null; 
        }

        
        
        // ab hier mapping
        
        
        // Response Header
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();
        
        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
            xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
            errorParameter.setId(param.getId());
            errorParameter.setValue(param.getValue());
            parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);
        
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());
        
        
        
        //payload
        // MetaInfo_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype metaInfo = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype> mi_col = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype>();
        
        try {
            for(com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype output: metaInfoOutput_ws) {
            xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype row = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype();
            row.setGuiname(output.getGuiname());   // boolean ausgabe Variablen heißen immer is...! 
            row.setColname(output.getColname());
            row.setColnum(output.getColnum().intValue());
            row.setChildtable(output.getChildtable());
            row.setParenttable(output.getParenttable());
            row.setParentcol(output.getParentcol());
            row.setInputType(output.getInputType());
            row.setInputFormat(output.getInputFormat());
            row.setOptional(output.getOptional());
            mi_col.add(row);
            }
        }
        catch(NullPointerException e) {} // do nothing
        metaInfo.setCol(mi_col);
        
        
        
        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype();
        payload.setMetaInfoOutput(metaInfo);
        
        
        //Return Objekt
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype();
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
  
  //-------------------------------------------------------------------------------------------------------------------------

  public Response_ctype insertRow(XynaOrderServerExtension correlatedXynaOrder, InsertRowRequest_ctype insertRowRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.netconfigurator.StaticHostImpl.insertRow(correlatedXynaOrder, insertRowRequest_ctype);

    // location: "DHCP SVN".dhcp.StaticHost.src.com.gip.www.juno.DHCP.WS.StaticHost

    // Input: insertRowRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.StaticHost.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = insertRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype insertRowInput = insertRowRequest_ctype.getInsertRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype insertRowInput_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype(insertRowInput.getStaticHostID(), insertRowInput.getSubnetID(), insertRowInput.getSubnet(), insertRowInput.getCpe_mac(), insertRowInput.getRemoteId(), insertRowInput.getIp(), insertRowInput.getDns(), insertRowInput.getHostname(), insertRowInput.getDeployed1(), insertRowInput.getDeployed2(), insertRowInput.getDynamicDnsActive(), insertRowInput.getConfigDescr(), insertRowInput.getAssignedPoolID(), insertRowInput.getPool(), insertRowInput.getDesiredPoolType(), insertRowInput.getPoolType(), insertRowInput.getCmtsip());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype insertRowRequest_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype(inputHeader_ws, insertRowInput_ws);



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl().insertRow(insertRowRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype insertRowOutput_ws;

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
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // Row_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype insertRowOutput = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype();

    try {
    insertRowOutput.setStaticHostID(insertRowOutput_ws.getStaticHostID());
    insertRowOutput.setSubnetID(insertRowOutput_ws.getSubnetID());
    insertRowOutput.setSubnet(insertRowOutput_ws.getSubnet());
    insertRowOutput.setCpe_mac(insertRowOutput_ws.getCpe_mac());
    insertRowOutput.setRemoteId(insertRowOutput_ws.getRemoteId());
    insertRowOutput.setIp(insertRowOutput_ws.getIp());
    insertRowOutput.setDns(insertRowOutput_ws.getDns());
    insertRowOutput.setHostname(insertRowOutput_ws.getHostname());
    insertRowOutput.setDeployed1(insertRowOutput_ws.getDeployed1());
    insertRowOutput.setDeployed2(insertRowOutput_ws.getDeployed2());
    insertRowOutput.setDynamicDnsActive(insertRowOutput_ws.getDynamicDnsActive());
    insertRowOutput.setConfigDescr(insertRowOutput_ws.getConfigDescr());
    insertRowOutput.setAssignedPoolID(insertRowOutput_ws.getAssignedPoolID());
    insertRowOutput.setPool(insertRowOutput_ws.getPool());
    insertRowOutput.setDesiredPoolType(insertRowOutput_ws.getDesiredPoolType());
    insertRowOutput.setPoolType(insertRowOutput_ws.getPoolType());
    insertRowOutput.setCmtsip(insertRowOutput_ws.getCmtsip());
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype();
    payload.setInsertRowOutput(insertRowOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype();
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

  //-------------------------------------------------------------------------------------------------------------------------
  
  public Response_ctype searchRows(XynaOrderServerExtension correlatedXynaOrder, SearchRowsRequest_ctype searchRowsRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.control.StaticHostImpl.searchRows(correlatedXynaOrder, searchRowsRequest_ctype);
    // Xyna-Objekte: searchRowsRequest_ctype

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype(rh, null);
    }




    // xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = searchRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype(searchRowsInput.getStaticHostID(), searchRowsInput.getSubnetID(), searchRowsInput.getSubnet(), searchRowsInput.getCpe_mac(), searchRowsInput.getRemoteId(), searchRowsInput.getIp(), searchRowsInput.getDns(), searchRowsInput.getHostname(), searchRowsInput.getDeployed1(), searchRowsInput.getDeployed2(), searchRowsInput.getDynamicDnsActive(), searchRowsInput.getConfigDescr(), searchRowsInput.getAssignedPoolID(), searchRowsInput.getPool(), searchRowsInput.getDesiredPoolType(), searchRowsInput.getPoolType(), searchRowsInput.getCmtsip());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);


    try{
        // Rückgabe des Webservices: (TODO)
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl().searchRows(searchRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype[] searchRowsOutput_ws;

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

        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //searchRowsOutput
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.RowList_ctype searchRows = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype output: searchRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype();
                row.setStaticHostID(output.getStaticHostID());
                row.setSubnetID(output.getSubnetID());
                row.setSubnet(output.getSubnet());
                row.setCpe_mac(output.getCpe_mac());
                row.setRemoteId(output.getRemoteId());
                row.setIp(output.getIp());
                row.setDns(output.getDns());
                row.setHostname(output.getHostname());
                row.setDeployed1(output.getDeployed1());
                row.setDeployed2(output.getDeployed2());
                row.setDynamicDnsActive(output.getDynamicDnsActive());
                row.setConfigDescr(output.getConfigDescr());
                row.setAssignedPoolID(output.getAssignedPoolID());
                row.setPool(output.getPool());
                row.setDesiredPoolType(output.getDesiredPoolType());
                row.setPoolType(output.getPoolType());
                row.setCmtsip(output.getCmtsip());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);
        
        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype();
        payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype();
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

  //-------------------------------------------------------------------------------------------------------------------------
  
  public Response_ctype updateRow(XynaOrderServerExtension correlatedXynaOrder, UpdateRowRequest_ctype updateRowRequest_ctype) {
    // Implemented as code snippet!
    // return xmcp.dhcp.v4.netconfigurator.StaticHostImpl.updateRow(correlatedXynaOrder, updateRowRequest_ctype);

    // location: "DHCP SVN".dhcp.StaticHost.src.com.gip.www.juno.DHCP.WS.StaticHost

    // Input: updateRowRequest_ctype

    // P1: com.gip.www.juno.DHCP.WS.StaticHost.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = updateRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype updateRowInput = updateRowRequest_ctype.getUpdateRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype updateRowInput_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype(updateRowInput.getStaticHostID(), updateRowInput.getSubnetID(), updateRowInput.getSubnet(), updateRowInput.getCpe_mac(), updateRowInput.getRemoteId(), updateRowInput.getIp(), updateRowInput.getDns(), updateRowInput.getHostname(), updateRowInput.getDeployed1(), updateRowInput.getDeployed2(), updateRowInput.getDynamicDnsActive(), updateRowInput.getConfigDescr(), updateRowInput.getAssignedPoolID(), updateRowInput.getPool(), updateRowInput.getDesiredPoolType(), updateRowInput.getPoolType(), updateRowInput.getCmtsip());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.UpdateRowRequest_ctype updateRowRequest_ws = new com.gip.www.juno.DHCP.WS.StaticHost.Messages.UpdateRowRequest_ctype(inputHeader_ws, updateRowInput_ws);



    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl().updateRow(updateRowRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.Row_ctype updateRowOutput_ws;

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
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // Row_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype updateRowOutput = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Row_ctype();

    try {
    updateRowOutput.setStaticHostID(updateRowOutput_ws.getStaticHostID());
    updateRowOutput.setSubnetID(updateRowOutput_ws.getSubnetID());
    updateRowOutput.setSubnet(updateRowOutput_ws.getSubnet());
    updateRowOutput.setCpe_mac(updateRowOutput_ws.getCpe_mac());
    updateRowOutput.setRemoteId(updateRowOutput_ws.getRemoteId());
    updateRowOutput.setIp(updateRowOutput_ws.getIp());
    updateRowOutput.setDns(updateRowOutput_ws.getDns());
    updateRowOutput.setHostname(updateRowOutput_ws.getHostname());
    updateRowOutput.setDeployed1(updateRowOutput_ws.getDeployed1());
    updateRowOutput.setDeployed2(updateRowOutput_ws.getDeployed2());
    updateRowOutput.setDynamicDnsActive(updateRowOutput_ws.getDynamicDnsActive());
    updateRowOutput.setConfigDescr(updateRowOutput_ws.getConfigDescr());
    updateRowOutput.setAssignedPoolID(updateRowOutput_ws.getAssignedPoolID());
    updateRowOutput.setPool(updateRowOutput_ws.getPool());
    updateRowOutput.setDesiredPoolType(updateRowOutput_ws.getDesiredPoolType());
    updateRowOutput.setPoolType(updateRowOutput_ws.getPoolType());
    updateRowOutput.setCmtsip(updateRowOutput_ws.getCmtsip());
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Payload_ctype();
    payload.setUpdateRowOutput(updateRowOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.StaticHost.www.gip.com.juno.DHCP.WS.StaticHost.Messages.Response_ctype();
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

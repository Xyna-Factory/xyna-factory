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
import xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype;
import xmcp.dhcp.v4.control.Dhcpv4PacketsServiceOperation;


public class Dhcpv4PacketsServiceOperationImpl implements ExtendedDeploymentTask, Dhcpv4PacketsServiceOperation {

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

  public Response_ctype getMetaInfo(XynaOrderServerExtension correlatedXynaOrder, GetMetaInfoRequest_ctype getMetaInfoRequest_ctype) {
    // Implemented as code snippet!
    // input: getMetaInfoRequest_ctype

    // P1: com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages
    // P2: com.gip.www.juno.Gui.WS.Messages  


    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype(rh, null);
    }


    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype(inputHeader_ws, getMetaInfoRequest_ctype.getGetMetaInfoInput());


    //WS-Aufruf: 
    try {
        com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype response_ws = new com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Dhcpv4PacketsBindingImpl().getMetaInfo(getMetaInfoRequest_ws);

    // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
    com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] getMetaInfoOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

    try {
    getMetaInfoOutput_ws = payload_ws.getMetaInfoOutput();
    }
    catch(NullPointerException e) {
    getMetaInfoOutput_ws = null; 
    }



    // ab hier mapping

    // Response Header
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

    try {
    for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
    errorParameter.setId(param.getId());
    errorParameter.setValue(param.getValue());
    parameter.add(errorParameter);
    }
    }
    catch(NullPointerException e) {} // do nothing
    parameterList.setParameter(parameter);

    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
    responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



    //payload 
    // MetaRowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages genommen!
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype MetaInfoOutput = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype();
    java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype> MetaInfo = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype>();

    // ...MetaInfoRow_ctype - heißt die Klasse dieses mal! 

    try {
    for(com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype output: getMetaInfoOutput_ws) {
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype row = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype();
    row.setVisible(output.isVisible());
    row.setUpdates(output.isUpdates());
    row.setGuiname(output.getGuiname());
    row.setColname(output.getColname());
    row.setColnum(output.getColnum().intValue());
    row.setChildtable(output.getChildtable());
    row.setParenttable(output.getParenttable());
    row.setParentcol(output.getParentcol());
    row.setInputType(output.getInputType());
    row.setOptional(output.getOptional());
    MetaInfo.add(row);
    }
    MetaInfoOutput.setCol(MetaInfo);
    }
    catch(NullPointerException e) {} // do nothing


    // payload main Attributes:
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype();
    payload.setMetaInfoOutput(MetaInfoOutput);



    // Return Objekt:
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype();
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
  
  // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  public Response_ctype searchRows(XynaOrderServerExtension correlatedXynaOrder, SearchRowsRequest_ctype searchRowsRequest_ctype) {
    // Implemented as code snippet!
    // input: searchRowsRequest_ctype

    // P1: com.gip.www.juno.Gui.WS.Messages.
    // P2: com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.

    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype(rh, null);
    }

    com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype(searchRowsInput.getHost(), searchRowsInput.getIp(), searchRowsInput.getInTime(), searchRowsInput.getDiscover(), searchRowsInput.getOffer());

    // übergabeparameter zur ws-funktion
    com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);

    try {
    com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype response_ws = new com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Dhcpv4PacketsBindingImpl().searchRows(searchRowsRequest_ws);

    //webservice Attribute extrahieren:
    com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
    com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype payload_ws = response_ws.getPayload();

    // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype[] getAllRowsOutput_ws;
        com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype[] searchRowsOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try{
            getAllRowsOutput_ws = payload_ws.getGetAllRowsOutput();
        }
        catch(NullPointerException e){
            getAllRowsOutput_ws = null;  //später abgefangen
        }

        try{
            searchRowsOutput_ws = payload_ws.getSearchRowsOutput();
        }
        catch(NullPointerException e){
            searchRowsOutput_ws = null;  //später abgefangen
        }

        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
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
        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.RowList_ctype searchRows = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype>();

        try{
        for(com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype output: searchRowsOutput_ws){
    xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype();
    row.setHost(output.getHost());
    row.setIp(output.getIp());
    row.setInTime(output.getInTime());
    row.setDiscover(output.getDiscover());
    row.setOffer(output.getOffer());
    sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);


       //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype();
        payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Dhcpv4Packets.www.gip.com.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;



    }
     catch(java.rmi.RemoteException e) {
    // TODO: implement
    e.printStackTrace();
    return null; 
     }

    // return response_ctype;
  }

}

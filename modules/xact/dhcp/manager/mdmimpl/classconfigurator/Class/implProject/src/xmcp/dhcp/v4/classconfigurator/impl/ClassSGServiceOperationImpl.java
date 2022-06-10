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
package xmcp.dhcp.v4.classconfigurator.impl;


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
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.DeleteRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.InsertRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.UpdateRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.GetAllRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype;
import xmcp.dhcp.v4.classconfigurator.ClassSGServiceOperation;


public class ClassSGServiceOperationImpl implements ExtendedDeploymentTask, ClassSGServiceOperation {

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
    //Xyna-Objekte: (getMetaInfoRequest_ctype)
    xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 deleteRowsInput = deleteRowsRequest_ctype.getDeleteRowsInput();

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype(rh, null);
    }

    //Mapping auf WebService-Objekte:
    com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype deleteRowsInput_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype(deleteRowsInput.getClassID(), deleteRowsInput.getName(), deleteRowsInput.getAttributes(), deleteRowsInput.getFixedAttributes(), deleteRowsInput.getConditional(), deleteRowsInput.getPriority());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Class.Messages.DeleteRowsRequest_ctype deleteRowsRequest_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.DeleteRowsRequest_ctype(inputHeader_ws, deleteRowsInput_ws);


    try{
        // Rückgabe des Webservices:
        com.gip.www.juno.DHCP.WS.Class.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Class.ClassBindingImpl().deleteRows(deleteRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Class.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        //nichts benötigt

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }
        

        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype();
        try{
            payload.setDeleteRowsOutput(payload_ws.getDeleteRowsOutput());
        }
        catch(NullPointerException e){}  //do nothing


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e){
        e.printStackTrace();
        return null;
    }
  }
  

  public Response_ctype getAllRows(XynaOrderServerExtension correlatedXynaOrder, GetAllRowsRequest_ctype getAllRowsRequest_ctype) {

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype(rh, null);
    }

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype(inputHeader_ws, getAllRowsRequest_ctype.getGetAllRowsInput());


    //WS-Aufruf: 
    try {
        com.gip.www.juno.DHCP.WS.Class.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Class.ClassBindingImpl().getAllRows(getAllRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Class.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype[] getAllRowsOutput_ws;

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
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
            xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
            errorParameter.setId(param.getId());
            errorParameter.setValue(param.getValue());
            parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //payload
        // RowList_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages genommen!   
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.RowList_ctype1 getAllRowsOutput = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.RowList_ctype1();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1> getAllRowsList = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1>();

        // ...Row_ctype1 - heißt die Klasse dieses mal!

        try {
            for(com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype output: getAllRowsOutput_ws) {
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 row = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1();
                row.setClassID(output.getClassID());
                row.setName(output.getName());
                row.setAttributes(output.getAttributes());
                row.setFixedAttributes(output.getFixedAttributes());
                row.setConditional(output.getConditional());
                row.setPriority(output.getPriority());
                getAllRowsList.add(row);
            }
            getAllRowsOutput.setRow(getAllRowsList);
        }
        catch(NullPointerException e) {} // do nothing
        
        
        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype();
        payload.setGetAllRowsOutput(getAllRowsOutput);
        
        
        // Return Objekt:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype();
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

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype(rh, null);
    }

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype(inputHeader_ws, getMetaInfoRequest_ctype.getGetMetaInfoInput());

    try {
    com.gip.www.juno.DHCP.WS.Class.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Class.ClassBindingImpl().getMetaInfo(getMetaInfoRequest_ws);

        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Class.Messages.Payload_ctype payload_ws = response_ws.getPayload();
            
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
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws) {
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e) {} // do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());



        //payload
        // MetaInfo_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype metaInfo = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype> mi_col = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype output: metaInfoOutput_ws) {
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype row = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype();
                row.setVisible(output.isVisible());   // boolean ausgabe Variablen heißen immer is...! 
                row.setUpdates(output.isUpdates());
                row.setGuiname(output.getGuiname());
                row.setColname(output.getColname());
                row.setColnum(output.getColnum().intValue());
                row.setChildtable(output.getChildtable());
                row.setParenttable(output.getParenttable());
                row.setParentcol(output.getParentcol());
                row.setInputType(output.getInputType());
                row.setOptional(output.getOptional());
                mi_col.add(row);
            }
        }
        catch(NullPointerException e) {} // do nothing
        metaInfo.setCol(mi_col);



        // payload main Attributes:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype();
        payload.setMetaInfoOutput(metaInfo);


        //Return Objekt
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype();
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
    
    //Xyna-Objekte: (getMetaInfoRequest_ctype)
    xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 insertRowInput = insertRowRequest_ctype.getInsertRowInput();

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype(rh, null);
    }

    //Mapping auf WebService-Objekte:
    com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype insertRowInput_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype(insertRowInput.getClassID(), insertRowInput.getName(), insertRowInput.getAttributes(), insertRowInput.getFixedAttributes(), insertRowInput.getConditional(), insertRowInput.getPriority());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Class.Messages.InsertRowRequest_ctype insertRowRequest_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.InsertRowRequest_ctype(inputHeader_ws, insertRowInput_ws);


    try{
        // Rückgabe des Webservices:
        com.gip.www.juno.DHCP.WS.Class.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Class.ClassBindingImpl().insertRow(insertRowRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Class.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype insertRowOutput_ws;

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }

        try{
            insertRowOutput_ws = payload_ws.getInsertRowOutput();
        }
        catch(NullPointerException e){
            insertRowOutput_ws = null;  //später abgefangen
        }
        

        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //InsertRowOutput
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 insertRow = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1();
        try{
            insertRow.setClassID(insertRowOutput_ws.getClassID());
            insertRow.setName(insertRowOutput_ws.getName());
            insertRow.setAttributes(insertRowOutput_ws.getAttributes());
            insertRow.setFixedAttributes(insertRowOutput_ws.getFixedAttributes());
            insertRow.setConditional(insertRowOutput_ws.getConditional());
            insertRow.setPriority(insertRowOutput_ws.getPriority());
        }
        catch(NullPointerException e){} //do nothing
        

        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype();
        payload.setInsertRowOutput(insertRow);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e){
        e.printStackTrace();
        return null;
    }
  }
  

  public Response_ctype searchRows(XynaOrderServerExtension correlatedXynaOrder, SearchRowsRequest_ctype searchRowsRequest_ctype) {
    
    //Xyna-Objekte:
    xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype(rh, null);
    }

    //Mapping auf WebService-Objekte:
    com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype(searchRowsInput.getClassID(), searchRowsInput.getName(), searchRowsInput.getAttributes(), searchRowsInput.getFixedAttributes(), searchRowsInput.getConditional(), searchRowsInput.getPriority());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Class.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);

    try{
        //Rückgabe des Webservices:
        com.gip.www.juno.DHCP.WS.Class.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Class.ClassBindingImpl().searchRows(searchRowsRequest_ws);

        //WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Class.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        //Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype[] searchRowsOutput_ws;

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

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //SearchRowsOutput
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.RowList_ctype1 searchRows = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.RowList_ctype1();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1>();

        try{
            for(com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype output: searchRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 row = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1();
                row.setClassID(output.getClassID());
                row.setName(output.getName());
                row.setAttributes(output.getAttributes());
                row.setFixedAttributes(output.getFixedAttributes());
                row.setConditional(output.getConditional());
                row.setPriority(output.getPriority());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);


        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype();
        payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;
        
    }
    catch(java.rmi.RemoteException e){
        e.printStackTrace();
        return null;
    }
  }
  

  public Response_ctype updateRow(XynaOrderServerExtension correlatedXynaOrder, UpdateRowRequest_ctype updateRowRequest_ctype) {
    
    //Xyna-Objekte: (getMetaInfoRequest_ctype)
    xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 updateRowInput = updateRowRequest_ctype.getUpdateRowInput();

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype(rh, null);
    }

    //Mapping auf WebService-Objekte:
    com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype updateRowInput_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype(updateRowInput.getClassID(), updateRowInput.getName(), updateRowInput.getAttributes(), updateRowInput.getFixedAttributes(), updateRowInput.getConditional(), updateRowInput.getPriority());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.DHCP.WS.Class.Messages.UpdateRowRequest_ctype updateRowRequest_ws = new com.gip.www.juno.DHCP.WS.Class.Messages.UpdateRowRequest_ctype(inputHeader_ws, updateRowInput_ws);


    try{
        // Rückgabe des Webservices:
        com.gip.www.juno.DHCP.WS.Class.Messages.Response_ctype response_ws = new com.gip.www.juno.DHCP.WS.Class.ClassBindingImpl().updateRow(updateRowRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.DHCP.WS.Class.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype updateRowOutput_ws;

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

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //UpdateRowOutput
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1 updateRow = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Row_ctype1();
        try{
            updateRow.setClassID(updateRowOutput_ws.getClassID());
            updateRow.setName(updateRowOutput_ws.getName());
            updateRow.setAttributes(updateRowOutput_ws.getAttributes());
            updateRow.setFixedAttributes(updateRowOutput_ws.getFixedAttributes());
            updateRow.setConditional(updateRowOutput_ws.getConditional());
            updateRow.setPriority(updateRowOutput_ws.getPriority());
        }
        catch(NullPointerException e){} //do nothing
        

        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Payload_ctype();
        payload.setUpdateRowOutput(updateRow);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Class0.www.gip.com.juno.DHCP.WS.Class0.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }
    catch(java.rmi.RemoteException e){
        e.printStackTrace();
        return null;
    }
  }

}

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
package xmcp.dhcp.v4.leasebrowser.impl;


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
import xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.CountRowsWithConditionRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.InsertRowRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype;
import xmcp.dhcp.v4.leasebrowser.LeasesServiceOperation;


public class LeasesServiceOperationImpl implements ExtendedDeploymentTask, LeasesServiceOperation {

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

  public Response_ctype countRowsWithCondition(XynaOrderServerExtension correlatedXynaOrder, CountRowsWithConditionRequest_ctype countRowsWithConditionRequest_ctype) {
    //  Xyna Objekte: countRowsWithConditionRequest_ctype
    // xyna -> WS Datentypen + session Registration

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = countRowsWithConditionRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.CountRowsWithConditionInput_ctype countRowsWithConditionRequestInput = countRowsWithConditionRequest_ctype.getCountRowsWithConditionInput();
    xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype row_CR = countRowsWithConditionRequest_ctype.getCountRowsWithConditionInput().getRow();

    // Mapping auf Webservice Objekte: 
    // WS Pfade sind ab juno ...Gui.WS.Messages und ...Audit.WS.Leases.Messages
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype row_CR_ws =  new com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype(row_CR.getHost(), row_CR.getIp(), row_CR.getIpNum(), row_CR.getStartTime(), row_CR.getEndTime(), row_CR.getType(), row_CR.getRemoteId(), row_CR.getDppInstance());
    com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionInput_ctype countRowsWithConditionInput_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionInput_ctype(countRowsWithConditionRequestInput.getTable(), row_CR_ws);

    // Objekte zur Weitergabe des Webservices:
    com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionRequest_ctype(inputHeader_ws, countRowsWithConditionInput_ws);

    // Aufruf Webservice
    try {
        //Rückgabe des Webservices:
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype response_ws = new com.gip.www.juno.Audit.WS.Leases.LeasesBindingImpl().countRowsWithCondition(countRowsWithConditionRequest_ws);
        
        //generieren der ws Datentypen für Xyna
        //payload liegt in Audit, response Header in Gui! 
        // WebService Attribute:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Audit.WS.Leases.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;
         
        //Payload:
        //com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype[] getAllRowsOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype[] searchRowsOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype updateRowOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype insertRowOutput_ws;

        try{ 
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e) { 
            parameterList_ws = null;   //später abgefangen
        }


        // ab hier mapping 

        // Response Header...

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try { 
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype();
        try{
            payload.setCountRowsWithConditionOutput(payload_ws.getCountRowsWithConditionOutput());
        }
        catch(NullPointerException e){} //do nothing

          
        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype();
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
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype(rh, null);
    }


    //Xyna-Objekte: (getMetaInfoRequest_ctype)
    // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = getMetaInfoRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype(inputHeader_ws, getMetaInfoRequest_ctype.getGetMetaInfoInput());


    try{
        // Rückgabe des Webservers:
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype  response_ws = new com.gip.www.juno.Audit.WS.Leases.LeasesBindingImpl().getMetaInfo(metaInfoRequest_ws);
        
        // WebService Attribute:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Audit.WS.Leases.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype[] getAllRowsOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype[] searchRowsOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype updateRowOutput_ws;
        //com.gip.www.juno.DHCP.WS.Pooltype.Messages.Row_ctype insertRowOutput_ws;

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

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());

        //Payload...

        //MetaInfoOutput
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype metaInfo = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype> mi_col = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype output: metaInfoOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype row = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype();
                row.setVisible(output.isVisible());
                row.setUpdates(output.isUpdates());
                row.setGuiname(output.getGuiname());
                row.setColname(output.getColname());
                row.setColnum(output.getColnum().intValue());  //ursprünglich BigInteger
                row.setChildtable(output.getChildtable());
                row.setParenttable(output.getParenttable());
                row.setParentcol(output.getParentcol());
                row.setInputType(output.getInputType());
                row.setOptional(output.getOptional());
                mi_col.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        metaInfo.setCol(mi_col);

        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype();
        payload.setMetaInfoOutput(metaInfo);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype();
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
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype(rh, null);
    }


    //Xyna-Objekte: (getMetaInfoRequest_ctype)
    // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = insertRowRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype insertRowInput = insertRowRequest_ctype.getInsertRowInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype insertRowInput_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype(insertRowInput.getHost(), insertRowInput.getIp(), insertRowInput.getIpNum(), insertRowInput.getStartTime(), insertRowInput.getEndTime(), insertRowInput.getType(), insertRowInput.getRemoteId(), insertRowInput.getDppInstance());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Audit.WS.Leases.Messages.InsertRowRequest_ctype insertRowRequest_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.InsertRowRequest_ctype(inputHeader_ws, insertRowInput_ws);


    try{
        // Rückgabe des Webservices:
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype response_ws = new com.gip.www.juno.Audit.WS.Leases.LeasesBindingImpl().insertRow(insertRowRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Audit.WS.Leases.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        //com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput_ws;
        //com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype[] getAllRowsOutput_ws;
        //com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype[] searchRowsOutput_ws;
        //com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype updateRowOutput_ws;
        com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype insertRowOutput_ws;
        //com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype updateRowPkOutput_ws;
        //java.lang.String[] locationsList_ws;
        //java.lang.String[] colValuesDistinct_ws;

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

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //InsertRowOutput (TODO)
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype insertRow = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype();
        try{
            insertRow.setHost(insertRowOutput_ws.getHost());
            insertRow.setIp(insertRowOutput_ws.getIp());
            insertRow.setIpNum(insertRowOutput_ws.getIpNum());
            insertRow.setStartTime(insertRowOutput_ws.getStartTime());
            insertRow.setEndTime(insertRowOutput_ws.getEndTime());
            insertRow.setType(insertRowOutput_ws.getType());
            insertRow.setRemoteId(insertRowOutput_ws.getRemoteId());
            insertRow.setDppInstance(insertRowOutput_ws.getDppInstance());
        }
        catch(NullPointerException e){} //do nothing
        

        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype();
        payload.setInsertRowOutput(insertRow);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype();
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
    
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype(rh, null);
    }


    //Xyna-Objekte: searchRowsRequest_ctype
    // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = searchRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.SearchRowsInput_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();
    xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype row_SR = searchRowsRequest_ctype.getSearchRowsInput().getRow();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype row_SR_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype(row_SR.getHost(), row_SR.getIp(), row_SR.getIpNum(), row_SR.getStartTime(), row_SR.getEndTime(), row_SR.getType(), row_SR.getRemoteId(), row_SR.getDppInstance());
    com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsInput_ctype searchRowsInput_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsInput_ctype(searchRowsInput.getTable(), row_SR_ws);

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);


    try{
        // Rückgabe des Webservices: (TODO)
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype response_ws = new com.gip.www.juno.Audit.WS.Leases.LeasesBindingImpl().searchRows(searchRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Audit.WS.Leases.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = is expected to be found for xyna
            
        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype[] getAllRowsOutput_ws;
        com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype[] searchRowsOutput_ws;

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
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //Payload...

        //GetAllRowsOutput
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.RowList_ctype getAllRows = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype> gar_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype output: getAllRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype();
                row.setHost(output.getHost());
                row.setIp(output.getIp());
                row.setIpNum(output.getIpNum());
                row.setStartTime(output.getStartTime());
                row.setEndTime(output.getEndTime());
                row.setType(output.getType());
                row.setRemoteId(output.getRemoteId());
                row.setDppInstance(output.getDppInstance());
                gar_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        getAllRows.setRow(gar_row);

        //SearchRowsOutput
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.RowList_ctype searchRows = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.Audit.WS.Leases.Messages.Row_ctype output: searchRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Row_ctype();
                row.setHost(output.getHost());
                row.setIp(output.getIp());
                row.setIpNum(output.getIpNum());
                row.setStartTime(output.getStartTime());
                row.setEndTime(output.getEndTime());
                row.setType(output.getType());
                row.setRemoteId(output.getRemoteId());
                row.setDppInstance(output.getDppInstance());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);


        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Payload_ctype();
        payload.setGetAllRowsOutput(getAllRows);
        // payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.Leases.www.gip.com.juno.Audit.WS.Leases.Messages.Response_ctype();
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

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
package xmcp.dhcp.v4.deploymentmonitor.impl;


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
import xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.CountAllRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype;
import xmcp.dhcp.v4.deploymentmonitor.DeployActionsServiceOperation;


public class DeployActionsServiceOperationImpl implements ExtendedDeploymentTask, DeployActionsServiceOperation {

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

  public Response_ctype countAllRowsOutput(XynaOrderServerExtension correlatedXynaOrder, CountAllRowsRequest_ctype countAllRowsRequest_ctype) {

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype(rh, null);
    }

    // xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = countAllRowsRequest_ctype.getInputHeader();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountAllRowsRequest_ctype countAllRowsRequest_ws = new com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountAllRowsRequest_ctype(inputHeader_ws, countAllRowsRequest_ctype.getCountAllRowsInput());

    try {
            // Rückgabe des Webservices: (TODO)
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype response_ws = new com.gip.www.juno.Deployments.WS.DeployActions.DeployActionsBindingImpl().countAllRows(countAllRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload: nicht notwendig

        try{
            parameterList_ws = responseHeader_ws.getParameterList();
        }
        catch(NullPointerException e){
            parameterList_ws = null;  //später abgefangen
        }


        //ab hier: mapping

        //Response Header...

        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());

        // get Payload main Attribute:
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Payload_ctype();
        
        try {
            payload.setCountAllRowsOutput(payload_ws.getCountAllRowsOutput());
            }
        catch(NullPointerException e) {} // do nothing

        // return objekt:

        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype();
        response.setResponseHeader(responseHeader);
        response.setPayload(payload);
        return response;

    }

    catch(java.rmi.RemoteException e){
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
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype(rh, null);
    }

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest_ws = new com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype(inputHeader_ws, getMetaInfoRequest_ctype.getGetMetaInfoInput());


    try {
        // GetMetaInfo Array zur Weitergabe für den Output:
        com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] mi_col_Output_ws;

        mi_col_Output_ws = new com.gip.www.juno.Deployments.WS.DeployActions.DeployActionsBindingReal().getMetaInfo(getMetaInfoRequest_ws);

     

        // ab hier mapping

        //Response Header...
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setDescription("Ok");


        //payload
        // MetaInfo_ctype wurde aus xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages genommen!
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype metaInfo = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.MetaInfo_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype> mi_col = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype>();

        try {
            for(com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype output: mi_col_Output_ws) {
                xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype row = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.MetaInfoRow_ctype();
                row.setGuiname(output.getGuiname());   // boolean ausgabe Variablen heißen immer is...! 
                row.setColname(output.getColname());
                row.setVisible(true); // by dm
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
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Payload_ctype();
        payload.setMetaInfoOutput(metaInfo);


        //Return Objekt
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype();
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

    //SessionID holen:
    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();
    try{
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e){
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype(rh, null);
    }


    // xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader = searchRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Row_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    //Mapping auf WebService-Objekte:
    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(inputHeader.getUsername(), inputHeader.getPassword());
    com.gip.www.juno.Deployments.WS.DeployActions.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.Deployments.WS.DeployActions.Messages.Row_ctype(searchRowsInput.getId(), searchRowsInput.getDeploy_Time(), searchRowsInput.getService(), searchRowsInput.getUser(), searchRowsInput.getLog(), searchRowsInput.getTarget());

    //Objekt zur Weitergabe an den WebService:
    com.gip.www.juno.Deployments.WS.DeployActions.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.Deployments.WS.DeployActions.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);

    try {
        // Rückgabe des Webservices:
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype response_ws = new com.gip.www.juno.Deployments.WS.DeployActions.DeployActionsBindingImpl().searchRows(searchRowsRequest_ws);
        
        // WebService-Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Row_ctype[] getAllRowsOutput_ws;

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
        
        
        //ab hier: mapping

        //Response Header:
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        responseHeader.setErrorDomain(responseHeader_ws.getErrorDomain());
        responseHeader.setErrorNumber(responseHeader_ws.getErrorNumber());
        responseHeader.setSeverity(responseHeader_ws.getSeverity());
        responseHeader.setDescription(responseHeader_ws.getDescription());
        responseHeader.setStacktrace(responseHeader_ws.getStacktrace());
        responseHeader.setParameterList(parameterList);
        responseHeader.setStatus(responseHeader_ws.getStatus());


        //GetAllRowsOutput
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.RowList_ctype getAllRows = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.Deployments.WS.DeployActions.Messages.Row_ctype output: getAllRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Row_ctype();
                row.setId(output.getId());
                row.setDeploy_Time(output.getDeploy_Time());
                row.setService(output.getService());
                row.setUser(output.getUser());
                row.setLog(output.getLog());
                row.setTarget(output.getTarget());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        getAllRows.setRow(sr_row);
        
        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Payload_ctype();
        payload.setGetAllRowsOutput(getAllRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.DeployActions.www.gip.com.juno.Deployments.WS.DeployActions.Messages.Response_ctype();
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

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
import xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Response_ctype;
import xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.SearchRowsRequest_ctype;
import xmcp.dhcp.v4.control.PoolUsageServiceOperation;


public class PoolUsageServiceOperationImpl implements ExtendedDeploymentTask, PoolUsageServiceOperation {

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

  public Response_ctype searchRows(XynaOrderServerExtension correlatedXynaOrder, SearchRowsRequest_ctype searchRowsRequest_ctype) {

    com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype();

    try {
        xfmg.xopctrl.XynaUserSession session = xfmg.xopctrl.SessionManagement.getCurrentXynaUserSession(correlatedXynaOrder);
        inputHeader_ws.setUsername(session.getSessionID());
    }
    catch(xfmg.xopctrl.CouldNotAccessSessionException e) {
        xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype rh = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
        rh.setDescription("Could not access session");
        return new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Response_ctype(rh, null);
    }

    //xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader= searchRowsRequest_ctype.getInputHeader();
    xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype searchRowsInput = searchRowsRequest_ctype.getSearchRowsInput();

    // com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader_ws = new com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype(); // inputHeader.getUsername(), inputHeader.getPassword()

    com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype searchRowsInput_ws = new com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype(searchRowsInput.getPoolID(), searchRowsInput.getSharedNetworkID(), searchRowsInput.getPoolTypeID(), searchRowsInput.getStandortGruppeID(), searchRowsInput.getSize(), searchRowsInput.getUsed(), searchRowsInput.getUsedFraction());

    // übergabeparameter zur ws-funktion

    com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.SearchRowsRequest_ctype searchRowsRequest_ws = new com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.SearchRowsRequest_ctype(inputHeader_ws, searchRowsInput_ws);

    try {
        com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Response_ctype response_ws = new com.gip.www.juno.SnmpTrap.WS.PoolUsage.PoolUsageBindingImpl().searchRows(searchRowsRequest_ws);

        //webservice Attribute extrahieren:
        com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader_ws = response_ws.getResponseHeader();
        com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Payload_ctype payload_ws = response_ws.getPayload();

        // Response Header:
        com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList_ws;

        //Payload:
        com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype[] getAllRowsOutput_ws;
        com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype[] searchRowsOutput_ws;

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

        xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype parameterList = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ErrorParameterList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype> parameter = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype>();

        try{
            for(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype param: parameterList_ws){
                xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype errorParameter = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ErrorParameter_ctype();
                errorParameter.setId(param.getId());
                errorParameter.setValue(param.getValue());
                parameter.add(errorParameter);
            }
        }
        catch(NullPointerException e){} //do nothing
        parameterList.setParameter(parameter);

        xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.Gui.WS.Messages.ResponseHeader_ctype();
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
        xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.RowList_ctype searchRows = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.RowList_ctype();
        java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype> sr_row = new java.util.ArrayList<xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype>();

        try{
            for(com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype output: searchRowsOutput_ws){
                xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype row = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Row_ctype();
                row.setPoolID(output.getPoolID());
                row.setSharedNetworkID(output.getSharedNetworkID());
                row.setPoolTypeID(output.getPoolTypeID());
                row.setStandortGruppeID(output.getStandortGruppeID());
                row.setSize(output.getSize());
                row.setUsed(output.getUsed());
                row.setUsedFraction(output.getUsedFraction());
                sr_row.add(row);
            }
        }
        catch(NullPointerException e){} //do nothing
        searchRows.setRow(sr_row);


        //Payload-main-attributes
        xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Payload_ctype payload = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Payload_ctype();
        payload.setSearchRowsOutput(searchRows);


        //return-Objekt:
        xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Response_ctype response = new xmcp.dhcp.v4.datatypes.generated.PoolUsage.www.gip.com.juno.SnmpTrap.WS.PoolUsage.Messages.Response_ctype();
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

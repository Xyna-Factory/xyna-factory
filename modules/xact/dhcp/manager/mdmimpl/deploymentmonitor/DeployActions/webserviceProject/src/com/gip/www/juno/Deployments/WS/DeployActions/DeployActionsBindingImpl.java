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

package com.gip.www.juno.Deployments.WS.DeployActions;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype;
import com.gip.www.juno.Deployments.WS.DeployActions.Messages.*;


public class DeployActionsBindingImpl implements com.gip.www.juno.Deployments.WS.DeployActions.DeployActions_PortType{
  
  
   public com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException {
        return null;
    }

  private Response_ctype createResponse(Payload_ctype payload) {
    Response_ctype ret=new Response_ctype();
    ret.setResponseHeader(new ResponseHeader_ctype());
    ret.getResponseHeader().setDescription("Ok");
    ret.setPayload(payload);
    return ret;
  }
  
    private static Response_ctype createResponse(String classification, Throwable t) {
    ResponseHeader_ctype header = new ResponseHeader_ctype();
    String errorDescription = "";
    if (t instanceof DPPWebserviceModificationCollisionException) {
      header.setParameterList(convertDPPCollisionToErrorParameter_ctype((DPPWebserviceModificationCollisionException)t));
      errorDescription = t.getMessage();
    } else if (t instanceof DPPWebserviceException) {
      errorDescription = t.getMessage();
    } else {
      errorDescription = new MessageBuilder().setDescription(classification).setCause(t).build();
    }
    Response_ctype ret = new Response_ctype();
    ret.setResponseHeader(header);
    ret.getResponseHeader().setDescription(errorDescription);
    return ret;
  }


  
  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setGetAllRowsOutput(new DeployActionsBindingReal().getAllRows(getAllRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype searchRows(com.gip.www.juno.Deployments.WS.DeployActions.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setGetAllRowsOutput(new DeployActionsBindingReal().searchRows(searchRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  /*
  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype updateRow(UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUpdateRowOutput(new DeployActionsBindingReal().updateRow(updateRowRequest));      
      return createResponse(payload);
    } catch (Throwable t){
      return createResponse("Error",t);
    }
  }
  */
 
  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype insertRow(com.gip.www.juno.Deployments.WS.DeployActions.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setInsertRowOutput(new DeployActionsBindingReal().insertRow(insertRowRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype deleteRows(com.gip.www.juno.Deployments.WS.DeployActions.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeleteRowsOutput(new DeployActionsBindingReal().deleteRows(deleteRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }  
  

  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype updateRowPk(com.gip.www.juno.Deployments.WS.DeployActions.Messages.UpdateRowPkRequest_ctype updateRowPkRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUpdateRowOutput(new DeployActionsBindingReal().updateRowPk(updateRowPkRequest));      
      return createResponse(payload);
    } catch (Throwable t){
      return createResponse("Error",t);
    }
  }

  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCountRowsWithConditionOutput(new DeployActionsBindingReal().countRowsWithCondition(countRowsWithConditionRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  
  public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype countAllRows(com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCountAllRowsOutput(new DeployActionsBindingReal().countAllRows(countAllRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }


  private static ErrorParameter_ctype[] convertDPPCollisionToErrorParameter_ctype(DPPWebserviceModificationCollisionException e) {
    Set<Entry<String, String>> errorParams = e.getErrorParameter();
    ErrorParameter_ctype[] params = new ErrorParameter_ctype[errorParams.size()];
    Iterator<Entry<String, String>> iterator = errorParams.iterator();
    for (int i = 0; i < errorParams.size(); i++) {
      Entry<String, String> entry = iterator.next();
      params[i] = new ErrorParameter_ctype(entry.getKey(), entry.getValue());
    }
    return params;
  }
}


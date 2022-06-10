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

package com.gip.www.juno.Audit.WS.Leases;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.exceptions.DPPWebserviceException;

import com.gip.juno.ws.exceptions.MessageBuilder;

import com.gip.www.juno.Gui.WS.Messages.*;

import com.gip.www.juno.Audit.WS.Leases.Messages.*;

public class LeasesBindingImpl implements com.gip.www.juno.Audit.WS.Leases.Leases_PortType{
   
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




  public  com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setMetaInfoOutput(new LeasesBindingReal().getMetaInfo(getMetaInfoRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setGetAllRowsOutput(new LeasesBindingReal().getAllRows(getAllRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype searchRows(com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setGetAllRowsOutput(new LeasesBindingReal().searchRows(searchRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype updateRow(com.gip.www.juno.Audit.WS.Leases.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUpdateRowOutput(new LeasesBindingReal().updateRow(updateRowRequest));      
      return createResponse(payload);
    } catch (Throwable t){
      return createResponse("Error",t);
    }
  }
 
  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype insertRow(com.gip.www.juno.Audit.WS.Leases.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setInsertRowOutput(new LeasesBindingReal().insertRow(insertRowRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype deleteRows(com.gip.www.juno.Audit.WS.Leases.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeleteRowsOutput(new LeasesBindingReal().deleteRows(deleteRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }  
  
  public  com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype searchLeases(com.gip.www.juno.Audit.WS.Leases.Messages.SearchLeasesRequest_ctype searchLeasesRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setSearchRowsOutput(new LeasesBindingReal().searchLeases(searchLeasesRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }

  }

  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCountRowsWithConditionOutput(new LeasesBindingReal().countRowsWithCondition(countRowsWithConditionRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  
  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype countAllRows(com.gip.www.juno.Audit.WS.Leases.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCountAllRowsOutput(new LeasesBindingReal().countAllRows(countAllRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype countLeases(com.gip.www.juno.Audit.WS.Leases.Messages.CountLeasesRequest_ctype countLeasesRequest) throws java.rmi.RemoteException {
        return null;
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

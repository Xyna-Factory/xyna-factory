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

package com.gip.www.juno.DHCP.WS.SharedNetwork;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingReal;
import com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Payload_ctype;
import com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype;
import com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.RowListOutput_ctype;

import com.gip.www.juno.Gui.WS.Messages.*;

import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;

public class SharedNetworkBindingImpl implements com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetwork_PortType{

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


  public com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setMetaInfoOutput(new SharedNetworkBindingReal().getMetaInfo(metaInfoRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }


  public com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setGetAllRowsOutput(new SharedNetworkBindingReal().getAllRows(getAllRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype searchRows(com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setSearchRowsOutput(new SharedNetworkBindingReal().searchRows(searchRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype updateRow(com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setUpdateRowOutput(new SharedNetworkBindingReal().updateRow(updateRowRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype insertRow(com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setInsertRowOutput(new SharedNetworkBindingReal().insertRow(insertRowRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.Response_ctype deleteRows(com.gip.www.juno.DHCP.WS.SharedNetwork.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setDeleteRowsOutput(new SharedNetworkBindingReal().deleteRows(deleteRowsRequest));
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


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

package com.gip.www.juno.DHCP.WS.ConnectData;

import com.gip.www.juno.DHCP.WS.ConnectData.ConnectDataBindingReal;
import com.gip.www.juno.DHCP.WS.ConnectData.Messages.Payload_ctype;
import com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype;
import com.gip.www.juno.DHCP.WS.ConnectData.Messages.RowListOutput_ctype;

import com.gip.www.juno.Gui.WS.Messages.*;

import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;

public class ConnectDataBindingImpl implements com.gip.www.juno.DHCP.WS.ConnectData.ConnectData_PortType{

 private Response_ctype createResponse(Payload_ctype payload) {
    Response_ctype ret=new Response_ctype();
    ret.setResponseHeader(new ResponseHeader_ctype());
    ret.getResponseHeader().setDescription("Ok");
    ret.setPayload(payload);
    return ret;
  }
  
  private Response_ctype createResponse(String ConnectDataification, Throwable t) {
    String errorDescription="";
    try{
      throw t;
    } catch (DPPWebserviceException e2){
      errorDescription=e2.getMessage();
    } catch (Throwable e2){
      errorDescription=new MessageBuilder().setDescription(ConnectDataification).setCause(e2).build();
    }    
    Response_ctype ret=new Response_ctype();
    ret.setResponseHeader(new ResponseHeader_ctype());
    ret.getResponseHeader().setDescription(errorDescription);
    return ret;
  }

  public com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setMetaInfoOutput(new ConnectDataBindingReal().getMetaInfo(metaInfoRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setGetAllRowsOutput(new ConnectDataBindingReal().getAllRows(getAllRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype searchRows(com.gip.www.juno.DHCP.WS.ConnectData.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setSearchRowsOutput(new ConnectDataBindingReal().searchRows(searchRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype updateRow(com.gip.www.juno.DHCP.WS.ConnectData.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setUpdateRowOutput(new ConnectDataBindingReal().updateRow(updateRowRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype insertRow(com.gip.www.juno.DHCP.WS.ConnectData.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setInsertRowOutput(new ConnectDataBindingReal().insertRow(insertRowRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.ConnectData.Messages.Response_ctype deleteRows(com.gip.www.juno.DHCP.WS.ConnectData.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setDeleteRowsOutput(new ConnectDataBindingReal().deleteRows(deleteRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }


}


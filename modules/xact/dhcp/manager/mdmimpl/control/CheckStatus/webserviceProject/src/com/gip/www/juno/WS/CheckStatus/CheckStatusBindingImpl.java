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

package com.gip.www.juno.WS.CheckStatus;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import org.apache.log4j.Logger;

import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.www.juno.Gui.WS.Messages.Payload_ctype;
import com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype;
import com.gip.www.juno.Gui.WS.Messages.Response_ctype;

public class CheckStatusBindingImpl implements com.gip.www.juno.WS.CheckStatus.CheckStatus_PortType{

  private static Logger _logger = Logger.getLogger("CheckStatus");

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



  public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIp(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setCheckStatusForIpResponseOutput(new CheckStatusBindingReal().checkStatusForIp(checkStatusForIpRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }


  public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIpv6(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpv6Request) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setCheckStatusForIpResponseOutput(new CheckStatusBindingReal().checkStatusForIpv6(checkStatusForIpv6Request));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype getInstanceInfoList(com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype getInstanceInfoListRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGetInstanceInfoListResponseOutput(new CheckStatusBindingReal().getInstanceInfoList(getInstanceInfoListRequest));
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

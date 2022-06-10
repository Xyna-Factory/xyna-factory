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

package com.gip.www.juno.WS.ConfigFile;

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

public class ConfigFileBindingImpl implements com.gip.www.juno.WS.ConfigFile.ConfigFile_PortType{

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


  public com.gip.www.juno.Gui.WS.Messages.Response_ctype tlvToAscii(com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype tlvToAsciiRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setTlvToAsciiResponse(new ConfigFileBindingReal().tlvToAscii(tlvToAsciiRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
    catch (Throwable t)
    {
      return createResponse("error",t);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype generateAsciiFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForInitializedCableModemResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForInitializedCableModem(generateAsciiFromTemplateForInitializedCableModemRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype generateAsciiFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForUnregisteredCableModemResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForUnregisteredCableModem(generateAsciiFromTemplateForUnregisteredCableModemRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromString(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype generateAsciiFromStringRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromStringResponse(new ConfigFileBindingReal().generateAsciiFromString(generateAsciiFromStringRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
    }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromStringV4(
      com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype generateAsciiFromStringV4Request)
      throws java.rmi.RemoteException {
    try {
      Payload_ctype payload = new Payload_ctype();
      payload.setGenerateAsciiFromStringV4Response(new ConfigFileBindingReal()
          .generateAsciiFromStringV4(generateAsciiFromStringV4Request));
      return createResponse(payload);
    } catch (Exception e) {
      return createResponse("Error", e);
    }
  }

public com.gip.www.juno.Gui.WS.Messages.Response_ctype showPacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype showPacketsAsAsciiRequest) throws java.rmi.RemoteException {
  try{
    Payload_ctype payload=new Payload_ctype();
    payload.setShowPacketsAsAsciiResponse(new ConfigFileBindingReal().showPacketsAsAscii(showPacketsAsAsciiRequest));
    return createResponse(payload);
  } catch (Exception e){
    return createResponse("Error",e);
  }
    }

public com.gip.www.juno.Gui.WS.Messages.Response_ctype showV4PacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype showV4PacketsAsAsciiRequest) throws java.rmi.RemoteException {
  try{
    Payload_ctype payload=new Payload_ctype();
    payload.setShowV4PacketsAsAsciiResponse(new ConfigFileBindingReal().showV4PacketsAsAscii(showV4PacketsAsAsciiRequest));
    return createResponse(payload);
  } catch (Exception e){
    return createResponse("Error",e);
  }  
    }

public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype generateAsciiFromTemplateForSipMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForSipMtaResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForSipMta(generateAsciiFromTemplateForSipMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype generateAsciiFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForNcsMtaResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForNcsMta(generateAsciiFromTemplateForNcsMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype generateAsciiFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForIsdnMtaResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForIsdnMta(generateAsciiFromTemplateForIsdnMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype generateAsciiFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForUninitializedMtaResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForUninitializedMta(generateAsciiFromTemplateForUninitializedMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype generateAsciiFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateAsciiFromTemplateForUnregisteredMtaResponse(new ConfigFileBindingReal().generateAsciiFromTemplateForUnregisteredMta(generateAsciiFromTemplateForUnregisteredMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromString(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype generateTlvFromStringRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromStringResponse(new ConfigFileBindingReal().generateTlvFromString(generateTlvFromStringRequest));
      return createResponse(payload);
    } catch (Throwable e){
      logger.error("", e);
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype generateTlvFromStringV4Request) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromStringV4Response(new ConfigFileBindingReal().generateTlvFromStringV4(generateTlvFromStringV4Request));
      return createResponse(payload);
    } catch (Throwable e){
      logger.error("", e);
      return createResponse("Error",e);
    }
    }

public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype generateTlvFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForInitializedCableModemResponse(new ConfigFileBindingReal().generateTlvFromTemplateForInitializedCableModem(generateTlvFromTemplateForInitializedCableModemRequest));
      return createResponse(payload);
    } catch (Throwable e){
      logger.error("", e);
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype generateTlvFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForUnregisteredCableModemResponse(new ConfigFileBindingReal().generateTlvFromTemplateForUnregisteredCableModem(generateTlvFromTemplateForUnregisteredCableModemRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype generateTlvFromTemplateForSipMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForSipMtaResponse(new ConfigFileBindingReal().generateTlvFromTemplateForSipMta(generateTlvFromTemplateForSipMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype generateTlvFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForNcsMtaResponse(new ConfigFileBindingReal().generateTlvFromTemplateForNcsMta(generateTlvFromTemplateForNcsMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype generateTlvFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForIsdnMtaResponse(new ConfigFileBindingReal().generateTlvFromTemplateForIsdnMta(generateTlvFromTemplateForIsdnMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype generateTlvFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForUninitializedMtaResponse(new ConfigFileBindingReal().generateTlvFromTemplateForUninitializedMta(generateTlvFromTemplateForUninitializedMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype generateTlvFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();
      payload.setGenerateTlvFromTemplateForUnregisteredMtaResponse(new ConfigFileBindingReal().generateTlvFromTemplateForUnregisteredMta(generateTlvFromTemplateForUnregisteredMtaRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  private static Logger logger = Logger.getLogger("ConfigFile");


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

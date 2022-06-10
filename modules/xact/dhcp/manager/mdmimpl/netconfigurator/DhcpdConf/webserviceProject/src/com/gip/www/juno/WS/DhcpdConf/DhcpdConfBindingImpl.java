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

package com.gip.www.juno.WS.DhcpdConf;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.juno.ws.tools.migration.MigrationHelper;

import com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype;
import com.gip.www.juno.Gui.WS.Messages.DeployStaticHostInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype;
import com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype;
import com.gip.www.juno.Gui.WS.Messages.Payload_ctype;
import com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype;
import com.gip.www.juno.Gui.WS.Messages.Response_ctype;
import com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype;
import com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype;
import com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype;

public class DhcpdConfBindingImpl implements com.gip.www.juno.WS.DhcpdConf.DhcpdConf_PortType{
  
  private final static String PROPERTY_USEISC = "externalcalls.hosts.deployment.useiscformat";
  static Logger logger = Logger.getLogger("DhcpdConf");
  
  private Response_ctype createResponse(Payload_ctype payload) {
    Response_ctype ret=new Response_ctype();
    ret.setResponseHeader(new ResponseHeader_ctype());
    ret.getResponseHeader().setDescription("Ok");
    ret.setPayload(payload);
    return ret;
  }
  
  private static Response_ctype createResponse(String classification, Throwable t) {
    Response_ctype ret = new Response_ctype();
    ret.setResponseHeader(createResponseHeader(classification, t));
    return ret;
  }
  
  
  private static ResponseHeader_ctype createResponseHeader(String classification, Throwable t) {
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
    header.setDescription(errorDescription);
    return header;
  }


  public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConf(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCheckDhcpdConfResponse(new DhcpdConfBindingReal().checkDhcpdConf(checkDhcpdConfRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCheckDhcpdConfResponse(new DhcpdConfBindingReal().checkDhcpdConfNewFormat(checkDhcpdConfRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
    }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConf(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeployDhcpdConfResponse(new DhcpdConfBindingReal().deployDhcpdConf(deployDhcpdConfRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }


  public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeployDhcpdConfResponse(new DhcpdConfBindingReal().deployDhcpdConfNewFormat(deployDhcpdConfRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
    }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHost(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeployStaticHostResponse(new DhcpdConfBindingReal().deployStaticHost(deployStaticHostRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostNewFormatRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeployStaticHostResponse(new DhcpdConfBindingReal().deployStaticHostNewFormat(deployStaticHostNewFormatRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
    }

  public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHost(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUndeployStaticHostResponse(new DhcpdConfBindingReal().undeployStaticHost(undeployStaticHostRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostNewFormatRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUndeployStaticHostResponse(new DhcpdConfBindingReal().undeployStaticHostNewFormat(undeployStaticHostNewFormatRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
    }

  public com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype deployCPE(com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype deployCPERequest) throws java.rmi.RemoteException {
    try {
      String statichostid = ExternalCallHelper.lookupStaticHostIdByMacAndIp(deployCPERequest.getDeployCPEInput().getCpe_mac(),
                                                                            deployCPERequest.getDeployCPEInput().getIp());
      
      DeployStaticHostRequest_ctype request = new DeployStaticHostRequest_ctype(deployCPERequest.getInputHeader(), new DeployStaticHostInput_ctype(statichostid));
      Response_ctype response;
      if (useIsc()) {
        response = deployStaticHost(request);
      } else {
        response = deployStaticHostNewFormat(request);
      }      
      if (response.getResponseHeader().getDescription().equals("Ok")) {
        return new DeployCPEResponse_ctype(response.getResponseHeader(), response.getPayload().getDeployStaticHostResponse().getOutputContent());
      } else {
        return new DeployCPEResponse_ctype(response.getResponseHeader(), "Error");
      }
    } catch (Throwable t) {
      return new DeployCPEResponse_ctype(createResponseHeader("Error", t), "Error");
    }
  }

  public com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype undeployCPE(com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype undeployCPERequest) throws java.rmi.RemoteException {
    try {
      String statichostid = ExternalCallHelper.lookupStaticHostIdByMacAndIp(undeployCPERequest.getUndeployCPEInput().getCpe_mac(),
                                                                            undeployCPERequest.getUndeployCPEInput().getIp());
      UndeployStaticHostRequest_ctype request = new UndeployStaticHostRequest_ctype(undeployCPERequest.getInputHeader(), new UndeployStaticHostInput_ctype(statichostid, true));
      Response_ctype response;
      if (useIsc()) {
        response = undeployStaticHost(request);
      } else {
        response = undeployStaticHostNewFormat(request);
      }
      
      
      if (response.getResponseHeader().getDescription().equals("Ok")) {
        return new UndeployCPEResponse_ctype(response.getResponseHeader(), response.getPayload().getUndeployStaticHostResponse().getOutputContent());
      } else {
        return new UndeployCPEResponse_ctype(response.getResponseHeader(), "Error");
      }
    } catch (Throwable t) {
      return new UndeployCPEResponse_ctype(createResponseHeader("Error", t), "Error");
    }
  }

  
  public com.gip.www.juno.Gui.WS.Messages.Response_ctype duplicateForMigration(com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype duplicateForMigrationRequest)
                  throws java.rmi.RemoteException {
    try {
      String sourceIdentifier = duplicateForMigrationRequest.getSource().getUniqueIdentifier();
      String targetIdentifier = duplicateForMigrationRequest.getTarget().getUniqueIdentifier();

      MigrationObjectType type = MigrationObjectType.getMigrationObjectTypeFromWebServiceInput(duplicateForMigrationRequest.getSource().getTargetType());
      switch (type) {
        case sharednetwork :
          MigrationHelper.duplicateSharedNetwork(sourceIdentifier, targetIdentifier);
          break;
        case subnet :
          MigrationHelper.duplicateSubnet(sourceIdentifier, targetIdentifier);
          break;
        case pool :
          MigrationHelper.duplicatePool(sourceIdentifier, targetIdentifier);
          break;
        default :
          throw new DPPWebserviceException("MigrationRequest did not contain a valid type");
      }
      DhcpdConfResponse_ctype response = new DhcpdConfResponse_ctype();
      response.setOutputContent("Success");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      response.setOutputHeader(header);
      Payload_ctype payload = new Payload_ctype();
      payload.setDuplicateForMigrationResponse(response);
      return createResponse(payload);
    } catch (Throwable t) {
      Logger.getLogger("DHCPDCONF ERROR").error("ERROR",t);
      return createResponse("Error", t);
    }
  }

  
  public com.gip.www.juno.Gui.WS.Messages.Response_ctype deactivateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deactivateForMigrationRequest) throws java.rmi.RemoteException {
    try {
      String identifier = deactivateForMigrationRequest.getUniqueIdentifier();

      MigrationObjectType type = MigrationObjectType.getMigrationObjectTypeFromWebServiceInput(deactivateForMigrationRequest.getTargetType());
      switch (type) {
        case sharednetwork :
          MigrationHelper.deactivateSharedNetwork(identifier);
          break;
        case subnet :
          MigrationHelper.deactivateSubnet(identifier);
          break;
        case pool :
          MigrationHelper.deactivatePool(identifier);
          break;
        default :
          throw new DPPWebserviceException("MigrationRequest did not contain a valid type");
      }
      DhcpdConfResponse_ctype response = new DhcpdConfResponse_ctype();
      response.setOutputContent("Success");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      response.setOutputHeader(header);
      Payload_ctype payload = new Payload_ctype();
      payload.setDeactivateForMigrationResponse(response);
      return createResponse(payload);
    } catch (Throwable t) {
      Logger.getLogger("DHCPDCONF ERROR").error("ERROR",t);
      return createResponse("Error", t);
    }
  }

  
  public com.gip.www.juno.Gui.WS.Messages.Response_ctype activateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype activateForMigrationRequest) throws java.rmi.RemoteException {
    try {
      String identifier = activateForMigrationRequest.getUniqueIdentifier();

      MigrationObjectType type = MigrationObjectType.getMigrationObjectTypeFromWebServiceInput(activateForMigrationRequest.getTargetType());
      switch (type) {
        case sharednetwork :
          MigrationHelper.activateSharedNetwork(identifier);
          break;
        case subnet :
          MigrationHelper.activateSubnet(identifier);
          break;
        case pool :
          MigrationHelper.activatePool(identifier);
          break;
        default :
          throw new DPPWebserviceException("MigrationRequest did not contain a valid type");
      }
      DhcpdConfResponse_ctype response = new DhcpdConfResponse_ctype();
      response.setOutputContent("Success");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      response.setOutputHeader(header);
      Payload_ctype payload = new Payload_ctype();
      payload.setActivateForMigrationResponse(response);
      return createResponse(payload);
    } catch (Throwable t) {
      Logger.getLogger("DHCPDCONF ERROR").error("ERROR",t);
      return createResponse("Error", t);
    }
  }

  
  public com.gip.www.juno.Gui.WS.Messages.Response_ctype deleteForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deleteForMigrationRequest) throws java.rmi.RemoteException {
    try {
      String identifier = deleteForMigrationRequest.getUniqueIdentifier();

      MigrationObjectType type = MigrationObjectType.getMigrationObjectTypeFromWebServiceInput(deleteForMigrationRequest.getTargetType());
      switch (type) {
        case sharednetwork :
          MigrationHelper.deleteSharedNetwork(identifier);
          break;
        case subnet :
          MigrationHelper.deleteSubnet(identifier);
          break;
        case pool :
          MigrationHelper.deletePool(identifier);
          break;
        default :
          throw new DPPWebserviceException("MigrationRequest did not contain a valid type");
      }
      DhcpdConfResponse_ctype response = new DhcpdConfResponse_ctype();
      response.setOutputContent("Success");
      OutputHeaderContent_ctype header = new OutputHeaderContent_ctype();
      header.setException("none");
      header.setStatus("OK");
      response.setOutputHeader(header);
      Payload_ctype payload = new Payload_ctype();
      payload.setDeleteForMigrationResponse(response);
      return createResponse(payload);
    } catch (Throwable t) {
      Logger.getLogger("DHCPDCONF ERROR").error("ERROR",t);
      return createResponse("Error", t);
    }
  }
  
  private enum MigrationObjectType {
    pool, subnet, sharednetwork;
    
    public static MigrationObjectType getMigrationObjectTypeFromWebServiceInput(String wsInput) throws DPPWebserviceException {
      for (MigrationObjectType type : values()) {
        if (type.toString().equalsIgnoreCase(wsInput)) {
          return type;
        }
      }
      throw new DPPWebserviceException("MigrationRequest did not contain a valid type");
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
  
  
  private Boolean useIsc() {
    Boolean useIsc = Boolean.TRUE;
    try {
      Properties props = PropertiesHandler.getWsProperties();
      String useIscValue = PropertiesHandler.getProperty(props, PROPERTY_USEISC, logger);
      if (useIscValue != null && useIscValue.length() > 0) {
        useIsc = Boolean.valueOf(useIscValue);
      }
    } catch (Throwable t) {
      logger.warn("Could not read " + PROPERTY_USEISC + " from ws.properties, using isc deployment",t);
    }
    return useIsc;
  }
  
}

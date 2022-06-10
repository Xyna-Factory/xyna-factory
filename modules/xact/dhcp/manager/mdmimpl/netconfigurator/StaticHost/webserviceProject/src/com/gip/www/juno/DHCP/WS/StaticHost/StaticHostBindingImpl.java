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

package com.gip.www.juno.DHCP.WS.StaticHost;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import com.gip.www.juno.DHCP.WS.StaticHost.ExternalCallHelper.PoolData;
import com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingReal;
import com.gip.www.juno.DHCP.WS.StaticHost.Messages.*;

import com.gip.www.juno.Gui.WS.Messages.*;

import java.util.ArrayList;
import java.util.List;

import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.handler.*;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;

import org.apache.log4j.Logger;

public class StaticHostBindingImpl implements com.gip.www.juno.DHCP.WS.StaticHost.StaticHost_PortType{

  private static TableHandler _handler = StaticHostTools.getHandler();
  private static final Logger logger = Logger.getLogger("StaticHost");
  
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

  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setMetaInfoOutput(new StaticHostBindingReal().getMetaInfo(metaInfoRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setGetAllRowsOutput(new StaticHostBindingReal().getAllRows(getAllRowsRequest, true));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype getAllRowsWithSessionAuthentification(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsWithSessionAuthentificationRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setGetAllRowsOutput(new StaticHostBindingReal().getAllRows(getAllRowsWithSessionAuthentificationRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype searchRows(com.gip.www.juno.DHCP.WS.StaticHost.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setSearchRowsOutput(new StaticHostBindingReal().searchRows(searchRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype updateRow(com.gip.www.juno.DHCP.WS.StaticHost.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setUpdateRowOutput(new StaticHostBindingReal().updateRow(updateRowRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype insertRow(com.gip.www.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setInsertRowOutput(new StaticHostBindingReal().insertRow(insertRowRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype deleteRows(com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();   
      payload.setDeleteRowsOutput(new StaticHostBindingReal().deleteRows(deleteRowsRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  
  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.GetFreeReservedIPsResponse_ctype getFreeReservedIPs(com.gip.www.juno.DHCP.WS.StaticHost.Messages.GetFreeReservedIPsRequest_ctype getFreeReservedIPsRequest) throws java.rmi.RemoteException {
    logger.info("getFreeReservedIPs");
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(getFreeReservedIPsRequest.getInputHeader().getUsername(),
                                                   getFreeReservedIPsRequest.getInputHeader().getPassword(), "dhcp",
                                                   wsInvocationId, _handler);
      
      List<String> freeIps = new ArrayList<String>();
      int maxIps = ExternalCallHelper.getFreeIpMaxRows();
      ExternalCallHelper.LeasesData lease = ExternalCallHelper.findLeaseForRemoteId(getFreeReservedIPsRequest.getGetFreeReservedIPsInput().getCpe_mac());
      logger.info("lease: " + lease);
      if (lease == null) {
        throw new DPPWebserviceException(new MessageBuilder().setDescription("Could not find a lease containing the given Mac as remoteId, lookup not possible.")
                                         .setDomain("F").setErrorNumber("400").setSeverity("3"));
      }
      List<ExternalCallHelper.PoolData> allPools = ExternalCallHelper.retrieveAllPoolRangesWithCmtsReference();
      int[] ipParts = ExternalCallHelper.convertIpStringToIntParts(lease.ip);
      for (ExternalCallHelper.PoolData poolData : allPools) {
        logger.info("pooldata: " + poolData.toString());
        logger.info("lease.ip: " + lease.ip);
        if (poolData.contains(lease.ip, ipParts)) {
          logger.info("Is contained :D");
          List<ExternalCallHelper.PoolData> reservedPools = ExternalCallHelper.retrieveReservedPoolsForCmts(poolData.associatedCmtsId);
          for (ExternalCallHelper.PoolData reservedPool : reservedPools) {
            List<String> exclusions = ExternalCallHelper.retrieveStatichostIPsForPool(reservedPool.poolId);
            freeIps.addAll(ExternalCallHelper.generateFreeIpsFromPoolDefinition(reservedPool, exclusions, maxIps - freeIps.size()));
            if (freeIps.size() >= maxIps) {
              break;
            }
          }
          ResponseHeader_ctype header = new ResponseHeader_ctype();
          header.setDescription("Ok");
          return new GetFreeReservedIPsResponse_ctype(header, freeIps.toArray(new String[freeIps.size()]));
        }
      }
      throw new DPPWebserviceException(new MessageBuilder().setDescription("Could not find pool containing leased ip.")
                                       .setDomain("F").setErrorNumber("400").setSeverity("3"));
    } catch (Throwable t){
      return new GetFreeReservedIPsResponse_ctype(createResponseHeader("Error",t), new String[0]);
    }
  }

  
  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPEResponse_ctype setIPforCPE(com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPERequest_ctype setIPforCPERequest) throws java.rmi.RemoteException {
    try {
      String ip = setIPforCPERequest.getSetIPforCPEInput().getIp();
      // check if still contained in reserved ip pool
      PoolData poolForIp = ExternalCallHelper.findReservedPoolByIp(ip);
      if (poolForIp == null) {
        throw new DPPWebserviceException(new MessageBuilder().setDescription("No reserved pool containing the given ip could be found.")
                                         .setDomain("F").setErrorNumber("400").setSeverity("3"));
      }
      
      String cpePoolId = ExternalCallHelper.lookupCpePoolId();
      poolForIp = new PoolData(poolForIp.poolId, poolForIp.associatedCmtsId, poolForIp.subnetId, Integer.parseInt(cpePoolId), poolForIp.rangeStart, poolForIp.rangeStop);
      
      // convert to InsertRowRequest
      InsertRowRequest_ctype request = convertSetIpForCpeRequestToInsertRowRequest(setIPforCPERequest, poolForIp);
      // call insert row
      new StaticHostBindingReal().insertRow(request);
      
      ResponseHeader_ctype header = new ResponseHeader_ctype();
      header.setDescription("Ok");
      return new SetIPforCPEResponse_ctype(header, "Success");
    } catch (Throwable t) {
      return new SetIPforCPEResponse_ctype(createResponseHeader("Error",t), "Error");
    }
  }

  
  public com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteIPforCPEResponse_ctype deleteIPforCPE(com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteIPforCPERequest_ctype deleteIPforCPERequest) throws java.rmi.RemoteException {
    try {
      // lookup staticHostId by ip & mac
      Row_ctype searchRow = new Row_ctype("", "", "", deleteIPforCPERequest.getDeleteIPforCPEInput().getCpe_mac(),
                                          "", deleteIPforCPERequest.getDeleteIPforCPEInput().getIp(),
                                          "", "", "", "", "", "", "", "", "", "", "");
  
      SearchRowsRequest_ctype searchRequest = new SearchRowsRequest_ctype(deleteIPforCPERequest.getInputHeader(), searchRow);
      Response_ctype searchResponse = searchRows(searchRequest);
      
      Row_ctype[] searchResult = searchResponse.getPayload().getSearchRowsOutput();
      if (searchResult == null || searchResult.length <= 0) {
        throw new DPPWebserviceException(new MessageBuilder().setDescription("No statichost found for the given ip and mac.")
                                         .setDomain("F").setErrorNumber("400").setSeverity("3"));
      }
      if (searchResult.length > 1) {
        logger.warn("The given combination of ip and mac did not yield a unique hit in the statichost table.");
      }
      
      Row_ctype deleteRow = searchResult[0];
      if (!deleteRow.getDeployed1().equals("NO") ||
          !deleteRow.getDeployed2().equals("NO")) {
        throw new DPPWebserviceException(new MessageBuilder().setDescription("Can not delete a deployed host.")
                                         .setDomain("F").setErrorNumber("400").setSeverity("3"));
      }
      
      // convert input to deleteRowsInput
      DeleteRowsRequest_ctype request = new DeleteRowsRequest_ctype(deleteIPforCPERequest.getInputHeader(), deleteRow);
      // call delete row
      deleteRows(request);
      ResponseHeader_ctype header = new ResponseHeader_ctype();
      header.setDescription("Ok");
      return new DeleteIPforCPEResponse_ctype(header, "Success");
    } catch (Throwable t) {
      return new DeleteIPforCPEResponse_ctype(createResponseHeader("Error",t), "Error");
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
  
  
  private static com.gip.www.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype convertSetIpForCpeRequestToInsertRowRequest(com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPERequest_ctype request, ExternalCallHelper.PoolData containingPool) {
    com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPEInput_ctype input = request.getSetIPforCPEInput();

    Row_ctype generatedRow = new Row_ctype();
    generatedRow.setStaticHostID("-1"); // will be adjusted
    generatedRow.setSubnetID(Integer.toString(containingPool.subnetId));
    generatedRow.setSubnet(""); //column is virtual
    generatedRow.setCpe_mac(input.getCpe_mac());
    generatedRow.setRemoteId(input.getRemoteId());
    generatedRow.setIp(input.getIp());
    generatedRow.setDns(input.getDns());
    generatedRow.setHostname(input.getHostname());
    generatedRow.setDeployed1("NO");
    generatedRow.setDeployed2("NO");
    generatedRow.setConfigDescr(input.getConfigDescr());
    generatedRow.setAssignedPoolID(Integer.toString(containingPool.poolId));
    generatedRow.setDesiredPoolType(Integer.toString(containingPool.poolTypeId)); // do they want to enter it? | CPE-PoolID
    
    return new InsertRowRequest_ctype(request.getInputHeader(), generatedRow);
  }
 

}

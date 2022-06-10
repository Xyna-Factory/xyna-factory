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


package com.gip.www.juno.Service.WS.CM;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.www.juno.Service.WS.CM.Messages.*;
import com.gip.www.juno.Gui.WS.Messages.*;
import com.gip.xyna.utils.db.ResultSetReader;

import org.apache.log4j.Logger;

import com.gip.juno.ws.handler.AuthenticationTools.AuthenticationMode;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.WebserviceHandler;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.juno.ws.db.tables.dhcp.StaticHostHandler;
import com.gip.juno.ws.db.tables.dhcpv6.HostHandler;
import com.gip.juno.ws.db.tables.service.CmHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;



public class CMBindingImpl implements com.gip.www.juno.Service.WS.CM.CM_PortType{

  private static final TableHandler _handler = new CmHandler();
  private static final Logger logger = Logger.getLogger("CMBindingImpl");
  
  
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




  public  com.gip.www.juno.Service.WS.CM.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setMetaInfoOutput(new CMBindingReal().getMetaInfo(getMetaInfoRequest));
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setGetAllRowsOutput(new CMBindingReal().getAllRows(getAllRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype searchRows(com.gip.www.juno.Service.WS.CM.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setSearchRowsOutput(new CMBindingReal().searchRows(searchRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype updateRow(com.gip.www.juno.Service.WS.CM.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUpdateRowOutput(new CMBindingReal().updateRow(updateRowRequest));      
      return createResponse(payload);
    } catch (Throwable t){
      return createResponse("Error",t);
    }
  }
 
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype insertRow(com.gip.www.juno.Service.WS.CM.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setInsertRowOutput(new CMBindingReal().insertRow(insertRowRequest, true));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype insertRowWithSessionAuthentification(com.gip.www.juno.Service.WS.CM.Messages.InsertRowRequest_ctype insertRowWithSessionAuthentificationRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setInsertRowOutput(new CMBindingReal().insertRow(insertRowWithSessionAuthentificationRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype deleteRows(com.gip.www.juno.Service.WS.CM.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeleteRowsOutput(new CMBindingReal().deleteRows(deleteRowsRequest, true));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }  
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype deleteRowsWithSessionAuthentification(com.gip.www.juno.Service.WS.CM.Messages.DeleteRowsRequest_ctype deleteRowsWithSessionAuthentificationRequest) throws java.rmi.RemoteException {
    try {
      Payload_ctype payload=new Payload_ctype();      
      payload.setDeleteRowsOutput(new CMBindingReal().deleteRows(deleteRowsWithSessionAuthentificationRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype getLocations(com.gip.www.juno.Gui.WS.Messages.GetLocationsRequest_ctype getLocationsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setLocationsList(new CMBindingReal().getLocations(getLocationsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype getColValuesDistinct(com.gip.www.juno.Gui.WS.Messages.GetColValuesDistinctRequest_ctype getColValuesDistinctRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setColValuesDistinct(new CMBindingReal().getColValuesDistinct(getColValuesDistinctRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  } 
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype updateRowPk(com.gip.www.juno.Service.WS.CM.Messages.UpdateRowPkRequest_ctype updateRowPkRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setUpdateRowPkOutput(new CMBindingReal().updateRowPk(updateRowPkRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  } 

  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype moveRowsChangeLocation(com.gip.www.juno.Service.WS.CM.Messages.MoveRowsChangeLocationRequest_ctype moveRowsChangeLocationRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setMoveRowsChangeLocationOutput(new CMBindingReal().moveRowsChangeLocation(moveRowsChangeLocationRequest));
      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  } 

  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.Service.WS.CM.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCountRowsWithConditionOutput(new CMBindingReal().countRowsWithCondition(countRowsWithConditionRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }
  
  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype countAllRows(com.gip.www.juno.Service.WS.CM.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException {
    try{
      Payload_ctype payload=new Payload_ctype();      
      payload.setCountAllRowsOutput(new CMBindingReal().countAllRows(countAllRowsRequest));      
      return createResponse(payload);
    } catch (Exception e){
      return createResponse("Error",e);
    }
  }

  
  public com.gip.www.juno.Service.WS.CM.Messages.Response_ctype syncCpeIPs(com.gip.www.juno.Service.WS.CM.Messages.SyncCpeIPsRequest_ctype syncCpeIPsRequest) throws java.rmi.RemoteException {
    try {
      Payload_ctype payload=new Payload_ctype();
      final InputHeaderContent_ctype inputHeader = syncCpeIPsRequest.getInputHeader();
      final String mac = syncCpeIPsRequest.getSyncCpeIPsInput().getMac();
      if (mac == null ||
          mac.length() == 0) {
        throw new DPPWebserviceException("Received invalid mac for CpeIp synchronization.");
      }
      final Collection<String> ipv6s = queryIPv6sForRemoteIdFromDeployed(mac);
      final Collection<String> ips = queryIPsForRemoteIdFromDeployed(mac);
      
      new MultiUserTools.GlobalLockRetryAction<Row_ctype>() {
        @Override
        public Row_ctype performAction() throws RemoteException {
          Row_ctype pkRow = new Row_ctype();
          pkRow.setMac(mac);
          List<Row_ctype> result = new WebserviceHandler<Row_ctype>().searchRows(pkRow, _handler, inputHeader.getUsername(), inputHeader.getPassword());
          if (result.size() < 1) {
            throw new DPPWebserviceException("A Cm for the given mac (" + mac + ") could not be found.");
          }
          Row_ctype update = result.get(0);
          update.setCpeIps(buildSeperatedList(ips, ","));
          update.setCpeIpsv6(buildSeperatedList(ipv6s, ","));
          return new WebserviceHandler<Row_ctype>().updateRow(update, _handler,
                                                              inputHeader.getUsername(),
                                                              inputHeader.getPassword(), AuthenticationMode.PRIVILEGED_SESSION);
        }
      }.executeRetryAction();
      payload.setSyncCpeIPsOutput("1");
      return createResponse(payload);
    } catch (Throwable t){
      return createResponse("Error",t);
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
  
  
  private static Collection<String> queryIPv6sForRemoteIdFromDeployed(String mac) throws RemoteException {
    SQLCommand query = new SQLCommand("SELECT assignedIp, prefixlength from dhcpv6.host WHERE host.agentRemoteId = ? and deploymentState='YES'");
    query.addConditionParam(mac);
    return new DBCommands<String>().query(new IPv6FullAddresseReader(), new HostHandler().getDBTableInfo(), query, logger);
  }
  
  
  private static Collection<String> queryIPsForRemoteIdFromDeployed(String mac) throws RemoteException {
    SQLCommand query = new SQLCommand("SELECT ip FROM dhcp.statichost WHERE statichost.remoteId = ? and deployed1='YES'");
    query.addConditionParam(mac);
    return new DBCommands<String>().query(new DBStringReader(), new StaticHostHandler().getDBTableInfo(), query, logger);
  }
  
  
  private static String buildSeperatedList(Collection<String> values, String seperation) {
    StringBuilder listBuilder = new StringBuilder();
    Iterator<String> valueIterator = values.iterator();
    while (valueIterator.hasNext()) {
      String value = valueIterator.next();
      if (value != null &&
          value.length() > 0) {
        listBuilder.append(value);
        if (valueIterator.hasNext()) {
          listBuilder.append(seperation);
        }
      }
    }
    return listBuilder.toString();
  }
  
  
  public static class IPv6FullAddresseReader implements ResultSetReader<String> {

    public String read(ResultSet rs) throws SQLException {
      String assignedIp = rs.getString("assignedIp");
      StringBuilder ipBuilder = new StringBuilder();
      if (assignedIp != null &&
          assignedIp.length() > 0) {
        ipBuilder.append(assignedIp);
        String prefix = rs.getString("prefixlength");
        if (prefix == null ||
            prefix.length() == 0) {
          ipBuilder.append("/128");
        } else {
          if (!prefix.startsWith("/")) {
            ipBuilder.append("/");
          }
          ipBuilder.append(prefix);
        }
      }
      return ipBuilder.toString();
    }
  }
  
  
  
  
  /*public static void main(String... args) throws Throwable {
    //Pool[1]:10.60.3.2<->10.60.3.254
    //lease.ip: 10.60.3.14
    //PoolData pd = new PoolData(1, 1, "10.60.3.2", "10.60.3.254");
    /*PoolData pd = new PoolData(1, 1, "10.60.3.2", "10.60.3.254");
    System.out.println(Arrays.toString(pd.startParts));
    String ip = "10.60.3.14";
    System.out.println(pd.contains(ip, convertIpStringToIntParts(ip)));*/
    /*PoolData pd = new PoolData(1, 1, "1.1.1.20", "1.1.2.10");
    IpGenerationIterator iter = pd.getPoolIpGenerationIerator(Arrays.asList(new String[] {}));
    while (iter.hasNext()) {
      System.out.println(iter.next());
    }
    
  }*/
  
}

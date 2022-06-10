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
/**
 * Dhcpv4PacketsBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.audit.Dhcpv4PacketsHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.handler.ChangeMonitor;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.WebserviceHandler;
import com.gip.juno.ws.handler.tables.DeploymentTools;
import com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Payload_ctype;
import com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype;
import com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Row_ctype;
import com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype;
import com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype;
import com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype;
import com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype;

public class Dhcpv4PacketsBindingImpl implements com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Dhcpv4Packets_PortType{
    
  static Logger logger = Logger.getLogger(Dhcpv4PacketsBindingImpl.class);
  private static TableHandler _handler = new Dhcpv4PacketsHandler();
  
  public com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException {
    try {
      Payload_ctype payload = new Payload_ctype();
      MetaInfoRow_ctype ref = new MetaInfoRow_ctype();
      InputHeaderContent_ctype header = metaInfoRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<MetaInfoRow_ctype> list = new WebserviceHandler<MetaInfoRow_ctype>()
          .getMetaInfo(ref, _handler, username, password);
      MetaInfoRow_ctype[] ret = list.toArray(new MetaInfoRow_ctype[list
          .size()]);

      payload.setMetaInfoOutput(ret);
      return createResponse(payload);
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      return createResponse("Error", e);
    }
    }

    public com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
      try{
        Payload_ctype payload=new Payload_ctype();
        Row_ctype ref = new Row_ctype();
        InputHeaderContent_ctype header = getAllRowsRequest.getInputHeader();
        String username = header.getUsername();
        String password = header.getPassword();
        
        List<String> colNames = _handler.getDBTableInfo().getColumnNames();
        for (int colCount=0;colCount<colNames.size();colCount++){
          logger.info(" SPALTE = " +colNames.get(colCount));
        }
        
        List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().getAllRows(ref, _handler, username,
            password);
        payload.setGetAllRowsOutput(ret.toArray(new Row_ctype[ret.size()]));
        return createResponse(payload);
      } catch (Exception e){
        _handler.getLogger().error("", e);
        return createResponse("Error",e);
      }
    }

    public com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype searchRows(com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
      try {
        Payload_ctype payload = new Payload_ctype();
        Row_ctype row = searchRowsRequest.getSearchRowsInput();
        InputHeaderContent_ctype header = searchRowsRequest
            .getInputHeader();
        String username = header.getUsername();
        String password = header.getPassword();
        List<Row_ctype> ret = new WebserviceHandler<Row_ctype>()
            .searchRows(row, _handler, username, password);
        payload.setSearchRowsOutput(ret.toArray(new Row_ctype[ret.size()]));
        return createResponse(payload);
      } catch (Exception e) {
        _handler.getLogger().error("", e);
        return createResponse("Error", e);
      }
    }

    public com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype updateRow(com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
      Payload_ctype payload = new Payload_ctype();
      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      try {
      InputHeaderContent_ctype header = updateRowRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = updateRowRequest.getUpdateRowInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(
          _handler, _handler.getLogger());
      deployInfo.setService(_handler.getTablename() + ".Update");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      Row_ctype before = monitor.queryRowToUpdate(input);

      Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRow(input,
          _handler, username, password);
      String changelog = monitor.buildUpdateStringPkOnly(before, ret);
      deployInfo.setLog(changelog);

      payload.setUpdateRowOutput(ret);
      return createResponse(payload);
    } catch (Exception e) {
      deployInfo.setLog(e);
      _handler.getLogger().error("", e);
      return createResponse("Error", e);
    } finally {
      DeploymentTools.insertRow(deployInfo);
    }
    }

    public com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype insertRow(com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
      Payload_ctype payload = new Payload_ctype();

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      try {
        InputHeaderContent_ctype header = insertRowRequest.getInputHeader();
        String username = header.getUsername();
        String password = header.getPassword();
        Row_ctype input = insertRowRequest.getInsertRowInput();
        ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(
            _handler, _handler.getLogger());
        String changelog = monitor.buildInsertStringPkOnly(input);
        deployInfo.setService(_handler.getTablename() + ".Insert");
        deployInfo.setUser(username);
        deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
        Row_ctype ret = new WebserviceHandler<Row_ctype>().insertRow(input,
            _handler, username, password);
        deployInfo.setLog(changelog);
        payload.setInsertRowOutput(ret);
        return createResponse(payload);
      } catch (Exception e) {
        deployInfo.setLog(e);
        _handler.getLogger().error("", e);
        return createResponse("Error", e);
      } finally {
        DeploymentTools.insertRow(deployInfo);
      }
    }

    public com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.Response_ctype deleteRows(com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
      Payload_ctype payload = new Payload_ctype();

      DeploymentTools.Row deployInfo = new DeploymentTools.Row();
      try {
        InputHeaderContent_ctype header = deleteRowsRequest
            .getInputHeader();
        String username = header.getUsername();
        String password = header.getPassword();
        Row_ctype input = deleteRowsRequest.getDeleteRowsInput();
        ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(
            _handler, _handler.getLogger());
        String changelog = monitor.buildDeleteStringPkOnly(monitor
            .queryOneRow(input));
        deployInfo.setService(_handler.getTablename() + ".Delete");
        deployInfo.setUser(username);
        deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
        String ret = new WebserviceHandler<Row_ctype>().deleteRows(input,
            _handler, username, password);
        deployInfo.setLog(changelog);
        payload.setDeleteRowsOutput(ret);
        return createResponse(payload);
      } catch (Exception e) {
        deployInfo.setLog(e);
        _handler.getLogger().error("", e);
        return createResponse("Error", e);
      } finally {
        DeploymentTools.insertRow(deployInfo);
      }
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

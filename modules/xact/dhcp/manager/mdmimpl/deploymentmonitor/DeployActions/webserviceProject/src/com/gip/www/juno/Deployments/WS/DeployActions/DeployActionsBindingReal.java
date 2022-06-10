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

import com.gip.www.juno.Deployments.WS.DeployActions.Messages.*;

import com.gip.www.juno.Gui.WS.Messages.*; 

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.deployments.Deploy_ActionsHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.handler.*;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.handler.ReflectionTools.DBReader;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;

public class DeployActionsBindingReal {

  private static TableHandler _handler = new Deploy_ActionsHandler();

  public MetaInfoRow_ctype[] getMetaInfo(GetMetaInfoRequest_ctype metaInfoRequest)
        throws java.rmi.RemoteException {
    try {
      MetaInfoRow_ctype ref = new MetaInfoRow_ctype();
      InputHeaderContent_ctype header = metaInfoRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<MetaInfoRow_ctype> list = new WebserviceHandler<MetaInfoRow_ctype>().getMetaInfo(ref,  _handler,
          username, password);
      MetaInfoRow_ctype[] ret = list.toArray(new MetaInfoRow_ctype[list.size()]);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }

  public Row_ctype[] getAllRows(GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try {
      Row_ctype ref = new Row_ctype();
      InputHeaderContent_ctype header = getAllRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<Row_ctype> ret = getAllRowsImpl(ref, _handler, username,
          password);
      return ret.toArray(new Row_ctype[ret.size()]);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }


  public Row_ctype insertRow(InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = insertRowRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = insertRowRequest.getInsertRowInput();
      Row_ctype ret = new WebserviceHandler<Row_ctype>().insertRow(input, _handler, username,
          password);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }


  public Row_ctype[] searchRows(SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try {
      Row_ctype row = searchRowsRequest.getSearchRowsInput();
      InputHeaderContent_ctype header = searchRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<Row_ctype> ret = searchRowsImpl(row, _handler, username, password);
      return ret.toArray(new Row_ctype[ret.size()]);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }

  }


  public String deleteRows(DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = deleteRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = deleteRowsRequest.getDeleteRowsInput();
      String ret = new WebserviceHandler<Row_ctype>().deleteRows(input, _handler, username,
          password);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }

  public Row_ctype updateRowPk(UpdateRowPkRequest_ctype updateRowPkRequest) throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = updateRowPkRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      UpdateRowPkInput_ctype input = updateRowPkRequest.getUpdateRowPkInput();
      Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRowPk(input.getConditions(), 
          input.getNewValues() , _handler, username, password);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }

  public String countAllRows(CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = countAllRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      String ret = new WebserviceHandler<Row_ctype>().countAllRows(_handler, username, password);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }

  public String countRowsWithCondition(CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) 
          throws java.rmi.RemoteException {
    try {
      Row_ctype row = countRowsWithConditionRequest.getCountRowsWithConditionInput();
      InputHeaderContent_ctype header = countRowsWithConditionRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      String ret = new WebserviceHandler<Row_ctype>().countRowsWithCondition(row, _handler, username,
          password);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }    
  }
  
  private List<Row_ctype> getAllRowsImpl(Row_ctype ref, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
//      AuthenticationTools.authenticate(username, password, logger);
//      AuthenticationTools.checkPermissionsDBSelect(username, handler.getDBTableInfo().getSchema(), logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER;
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<Row_ctype> reader = new ReflectionTools.DBReader<Row_ctype>(table, ref, logger);    
      
      SQLCommand builder = SQLBuilder.buildSQLSelectAll(table, false);   
      builder.sql += " ORDER BY deploy_time DESC LIMIT 999";
      
      List<Row_ctype> ret = new DBCommands<Row_ctype>().query(reader, table, builder, logger);
      if (ret == null) {
        ret = new ArrayList<Row_ctype>();
      }
      //List<Row_ctype> ret = new DBCommandHandler<Row_ctype>().getAllRows(reader, table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRows", e);
    }
  }
  

  private List<Row_ctype> searchRowsImpl(Row_ctype input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      //AuthenticationTools.authenticate(username, password, logger);
      //AuthenticationTools.checkPermissionsDBSelect(username, handler.getDBTableInfo().getSchema(), logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER;
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<Row_ctype> reader = new ReflectionTools.DBReader<Row_ctype>(table, input, logger);
      TreeMap<String, String> map = new ReflectionTools<Row_ctype>(input).getRowMap(table, input, logger);
      
      SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table, false);
      builder.sql += " ORDER BY deploy_time DESC LIMIT 999";
      
      List<Row_ctype> ret = new DBCommands<Row_ctype>().query(reader, table, builder, logger);   
      if (ret == null) {
        ret = new ArrayList<Row_ctype>();
      }
      //List<Row_ctype> ret = new DBCommandHandler<Row_ctype>().searchRows(input, reader, table, map, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }
}


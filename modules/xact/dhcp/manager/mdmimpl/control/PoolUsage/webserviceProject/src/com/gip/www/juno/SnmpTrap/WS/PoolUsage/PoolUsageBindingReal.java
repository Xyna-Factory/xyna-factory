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

package com.gip.www.juno.SnmpTrap.WS.PoolUsage;

import com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages.*;

import com.gip.www.juno.Gui.WS.Messages.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.gip.juno.ws.db.tables.snmptrap.PoolUsageHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.handler.*;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.handler.ReflectionTools.DBReader;
import com.gip.juno.ws.handler.tables.DeploymentTools;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;

public class PoolUsageBindingReal{

  private static TableHandler _handler = new PoolUsageHandler();

  public MetaInfoRow_ctype[] getMetaInfo(GetMetaInfoRequest_ctype metaInfoRequest)
        throws java.rmi.RemoteException {
    try {
      MetaInfoRow_ctype ref = new MetaInfoRow_ctype();
      InputHeaderContent_ctype header = metaInfoRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<MetaInfoRow_ctype> list = new WebserviceHandler<MetaInfoRow_ctype>().getMetaInfo(ref,  _handler,username, password);
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
      List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().getAllRows(ref, _handler, username,
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
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = insertRowRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = insertRowRequest.getInsertRowInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
      String changelog = monitor.buildInsertString(input);
      deployInfo.setService(_handler.getTablename() + ".Insert");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      Row_ctype ret = new WebserviceHandler<Row_ctype>().insertRow(input, _handler, username,
          password);
      deployInfo.setLog(changelog);
      return ret;
    } catch (java.rmi.RemoteException e) {
      deployInfo.setLog(e);
      throw e;
    } catch (Exception e) {
      deployInfo.setLog(e);
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
    finally {
      DeploymentTools.insertRow(deployInfo);
    }
  }


  public Row_ctype[] searchRows(SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try {
      Row_ctype row = searchRowsRequest.getSearchRowsInput();
      InputHeaderContent_ctype header = searchRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<Row_ctype> ret;
      String comparision = row.getPoolID().replaceAll("&gt;", ">");
      comparision = comparision.replaceAll("&lt;", "<");
      if (comparision.startsWith("<") || comparision.startsWith(">")) {
        ret = specialSearchByPoolID(row, comparision, username, password);
      } else {
        ret = new WebserviceHandler<Row_ctype>().searchRows(row, _handler, username, password);
      }
      return ret.toArray(new Row_ctype[ret.size()]);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }
  
  
  private List<Row_ctype> specialSearchByPoolID(Row_ctype row, String comparision, String username, String password) throws RemoteException {

    WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER
                    .clone();
    AuthenticationTools.authenticateAndAuthorize(username, password, _handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, _handler);
    DBTableInfo table = _handler.getDBTableInfo();
    DBReader<Row_ctype> reader = new ReflectionTools.DBReader<Row_ctype>(table, row, _handler.getLogger());
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT * FROM ")
           .append(_handler.getDBTableInfo().getTablename())
           .append(" WHERE ")
           .append(PoolUsageHandler.POOLID_COLUMN_NAME)
           .append(' ')
           .append(comparision)
           .append(" ORDER BY ")
           .append(PoolUsageHandler.USEDFRACTION_COLUMN_NAME)
           .append(" DESC");
    _handler.getLogger().info("going to query: " + builder.toString());
    SQLCommand query = new SQLCommand(builder.toString());
    List<Row_ctype> ret = new DBCommands<Row_ctype>().query(reader, table, query, _handler.getLogger());
    return ret;
  }


  public String deleteRows(DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = deleteRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = deleteRowsRequest.getDeleteRowsInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
      String changelog = monitor.buildDeleteString(monitor.queryOneRow(input));
      deployInfo.setService(_handler.getTablename() + ".Delete");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      String ret = new WebserviceHandler<Row_ctype>().deleteRows(input, _handler, username,
          password);
      deployInfo.setLog(changelog);
      return ret;
    } catch (java.rmi.RemoteException e) {
      deployInfo.setLog(e);
      throw e;
    } catch (Exception e) {
      deployInfo.setLog(e);
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
    finally {
      DeploymentTools.insertRow(deployInfo);
    }
  }

  /*
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
  */

}


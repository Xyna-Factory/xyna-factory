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

package com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold;

import com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold.Messages.*;

import com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype;
import com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype;
import com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype;

import java.util.List;

import com.gip.juno.ws.db.tables.snmptrap.PoolUsageThresholdHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.handler.*;
import com.gip.juno.ws.handler.tables.DeploymentTools;

public class PoolUsageThresholdBindingReal{

  private static TableHandler _handler = new PoolUsageThresholdHandler();

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
      List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().searchRows(row, _handler, username,
          password);
      return ret.toArray(new Row_ctype[ret.size()]);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }

  }


  public Row_ctype updateRow(UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = updateRowRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = updateRowRequest.getUpdateRowInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
      deployInfo.setService(_handler.getTablename() + ".Update");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      Row_ctype before = monitor.queryRowToUpdate(input);
      Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRow(input, _handler, username,
          password);
      String changelog = monitor.buildUpdateString(before, ret);
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


  public String deleteRowsWithNullConditions(DeleteRowsWithNullConditionsRequest_ctype deleteRowsWithNullConditionsRequest)
      throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = deleteRowsWithNullConditionsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = deleteRowsWithNullConditionsRequest.getDeleteRowsWithNullConditionsInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
      String changelog = monitor.buildDeleteString(input);
      deployInfo.setService(_handler.getTablename() + ".Delete");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      String ret = new WebserviceHandler<Row_ctype>().deleteRowsWithNullConditions(input, _handler, username,
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

  public Row_ctype updateRowPkWithNullConditions(UpdateRowPkWithNullConditionsRequest_ctype updateRowPkWithNullConditionsRequest)
        throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = updateRowPkWithNullConditionsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      UpdateRowPkWithNullConditionsInput_ctype input =
          updateRowPkWithNullConditionsRequest.getUpdateRowPkWithNullConditionsInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, _handler.getLogger());
      deployInfo.setService(_handler.getTablename() + ".Update");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      //Row_ctype before = monitor.queryRowToUpdate(input.getConditions());
      Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRowPkWithNullConditions(input.getConditions(),
          input.getNewValues() , _handler, username, password);
      String changelog = monitor.buildUpdateStringMultipleRows(input.getConditions(), input.getNewValues());
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

}


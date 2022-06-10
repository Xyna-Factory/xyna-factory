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

import com.gip.www.juno.Service.WS.CM.Messages.*;
import com.gip.www.juno.Gui.WS.Messages.*;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.handler.ChangeMonitor;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.WebserviceHandler;
import com.gip.juno.ws.handler.tables.DeploymentTools;
import com.gip.juno.ws.tools.OutputHeaderData;
import com.gip.juno.ws.tools.WSTools;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.juno.ws.db.tables.service.CmHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;

public class CMBindingReal{

  private static final TableHandler _handler = new CmHandler();
  private static final Logger logger = Logger.getLogger("CMBindingImpl");
  

  public String[] getLocations(GetLocationsRequest_ctype getLocationsRequest) throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = getLocationsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      return WebserviceHandler.getLocations(_handler, username, password);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error(e);
      throw new DPPWebserviceException("Error.", e);
    }
  }

  public MetaInfoRow_ctype[] getMetaInfo(GetMetaInfoRequest_ctype metaInfoRequest)
        throws java.rmi.RemoteException {
    try {
      MetaInfoRow_ctype ref = new MetaInfoRow_ctype();
      InputHeaderContent_ctype header = metaInfoRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<MetaInfoRow_ctype> list = new WebserviceHandler<MetaInfoRow_ctype>().getMetaInfo(ref, _handler,
          username, password);
      MetaInfoRow_ctype[] ret = list.toArray(new MetaInfoRow_ctype[list.size()]);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error(e);
      throw new DPPWebserviceException("Error.", e);
    }
  }

  
  public RowListOutput_ctype getAllRows(GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
    try {
      Row_ctype ref = new Row_ctype();
      InputHeaderContent_ctype header = getAllRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<Row_ctype> result = new WebserviceHandler<Row_ctype>().getAllRows(ref, _handler, username,
          password);
      RowListOutput_ctype ret = new RowListOutput_ctype();
      RowListWrapper_ctype wrapper = new RowListWrapper_ctype();
      wrapper.setItem(result.toArray(new Row_ctype[result.size()]));
      ret.setContent(wrapper);
      ret.setOutputHeader(getOutputHeader(result));
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error(e);
      throw new DPPWebserviceException("Error.", e);
    }
  }


  public Row_ctype insertRow(InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
    return insertRow(insertRowRequest, false);
  }
  
  
  public Row_ctype insertRow(InsertRowRequest_ctype insertRowRequest, final boolean useLegacyAuthentication) throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = insertRowRequest.getInputHeader();
      final String username = header.getUsername();
      final String password = header.getPassword();
      final Row_ctype input = insertRowRequest.getInsertRowInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, logger);
      String changelog = monitor.buildInsertString(input);
      deployInfo.setService(_handler.getTablename() + ".Insert");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      WSTools.validate(_handler.getTablename(), insertRowRequest.getInsertRowInput().getXml(), logger);
      Row_ctype ret = null;
      if (useLegacyAuthentication) {
        ret = new MultiUserTools.GlobalLockRetryAction<Row_ctype>() {
          @Override
          public Row_ctype performAction() throws RemoteException {
            return new WebserviceHandler<Row_ctype>().insertRow(input, _handler, username, password, useLegacyAuthentication);
          }
        }.executeRetryAction();
      } else {
        ret = new WebserviceHandler<Row_ctype>().insertRow(input, _handler, username, password, useLegacyAuthentication);
      }
      deployInfo.setLog(changelog);
      return ret;
    } catch (java.rmi.RemoteException e) {
      deployInfo.setLog(e);
      _handler.getLogger().error(e);
      throw e;
    } catch (Exception e) {
      deployInfo.setLog(e);
      _handler.getLogger().error(e);
      throw new DPPWebserviceException("Error.", e);
    } catch (Error t) {
      _handler.getLogger().error(t);
      throw t;
    }
    finally {
      DeploymentTools.insertRow(deployInfo);
    }

  }


  public RowListOutput_ctype searchRows(SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
    try {
      Row_ctype row = searchRowsRequest.getSearchRowsInput();
      InputHeaderContent_ctype header = searchRowsRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      List<Row_ctype> result = new WebserviceHandler<Row_ctype>().searchRows(row,
          _handler, username, password);
      RowListOutput_ctype ret = new RowListOutput_ctype();
      RowListWrapper_ctype wrapper = new RowListWrapper_ctype();
      wrapper.setItem(result.toArray(new Row_ctype[result.size()]));
      ret.setContent(wrapper);
      ret.setOutputHeader(getOutputHeader(result));
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error(e);
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
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, logger);
      deployInfo.setService(_handler.getTablename() + ".Update");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      WSTools.validate(_handler.getTablename(), updateRowRequest.getUpdateRowInput().getXml(),logger);
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
      _handler.getLogger().error(e);
      throw new DPPWebserviceException("Error.", e);
    }
    finally {
      DeploymentTools.insertRow(deployInfo);
    }
  }

  public String deleteRows(DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {
    return deleteRows(deleteRowsRequest, false);
  }
  
  
  public String deleteRows(DeleteRowsRequest_ctype deleteRowsRequest, final boolean useLegacyAuthentication) throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = deleteRowsRequest.getInputHeader();
      final String username = header.getUsername();
      final String password = header.getPassword();
      final Row_ctype input = deleteRowsRequest.getDeleteRowsInput();
      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, logger);
      String changelog = monitor.buildDeleteString(monitor.queryOneRow(input));
      deployInfo.setService(_handler.getTablename() + ".Delete");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      String ret = null;
      if (useLegacyAuthentication) {
        ret = new MultiUserTools.GlobalLockRetryAction<String>() {
          @Override
          public String performAction() throws RemoteException {
            return new WebserviceHandler<Row_ctype>().deleteRows(input, _handler, username, password, useLegacyAuthentication);
          }
        }.executeRetryAction();
      } else {
        ret = new WebserviceHandler<Row_ctype>().deleteRows(input, _handler, username, password, useLegacyAuthentication);
      }
      deployInfo.setLog(changelog);
      return ret;
    } catch (java.rmi.RemoteException e) {
      deployInfo.setLog(e);
      throw e;
    } catch (Exception e) {
      deployInfo.setLog(e);
      _handler.getLogger().error(e);
      throw new DPPWebserviceException("Error.", e);
    }
    finally {
      DeploymentTools.insertRow(deployInfo);
    }
  }

  private OutputHeaderContent_ctype getOutputHeader(List<Row_ctype> result) throws RemoteException {
    OutputHeaderContent_ctype ret = new OutputHeaderContent_ctype();
    OutputHeaderData data = getOutputHeaderData(_handler.getLogger());
    ret.setException(data.exceptionText);
    ret.setStatus(data.status);
    return ret;
  }

  /**
   * vermutlich ueberfluessig
   * @param logger
   * @return
   * @throws RemoteException
   */
  public OutputHeaderData getOutputHeaderData( Logger logger)
  throws RemoteException {
    try {
      OutputHeaderData ret = new OutputHeaderData();
      ret.exceptionText = "";
      ret.status = "Success.";
      return ret;
    } catch (Exception e) {
      logger.error("Error in getOutputHeaderData: ", e);
      throw new java.rmi.RemoteException("Error in getOutputHeaderData: " + e.toString());
    }
  }

  
  public String[] getColValuesDistinct(GetColValuesDistinctRequest_ctype getColValuesDistinctRequest)
        throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = getColValuesDistinctRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      String input = getColValuesDistinctRequest.getGetColValuesDistinctInput();
      List<String> ret = WebserviceHandler.getColValuesDistinct(input, _handler, username,
          password);
      return ret.toArray(new String[ret.size()]);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      _handler.getLogger().error("", e);
      throw new DPPWebserviceException("Error.", e);
    }
  }


  public Row_ctype updateRowPk(UpdateRowPkRequest_ctype updateRowPkRequest) throws java.rmi.RemoteException {
    DeploymentTools.Row deployInfo = new DeploymentTools.Row();
    try {
      InputHeaderContent_ctype header = updateRowPkRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      UpdateRowPkInput_ctype input = updateRowPkRequest.getUpdateRowPkInput();

      ChangeMonitor<Row_ctype> monitor = new ChangeMonitor<Row_ctype>(_handler, logger);
      deployInfo.setService(_handler.getTablename() + ".Update");
      deployInfo.setUser(username);
      deployInfo.setTarget(ChangeMonitor.Constant.MANAGEMENT);
      Row_ctype before = monitor.queryRowToUpdate(input.getConditions());

      Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRowPk(input.getConditions(),
          input.getNewValues() , _handler, username, password);
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

  /**
   * @deprecated
   * @see com.gip.www.juno.Service.WS.CM.CM_PortType#moveRowsChangeLocation(com.gip.www.juno.Service.WS.CM.Messages.MoveRowsChangeLocationRequest_ctype)
   */
  public String moveRowsChangeLocation(MoveRowsChangeLocationRequest_ctype moveRowsChangeLocationRequest)
        throws java.rmi.RemoteException {
    try {
      InputHeaderContent_ctype header = moveRowsChangeLocationRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = moveRowsChangeLocationRequest.getMoveRowsChangeLocationInput().getCondition();
      String newLocation = moveRowsChangeLocationRequest.getMoveRowsChangeLocationInput().getNewLocation();
      String ret =""; //new WebserviceHandler<Row_ctype>().moveRowsChangeLocation(input, newLocation, _handler,
          //username, password);
      return ret;
    }  catch (Exception e) {
      _handler.getLogger().error(e);
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

  



}

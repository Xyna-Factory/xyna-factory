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

package com.gip.www.juno.DHCP.WS.GuiParameter;

import com.gip.www.juno.DHCP.WS.GuiParameter.Messages.*;

import com.gip.www.juno.Gui.WS.Messages.*;

import java.util.List;

import com.gip.juno.ws.db.tables.dhcp.GuiParameterHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.handler.*;

import org.apache.log4j.Logger;

public class GuiParameterBindingReal{

  private static TableHandler _handler = new GuiParameterHandler();

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
    try {
      InputHeaderContent_ctype header = updateRowRequest.getInputHeader();
      String username = header.getUsername();
      String password = header.getPassword();
      Row_ctype input = updateRowRequest.getUpdateRowInput();
      Row_ctype ret = new WebserviceHandler<Row_ctype>().updateRow(input, _handler, username,
          password);
      return ret;
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

}


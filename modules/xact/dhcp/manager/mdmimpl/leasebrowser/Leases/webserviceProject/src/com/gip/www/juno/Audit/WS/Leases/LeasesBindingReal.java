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

package com.gip.www.juno.Audit.WS.Leases;

import java.util.List;

import com.gip.juno.ws.db.tables.audit.LeasesHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.WebserviceHandler;
import com.gip.www.juno.Gui.WS.Messages.*;
import com.gip.www.juno.Audit.WS.Leases.Messages.*;

public class LeasesBindingReal{
   
    private static final TableHandler _handler = new LeasesHandler(); 
  
    public Row_ctype[] searchLeases(SearchLeasesRequest_ctype searchLeasesRequest) throws java.rmi.RemoteException {
       try {
         InputHeaderContent_ctype header = searchLeasesRequest.getInputHeader();  
         String username = header.getUsername();
         String password = header.getPassword();
         String type = searchLeasesRequest.getSearchLeasesInput().getType();
         Row_ctype ref = new Row_ctype();
         List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().searchLeases(ref, type, username,
             password, _handler);
         return ret.toArray(new Row_ctype[ret.size()]);
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

    public Row_ctype insertRow(InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException {
      throw new DPPWebserviceException("Table Leases may not be edited.");
    }

    
    public Row_ctype updateRow(UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException {
      throw new DPPWebserviceException("Table Leases may not be edited.");
    }
      
    public String deleteRows(DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException {     
      throw new DPPWebserviceException("Table Leases may not be edited.");
    }


    public Row_ctype[] searchRows(SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException {
      try {
        Row_ctype row = searchRowsRequest.getSearchRowsInput().getRow();
        InputHeaderContent_ctype header = searchRowsRequest.getInputHeader();  
        String username = header.getUsername();
        String password = header.getPassword();
        String table = searchRowsRequest.getSearchRowsInput().getTable();
        if ((table == null) || (table.trim().length() == 0)) {
          throw new DPPWebserviceIllegalArgumentException("Parameter table must not be empty.");
        }
        List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().searchRowsLeases(table, row, 
            _handler, username, password);
        return ret.toArray(new Row_ctype[ret.size()]);
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        _handler.getLogger().error(e);
        throw new DPPWebserviceException("Error.", e);
      }    
    }
    

    public Row_ctype[] getAllRows(GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException {
      try {
        Row_ctype ref = new Row_ctype();    
        InputHeaderContent_ctype header = getAllRowsRequest.getInputHeader();  
        String username = header.getUsername();
        String password = header.getPassword();
        List<Row_ctype> ret = new WebserviceHandler<Row_ctype>().getAllRowsLeases(
            ref, _handler, username, password);
        return ret.toArray(new Row_ctype[ret.size()]);
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        _handler.getLogger().error(e);
        throw new DPPWebserviceException("Error.", e);
      }
    }

    
    public String countRowsWithCondition(CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) 
            throws java.rmi.RemoteException {
      try {
        Row_ctype row = countRowsWithConditionRequest.getCountRowsWithConditionInput().getRow();
        InputHeaderContent_ctype header = countRowsWithConditionRequest.getInputHeader();  
        String username = header.getUsername();
        String password = header.getPassword();
        String table = countRowsWithConditionRequest.getCountRowsWithConditionInput().getTable();
        if ((table == null) || (table.trim().length() == 0)) {
          throw new DPPWebserviceIllegalArgumentException("Parameter table must not be empty.");
        }
        String ret = new WebserviceHandler<Row_ctype>().countRowsWithConditionLeases(table, row, 
            _handler, username, password);
        return ret;
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        _handler.getLogger().error(e);
        throw new DPPWebserviceException("Error.", e);
      }      
    }

    
    public String countAllRows(CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException {
      try { 
        InputHeaderContent_ctype header = countAllRowsRequest.getInputHeader();  
        String username = header.getUsername();
        String password = header.getPassword();
        String ret = new WebserviceHandler<Row_ctype>().countAllRowsLeases(_handler, username, password);
        return ret;
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        _handler.getLogger().error(e);
        throw new DPPWebserviceException("Error.", e);
      }      
    }


    public String countLeases(CountLeasesRequest_ctype countLeasesRequest) throws java.rmi.RemoteException {
      try {
        InputHeaderContent_ctype header = countLeasesRequest.getInputHeader();  
        String username = header.getUsername();
        String password = header.getPassword();
        String type = countLeasesRequest.getCountLeasesInput().getType();
        String ret = new WebserviceHandler<Row_ctype>().countLeases(type, username, password, _handler);
        return ret;
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        _handler.getLogger().error(e);
        throw new DPPWebserviceException("Error.", e);
      }
    }

}

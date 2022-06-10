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
package com.gip.juno.ws.handler.tables;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.juno.cfgdecode.docsis.DocsisDecoding;
import com.gip.juno.cfggen.tlvencoding.docsis.DocsisEncoding;
import com.gip.juno.cfggen.util.StringToMapUtil;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainer;
import com.gip.juno.ws.tools.WSTools;
import com.gip.xyna.utils.db.ResultSetReader;

public class DocsisEncodingTools {

  private static final String _schema = "service";
  private static final String _sqlSelectAllForDecoding = 
        "SELECT id, parent_id, type_name, type_encoding, value_data_type_name FROM docsis_encoding";
  private static final String _sqlDocsisEncodingSelectAll = 
        "SELECT id, parent_id, cmts_mic_order, type_name, type_encoding, value_data_type_name,"
        + " value_data_type_arguments FROM docsis_encoding";
  
  public static class RowForDecoding {
    public String id = "";
    public String parentId = "";
    public String typeName = "";
    public String typeEncoding = "";
    public String valueDataTypeName = "";
    
    public DocsisDecoding toDocsisDecoding() {
      int id = Integer.parseInt(this.id);
      Integer parentId = null;
      if (!this.parentId.equals("")) {
        parentId = new Integer(this.parentId);          
      }
      int typeEncoding = Integer.parseInt(this.typeEncoding);
      return new DocsisDecoding(id, parentId, typeName, typeEncoding, valueDataTypeName);
    }
  }
  
  public static class Row {
    public String id = "";
    public String parentId = "";
    public String cmtsMicOrder = "";
    public String typeName = "";
    public String typeEncoding = "";
    public String valueDataTypeName = "";
    public String valueDataTypeArguments = "";
    
    public DocsisEncoding toDocsisEncoding() {
      int id = Integer.parseInt(this.id);
      Integer parentId = null;      
      if (!this.parentId.equals("")) {
        parentId = new Integer(this.parentId);          
      }
      Integer cmtsMicOrderNumber = null;      
      if (!this.cmtsMicOrder.equals("")) {
        cmtsMicOrderNumber = new Integer(this.cmtsMicOrder);          
      }
      int typeEncoding = Integer.parseInt(this.typeEncoding);
      try {
        Map<String, String> valueDataTypeArgumentsMap = StringToMapUtil.toMap(valueDataTypeArguments);
        DocsisEncoding ret = new DocsisEncoding(id, parentId, cmtsMicOrderNumber, typeName, typeEncoding, 
            valueDataTypeName, valueDataTypeArgumentsMap);
        return ret;
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Error in DOCSIS encoding with id: <" + id + ">", e);
      }
    }
  }

  private static class ReaderForDecoding implements ResultSetReader<DocsisDecoding> {    
    public DocsisDecoding read(ResultSet rs) throws SQLException {      
      RowForDecoding ret = new RowForDecoding();    
      ret.id = WSTools.getColValue(rs, "id");
      ret.parentId = WSTools.getColValue(rs, "parent_id");
      ret.typeName = WSTools.getColValue(rs, "type_name");
      ret.typeEncoding = WSTools.getColValue(rs, "type_encoding");
      ret.valueDataTypeName = WSTools.getColValue(rs, "value_data_type_name");
      return ret.toDocsisDecoding();
    }
  }
  
  private static class DocsisEncodingReader implements ResultSetReader<DocsisEncoding> {
    private static Logger _logger = null;
    public DocsisEncodingReader(Logger logger) {
      _logger = logger;
    }
    public Logger getLogger() {
      return _logger;
    }    
    public DocsisEncoding read(ResultSet rs) throws SQLException {
      Row row = new Row();      
      row.id = WSTools.getColValue(rs, "id");
      row.parentId = WSTools.getColValue(rs, "parent_id");
      row.typeName = WSTools.getColValue(rs, "type_name");
      row.typeEncoding = WSTools.getColValue(rs, "type_encoding");
      row.valueDataTypeName = WSTools.getColValue(rs, "value_data_type_name");
      row.valueDataTypeArguments = WSTools.getColValue(rs, "value_data_type_arguments");
      row.cmtsMicOrder = WSTools.getColValue(rs, "cmts_mic_order");
      DocsisEncoding ret = row.toDocsisEncoding();
      return ret;
    }
  }

  /**
   * returns a list with the relevant columns of all rows of DB table Docsis_Encoding
   */
  public static List<DocsisDecoding> queryDocsisDecoding(Logger logger) throws RemoteException {
    try {
      //List<DocsisDecoding> ret = new ArrayList<DocsisDecoding>();
      String sql = _sqlSelectAllForDecoding;
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      SQLUtilsContainer container = SQLUtilsCache.getForManagement(_schema, logger);
      return new DBCommands<DocsisDecoding>().query(new ReaderForDecoding(), builder, container, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error: " + e.toString());
    }
  }
  

  /**
   * returns a list with the relevant columns of all rows of DB table Docsis_Encoding
   */
  public static List<DocsisEncoding> queryDocsisEncoding(Logger logger) throws RemoteException {
    try {
      //List<DocsisDecoding> ret = new ArrayList<DocsisDecoding>();
      String sql = _sqlDocsisEncodingSelectAll;
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      SQLUtilsContainer container = SQLUtilsCache.getForManagement(_schema, logger);
      return new DBCommands<DocsisEncoding>().query(new DocsisEncodingReader(logger), builder, container, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error: " + e.toString());
    }
  }
  
  
  
  public static void listToLog(List<DocsisDecoding> list, Logger logger) throws RemoteException {
    try {
      for (DocsisDecoding item : list) {
        logger.info(item.toString());
      }
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error: " + e.toString());
    }
  }
  
}

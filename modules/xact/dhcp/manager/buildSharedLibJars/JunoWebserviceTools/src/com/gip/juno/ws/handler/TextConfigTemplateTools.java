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

package com.gip.juno.ws.handler;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import org.apache.log4j.Logger;

import com.gip.juno.cfggen.textconfig.template.TextConfigTemplate;
import com.gip.juno.cfggen.textconfig.template.TextConfigType;
import com.gip.juno.cfggen.util.StringToMapUtil;


import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBCommandsForLocation;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.ResultSetReaderForLocation;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.xyna.utils.db.ResultSetReader;


public class TextConfigTemplateTools {

  private static DBTableInfo _table = getDBTableInfo();
  private static LocationSchema _schema = LocationSchema.service;
  
  public static class Row {
    private String Id;
    private String Type_Name;
    private String Template_Name;
    private String Constraints;
    private String Constraints_Score;
    private String Content;
    
    public void setId(String val) { Id = val; }
    public void setType_Name(String val) { Type_Name = val; }
    public void setTemplate_Name(String val) { Template_Name = val; }
    public void setConstraints(String val) { Constraints = val; }
    public void setConstraints_Score(String val) { Constraints_Score = val; }
    public void setContent(String val) { Content = val; }
    
    public String getId() { return Id; }
    public String getType_Name() { return Type_Name; }
    public String getTemplate_Name() { return Template_Name; }
    public String getConstraints() { return Constraints; }
    public String getConstraints_Score() { return Constraints_Score; }
    public String getContent() { return Content; }
    
    TextConfigTemplate toTextConfigTemplate()  {
      try {
        int id = Integer.parseInt(Id);
        TextConfigType type = TextConfigType.valueOf(Type_Name);
        int constraintsScore = Integer.parseInt(Constraints_Score);
        Map<String, Set<String>> constraints = StringToMapUtil.toMapOfSets(Constraints);
        TextConfigTemplate ret = new TextConfigTemplate(id, type, Template_Name, constraints,
            constraintsScore, Content);
        return ret;
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Error in text config template with id: <" + Id + ">.", e);
      }
    }
  }
  
  private static class DBReader implements ResultSetReader<Row> {
    private Logger _logger;
    public DBReader(Logger logger) {
      _logger = logger;
    }
    public Logger getLogger() {
      return _logger;
    }
    public Row read(ResultSet rs) throws SQLException {
      Row ret = new Row();

      ret.setId(ValueAdapter.getColValueDBToGui(rs, _table, "Id", _logger));
      ret.setType_Name(ValueAdapter.getColValueDBToGui(rs, _table, "Type_Name", _logger));
      ret.setTemplate_Name(ValueAdapter.getColValueDBToGui(rs, _table, "Template_Name", _logger));
      ret.setConstraints(ValueAdapter.getColValueDBToGui(rs, _table, "Constraints", _logger));
      ret.setConstraints_Score(ValueAdapter.getColValueDBToGui(rs, _table, "Constraints_Score", _logger));
      ret.setContent(ValueAdapter.getColValueDBToGui(rs, _table, "Content", _logger));
      return ret;
    }
  }
  
  private static class DBReaderForLocation  extends DBReader implements ResultSetReaderForLocation<Row> {
    public DBReaderForLocation(String location, Logger logger) {
      super(logger);
      _location = location;
    }
    private String _location;
    public String getLocation() { return _location; }
    public void setLocation(String val) { _location = val; }
  }
  
  private static class ReaderForTextConfigTemplate implements ResultSetReader<TextConfigTemplate> {
    private Logger _logger;
    public ReaderForTextConfigTemplate(Logger logger) {
      _logger = logger;
    }
    public Logger getLogger() {
      return _logger;
    }
    public TextConfigTemplate read(ResultSet rs) throws SQLException {
      Row ret = new DBReader(_logger).read(rs);
      return ret.toTextConfigTemplate();
    }
  }
  
  public static DBTableInfo getDBTableInfo() {
    DBTableInfo table = new DBTableInfo("text_config_template", "service");

    table.addColumn(new ColInfo("Id").setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Type_Name").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Template_Name").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Constraints").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Constraints_Score").setType(ColType.integer).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Content").setType(ColType.string).setVisible(true).setUpdates(true).setEndsWithLinebreak());
    return table;
  }

  private static TreeMap<String, String> getRowMap(Row row, Logger logger) throws java.rmi.RemoteException {
       TreeMap<String, String> map = new TreeMap<String, String>();
    ValueAdapter.setColValueGuiToDBInMap(map, "Id", _table, row.getId(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Type_Name", _table, row.getType_Name(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Template_Name", _table, row.getTemplate_Name(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Constraints", _table, row.getConstraints(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Constraints_Score", _table, row.getConstraints_Score(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Content", _table, row.getContent(), logger);
    return map;
  }
  
  public static List<Row> getAllRows(Logger logger) throws java.rmi.RemoteException {
    try {
      DBTableInfo table = _table;
      DBReader reader = new DBReader(logger);
      List<Row> ret = new DBCommandHandler<Row>().getAllRows(reader, table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error in TextConfigTemplateTools. ", e);
      throw new DPPWebserviceDatabaseException("Error in TextConfigTemplateTools. ", e);
    }
  }

  public static Row insertRowOfLocation(Row oneRowRequest, String location, FailoverFlag flag,
          Logger logger) throws java.rmi.RemoteException {
    try {
      DBTableInfo table = _table;
      DBReaderForLocation reader = new DBReaderForLocation(location, logger);
      TreeMap<String, String> map = getRowMap(oneRowRequest,logger);
      return new LocationHandler<Row>().insertRowOfLocation(oneRowRequest, reader, table, map, 
          reader.getLocation(), flag, _schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error in TextConfigTemplateTools. ", e);
      throw new DPPWebserviceDatabaseException("Error in TextConfigTemplateTools. ", e);
    }
  }

  /*
  public static Row insertRowOfLocation(Row oneRowRequest, String location, Logger logger) 
          throws java.rmi.RemoteException {
    return insertRowOfLocation(oneRowRequest, location, logger);
  }
  */
  
  public static void clearTableForLocation(String location, FailoverFlag flag, Logger logger) 
          throws RemoteException {
    try {
       String sql = "DELETE FROM " + _table.getTablename();
       SQLCommand builder = new SQLCommand();
       builder.sql = sql;
       new DBCommandsForLocation<Row>().executeDMLForLocation(location, builder, flag, _schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error in TextConfigTemplateTools. ", e);
      throw new DPPWebserviceDatabaseException("Error in TextConfigTemplateTools. ", e);
    }
  }
  
  public static List<TextConfigTemplate> queryAllTextConfigTemplates(Logger logger) 
          throws java.rmi.RemoteException {
    try {
      DBTableInfo table = _table;
      ReaderForTextConfigTemplate reader = new ReaderForTextConfigTemplate(logger);
      List<TextConfigTemplate> ret = new DBCommandHandler<TextConfigTemplate>().getAllRows(reader, table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error in TextConfigTemplateTools. ", e);
      throw new DPPWebserviceDatabaseException("Error in TextConfigTemplateTools. ", e);
    }
  }
  
  public static void checkTypenameScoreConstraint(String id, String typename, String constraintsScore,  
        Logger logger) throws java.rmi.RemoteException {
    checkTypenameScoreConstraint(id, true, typename, constraintsScore, logger);
  }
  
  public static void checkTypenameScoreConstraint(String typename, String constraintsScore, Logger logger) 
        throws java.rmi.RemoteException {
    checkTypenameScoreConstraint("", false, typename, constraintsScore, logger);    
  }
    
  private static void checkTypenameScoreConstraint(String id, boolean useId, String typename, 
        String constraintsScore, Logger logger) throws java.rmi.RemoteException {
    try {
      String sql = "SELECT COUNT(*) FROM " + _table.getTablename() + " WHERE type_name = ? AND "
          + " constraints_score = ? ";
      if (useId) {
        sql += " AND id != ?";
      }
      String schema = _table.getSchema();
      QueryTools.DBStringReader reader = new QueryTools.DBStringReader();
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      builder.addConditionParam(typename);
      builder.addConditionParam(constraintsScore);
      if (useId) {
        builder.addConditionParam(id);
      }
      String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
      if (ret.trim().equals("0")) {
        return;
      }
      logger.error("Value pair (" + typename + ", " + constraintsScore + 
          ") for columns type_name and constraints_score already exists in table text_config_templates.");
      throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().addParameter(typename)
          .addParameter(constraintsScore).setErrorNumber("00204").setDescription(
          "Value pair for columns type_name and constraints_score already exists in table text_config_templates."));
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error in TextConfigTemplateTools. ", e);
      throw new DPPWebserviceDatabaseException("Error in TextConfigTemplateTools. ", e);
    }
  }
    
}

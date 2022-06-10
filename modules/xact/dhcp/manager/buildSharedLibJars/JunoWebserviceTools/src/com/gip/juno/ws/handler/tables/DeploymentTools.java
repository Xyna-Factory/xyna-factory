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
import java.util.List;
import java.util.TreeMap;


import org.apache.log4j.Logger;

import com.gip.juno.ws.handler.DBCommandHandler;
import com.gip.juno.ws.handler.ValueAdapter;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.xyna.utils.db.ResultSetReader;


public class DeploymentTools {

  private static Logger logger= Logger.getLogger("Deployments");
  private static DBTableInfo _table = getDBTableInfo();

  public static class Row {
    private String Id;
    private String Deploy_Time;
    private String Service;
    private String User;
    private String Log;
    private String Target;

    public void setId(String val) { Id = val; }
    public void setDeploy_Time(String val) { Deploy_Time = val; }
    public void setService(String val) { Service = val; }
    public void setUser(String val) throws RemoteException { try { User = MultiUserTools.lookupUserForSession(val); }
                                                             catch (Throwable t) { User = val;}}
    public void setUserDirectly(String val) { User = val; }
    public void setLog(String val) { Log = val; }
    public void setLog(boolean success) { Log = success ? "Success" : "Failed"; }
    public void setLog(Throwable t) { Log = "Failed: " +  MessageBuilder.stackTraceToString(t); }
    public void setTarget(String val) { Target = val; }

    public String getId() { return Id; }
    public String getDeploy_Time() { return Deploy_Time; }
    public String getService() { return Service; }
    public String getUser () { return User; }
    public String getLog() { return Log; }
    public String getTarget() { return Target; }
  }

  private static class DBReader implements ResultSetReader<Row> {
    public Logger getLogger() {
      return logger;
    }
    public Row read(ResultSet rs) throws SQLException {
      Row ret = new Row();

      ret.setId(ValueAdapter.getColValueDBToGui(rs, _table, "Id", logger));
      ret.setDeploy_Time(ValueAdapter.getColValueDBToGui(rs, _table, "Deploy_Time", logger));
      ret.setService(ValueAdapter.getColValueDBToGui(rs, _table, "Service", logger));
      ret.setUserDirectly(ValueAdapter.getColValueDBToGui(rs, _table, "User", logger));
      ret.setLog(ValueAdapter.getColValueDBToGui(rs, _table, "Log", logger));
      ret.setTarget(ValueAdapter.getColValueDBToGui(rs, _table, "Target", logger));
      return ret;
    }
  }

  public static DBTableInfo getDBTableInfo() {
    DBTableInfo table = new DBTableInfo("deploy_actions", "deployments");

    table.addColumn(new ColInfo("Id").setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Deploy_Time").setType(ColType.time).setVisible(true).setUpdates(true)
        .setInsertCurrentTime());
    table.addColumn(new ColInfo("Service").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("User").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Log").setType(ColType.string).setVisible(true).setUpdates(true).setEndsWithLinebreak());
    table.addColumn(new ColInfo("Target").setType(ColType.string).setVisible(true).setUpdates(true));
    return table;
  }

  private static TreeMap<String, String> getRowMap(Row row) throws java.rmi.RemoteException {
       TreeMap<String, String> map = new TreeMap<String, String>();
    ValueAdapter.setColValueGuiToDBInMap(map, "Id", _table, row.getId(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Deploy_Time", _table, row.getDeploy_Time(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Service", _table, row.getService(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "User", _table, row.getUser(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Log", _table, row.getLog(), logger);
    ValueAdapter.setColValueGuiToDBInMap(map, "Target", _table, row.getTarget(), logger);
    return map;
  }

  public static List<Row> getAllRows() throws java.rmi.RemoteException {
    DBTableInfo table = _table;
    DBReader reader = new DBReader();
    List<Row> ret = new DBCommandHandler<Row>().getAllRows(reader, table, logger);
    return ret;
  }

  public static synchronized Row insertRow(Row oneRowRequest)
          throws java.rmi.RemoteException {
    DBTableInfo table = _table;
    DBReader reader = new DBReader();
    TreeMap<String, String> map = getRowMap(oneRowRequest);
    int retries = 0;
    Row insert = null;
    while (retries < 10) {
      try {
        insert = new DBCommandHandler<Row>().insertRow(oneRowRequest, reader, table, map, logger);
        return insert;
      } catch (Throwable t) {
        map.put("Id", null);
        retries++;
        Thread.yield();
        if (retries >= 10) {
          throw new DPPWebserviceException(t);
        }
      }
    }
    return insert;
  }


  public static void clearTable() throws RemoteException {
     DBTableInfo table = _table;
     String sql = "DELETE FROM " + table.getTablename();
     SQLCommand builder = new SQLCommand();
     builder.sql = sql;
     DBCommands.executeDML(table, builder, logger);
  }

}

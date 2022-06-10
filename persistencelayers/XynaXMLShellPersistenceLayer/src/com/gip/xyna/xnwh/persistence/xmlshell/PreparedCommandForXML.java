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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


public class PreparedCommandForXML implements PreparedCommand {

  private Command command;
  private String tableSuffix;
  private PreparedQueryForXML preparedSelectQueryForAffectedRows;


  public String getTable() {
    return this.command.getTable();
  }


  public PreparedCommandForXML(Command cmd, String tableSuffix, int grepTimoutInSeconds)
      throws PersistenceLayerException {
    this.command = cmd;
    this.tableSuffix = tableSuffix;
    preparedSelectQueryForAffectedRows = parseSQLString(cmd.getSqlString(), grepTimoutInSeconds);
  }


  //delete from table where ...
  private PreparedQueryForXML parseSQLString(String sqlString, int grepTimoutInSeconds)
      throws PersistenceLayerException {

    Pattern pat = Pattern.compile("^delete from (.*?) where ");
    Matcher mat = pat.matcher(sqlString);
    if (mat.find()) {
      sqlString = sqlString.substring(mat.end());
    } else {
      throw new XNWH_GeneralPersistenceLayerException("invalid DML given: " + sqlString);
    }
    sqlString = "select x, y, z from " + command.getTable() + " where " + sqlString;
    PreparedQueryForXML query = null;
    try {
      query =
          new PreparedQueryForXML(new Query(sqlString, new MyStorable.MyResultReader()), tableSuffix,
                                  grepTimoutInSeconds);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new XNWH_GeneralPersistenceLayerException("Could not generate query", e);
    }
    return query;
  }


  public Set<String> execute(Parameter parameter, Set<String> allSet, ExecutorService grepExecutor)
      throws PersistenceLayerException, GrepException {
    try {
      return preparedSelectQueryForAffectedRows.execute(parameter, allSet);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new XNWH_GeneralPersistenceLayerException("Query missfired", e);
    }
  }


  @Persistable(primaryKey = "id", tableName = "tmp")
  private static class MyStorable extends Storable<MyStorable> {

    public MyStorable() {
    }


    @Override
    public Object getPrimaryKey() {
      return "";
    }


    @Override
    public String getTableName() {
      return "";
    }


    @Override
    public ResultSetReader<? extends MyStorable> getReader() {
      return new MyResultReader();
    }


    @Override
    public <U extends MyStorable> void setAllFieldsFromData(U data) {
    }


    private static class MyResultReader implements ResultSetReader<MyStorable> {


      public MyResultReader() {
      }


      public MyStorable read(ResultSet rs) throws SQLException {
        return null;
      }
    }
  }
}

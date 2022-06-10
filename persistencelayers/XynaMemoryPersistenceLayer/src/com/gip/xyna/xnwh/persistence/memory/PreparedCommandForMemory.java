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
package com.gip.xyna.xnwh.persistence.memory;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



/**
 * FIXME workaround und nicht schön!
 */
public class PreparedCommandForMemory implements PreparedCommand {

  private Command cmd;


  public static enum CommandType {
    insert, update, delete;
  }


  private PreparedQueryForMemory preparedSelectQueryForAffectedRows;
  private static final Pattern deletePattern = Pattern.compile("^delete (.*)$", Pattern.CASE_INSENSITIVE);


  public PreparedCommandForMemory(Command cmd, TableObject table, PersistenceLayerConnection connection)
      throws PersistenceLayerException {

    this.cmd = cmd;
    if (cmd.getSqlString().startsWith("delete")) {

      Matcher m = deletePattern.matcher(cmd.getSqlString());
      if (!m.matches()) {
        throw new XNWH_GeneralPersistenceLayerException("Invalid delete command");
      }
      String sqlStringFromAndWhere = m.group(1);

      StringBuilder sqlSelectString = new StringBuilder().append("select ");
      ColumnDeclaration columnDeclarationPK = null;
      for (ColumnDeclaration declaration : table.getColTypes()) {
        if (declaration.isPrimaryKey()) {
          sqlSelectString.append(declaration.getName());
          columnDeclarationPK = declaration;
          break;
        }
      }
      if (columnDeclarationPK == null)
        throw new XNWH_GeneralPersistenceLayerException("Could not determine primary key");
      sqlSelectString.append(" ").append(sqlStringFromAndWhere);
      sqlSelectString.append(" order by " + table.getNameOfPrimaryKey());
      preparedSelectQueryForAffectedRows =
          (PreparedQueryForMemory) connection.prepareQuery(new Query(sqlSelectString.toString(),
                                                                     new MyStorable(null, table.getName())
                                                                         .getReader(columnDeclarationPK)));
    }

  }


  public CommandType getCommandType() throws PersistenceLayerException {
    if (cmd.getSqlString().startsWith("insert")) {
      return CommandType.insert;
    } else if (cmd.getSqlString().startsWith("delete")) {
      return CommandType.delete;
    } else if (cmd.getSqlString().startsWith("update")) {
      return CommandType.update;
    }
    throw new XNWH_GeneralPersistenceLayerException("Commandtype could not be identified in sql command: " + cmd.getSqlString());
  }


  public Command getCommand() {
    return cmd;
  }
  
  public String getTable() {
    return cmd.getTable();
  }

  public PreparedQueryForMemory getPreparedSelectQueryForAffectedRows() {
    return preparedSelectQueryForAffectedRows;
  }


  @Persistable(primaryKey = "id", tableName = "tmp")
  private static class MyStorable extends Storable<MyStorable> {

    @Column(name = "id")
    protected Object id;
    private String tableName;


    public MyStorable(Object pk, String tableName) {
      this.id = pk;
      this.tableName = tableName;
    }


    @Override
    public Object getPrimaryKey() {
      return id;
    }


    @Override
    public String getTableName() {
      return tableName;
    }


    @Override
    public ResultSetReader<? extends MyStorable> getReader() {
      return null;
    }


    public ResultSetReader<MyStorable> getReader(ColumnDeclaration colDeclaration) {
      return new MyResultReader(colDeclaration, tableName);
    }


    @Override
    public <U extends MyStorable> void setAllFieldsFromData(U data) {
      MyStorable cast = data;
      id = cast.id;
    }


    private static class MyResultReader implements ResultSetReader<MyStorable> {

      ColumnDeclaration colDeclaration = null;
      String tableName;


      public MyResultReader(ColumnDeclaration colDeclaration, String tableName) {
        this.colDeclaration = colDeclaration;
        this.tableName = tableName;
      }


      public MyStorable read(ResultSet rs) throws SQLException {
        if (colDeclaration.getJavaType().toLowerCase().startsWith("int")) {
          return new MyStorable(rs.getInt(colDeclaration.getName()), tableName);
        } else if (colDeclaration.getJavaType().toLowerCase().equals("long")) {
          return new MyStorable(rs.getLong(colDeclaration.getName()), tableName);
        } else if (colDeclaration.getJavaType().toLowerCase().endsWith("string")) {
          return new MyStorable(rs.getString(colDeclaration.getName()), tableName);
        } else {
          throw new SQLException("unsupported col type of primary key: " + colDeclaration.getJavaType());
        }
      }

    }

  }


  public String toString() {
    return cmd.getSqlString();
  }
}

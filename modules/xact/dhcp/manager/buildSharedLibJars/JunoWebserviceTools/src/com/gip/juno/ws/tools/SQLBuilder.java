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

package com.gip.juno.ws.tools;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.dhcpv6.HostHandler;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.Pk;
import com.gip.juno.ws.enums.VirtualCol;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;

/**
 * class that builds an SQL-string and jdbc-parameters for a database command
 */
public class SQLBuilder {

  public static SQLCommand buildSQLInsert(TreeMap<String, String> map, DBTableInfo table)
        throws java.rmi.RemoteException{
    StringBuilder sql = new StringBuilder("INSERT INTO ");
    sql.append(table.getTablename() + " ( ");
    StringBuilder values = new StringBuilder(" VALUES (");
    SQLCommand builder = new SQLCommand();
    boolean noValueYet = true;
    boolean isnull = false;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
        isnull = false;
      } else {
        isnull = true;
      }
      if (table.getColumns().containsKey(entry.getKey())) {
        ColInfo col = table.getColumns().get(entry.getKey());
        if (isnull && (col.ifNoVal == IfNoVal.IgnoreColumn)) {
            // do nothing
        } else {
          if (noValueYet) {
            noValueYet = false;
          } else {
            sql.append(", ");
            values.append(", ");
          }
          String colname = col.dbname;
          sql.append(colname);
          if ((isnull) && (col.type == ColType.string) && (col.ifNoVal == IfNoVal.EmptyString)) {
            values.append(" '' ");
          } else if ((isnull) && (col.ifNoVal == IfNoVal.DefaultValue)) {
            builder.addParam(new ColName(colname), new ColStrValue(col.defaultValue), col.type);
            values.append(" ? ");
          } else if ((col.type == ColType.time) && (col.insertCurrentTime)) {
            values.append(" NOW() ");
          } else if (isnull) {
              values.append("NULL ");
          } else if (col.type == ColType.binaryhex) {
            builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            values.append("UNHEX(?) ");
          } else {
            builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            values.append(" ? ");
          }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");

      }
    }
    if (noValueYet) { return builder; }
    values.append(") ");
    sql.append(") " + values.toString());
    builder.sql = sql.toString();
    return builder;
  }

  /**
   * builds SQL update command where the primary key values are the conditions
   * and the primary key values are not updated;
   * empty values for primary keys are used for condition, too.
   */
  public static SQLCommand buildSQLUpdate(TreeMap<String, String> map, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("UPDATE ");
    sql.append(table.getTablename() + " SET ");
    StringBuilder condition = new StringBuilder("");
    SQLCommand builder = new SQLCommand();
    boolean noConditionYet = true;
    boolean noValueYet = true;
    boolean isnull = false;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
          ColInfo col = table.getColumns().get(entry.getKey());
          String colname = col.dbname;
          if (col.pk == Pk.True) {
            if (!isnull) {
              if (noConditionYet) {
                noConditionYet = false;
                condition.append(" WHERE ");
              } else {
                condition.append(" AND ");
              }
              builder.addConditionParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              if (col.type == ColType.binaryhex) {
                condition.append(" " + colname + " = UNHEX(?) ");
              } else {
                condition.append(" " + colname + " = ? ");
              }
            }
          } else if (isnull && (col.ifNoVal == IfNoVal.IgnoreColumn)) {
            // do nothing
          } else {
            if (noValueYet) {
              noValueYet = false;
            } else {
              sql.append(", ");
            }
            if ((isnull) && (col.type == ColType.string) && (col.ifNoVal == IfNoVal.EmptyString)) {
              sql.append(colname + " = '' ");
            } else if (isnull) {
              sql.append(colname + " = NULL ");
            } else if (col.type == ColType.binaryhex) {
              builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              sql.append(colname + " = UNHEX(?) ");
            } else {
              builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              sql.append(colname + " = ? ");
            }
          }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }
    if (noConditionYet) { return builder; }
    if (noValueYet) { return builder; }
    if (condition.toString().equals("")) { return builder; }
    sql.append(condition.toString());
    builder.sql = sql.toString();
    return builder;
  }

  public static SQLCommand buildSQLSelectAll(DBTableInfo table) throws java.rmi.RemoteException {
    return buildSQLSelectAll(table, true);
  }

  public static SQLCommand buildSQLSelectAll(DBTableInfo table, boolean useLimit) throws java.rmi.RemoteException {
    TreeMap<String, String> map = new TreeMap<String, String>();
    for (Map.Entry<String, ColInfo> entry : table.getColumns().entrySet()) {
      ColInfo col = table.getColumns().get(entry.getKey());
      if (col.virtual == VirtualCol.False) {
        map.put(entry.getKey(), "");
      }
    }
    return buildSQLSelectWhere(map, table, useLimit);
  }

  public static SQLCommand buildSQLSelectWhere(TreeMap<String, String> map, DBTableInfo table)
        throws java.rmi.RemoteException {
    return buildSQLSelectWhere(map, table, true);
  }

  public static SQLCommand buildSQLSelectWhere(TreeMap<String, String> map, DBTableInfo table, boolean useLimit)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("SELECT ");
    StringBuilder condition = new StringBuilder("");
    SQLCommand sqlBuilder = new SQLCommand();
    boolean noConditionYet = true;
    boolean noColYet = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if (noColYet) {
          noColYet = false;
        } else {
          sql.append(", ");
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if (col.type == ColType.binaryhex) {
          sql.append("HEX(" + colname + ") AS " + colname);
        } else {
          sql.append(colname);
        }
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          if (noConditionYet) {
            condition.append(" WHERE ");
            noConditionYet = false;
          } else {
            condition.append(" AND ");
          }
          if (col.type == ColType.binaryhex) {
            if (WSTools.hasWildcard(entry.getValue())) {
              String colval = entry.getValue();
              colval = WSTools.adjustWildcard(colval);
              sqlBuilder.addParam(new ColName(colname), new ColStrValue(colval), col.type);
              condition.append(" HEX( " + colname + ")  LIKE ? ");
            } else {
              sqlBuilder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              condition.append(colname + " = UNHEX(?) ");
            }
          } else if (WSTools.hasWildcard(entry.getValue())) {
            String colval = entry.getValue();
            colval = WSTools.adjustWildcard(colval);
            sqlBuilder.addParam(new ColName(colname), new ColStrValue(colval), col.type);
            condition.append(colname + " LIKE ? ");
          } else {
            sqlBuilder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            condition.append(colname + " = ? ");
          }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }
    sql.append(" FROM ");
    sql.append(table.getTablename());
    sql.append(condition.toString());
    if (useLimit) {
      sql.append(" LIMIT 999");
      //sql.append(" LIMIT 999999");
      //sql.append(" LIMIT 9999");
    }
    sqlBuilder.sql = sql.toString();
    return sqlBuilder;
  }
  
  
  public static SQLCommand buildSQLSelectAllWhere(TreeMap<String, String> map, DBTableInfo table, boolean useLimit)
                  throws java.rmi.RemoteException {
    TreeMap<String, String> selectionColumns = new TreeMap<String, String>();
    for (Map.Entry<String, ColInfo> entry : table.getColumns().entrySet()) {
      ColInfo col = table.getColumns().get(entry.getKey());
      if (col.virtual == VirtualCol.False) {
        selectionColumns.put(entry.getKey(), "");
      }
    }
    StringBuilder sql = new StringBuilder("SELECT ");
    boolean noColYet = true;
    for (Map.Entry<String, String> entry : selectionColumns.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if (noColYet) {
          noColYet = false;
        } else {
          sql.append(", ");
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if (col.type == ColType.binaryhex) {
          sql.append("HEX(" + colname + ") AS " + colname);
        } else {
          sql.append(colname);
        }
      }
    }
    StringBuilder condition = new StringBuilder("");
    SQLCommand sqlBuilder = new SQLCommand();
    boolean noConditionYet = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if ((entry.getValue() != null) && (!entry.getValue().equals(""))) {
          if (noConditionYet) {
            condition.append(" WHERE ");
            noConditionYet = false;
          } else {
            condition.append(" AND ");
          }
          
          if (col.type == ColType.binaryhex) {
            if (WSTools.hasWildcard(entry.getValue())) {
              String colval = entry.getValue();
              colval = WSTools.adjustWildcard(colval);
              sqlBuilder.addParam(new ColName(colname), new ColStrValue(colval), col.type);
              condition.append(" HEX( " + colname + ")  LIKE ? ");
            } else {
              sqlBuilder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              condition.append(colname + " = UNHEX(?) ");
            }
          } else if (WSTools.hasWildcard(entry.getValue())) {
            String colval = entry.getValue();
            colval = WSTools.adjustWildcard(colval);
            sqlBuilder.addParam(new ColName(colname), new ColStrValue(colval), col.type);
            condition.append(colname + " LIKE ? ");
          } else {
            sqlBuilder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            condition.append(colname + " = ? ");
          }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name (" + entry.getKey() + ")!");
      }
    }
    sql.append(" FROM ");
    sql.append(table.getTablename());
    sql.append(condition.toString());
    if (useLimit) {
      sql.append(" LIMIT 999");
    }
    sqlBuilder.sql = sql.toString();
    return sqlBuilder;
  }


  /**
   * Builds SQL delete command;
   * Null or empty values in "map" parameter will be ignored for condition
   */
  public static SQLCommand buildSQLDelete(TreeMap<String, String> map, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("DELETE FROM ");
    StringBuilder condition = new StringBuilder("");
    SQLCommand builder = new SQLCommand();
    boolean noConditionYet = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          ColInfo col = table.getColumns().get(entry.getKey());
          String colname = col.dbname;
          if (noConditionYet) {
            condition.append(" WHERE ");
            noConditionYet = false;
          } else {
            condition.append(" AND ");
          }
          builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
          if (col.type == ColType.binaryhex) {
            condition.append(colname + " = UNHEX(?) ");
          } else {
            condition.append(colname + " = ? ");
          }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }
    sql.append(table.getTablename() + condition.toString());
    builder.sql = sql.toString();
    return builder;
  }


  /**
   * Builds SQL delete command;
   * Null or empty values in "map" parameter will be used in condition and match NULL values
   */
  public static SQLCommand buildSQLDeleteWithNullConditions(TreeMap<String, String> map, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("DELETE FROM ");
    StringBuilder condition = new StringBuilder("");
    SQLCommand builder = new SQLCommand();
    boolean noConditionYet = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if (noConditionYet) {
          condition.append(" WHERE ");
          noConditionYet = false;
        } else {
          condition.append(" AND ");
        }
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
          if (col.type == ColType.binaryhex) {
            condition.append(colname + " = UNHEX(?) ");
          } else {
            condition.append(colname + " = ? ");
          }
        } else {
          condition.append(colname + " IS NULL ");
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }
    sql.append(table.getTablename() + condition.toString());
    builder.sql = sql.toString();
    return builder;
  }


  public static String buildSQLGetColValuesDistinct(String colname, DBTableInfo table) {
    StringBuilder sql = new StringBuilder("SELECT DISTINCT ");
    sql.append(colname);
    sql.append(" FROM " + table.getTablename() + " ");
    return sql.toString();
  }


  /**
   * builds SQL update command;
   * parameter conditionmap contains values for condition;
   * nulls or empty strings in conditionmap will not be used for the condition;
   * null or empty strings in newvalmap will be used as new values
   */
  public static SQLCommand buildSQLUpdatePk(TreeMap<String, String> conditionmap,
        TreeMap<String, String> newvalmap, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("UPDATE ");
    SQLCommand builder = new SQLCommand();
    sql.append(table.getTablename() + " SET ");
    boolean noValueYet = true;
    boolean isnull = false;
    for (Map.Entry<String, String> entry : newvalmap.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        if (isnull && (col.ifNoVal == IfNoVal.IgnoreColumn)) {
          // do nothing
        } else {
          String colname = col.dbname;
            if (noValueYet) {
              noValueYet = false;
            } else {
              sql.append(", ");
            }
            if ((isnull) && (col.type == ColType.string) && (col.ifNoVal == IfNoVal.EmptyString)) {
              sql.append(colname + " = '' ");
            } else if (isnull) {
              sql.append(colname + " = NULL ");
            } else if (col.type == ColType.binaryhex) {
              builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              sql.append(colname + " = UNHEX(?) ");
            } else {
              builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              sql.append(colname + " = ? ");
            }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }

    StringBuilder condition = new StringBuilder("");
    boolean noConditionYet = true;
    isnull = false;
    for (Map.Entry<String, String> entry : conditionmap.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if (!isnull) {
            if (noConditionYet) {
              noConditionYet = false;
              condition.append(" WHERE ");
            } else {
              condition.append(" AND ");
            }
            builder.addConditionParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            if (col.type == ColType.binaryhex) {
              condition.append(" " + colname + " = UNHEX(?) ");
            } else {
              condition.append(" " + colname + " = ? ");
            }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }

    if (noConditionYet) { return builder; }
    if (noValueYet) { return builder; }
    if (condition.toString().equals("")) { return builder; }
    sql.append(condition.toString());
    builder.sql = sql.toString();
    return builder;
  }



  /**
   * builds SQL update command;
   * parameter conditionmap contains values for condition;
   * nulls or empty strings in conditionmap will not be used for the condition;
   * null or empty strings in newvalmap mean that the old values are not overwritten
   */
  public static SQLCommand buildSQLUpdatePkIgnoreEmpty(TreeMap<String, String> conditionmap,
        TreeMap<String, String> newvalmap, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("UPDATE ");
    SQLCommand builder = new SQLCommand();
    sql.append(table.getTablename() + " SET ");
    boolean noValueYet = true;
    boolean isnull = false;
    for (Map.Entry<String, String> entry : newvalmap.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        String newval = entry.getValue();
        if ((newval != null)
            && (!newval.equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
        if (!isnull) {
          ColInfo col = table.getColumns().get(entry.getKey());
          String colname = col.dbname;
            if (noValueYet) {
              noValueYet = false;
            } else {
              sql.append(", ");
            }
            if (col.type == ColType.binaryhex) {
              builder.addParam(new ColName(colname), new ColStrValue(newval), col.type);
              sql.append(colname + " = UNHEX(?) ");
            } else if ((newval.trim().equals("NULL")) && (col.ifNoVal == IfNoVal.Null)) {
              sql.append(colname + " = NULL ");
            } else {
              builder.addParam(new ColName(colname), new ColStrValue(newval), col.type);
              sql.append(colname + " = ? ");
            }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }

    StringBuilder condition = new StringBuilder("");
    boolean noConditionYet = true;
    isnull = false;
    for (Map.Entry<String, String> entry : conditionmap.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if (!isnull) {
            if (noConditionYet) {
              noConditionYet = false;
              condition.append(" WHERE ");
            } else {
              condition.append(" AND ");
            }
            builder.addConditionParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            if (col.type == ColType.binaryhex) {
              condition.append(" " + colname + " = UNHEX(?) ");
            } else {
              condition.append(" " + colname + " = ? ");
            }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }

    if (noConditionYet) { return builder; }
    if (noValueYet) { return builder; }
    if (condition.toString().equals("")) { return builder; }
    sql.append(condition.toString());
    builder.sql = sql.toString();
    return builder;
  }


  public static SQLCommand buildSQLCountStar(DBTableInfo table)
        throws java.rmi.RemoteException {
    TreeMap<String, String> map = new TreeMap<String, String>();
    for (Map.Entry<String, ColInfo> entry : table.getColumns().entrySet()) {
      ColInfo col = table.getColumns().get(entry.getKey());
      if (col.virtual == VirtualCol.False) {
        map.put(entry.getKey(), "");
      }
    }
    return buildSQLCountStarWhere(map, table);
  }

  public static SQLCommand buildSQLCountStarWhere(TreeMap<String, String> map, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) ");
    StringBuilder condition = new StringBuilder("");
    SQLCommand sqlBuilder = new SQLCommand();
    boolean noConditionYet = true;
    boolean noColYet = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if (noColYet) {
          noColYet = false;
        } else {}
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;

        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          if (noConditionYet) {
            condition.append(" WHERE ");
            noConditionYet = false;
          } else {
            condition.append(" AND ");
          }
          if (col.type == ColType.binaryhex) {
            if (WSTools.hasWildcard(entry.getValue())) {
              String colval = entry.getValue();
              colval = WSTools.adjustWildcard(colval);
              sqlBuilder.addParam(new ColName(colname), new ColStrValue(colval), col.type);
              condition.append(" HEX( " + colname + ")  LIKE ? ");
            } else {
              sqlBuilder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              condition.append(colname + " = UNHEX(?) ");
            }
          } else if (WSTools.hasWildcard(entry.getValue())) {
            String colval = entry.getValue();
            colval = WSTools.adjustWildcard(colval);
            sqlBuilder.addParam(new ColName(colname), new ColStrValue(colval), col.type);
            condition.append(colname + " LIKE ? ");
          } else {
            sqlBuilder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            condition.append(colname + " = ? ");
          }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey()+")!");
      }
    }
    sql.append(" FROM ");
    sql.append(table.getTablename());
    sql.append(condition.toString());
    sqlBuilder.sql = sql.toString();
    return sqlBuilder;
  }

  public static SQLCommand buildSQLCountColValue(DBTableInfo table, String colName, String colValue) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) ");
    StringBuilder condition = new StringBuilder("");
    SQLCommand command = new SQLCommand();
    sql.append(" FROM ");
    sql.append(table.getTablename());
    condition.append(" WHERE ");
    condition.append(colName);
    condition.append(" = ?");
    command.addConditionParam(colValue);
    sql.append(condition.toString());
    command.sql = sql.toString();
    return command;
  }


  /**
   * adds parameters and sql string of sql where condition that describes match with map values;
   * leading "WHERE" will be omitted in string
   */
  public static void addSQLNonEmptyColsCondition(Map<String, String> map, DBTableInfo table, SQLCommand command)
        throws java.rmi.RemoteException {
    StringBuilder condition = new StringBuilder("");
    boolean noConditionYet = true;
    boolean noColYet = true;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if (noColYet) {
          noColYet = false;
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          if (noConditionYet) {
            //condition.append(" WHERE ");
            noConditionYet = false;
          } else {
            condition.append(" AND ");
          }
          if (col.type == ColType.binaryhex) {
            if (WSTools.hasWildcard(entry.getValue())) {
              String colval = entry.getValue();
              colval = WSTools.adjustWildcard(colval);
              command.addParam(new ColName(colname), new ColStrValue(colval), col.type);
              condition.append(" HEX( " + colname + ")  LIKE ? ");
            } else {
              command.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              condition.append(colname + " = UNHEX(?) ");
            }
          } else if (WSTools.hasWildcard(entry.getValue())) {
            String colval = entry.getValue();
            colval = WSTools.adjustWildcard(colval);
            command.addParam(new ColName(colname), new ColStrValue(colval), col.type);
            condition.append(colname + " LIKE ? ");
          } else {
            command.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            condition.append(colname + " = ? ");
          }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+ entry.getKey() +") !");
      }
    }
    command.sql += condition.toString();
  }


  /**
   * adds condition to parameter SQLCommand;
   * "WHERE" will be omitted in sql string;
   * only values in map which belong to pk-rows will be used for condition
   */
  public static void addSQLPkColsCondition(TreeMap<String, String> map, DBTableInfo table, SQLCommand command)
        throws java.rmi.RemoteException {
    StringBuilder condition = new StringBuilder("");
    boolean noConditionYet = true;
    boolean isnull = false;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
          ColInfo col = table.getColumns().get(entry.getKey());
          String colname = col.dbname;
          if (col.pk == Pk.True) {
            if (!isnull) {
              if (noConditionYet) {
                noConditionYet = false;
                //condition.append(" WHERE ");
              } else {
                condition.append(" AND ");
              }
              command.addConditionParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              if (col.type == ColType.binaryhex) {
                condition.append(" " + colname + " = UNHEX(?) ");
              } else {
                condition.append(" " + colname + " = ? ");
              }
            }
          }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }
    if (noConditionYet) { return; }
    if (condition.toString().equals("")) { return; }
    command.sql += condition.toString();
  }



  /**
   * builds SQL update command;
   * parameter conditionmap contains values for condition;
   * nulls or empty strings in conditionmap will actually be used for the condition;
   * null or empty strings in newvalmap will be used as new values
   */
  public static SQLCommand buildSQLUpdatePkWithNullConditions(TreeMap<String, String> conditionmap,
        TreeMap<String, String> newvalmap, DBTableInfo table)
        throws java.rmi.RemoteException {
    StringBuilder sql = new StringBuilder("UPDATE ");
    SQLCommand builder = new SQLCommand();
    sql.append(table.getTablename() + " SET ");
    boolean noValueYet = true;
    boolean isnull = false;
    for (Map.Entry<String, String> entry : newvalmap.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        if (isnull && (col.ifNoVal == IfNoVal.IgnoreColumn)) {
          // do nothing
        } else {
          String colname = col.dbname;
            if (noValueYet) {
              noValueYet = false;
            } else {
              sql.append(", ");
            }
            if ((isnull) && (col.type == ColType.string) && (col.ifNoVal == IfNoVal.EmptyString)) {
              sql.append(colname + " = '' ");
            } else if (isnull) {
              sql.append(colname + " = NULL ");
            } else if (col.type == ColType.binaryhex) {
              builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              sql.append(colname + " = UNHEX(?) ");
            } else {
              builder.addParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
              sql.append(colname + " = ? ");
            }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }

    StringBuilder condition = new StringBuilder("");
    boolean noConditionYet = true;
    isnull = false;
    for (Map.Entry<String, String> entry : conditionmap.entrySet()) {
      if (table.getColumns().containsKey(entry.getKey())) {
        if ((entry.getValue() != null)
            && (!entry.getValue().equals(""))) {
          isnull = false;
        } else {
          isnull = true;
        }
        ColInfo col = table.getColumns().get(entry.getKey());
        String colname = col.dbname;

        if (noConditionYet) {
          noConditionYet = false;
          condition.append(" WHERE ");
        } else {
          condition.append(" AND ");
        }
        if (isnull) {
          condition.append(" " + colname + " IS NULL ");
        } else {
          builder.addConditionParam(new ColName(colname), new ColStrValue(entry.getValue()), col.type);
            if (col.type == ColType.binaryhex) {
              condition.append(" " + colname + " = UNHEX(?) ");
            } else {
              condition.append(" " + colname + " = ? ");
            }
        }
      } else {
        throw new DPPWebserviceDatabaseException("DBTableInfo: Wrong column name ("+entry.getKey() + ")!");
      }
    }
    if (noValueYet) { return builder; }
    sql.append(condition.toString());
    builder.sql = sql.toString();
    return builder;
  }
  
  
  public static void main(String... args) throws RemoteException {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("HostID", "123");
    map.put("tat", "1234");
    //SQLCommand c = buildSQLSelectAllWhere(map, new HostHandler().getDBTableInfo(), true);
    //System.out.println(c.sql);
    SQLCommand c = new SQLCommand();
    c.sql = "Select baum where";
    DBTableInfo table = new DBTableInfo("arg", "schema");
    table.addColumn(new ColInfo("tat").setPk());
    table.addColumn(new ColInfo("HostID"));
    SQLBuilder.addSQLPkColsCondition(map, table, c);
    System.out.println(c.sql);
    System.out.println(c.buildParameter());
  }


}

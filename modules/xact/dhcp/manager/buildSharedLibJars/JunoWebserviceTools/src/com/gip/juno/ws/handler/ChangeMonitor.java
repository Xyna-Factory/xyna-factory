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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.handler.ReflectionTools.DBReader;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;


public class ChangeMonitor<T> {

  public static class Constant {
    public static final String MANAGEMENT = "Verwaltung";
  }

  TableHandler _handler = null;
  Logger _logger = null;


  public ChangeMonitor(TableHandler handler, Logger logger) {
    _handler = handler;
    _logger = logger;
  }

  public T queryOneRow(T condition) throws RemoteException {
    DBTableInfo table = _handler.getDBTableInfo();
    DBReader<T> reader = new ReflectionTools.DBReader<T>(table, condition, _logger);
    TreeMap<String, String> map = new ReflectionTools<T>(condition).getRowMap(table, condition, _logger);
    _logger.warn("ChangeMonitor queryOneRow build: " + map);
    return new DBCommands<T>().queryOneRow(reader, table, map, _logger);
  }

  public T queryRowToUpdate(T newRow) throws RemoteException {
    DBTableInfo table = _handler.getDBTableInfo();
    DBReader<T> reader = new ReflectionTools.DBReader<T>(table, newRow, _logger);
    TreeMap<String, String> map = new ReflectionTools<T>(newRow).getRowMapNonPkValuesEmpty(table, newRow, _logger);
    return new DBCommands<T>().queryOneRow(reader, table, map, _logger);
  }


  public String buildInsertStringPkOnly(T row) throws RemoteException {
    return buildInsertString(row, true);
  }

  public String buildInsertString(T row) throws RemoteException {
    return buildInsertString(row, false);
  }


  public String buildInsertString(T row, boolean pkOnly) throws RemoteException {
    if (row == null) {
      return "null";
    }
    return buildRowString(row, "INSERTED", pkOnly);
  }


  public String buildDeleteStringPkOnly(T row) throws RemoteException {
    return buildDeleteString(row, true);
  }

  public String buildDeleteString(T row) throws RemoteException {
    return buildDeleteString(row, false);
  }

  public String buildDeleteString(T row, boolean pkOnly) throws RemoteException {
    if (row == null) {
      return "null";
    }
    return buildRowString(row, "DELETED", pkOnly);
  }


  public String buildUpdateString(T oldRow, T newRow) throws RemoteException {
    return buildUpdateString(oldRow, newRow, false);
  }

  public String buildUpdateStringPkOnly(T oldRow, T newRow) throws RemoteException {
    return buildUpdateString(oldRow, newRow, true);
  }

  public String buildUpdateString(T oldRow, T newRow, boolean pkOnly) throws RemoteException {
    if (oldRow == null) {
      return "null";
    }
    if (newRow == null) {
      return "null";
    }
    StringBuilder s = new StringBuilder("UPDATED: ");
    s.append(buildRowString(oldRow, "BEFORE", pkOnly));
    s.append(buildRowString(newRow, ", AFTER", pkOnly));
    return s.toString();
  }


  public String buildUpdateStringMultipleRowsIgnoreEmpty(T condition, T newRow) throws RemoteException {
    return buildUpdateStringMultipleRows(condition, newRow, false, true);
  }

  public String buildUpdateStringMultipleRowsPkOnlyIgnoreEmpty(T condition, T newRow) throws RemoteException {
    return buildUpdateStringMultipleRows(condition, newRow, true, true);
  }


  public String buildUpdateStringMultipleRows(T condition, T newRow) throws RemoteException {
    return buildUpdateStringMultipleRows(condition, newRow, false, false);
  }

  public String buildUpdateStringMultipleRowsPkOnly(T condition, T newRow) throws RemoteException {
    return buildUpdateStringMultipleRows(condition, newRow, true, false);
  }


  private String buildUpdateStringMultipleRows(T condition, T newRow, boolean pkOnly, boolean ignoreEmpty)
          throws RemoteException {
    if (condition == null) {
      return "null";
    }
    if (newRow == null) {
      return "null";
    }
    StringBuilder s = new StringBuilder("UPDATED MULTIPLE ROWS: ");
    s.append(buildRowString(condition, "CONDITION", pkOnly, ignoreEmpty));
    s.append(buildRowString(newRow, ", NEW VALUES", pkOnly, ignoreEmpty));
    return s.toString();
  }


  private String buildRowString(T row, String prefix, boolean pkOnly) throws RemoteException {
    return buildRowString(row, prefix, pkOnly, false);
  }


  private String buildRowStringIgnoreEmpty(T row, String prefix, boolean pkOnly) throws RemoteException {
    return buildRowString(row, prefix, false, true);
  }


  private String buildRowString(T row, String prefix, boolean pkOnly, boolean ignoreEmpty)
                 throws RemoteException {
    if (row == null) {
      return "null";
    }
    StringBuilder s = new StringBuilder(prefix);
    s.append(" { ");
    DBTableInfo table = _handler.getDBTableInfo();
    TreeMap<String, String> map = null;
    if (pkOnly) {
      map = new ReflectionTools<T>(row).getRowMapPkColsOnly(table, row, _logger);
    }
    else {
      map = new ReflectionTools<T>(row).getRowMap(table, row, _logger);
    }
    boolean first = true;
    for (Entry<String, String> entry : map.entrySet()) {
      if (first) { first = false; } else { s.append(", "); }
      if (ignoreEmpty) {
        if ((entry.getValue() == null) || (entry.getValue().trim().length() < 1)) {
          continue;
        }
      }
      s.append(entry.getKey()).append(": ").append(entry.getValue());
    }
    s.append(" } ");
    return s.toString();
  }



}

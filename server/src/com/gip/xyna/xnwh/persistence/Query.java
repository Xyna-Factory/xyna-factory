/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.xnwh.persistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;



/**
 * kapselt informationen zu einem abfragestatement
 */
public class Query<E> {

  private static final Pattern QUERY_PATTERN =
      Pattern
          .compile("^\\s*select\\s+.*?\\s+from\\s+((?:\\w+\\.?)*)(\\s+\\w+)?(((\\s+(left|right))?\\s+((inner|outer)\\s+)?)?\\s*join\\s+.*\\s+on\\s+.*)?(\\s+where\\s+.*?)?(\\s+order by\\s+.*?)?(\\s+for update)?\\s*$",
                   Pattern.CASE_INSENSITIVE);

  private static Pattern SQL_STRING_SPLIT_PATTERN = Pattern.compile("\\?");
  private static final Pattern endsWithLikePattern = Pattern.compile("^.*\\s+like\\s*", Pattern.CASE_INSENSITIVE);


  private String table;
  private ResultSetReader<? extends E> reader;
  private String sqlString;

  private final boolean[] likeParameters;


  public Query(String sqlString, ResultSetReader<? extends E> resultSetReader) throws PersistenceLayerException {
    this(sqlString, resultSetReader, parseSqlStringFindTable(sqlString));
  }

  public Query(String sqlString, ResultSetReader<? extends E> resultSetReader, String tableName) throws PersistenceLayerException {
    if (resultSetReader == null) {
      throw new IllegalArgumentException("resultSetReader may not be null");
    }
    if (sqlString == null) {
      throw new IllegalArgumentException("sqlString may not be null");
    }
    table = tableName.toLowerCase();
    reader = resultSetReader;
    this.sqlString = sqlString;

    String[] parts = SQL_STRING_SPLIT_PATTERN.split(getSqlString());
    likeParameters = new boolean[parts.length];
    for (int i = 0; i < parts.length; i++) {
      if (endsWithLikePattern.matcher(parts[i]).matches()) {
        likeParameters[i] = true;
      }
    }

  }


  public void modifyTargetTable(String newTableName) {
    modifyTargetTable(newTableName, "");
  }

  public void modifyTargetTable(String newTableName, String escape) {
    newTableName = newTableName.toLowerCase();
    this.sqlString = changeTableNameInSqlString(sqlString, newTableName, table, escape);
    this.table = newTableName;
  }

  private static String changeTableNameInSqlString(String sqlString, String newTableName, String oldTableName, String escape) {
    String table = escape.isEmpty() ? "(" + oldTableName + ")" : escape + "?(" + oldTableName + ")" + escape + "?";
    Pattern replacementPattern = Pattern.compile("^.*\\s+from\\s+" + table + "(\\s+.*)?$", Pattern.CASE_INSENSITIVE);
    Matcher m = replacementPattern.matcher(sqlString);
    final int groupToReplace = 1;
    if (!m.find()) {
      // should not happen because the statement had been parsed before
      throw new RuntimeException("No table found in SQL statement");
    }
    return new StringBuilder(sqlString).replace(m.start(groupToReplace), m.end(groupToReplace), newTableName).toString();
  }


  public static String parseSqlStringFindTable(String sqlString) throws PersistenceLayerException {
    Matcher m = QUERY_PATTERN.matcher(sqlString);
    if (m.matches()) {
      return m.group(1);
    } else {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("query statement: " + sqlString);
    }
  }


  public String getTable() {
    return table;
  }


  public boolean[] getLikeParameters() {
    return likeParameters;
  }


  public String getSqlString() {
    return sqlString;
  }

  
  public ResultSetReader<? extends E> getReader() {
    return reader;
  }

}

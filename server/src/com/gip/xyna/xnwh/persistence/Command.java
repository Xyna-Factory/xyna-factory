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



public class Command {

  private static final Pattern deletePattern = Pattern.compile("^delete from (.*?)( where .*?)?$",
                                                               Pattern.CASE_INSENSITIVE);
  private static final Pattern updatePattern = Pattern
      .compile("^update (.*?) set (\\w+\\s*=.*?)(,\\s*\\w+\\s*=.*?)*( where .*?)?$", Pattern.CASE_INSENSITIVE);
  private static final Pattern insertPattern = Pattern.compile("^insert into (.*?) \\(.*?\\) values \\(.*?\\)$",
                                                               Pattern.CASE_INSENSITIVE);

  private String sqlString;
  private String tableName;


  public Command(String sqlString) throws PersistenceLayerException {
    this(sqlString, Command.parseSqlStringFindTable(sqlString).toLowerCase());
  }
  
  public Command(String sqlString, String tableName) throws PersistenceLayerException {
    this.sqlString = sqlString;
    this.tableName = tableName;
  }


  public static String parseSqlStringFindTable(String sqlString) {
    Matcher m = updatePattern.matcher(sqlString);
    if (m.matches()) {
      return m.group(1);
    } else {
      m = insertPattern.matcher(sqlString);
      if (m.matches()) {
        return m.group(1);
      } else {
        m = deletePattern.matcher(sqlString);
        if (m.matches()) {
          return m.group(1);
        } else {
          throw new RuntimeException("unsupported command statement: " + sqlString);
        }
      }
    }
  }


  public String getSqlString() {
    return sqlString;
  }


  public String getTable() {
    return tableName;
  }

}

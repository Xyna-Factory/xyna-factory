/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.memory.sqlparsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParsedQuery {

  private static Pattern sqlPattern = Pattern
      .compile("^select (.*) from .*?(?:where (.*?))?(?: order by (.*?))?\\s*(for update)?(?: group by (.*?))?$",
               Pattern.CASE_INSENSITIVE);

  private static final Pattern IS_COUNT_QUERY_PATTERN = Pattern.compile("count\\(\\*\\)", Pattern.CASE_INSENSITIVE);
  private static final Pattern SELECTION_DISSECTOR = Pattern.compile("select (\\w+(\\s*,\\s*\\w+)*) .*", Pattern.CASE_INSENSITIVE);

  private Condition whereCondition;
  private boolean isCountQuery = false;
  private OrderBy[] orderBy;
  private final boolean forUpdate;
  private String[] selection;


  public ParsedQuery(String sqlString) throws PreparedQueryParsingException {

    Matcher m = sqlPattern.matcher(sqlString);
    if (!m.matches()) {
      throw new PreparedQueryParsingException("invalid sql: " + sqlString);
    }

    String select = m.group(1);
    if (IS_COUNT_QUERY_PATTERN.matcher(select).matches()) {
      isCountQuery = true;
      selection = new String[] {"count"};
    } else {
      selection = select.split(",");
      for (int i=0; i < selection.length; i++) {
        selection[i] = selection[i].trim();
      }
    }

    String whereString = m.group(2);
    if (whereString != null && whereString.length() > 0) {
      whereCondition = Condition.create(whereString);
    } else {
      whereCondition = new Condition();
    }

    String orderByString = m.group(3);
    if (orderByString != null && orderByString.length() > 0) {
      String[] parts = orderByString.split(",");
      orderBy = new OrderBy[parts.length];
      for (int i = 0; i < parts.length; i++) {
        orderBy[i] = new OrderBy(parts[i].trim());
      }
    } else {
      orderBy = new OrderBy[0];
    }

    String forUpdateString = m.group(4);
    String groupByString = m.group(5);
    if (forUpdateString != null && forUpdateString.length() > 0) {
      if (groupByString != null) {
        throw new PreparedQueryParsingException("Inconsistent where clause: Cannot combine \"group by\" and \"for update\"");
      }
      if (isCountQuery) {
        throw new PreparedQueryParsingException("Inconsistent where clause: Cannot combine \"count(*)\" and \"for update\"");
      }
      forUpdate = true;
    } else {
      forUpdate = false;
    }

  }

  public Condition getWhereClause() {      
    return whereCondition;
  }

  public boolean isCountQuery() {
    return isCountQuery;
  }
  
  public OrderBy[] getOrderBys() {
    return orderBy;
  }


  public boolean isForUpdate() {
    return forUpdate;
  }
  
  
  public String[] getSelection() {
    return selection;
  }
  
  
}

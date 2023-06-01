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
package com.gip.xyna.xnwh.persistence.oracle;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



public class OraclePreparedQuery<T> implements PreparedQuery<T> {

  private Query<T> query;


  public OraclePreparedQuery(Query<T> query) {
    this.query = query;
  }


  public ResultSetReader<? extends T> getReader() {
    return query.getReader();
  }


  public String getTable() {
    return query.getTable();
  }


  public Query<T> getQuery() {
    return query;
  }


  public static void main(String[] args) {
    //String sqlQuery = "SELECT * FROM cronlikeorders ORDER BY nextexecution ASC for update";

    String sqlQuery = "SELECT tbl0.dt4 AS col5, tbl0.uniquehelper AS col4, tbl0.parentuid AS col7, tbl0.unid AS col6, xmomdt3.kuhrant AS col1, xmomdt3.histostap AS col0, xmomdt3.uuid AS col3, xmomdt3.uidacrosshistory AS col2 FROM xmomdt3 LEFT JOIN xmomstorabledt41 tbl0 ON xmomdt3.uidacrosshistory = tbl0.parentuid WHERE ((xmomdt3.uuid = ?) AND (xmomdt3.kuhrant = ?)) FOR UPDATE";
    
    Matcher forUpdatePatternMatcher = forUpdatePattern.matcher(sqlQuery);
    boolean forUpdate = forUpdatePatternMatcher.find();
    if (forUpdate) {
      String rows = forUpdatePatternMatcher.group(1);
      String from = forUpdatePatternMatcher.group(2);
      String pk = "uidacrosshistory";
      String whereclauses = forUpdatePatternMatcher.group(forUpdatePatternMatcher.groupCount() - 1);
      String orderby = forUpdatePatternMatcher.group(forUpdatePatternMatcher.groupCount());
      /*System.out.println("rows: " + rows);
      System.out.println("whereclauses: " + whereclauses);
      System.out.println("orderby: " + orderby);
      System.out.println("");
      System.out.println("groupCount: " + forUpdatePatternMatcher.groupCount());*/
      for (int i = 0; i < forUpdatePatternMatcher.groupCount(); i++) {
        System.out.println(forUpdatePatternMatcher.group(i));
      }
      sqlQuery =
          "select" + rows + from + " where " + pk + " in (select " + pk +
          " from (select " + pk + from + " " + whereclauses + orderby + ") where rownum <= ?) "
          + orderby + " for update";
    }
    //System.out.println(sqlQuery);
  }

  // copy & paste from QueryGenerator (to allow it to develop independently)
  private static String PQC /* possibly qualified column - regexp pattern */ = "[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?";
  private static String PLACE_FOR_WHERE_PATTERN = "\\s+FROM\\s+"+PQC+"(((\\s+(LEFT|RIGHT))?\\s+((INNER|OUTER)\\s+)?)?\\s*JOIN\\s+"+PQC+"(\\s+[a-zA-Z0-9_]+)?\\s+ON\\s+"+PQC+"\\s*=\\s*"+PQC+")*";

  private static final Pattern forUpdatePattern =
      Pattern
          .compile(
                   "select(.*?)(" + PLACE_FOR_WHERE_PATTERN + ")\\s+((?:where.*?\\s+)?)((?:order\\s+by\\s+(?:[^\\s]+\\s+(?:asc\\s+|desc\\s+)?)+)?)for\\s+update.*",
                   Pattern.CASE_INSENSITIVE);


  public String getTransformedQueryToUseWithMaxRows(Class<? extends Storable> storableClass) {
    String sqlQuery = query.getSqlString();
    /*
     * ein maxrows-beschränktes select der form
     *  
     *  select <cols> from table <whereclauses> order by bla for update 
     *  
     *  funktioniert bei oracle nicht mit dem typischen umgebauten syntax
     *  
     *  select <cols> from (select from table <whereclauses> order by bla) where rownum < x for update
     *  
     *  mit dem fehler:
     *  java.sql.SQLException: ORA-02014: cannot select FOR UPDATE from view with DISTINCT, GROUP BY, etc.
     *  
     *  deshalb der umbau in ein sql der form
     *  
     *   select <cols> from table where <pk> in 
     *     (select <pk> from 
     *        (select <pk> from table <whereclauses> order by bla)
     *      where rownum < x
     *      )
     *   order by bla for update
     */
    //FIXME in prepare() verschieben
    Matcher forUpdatePatternMatcher = forUpdatePattern.matcher(sqlQuery);
    boolean forUpdate = forUpdatePatternMatcher.find();
    if (forUpdate) {
      String rows = forUpdatePatternMatcher.group(1);
      String from = forUpdatePatternMatcher.group(2);
      String pk = Storable.getPersistable(storableClass).primaryKey();
      String whereclauses = forUpdatePatternMatcher.group(forUpdatePatternMatcher.groupCount() - 1);
      String orderby = forUpdatePatternMatcher.group(forUpdatePatternMatcher.groupCount());
      if (orderby != null && orderby.length() > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(rows).append(from).append(" where ").append(pk).append(" in (select ");
        sb.append(pk).append(" from (select ").append(pk).append(from);
        if (whereclauses.length() > 0) {
          sb.append(" ").append(whereclauses);
        }
        sb.append(" ").append(orderby).append(") where rownum <= ?) ").append(orderby).append(" for update");
        sqlQuery = sb.toString();
      } else {
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(rows).append(from);
        if (whereclauses.length() > 0) {
          sb.append(" ").append(whereclauses).append(" and rownum <= ? for update");
        } else {
          sb.append(" where rownum <= ? for update");
        }
        sqlQuery = sb.toString();
      }
    } else {
      sqlQuery = new StringBuilder().append(" select * from (").append(sqlQuery).append(") where rownum <= ?").toString();
    }
    return sqlQuery;
  }

}

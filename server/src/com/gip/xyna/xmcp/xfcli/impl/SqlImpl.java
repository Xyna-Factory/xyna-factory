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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.ObjectStringRepresentation;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Sql;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;



public class SqlImpl extends XynaCommandImplementation<Sql> {

  private static final Logger logger = CentralFactoryLogging.getLogger(SqlImpl.class);

  private static final Pattern DESCRIBE_PATTERN = Pattern.compile("^\\s*(?:desc|describe)\\s+(.*)\\s*",
                                                                  Pattern.CASE_INSENSITIVE);
  private static final Pattern IS_COUNT_QUERY_PATTERN = Pattern.compile(".*count\\(\\*\\).*", Pattern.CASE_INSENSITIVE);


  enum RequestType {
    Describe, Count, Select;
  }
  
  private Pair<RequestType,String> examineRequestType(String statement) throws PersistenceLayerException {
    Matcher mat = DESCRIBE_PATTERN.matcher(statement);
    if( mat.matches() ) {
      return Pair.of(RequestType.Describe, mat.group(1) );
    }
    String tableName = Query.parseSqlStringFindTable(statement);

    if (IS_COUNT_QUERY_PATTERN.matcher(statement).matches()) {
      return Pair.of(RequestType.Count, tableName );
    }
    return Pair.of(RequestType.Select, tableName );
  }

  
  public void execute(OutputStream statusOutputStream, Sql payload) throws XynaException {

    Pair<RequestType,String> request = examineRequestType(payload.getStatement());
    
    String tableName = request.getSecond();
    ODS ods = ODSImpl.getInstance();
    Class<Storable<?>> storableKlazz = ods.getStorableByTableName(tableName);
    if (storableKlazz == null) {
      writeLineToCommandLine(statusOutputStream, "Table '" + tableName + "' is not registered.");
      return;
    }
    
    StorableProperty[] props = Storable.getPersistable(storableKlazz).tableProperties();
    for (StorableProperty prop : props) {
      if (prop.isProtected()) {
        writeLineToCommandLine(statusOutputStream, "Table '" + tableName
            + "' is protected and may not be queried via CLI.");
        return;
      }
    }
    
    Storable<?> storableInstance = instantiate( storableKlazz );
    if( storableInstance == null ) {
      writeLineToCommandLine(statusOutputStream, "Could not instantiate table '" + tableName + "'.");
      return;
    }

    if( request.getFirst() == RequestType.Describe ) {
      describe( statusOutputStream, ods, storableKlazz, storableInstance, tableName );
      return;
    }
    
    

    ODSConnectionType connectionType;
    if (payload.getConnectionType() != null) {
      try {
        connectionType = ODSConnectionType.valueOf(payload.getConnectionType());
      } catch (IllegalArgumentException e) {
        writeLineToCommandLine(statusOutputStream, "Unknown connection type: <" + payload.getConnectionType() + ">");
        return;
      }
    } else {
      connectionType = ODSConnectionType.DEFAULT;
    }

    String sqlString = payload.getStatement();

    ODSConnection con = ods.openConnection(connectionType);
    try {
      if( request.getFirst() == RequestType.Count ) {
        count( statusOutputStream, con, sqlString);
        return;
      } else if( request.getFirst() == RequestType.Select ) {
        select( statusOutputStream, con, storableInstance, sqlString, payload.getAsTable());
      } else {
        writeLineToCommandLine(statusOutputStream, "Unexpected statement");
        return;
      }
    } finally {
      con.closeConnection();
    }
  }

  private Storable<?> instantiate(Class<Storable<?>> storableKlazz) {
    try {
      return storableKlazz.getConstructor().newInstance();
    } catch (Exception e) {
      logger.debug("", e);
      return null;
    }
  }


  private void describe(OutputStream statusOutputStream, ODS ods, Class<Storable<?>> storableKlazz, Storable<?> storableInstance, String tableName) {
    writeLineToCommandLine(statusOutputStream, "Description of table '" + tableName + "':");
    Column[] columns = storableInstance.getColumns();
    for (Column c : columns) {
      writeLineToCommandLine(statusOutputStream, "\t" + c.name() + " (type=" + c.type() + ", size=" + c.size()
          + ", index=" + c.index() + ")");
    }
  }

  private void count(OutputStream statusOutputStream, ODSConnection con, String sqlString) throws PersistenceLayerException {
    Query<?> query = new Query<OrderCount>(sqlString, OrderCount.getCountReader());
    PreparedQuery<?> preparedQuery = con.prepareQuery(query);
    List<?> result = con.query(preparedQuery, new Parameter(), -1);
    int size = result.size();
    if (size > 0) {
      Object r = result.get(0);
      if (r instanceof OrderCount) {
        writeLineToCommandLine(statusOutputStream, "Count: " + ((OrderCount)r).getCount());
      } else {
        writeLineToCommandLine(statusOutputStream, "unsupported query");
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "Query did not match any object.");
    }
  }
  
  private void select(OutputStream statusOutputStream, ODSConnection con, Storable<?> storableInstance, String sqlString, boolean asTable) throws PersistenceLayerException {
    //FIXME dynamischen reader verwenden der nur die spalten liest die ausgelesen werden.
    Query<Storable<?>> query = new Query<Storable<?>>(sqlString, storableInstance.getReader());
    PreparedQuery<Storable<?>> preparedQuery = con.prepareQuery(query);
    List<Storable<?>> result = con.query(preparedQuery, new Parameter(), -1);
    int size = result.size();
    if (size > 0) {
      writeLineToCommandLine(statusOutputStream, "Query matched " + result.size() + " object" + (size == 1 ? "" : "s"));
      if( asTable ) {
        SelectTableFormatter stf = new SelectTableFormatter(result.get(0).getColumns(), result);
        StringBuilder output = new StringBuilder();
        stf.writeTableHeader(output);
        stf.writeTableRows(output);
        writeLineToCommandLine(statusOutputStream, output.toString() );
      } else {
        Column[] allCollumns = result.get(0).getColumns();
        for (Storable<?> storable : result) {
          writeToCommandLine(statusOutputStream, storableToString( storable, allCollumns ) );
        } 
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "Query did not match any object.");
    }
  }


  private String storableToString(Storable<?> storable, Column[] allCollumns) {
    StringBuilder sb = new StringBuilder();
    sb.append("\t").append(storable.getPrimaryKey()).append(":\n");
    for (Column col : allCollumns) {
      sb.append("\t\t");
      sb.append(col.name()).append(" = ");
      sb.append( storableValueToString(storable, col) );
      sb.append("\n");
    }
    return sb.toString();
  }
  
  private static String storableValueToString(Storable<?> storable, Column col) {
    try {
      StringBuilder sb = new StringBuilder();
      if (storable instanceof CronLikeOrder) {
        ((CronLikeOrder) storable).getCreationParameters();
      }
      ObjectStringRepresentation.createStringRepOfObject(sb, storable.getValueByColName(col));
      return sb.toString();
    } catch (IllegalArgumentException e) {
      logger.warn(null, e);
      return "ERROR";
    } catch (IllegalAccessException e) {
      logger.warn(null, e);
      return "ERROR";
    }
    // TODO BLOBs per reflection ausgeben
  }

  private static class SelectTableFormatter extends TableFormatter {
    private List<List<String>> rows;
    private List<String> header;
    
    public SelectTableFormatter(Column[] columns, List<Storable<?>> result) {
      header = new ArrayList<String>(columns.length);
      for(Column c : columns ) {
        header.add( c.name() );
      }
      rows = new ArrayList<List<String>>();
      for( Storable<?> s : result ) {
        rows.add( generateRow(columns, s) );
      }
    }

    private List<String> generateRow(Column[] columns, Storable<?> storable) {
      List<String> row = new ArrayList<String>(columns.length);
      for (Column col : columns) {
        row.add( storableValueToString(storable, col) );
      }
      return row;
    }

    @Override
    public List<List<String>> getRows() {
      return rows;
    }

    @Override
    public List<String> getHeader() {
      return header;
    }
    
  }
  
}

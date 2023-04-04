/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db.dbFiller;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.NDC;

import com.gip.xyna.demon.DemonProperties;

import dhcpAdapterDemon.db.dbFiller.DataProcessor.SQLData;
import dhcpAdapterDemon.types.State;

public class BulkDBFiller<Data> extends AbstractDBFillerBase<Data> {

  int bulkSize;
  int maxSqlRetry;
  
  ArrayList<ExecuteSQLStruct> sqlBuffer;
  private class ExecuteSQLStruct{
    private SQLData sqlData;
    private Data data;

    public ExecuteSQLStruct(SQLData sqlData, Data data) {
      this.sqlData=sqlData;
      this.data=data;
    }
    public SQLData getSqlData() {
      return sqlData;
    }
    public Data getData() {
      return data;
    }

  }
  
  private volatile float averageBulkCommitSize;
  
  /**
   * Konstruktor
   * @param dbFillerData
   * @param name
   */
  public BulkDBFiller(DBFillerData dbFillerData, String name) {
    super(dbFillerData,name);
    this.maxSqlRetry = DemonProperties.getIntProperty("db.sql.retry.max", 3);
    this.bulkSize = dbFillerData.getBulksize();
    sqlBuffer=new ArrayList<BulkDBFiller<Data>.ExecuteSQLStruct>(bulkSize);  
  }

  @Override
  protected void rollback() {
    sqlBuffer.clear();
  }
  
  @Override
  /*
   * Statements im Bulk ausführen und committen
   * Im Problemfall Rollback und Problemfallbehandlung mit Single-Commit: commitSingleWithRetry
   */
  protected void commit() {
    int size = sqlBuffer.size();
    logger.debug( "Committing "+size+" entries" );
    try{
      logger.debug("Doing bulk commit");
      for(ExecuteSQLStruct executeSQLStruct:sqlBuffer){
        executeOneSQL(executeSQLStruct.getSqlData());
      }
      boolean success=sqlUtils.commit(); 
      if (success){
        addBulkCommitSize(sqlBuffer.size());
        for(ExecuteSQLStruct executeSQLStruct:sqlBuffer){
          dataProcessor.state( executeSQLStruct.getData(), State.SUCCEEDED );
        }
      } else{
        sqlUtils.rollback();
        throw new SQLException("commit failed");
      }
    } catch (Exception e){
      logger.info("Problem with bulk commit occurred. cleanup...");
      sqlUtils.rollback();
      if (e instanceof java.sql.SQLTransactionRollbackException){
        incDeadlockCounter();
      } else {
        rebuildConnection("executeSQL  failed");        
      }
      logger.info("and start single commit as alternative.");
      commitSingleWithRetry();
    }
    sqlBuffer.clear();
  }

  /**
   * Im Problemfall einzeln committen: Alle Statements ausführen und jeweils einzeln committen.
   * Bei weiteren Problemen jedes Statement maxSqlRetry-mal wiederholen
   * Statements, die nicht ausgeführt werden können, ignorieren und als failed kennzeichnen
   * Damit haben fehlerhafte Statements keine Auswirkungen auf andere Statements.
   */
  private void commitSingleWithRetry() {
    for(ExecuteSQLStruct executeSQLStruct:sqlBuffer){
      boolean success=false;
      for(int i=0;i<maxSqlRetry;i++){
        logger.debug("Retry#"+(i+1));
        try{
          executeOneSQL(executeSQLStruct.getSqlData());          
          success=sqlUtils.commit();        
          addBulkCommitSize(1);//Single Commit==>1 Statement
          break;
        } catch (Exception e){
          boolean successRollback=sqlUtils.rollback();
          if (successRollback && e instanceof java.sql.SQLTransactionRollbackException){
            incDeadlockCounter();
          } else {//SQLException + sonstige incl. Runtime
            rebuildConnection("executeSQL  failed");        
          }
        }
      }
      dataProcessor.state( executeSQLStruct.getData(), success?State.SUCCEEDED:State.FAILED );
    }
  }
    
  
  /* Ersatz für sqlData.execute. Bei Problemen Exception werfen statt getLastException. Ermoeglicht gemeinsame Behandlung für SQLException/RuntimeException */
  private void executeOneSQL(SQLData sqlData) throws SQLException {
    sqlData.execute(sqlUtils);      
    if( sqlUtils.getLastException() != null ) {//Alles ok
      throw sqlUtils.getLastException();
    }
  }

  @Override
  protected Data tryGetData() {
    //1. Versuch etwas auszulesen
    Data entry = readEntryFromRingBuffer();
    if( entry != null ) {
      return entry;
    }
    //keine Daten erhalten, der RingBuffer ist leer. Da dies länger dauern kann, wird hier committed.
    if( sqlBuffer.size() != 0 ) {
      commit();
    }
    
    //Warten auf neue Daten
    entry = waitForNewData();
    return entry;
  }


  @Override
  protected String getMode() {
    return "bulkCommit("+bulkSize+")";
  }
  
  /**
   * @param size
   */
  private void addBulkCommitSize(int size) {
    //System.err.println("addBulkCommitSize"+size);
    averageBulkCommitSize *= 0.9;
    averageBulkCommitSize += 0.1 * size;
  }
  
  /**
   * @return
   */
  public float getAverageBulkCommitSize() {
    //System.err.println(averageBulkCommitSize);
    return averageBulkCommitSize;
  }
  
  @Override
  protected void processData() {
    //Versuch einen Eintrag zu holen
    Data entry = tryGetData();    
    //Falls kein Eintrag vorhanden ist, einfach nochmal probieren
    if( entry == null ) {
      return;
    } 
    //ermitteln der SQL-Daten
    NDC.push( dataProcessor.getNDC(entry) );
    try {
      SQLData sqlData = dataProcessor.processData( entry );
      if( sqlData == null ) {
        dataProcessor.state(entry,State.IGNORED);
        return;
      }
      sqlBuffer.add(new ExecuteSQLStruct(sqlData, entry));//im executeSQLBuffer speichern
      if( sqlBuffer.size() >= bulkSize ) {
        commit();
      }      
      logger.debug("processData done");
    }
    finally {
      NDC.pop();
    }
  }
  
  
  
}

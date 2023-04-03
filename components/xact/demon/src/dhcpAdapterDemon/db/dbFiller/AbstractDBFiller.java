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

import org.apache.log4j.NDC;

import com.gip.xyna.demon.DemonProperties;

import dhcpAdapterDemon.db.dbFiller.DataProcessor.SQLData;
import dhcpAdapterDemon.types.State;

public abstract class AbstractDBFiller<Data> extends AbstractDBFillerBase<Data> {
  
  int maxSqlRetry;
  
      
  /**
   * @param dbFillerData
   * @param name
   */
  public AbstractDBFiller(DBFillerData dbFillerData, String name) {
    super(dbFillerData,name);
    this.maxSqlRetry = DemonProperties.getIntProperty("db.sql.retry.max", 3);
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
      //in DB eintragen

      if( sqlData == null ) {
        state( entry, State.IGNORED );
        return;
      }
      
      State state = executeSQL( sqlData, entry  );
      if( state == State.FAILED ) {
        //direkter Neubau der Connection, vielleicht kann der Fehler damit behoben werden
        rebuildConnection("executeSQL failed");
        if( sqlUtils == null ) {
          rollback(entry,State.FAILED);
          throw new RebuildConnectionException("executeSQL failed");
        }
        state = executeSQL( sqlData, entry  );
      }
      
      switch( state ) {
      case SUCCEEDED:
        commit( entry, State.SUCCEEDED );
        break;
      case UNEXPECTED:
        commit( entry, State.UNEXPECTED );
        break;
      case FAILED:
        rollback(entry,State.FAILED);
        break;
      default:
        //darf nicht auftreten
        logger.warn("Unexpected state"+state);
      }
      
      logger.debug("done");
    }
    finally {
      NDC.pop();
    }
  }
  
  
  protected State executeSQL(SQLData sqlData, Data entry) {
    try {
      sqlData.execute(sqlUtils);
      
      Exception lastException=sqlUtils.getLastException();
      
      if( lastException != null ) {//Sonderfall Fehlerbehandlung von SQL-Fehlern
        for( int cnt=0; cnt< maxSqlRetry; ++cnt ) {//maxSqlRetry mal Retry, Schleife wird im Normalfall mit break am Ende der Schleife verlassen
          logger.warn( "Retry #+"+cnt+": " );
          if (lastException instanceof com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException){
            logger.warn( "Exception while sqlData.execute:"+lastException.getMessage(),lastException );
            sqlUtils.rollback();//setzt auch sqlUtils.getLastException() zurueck
            incDeadlockCounter();
          } else {
            logger.error( "Exception while sqlData.execute:"+lastException.getMessage(), lastException);
            sqlUtils.rollback();//setzt auch  sqlUtils.getLastException() zurueck
            rebuildConnection("executeSQL or commit failed");
          }
          sqlData.execute(sqlUtils);//eigentlicher Retry
          lastException=sqlUtils.getLastException();
          if( lastException == null ) {
            break;
          }
        }
        if( lastException != null ) {
          //es trat auch beim letzten Durchlauf eine SQL-Exception auf. Da die Statements keinen
          //fachlichen Grund für Exceptions haben sollten, wird dies als
          //schwerer Fehler gewertet
          logger.warn( "SQLException "+sqlUtils.getLastException().getMessage()+" for "+entry);
          setLastException( sqlUtils.getLastException() );
          return State.FAILED;
        }
      }//Ende der Fehlerbehandlung

      if( false ) { //Prüfung ist nicht mehr gewünscht, da falsche Ergebnisse geliefert werden
        if( ! sqlData.matchesExpectation() ) {
          logger.warn( "unexpected rowCount "+sqlData.getNumRows()+", expected was "+sqlData.getExpectedRows()+" for "+entry);
                       return State.UNEXPECTED;
        }
      }
      return State.SUCCEEDED;
    } catch( RuntimeException e ) {
      logger.error("Severe failure, trying to keep running",e);
      return State.FAILED;
    }
  }

  protected abstract void state(Data data, State state);
  protected abstract void commit(Data data, State state);
  protected abstract void rollback(Data data,State state);
  
}

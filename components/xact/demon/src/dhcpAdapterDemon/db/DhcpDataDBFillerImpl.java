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
package dhcpAdapterDemon.db;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.InputStreamTokenizer;
import dhcpAdapterDemon.db.dbFiller.BulkDBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFillerData;
import dhcpAdapterDemon.db.dbFiller.DataProcessor;
import dhcpAdapterDemon.db.dbFiller.SingleDBFiller;
import dhcpAdapterDemon.types.DhcpAction;
import dhcpAdapterDemon.types.State;

public abstract class DhcpDataDBFillerImpl implements DataProcessor<DhcpData>, DhcpDataDBFiller {
  final static Logger defaultLogger = Logger.getLogger(DhcpDataDBFillerImpl.class);
    
  protected DBFiller<DhcpData> dbFiller;
  private Logger logger = defaultLogger;
  private String filename;
  private DBFillerStatistics dbFillerStatistics;
  
  /**
   * @param name
   * @param dfd
   * @param ndcLogger
   */
  public DhcpDataDBFillerImpl(String name, DBFillerData dfd, Logger Logger) {
    switch( dfd.getType() ) {
    case SINGLE_COMMIT: 
      dbFiller = new SingleDBFiller<DhcpData>(dfd,getClass().getSimpleName());
      break;
    case BULK_COMMIT: 
      dbFiller = new BulkDBFiller<DhcpData>(dfd,getClass().getSimpleName());
      break;
    default:
      throw new IllegalArgumentException( "Unsupported DBFillerData.Type "+ dfd.getType() );
    }
    
    dbFiller.setLogger(logger);
    dbFiller.setDataProcessor(this);
    this.filename = dfd.getFilename();
    this.logger = logger;
    dbFillerStatistics = new DBFillerStatisticsImpl(name,dbFiller);
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DataProcessor#state(java.lang.Object, dhcpAdapterDemon.types.State)
   */
  public void state(DhcpData data,State state) {
    dbFillerStatistics.state(data.getAction(),state);
  }
 
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#add(dhcpAdapterDemon.DhcpData)
   */
  public void add(DhcpData dhcpData) {
    dbFiller.add( dhcpData );
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#start()
   */
  public void start() {
    readDhcpData();
    
    Thread t = new Thread(dbFiller);
    t.setName( dbFiller.getName() );
    t.start();
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#terminate()
   */
  public void terminate() {
    if( dbFiller == null ) {
      return;
    }
    ArrayList<DhcpData> entries = dbFiller.terminate();
    if( entries.size() != 0 ) {
      saveDhcpData(entries);
    }
  }

  
  /**
   * Lesen der gesicherten DhcpDaten
   */
  private void readDhcpData() {
    File file = new File( filename );
    if( !file.canRead() ) {
      return; //es liegen keine Daten vor, dies ist kein Fehler
    }
    
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      InputStreamTokenizer isr = new InputStreamTokenizer(fis,"\teol\n",1000);
      for( String data : isr ) {
        //System.err.println( data );
        dbFiller.add( DhcpData.readFromString(data) );
      }
      fis.close();
      //nach erfolgreichem Einlesen Datei löschen
      logger.info( filename+ " successfully read, now deleting file");
      if( file.delete() ) {
        logger.info( filename+ " successfully deleted");
      } else {
        logger.warn( filename+ " not deleted");
      }
    }
    catch (IOException e) {
      logger.error( "Error while reading file " + file, e );
    }
    finally {
      try {
        if( fis != null ) fis.close();
      }
      catch (Exception e) {
        logger.error( e ); //ignore
      }
    }   

  }

  /**
   * Sichern der nicht verabeiteten DhcpDaten
   * @param entries
   */
  private void saveDhcpData(ArrayList<DhcpData> entries) {
    logger.info( "Writing "+entries.size()+" DhcpData to "+ filename);
    File file = new File( filename );
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      for( DhcpData dd : entries ) {
        DhcpData.writeToOutputStream( fos, dd );
      }
      fos.close();
    }
    catch (IOException e) {
      logger.error( "Error while writing file " + file, e );
    }
    finally {
      try {
        if( fos != null ) fos.close();
      }
      catch (Exception e) {
        logger.error( e ); //ignore
      }
    }   
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#logStatus(org.apache.log4j.Logger)
   */
  public void logStatus(Logger statusLogger) {
    dbFiller.logStatus(statusLogger);
    String scn = getClass().getSimpleName() +" ";
    for( DhcpAction a : DhcpAction.values() ) {
      statusLogger.info( scn + dbFillerStatistics.getCountersAsString(a) );
    }
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DataProcessor#getNDC(java.lang.Object)
   */
  public String getNDC(DhcpData dhcpData) {
    if( dhcpData == null ) {
      return "none";
    }
    return dhcpData.getChaddr().toString();
  }
   
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#getDBFillerStatistics()
   */
  public DBFillerStatistics getDBFillerStatistics() {
    return dbFillerStatistics;
  }
  
  @Override
  public String toString() {
    return dbFiller.toString();
  }
  
}

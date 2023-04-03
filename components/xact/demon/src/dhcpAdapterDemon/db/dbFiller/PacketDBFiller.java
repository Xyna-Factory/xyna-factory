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

import com.gip.xyna.utils.db.SQLUtils;

import dhcpAdapterDemon.types.State;

/**
 *
 *
 */
public class PacketDBFiller<Data extends PacketDBFiller.Packet> extends AbstractDBFillerBase<Data> {

  public interface Packet {
    public void process(SQLUtils sqlUtils) throws SQLException;
    public void commit();
    public void rollback();
  }
  
  /**
   * Konstruktor
   * @param dbFillerData
   * @param name
   */
  public PacketDBFiller(DBFillerData dbFillerData, String name) {
    super(dbFillerData,name);
  }

  /**
   * Konstruktor fuer Leaselog
   * @param dbFillerData
   * @param name
   */
  
  public PacketDBFiller(DBFillerData dbFillerData, String name, boolean leaselog) {
    super(dbFillerData,name, leaselog);
  }

  
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.AbstractDBFillerBase#commit()
   */
  @Override
  protected void commit() {
    logger.debug("commit");
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.AbstractDBFillerBase#getMode()
   */
  @Override
  protected String getMode() {
    return "packet";
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.AbstractDBFillerBase#processData()
   */
  @Override
  protected void processData() {
    //Versuch einen Eintrag zu holen
    Data entry = tryGetData();
    
    //Falls kein Eintrag vorhanden ist, einfach nochmal probieren
    if( entry == null ) {
      return;
    }
    try {
      if(sqlUtils.getConnection().isClosed())rebuildConnection("Connection was closed!");
    }
    catch (SQLException e) {
      logger.warn("Error rebuilding connection, because it was closed:",e);
    }
    
    String error = processData(entry);
    if( error != null ) {
      rebuildConnection(error); //direkter Neubau der Connection
      if( sqlUtils != null ) {
        error = processData(entry);//zweiter Versuch, die Daten einzutragen
      }
    }
    //egal ob gescheitert oder nicht: nun aus dem RingBuffer austragen 
    //(auf jeden Fall austragen, damit nicht ein permanent scheiternder Entry den RingBuffer verstopft)
    ringBuffer.poll(entry);
    
    if( error != null ) {
      //endgültig gescheitert
      rollback();
      entry.rollback();
      dataProcessor.state(entry,State.FAILED);
      throw new RebuildConnectionException(error);
    }
  }

  /**
   * @param entry
   * @return 
   */
  private String processData(Data entry) {
    try {
      entry.process(sqlUtils);
      sqlUtils.commit();
      entry.commit();
      dataProcessor.state(entry,State.SUCCEEDED);
      if(leaselog) lastDataWrite=System.currentTimeMillis();
      return null;
    } catch( SQLException e ) {
      setLastException(e);
      logger.error(e);
      sqlUtils.rollback();
      return e.getMessage();
    }
  }
  
  /**
   * liest den nächsten Datensatz aus dem RingBuffer
   * Achtung: peek, damit RingBuffer alle Daten behält
   */
  @Override
  protected Data readEntryFromRingBuffer() {
    return ringBuffer.peek();
  }
  
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.AbstractDBFillerBase#rollback()
   */
  @Override
  protected void rollback() {
    logger.debug("rollback");
    if( sqlUtils != null ) {
      sqlUtils.rollback();
    }
  }
  

}

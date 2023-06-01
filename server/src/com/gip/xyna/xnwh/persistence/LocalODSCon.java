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

/**
 * LocalODSCon kapselt eine übergebene Connection, die null sein kann, und stellt in diesem Fall 
 * eine eigene lokale ODSConnection zu Verfügung. 
 * Beispiel:
 * <pre>
 * public void store( Storable s, ODSConnection con ) throws PersistenceLayerException {
 *   LocalODSCon localCon = LocalODSCon.constructDefaultCon(con);
 *   try {
 *     localCon.getConnection().persistObject(s);
 *     localCon.commitIfLocal();
 *   } finally {
 *     localCon.close();
 *   }
 * }
 * </pre>
 * anstelle von 
 * <pre>
 * public void store( Storable s, ODSConnection con ) throws PersistenceLayerException {
 *   ODSConnection localCon = con;
 *   boolean openedCon = false;
 *   if( localCon == null ) {
 *     localCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
 *     openedCon = true;
 *   }
 *   try {
 *     localCon.persistObject(s);
 *     if( openedCon ) {
 *       localCon.commit();
 *     }
 *   } finally {
 *     if( openedCon ) {
 *       localCon.closeConenction();
 *     }
 *   }
 * }
 * </pre>
 */
public class LocalODSCon {

  private boolean isLocal;
  private ODSConnection con;
  
  public LocalODSCon(ODSConnection con, ODSConnectionType type) {
    if( con == null ) {
      this.con = ODSImpl.getInstance().openConnection(type);
      this.isLocal = true;
    } else {
      this.isLocal = false;
      this.con = con;
    }
  }
  
  private LocalODSCon(boolean isLocal, ODSConnection con) {
    this.isLocal = isLocal;
    this.con = con;
  }
  
  /**
   * Liefert die Connection, dabei sind 3 Fälle möglich: a) null, b) lokale Connection 
   * und c) externe Connection je nach Konstruktion des LocalODSCon.
   * @return
   */
  public ODSConnection getConnection() {
    return con;
  }
  
  /**
   * Ist Connection lokal geöffnet worden?
   * @return
   */
  public boolean isLocal() {
    return isLocal;
  }
  
  /**
   * Commit bei lokaler Connection, externe Connection bleibt unverändert
   * @throws PersistenceLayerException
   */
  public void commitIfLocal() throws PersistenceLayerException {
    if( isLocal ) {
      con.commit();
    }
  }
  
  /**
   * Rollback bei lokaler Connection, externe Connection bleibt unverändert
   * @throws PersistenceLayerException
   */
  public void rollbackIfLocal() throws PersistenceLayerException {
    if( isLocal ) {
      con.rollback();
    }
  }
  
  /**
   * Commit, egal ob Connection lokal oder extern ist
   * @throws PersistenceLayerException
   */
  public void commit() throws PersistenceLayerException {
    if( con != null ) {
      con.commit();
    }
  }
  
  /**
   * Rollback, egal ob Connection lokal oder extern ist
   * @throws PersistenceLayerException
   */
  public void rollback() throws PersistenceLayerException {
    if( con != null ) {
      con.rollback();
    }
  }
  
  /**
   * Schließen der lokalen Connection, externe Connection bleibt unverändert
   * @throws PersistenceLayerException
   */
  public void close() throws PersistenceLayerException {
    if( isLocal ) {
      con.closeConnection();
    }
    con = null;
  }
  
  @Override
  public String toString() {
    return "LocalODSCon(isLocal="+isLocal+",con="+con+")";
  }

  /**
   * LocalODSCon hat sicher eine Connection != null. Falls con null ist, wird eine neue DEFAULT-Connection lokal geöffnet
   * @param con
   * @return
   */
  public static LocalODSCon constructDefaultCon(ODSConnection con) {
    if( con == null ) {
      return new LocalODSCon( true, ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT) );
    } else {
      return new LocalODSCon( false, con );
    }
  }
  
  /**
   * LocalODSCon hat sicher eine Connection != null. Falls con null ist, wird eine neue HISTORY-Connection lokal geöffnet
   * @param con
   * @return
   */
  public static LocalODSCon constructHistoryCon(ODSConnection con) {
    if( con == null ) {
      return new LocalODSCon( true, ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY) );
    } else {
      return new LocalODSCon( false, con );
    }
  }

  /**
   * LocalODSCon kann eine null-Connection haben, wenn con null ist
   * @param con
   * @return
   */
  public static LocalODSCon constructNullableCon(ODSConnection con) {
    if( con == null ) {
      return new LocalODSCon( false, null );
    } else {
      return new LocalODSCon( false, con );
    }
  }

}

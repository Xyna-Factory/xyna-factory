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
package gip.base.db.drivers;

import java.sql.Connection;

import gip.base.common.OBException;
import gip.base.db.OBContext;
import gip.base.db.OBDatabase;

/**
 * Interface, das alle Treiber implementieren muessen
 */
public interface OBDriverInterface {

  /** Uebersetzung des Datenmodell-Tabellennamens in den SQL-Namen
   * @param schema
   * @param tableName Der Tabellen-Name aus dem Daten-Modell
   * @return tableName, wie er bei DB-Operationen angegeben werden muss, z.B. USER.iTabelName
   * @throws OBException
   */
  public String getTableName(String schema, String tableName) throws OBException ;

  public String getNextKeyVal(String schema, String sequenceName) throws OBException;

  public String getNextKeyValStatement(String schema, String sequenceName) throws OBException;

  public String getCurrentKeyVal(String schema, String sequenceName) throws OBException;
  
  public String getCurrentKeyValStatement(String schema, String sequenceName) throws OBException;
  
  /* Datum und Zeit */
  public String getSysDateString() throws OBException;
  
  public String getSysTimeStampString() throws OBException;
  
  public String getEmptyClob() throws OBException;
  
  public String getEmptyBlob() throws OBException;
  
  public String getMaxRows(int maxRows) throws OBException;
  
  public void createUser(OBContext context, String userName, String pw) throws OBException;
  
  public void dropUser(OBContext context, String userName) throws OBException;
  
  public void registerDriver();
  
  public void setConstaintsDeferred(OBDatabase con);

  public void setSessionInfo(Connection con, String info);
  
  public void setVSessionActionInfo(Connection con, String module, String action) throws OBException;
}

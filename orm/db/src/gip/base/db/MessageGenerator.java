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
package gip.base.db;

import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import gip.base.common.OBException;

/** 
 * Liefert eine allgemeine Fehlermeldung aus einem Constraint-Namen 
 */
public class MessageGenerator {

  private transient static Logger logger = Logger.getLogger(MessageGenerator.class);
  
  /** 
   * Liefert eine allgemeine Fehlermeldung aus einem Constraint-Namen 
   * @param constrName
   * @return Message
   * @throws OBException
   */
  public String generateMessage(String constrName) throws OBException {
    return "Der DB-Constraint " + constrName + " wurde verletzt.";//$NON-NLS-1$//$NON-NLS-2$
  }

  
  /**
   * @param constrName
   * @param con
   * @return Message
   * @throws OBException
   */
  public String generateMessage(String constrName, OBConnectionInterface con) throws OBException {
    logger.debug("generate message for:" +constrName);//$NON-NLS-1$
    Statement stmt = null;
    try {
      stmt = con.createStatement();
      String sqlString = "SELECT messageText FROM errorMessage WHERE lower(messageName) = lower('" + constrName+ "')"; //$NON-NLS-1$ //$NON-NLS-2$
      ResultSet rs = stmt.executeQuery(sqlString);
      String helpBack = ""; //$NON-NLS-1$
      if (rs.next()) {
        helpBack = rs.getString(1);
      }
      if (rs.next()) {
        // nicht eindeutig
        return ""; //$NON-NLS-1$
      }
      return helpBack;
    }
    catch (Exception e) {
      logger.debug("error generating message", e); //$NON-NLS-1$
      return ""; //$NON-NLS-1$
    }
    finally {
      try {
        if (stmt!=null) stmt.close();
      }
      catch (Exception e) {
        logger.error("error generating message", e); //$NON-NLS-1$
      }
    }
  }
 

  /**
   * @param oraError
   * @return OraMessage
   * @throws OBException
   */
  public String generateOraMessage(String oraError) throws OBException {
    return OBException.getErrorMessage(oraError);
  }
  
  
}



/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils.db;

/**
 * Collection of common sql queries depending on oracle databases.
 */
public class OracleSQLStatements {

   /**
    * Get sql for selecting the next value from a sequence.
    * 
    * @param schema
    *              schema of the sequence
    * @param sequenceName
    *              name of the sequence
    * @return select statement
    */
   public static String getNextValStatement(String schema, String sequenceName) {
      return "SELECT " + OracleHelpers.getNextValString(schema, sequenceName)
            + " FROM dual";
   }

   /**
    * Get sql for selecting the current value from a sequence.
    * 
    * @param schema
    *              schema of the sequence
    * @param sequenceName
    *              name of the sequence
    * @return select statement
    */
   public static String getCurrentValStatement(String schema,
         String sequenceName) {
      return "SELECT "
            + OracleHelpers.getCurrentValString(schema, sequenceName)
            + " FROM dual";
   }

   /**
    * Get sql for creating a new user.
    * 
    * @param userName
    *              user name
    * @param password
    *              password for the user
    * @return create statement
    */
   public String getCreateUserStatement(String userName, String password) {
      return "CREATE USER " + userName + " IDENTIFIED BY " + password
            + " DEFAULT TABLESPACE DATA "
            + " TEMPORARY TABLESPACE TEMPORARY_DATA";
      // "GRANT CREATE SESSION TO " + userName);
      // "GRANT IPNET TO " + userName);
   }

   /**
    * Get sql for deleting a existing user from the database.
    * 
    * @param userName
    *              user name
    * @return drop statement
    */
   public String getDropUserStatement(String userName) {
      return "DROP USER " + userName;
   }

}

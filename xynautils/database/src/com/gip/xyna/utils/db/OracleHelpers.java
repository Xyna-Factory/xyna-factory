/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
 * Collection of small helpers for the interaction with oracle databases.
 */
public class OracleHelpers {

   /**
    * Create a full qualified table name from given schema and table name.
    * 
    * @param schema
    *              database schema
    * @param tableName
    *              name of a database table
    * @return full qualified table name
    */
   public static String getTableName(String schema, String tableName) {
      return schema + "." + tableName;
   }

   /**
    * Get nextVal String for given schema and sequence.
    * 
    * @param schema
    *              schema of the sequence
    * @param sequenceName
    * @return
    */
   public static String getNextValString(String schema, String sequenceName) {
      return schema + "." + sequenceName + ".nextVal";
   }

   /**
    * Get currentVal String for given schema and sequence.
    * 
    * @param schema
    *              schema of the sequence
    * @param sequenceName
    * @return
    */
   public static String getCurrentValString(String schema, String sequenceName) {
      return schema + "." + sequenceName + ".currVal";
   }

   /**
    * Get sysDate String.
    * 
    * @return
    */
   public static String getSysDateString() {
      return "sysDate";
   }

}

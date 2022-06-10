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

package com.gip.juno.ws.tools;

import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.xyna.utils.db.ResultSetReader;

/**
 * Class whose methods handle SQL commands for one database (schema), but on a table that
 * also exists on many other DPP instances (locations);
 * that means the SQL operation is one of several analogue operations on several locations,
 * typically directed by a method of LocationHandler.
 * 
 * The command will be actually executed by calling a method of DBCommands.
 * 
 * The only activity which the methods of DBCommandsForLocation perform themselves is to build 
 * an instance of SQLUtilsContainer. 
 * 
 * On which DPP instance the command will be performed is specified by the parameter
 * location which contains the name of the DPP instance.
 * 
 * The SQL of the command is provided by a parameter of type SQLCommand.
 * 
 * On which database (schema) on the location the command will be executed is specified by 
 * a parameter of type LocationSchema.
 */
public class DBCommandsForLocation<T> {

  
  public List<T> queryForLocation(ResultSetReader<T> reader, String location, SQLCommand builder, 
        LocationSchema schema, Logger logger) throws java.rmi.RemoteException {
    SQLUtilsContainerForLocation container = SQLUtilsCache.getForLocation(location, schema, logger);
    return new DBCommands<T>().query(reader, builder, container, logger);
  }
   

  public int executeDMLForLocation(String location, SQLCommand builder, LocationSchema schema, Logger logger) 
          throws java.rmi.RemoteException {
    SQLUtilsContainerForLocation container = SQLUtilsCache.getForLocation(location, schema, logger);
    return new DBCommands<T>().executeDML(builder, container, logger);
  }
  
  
  public int executeDMLForLocation(String location, SQLCommand builder, FailoverFlag flag, LocationSchema schema,
        Logger logger) throws java.rmi.RemoteException {
    SQLUtilsContainerForLocation container = SQLUtilsCache.getForLocation(location, schema, flag,
        logger);
    return new DBCommands<T>().executeDML(builder, container, logger);
  }
  

  public T queryOneRowForLocation(ResultSetReader<T> reader, DBTableInfo table, TreeMap<String, String> map, 
        String location, LocationSchema schema, Logger logger) throws java.rmi.RemoteException {
    SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
    return queryOneRowForLocation(reader, location, builder, schema, logger);
  }

  
  public T queryOneRowForLocation(ResultSetReader<T> reader, DBTableInfo table, TreeMap<String, String> map,
        String location, FailoverFlag flag, LocationSchema schema, Logger logger) throws java.rmi.RemoteException {
    SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
    return queryOneRowForLocation(reader, location, builder, flag, schema, logger);
  }
  
  
  public T queryOneRowForLocation(ResultSetReader<T> reader, String location, SQLCommand builder, 
        LocationSchema schema, Logger logger) throws java.rmi.RemoteException {
    SQLUtilsContainerForLocation container = SQLUtilsCache.getForLocation(location, schema, logger);
    return new DBCommands<T>().queryOneRow(reader, builder, container, logger);
  }
  

  public T queryOneRowForLocation(ResultSetReader<T> reader, String location, SQLCommand builder,
          FailoverFlag flag, LocationSchema schema, Logger logger) throws java.rmi.RemoteException {
    SQLUtilsContainerForLocation container = SQLUtilsCache.getForLocation(location, schema, flag,
        logger);
    return new DBCommands<T>().queryOneRow(reader, builder, container, logger);
  }
  
}

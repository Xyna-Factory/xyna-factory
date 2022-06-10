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

package com.gip.juno.ws.handler;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.tools.DBCommandsForLocation;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.ExceptionData;
import com.gip.juno.ws.tools.LocationData;
import com.gip.juno.ws.tools.OutputHeaderData;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.ResultFromLocations;
import com.gip.juno.ws.tools.ResultSetReaderForLocation;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.FailoverTools;

/**
 * Class whose methods handle SQL operations that are executed on analogue tables
 * on several DPP instances (locations).
 *
 * For each location a method of DBCommandsForLocation will be called to handle the operation
 * for the respective database (schema) on that location.
 */
public class LocationHandler<T> {

  /**
   * Get all rows in all database connections
   */
  public ResultFromLocations<T> getAllRowsAllLocations(T ref, LocationSchema schema,
        DBTableInfo table, Logger logger) throws java.rmi.RemoteException {
    try {
      List<T> finalRet = new ArrayList<T>();
      ResultFromLocations<T> conndata = new ResultFromLocations<T>();
      SQLCommand builder = SQLBuilder.buildSQLSelectAll(table);
      String[] conns = LocationData.getInstance(schema, logger).getAllLocations(logger);
      for (String location : conns) {
        logger.info("Using connection " + location);
          ResultSetReaderForLocation<T> reader = new ReflectionTools<T>(ref).createResultSetReaderForLocation(
              table, schema, location, logger);
          try {
            List<T> ret = new DBCommandsForLocation<T>().queryForLocation(reader, location, builder, schema,
                logger);
            if (ret != null) {
              finalRet.addAll(ret);
              logger.info(location + ": Select returns rows: "+ ret.size());

              if (finalRet.size() >= 1000) {
              //if (finalRet.size() >= 1000000) {
                List<T> retcut = finalRet.subList(0,999);
                //List<T> retcut = finalRet.subList(0,999999);
                conndata.data = retcut;
                return conndata;
              }
            }
          } catch (Exception e) {
            logger.error("getAllRowsAllLocations: location = " + location + " : ", e);
            ExceptionData err = new ExceptionData();
            err.connectionname = location;
            err.exceptionText = e.toString();
            conndata.exceptions.add(err);
          }
      }
      conndata.data = finalRet;
      return conndata;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.getAllRowsAllLocations: " + e.toString());
    }
  }


  /**
   * search rows in named database location
   */
  public ResultFromLocations<T> searchRowsOfLocations(T ref, DBTableInfo table,
          TreeMap<String, String> map, String location, LocationSchema schema, Logger logger)
          throws java.rmi.RemoteException {
    try {
      SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
      return searchRowsOfLocations(builder, ref, table, location, schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.searchRowsOfLocation: ", e);
    }
  }

  public ResultFromLocations<T> searchRowsOfLocations(SQLCommand builder, T ref, DBTableInfo table,
        String location, LocationSchema schema, Logger logger)
        throws java.rmi.RemoteException {
    try {
      if ((location == null) || (location.equals(""))) {
        return searchRowsAllLocations(builder, ref, schema, table, logger);
      }
      List<T> finalRet = new ArrayList<T>();
      ResultSetReaderForLocation<T> reader = new ReflectionTools<T>(ref).createResultSetReaderForLocation(
          table, schema, location, logger);
      ResultFromLocations<T> conndata = new ResultFromLocations<T>();
      reader.setLocation(location);
      logger.debug("Using location " + location);
      List<T> ret = new DBCommandsForLocation<T>().queryForLocation(reader, location, builder, schema, logger);
        if (ret!=null) {
          if (ret.size() >= 1000) {
            List<T> retcut = ret.subList(0, 999);
            ret = retcut;
          }
          finalRet = ret;
          logger.info("Select returns rows: "+ ret.size());
        }
      conndata.data = finalRet;
      return conndata;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.searchRowsOfLocation: ", e);
    }
  }


  /**
   * search rows in all database connections
   */
  private ResultFromLocations<T> searchRowsAllLocations(SQLCommand builder, T ref, LocationSchema schema,
          DBTableInfo table, Logger logger) throws java.rmi.RemoteException {
    try {
      List<T> finalRet = new ArrayList<T>();
      ResultFromLocations<T> conndata = new ResultFromLocations<T>();
      //SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
      String[] conns = LocationData.getInstance(schema, logger).getAllLocations(logger);

      for (String location : conns) {
        ResultSetReaderForLocation<T> reader = new ReflectionTools<T>(ref).createResultSetReaderForLocation(
            table, schema, location, logger);
        //reader.setLocation(location);
        logger.info("Using connection " + location);
        try {
          List<T> ret = new DBCommandsForLocation<T>().queryForLocation(reader, location, builder, schema, logger);
          if (ret!=null) {
            finalRet.addAll(ret);
            logger.info("Select returns rows: "+ ret.size());
            if (finalRet.size() >= 1000) {
              List<T> retcut = finalRet.subList(0,999);
              conndata.data = retcut;
              return conndata;
            }
          }
        } catch (Exception e) {
          logger.error("searchRowsOfAllLocations, location = " + location + " : ", e);
          ExceptionData err = new ExceptionData();
          err.connectionname = location;
          err.exceptionText = e.toString();
          conndata.exceptions.add(err);
        }
      }
      conndata.data = finalRet;
      return conndata;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.searchRowsAllLocations " + e.toString());
    }
  }


  /**
   * update row in given location
   */
  public T updateRowOfLocation(T row, DBTableInfo table,
        LocationSchema schema, TreeMap<String, String> map, String location, Logger logger)
        throws java.rmi.RemoteException {
    try {
      ValueAdapter.adjustColValuesForUpdate(map, table, logger);
      SQLCommand builder = SQLBuilder.buildSQLUpdate(map, table);
      if (location == null) {
        location = "";
      }
      ResultSetReaderForLocation<T> reader = new ReflectionTools<T>(row).createResultSetReaderForLocation(
          table, schema, location, logger);

      new DBCommandsForLocation<T>().executeDMLForLocation(location, builder, schema, logger);
      return new DBCommandsForLocation<T>().queryOneRowForLocation(reader, table, map, location, schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.updateRowOfLocation: " + e.toString());
    }
  }


  /**
   * insert row in location
   */
  public T insertRowOfLocation(T row, LocationSchema schema, DBTableInfo table,
        TreeMap<String, String> map, String location, Logger logger)
        throws java.rmi.RemoteException {
    FailoverFlag flag = FailoverTools.getCurrentFailover(location, logger);
    return insertRowOfLocation(row, schema, table, map, location, flag, logger);
  }

  /**
   * insert row in location
   */
  public T insertRowOfLocation(T row, LocationSchema schema, DBTableInfo table,
        TreeMap<String, String> map, String location,  FailoverFlag flag, Logger logger)
        throws java.rmi.RemoteException {
    ResultSetReaderForLocation<T> reader = new ReflectionTools<T>(row).createResultSetReaderForLocation(
        table, schema, location, logger);
    return insertRowOfLocation(row, reader, table, map, location, flag, schema, logger);
  }


  /**
   * insert row in location
   */
  public T insertRowOfLocation(T oneRowRequest, ResultSetReaderForLocation<T> reader, DBTableInfo table,
        TreeMap<String, String> map, String location, FailoverFlag flag, LocationSchema schema, Logger logger)
        throws java.rmi.RemoteException {
    try {
      if (location == null) {
        location = "";
      }
      QueryTools.checkAutoIncrement(map, table, logger);
      ValueAdapter.adjustColValuesForInsert(map, table, logger);
      SQLCommand builder = SQLBuilder.buildSQLInsert(map, table);
      reader.setLocation(location);
      new DBCommandsForLocation<T>().executeDMLForLocation(location, builder, flag, schema, logger);
      return new DBCommandsForLocation<T>().queryOneRowForLocation(reader, table, map, location, flag, schema,
          logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.insertRowOfLocation: " + e.toString());
    }
  }



  /**
   * delete rows in named location
   */
  public String deleteRowsOfLocation(T deleteRowsRequest, LocationSchema schema,
        DBTableInfo table, TreeMap<String, String> map, String location, Logger logger)
        throws java.rmi.RemoteException {
    try {
      if (location == null) {
        location = "";
      }
      SQLCommand builder = SQLBuilder.buildSQLDelete(map, table);
      return new DBCommandsForLocation<T>().executeDMLForLocation(location, builder, schema, logger)
          + " Rows deleted.";
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error in LocationHandler.getAllRows " + e.toString());
    }
  }


  public OutputHeaderData getOutputHeaderData(ResultFromLocations<T> result, Logger logger)
        throws RemoteException {
    try {
      OutputHeaderData ret = new OutputHeaderData();
      ret.exceptionText = "";
      if (result.exceptions.size() ==0) {
        ret.status = "Success.";
      } else {
        ret.status = "";
        if (result.exceptions.size() == 1) {
          ret.status += "Exception for location: ";
        } else {
          ret.status += "Exceptions for locations: ";
        }
        for (int i=0; i < result.exceptions.size(); i++) {
          ret.status += result.exceptions.get(i).connectionname;
          if (i + 1 < result.exceptions.size()) {
            ret.status += ", ";
          }
          ret.exceptionText += "Exception for location: " + result.exceptions.get(i).connectionname;
          ret.exceptionText += "\n" + result.exceptions.get(i).exceptionText;
          ret.exceptionText += "\n";
        }
      }
      return ret;
    } catch (Exception e) {
      logger.error("Error in getOutputHeaderData: ", e);
      throw new java.rmi.RemoteException("Error in getOutputHeaderData: " + e.toString());
    }
  }


  /**
   * delete rows that fit to the mask supplied by parameter map in oldLocation and insert them into newLocation
   */
  public String moveRowsChangeLocation(T ref, DBTableInfo table, TreeMap<String, String> map,
          String oldLocation, String newLocation, LocationSchema schema, ReflectionTools<T> reftools,
          Logger logger) throws java.rmi.RemoteException {
    try {
      int numRows = 0;
      SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
      ResultFromLocations<T> conndata = searchRowsOfLocations(builder, ref, table, oldLocation, schema, logger);
      List<T> rows = conndata.data;
      numRows = rows.size();

      String delRet = deleteRowsOfLocation(ref, schema, table, map, newLocation, logger);
      logger.info("updateRowsChangeLocation : Delete Result for destination location = " + delRet);

      delRet = deleteRowsOfLocation(ref, schema, table, map, oldLocation, logger);
      logger.info("updateRowsChangeLocation : Delete Result for source location = " + delRet);
      for (T row : rows) {
        TreeMap<String, String> rowMap = reftools.getRowMap(table, row, logger);
        insertRowOfLocation(row, schema, table, rowMap, newLocation, logger);
      }
      return numRows + " rows were moved from location " + oldLocation + " to location " + newLocation;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new java.rmi.RemoteException("Error in LocationHandler.updateRowsChangeLocation: ", e);
    }
  }


}

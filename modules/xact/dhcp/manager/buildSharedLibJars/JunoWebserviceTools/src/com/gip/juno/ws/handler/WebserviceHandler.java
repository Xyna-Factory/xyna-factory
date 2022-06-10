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
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.handler.AuthenticationTools.AuthenticationMode;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.handler.ReflectionTools.DBReader;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.LocationData;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.ResultFromLocations;
import com.gip.juno.ws.tools.WSTools;

/**
 * The methods of this class are supposed to be called directly by the java code in the war-files.
 * The webservices for database tables should call no other classes than this one. 
 * 
 */
public class WebserviceHandler<T> {

  
  /**
   * Returns meta-information describing a database table, and implicitly the XML data structure 
   * of the related webservice.
   * 
   * Information source is hardcoded in the com.gip.juno.ws.db.* packages.
   * 
   * This method does not perform any database operation and does NOT check if the meta-information
   * is consistent with the actual database.  
   */
  public List<T> getMetaInfo(T ref, HeaderDataBean headerData) throws RemoteException {
    return getMetaInfo(ref, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public List<T> getMetaInfo(T ref, TableHandler handler, String username, String password) 
      throws RemoteException {
    Logger logger = handler.getLogger();    
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      List<T> ret = new MetaInfoTools<T>(ref, logger).getMetaInfo(handler.getDBTableInfo());
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetMetaInfo", e);
    }
  }
  
  
  /**
   * returns all rows in a management table 
   */
  public List<T> getAllRows(T ref, HeaderDataBean headerData) throws RemoteException {
    return getAllRows(ref, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public List<T> getAllRows(T ref, TableHandler handler, String username, String password) throws RemoteException {
    return getAllRows(ref, handler, username, password, false);
  }
  
  public List<T> getAllRows(T ref, TableHandler handler, String username, String password, boolean useLegayAuthentication) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      if (useLegayAuthentication) {
        AuthenticationTools.authenticate(username, password, logger);
        AuthenticationTools.checkPermissionsDBSelect(username, handler.getDBTableInfo().getSchema(), logger);
      } else {
        AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
        AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);        
      }
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, ref, logger);    
      List<T> ret = new DBCommandHandler<T>().getAllRows(reader, table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRows", e);
    }
  }
  
  /**
   * returns all rows of a management table that fit to condition mask supplied in parameter input
   */
  public List<T> searchRows(T input, HeaderDataBean headerData) throws RemoteException {
    return searchRows(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  public List<T> searchRows(T input, HeaderDataBean headerData, WebServiceInvocationIdentifier wsInvocationId) throws RemoteException {
    return searchRows(input, headerData.getTablehandler(), wsInvocationId, headerData.getUsername(), headerData.getPassword());
  }
  
  
  public List<T> searchRows(T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    return searchRows(input, handler, null, username, password);
  }
  
  public List<T> searchRows(T input, TableHandler handler, WebServiceInvocationIdentifier wsInvocationId, String username, String password) 
    throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      if (wsInvocationId == null) {
        wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      }
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      List<T> ret = new DBCommandHandler<T>().searchRows(input, reader, table, map, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }

  
  /**
   * Inserts one row (supplied by parameter input) into a management table.
   * After that performs a select for that inserted row and returns the result.
   */
  public T insertRow(T input, HeaderDataBean headerData) throws RemoteException {
    return insertRow(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  public T insertRow(T input, TableHandler handler, String username, String password) throws RemoteException {
    return insertRow(input, handler, username, password, false);
  }
  
  public T insertRow(T input, TableHandler handler, String username, String password, boolean useLegacyAuthentication) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      if (useLegacyAuthentication) {
        AuthenticationTools.authenticate(username, password, logger);
        AuthenticationTools.checkPermissionsDBEdit(username, handler.getDBTableInfo().getSchema(), logger);
      } else {
        AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.INSERTION_WEBSERVICE_IDENTIFIER.clone();
        AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      }
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      T ret = new DBCommandHandler<T>().insertRow(input, reader, table, map, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in InsertRow.", e);
    }
  }
  

  /**
   * Updates one row in a management table.
   * parameter input supplies both condition and new values:
   * the values of primary key columns in input will be used to build a where-condition,
   * the values of the other columns in input as new values to set by the update  ;
   * empty values in non-pk-columns mean that either null or empty string will be inserted
   * (that can be manipulated by the settings in parameter handler);
   * empty values for primary keys are used for the condition, too.
   */
  public T updateRow(T input, HeaderDataBean headerData) throws RemoteException {
    return updateRow(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T updateRow(T input, TableHandler handler, String username, String password) throws RemoteException {
    return updateRow(input, handler, username, password, AuthenticationMode.SESSION);
  }
  
  
  public T updateRow(T input, TableHandler handler, String username, String password, AuthenticationMode authMode) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId;
      if (handler.supportsCollisionDetection()) {
        wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
        wsInvocationId.setAuthenticationMode(authMode);
      } else {
        wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier(AuthenticationTools.EDIT_OPERATION_IDENTIFIER, authMode);
      }
      if (authMode == AuthenticationMode.LEGACY) {
        AuthenticationTools.authenticate(username, password, logger);
        AuthenticationTools.checkPermissionsDBEdit(username, handler.getDBTableInfo().getSchema(), logger);
      } else {
        AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      }
      //AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      T ret = new DBCommandHandler<T>().updateRow(input, reader, table, map, wsInvocationId, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in UpdateRow.", e);
    }
  }
      
  /**
   * Deletes rows in a management table.
   * Parameter input is used as a condition mask;
   * null or empty values in parameter "input" will not be used for the condition.
   * Returns a message string with the number of deleted rows.
   */
  public String deleteRows(T input, HeaderDataBean headerData) throws RemoteException {
    return deleteRows(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String deleteRows(T input, TableHandler handler, String username, String password) throws RemoteException {
    return deleteRows(input, handler, username, password, false);
  }
  
  public String deleteRows(T input, TableHandler handler, String username, String password, boolean useLegayAuthentication) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.DELETION_WEBSERVICE_IDENTIFIER.clone();
      if (useLegayAuthentication) {
        AuthenticationTools.authenticate(username, password, logger);
        AuthenticationTools.checkPermissionsDBEdit(username, handler.getDBTableInfo().getSchema(), logger);
      } else {
        AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      }
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      String ret = new DBCommandHandler<T>().deleteRows(input, reader, table, map, wsInvocationId, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in DeleteRows.", e);
    }
  }
  
  
  /**
   * Deletes rows in a management table.
   * Parameter input is used as a condition mask;
   * null or empty values in parameter "input" will actually be used for the condition and match NULL values.
   * Returns a message string with the number of deleted rows.
   */
  public String deleteRowsWithNullConditions(T input, HeaderDataBean headerData) throws RemoteException {
    return deleteRowsWithNullConditions(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String deleteRowsWithNullConditions(T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId;
      if (handler.supportsCollisionDetection()) {
        wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.DELETION_WEBSERVICE_IDENTIFIER.clone();
      } else {
        wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier(AuthenticationTools.EDIT_OPERATION_IDENTIFIER);
      }
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      String ret = new DBCommandHandler<T>().deleteRowsWithNullConditions(input, reader, table, map, wsInvocationId, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in DeleteRows.", e);
    }
  }


  /**
   * Updates rows in a management table.
   * Parameter newvalues supplies the new values to set in the update;
   * empty values in parameter newvalues will be set as null or empty strings
   * (can be configured in parameter handler);
   * parameter conditions is used as a mask to build the where condition;
   * empty values in parameter conditions will not be used for the condition.
   * That means the condition could theoretically fit to more than one row;
   * that case should be avoided, however:
   * It is assumed here instead that only one row fits to the condition.
   * That is not explicitly checked, however, so updating more than one row with one call
   * is possible, though not recommended.
   * 
   * Return value is one row which is the result of a select with parameter newvalues used
   * as a condition mask. 
   * (It is assumed here that that condition selects only one row; if that is not the case,
   * unexpected behavior can occur.)
   */
  public T updateRowPk(T conditions, T newvalues, HeaderDataBean headerData) throws RemoteException {
    return updateRowPk(conditions, newvalues, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T updateRowPk(T conditions, T newvalues, TableHandler handler, String username, String password) 
         throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, newvalues, logger);    
      T ret = new DBCommandHandler<T>().updateRowPk(reader, table, conditions, newvalues, wsInvocationId, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in UpdateRowPk.", e);
    }
  }
  

  /**
   * Updates rows in a management table.
   * Parameter newvalues supplies the new values to set in the update;
   * empty values in parameter newvalues will be set as null or empty strings
   * (can be configured in parameter handler);
   * parameter conditions is used as a mask to build the where condition;
   * empty values in parameter conditions will be used for the condition to match NULL values.
   * That means the condition could fit to more than one row.
   * 
   * Return value is one row which is the result of a select with parameter newvalues used
   * as a condition mask. 
   * (It is assumed here that that condition selects only one row; if that is not the case,
   * unexpected behaviour can occur.)
   */
  public T updateRowPkWithNullConditions(T conditions, T newvalues, HeaderDataBean headerData) throws RemoteException {
    return updateRowPkWithNullConditions(conditions, newvalues, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T updateRowPkWithNullConditions(T conditions, T newvalues, TableHandler handler, String username, 
         String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, newvalues, logger);    
      TreeMap<String, String> newValuesMap = new ReflectionTools<T>(newvalues).getRowMap(table, newvalues, logger);
      TreeMap<String, String> conditionsMap = new ReflectionTools<T>(conditions).getRowMap(table, conditions, logger);
      T ret = new DBCommandHandler<T>().updateRowPkWithNullConditions(reader, table, conditionsMap, newValuesMap, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in UpdateRowPk.", e);
    }
  }
  
  
  /**
   * Returns a list of the configured locations (DPP instances).
   * 
   */
  public static String[] getLocations(HeaderDataBean headerData) throws RemoteException {
    return getLocations(headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public static String[] getLocations(TableHandler handler, String username, String password) 
         throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      String[] ret = LocationData.getInstance(handler.getLocationSchema(), logger).getAllLocations(logger);
      return ret;      
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetLocations.", e);
    }
  }

  
  /**
   * Just like method getAllRows, only operates on several analog tables on several locations (DPP instances).
   */
  public ResultFromLocations<T> getAllRowsAllLocations(T ref, HeaderDataBean headerData) throws RemoteException {
    return getAllRowsAllLocations(ref, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public ResultFromLocations<T> getAllRowsAllLocations(T ref, TableHandler handler, String username, 
        String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      ResultFromLocations<T> result = new LocationHandler<T>().getAllRowsAllLocations(ref, 
          handler.getLocationSchema(), table, logger);
      return result;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRowsAllLocations.", e);
    }
  }

  /**
   * Just like method searchRows, only operates on several analog tables on several locations (DPP instances).
   */
  public ResultFromLocations<T> searchRowsOfLocations(T input, HeaderDataBean headerData) throws RemoteException {
    return searchRowsOfLocations(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public ResultFromLocations<T> searchRowsOfLocations(T input, TableHandler handler, String username, 
        String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      ReflectionTools<T> reftools = new ReflectionTools<T>(input);
      TreeMap<String, String> map = reftools.getRowMap(table, input, logger);
      LocationSchema schema = handler.getLocationSchema();
      String location = "";
      if (schema == LocationSchema.service) {
        location = reftools.getLocationValue(input);
      } else {
        throw new DPPWebserviceIllegalArgumentException("WebserviceHandler: Illegal value for schema: " + schema);
      }
      if ((location == null) || (location.trim().equals(""))) {
        location = "";
      }
      ResultFromLocations<T> result = new LocationHandler<T>().searchRowsOfLocations(input, table, map,
           location, schema, logger);
      return result;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
    

  /**
   * Just like method insertRow, only operates on several analog tables on several locations (DPP instances).
   */
  public T insertRowOfLocation(T input, HeaderDataBean headerData) throws RemoteException {
    return insertRowOfLocation(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T insertRowOfLocation(T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.DELETION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      ReflectionTools<T> reftools = new ReflectionTools<T>(input);
      TreeMap<String, String> map = reftools.getRowMap(table, input, logger);
      LocationSchema schema = handler.getLocationSchema();
      String location = reftools.getLocationValue(input);
      T ret = new LocationHandler<T>().insertRowOfLocation(input, schema, table, map, location, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  

  /**
   * Just like method updateRow, only operates on several analog tables on several locations (DPP instances).
   */
  public T updateRowOfLocation(T input, HeaderDataBean headerData) throws RemoteException {
    return updateRowOfLocation(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T updateRowOfLocation(T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      ReflectionTools<T> reftools = new ReflectionTools<T>(input);
      TreeMap<String, String> map = reftools.getRowMap(table, input, logger);
      LocationSchema schema = handler.getLocationSchema();
      String location = reftools.getLocationValue(input);
      T ret = new LocationHandler<T>().updateRowOfLocation(input, table, schema, map, location, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
      

  /**
   * Just like method deleteRows, only operates on several analog tables on several locations (DPP instances).
   */
  public String deleteRowsOfLocation(T input, HeaderDataBean headerData) throws RemoteException {
    return deleteRowsOfLocation(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String deleteRowsOfLocation(T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.DELETION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      ReflectionTools<T> reftools = new ReflectionTools<T>(input);
      TreeMap<String, String> map = reftools.getRowMap(table, input, logger);
      LocationSchema schema = handler.getLocationSchema();
      String location = reftools.getLocationValue(input);
      String ret = new LocationHandler<T>().deleteRowsOfLocation(input, schema, table, map, location, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  
  /**
   * Returns for one column in a management table all distinct values.
   */
  public static  List<String> getColValuesDistinct(String colname, HeaderDataBean headerData) throws RemoteException {
    return getColValuesDistinct(colname, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public static  List<String> getColValuesDistinct(String colname, TableHandler handler, String username, 
          String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      if (colname == null) {
        throw new DPPWebserviceIllegalArgumentException("Name of column is missing.");
      }
      if (colname.trim().equals("")) {
        throw new DPPWebserviceIllegalArgumentException("Name of column is missing.");
      }
      if (colname.trim().equals("?")) {
        throw new DPPWebserviceIllegalArgumentException("Name of column is missing.");
      }
      List<String> ret = QueryTools.getColValuesDistinct(colname, handler.getDBTableInfo(), handler.getLogger()); 
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  
  /**
   * Performs a special select on the leases tables.
   */
  public List<T> searchLeases(T ref, String type, HeaderDataBean headerData) throws RemoteException {
    return searchLeases(ref, type, headerData, false);
  }
  
  
  public List<T> searchLeases(T ref, String type, HeaderDataBean headerData, boolean leasesv6) throws RemoteException {
    return searchLeases(ref, type, headerData.getUsername(), headerData.getPassword(), headerData.getTablehandler(), leasesv6);
  }
  
  
  public List<T> searchLeases(T ref, String type, String username, String password, TableHandler handler) throws RemoteException {
    return searchLeases(ref, type, username, password, handler, false);
  }
  
  
  public List<T> searchLeases(T ref, String type, String username, String password, TableHandler handler, boolean leasesv6) throws RemoteException {
    Logger logger = handler.getLogger();
    try {       
      LeasesTools<T> leases = new LeasesTools<T>();
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      return leases.searchLeases(ref, type, logger, leasesv6);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  

  /**
   * As updateRowPk, only that empty values in parameter newvalues will not mean the column
   * is set to null or empty string, but ignored so that the old value remains.
   * 
   * Is also supposed to be used to update more than one row (if the conditions parameter
   * indicates that).
   * 
   * Returns the number of rows that were updated.
   */
  public String updateRowPkIgnoreEmpty(T conditions, T newvalues, HeaderDataBean headerData) throws RemoteException {
    return updateRowPkIgnoreEmpty(conditions, newvalues, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String updateRowPkIgnoreEmpty(T conditions, T newvalues, TableHandler handler, String username, 
         String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      //AuthenticationTools.authenticate(username, password, logger);
      //AuthenticationTools.checkPermissionsDBEdit(username, handler.getDBTableInfo().getSchema(), logger);
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, newvalues, logger);    
      TreeMap<String, String> newValuesMap = new ReflectionTools<T>(newvalues).getRowMap(table, newvalues, logger);
      TreeMap<String, String> conditionsMap = new ReflectionTools<T>(conditions).getRowMap(table, conditions, logger);
      String ret = new DBCommandHandler<T>().updateRowPkIgnoreEmpty(reader, table, conditionsMap, newValuesMap, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in UpdateRowPkIgnoreEmpty.", e);
    }
  }
  

  /**
   * Special insert operation for table textconfigtemplate.
   */
  public T insertRowTextConfigTemplate(T input, String typename, String constraintsScore, HeaderDataBean headerData) throws RemoteException {
    return insertRowTextConfigTemplate(input, typename, constraintsScore, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T insertRowTextConfigTemplate(T input, String typename, String constraintsScore, 
          TableHandler handler, String username, String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.INSERTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      TextConfigTemplateTools.checkTypenameScoreConstraint(typename, constraintsScore, handler.getLogger());
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      T ret = new DBCommandHandler<T>().insertRow(input, reader, table, map, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in InsertRowTextConfigTemplate.", e);
    }
  }
  
  
  /**
   * Special update operation for table textconfigtemplate.
   */
  public T updateRowTextConfigTemplate(T input, String id, String typename, String constraintsScore, HeaderDataBean headerData) throws RemoteException {
    return updateRowTextConfigTemplate(input, id, typename, constraintsScore, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T updateRowTextConfigTemplate(T input, String id, String typename, String constraintsScore, 
          TableHandler handler, String username, String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      TextConfigTemplateTools.checkTypenameScoreConstraint(id, typename, constraintsScore, handler.getLogger());
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      T ret = new DBCommandHandler<T>().updateRow(input, reader, table, map, wsInvocationId, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in UpdateRowTextConfigTemplate.", e);
    }
  }
  
  
  /**
   * Special insert operation for table statichost
   */
  public T insertRowStaticHost(T input, HeaderDataBean headerData) throws RemoteException {
    return insertRowStaticHost(input, headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T insertRowStaticHost(T input, String username, String password) throws RemoteException {
    TableHandler handler = StaticHostTools.getHandler();
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.INSERTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      StaticHostTools.adjustDns(map, logger);
      T ret = new DBCommandHandler<T>().insertRow(input, reader, table, map, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in InsertRowStaticHost.", e);
    }
  }
  
  /**
   * Special update operation for table statichost
   */
  public T updateRowStaticHost(T input, HeaderDataBean headerData) throws RemoteException {
    return updateRowStaticHost(input, headerData.getUsername(), headerData.getPassword());
  }
  
  
  public T updateRowStaticHost(T input, String username, String password) throws RemoteException {
    TableHandler handler = StaticHostTools.getHandler();
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.MODIFICATION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      logger.warn("updateRowStaticHost: " + wsInvocationId.getSessionId());
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      StaticHostTools.adjustDns(map, logger);
      if (!StaticHostTools.editIsAllowed(map, logger)) {
        throw new DPPWebserviceIllegalArgumentException("The indicated row in table StaticHost may not be "
            + "updated.");
      }
      T ret = new DBCommandHandler<T>().updateRow(input, reader, table, map, wsInvocationId, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in UpdateRowStaticHost.", e);
    }
  }
  
  /**
   * Special delete operation for table statichost
   */
  public String deleteRowStaticHost(T input, HeaderDataBean headerData) throws RemoteException {
    return deleteRowStaticHost(input, headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String deleteRowStaticHost(T input, String username, String password) throws RemoteException {
    TableHandler handler = StaticHostTools.getHandler();
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.DELETION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);    
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      logger.warn("deleteRowStaticHost build map: " + map);
      if (!StaticHostTools.editIsAllowed(map, logger)) {
        throw new DPPWebserviceIllegalArgumentException("The indicated row in table StaticHost may not be "
            + "deleted.");
      }
      String ret = new DBCommandHandler<T>().deleteRows(input, reader, table, map, wsInvocationId, logger);      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in DeleteRows.", e);
    }
  }
  

  /**
   * Special select operation for leases tables
   */
  public List<T> getAllRowsLeases(T ref, HeaderDataBean headerData) throws RemoteException {
    return getAllRowsLeases(ref, headerData, false);
  }
  
  public List<T> getAllRowsLeases(T ref, HeaderDataBean headerData, boolean leasesv6) throws RemoteException {
    return getAllRowsLeases(ref, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword(), leasesv6);
  }
  
  
  public List<T> getAllRowsLeases(T ref, TableHandler handler, String username, String password) 
    throws RemoteException {
    return getAllRowsLeases(ref, handler, username, password, false);
  }
  
  public List<T> getAllRowsLeases(T ref, TableHandler handler, String username, String password, boolean leasesv6) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      List<T> ret = new LeasesTools<T>().getAllRows(ref, logger, leasesv6);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRowsLeases", e);
    }
  }
  
  
  /**
   * Special operations for IPv6-CPEs
   */
  public List<T> searchLeasesForCPEs(T input, TableHandler handler, List<DBTableInfo> tables, String username, String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      return new LeasesTools<T>().searchLeasesForCPEs(input, tables, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchLeasesForCPEs", e);
    }
  }
  
  
  public int countRowsWithConditionForCPEs(T input, TableHandler handler, List<DBTableInfo> tables, String username, String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      return new LeasesTools<T>().countWithConditionsForCPEs(input, tables, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in CountRowsWithConditionForCPEs", e);
    }
  }
  
  
  public List<T> searchLeases(T input, TableHandler handler, List<DBTableInfo> tables, String username, String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      return new LeasesTools<T>().searchRows(input, tables, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchLeasesForCPEs", e);
    }
  }
  
  
  public int countLeasesWithCondition(T input, TableHandler handler, List<DBTableInfo> tables, String username, String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      return new LeasesTools<T>().countRowsWithCondition(input, tables, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchLeasesForCPEs", e);
    }
  }
  

  /**
   * Special select operation for leases tables.
   */  
  public List<T> searchRowsLeases(String table, T input, HeaderDataBean headerData) throws RemoteException {
    return searchRowsLeases(table, input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
    
  
  public List<T> searchRowsLeases(String table, T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      List<T> ret = new LeasesTools<T>().searchRows(input, table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRowsLeases.", e);
    }
  }
  
  
  /**
   * Returns the encrypted value of the parameter value. 
   */
  public String encrypt(String value, String schema, HeaderDataBean headerData, Logger logger) throws RemoteException {
    return encrypt(value, schema, headerData.getUsername(), headerData.getPassword(), logger);
  }
  
  
  public String encrypt(String value, String schema, String username, String password, Logger logger) 
          throws RemoteException {
    try {
      // FIXME AuthenticationTools.authenticate(username, password, logger);
      // Who calls this, how can we let this pass (check the SESSION_CREATION-right?)
      return WSTools.encrypt(value, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in Encrypt.", e);
    }
  }
  
  
  /**
   * Returns the decrypted value of the parameter value.
   */
  public String decrypt(String value, String schema, HeaderDataBean headerData, Logger logger) throws RemoteException {
    return decrypt(value, schema, headerData.getUsername(), headerData.getPassword(), logger);
  }
  
  
  public String decrypt(String value, String schema, String username, String password, Logger logger) 
          throws RemoteException {
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new WebServiceInvocationIdentifier("decrypt");
      AuthenticationTools.authenticateAndAuthorize(username, password, schema, wsInvocationId, logger);
      return WSTools.decrypt(value, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in Encrypt.", e);
    }
  }
  

  /**
   * delete rows that fit to the mask supplied by parameter input in location also supplied in input 
   * and insert them into location newLocation
   */
  public String moveRowsChangeLocation(T input, String newLocation, HeaderDataBean headerData) throws RemoteException {
    return moveRowsChangeLocation(input, newLocation, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String moveRowsChangeLocation(T input, String newLocation, TableHandler handler, String username, 
        String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      if ((newLocation == null) || (newLocation.trim().length()<1)) {
        throw new DPPWebserviceIllegalArgumentException("MoveRowsChangeLocation: No destination location supplied.");
      }
      DBTableInfo table = handler.getDBTableInfo();
      ReflectionTools<T> reftools = new ReflectionTools<T>(input);
      TreeMap<String, String> map = reftools.getRowMap(table, input, logger);
      LocationSchema schema = handler.getLocationSchema();
      String oldlocation = "";
      if (schema == LocationSchema.service) {
        oldlocation = reftools.getLocationValue(input);
      } else {
        throw new DPPWebserviceIllegalArgumentException("WebserviceHandler: Illegal value for schema: " + schema);
      }
      if ((oldlocation == null) || (oldlocation.trim().length()<1)) {
        throw new DPPWebserviceIllegalArgumentException("MoveRowsChangeLocation: No source location supplied.");
      }
      return new LocationHandler<T>().moveRowsChangeLocation(input, table, map, oldlocation, newLocation, 
          schema, reftools, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  
  /**
   * count rows that fit to condition mask in parameter input;
   * if input specifies a location name, count only in that location, otherwise in all locations
   */
  public String countRowsWithConditionOfLocations(T input, HeaderDataBean headerData) throws RemoteException {
    return countRowsWithConditionOfLocations(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String countRowsWithConditionOfLocations(T input, TableHandler handler, String username, 
        String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      ReflectionTools<T> reftools = new ReflectionTools<T>(input);
      TreeMap<String, String> map = reftools.getRowMap(table, input, logger);
      LocationSchema schema = handler.getLocationSchema();
      String location = "";      
      if (schema == LocationSchema.service) {
        location = reftools.getLocationValue(input);
      } else {
        throw new DPPWebserviceIllegalArgumentException("WebserviceHandler: Illegal value for schema: " + schema);
      }
      if ((location == null) || (location.trim().equals(""))) {
        return "" + QueryTools.countRowsWithConditionAllLocations(table, schema, map, logger); 
      }      
      return "" + QueryTools.countRowsWithConditionOfLocation(table, schema, map, location, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  
  /**
   * count all rows in all locations for the table specified in parameter handler
   */
  public String countAllRowsAllLocations(HeaderDataBean headerData) throws RemoteException {
    return countAllRowsAllLocations(headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String countAllRowsAllLocations(TableHandler handler, String username, 
        String password) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      LocationSchema schema = handler.getLocationSchema();
      return "" + QueryTools.countAllRowsAllLocations(table, schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  

  /**
   * count all rows in a management table that fit to condition mask supplied by parameter input 
   */
  public String countRowsWithCondition(T input, HeaderDataBean headerData) throws RemoteException {
    return countRowsWithCondition(input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String countRowsWithCondition(T input, TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      String ret = "" + QueryTools.countRowsWithCondition(table, map, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  

  /**
   * Count all rows without condition in a management table.
   */
  public String countAllRows(HeaderDataBean headerData) throws RemoteException {
    return countAllRows(headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword());
  }
  
  
  public String countAllRows(TableHandler handler, String username, String password) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      DBTableInfo table = handler.getDBTableInfo();
      String ret = "" + QueryTools.countAllRows(table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }


  /**
   * Count all rows of leases tables
   */
  public String countAllRowsLeases(HeaderDataBean headerData) throws RemoteException {
    return countAllRowsLeases(headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword(), false);
  }
  
  public String countAllRowsLeases(HeaderDataBean headerData, boolean leasesv6) throws RemoteException {
    return countAllRowsLeases(headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword(), leasesv6);
  }
  
  public String countAllRowsLeases(TableHandler handler, String username, String password) 
  throws RemoteException {
    return countAllRowsLeases(handler, username, password, false);
  }
  
  public String countAllRowsLeases(TableHandler handler, String username, String password, boolean leasesv6) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      int ret = new LeasesTools<T>().countAllRows(logger, leasesv6);
      return "" + ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error", e);
    }
  }
  
  
  /**
   * count rows in leases tables that fit to condition supplied in parameter input 
   */
  public String countRowsWithConditionLeases(String table, T input, HeaderDataBean headerData) throws RemoteException {
    return countRowsWithConditionLeases(table, input, headerData, false);
  }
  
  
  public String countRowsWithConditionLeases(String table, T input, HeaderDataBean headerData, boolean leasesv6) throws RemoteException {
    return countRowsWithConditionLeases(table, input, headerData.getTablehandler(), headerData.getUsername(), headerData.getPassword(), leasesv6);
  }
  
  
  public String countRowsWithConditionLeases(String table, T input, TableHandler handler, String username, 
                                             String password) throws RemoteException {
    return countRowsWithConditionLeases(table, input, handler, username, password, false);
  }
  
  
  public String countRowsWithConditionLeases(String table, T input, TableHandler handler, String username, 
          String password, boolean leasesv6) throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      int ret = new LeasesTools<T>().countRowsWithCondition(input, table, logger, leasesv6);
      return "" + ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }

  /**
   * count rows in leases tables that fit to the parameter type 
   */
  public String countLeases(String type, HeaderDataBean headerData) throws RemoteException {
    return countLeases(type, headerData, false);
  }
  
  
  public String countLeases(String type, HeaderDataBean headerData, boolean leasesv6) throws RemoteException {
    return countLeases(type, headerData.getUsername(), headerData.getPassword(), headerData.getTablehandler());
  }
  

  public String countLeases(String type, String username, String password, TableHandler handler) 
  throws RemoteException {
    return countLeases(type, username, password, handler, false);
  }
  
  
  public String countLeases(String type, String username, String password, TableHandler handler, boolean leasesv6) 
          throws RemoteException {
    Logger logger = handler.getLogger();
    try {
      LeasesTools<T> leases = new LeasesTools<T>();
      AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = AuthenticationTools.WebServiceInvocationIdentifier.SELECTION_WEBSERVICE_IDENTIFIER.clone();
      AuthenticationTools.authenticateAndAuthorize(username, password, handler.getDBTableInfo().getSchema().toLowerCase(), wsInvocationId, handler);
      int ret = leases.countLeases(type, logger, leasesv6);
      return "" + ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
}

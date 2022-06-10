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

import java.rmi.RemoteException;
import java.util.List;
import java.util.TreeMap;


import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.*;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.xyna.utils.db.ResultSetReader;

/**
 * Class whose methods actually execute database commands (by calling xyna database utils).
 *
 * Compiles only with Java 1.6!
 *
 * The methods use the following parameters:
 *
 * The SQL command is specified with a parameter of type SQLCommand.
 *
 * That command is executed only on one database (schema), which is either specified by
 * a parameter of type SQLUtilsContainer (containing an instance of SQLUtils with an
 * open connection), or, in case of databases on management computers, by the name of
 * the management database (schema), so that the necessary instance of SQLUtilsContainer
 * can be created internally.
 *
 * Query methods also need a parameter of type ResultSetReader.
 *
 */
public class DBCommands<T> {


  public List<T> query(ResultSetReader<T> reader, DBTableInfo table, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
    return query(reader, schema, builder, logger);
  }

  public List<T> query(ResultSetReader<T> reader, DBSchema schema, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    SQLUtilsContainerForManagement sql = SQLUtilsCache.getForManagement(schema, logger);
    return query(reader, builder, sql, logger);
  }

  /**
   * performs a SQL query
   */
  public List<T> query(ResultSetReader<T> reader, SQLCommand builder, SQLUtilsContainer cont, Logger logger)
        throws java.rmi.RemoteException {
    SQLUtilsContainer container = cont;
    try {
      if (builder.sql.equals("")) {
        logger.error("DBCommands: Unable to build SQL Command.");
        throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
      }
      logger.info("SQL Builder-sql: " + builder.sql);
      int attemptsleft = getRetryAmount(logger);
      List<T> ret = null;
      try {
        while (attemptsleft >0) {
          ret = container.getSQLUtils().query(builder.sql, builder.buildParameter(), reader);
          Exception e = container.getSQLUtils().getLastException();
          if (e == null) {
            attemptsleft = 0;
            logger.info("Select returns " + ret.size() + " rows.");
          }
          else if (doesExceptionWarrantARetry(e)) {
              try {
                Thread.sleep(getRetryWaitInMillis(logger));
              } catch (Throwable t) { }

              container = refreshContainer(container, logger);
              attemptsleft--;
              if (attemptsleft <= 0) {
                logger.error("Stopped attempts to repeat.");
                throw new DPPWebserviceDatabaseException(e.toString());
              }
              logger.error("Going to try again with fresh connection...");
          }
          else {
            logger.error("Database Exception, no repeat will be tried :", e);
            container = refreshContainer(container, logger);
            throw new DPPWebserviceDatabaseException(e);
          }
        }
        return ret;
      }
      catch (java.rmi.RemoteException e) {
        throw e;
      }
      catch (Exception e) {
        container = refreshContainer(container, logger);
        throw e;
      }
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in DBCommands.query ", e);
    }
    finally {
      SQLUtilsCache.release(container, logger);
    }
  }



  public static int executeDML(DBTableInfo table, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
    return executeDML(schema, builder, logger);
  }

  public static int executeDML(DBSchema schema, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    SQLUtilsContainerForManagement sql = SQLUtilsCache.getForManagement(schema, logger);
    return executeDML(builder, sql, logger);
  }


  /**
   * performs a SQL DML command (insert, update or delete)
   */
  public static int executeDML(SQLCommand builder, SQLUtilsContainer cont, Logger logger)
          throws java.rmi.RemoteException {
    SQLUtilsContainer container = cont;
    try {
      if (builder.sql.equals("")) {
        logger.error("DBCommands: Unable to build SQL Command.");
        throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
      }
      logger.info("SQL Builder-sql: " + builder.sql);
      int attemptsleft = getRetryAmount(logger);
      try {
        int rows = 0;
        while (attemptsleft >0) {
          rows = container.getSQLUtils().executeDML(builder.sql, builder.buildParameter());
          container.getSQLUtils().commit();
          Exception e = container.getSQLUtils().getLastException();
          if (e == null) {
            attemptsleft = 0;
          }
          else if (doesExceptionWarrantARetry(e)) {
            try {
              Thread.sleep(getRetryWaitInMillis(logger));
            } catch (Throwable t) { }
            container = refreshContainer(container, logger);
            attemptsleft--;
            if (attemptsleft <= 0) {
              logger.error("Stopped attempts to repeat.");
              throw new DPPWebserviceDatabaseException(e);
            }
            logger.error("Going to try again with fresh connection...");
            //container = SQLUtilsCache.getFresh(container, logger);
          } else {
            logger.error("Database Exception, no repeat will be tried :", e);
            if (e instanceof com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException) {
              WSTools.HandleIntegrityConstraintViolation(logger,
                  (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException) e);
            }
            container = refreshContainer(container, logger);
            throw new DPPWebserviceDatabaseException(e);
          }
        }
        return rows;
      }
      catch (java.rmi.RemoteException e) {
        throw e;
      }
      catch (Exception e) {
        container = refreshContainer(container, logger);
        throw e;
      }
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in DBCommands.executeDML: ", e);
    }
    finally {
      //logger.info("Entering finally block");
      SQLUtilsCache.release(container, logger);
    }
  }


  public T queryOneRow(ResultSetReader<T> reader, DBTableInfo table,
        TreeMap<String, String> map, Logger logger) throws java.rmi.RemoteException {
    SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
    return queryOneRow(reader, table, builder, logger);
  }

  public T queryOneRow(ResultSetReader<T> reader, DBTableInfo table, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
    return queryOneRow(schema, reader, builder, logger);
  }


  public T queryOneRow(String schemaName, ResultSetReader<T> reader, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(schemaName, logger);
    return queryOneRow(schema, reader, builder, logger);
  }

  public T queryOneRow(DBSchema schema, ResultSetReader<T> reader, SQLCommand builder, Logger logger)
          throws java.rmi.RemoteException {
    SQLUtilsContainerForManagement sql = SQLUtilsCache.getForManagement(schema, logger);
    return queryOneRow(reader, builder, sql, logger);
  }


  /**
   * performs a SQL query which returns only one row
   */
  public T queryOneRow(ResultSetReader<T> reader, SQLCommand builder, SQLUtilsContainer cont, Logger logger)
          throws java.rmi.RemoteException {
    SQLUtilsContainer container = cont;
    try {
      if (builder.sql.equals("")) {
        logger.error("DBCommands: Unable to build SQL Command.");
        throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
      }
      logger.info("SQL Builder-sql: " + builder.sql);
      try {
        int attemptsleft = getRetryAmount(logger);
        T ret = null;
        while (attemptsleft >0) {
          ret = container.getSQLUtils().queryOneRow(builder.sql, builder.buildParameter(), reader);
          Exception e = container.getSQLUtils().getLastException();
          if (e == null) {
            attemptsleft = 0;
          } else if (doesExceptionWarrantARetry(e)) {
              try {
                Thread.sleep(getRetryWaitInMillis(logger));
              } catch (Throwable t) { }
              container = refreshContainer(container, logger);
              attemptsleft--;
              if (attemptsleft <= 0) {
                logger.error("Stopped attempts to repeat.");
                throw new DPPWebserviceDatabaseException(e);
              }
              logger.error("Going to try again with fresh connection...");
              //container = SQLUtilsCache.getFresh(container, logger);
          } else {
            logger.error("Database Exception, no repeat will be tried :", e);
            container = refreshContainer(container, logger);
            throw new DPPWebserviceDatabaseException(e);
          }
        }
        return ret;
      }
      catch (java.rmi.RemoteException e) {
        throw e;
      }
      catch (Exception e) {
        container = refreshContainer(container, logger);
        throw e;
      }
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in DBCommands.queryOneRow: ", e);
    }
    finally {
      SQLUtilsCache.release(container, logger);
    }
  }


  private static SQLUtilsContainer refreshContainer(SQLUtilsContainer container, Logger logger)
                 throws RemoteException {
    try {
      container.getSQLUtils().closeConnection();
    }
    catch (Exception e1) {
      //do nothing
    }
    SQLUtilsContainer ret = SQLUtilsCache.getFresh(container, logger);
    return ret;
  }
  
  
  private static final String PROPERTY_RETRY_AMOUNT = "dbconnection.retries.amount";
  private static final int DEFAULT_RETRY_AMOUNT = 4;
  private static final String PROPERTY_RETRY_WAITBETWEEN = "dbconnection.retries.waitbetween.ms";
  private static final int RETRY_WAITBETWEEN = 100;
  
  private static int getRetryAmount(Logger logger) {
    try {
      return PropertiesHandler.getIntProperty(PropertiesHandler.getWsProperties(), PROPERTY_RETRY_AMOUNT, logger);
    } catch (RemoteException e) {
      return DEFAULT_RETRY_AMOUNT;
    }
  }
  
  
  private static int getRetryWaitInMillis(Logger logger) {
    try {
      return PropertiesHandler.getIntProperty(PropertiesHandler.getWsProperties(), PROPERTY_RETRY_WAITBETWEEN, logger);
    } catch (RemoteException e) {
      return RETRY_WAITBETWEEN;
    }
  }
  
  
  private static boolean doesExceptionWarrantARetry(Exception e) {
    if ((e instanceof com.mysql.jdbc.CommunicationsException)
        || (e instanceof com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException)
        || (e instanceof com.mysql.jdbc.exceptions.jdbc4.CommunicationsException)
        || (e instanceof com.mysql.jdbc.exceptions.MySQLTransactionRollbackException)
        || (e instanceof com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException)) {
      return true;
    } else if (e instanceof java.sql.SQLException && e.getMessage().contains("try restarting transaction")) {
      return true;
    } else {
      return false;
    }
  }

}

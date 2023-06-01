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
package com.gip.xyna.utils.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.exception.DBUtilsException;

/**
 * DBConnectionData sammelt alle Daten, die zum Öffnen einer Connection zur DB nötig sind.
 * Die Methode {@link createSQLUtils} erzeugt eine fertige SQLUtils-Instanz.
 * 
 * Diese Klasse kann sowohl für MySQL als auch für Oracle-Connections verwendet werden.
 * Der zu verwendende Treiber kann entweder übergeben werden oder 
 * kann aus der URL erkannt werden.  
 * 
 * Aufruf:
 * {@code 
   DBConnectionData dbd = DBConnectionData.newDBConnectionData().
      user("test").
      password("testPwd").
      url("jdbc:mysql://127.0.0.1/testdb").
      build(); 
    SQLUtils sqlUtils = dbd.createSQLUtils( new SQLUtilsLoggerImpl( logger ) );
 * }
 * 
 *
 */
public class DBConnectionData {

  
  private static final Logger logger = Logger.getLogger(DBConnectionData.class);
  
  private static final String classNameOfClassLoaderHelper = ClassLoaderHelper.class.getName();
  public static final String DRIVER_MYSQL = "com.mysql.jdbc.Driver";
  public static final String DRIVER_ORACLE = "oracle.jdbc.OracleDriver";
  public static final String DRIVER_SQLSERVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  public static final String DRIVER_DB2 = "com.ibm.db2.jcc.DB2Driver";
  public static final String DRIVER_MARIADB = "org.mariadb.jdbc.Driver";
  
  public static final String PROPERTY_USER = "user";
  public static final String PROPERTY_PASSWORD = "password";
  
  public enum Type {
    Oracle, MySQL, DB2, SQLServer, MARIADB, Other;
  }
  
  public static final Map<Type,String> DRIVERS;
  static {
    EnumMap<Type,String> m = new EnumMap<Type,String>(Type.class);
    m.put( Type.Oracle, DRIVER_ORACLE );
    m.put( Type.MySQL, DRIVER_MYSQL );
    m.put( Type.DB2, DRIVER_DB2 );
    m.put( Type.SQLServer, DRIVER_SQLSERVER);
    m.put( Type.MARIADB, DRIVER_MARIADB);
    DRIVERS = Collections.unmodifiableMap(m);
  }
  
  private String driver;
  private Properties properties;
  private String url;
  private String clientInfo;
  private boolean autoCommit = false;
  private int networkTimeout;
  private ClassLoader classLoaderToLoadDriver;
  private Type type;
  
  protected DBConnectionData() {/*Konstruktor darf nur intern verwendet werden*/}
  
  /**
   * @param cd
   */
  public DBConnectionData(DBConnectionData cd) {
    this.driver = cd.driver;
    this.url = cd.url;
    this.properties = (Properties) cd.properties.clone();
    this.autoCommit = cd.autoCommit;
    this.clientInfo = cd.clientInfo;
    this.classLoaderToLoadDriver = cd.classLoaderToLoadDriver;
  }
  
  /**
   * @return
   */
  public static DBConnectionDataBuilder newDBConnectionData() {
    return new DBConnectionDataBuilder();
  }
  
  /**
   * @param cd
   * @return
   */
  public static DBConnectionDataBuilder copyDBConnectionData(DBConnectionData cd) {
    return new DBConnectionDataBuilder(cd);
  }

  /**
   * Baut ein DBConnectionData-Objekt
   *
   */
  public static class DBConnectionDataBuilder {
    
    public static Executor DefaultNetworkTimeoutExecutor = Executors.newCachedThreadPool(); //hier als Default, könnte konfigurierbar werden..

    protected DBConnectionData dcd;
    
    private int connectTimeout = 0;
    
    public DBConnectionDataBuilder() {
      dcd = new DBConnectionData();
      dcd.properties = new Properties();
    }
    public DBConnectionDataBuilder(DBConnectionData cd) {
      dcd = new DBConnectionData(cd);
    }
    
    public DBConnectionDataBuilder type(Type type) {
      dcd.type = type;
      return this;
    }
    
    public DBConnectionDataBuilder driver(String driver) {
      dcd.driver = driver;
      return this;
    }

    public DBConnectionDataBuilder url(String url) {
      dcd.url = url;
      return this;
    }
    
    public DBConnectionDataBuilder user(String user) {
      dcd.properties.setProperty(PROPERTY_USER, user );
      return this;
    }

    public DBConnectionDataBuilder password(String password) {
      dcd.properties.setProperty(PROPERTY_PASSWORD, password );
      return this;
    }
    
    public DBConnectionDataBuilder autoCommit(boolean autoCommit) {
      dcd.autoCommit = autoCommit;
      return this;
    }

    public DBConnectionDataBuilder connectTimeoutInSeconds(int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }
    
    public DBConnectionDataBuilder socketTimeoutInSeconds(int socketTimeout) {
      dcd.networkTimeout = socketTimeout*1000;
      return this;
    }
    
    public DBConnectionDataBuilder clientInfo(String clientInfo) {
      dcd.clientInfo = clientInfo;
      return this;
    }
    
    public DBConnectionDataBuilder property(String key, String value ) {
      dcd.properties.setProperty(key, value);
      return this;
    }
    
    /**
     * der classloader, der imstande ist, die jdbcdriver klasse zu finden
     * @param classLoaderToLoadDriver
     * @return
     */
    public DBConnectionDataBuilder classLoaderToLoadDriver(ClassLoader classLoaderToLoadDriver) {
      dcd.classLoaderToLoadDriver = classLoaderToLoadDriver;
      return this;
    }
    
    private Type inferType(String url, String driver) {
      if( driver != null ) {
        for( Type t : Type.values() ) {
          if( driver.equals(DRIVERS.get(t)) ) {
            return t;
          }
        }
      }
      String lowerURL = url.toLowerCase().trim();
      if( lowerURL.startsWith("jdbc:oracle:") ) {
        return Type.Oracle;
      } else if( lowerURL.startsWith("jdbc:mysql:") ) {
        return Type.MySQL;
      } else if( lowerURL.startsWith("jdbc:db2:") ) {
        return Type.DB2;
      } else if( lowerURL.startsWith("jdbc:sqlserver:") ) {
        return Type.SQLServer;
      } else if( lowerURL.startsWith("jdbc:mariadb:") ) {
        return Type.MARIADB;
      } else {
        throw new IllegalStateException( "Unknown driver for url "+url);
      }
    }
    
    public DBConnectionData build() {
      if( dcd.url == null ) {
        throw new IllegalStateException( "URL is not set" );
      }
      if( dcd.type == null ) {
        dcd.type = inferType(dcd.url, dcd.driver);
      }
      if( dcd.driver == null ) {
        dcd.driver = DRIVERS.get(dcd.type);
      }
      if (dcd.classLoaderToLoadDriver == null) {
        dcd.classLoaderToLoadDriver = dcd.getClass().getClassLoader();
      }
      fillProperties();
      
      return new DBConnectionData(dcd);
    }
    
    
    private void fillProperties() {
      if( connectTimeout != 0 ) {
        DriverManager.setLoginTimeout(connectTimeout);//LoginTimeout in Sekunden, funktioniert nicht bei MySQL. Funktioniert mit MariaDB (3.0.3)
        if( dcd.type == Type.MySQL || dcd.type == Type.MARIADB ) {
          dcd.properties.setProperty("connectTimeout", String.valueOf(connectTimeout*1000) );//Timeout for socket connect (in milliseconds), with 0 being no timeout
        }
      } else {
        dcd.properties.remove("connectTimeout");
      }
      if( dcd.networkTimeout != 0 ) {
        if( dcd.type == Type.MySQL  || dcd.type == Type.MARIADB ) {
          dcd.properties.setProperty("socketTimeout", String.valueOf(dcd.networkTimeout) );//Timeout on network socket operations (0, the default means no timeout). 
        }
        if( dcd.type == Type.Oracle ) {
          dcd.properties.setProperty("oracle.jdbc.ReadTimeout", String.valueOf(dcd.networkTimeout) );//read timeout while reading from the socket. This affects thin driver only. Timeout is in milliseconds.
          dcd.properties.setProperty("oracle.net.READ_TIMEOUT", String.valueOf(dcd.networkTimeout) );//read timeout while reading from the socket. Hopefully this affects the coi driver. Timeout is in milliseconds.
//          props.setProperty("SQLNET.OUTBOUND_CONNECT_TIMEOUT", String.valueOf(""));
        }
      } else {
        dcd.properties.remove("socketTimeout");
        dcd.properties.remove("oracle.jdbc.ReadTimeout");
        dcd.properties.remove("oracle.net.READ_TIMEOUT");
      }
      
      switch( dcd.type ) {
      case MySQL:
      case MARIADB:
        //Für weitere mögliche Properties: http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-configuration-properties.html
        dcd.properties.setProperty("tcpKeepAlive","true");//Tcp Keepalive
        break;
      case Oracle:
        //Für weitere mögliche Properties: http://www.orindasoft.com/public/Oracle_JDBC_JavaDoc/javadoc1020/oracle/jdbc/pool/OracleDataSource.html#setConnectionProperties(java.util.Properties)
        break;
      case DB2:
        break;
      case SQLServer:
        break;
      case Other:
        break;
      }
    }
    
  }

  @Override
  public String toString() {
    return getUser()+"@"+url;
  }
  
  /**
   * @return the driver
   */
  public String getDriver() {
    return driver;
  }


  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  
  /**
   * @return the user
   */
  public String getUser() {
    return properties.getProperty(PROPERTY_USER);
  }


  /**
   * @return the password
   */
  public String getPassword() {
    return properties.getProperty(PROPERTY_PASSWORD);
  }
  
  /**
   * @return autoCommit
   */
  public boolean isAutoCommit() {
    return autoCommit;
  }
  
  /**
   * @return the clientInfo
   */
  public String getClientInfo() {
    return clientInfo;
  }
  
  public Type getType() {
    return type;
  }
  
  /**
   * @return
   * @throws Exception 
   */
  protected Connection createConnectionInternal(String local_url) throws Exception {
    registerDriver();
   
    Connection connection;

    // if a special classloader has been set we have to use a specially loaded proxy class (see below)
    if (classLoaderToLoadDriver != getClass().getClassLoader()) {

      // Use a custom URLClassLoader to load the helper class 
      URLClassLoader helperClassLoader = getClassLoader(classLoaderToLoadDriver);
      
      //TODO reflection kram auch cachen
      
      Class<?> c = helperClassLoader.loadClass(ClassLoaderHelper.class.getName());
      //über reflection getConnection von classloaderhelper aufrufen, weil um ohne connection aufrufen zu können, müsste man 
      //eine instanz auf die klasse casten, wobei man eine classcastexception zur laufzeit bekommt (weil klasse
      //lokal anders ist als die von dem angegebenen classloader geladene).
      Method[] methods = c.getMethods();
      Method method = null;
      for (int i = 0; i < methods.length; i++) {
        if (methods[i].getName().equals("getConnection")) {
          method = methods[i];
          break;
        }
      }
      if (method != null) {
        try {
          connection = (Connection) method.invoke(c.newInstance(), local_url, properties);
        } catch (InvocationTargetException e) {
          Throwable cause = e.getCause();
          if (cause instanceof SQLException) {
            throw (SQLException)cause;
          } else if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
          } else if (cause instanceof Error) {
            throw (Error)cause;
          } else {
            throw new RuntimeException("unexpected type of exception", e.getCause());
          }
        }
      } else {
        throw new RuntimeException("Unexpected error: Expected method does not exist: 'getConnection'");
      }
    } else {
      connection = DriverManager.getConnection(local_url, properties);
    }

    if (connection == null) {
      return null;
    }
    
    connection.setAutoCommit(autoCommit);
    if (clientInfo != null) {
      markConnection(connection, clientInfo);
    }
    
    if( networkTimeout != 0 ) {
      try {
        connection.setNetworkTimeout(DBConnectionDataBuilder.DefaultNetworkTimeoutExecutor, networkTimeout);
      } catch( AbstractMethodError e ) {
        if( ! abstractMethodError_occured ) {
          logger.warn("setNetworkTimeout failed due to not JDBC 4.1 compliant driver. This exception is logged only once.", e);
        }
        abstractMethodError_occured = true;
      }
    }
    
    return connection;
  }
  private static boolean abstractMethodError_occured = false;

  private void registerDriver() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
    if (driverRegistered(driver)) {
      return;
    }
    synchronized (java.sql.DriverManager.class) {
      if (driverRegistered(driver)) {
        return;
      }
      Class<?> driverClass = Class.forName(driver, true, classLoaderToLoadDriver);
      DriverManager.registerDriver((Driver) driverClass.newInstance());
    }
  }


  private boolean driverRegistered(String driverName) {
    for (Driver driver : Collections.list(DriverManager.getDrivers())) {
      if (driver.getClass().getName().equals(driverName) &&
          driver.getClass().getClassLoader().equals(classLoaderToLoadDriver)) {
        return true;
      }
    }
    return false;
  }

  /*
   * cache für urlclassloader, die verwendet werden, um jdbc treiber zu laden.
   * der cache ist dazu da, dass nicht für mehrfaches createconnection unterschiedliche classloader-instanzen verwendet werden
   * und der jdbc treiber dadurch oom wird. (bei oracle jdbc schon passiert)
   * 
   * wenn ein alter classloader nicht mehr verwendet werden, darf hier kein classloaderleak
   * sein -> nur weak reference darauf.
   */
  private static WeakHashMap<ClassLoader, URLClassLoader> classLoaderCache =
      new WeakHashMap<ClassLoader, URLClassLoader>();

  private static URLClassLoader getClassLoader(ClassLoader classLoaderToLoadDriver) {
    URLClassLoader result = classLoaderCache.get(classLoaderToLoadDriver);
    if (result == null) {
      synchronized (classLoaderCache) {
        result = classLoaderCache.get(classLoaderToLoadDriver);
        if (result == null) {
          //FIXME nicht für jeden übergebenen classloader einen helper-classloader erstellen, wenn 
          //diese classloader eigtl alle das jdbc-loading an einen gemeimsamen anderen classloader delegieren
          //beispiel: mehrere black services delegieren alle das jdbc-laden an einen sharedlib-classloader. dann bräuchte
          //man nur den classloaderhelper für den sharedlib-classloader
          ClassLoader classLoaderToGetURLsFrom = DBConnectionData.class.getClassLoader();
          while (!(classLoaderToGetURLsFrom instanceof URLClassLoader) && classLoaderToGetURLsFrom != null) {
            classLoaderToGetURLsFrom = classLoaderToGetURLsFrom.getParent();
          }
          if (classLoaderToGetURLsFrom == null || !(classLoaderToGetURLsFrom instanceof URLClassLoader)) {
            throw new RuntimeException("Unexpected error: Failed to obtain URLClassloader");
          }
          URLClassLoader t =
              new URLClassLoader(((URLClassLoader) classLoaderToGetURLsFrom).getURLs(), classLoaderToLoadDriver) {

                @Override
                public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
                  //synchronized, damit findloadedclass threadsafe wird. sonst kann es passieren, dass für die gleiche klasse zweimal
                  //findclass aufgerufen wird.
                  Class<?> c = findLoadedClass(name);
                  if (c == null) {
                    if (name.equals(classNameOfClassLoaderHelper)) {
                      c = findClass(name);
                    } else {
                      c = super.loadClass(name);                      
                    }                    
                  }
                  return c;
                }

              };
          classLoaderCache.put(classLoaderToLoadDriver, t);
          return t;
        }
      }
    }
    return result;
  }


  /**
   * This is used as a proxy that is loaded by the specified classloader. This way the DriverManager is able to locate
   * the driver classes
   */
  protected static class ClassLoaderHelper {

    public ClassLoaderHelper() {
    }

    public Connection getConnection(String local_url, Properties props) throws SQLException {
      return DriverManager.getConnection(local_url, props);
    }
  }


  /**
   * Anlegen der SQLUtils
   * @param sqlUtilsLogger
   * @return
   */
  public SQLUtils createSQLUtils( SQLUtilsLogger sqlUtilsLogger) {
    Connection connection = null;
    try {
      sqlUtilsLogger.logSQL("Create connection for "+this.toString() );
      connection = createConnectionInternal(url);
      if (connection == null) {
        sqlUtilsLogger.logException(new Exception(
            "Connection could not be created. Propable causes are: No jdbc driver available in classpath of classloader or invalid connection parameters."));
      }
    } catch (Exception e) {
      sqlUtilsLogger.logException(e);
    }
    if( connection != null ) {
      SQLUtils su = new SQLUtils( connection, sqlUtilsLogger );
      su.setName( getUser()+"@"+url );
      return su;
    } else {
      return null;
    }
  }

  public Connection createConnection() throws Exception {
    return createConnectionInternal(url);
  }


  private static boolean isOracleConnection(Connection connection) {
    //nicht instanceof verwenden, das gibt evtl irgendwelche classloading probleme
    return connection != null && connection.getClass().getName().contains("oracle.");
  }


  /**
   * Markieren der Connection mittels der clientInfo
   * @param connection
   * @param clientInfo
   */
  public static void markConnection(Connection connection, String clientInfo) {
    if (!isOracleConnection(connection)) {
      return;
    }
    ConnectionFactory.markConnection(connection, clientInfo);
  }
  
  public static void closeConnection(Connection connection) {
    boolean closed = (null == connection);
    if( closed ) {
      return;
    }
    try {
      closed = connection.isClosed();
    } catch( Throwable t ) {
      //Fehler hier wird unterdrückt, da schlecht behandelbar, 
      //wenn wirklich ein schwerwiegender Fehler auftritt, wird 
      //unten ein DBUtilsException geworfen.
    }
    if( closed ) {
      return;
    }
    markConnection(connection,""); //Markierung entfernen
    try {
      connection.close();
    } catch(Throwable t) {
      throw new DBUtilsException( "Connection could not be closed.", t );
    }
  }
  
  public static void closeConnection( SQLUtils sqlUtils ) {
    boolean closed = (null == sqlUtils || sqlUtils.getConnection() == null );
    if( closed ) {
      return;
    }
    try {
      closed = sqlUtils.getConnection().isClosed();
    } catch( Throwable t ) {
      //Fehler hier wird unterdrückt, da schlecht behandelbar, 
      //wenn wirklich ein schwerwiegender Fehler auftritt, wird 
      //unten ein DBUtilsException geworfen.
    }
    if( closed ) {
      return;
    }
    markConnection(sqlUtils.getConnection(),""); //Markierung entfernen
    sqlUtils.closeConnection();
    if( sqlUtils.getLastException() != null ) {
      throw new DBUtilsException( "Connection could not be closed.", sqlUtils.getLastException() );
    }
  }

  
  
}

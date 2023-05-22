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
package com.gip.xyna.demon;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;
import com.gip.xyna.utils.snmp.SnmpAccessData;


/**
 * DemonProperties verwaltet die im Demon ben�tigten Properties und kann 
 * auch f�r weitere Konfigurationen verwendet werden.
 * 
 * Die Initialisierung erfolgt mit der Methode {@code readProperties}
 * entweder �ber das Lesen einer Datei oder das �bernehmen von 
 * bestehenden Properties.
 * 
 * Die Properties "start.time", "build.date" und "build.version" werden
 * automatisch angelegt.
 *
 */
public class DemonProperties {

  public static final String START_TIME = "start.time";
  public static final String BUILD_DATE = "build.date";
  public static final String BUILD_VERSION = "build.version";
  private static final String HOST = ".snmp.host";
  private static final String PORT = ".snmp.port";
  private static final String COMMUNITY = ".snmp.community";
  
  private static long startTime = System.currentTimeMillis();
  
  
  static Logger logger = Logger.getLogger(DemonProperties.class.getName());
  
  private static DemonProperties INSTANCE = null;
  public static DemonProperties getInstance() {
    return INSTANCE;
  }
  private DemonProperties(Properties properties) {
    this.properties = properties;
    properties.setProperty( START_TIME, String.valueOf(startTime) );
    readManifest();
  }
  private Properties properties;


  /**
   * Lesen der Properties aus der Datei propertyFilename
   * @param propertyFilename
   */
  public static void readProperties( String propertyFilename ) {
    Properties properties = new Properties();
    try {
      URL propUrl = new URL("file", "", propertyFilename );
      InputStream inputStream = propUrl.openStream();
      if (inputStream == null) {
        throw new RuntimeException("Failed to find resource: <" + propertyFilename + ">.");
      }
      properties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: <" + propertyFilename + ">.", e);
    }
    INSTANCE = new DemonProperties(properties);
  }

  /**
   * Wiederverwendung der �bergebenen Properties 
   * @param properties
   */
  public static void readProperties( Properties properties ) {
    Properties props = (Properties)properties.clone();
    INSTANCE = new DemonProperties(props);
  }
  
  /**
   * Bau der SnmpAccessData durch Lesen aus den Properties
   * @param props
   * @param version
   * @param name
   * @return
   */
  public static SnmpAccessData getSnmpAccessData(String version, String name) {
    Properties properties = INSTANCE.properties;
    if( ! properties.containsKey(name+HOST) ) {
      return null;
    }
    SnmpAccessData sad = 
      SnmpAccessData.newSNMP(version).
      host(properties.getProperty(name+HOST)).
      port(getIntProperty(name+PORT,161)).
      community(properties.getProperty(name+COMMUNITY)).
      build();
    return sad;
  }
  
  public static boolean existsProperty(String key) {
    return INSTANCE.properties.containsKey(key);
  }
  
  public static String getProperty(String key) {
    String property = INSTANCE.properties.getProperty(key);
    if( property == null ) {
      logger.warn("Property "+key+" not set");
    }
    return property;
  }
  
  public static String getProperty(String key, String defVal) {
    if( INSTANCE.properties.containsKey(key) ) {
      return INSTANCE.properties.getProperty(key);
    } else {
      return defVal;
    }
  }
 
  public static int getIntProperty(String key) {
    try {
      return Integer.parseInt( getProperty(key) );
    } catch( RuntimeException e ) {
      logger.warn("Property "+key+" has no integer value");
      throw e;
    }
  }
  public static int getIntProperty(String key, int defVal) {
    if( INSTANCE.properties.containsKey(key) ) {
      return getIntProperty(key);
    } else {
      return defVal;
    }
  }
  public static long getLongProperty(String key) {
    try {
      return Long.parseLong( getProperty(key) );
    } catch( RuntimeException e ) {
      logger.warn("Property "+key+" has no long value");
      throw e;
    }
  }
  public static boolean getBooleanProperty(String key) {
    if( INSTANCE.properties.containsKey(key) ) {
      return Boolean.parseBoolean( getProperty(key) );
    } else {
      logger.warn("Property "+key+" is not set");
      return false;
    }
  }
  public static boolean getBooleanProperty(String key, boolean defVal) {
    if( INSTANCE.properties.containsKey(key) ) {
      return getBooleanProperty(key);
    } else {
      return defVal;
    }
  }
 
  public static Properties getProperties() {
    return INSTANCE.properties;
  }
  
  /**
   * Reads 
   * @param name
   * @return
   */
  public static DBConnectionData getDBProperty( String name ) {
    DBConnectionData cd = DBConnectionData.
    newDBConnectionData().
    url( getProperty("db."+name+".url") ).
    user( getProperty("db."+name+".user") ).
    password( getProperty("db."+name+".password") ).
    autoCommit( getBooleanProperty( "db."+name+".autocommit", false ) ).
    clientInfo( getProperty("db."+name+".clientinfo", null ) ).
    connectTimeoutInSeconds( getIntProperty("db."+name+".connectTimeout",4) ).
    build();
    return cd;
  }

  /**
   * @param name
   * @param dbConnectionData
   * @return
   */
  public static FailoverDBConnectionData getFailoverDBProperty( String name, DBConnectionData dbConnectionData ) {
    if( existsProperty( "db."+name+".failover.url") ) {
      FailoverDBConnectionData fcd = 
        FailoverDBConnectionData.newFailoverDBConnectionData().
        dbConnectionData(dbConnectionData).
        failoverUrl( getProperty("db."+name+".failover.url") ).
        failoverSource( getProperty("db."+name+".failover.source") ).
        failoverSourceParam( getProperty("db."+name+".failover.sourceparams") ).
        build();
      return fcd;
    } else {
      FailoverDBConnectionData fcd = 
        FailoverDBConnectionData.newFailoverDBConnectionData().
        dbConnectionData(dbConnectionData).
        failoverUrl(dbConnectionData.getUrl()).
        failoverSource("none").
        build();
      return fcd;
    }
  }
  
  
  /**
   * Lesen des Manifests, um an BuildDate und Version zu gelangen
   */
  private void readManifest() {
    String version = "unknown";
    String buildDate = "unknown";
    try {
      Manifest manifest = getManifestForClass(getClass());
      version = manifest.getMainAttributes().getValue("Version");
      buildDate= manifest.getMainAttributes().getValue("Build-Date");
    } catch( RuntimeException e ) {
      //ignorieren
    }
    properties.setProperty( BUILD_VERSION, version );
    properties.setProperty( BUILD_DATE, buildDate );
  }

  private static class ManifestNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ManifestNotFoundException(String message) {
      super(message);
    }
    public ManifestNotFoundException(String message, Throwable cause) {
      super(message,cause);
    }
  }
  
  /**
   * Lesen des Manifests, welches im JarFile liegt, aus dem auch die Klasse clazz gelesen wurde
   * @param clazz
   * @return
   */
  private static Manifest getManifestForClass( Class<?> clazz ) {
    String className = clazz.getSimpleName() + ".class";
    String classPath = clazz.getResource(className).toString();
    if (!classPath.startsWith("jar")) {
      throw new ManifestNotFoundException( "Class not loaded from jar");
    }
    String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
    try {
      return new Manifest(new URL(manifestPath).openStream());
    } catch (Exception e) {
      throw new ManifestNotFoundException( "No manifest found", e );
    }
  }

  
}

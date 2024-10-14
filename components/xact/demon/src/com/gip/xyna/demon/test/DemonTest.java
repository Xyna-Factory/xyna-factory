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
package com.gip.xyna.demon.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.gip.xyna.demon.Demon;
import com.gip.xyna.demon.DemonPersistency;
import com.gip.xyna.demon.DemonSnmpConfigurator;
import com.gip.xyna.demon.DemonWorker;
import com.gip.xyna.utils.snmp.SnmpAccessData;


public class DemonTest {

  private static final String PROPERTY_FILENAME = "dt.properties";
  
  private static final String HOST = ".snmp.host";
  private static final String PORT = ".snmp.port";
  private static final String COMMUNITY = ".snmp.community";
  private static final String INDEX = ".snmp.oid.index";
  private static final String STATUS_FILENAME = ".status.filename";
  
  private static final String DEMON_NAME = "demon";

  public static class DemonWorkerTest implements DemonWorker {

    private boolean running;
    private int counter = 0;
    
    public void terminate() {
      System.err.println( "DemonWorkerTest terminate");
      running = false;
    }

    public void run() {
      running = true;
      System.err.println( "DemonWorkerTest start");
      
      while( running ) {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        ++counter;
        System.err.println( "DemonWorkerTest running");
      }
      System.err.println( "DemonWorkerTest finished");
    }



    public String getName() {
      return "DemonWorkerTest";
    }

    public void logStatus(Logger statusLogger) {
      System.out.println( "running "+running+", counter "+counter );
    }

    /**
     * @param index  
     */
    public int getInt(int index) {
     return 0;
    }

    /**
     * @param index  
     */
    public String getString(int index) {
      return null;
    }

    public int size() {
      return 0;
    }

    public String getStatus() {
      return running? "running" : "not running";
    }

    public void configureDemonSnmp(DemonSnmpConfigurator demonSnmpConfigurator) {
      //nichts zu tun
    }
    
  }
  
  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    PropertyConfigurator.configure("./log4j.properties");
    Properties props = readProperties();
    
    SnmpAccessData sadDemon = readSnmpAccessData( props, DEMON_NAME );
    
    System.out.println( sadDemon );
    String index = props.getProperty( DEMON_NAME+INDEX );

    DemonPersistency.createInstance(props.getProperty( DEMON_NAME+STATUS_FILENAME ) );
    
    DemonWorkerTest dwt = new DemonWorkerTest();
    Demon d = new Demon( DEMON_NAME, dwt, sadDemon, index );
      
    d.startDemon();
  }
  
  private static SnmpAccessData readSnmpAccessData(Properties props, String name) {
    SnmpAccessData sad = 
      SnmpAccessData.newSNMPv1().
      host(props.getProperty(name+HOST)).
      port(props.getProperty(name+PORT)).
      community(props.getProperty(name+COMMUNITY)).
      build();
    return sad;
  }

  private static Properties readProperties() {
    Properties properties = new Properties();
    try {
      URL propUrl = new URL("file", "", PROPERTY_FILENAME );
      InputStream inputStream = propUrl.openStream();
      if (inputStream == null) {
        throw new RuntimeException("Failed to find resource: <" + PROPERTY_FILENAME + ">.");
      }
      properties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: <" + PROPERTY_FILENAME + ">.", e);
    }
    return properties;
  }

}

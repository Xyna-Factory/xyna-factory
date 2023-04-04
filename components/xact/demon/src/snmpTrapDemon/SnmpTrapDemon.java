/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpTrapDemon;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.gip.xyna.demon.Demon;
import com.gip.xyna.demon.DemonProperties;


/**
 *
 */
public class SnmpTrapDemon {

  static Logger logger = Logger.getLogger(SnmpTrapDemon.class.getName());

  private static final String PROPERTY_FILENAME = "snmpTrapDemon.properties";
   
  private static final String DEMON_NAME = "demon";
    
  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) {
    String pathToProperties = (args.length > 0 ? args[0] : "./config" )+"/";
    
    PropertyConfigurator.configure(pathToProperties+PROPERTY_FILENAME);
    DemonProperties.readProperties(pathToProperties+PROPERTY_FILENAME);
    
    Demon demon = Demon.createDemon( DEMON_NAME );  
    demon.startDemon();  
    logger.info( "Demon started");
    
    try {
      logger.debug( "Initializing SnmpTrapDemonWorker");
      SnmpTrapDemonWorker snmpTrapDemonWorker = new SnmpTrapDemonWorker();
      
      logger.debug( "Starting SnmpTrapDemonWorker");
      demon.setDemonWorker( snmpTrapDemonWorker );
      demon.startDemonWorker();
      
    } catch( Exception e ) {
      logger.error("Exception while initializing SnmpTrapDemon",e);
      logger.error("Demon will be stopped now");
      demon.stopDemon();
    }
  }
  
}

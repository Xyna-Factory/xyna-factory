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
package dhcpAdapterDemon;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.gip.xyna.demon.Demon;
import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.DemonWorker;

public class DhcpAdapterDemon {
  private static final String PROPERTY_FILENAME = "dhcpAdapterDemon.properties";
  private static final String LOGFILE_DEFAULT = PROPERTY_FILENAME;
  private static final String DEMON_PREFIX = "demon";
  
  static Logger logger = Logger.getLogger(DhcpAdapterDemon.class.getName());
  
  private DemonWorker demonWorker;
  
  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) {
    String pathToProperties = (args.length > 0 ? args[0] : "./config" )+"/";
    
    PropertyConfigurator.configure(pathToProperties+LOGFILE_DEFAULT);
    DemonProperties.readProperties(pathToProperties+PROPERTY_FILENAME);
   
    logger.debug( "Initializing Demon");
    Demon demon = Demon.createDemon(DEMON_PREFIX);  
    demon.startDemon();  
    logger.info( "Demon started");

    try {
      
      DhcpAdapterDemon dad = new DhcpAdapterDemon();
      dad.startDemonWorker( demon );
      
    } catch( Exception e ) {
      logger.error("Exception while initializing DhcpAdapterDemon",e);
      logger.error("Demon will be stopped now");
      demon.stopDemon();
    }
    
  }
    
  public DhcpAdapterDemon() throws Exception {
    demonWorker = new DhcpDemonWorker(DEMON_PREFIX);
  }

  private void startDemonWorker(Demon demon) {
    logger.debug( "Starting demonWorker");
    demon.setDemonWorker( demonWorker );
    demon.startDemonWorker();
  }
  
}

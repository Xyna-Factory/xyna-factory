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

package com.gip.xyna.utils.logging;



import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Die Klasse BashLogger ist dazu gedacht, dass bash Skripte die gleiche Log4j Konfiguration verwenden koennen, wie die Factory selbst.
 * 
 */
public class BashLogger {
  
  
  /**
   * The main method.
   *
   * @param args the arguments
   * 
   * Ueblicherweise wird die Klasse folgendermassen aufgerufen:
   * java -classpath $CLASSPATH com.gip.BashLogger $FACILITY $SEVERITY
   * Um das "Facility" Feature zu nutzen,  muss in der log4j2.xml Datei ein Logger definiert sein
   * Beispiel
   * <logger name="local0" level="TRACE">
   *    <AppenderRef ref="RollingFile"/>
   * </logger>
   * 
   */
  public static void main(String[] args) {
    
    Logger logger = LogManager.getLogger(BashLogger.class);
    if (args.length >= 1 && args[0].length()>0) {
      logger = LogManager.getLogger(args[0]);
    }
    
    
    Level level = Level.DEBUG;
    if (args.length == 2 && args[1].length() > 0) {
      try {
        level = Level.forName(args[1].toUpperCase(), 0);
      }
      catch (Exception e) {
        // just print, nothing else
        e.printStackTrace();
      }
    }
    
    InputStreamReader isReader = new InputStreamReader(System.in);
    BufferedReader bufReader = new BufferedReader(isReader);
    while(true){
        try {
            String inputStr = null;
            if((inputStr=bufReader.readLine()) != null) {
                logger.log(level, inputStr);
            }
            else {
                break;
            }
        }
        catch (Exception e) {
            logger.error("Logging Error",e);
        }
    }
  }
}

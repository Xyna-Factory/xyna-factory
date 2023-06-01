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
package com.gip.xyna.demon.test;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.gip.xyna.demon.DemonWorker;
import com.gip.xyna.demon.DemonWorkerFactory;
import com.gip.xyna.demon.worker.Master;
import com.gip.xyna.demon.worker.SlaveInitializer;
import com.gip.xyna.demon.worker.SlavePool;
import com.gip.xyna.demon.worker.SlaveWork;


public class DemonWorkerTest {
  private static final String LOGFILE_DEFAULT = "log4j.properties";
  static Logger logger = Logger.getLogger(DemonWorkerTest.class.getName());
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    PropertyConfigurator.configure(LOGFILE_DEFAULT);

    
    SlaveInitializer<Connection> sic = new SlaveInitializer<Connection>() {
      
      public Connection create(int number) {
        return new Connection(number);
      }
      public void destroy(Connection con, int number) {
        con.close(number);
      }
      public String getThreadNamePrefix() {
        return "tester";
      }
      public void initialize() {/*nicht verwendet*/}
      public void terminate() {/*nicht verwendet*/}
      public void logStatus(Logger statusLogger) {/*nicht verwendet*/}
    };
    
    Master<Connection,Integer> master = new Master<Connection,Integer>(){

      private SlavePool<Connection,Integer> slavePool;

      public void run() {
        for( int i=0; i< 100; ++i ) {
          final int i2 = i;
          System.out.println(i);
          slavePool.execute( new SlaveWork<Connection,Integer>() {
            public boolean work(Connection connection) {
              
              try {
                Thread.sleep(30);
              } catch (InterruptedException e) {
                logger.error(e);
              }
              
              System.err.println( "Work "+i2+" von Slave "+connection+" erledigt" );
              return true;
            }} );
          
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            logger.error(e);
          }
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          logger.error(e);
        }
      }

      public void setSlavePool(SlavePool<Connection,Integer> slavePool) {
        this.slavePool=slavePool;
      }

      public void terminate() {
        //nicht implementiert
      }

      public void logStatus(Logger statusLogger) {
        //nicht implementiert
      }

      };
    
    DemonWorker dw = DemonWorkerFactory.createDemonWorker( "num", master, sic ); 
    new Thread(dw).start();
    
    
  }

  
  
  public static class Connection {

    private int number;
    public Connection(int number) {
      this.number = number;
      System.err.println( "create Connection" +number );
    }

    public void close(@SuppressWarnings("hiding") int number) {
      System.err.println( "close Connection" +number );
    }
    @Override
    public String toString() {
      return ""+number;
    }
  }
  
}

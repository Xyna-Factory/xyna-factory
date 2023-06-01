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

package common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.snmp.OID;



public class Failover {
  
  final static private int PRIMARY='1';
  final static private int FAILOVER='2';
  private static Logger logger = Logger.getLogger(Failover.class.getName());
  private String source;
  private  int failoverstate=PRIMARY;
    
  
  @SuppressWarnings("unused")
  private Failover(){/*private Constructor. Do not use.*/}

  public Failover(String failoverSource) {
    this.source=failoverSource;
  }

  /* Soll ein Connect zur Primären Instanz (Rückgabewert==false) oder zur
   * Failover-Instanz (Rückgabewert=true) durchgeführt werden.
   */
  public boolean isFailover() {
    return failoverstate==FAILOVER;
  }
  
  private int readState()  {
    int ret=0;
    if(source.equals("none") ) {
      return PRIMARY;
    } else if(source.indexOf("/")==0) {
        ret='1';
        BufferedReader br=null;
        try{
          br=new BufferedReader(new FileReader(source));
          ret=br.read();          
        } catch (IOException e){
          logger.error(e);
        }finally {
          try{if (br!=null) br.close();}catch(Exception e){logger.error("ignored exception",e);}
        }
    } else {
        ret='0'+((Integer)FailoverOidSingleHandler.getInstance().get(new OID(source),0).getValue()).intValue();//numersiches add!!
    }
    return ret;
  }

  /* Diese Funktion sollte an einer strategisch günstigen Stelle eingabut werden. Its der Rückgabewert true,
   * sollte ein Reconnect  durchgeführt werden . IsFailover gibt dann zurück, zu welcher Instanz das Connect
   * durchgeführt werden soll. */
  public synchronized boolean signalFailover() {
    boolean ret=false;
    try{
      int newfailoverdppstate=readState();
      if (newfailoverdppstate!=failoverstate){
        logger.error( "Failover: "+(char)failoverstate+"->"+(char)newfailoverdppstate);
        failoverstate=newfailoverdppstate;
        ret=true;
      }
    }catch(Exception e){
      logger.error(e);
    }
    return ret;
  }

}

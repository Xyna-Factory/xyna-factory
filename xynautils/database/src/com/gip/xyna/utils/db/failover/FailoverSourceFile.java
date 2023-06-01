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
package com.gip.xyna.utils.db.failover;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * FailoverSourceFile: failover-status is read from File
 */
public class FailoverSourceFile implements FailoverSource {
  final static private int PRIMARY='1';
  final static private int FAILOVER='2';
  
  static Logger logger = Logger.getLogger(FailoverSourceFile.class.getName());
  
  protected File source;
  protected int lastState;
  
  public FailoverSourceFile() {
    source = null;
    this.lastState = PRIMARY;
  }
 
  public FailoverSourceFile( File source ) {
    this.source = source;
    this.lastState = PRIMARY;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.db.failover.FailoverSource#isFailover()
   */
  public boolean isFailover() {
    readFile();
    return lastState == FAILOVER;
  }

  protected void readFile() {
    if( source == null ) {
      logger.error( "File is not configured" ); 
      return;
    }
    BufferedReader br=null;
    try{
      br=new BufferedReader(new FileReader(source));
      int newState = br.read();
      if( newState == PRIMARY || newState == FAILOVER ) {
        lastState = newState;
      } else {
        logger.error( "Invalid state in File "+source+" '"+newState+"', expected is '"+PRIMARY+"' or '"+FAILOVER+"'" );
        //failover-status does not change
      }
    } catch (IOException e){
      logger.error(e);
      //failover-status does not change
    }finally {
      try{
        if (br!=null) br.close();
      } catch(Exception e){
        logger.error("ignored exception",e);
      }
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.db.failover.FailoverSource#newInstance(java.lang.String)
   */
  public FailoverSource newInstance(String failoverParam) {
    return new FailoverSourceFile( new File(failoverParam) );
  }
  
}

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

package com.gip.xyna.xsor.protocol;

import org.apache.log4j.Logger;


public class XSORMemoryChecker implements Runnable {
  
  private static final Logger logger=Logger.getLogger(XSORMemoryChecker.class.getName());
  final XSORMemory xsorMemory;
  boolean stopped;
  
  public XSORMemoryChecker(XSORMemory xsorMemory){
    this.xsorMemory=xsorMemory;
    stopped=false;
  }
  
  
  public void run() {
    while (!stopped){
      if (logger.isTraceEnabled()){
        logger.trace("XSORMemoryChecker "+Thread.currentThread().getId()+ " started");
        for(int i=0;i<xsorMemory.size;i++){
          if(stopped) break;
          if (xsorMemory.lock.get(i)>=0){
            try{            
              long begin=System.currentTimeMillis();    
              xsorMemory.lockObjectForceUnlock(i);
              if(System.currentTimeMillis()-begin>50000){
                String trace="handling Object"+i+"\n";
                trace+=xsorMemory.toString(i, false);          
                logger.trace(trace);
              }
            } catch (Throwable t){
              logger.trace("got Exception=",t);
            } finally {
              xsorMemory.unlockObject(i);
            }
          }
        }
        logger.trace("XSORMemoryChecker "+Thread.currentThread().getId()+ " end");
        try{
          Thread.sleep(10000);
        } catch (Throwable t){
          //ignore
        }
      } else {
        try{
          Thread.sleep(1000);
        } catch (Throwable t){
          //ignore all Errors
        }
      }
    }    
  }

  public void stop() {
    stopped=true;    
  }
  

}

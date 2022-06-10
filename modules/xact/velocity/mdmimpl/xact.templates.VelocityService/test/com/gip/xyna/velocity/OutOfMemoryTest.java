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
package com.gip.xyna.velocity;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.Before;


public class OutOfMemoryTest  {
  
  private static Logger logger = Logger.getLogger(OutOfMemoryTest.class);

  private static VelocityEngine velocityEngine;
  
  @Before
  public static void setUp() throws Exception {
    velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
            "org.apache.velocity.runtime.log.Log4JLogChute");
    velocityEngine.setProperty("runtime.log.logsystem.log4j.logger", logger.getName());
    velocityEngine.setProperty(VelocityEngine.VM_PERM_INLINE_LOCAL, true);
    velocityEngine.setProperty(VelocityEngine.PARSER_POOL_SIZE, 20);
    velocityEngine.init();
  }
  
  private static List<String> tokens = new ArrayList<String>();
  private static int highestToken = -1;
  
  private static synchronized String getVelocityToken() {
    if (tokens.size() == 0) {
      StringBuffer sb = new StringBuffer("Velocity<");
      sb.append(highestToken++).append(">");
      return sb.toString(); 
    } else {
      return tokens.remove(tokens.size()-1);
    }
  }
  
  private static synchronized void freeVelocityToken(String token) {
    tokens.add(token);
  }
  
  private static String evaluate(String template, VelocityContext context) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
    String token = getVelocityToken();
    try {
      StringWriter writer = new StringWriter();
      //bugz 8908: logtag muss für jeden thread unterschiedlich sein, ansonsten funktioniert der test nicht
      velocityEngine.evaluate(context, writer, token, new StringReader(template));
      return writer.toString();
    } finally {
      freeVelocityToken(token);
    }
  }
  
  public static void main(String[] args) throws Exception {

    setUp();
    Map map = new HashMap();
    map.put("muh", "kuh");
    final VelocityContext ctx = new VelocityContext(map);
    final Random rand = new Random();
    Runnable r = new Runnable() {

      public void run() {
     /*   StringBuffer randomString = new StringBuffer();
        for (int i = 0; i<100; i++) {
          randomString.append(rand.nextDouble());
        }*/
        try {
          String s = evaluate("bla ${muh} #macro( mymacro ) macrooutput " + rand.nextDouble() + " #end\n" +
    "testtemplate " +
    "#mymacro()\n wuh", ctx);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    ThreadPoolExecutor tpe =
        new ThreadPoolExecutor(1, 20, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>() );
    for (int i = 0; i < 200000; i++) {
      while (true) {
        try {
          if (rand.nextInt(100) < 1) {
            System.out.println("waiting for threads");
            while (tpe.getPoolSize() > 1) {
              
            }
          }
          tpe.execute(r);
          break;
        } catch (RejectedExecutionException e) {
          
        }
      }
      System.out.println(i + ". " +     tpe.getActiveCount());
    }
    System.out.println("highest token " + highestToken);
    while (true) {
      System.gc();
      Thread.sleep(10000);
    }
  }
  

}

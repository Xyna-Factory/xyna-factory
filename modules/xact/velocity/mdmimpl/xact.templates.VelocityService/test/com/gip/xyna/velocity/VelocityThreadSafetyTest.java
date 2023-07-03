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
package com.gip.xyna.velocity;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.Before;
import org.junit.Test;


public class VelocityThreadSafetyTest extends TestCase {
  
  private static Logger logger = Logger.getLogger(VelocityThreadSafetyTest.class);

  private VelocityEngine velocityEngine;
  
  @Before
  public void setUp() throws Exception {
    this.velocityEngine = new VelocityEngine();
    // VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS has been removed in velocity-engine-core 2.0 but this is the value that was stored in there
    velocityEngine.setProperty("runtime.log.logsystem.class",
            "org.apache.velocity.runtime.log.Log4JLogChute");
    velocityEngine.setProperty("runtime.log.logsystem.log4j.logger", logger.getName());
    velocityEngine.setProperty(VelocityEngine.VM_PERM_INLINE_LOCAL, true);
    velocityEngine.setProperty(VelocityEngine.PARSER_POOL_SIZE, 20);
    //velocityEngine.setProperty(VelocityEngine.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, true);
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
  
  private String evaluate(String template, VelocityContext context) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
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
  
  @Test
  public void testThreadSafety() throws Exception {
    final String template = "#macro( mymacro ) macrooutput #end\n" +
    "testtemplate " +
    "#mymacro()";
    final VelocityContext velocityContext = new VelocityContext();
    final List<String> errors = new ArrayList<String>();
    final CyclicBarrier cb = new CyclicBarrier(3);

    List<Thread> l = new ArrayList<Thread>();
    for (int i = 0; i<2; i++) {
      Thread t = new Thread() {
        public void run() {
          try {
            for (int i = 0; i<1000; i++) {
              String evaluated = evaluate(template, velocityContext);
              logger.trace(evaluated);
              if (evaluated.contains("#")) {
                logger.error(evaluated);
                synchronized (errors) {
                  errors.add(evaluated);
                }
              }
            }
          } catch (Exception e) {
            logger.error("", e);
            synchronized (errors) {
              errors.add("exception: " + e.getMessage());
            }
          } finally {
            try {
              cb.await();
            } catch (Exception e) {
              logger.error("", e);
              synchronized (errors) {
                errors.add("exception: " + e.getMessage());
              }
            }
          }
        }
      };
      l.add(t);
    }
    for (int i= 0; i<l.size(); i++) {
      l.get(i).start();
    }
    //wait for all threads, before concluding test result
    cb.await(100, TimeUnit.SECONDS);
    assertEquals("generated text contained #", 0, errors.size());
  }
  
  
  public static class MyBean {
    private String v;
    public String getV() {
      return v;
    }
  }
  public void testBla() throws MethodInvocationException, ResourceNotFoundException, IOException  {
    HashMap<String, Object> map = new HashMap<String, Object>();
    MyBean b = new MyBean();
    b.v = "gad";
    map.put("bla", b);
    try {
      System.out.println(evaluate("bla\n${bla.v}", new VelocityContext(map
      )));
    } catch (ParseErrorException e) {
      System.out.println(e.getTemplateName());
      System.out.println(e.getLineNumber());
      e.printStackTrace();
    }
    
  }
  
}

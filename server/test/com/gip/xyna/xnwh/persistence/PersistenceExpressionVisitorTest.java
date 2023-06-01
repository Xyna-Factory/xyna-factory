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

package com.gip.xyna.xnwh.persistence;



import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;



public class PersistenceExpressionVisitorTest extends TestCase {

  private Object pev;
  private Method methodUnderTest;
  private Field fieldUnderTest;

  @Override
  protected void setUp() throws Exception {
    Class<?> c = Class.forName("com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors$QueryBuildingVisitor");
    Constructor<?>[] constructors = c.getDeclaredConstructors();
    constructors[0].setAccessible(true);
    pev = constructors[0].newInstance(null, new Parameter());
    methodUnderTest = c.getDeclaredMethod("applyGlobEscapes", String.class);
    methodUnderTest.setAccessible(true);
    fieldUnderTest = c.getDeclaredField("parameter");
    fieldUnderTest.setAccessible(true);
    super.setUp();
  }


  @Override
  protected void tearDown() throws Exception {
    pev = null;
    super.tearDown();
  }
  

  public void testUnescapedStar() {
    execute("*", "%");
    execute("\\\\*", "\\\\\\\\%");
    execute("\\\\\\\\*", "\\\\\\\\\\\\\\\\%");
  }


  public void testEscapedStar() {
    execute("\\*", "*");
    execute("\\\\\\*", "\\\\\\\\*");
    execute("\\\\\\\\\\*", "\\\\\\\\\\\\\\\\*");
  }


  public void testUnescapedPercentSign() {
    execute("%", "\\%");
    execute("\\\\%", "\\\\\\\\\\%");
    execute("\\\\\\\\%", "\\\\\\\\\\\\\\\\\\%");

  }


  public void testEscapedPercentSign() {
    execute("\\%", "\\%");
    execute("\\\\\\%", "\\\\\\\\\\%");
    execute("\\\\\\\\\\%", "\\\\\\\\\\\\\\\\\\%");
  }


  private void execute(String input, String expectedResult) {
    String result = null;
    try {
      result = (String) methodUnderTest.invoke(pev, input);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      e.printStackTrace();
    }

    assertEquals(expectedResult, result);
  }
}

/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl;



import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;



public class ConstraintTest {
  
  private enum ExpectedResult {
    NULL, EXCEPTION, EMPTY_LIST
  }

  private void assertPathReturnsNull(String path, ExpectedResult expresult) {
    String json = "{\"a\":3, \"b\":{}, \"l\":[{\"x\":1}]}";
    List<String> paths = new ArrayList<>();
    paths.add(path);
    String expression = "EVAL($0)";
    TMFExpressionParser parser = ParserCache.getParser(-1L);
    TMFExpressionContext ctx = new TMFExpressionContext(json, paths, null);
    SyntaxTreeNode n = parser.parse(expression, 0, true);
    n.validate();
    try {
      Object result = n.eval(ctx);
      if (expresult == ExpectedResult.NULL) {
        assertNull(result);
      } else if (expresult == ExpectedResult.EMPTY_LIST) {
        if (!(result instanceof List)) {
          fail("result was expected to be a list. was " + result.getClass());
        }
      }
    } catch (RuntimeException e) {
      if (expresult != ExpectedResult.EXCEPTION) {
        throw e;
      }
      return;
    }
  }

  @Test
  public void testNonExistingPathReturnsNull() {
    assertPathReturnsNull("$.l[?(@.b==1)].x", ExpectedResult.EMPTY_LIST);
    assertPathReturnsNull("$.a[?(@.b==1)].x", ExpectedResult.EXCEPTION);
    assertPathReturnsNull("$.b[?(@.b==1)].x", ExpectedResult.EXCEPTION);
    assertPathReturnsNull("$.a[0].b", ExpectedResult.EXCEPTION);
    assertPathReturnsNull("$.a.b", ExpectedResult.EXCEPTION);
    assertPathReturnsNull("$.b.c", ExpectedResult.NULL);
    assertPathReturnsNull("$.c.d", ExpectedResult.NULL);
    assertPathReturnsNull("$.l[1].x", ExpectedResult.NULL);
    assertPathReturnsNull("$.l[0].y", ExpectedResult.NULL);
    assertPathReturnsNull("$.k[0].x", ExpectedResult.NULL);
  }
  
}

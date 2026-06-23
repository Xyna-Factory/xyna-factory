/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.http.impl;



import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;

import base.File;
import xact.http.Header;
import xact.http.HeaderField;
import xact.http.SendParameter;
import xact.http.URLPath;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.statuscode.HTTPStatusCode;
import xact.http.enums.statuscode.OK;
import xact.http.impl.HTTPResponseResolverServiceOperationImpl.Condition;
import xact.templates.Document;



class HTTPResponseResolverServiceOperationImplTest {


  @Test
  void testJSONParseWrite() throws InvalidJSONException, UnexpectedJSONContentException {
    String j1 = """
        [
          1,
          "2"
        ]
        """;
    String j2 = """
        [
        ]
        """;
    String j3 = """
        123
        """;
    String j4 = """
        {
          "k1" : {
            "k1.x" : [
            ],
            "k1.y" : 4
          },
          "k2" : "value",
          "k3" : {
            "k3.x" : "x"
          }
        }
        """;
    for (String json : new String[] {j1, j2, j3, j4}) {
      if (json.endsWith("\n")) {
        json = json.substring(0, json.length() - 1);
      }
      String rerendered = new HTTPResponseResolverServiceOperationImpl.JSONFileContent(json).val.toString();
      assertEquals(json, rerendered);
    }

  }


  @Test
  void testExceptions() {
    String[] j1 = {"""
        """, "JSON string invalid at position 1. Cause: Unexpected end of JSON: JSON empty"};
    String[] j2 = {"""
        [ "key : ":123"]
        """, "JSON string invalid at position 12. Cause: Text could not be parsed as a number."};
    String[] j3 = {"""
        [ "key":  123,, "key2": 124 ]
        """, "JSON string invalid at position 8. Cause: Expected comma or array end"};
    String[] j4 = {"""
        { "key":  123,, "key2": 124 }
        """, "JSON string invalid at position 15. Cause: Missing string"};
    String[] j5 = {"""
        "key":  123
        """, "JSON string invalid at position 6. Cause: JSON continues after parser finished"};
    String[] j6 = {"""
         { "a":1.3e-133513, "a":true}
        """, "JSON string invalid at position 22. Cause: Duplicate key in object"};
    String[] j7 = {"""
         { "a":true, "b\\n":{}, "c\\r":{}, "\\"a":[1,"2",null,4}
        """, "JSON string invalid at position 53. Cause: Expected comma or array end"};
    String[] j8 = {"""
         { "a\n\r\"":null }
        """, "JSON string invalid at position 9. Cause: String does not terminate"};
    String[] j9 = {"""
         { "a\n\r":TRUE, [] }
        """, "JSON string invalid at position 10. Cause: Unexpected character"};
    for (String[] jsonAndExc : new String[][] {j1, j2, j3, j4, j5, j6, j7, j8, j9}) {
      String json = jsonAndExc[0];
      if (json.endsWith("\n")) {
        json = json.substring(0, json.length() - 1);
      }
      try {
        parse(json);
        fail("expected parsing failure");
      } catch (xact.http.impl.InvalidJSONException e) {
        assertEquals(jsonAndExc[1], e.getMessage());
      }
    }
  }


  @Test
  void testCondition() {
    String json = """
        [
        { "source": "header", "op": "equals", "key": "myheader", "value": "12345678" },
         { "source": "url", "op": "regex", "value": ".*blubb" }
        ]
        """;
    Condition[] c = Condition.parseConditions(parse(json));
    assertEquals(2, c.length);
    assertEquals("header", c[0].source);
    assertEquals("equals", c[0].op);
    assertEquals("myheader", c[0].key);
    assertEquals("12345678", c[0].value.stringVal);
    assertEquals("url", c[1].source);
    assertEquals("regex", c[1].op);
    assertEquals(null, c[1].key);
    assertEquals(".*blubb", c[1].value.stringVal);

    List<HeaderField> headers = new ArrayList<>();
    headers.add(new HeaderField("bla", "123"));
    headers.add(new HeaderField("myheader", "12345678"));
    SendParameter sp1 = new SendParameter(new GET(), new URLPath("pathbla/blubb", null, ""), new Header(null, headers), null, null);
    assertTrue(c[0].match(new Document(), sp1));
    assertTrue(c[1].match(new Document(), sp1));

    List<HeaderField> headers2 = new ArrayList<>();
    headers2.add(new HeaderField("bla", "123"));
    headers2.add(new HeaderField("myheader", "12345679"));
    SendParameter sp2 = new SendParameter(new GET(), new URLPath("path", null, ""), new Header(null, headers2), null, null);
    assertTrue(!c[0].match(new Document(), sp2));
    assertTrue(!c[1].match(new Document(), sp2));

    List<HeaderField> headers3 = new ArrayList<>();
    headers3.add(new HeaderField("bla", "123"));
    headers3.add(new HeaderField("myheader2", "12345678"));
    SendParameter sp3 = new SendParameter(new GET(), new URLPath("path/blubb/x", null, ""), new Header(null, headers3), null, null);
    assertTrue(!c[0].match(new Document(), sp3));
    assertTrue(!c[1].match(new Document(), sp3));


  }


  private static String fileContent = """
            {
        "fileMatch": [

             { "source": "header", "op": "equals", "key": "myheader", "value": "12345677" }
        ],
        "responses": [
          {
            "match": [

                  { "source": "method", "op": "equals", "value": "POST" },
                  { "source": "url", "op": "regex", "value": ".*blubb.*" }
             ],
             "response": [7, "hello"]
          }
        ]
      }
      """;


  @Test
  void testRoundtrip() throws Ex_FileAccessException {
    List<HeaderField> headers = new ArrayList<>();
    headers.add(new HeaderField("bla", "123"));
    headers.add(new HeaderField("myheader", "12345678"));
    SendParameter sp1 = new SendParameter(new POST(), new URLPath("pathbla/blubb", null, ""), new Header(null, headers), null, null);
    HTTPResponseResolverServiceOperationImpl impl = new HTTPResponseResolverServiceOperationImpl();

    java.io.File f = new java.io.File("test");
    String p = f.getAbsolutePath();
    System.out.println(p);
    Container c = impl.resolveResponse(null, sp1, new File(p));
    Document outputDoc = (Document) (c.get(0));
    HTTPStatusCode code = (HTTPStatusCode) (c.get(2));
    String expectedJsonResponse = """
          { "key" : "value",
          "other_key": 128
        }
        """;
    assertEquals(parse(expectedJsonResponse).toString(), outputDoc.getText());
    assertTrue(code instanceof OK);

    java.io.File simfile3 = new java.io.File("test/simfile3");
    try {
      FileUtils.saveToFile(new ByteArrayInputStream(fileContent.getBytes()), simfile3);

      //caching?
      List<HeaderField> headers2 = new ArrayList<>();
      headers2.add(new HeaderField("bla", "123"));
      headers2.add(new HeaderField("myheader", "12345677"));
      SendParameter sp2 = new SendParameter(new POST(), new URLPath("pathbla/blubb", null, ""), new Header(null, headers2), null, null);
      c = impl.resolveResponse(null, sp2, new File(p));
      outputDoc = (Document) (c.get(0));
      code = (HTTPStatusCode) (c.get(2));
      expectedJsonResponse = """
          [7, "hello"]
          """;
      assertEquals(parse(expectedJsonResponse).toString(), outputDoc.getText());
      assertTrue(code instanceof OK);
    } finally {
      FileUtils.deleteFileWithRetries(simfile3);
    }

  }


  private JSONValue parse(String json) {
    return new JSONParser(json).parse(new JSONTokenizer().tokenize(json));
  }

}

/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xfmg.xfctrl.datamode.json.impl;

import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import xfmg.xfctrl.datamodel.json.impl.InvalidJSONException;
import xfmg.xfctrl.datamodel.json.impl.JSONParser;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObjectWriter;
import xfmg.xfctrl.datamodel.json.JSONDatamodelServicesImpl;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;
import junit.framework.TestCase;
import xact.templates.Document;


public class JSONTest extends TestCase {

  public void testExample1() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"Event\":\"Initialize\",\"Payload\":{\"panelID\":\"pagetopper\"}}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"Event\" : \"Initialize\",\n" + 
        "  \"Payload\" : {\n" + 
        "      \"panelID\" : \"pagetopper\"\n" + 
        "    }\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  public void testExample2() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"user\":{\"userID\":\"1\"},\"contactGroups\":[\"TestGroup\"]}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"contactGroups\" : [\n" + 
        "    \"TestGroup\"\n" + 
        "  ],\n" + 
        "  \"user\" : {\n" + 
        "      \"userID\" : \"1\"\n" + 
        "    }\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  public void testExample3() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"actionID\":\"s_573615\",\"payload\":{\"user\":{\"userID\":\"6\"},\"contactGroups\":[\"TestGroup\"]}}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"actionID\" : \"s_573615\",\n" + 
        "  \"payload\" : {\n" + 
        "      \"contactGroups\" : [\n" + 
        "        \"TestGroup\"\n" + 
        "      ],\n" + 
        "      \"user\" : {\n" + 
        "          \"userID\" : \"6\"\n" + 
        "        }\n" + 
        "    }\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  public void testExample4() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"actionID\":\"s_573615\",\"payload\":{\"user\":{\"userID\":\"6\"},\"contactGroups\":[\"TestGroup\"],\"note\":\"Bla\"}}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"actionID\" : \"s_573615\",\n" + 
        "  \"payload\" : {\n" + 
        "      \"contactGroups\" : [\n" + 
        "        \"TestGroup\"\n" + 
        "      ],\n" + 
        "      \"note\" : \"Bla\",\n" + 
        "      \"user\" : {\n" + 
        "          \"userID\" : \"6\"\n" + 
        "        }\n" + 
        "    }\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  public void testExample5() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[\"TestGroup\"],\"b\":\"Bla\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [\n" + 
        "    \"TestGroup\"\n" + 
        "  ],\n" + 
        "  \"b\" : \"Bla\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  
  public void testExample6() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[],\"b\":\"Bla\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [],\n" + 
        "  \"b\" : \"Bla\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  

  public void testExample7() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"type\":\"candidate\",\"candidate\":\"a=candidate:802777727 2 tcp 1509957375 10.0.13.94 0 typ host generation 0\\\\r\\\\n\",\"sdpMLineIndex\":\"1\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"candidate\" : \"a=candidate:802777727 2 tcp 1509957375 10.0.13.94 0 typ host generation 0\\\\r\\\\n\",\n" + 
        "  \"sdpMLineIndex\" : \"1\",\n" + 
        "  \"type\" : \"candidate\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  

  public void testExample8() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString =  "{\"type\":\"candidate\",\"candidate\":\"a=candidate:802777727 2 tcp 1509957375 10.0.13.94 0 typ host generation 0\\r\\n\",\"sdpMLineIndex\":\"1\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"candidate\" : \"a=candidate:802777727 2 tcp 1509957375 10.0.13.94 0 typ host generation 0\\r\\n\",\n" + 
        "  \"sdpMLineIndex\" : \"1\",\n" + 
        "  \"type\" : \"candidate\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample9() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"type\":\"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"type\" : \"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  

  public void testExample10() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"type\":\n\"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"type\" : \"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample11() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"type\":\"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"type\" : \"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample12() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"type\":\n\"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"type\" : \"Bla\\r\\nBlup\\\"Hallo\\\" We\\\\lt. Ende gut, \\\\\\\"Alles gut\\\\\\\"\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample13() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[1,2,3],\"b\":\"Bla\",\"c\":{\"a\":[1,2,3],\"b\":\"Bla\"}}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [\n" + 
        "    1,\n" + 
        "    2,\n" + 
        "    3\n" + 
        "  ],\n" + 
        "  \"b\" : \"Bla\",\n" + 
        "  \"c\" : {\n" + 
        "      \"a\" : [\n" + 
        "        1,\n" + 
        "        2,\n" + 
        "        3\n" + 
        "      ],\n" + 
        "      \"b\" : \"Bla\"\n" + 
        "    }\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample14() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[123,21233312123,3,123314.43243,true,23423423,\"abc\",-3333],\"b\":\"Bla\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [\n" + 
        "    123,\n" + 
        "    21233312123,\n" + 
        "    3,\n" + 
        "    123314.43243,\n" + 
        "    true,\n" + 
        "    23423423,\n" + 
        "    \"abc\",\n" + 
        "    -3333\n" + 
        "  ],\n" + 
        "  \"b\" : \"Bla\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample15() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[123,1.0E-10,3,123314.43243,true,23423423,\"abc\",-3333,\"a\\f\"],\"b\":\"Bla\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [\n" + 
        "    123,\n" + 
        "    1.0E-10,\n" + 
        "    3,\n" + 
        "    123314.43243,\n" + 
        "    true,\n" + 
        "    23423423,\n" + 
        "    \"abc\",\n" + 
        "    -3333,\n" + 
        "    \"a\\f\"\n" + 
        "  ],\n" + 
        "  \"b\" : \"Bla\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample16() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[    123,21233312123 ,  3,   123314.43243,true,23423423,\"abc\",-3333],\"b\":\"Bla\"\n}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [\n" + 
        "    123,\n" + 
        "    21233312123,\n" + 
        "    3,\n" + 
        "    123314.43243,\n" + 
        "    true,\n" + 
        "    23423423,\n" + 
        "    \"abc\",\n" + 
        "    -3333\n" + 
        "  ],\n" + 
        "  \"b\" : \"Bla\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  public void testExample17() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[123,21233312123,3,123314.43243,true,23423423,\"abc\",-3333],\"b\":\"Bla\"}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"a\" : [\n" + 
        "    123,\n" + 
        "    21233312123,\n" + 
        "    3,\n" + 
        "    123314.43243,\n" + 
        "    true,\n" + 
        "    23423423,\n" + 
        "    \"abc\",\n" + 
        "    -3333\n" + 
        "  ],\n" + 
        "  \"b\" : \"Bla\"\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }

  public void testExample18() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"asd\\u20ach@\":3e-12   , \"basdasd\":[\"\\\"\\\\a,]}[{s\",  \"a2\", { \"x\":  null,\"b\":111111111111111111111111111111e33333333}] }";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);
    assertEquals("{\n" + 
        "  \"asd€h@\" : 3e-12,\n" + 
        "  \"basdasd\" : [\n" + 
        "    \"\\\"\\\\a,]}[{s\",\n" + 
        "    \"a2\",\n" + 
        "    {\n" + 
        "        \"b\" : 111111111111111111111111111111e33333333,\n" + 
        "        \"x\" : null\n" + 
        "      }\n" + 
        "  ]\n" + 
        "}", JSONObjectWriter.toJSON("", job));
  }
  
  public void testExample19() {
    Document doc = new Document(null, "  ");
    GeneralXynaObject obj = JSONDatamodelServicesImpl.parseObjectFromJSON(doc, null);
    assertEquals(null, obj);
    List<GeneralXynaObject> lst = JSONDatamodelServicesImpl.parseListFromJSON(doc, null);
    assertEquals(lst.size(),0);
  }

  
  public void testException1() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":nula}";
    try {
      List<JSONToken> l = jt.tokenize(jsonString);
      System.out.println(l);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 6. Cause: Unexpected characters", e.getMessage());
    }
  }
  
  public void testException2() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":  nullm}";
    try {
      List<JSONToken> l = jt.tokenize(jsonString);
      System.out.println(l);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 12. Cause: Unexpected character", e.getMessage());
    }
  }
  
  public void testException3() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"\":true,\"\\u444xmm\"}";
    try {
      List<JSONToken> l = jt.tokenize(jsonString);
      System.out.println(l);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 12. Cause: Expected unicode code point (4 hexadecimal digits)", e.getMessage());
    }
  }
  
  public void testException4() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"\":true,\"\\u4444s\\a\"}";
    try {
      List<JSONToken> l = jt.tokenize(jsonString);
      System.out.println(l);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 19. Cause: Backslash may not be used in strings except to escape a fixed set of characters.", e.getMessage());
    }
  }
  
  public void testException5() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":-3555e0x}";
    try {
      List<JSONToken> l = jt.tokenize(jsonString);
      System.out.println(l);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 6. Cause: Text could not be parsed as a number.", e.getMessage());
    }
  }

  public void testException6() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\",:-3555}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      System.out.println(job);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 5. Cause: Missing ':'.", e.getMessage());
    }
  }
  

  public void testException7() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":,-3555}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      System.out.println(job);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 6. Cause: Expected the start of a JSON value.", e.getMessage());
    }
  }
  

  public void testException8() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[,-3555}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      System.out.println(job);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 7. Cause: Unexpected comma.", e.getMessage());
    }
  }
  

  public void testException9() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[  {}\" ,-3555}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      System.out.println(job);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 10. Cause: Missing ']'.", e.getMessage());
    }
  }
  

  public void testException10() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[  {},\"\"] ,-3555}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      System.out.println(job);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 17. Cause: Missing key.", e.getMessage());
    }
  }

  public void testException11() {
    JSONTokenizer jt = new JSONTokenizer();
    String jsonString = "{\"a\":[  {},,\"\"] ,-3555}";
    List<JSONToken> tokens = jt.tokenize(jsonString);
    JSONParser jp = new JSONParser(jsonString);
    JSONObject job = new JSONObject();
    try {
      jp.fillObject(tokens, 0, job);
      System.out.println(job);
      fail("err");
    } catch (InvalidJSONException e) {
      assertEquals("JSON string invalid at position 13. Cause: Too may commas.", e.getMessage());
    }
  }
}

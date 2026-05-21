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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONValue;



public class TestWhiteListUtils {

  @Test
  public void testWriteJSON() {
    Object o = JsonPath.compile("$.*").read("{\"a\":[\"b\"]}", Configuration.defaultConfiguration());
    System.out.println(o.getClass().getName());
    System.out.println(o);
    String s = JSONValue.toJSONString(o);
    System.out.println(s);
  }


  @Test
  public void testExtractJSON() {
    String json = WhiteListUtils.extractPathFromJSON(TestParser.JSON, "$.state");
    assertEquals("\"active\"", json);
    json = WhiteListUtils.extractPathFromJSON(TestParser.JSON, "$.serviceCharacteristic[?(@.name=='nonexisting')].value");
    assertEquals("[]", json);
    json = WhiteListUtils.extractPathFromJSON(TestParser.JSON, "$.serviceCharacteristic[?(@.name=='charac2')].value");
    assertEquals("[\"val2\",\"val3\"]", json);

  }


  @Test
  public void testRemovePathFromJSON() {
    String json = WhiteListUtils.removePathFromJSON(TestParser.JSON, "$.serviceCharacteristic");
    if (json.contains("serviceCharacteristic")) {
      fail("serviceCharacteristic not gone");
    }
    if (!json.contains("relatedParty")) {
      fail("relatedParty gone");
    }
    System.out.println(json);
  }


  @Test
  public void testEqualRestOfJSON() {
    assertEquals(false, WhiteListUtils.isJSONTheSameExceptPaths(TestParser.JSON, 
                                                                TestParser.JSON2, 
                                                                Arrays.asList("$.serviceCharacteristic[?(@.name=='intchar1')].value")));
    assertEquals(true, WhiteListUtils.isJSONTheSameExceptPaths(TestParser.JSON,
                                                               TestParser.JSON2,
                         Arrays.asList("$.serviceCharacteristic",
                                       "$.id",
                                       "$.feature",
                                       "$.note",
                                       "$.place",
                                       "$.relatedEntity",
                                       "$.serviceOrderItem",
                                       "$.serviceRelationship[?(@.relationshipType=='redundantTo')].serviceRelationshipCharacteristic",
                                       "$.supportingService",
                                       "$.supportingResource[?(@.name=='res3')].value"
                                       )));
  }
  
  @Test
  public void testListOfChanges() {
    List<String> l = WhiteListUtils.createJsonPathListOfAllChanges(TestParser.JSON, TestParser.JSON2);
    System.out.println(l);
  }
  
  @Test
  public void testIsJSONPartTheSamePathNotFound() {
    assertEquals(true, WhiteListUtils.isJSONPartTheSame("{}", "{}", "$['test']"));
    assertEquals(false, WhiteListUtils.isJSONPartTheSame("{\"test\":\"bla\"}", "{}", "$['test']"));
  }  
  
  @Test
  public void testIsJSONPartTheSame() {
    assertEquals(true, WhiteListUtils.isJSONPartTheSame("{\"test\":{}}", "{\"test\":{}, \"bla\":1}", "$['test']"));
    assertEquals(true, WhiteListUtils.isJSONPartTheSame("{\"test\":\"bla\"}", "{\"test\":\"bla\", \"bla\":1}", "$['test']"));
    assertEquals(false, WhiteListUtils.isJSONPartTheSame("{\"test\":\"bla\"}", "{\"test\":[\"bla\"], \"bla\":1}", "$['test']"));
  }
  
  @Test
  public void testIsJSONPartTheSameEmptyArrayVsNull() {
    assertEquals(true, WhiteListUtils.isJSONPartTheSame("{\"serviceCharacteristic\":[]}", "{}", "$.serviceCharacteristic[?(@.name=='name')]"));
  }
 
  @Test
  public void testJsJSONPartTheSameWrongAccess() {
    assertEquals(true, WhiteListUtils.isJSONPartTheSame("{\"serviceCharacteristic\":{}}", "{}", "serviceCharacteristic[0].@type"));
  }

}

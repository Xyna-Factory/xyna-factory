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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;



public class TestParser {
  
  public static final String JSON="{\r\n"
      + "  \"state\": \"active\",\r\n"
      + "  \"serviceType\": \"st\",\r\n"
      + "  \"@type\": \"Service\",\r\n"
      + "  \"name\": \"My Service\",\r\n"
      + "  \"serviceSpecification\": {\r\n"
      + "    \"name\": \"servicename\",\r\n"
      + "    \"id\": \"serviceid123\",\r\n"
      + "    \"@type\": \"ServiceSpecificationRef\"\r\n"
      + "  },\r\n"
      + "  \"serviceCharacteristic\": [\r\n"
      + "    {\r\n"
      + "      \"name\": \"charac1\",\r\n"
      + "      \"@type\": \"StringCharacteristic\",\r\n"
      + "      \"value\": \"val1\"\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"intchar1\",\r\n"
      + "      \"@type\": \"IntegerCharacteristic\",\r\n"
      + "      \"value\": 1\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"intchar2\",\r\n"
      + "      \"@type\": \"IntegerCharacteristic\",\r\n"
      + "      \"value\": 2345\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"charac2\",\r\n"
      + "      \"@type\": \"StringCharacteristic\",\r\n"
      + "      \"value\": \"val2\"\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"charac2\",\r\n"
      + "      \"@type\": \"StringCharacteristic\",\r\n"
      + "      \"value\": \"val3\"\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"charac3\",\r\n"
      + "      \"@type\": \"StringCharacteristic\",\r\n"
      + "      \"value\": \"555555\"\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"bchar1\",\r\n"
      + "      \"@type\": \"BooleanCharacteristic\",\r\n"
      + "      \"value\": true\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"bchar2\",\r\n"
      + "      \"@type\": \"BooleanCharacteristic\",\r\n"
      + "      \"value\": true\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"name\": \"charac4\",\r\n"
      + "      \"@type\": \"StringCharacteristic\",\r\n"
      + "      \"value\": \"1234567890\"\r\n"
      + "    }\r\n"
      + "  ],\r\n"
      + "  \"serviceRelationship\": [\r\n"
      + "    {\r\n"
      + "      \"relationshipType\": \"redundantTo\",\r\n"
      + "      \"@type\": \"ServiceRelationship\",\r\n"
      + "      \"service\": {\r\n"
      + "        \"id\": \"id of earlier ordered service instance\",\r\n"
      + "        \"@type\": \"ServiceRefOrValue\",\r\n"
      + "        \"@referredType\": \"Service\"\r\n"
      + "      },\r\n"
      + "      \"serviceRelationshipCharacteristic\": [\r\n"
      + "        {\r\n"
      + "          \"name\": \"strchar\",\r\n"
      + "          \"@type\": \"StringCharacteristic\",\r\n"
      + "          \"value\": \"val1\"\r\n"
      + "        }\r\n"
      + "      ]\r\n"
      + "    }\r\n"
      + "  ],\r\n"
      + "  \"relatedParty\": [\r\n"
      + "    {\r\n"
      + "      \"role\": \"role1\",\r\n"
      + "      \"id\": \"id123\",\r\n"
      + "      \"@type\": \"RelatedParty\",\r\n"
      + "      \"@referredType\": \"Organization\"\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"role\": \"rol2\",\r\n"
      + "      \"id\": \"id124\",\r\n"
      + "      \"@type\": \"RelatedParty\",\r\n"
      + "      \"@referredType\": \"Individual\"\r\n"
      + "    }\r\n"
      + "  ],\r\n"
      + "  \"supportingResource\": [\r\n"
      + "     {\r\n"
      + "       \"name\": \"res1\",\r\n"
      + "       \"id\": \"res1id123\",\r\n"
      + "       \"value\": \"res1val\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     },\r\n"
      + "     {\r\n"
      + "       \"name\": \"res2\",\r\n"
      + "       \"id\": \"res2id123\",\r\n"
      + "       \"value\": \"res2val\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     },\r\n"
      + "     {\r\n"
      + "       \"name\": \"res2\",\r\n"
      + "       \"id\": \"res2id124\",\r\n"
      + "       \"value\": \"res2val2\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     },\r\n"
      + "     {\r\n"
      + "       \"name\": \"res3\",\r\n"
      + "       \"id\": \"res3id123\",\r\n"
      + "       \"value\": \"res3val\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     }\r\n"
      + "   ]\r\n"
      + "}"; 
  
  
  public static final String JSON2 ="{\r\n"
      + "    \"@type\" : \"Service\",\r\n"
      + "    \"feature\" : [],\r\n"
      + "    \"id\" : \"Service.101111\",\r\n"
      + "    \"name\" : \"My Service\",\r\n"
      + "    \"note\" : [],\r\n"
      + "    \"place\" : [],\r\n"
      + "    \"relatedEntity\" : [],\r\n"
      + "  \"relatedParty\": [\r\n"
      + "    {\r\n"
      + "      \"role\": \"role1\",\r\n"
      + "      \"id\": \"id123\",\r\n"
      + "      \"@type\": \"RelatedParty\",\r\n"
      + "      \"@referredType\": \"Organization\"\r\n"
      + "    },\r\n"
      + "    {\r\n"
      + "      \"role\": \"rol2\",\r\n"
      + "      \"id\": \"id124\",\r\n"
      + "      \"@type\": \"RelatedParty\",\r\n"
      + "      \"@referredType\": \"Individual\"\r\n"
      + "    }\r\n"
      + "  ],\r\n"
      + "    \"serviceCharacteristic\" : [\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"StringCharacteristic\",\r\n"
      + "          \"name\" : \"char1\",\r\n"
      + "          \"value\" : \"123\"\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"IntegerCharacteristic\",\r\n"
      + "          \"name\" : \"char2\",\r\n"
      + "          \"value\" : 2\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"IntegerCharacteristic\",\r\n"
      + "          \"name\" : \"char3\",\r\n"
      + "          \"value\" : 7777\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"StringCharacteristic\",\r\n"
      + "          \"name\" : \"char4\",\r\n"
      + "          \"value\" : \"bla\"\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"StringCharacteristic\",\r\n"
      + "          \"name\" : \"char5\",\r\n"
      + "          \"value\" : \"testtt\"\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"BooleanCharacteristic\",\r\n"
      + "          \"name\" : \"char6\",\r\n"
      + "          \"value\" : true\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"BooleanCharacteristic\",\r\n"
      + "          \"name\" : \"char7\",\r\n"
      + "          \"value\" : true\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"StringCharacteristic\",\r\n"
      + "          \"name\" : \"char8\",\r\n"
      + "          \"value\" : \"yes\"\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"StringCharacteristic\",\r\n"
      + "          \"name\" : \"char9\",\r\n"
      + "          \"value\" : \"no\"\r\n"
      + "        },\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"IntegerCharacteristic\",\r\n"
      + "          \"name\" : \"char10\",\r\n"
      + "          \"value\" : 2\r\n"
      + "        }\r\n"
      + "    ],\r\n"
      + "    \"serviceOrderItem\" : [],\r\n"
      + "    \"serviceRelationship\" : [\r\n"
      + "      {\r\n"
      + "          \"@type\" : \"ServiceRelationship\",\r\n"
      + "          \"relationshipType\" : \"redundantTo\",\r\n"
      + "          \"service\" : {\r\n"
      + "              \"@referredType\" : \"Service\",\r\n"
      + "              \"@type\" : \"ServiceRefOrValue\",\r\n"
      + "              \"id\" : \"id of earlier ordered service instance\"\r\n"
      + "            }\r\n"
      + "        }\r\n"
      + "    ],\r\n"
      + "  \"serviceSpecification\": {\r\n"
      + "    \"name\": \"servicename\",\r\n"
      + "    \"id\": \"serviceid123\",\r\n"
      + "    \"@type\": \"ServiceSpecificationRef\"\r\n"
      + "  },\r\n"
      + "    \"serviceType\" : \"st\",\r\n"
      + "    \"state\" : \"active\",\r\n"
      + "  \"supportingResource\": [\r\n"
      + "     {\r\n"
      + "       \"name\": \"res1\",\r\n"
      + "       \"id\": \"res1id123\",\r\n"
      + "       \"value\": \"res1val\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     },\r\n"
      + "     {\r\n"
      + "       \"name\": \"res2\",\r\n"
      + "       \"id\": \"res2id123\",\r\n"
      + "       \"value\": \"res2val\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     },\r\n"
      + "     {\r\n"
      + "       \"name\": \"res2\",\r\n"
      + "       \"id\": \"res2id124\",\r\n"
      + "       \"value\": \"res2val2\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     },\r\n"
      + "     {\r\n"
      + "       \"name\": \"res3\",\r\n"
      + "       \"id\": \"res3id123\",\r\n"
      + "       \"value\": \"res3val2\",\r\n"
      + "       \"@referredType\": \"PhysicalResource\",\r\n"
      + "       \"@type\": \"ResourceRef\"\r\n"
      + "     }\r\n"
      + "   ],\r\n"
      + "    \"supportingService\" : []\r\n"
      + "  }";

  private void testMatches(String json, String expr, List<String> paths, String expectedExpression, Object expectedOutput) {
    TMFExpressionParser p = ParserCache.getParser(-1L);
    SyntaxTreeNode stn = p.parse(expr, 0, true);
    if (expectedExpression != null) {
      assertEquals(expectedExpression, stn.toString());
    } else {
      System.out.println(stn.toString());
    }
    Object r = stn.eval(new TMFExpressionContext(json, paths, null));
    assertEquals(expectedOutput, r);
  }


  private void testParsingError(String expr) {
    TMFExpressionParser p = ParserCache.getParser(-1L);
    try {
      p.parse(expr, 0, true);
    } catch (RuntimeException e) {
      System.out.println("could not parse <" + expr + ">: " + e.getMessage());
      return;
    }
    fail();
  }

  private void testValidationError(String expr) {
    TMFExpressionParser p = ParserCache.getParser(-1L);
    SyntaxTreeNode stn = p.parse(expr, 0, true);
    try {
      stn.validate();
    } catch (RuntimeException e) {
      System.out.println("could not validate <" + expr + ">: " + e.getMessage());
      return;
    }
    fail();
  }

  @Test
  public void test1() {
    testMatches("{}", "1+2", null, "\"1\" + \"2\"", 3L);
  }


  @Test
  public void testSyntaxErrors() {
    testParsingError("1+");
    testParsingError("1+2+LEN(\"3\"");
    testParsingError("1+2+LEN(\"3)");
    testParsingError("1++LEN(\"3\")");
    testParsingError("+LEN(\"3\")");
    testParsingError("LEN(\"3\")+");
    testParsingError("LEN(\"3\")+  ");
    testParsingError("LEN(\"3\")  +  ");
    testParsingError("LEN (\"3\")  +  1");
    testParsingError("LEN\"3\")  +  1");
  }
  
  @Test
  public void testValidationErrors() {
    testValidationError("LEN(1, 2)");
  }


  @Test
  public void testOperatorPrecedence() {
    testMatches("{}", "1+2*3==7.0e0", null, "\"1\" + \"2\" * \"3\" == \"7.0e0\"", true);
    testMatches("{}", "1+2*3<=6/(1+2)+6", null, null, true);
  }
  
  @Test
  public void testStringComparison() {
    testMatches("{}", "\"ASD\"==\"ASD\"", null, null, true);
    testMatches("{}", "\"ASD\"==\"BSD\"", null, null, false);
    testMatches("{}", "\"ASD\"!=\"BSD\"", null, null, true);
    testMatches("{}", "\"4\"==4", null, null, true);
    testMatches("{}", "\"4\"<5", null, null, true);
    testMatches("{}", "\"4\"<4-3", null, null, false);
  }
  
  @Test
  public void testBooleanOperators() {
    testMatches("{}", "true", null, null, "true");
    testMatches("{}", "true || 1 == 2", null, null, true);
    testMatches("{}", "true AND 1 == 2", null, null, false);
    testMatches("{}", "\"ASD\" != \"ASD\" OR 2 > 1", null, null, true);
  } 
  
  @Test
  public void testLength() {
    testMatches("{}", "1+LEN(\"123\")*2==LEN(7)+6", null, null, true);
  }
  
  @Test
  public void testLengthWithPath() {
    testMatches(JSON, "LEN(EVAL($0))==0", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='notthere')].value"}), null, true);
    testMatches(JSON, "LEN(EVAL($0))==1", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='charac1')].value"}), null, true);
    testMatches(JSON, "LEN(EVAL($0))==2", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='charac2')].value"}), null, true);    
    testMatches(JSON, "LEN(CONCAT(EVAL($0), \"A\"))==5", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='intchar2')].value"}), null, true);

    testMatches(JSON, "LEN(EVAL($0))==0", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='notthere')]"}), null, true);
    testMatches(JSON, "LEN(EVAL($0))==1", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='charac1')]"}), null, true);
    testMatches(JSON, "LEN(EVAL($0))==2", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='charac2')]"}), null, true);    
  }
  
  
  @Test
  public void test163817() {
    testMatches(JSON, "EVAL($0)>0", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='intchar2')].value"}), null, true);
    testMatches(JSON, "LEN(EVAL($0))==1", Arrays.asList(new String[] {"$.supportingResource[?(@.name=='res1')]"}), null, true);
  }
  
  @Test
  public void testEval() {
    testMatches(JSON, "LEN(EVAL($0))==0", Arrays.asList(new String[] {"$.serviceCharacteristic[?(@.name=='intchar5')].value"}), null, true);
  }
  
  @Test
  public void testNot() {
    testMatches("{}", "2 == 3 OR NOT(1>3)", null, null, true);
  }
  
  @Test
  public void testStringEscaping() {
    testMatches("{}", "\"\\\"\\\\\"", null, null, "\"\\");
  }

  
  private void assertResultOfExpression(String expression, Object expectedResult) {
    String json = "{}";
    List<String> paths = new ArrayList<>();
    TMFExpressionParser parser = ParserCache.getParser(-1L);
    TMFExpressionContext ctx = new TMFExpressionContext(json, paths, null);
    SyntaxTreeNode n = parser.parse(expression, 0, true);
    n.validate();
      Object result = n.eval(ctx);
      if (expectedResult == null) {
        if (result != null) {
          fail("result was not null but " + result);
        }
      } else  {
        assertEquals(expectedResult, result);
      }
  }

  @Test
  public void testMatchLeft() {
    assertResultOfExpression("\"\\d{2}\"~=31", true);
    assertResultOfExpression("\"\\d{3}\"~=31", false);
  }
  
  @Test
  public void testMatchRight() {
    assertResultOfExpression("\"aba\"=~\"a.a?\"", true);
    assertResultOfExpression("\"ac\"=~\"a.a?\"", true);
    assertResultOfExpression("\"acaa\"=~\"a.a?\"", false);
    assertResultOfExpression("\"acaax\"=~\"(a.?){1,3}\"", true);
  }
  
  @Test
  public void testMatchFunction() {
    assertResultOfExpression("MATCH(\"aba\",\"a.a?\")", true);
    assertResultOfExpression("MATCH(\"\\d{3}\", 31)", false);
    assertResultOfExpression("MATCH(\"\\d{2}\", 31)", false);
    assertResultOfExpression("MATCH(31, \"\\d{2}\")", true);
  }
  
  @Test
  public void testLazyExecution() {
    assertResultOfExpression("1>0 OR MATCH(\"a\", \"+\")", true);
    try {
      assertResultOfExpression("MATCH(\"a\", \"+\") OR 1>0", true);
      fail("expected exception: illegal pattern syntax");
    } catch (Exception e) {
      //expected
    }
    assertResultOfExpression("1<0 AND MATCH(\"a\", \"+\")", false);
    try {
      assertResultOfExpression("MATCH(\"a\", \"+\") AND 1<0", false);
      fail("expected exception: illegal pattern syntax");
    } catch (Exception e) {
      //expected
    }
  }
  
  @Test
  public void testBigNumbers() {
    assertResultOfExpression("5.0==5", true);
    assertResultOfExpression("0.1==00000000000000000000000.100000000", true);
    assertResultOfExpression("0.1==0.10000000000000000000000000000001", false);
    assertResultOfExpression("110000000000000000000==1.1e20", true);
    assertResultOfExpression("0123456789012345678901234567890.0==123456789012345678901234567890", true);
    assertResultOfExpression("0123456789012345678901234567890!=123456789012345678901234567891", true);
    assertResultOfExpression("1+0123456789012345678901234567890==123456789012345678901234567891", true);
    assertResultOfExpression("1+0123456789012345678901234567890>123456789012345678901234567890", true);
    assertResultOfExpression("1e834==1.0000000000000000e834", true);
    assertResultOfExpression("1.0e834==1.0000000000000000e834", true);
    assertResultOfExpression("1.00e834==1.0000000000000000e834", true);
    assertResultOfExpression("1.000e834==1.0000000000000000e834", true);
    assertResultOfExpression("1.0000e834==1.0000000000000000e834", true);
  }
  
}

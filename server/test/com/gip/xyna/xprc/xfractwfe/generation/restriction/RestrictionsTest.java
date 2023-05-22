/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.generation.restriction;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class RestrictionsTest extends TestCase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  @Test
  public void testMandatoryObjectXMLParsing() {
    Map<String, Restrictions> restrictionMap = parseDatatypeXML(MandatoryTestObject.getXMLDefinition());
    assertMandatoryObjectRestrictionsMap(restrictionMap);
  }
  
  
  @Test
  public void testMandatoryObjectClassParsing() {
    Map<String, Restrictions> restrictionMap = RestrictionUtils.parseClass(MandatoryTestObject.class);
    assertMandatoryObjectRestrictionsMap(restrictionMap);
  }
  
  
  @Test
  public void testMultipleRestrictionsObjectXMLParsing() {
    Map<String, Restrictions> restrictionMap = parseDatatypeXML(MultipleRestrictionsTestObject.getXMLDefinition());
    assertMultipleRestrictionsObjectRestrictionsMap(restrictionMap);
  }
  
  
  @Test
  public void testMultipleRestrictionsObjectClassParsing() {
    Map<String, Restrictions> restrictionMap = RestrictionUtils.parseClass(MultipleRestrictionsTestObject.class);
    assertMultipleRestrictionsObjectRestrictionsMap(restrictionMap);
  }
  
  
  private void assertMandatoryObjectRestrictionsMap(Map<String, Restrictions> restrictionMap) {
    assertTrue("Missing Restrictions for memVar " + MandatoryTestObject.ALWAYS_MANDATORY_VAR_NAME,
               restrictionMap.containsKey(MandatoryTestObject.ALWAYS_MANDATORY_VAR_NAME));
    assertTrue("Missing Restrictions for memVar " + MandatoryTestObject.NEVER_MANDATORY_VAR_NAME,
               restrictionMap.containsKey(MandatoryTestObject.NEVER_MANDATORY_VAR_NAME));
    assertTrue("Missing Restrictions for memVar " + MandatoryTestObject.STORE_MANDATORY_VAR_NAME,
               restrictionMap.containsKey(MandatoryTestObject.STORE_MANDATORY_VAR_NAME));
    assertTrue("Missing Restrictions for memVar " + MandatoryTestObject.MULTIPLE_MANDATORY_VAR_NAME,
               restrictionMap.containsKey(MandatoryTestObject.MULTIPLE_MANDATORY_VAR_NAME));

    final String ARBITRARY_UTILIZATION = "some.ValidationService";
    final String XMOM_PERSISTENCE_STORE_UTILIZATION = Utilizations.XMOM_PERSISTENCE_STORE.getName();
    
    Restrictions alwaysRestrictions = restrictionMap.get(MandatoryTestObject.ALWAYS_MANDATORY_VAR_NAME);
    assertNotNull("Missing Restrictions for memVar " + MandatoryTestObject.ALWAYS_MANDATORY_VAR_NAME, alwaysRestrictions);
    assertTrue("Failed to find MANDATORY-Typed restriction", alwaysRestrictions.hasRestriction(RestrictionType.MANDATORY));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + ARBITRARY_UTILIZATION,
               alwaysRestrictions.hasRestriction(RestrictionType.MANDATORY, ARBITRARY_UTILIZATION));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + XMOM_PERSISTENCE_STORE_UTILIZATION,
               alwaysRestrictions.hasRestriction(RestrictionType.MANDATORY, XMOM_PERSISTENCE_STORE_UTILIZATION));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + MandatoryTestObject.REASON1_UTILIZATION,
               alwaysRestrictions.hasRestriction(RestrictionType.MANDATORY, MandatoryTestObject.REASON1_UTILIZATION));
    assertNull("Restriction is expected to contain no utilizations",
                alwaysRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations);
    
    Restrictions neverRestrictions = restrictionMap.get(MandatoryTestObject.NEVER_MANDATORY_VAR_NAME);
    assertNotNull("Missing Restrictions for memVar " + MandatoryTestObject.NEVER_MANDATORY_VAR_NAME, neverRestrictions);
    assertTrue("Failed to find MANDATORY-Typed restriction", neverRestrictions.hasRestriction(RestrictionType.MANDATORY));
    assertFalse("Restriction should not have been found with utilization " + ARBITRARY_UTILIZATION,
                neverRestrictions.hasRestriction(RestrictionType.MANDATORY, ARBITRARY_UTILIZATION));
    assertFalse("Restriction should not have been found with utilization " + XMOM_PERSISTENCE_STORE_UTILIZATION,
                neverRestrictions.hasRestriction(RestrictionType.MANDATORY, XMOM_PERSISTENCE_STORE_UTILIZATION));
    assertFalse("Restriction should not have been found with utilization " + MandatoryTestObject.REASON1_UTILIZATION,
                neverRestrictions.hasRestriction(RestrictionType.MANDATORY, MandatoryTestObject.REASON1_UTILIZATION));
    assertNotNull("Restriction is expected to contain a utilization",
                  neverRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations);
    assertEquals("Restriction is expected to contain a utilization",
                  1, neverRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.size());
    assertEquals("Restriction is expected to contain the utilization " + MandatoryTestObject.NEVER_UTILIZATION,
                 MandatoryTestObject.NEVER_UTILIZATION, neverRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.get(0));
    
    Restrictions storeRestrictions = restrictionMap.get(MandatoryTestObject.STORE_MANDATORY_VAR_NAME);
    assertNotNull("Missing Restrictions for memVar " + MandatoryTestObject.STORE_MANDATORY_VAR_NAME, neverRestrictions);
    assertTrue("Failed to find MANDATORY-Typed restriction", storeRestrictions.hasRestriction(RestrictionType.MANDATORY));
    assertFalse("Restriction should not have been found with utilization " + ARBITRARY_UTILIZATION,
                storeRestrictions.hasRestriction(RestrictionType.MANDATORY, ARBITRARY_UTILIZATION));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + XMOM_PERSISTENCE_STORE_UTILIZATION,
                storeRestrictions.hasRestriction(RestrictionType.MANDATORY, XMOM_PERSISTENCE_STORE_UTILIZATION));
    assertFalse("Restriction should not have been found with utilization " + MandatoryTestObject.REASON1_UTILIZATION,
                storeRestrictions.hasRestriction(RestrictionType.MANDATORY, MandatoryTestObject.REASON1_UTILIZATION));
    assertNotNull("Restriction is expected to contain a utilization",
                  storeRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations);
    assertEquals("Restriction is expected to contain a utilization",
                  1, storeRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.size());
    assertEquals("Restriction is expected to contain a utilization" + XMOM_PERSISTENCE_STORE_UTILIZATION,
                 XMOM_PERSISTENCE_STORE_UTILIZATION, storeRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.get(0));
    
    Restrictions multipleRestrictions = restrictionMap.get(MandatoryTestObject.MULTIPLE_MANDATORY_VAR_NAME);
    assertNotNull("Missing Restrictions for memVar " + MandatoryTestObject.MULTIPLE_MANDATORY_VAR_NAME, multipleRestrictions);
    assertTrue("Failed to find MANDATORY-Typed restriction", storeRestrictions.hasRestriction(RestrictionType.MANDATORY));
    assertFalse("Restriction should not have been found with utilization " + ARBITRARY_UTILIZATION,
                multipleRestrictions.hasRestriction(RestrictionType.MANDATORY, ARBITRARY_UTILIZATION));
    assertFalse("Restriction should not have been found with utilization " + XMOM_PERSISTENCE_STORE_UTILIZATION,
               multipleRestrictions.hasRestriction(RestrictionType.MANDATORY, XMOM_PERSISTENCE_STORE_UTILIZATION));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + MandatoryTestObject.REASON1_UTILIZATION,
                multipleRestrictions.hasRestriction(RestrictionType.MANDATORY, MandatoryTestObject.REASON1_UTILIZATION));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + MandatoryTestObject.REASON2_UTILIZATION,
               multipleRestrictions.hasRestriction(RestrictionType.MANDATORY, MandatoryTestObject.REASON2_UTILIZATION));
    assertTrue("Failed to find MANDATORY-Typed restriction with utilization " + MandatoryTestObject.REASON3_UTILIZATION,
               multipleRestrictions.hasRestriction(RestrictionType.MANDATORY, MandatoryTestObject.REASON3_UTILIZATION));
    assertNotNull("Restriction is expected to contain a utilization",
                  multipleRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations);
    assertEquals("Restriction is expected to contain 3 utilization",
                  3, multipleRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.size());
    assertTrue("Restriction is expected to contain a utilization" + MandatoryTestObject.REASON1_UTILIZATION,
               multipleRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.contains(MandatoryTestObject.REASON1_UTILIZATION));
    assertTrue("Restriction is expected to contain a utilization" + MandatoryTestObject.REASON2_UTILIZATION,
               multipleRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.contains(MandatoryTestObject.REASON2_UTILIZATION));
    assertTrue("Restriction is expected to contain a utilization" + MandatoryTestObject.REASON3_UTILIZATION,
               multipleRestrictions.getRestriction(RestrictionType.MANDATORY).utilizations.contains(MandatoryTestObject.REASON3_UTILIZATION));
  }
  
  
  private void assertMultipleRestrictionsObjectRestrictionsMap(Map<String, Restrictions> restrictionMap) {
    assertTrue("Missing Restrictions for memVar " + MultipleRestrictionsTestObject.MAN_MAX_VAR_NAME,
               restrictionMap.containsKey(MultipleRestrictionsTestObject.MAN_MAX_VAR_NAME));
    assertTrue("Missing Restrictions for memVar " + MultipleRestrictionsTestObject.MAX_DEF_VAR_NAME,
               restrictionMap.containsKey(MultipleRestrictionsTestObject.MAX_DEF_VAR_NAME));
    assertTrue("Missing Restrictions for memVar " + MultipleRestrictionsTestObject.DEF_MAN_VAR_NAME,
               restrictionMap.containsKey(MultipleRestrictionsTestObject.DEF_MAN_VAR_NAME));
    assertTrue("Missing Restrictions for memVar " + MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME,
               restrictionMap.containsKey(MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME));
    
    Restrictions manMaxRestrictions = restrictionMap.get(MultipleRestrictionsTestObject.MAN_MAX_VAR_NAME);
    assertEquals("Variable " + MultipleRestrictionsTestObject.MAN_MAX_VAR_NAME + " is expected to have 2 restrictions",
                 2, manMaxRestrictions.getRestrictions().size());
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAN_MAX_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.MANDATORY, 
               manMaxRestrictions.hasRestriction(RestrictionType.MANDATORY));
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAN_MAX_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.MAX_LENGTH, 
               manMaxRestrictions.hasRestriction(RestrictionType.MAX_LENGTH));
    assertEquals("Restriction of type " + RestrictionType.MAX_LENGTH + " is expected to have a value of " + MultipleRestrictionsTestObject.MAN_MAX_LIMIT,
                 MultipleRestrictionsTestObject.MAN_MAX_LIMIT, manMaxRestrictions.<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH).getLimit());
    
    Restrictions maxDefRestrictions = restrictionMap.get(MultipleRestrictionsTestObject.MAX_DEF_VAR_NAME);
    assertEquals("Variable " + MultipleRestrictionsTestObject.MAX_DEF_VAR_NAME + " is expected to have 2 restrictions",
                 2, maxDefRestrictions.getRestrictions().size());
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAX_DEF_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.MAX_LENGTH, 
               maxDefRestrictions.hasRestriction(RestrictionType.MAX_LENGTH));
    assertEquals("Restriction of type " + RestrictionType.MAX_LENGTH + " is expected to have a value of " + MultipleRestrictionsTestObject.MAX_DEF_LIMIT,
                 MultipleRestrictionsTestObject.MAX_DEF_LIMIT, maxDefRestrictions.<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH).getLimit());
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAX_DEF_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.DEFAULT_TYPE, 
               maxDefRestrictions.hasRestriction(RestrictionType.MAX_LENGTH));
    assertEquals("Restriction of type " + RestrictionType.DEFAULT_TYPE + " is expected to have a value of " + MultipleRestrictionsTestObject.MAX_DEF_DEFAULT_TYPE,
                 MultipleRestrictionsTestObject.MAX_DEF_DEFAULT_TYPE, maxDefRestrictions.<DefaultTypeRestriction>getRestriction(RestrictionType.DEFAULT_TYPE).getDefaultType());
    
    Restrictions defManRestrictions = restrictionMap.get(MultipleRestrictionsTestObject.DEF_MAN_VAR_NAME);
    assertEquals("Variable " + MultipleRestrictionsTestObject.DEF_MAN_VAR_NAME + " is expected to have 2 restrictions",
                 2, defManRestrictions.getRestrictions().size());
    assertTrue("Variable " + MultipleRestrictionsTestObject.DEF_MAN_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.DEFAULT_TYPE, 
               defManRestrictions.hasRestriction(RestrictionType.DEFAULT_TYPE));
    assertEquals("Restriction of type " + RestrictionType.DEFAULT_TYPE + " is expected to have a value of " + MultipleRestrictionsTestObject.DEF_MAN_DEFAULT_TYPE,
                 MultipleRestrictionsTestObject.DEF_MAN_DEFAULT_TYPE, defManRestrictions.<DefaultTypeRestriction>getRestriction(RestrictionType.DEFAULT_TYPE).getDefaultType());
    assertTrue("Variable " + MultipleRestrictionsTestObject.DEF_MAN_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.MANDATORY, 
               defManRestrictions.hasRestriction(RestrictionType.MANDATORY));
    
    Restrictions manMaxDefRestrictions = restrictionMap.get(MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME);
    assertEquals("Variable " + MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME + " is expected to have 3 restrictions",
                 3, manMaxDefRestrictions.getRestrictions().size());
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.MANDATORY, 
               manMaxDefRestrictions.hasRestriction(RestrictionType.MANDATORY));
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.MAX_LENGTH, 
               manMaxDefRestrictions.hasRestriction(RestrictionType.MAX_LENGTH));
    assertEquals("Restriction of type " + RestrictionType.MAX_LENGTH + " is expected to have a value of " + MultipleRestrictionsTestObject.MAN_MAX_DEF_LIMIT,
                 MultipleRestrictionsTestObject.MAN_MAX_DEF_LIMIT, manMaxDefRestrictions.<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH).getLimit());
    assertTrue("Variable " + MultipleRestrictionsTestObject.MAN_MAX_DEF_VAR_NAME + " is expected to have a restriction of type " + RestrictionType.DEFAULT_TYPE, 
               manMaxDefRestrictions.hasRestriction(RestrictionType.DEFAULT_TYPE));
    assertEquals("Restriction of type " + RestrictionType.DEFAULT_TYPE + " is expected to have a value of " + MultipleRestrictionsTestObject.MAN_MAX_DEF_DEFAULT_TYPE,
                 MultipleRestrictionsTestObject.MAN_MAX_DEF_DEFAULT_TYPE, manMaxDefRestrictions.<DefaultTypeRestriction>getRestriction(RestrictionType.DEFAULT_TYPE).getDefaultType());
  }

  
  private Map<String, Restrictions> parseDatatypeXML(String xml) {
    Map<String, Restrictions> restrictionMap = new HashMap<>();
    try {
      Document doc = XMLUtils.parseString(xml);
      List<Element> memVarElements = XMLUtils.getChildElementsByName(doc.getDocumentElement(), GenerationBase.EL.DATA);
      for (Element memVarElement : memVarElements) {
        Element restrictionElement = XMLUtils.getChildElementByName(memVarElement, GenerationBase.EL.RESTRICTION);
        if (restrictionElement != null) {
          Restrictions restrictions = new Restrictions();
          restrictions.parseXml(restrictionElement);
          restrictionMap.put(memVarElement.getAttribute(GenerationBase.ATT.VARIABLENAME), restrictions);
        }
      }
    } catch (XPRC_XmlParsingException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    return restrictionMap;
  }
  
  
}

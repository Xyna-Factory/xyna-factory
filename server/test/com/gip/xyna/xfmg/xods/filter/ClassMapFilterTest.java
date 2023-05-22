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
package com.gip.xyna.xfmg.xods.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;


public class ClassMapFilterTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    for (TestMapper mapper : TestMapper.values()) {
      ClassMapFilters.getInstance().registerMapper(TestObject.class.getSimpleName(), mapper);
    }
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    Field field = ClassMapFilters.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null); // clear static instance variable
    super.tearDown();
  }
  
  public static String[] filters = new String[] {
/* test0 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aBoolean+").filter(WhiteList(true)).allMatch()",
/* test1 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aBoolean+").filter(WhiteList(true)).noneMatch()",
/* test2 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aBoolean+").filter(BlackList(true)).allMatch()",
/* test3 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aBoolean+").filter(BlackList(true)).noneMatch()",
/* test4 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aInt+").filter(WhiteList(1,2,3)).allMatch()",
/* test5 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aInt+").filter(WhiteList(1,2,3)).map("+TestMapper.aBoolean+").filter(WhiteList(true)).allMatch()",
/* test6 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aInt+").filter(WhiteList(1,2,3)).map("+TestMapper.aBoolean+").filter(WhiteList(true)).map("+TestMapper.aString+").filter(WhiteList(acdc,adac)).allMatch()",
/* test7 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aString+").filter(RegExp(^a.+c$)).allMatch()",
/* test8 */ "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aString+").filter(RegExp(^a.+c$)).filter(BlackList(abc)).allMatch()"
  };
  
  
  public static String[][] testValues  = new String[][] {
    // aString  aInt  aBoolean   test0     test1    test2    test3    test4    test5    test6    test7    test8
    {    "abc",  "0",   "true",  "true", "false", "false",  "true", "false", "false", "false",  "true", "false" },
    {    "abc",  "0",  "false", "false",  "true",  "true", "false", "false", "false", "false",  "true", "false" },
    {   "adac",  "1",   "true",  "true", "false", "false",  "true",  "true",  "true",  "true",  "true",  "true" },
    {   "adac",  "1",  "false", "false",  "true",  "true", "false",  "true", "false", "false",  "true",  "true" },
    {   "acdc",  "2",   "true",  "true", "false", "false",  "true",  "true",  "true",  "true",  "true",  "true" },
    {   "acdc",  "2",  "false", "false",  "true",  "true", "false",  "true", "false", "false",  "true",  "true" },
    {   "abba",  "3",   "true",  "true", "false", "false",  "true",  "true",  "true", "false", "false", "false" },
    {   "abba",  "3",  "false", "false",  "true",  "true", "false",  "true", "false", "false", "false", "false" },
    {    "abc",  "4",   "true",  "true", "false", "false",  "true", "false", "false", "false",  "true", "false" },
    {    "abc",  "4",  "false", "false",  "true",  "true", "false", "false", "false", "false",  "true", "false" },
  };
  
  
  public void testClassMapFilters() {
    List<TestObject> instances = createInstances();
    List<ClassMapFilter<TestObject>> filters = createFilter();
    for (int i = 0; i < instances.size(); i++) {
      for (int j = 0; j < filters.size(); j++) {
        boolean expectation = getExpectedResult(i, j);
        assertEquals("According to testValues test"+j+" should have evaluated to "+ expectation + " for " + instances.get(i),
                     expectation,
                     filters.get(j).accept(instances.get(i)));
      }
    }
  }
  
  
  public void testSpacesInWhiteList() {
    //class(GenerationBase).map(fqXmlName).filter(WhiteList("xact.snmp.commands.SNMPService", "xact.monitoring.MonitoringHelperServices")).allMatch()
    String definition1 = "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aString+").filter(WhiteList(\"a\", \"b\")).allMatch()";
    ClassMapFilter<TestObject> classMapFilter = ClassMapFilterParser.<TestObject>build(definition1);
    TestObject match1 = new TestObject("a", 1, true);
    TestObject match2 = new TestObject("b", 1, true);
    TestObject miss = new TestObject("c", 1, true);
    
    assertTrue(classMapFilter.accept(match1));
    assertTrue(classMapFilter.accept(match2));
    assertFalse(classMapFilter.accept(miss));
    
    String definition2 = "class("+TestObject.class.getSimpleName()+").map("+TestMapper.aString+").filter(WhiteList(a, b)).allMatch()";
    classMapFilter = ClassMapFilterParser.<TestObject>build(definition2);
    
    assertTrue(classMapFilter.accept(match1));
    assertTrue(classMapFilter.accept(match2));
    assertFalse(classMapFilter.accept(miss));
  }
  
  
  private List<TestObject> createInstances() {
    List<TestObject> instances = new ArrayList<ClassMapFilterTest.TestObject>();
    for (String[] values : testValues) {
      instances.add(new TestObject(values[0], Integer.parseInt(values[1]), Boolean.parseBoolean(values[2])));
    }
    return instances;
  }
  
  
  private List<ClassMapFilter<TestObject>> createFilter() {
    List<ClassMapFilter<TestObject>> filters = new ArrayList<ClassMapFilter<TestObject>>();
    for (String filter : ClassMapFilterTest.filters) {
      ClassMapFilter<TestObject> classMapFilter = ClassMapFilterParser.<TestObject>build(filter);
      assertNotNull("Failed to construct ClassMapFilter for: " + filter, classMapFilter);
      filters.add(classMapFilter);
    }
    return filters;
  }
  
  
  private boolean getExpectedResult(int valueIndex, int filterIndex) {
    return Boolean.parseBoolean(testValues[valueIndex][filterIndex+3]);
  }
  

  private static enum TestMapper implements StringMapper<TestObject> {
    aString {
      @Override
      public String map(TestObject instance) {
        return instance.aString;
      }
    },
    aInt {
      @Override
      public String map(TestObject instance) {
        return String.valueOf(instance.aInt);
      }
    },
    aBoolean {
      @Override
      public String map(TestObject instance) {
        return String.valueOf(instance.aBoolean);
      }
    };

    public String getIdentifier() {
      return toString();
    }

    public abstract String map(TestObject instance);
    
  }
  
  
  private static class TestObject {
    
    private String aString;
    private int aInt;
    private boolean aBoolean;
    
    public TestObject(String aString, int aInt, boolean aBoolean) {
      this.aString = aString;
      this.aInt = aInt;
      this.aBoolean = aBoolean;
    }
    
    @Override
    public String toString() {
      return "aString="+aString+" aInt="+aInt+" aBool="+aBoolean;
    }
    
  }
  
}

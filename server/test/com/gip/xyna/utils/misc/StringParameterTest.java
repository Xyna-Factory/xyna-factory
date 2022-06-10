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
package com.gip.xyna.utils.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;
import com.gip.xyna.utils.misc.StringParameter.DefaultValueModifiable;
import com.gip.xyna.utils.misc.StringParameter.ListSeparator;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;


/**
 *
 */
public class StringParameterTest extends TestCase {

  public enum Testenum { abc, def, ghi };
  
  public enum Testenum2 implements DocumentedEnum { 
    abc( Documentation.de("erste 3 Buchstaben des Alphabets").
         en("first 3 letters of alphabet").
         build() ), 
    def( Documentation.de("Abkürzung für default").
         en("abbreviation for default").
         build() ), 
    ghi( Documentation.de("weitere drei Buchstaben").
         en("another three letters").
         build() );
  
    private Documentation doc;

    private Testenum2( Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
    
  }
  
  public final static StringParameter<Boolean> BOOLEAN = StringParameter.typeBoolean("boolean").build();
  public final static StringParameter<Integer> INTEGER = StringParameter.typeInteger("integer").build();
  
  public final static StringParameter<List<String>> STRING_LIST = StringParameter.typeList(String.class, "stringList", ListSeparator.WHITESPACE).build();
  public final static StringParameter<List<Integer>> INTEGER_LIST = StringParameter.typeList(Integer.class, "integerList", ListSeparator.WHITESPACE).build();
  
  public final static StringParameter<Testenum> ENUM = StringParameter.typeEnum(Testenum.class, "enum").build();
  public final static StringParameter<EnumSet<Testenum>> ENUMSET = StringParameter.typeEnumSet(Testenum.class, "enumSet").build();

  private String parse(StringParameter<?> sp, Object object) {
    try {
      Object value = sp.parse(object);
      if( value != null ) {
        return "type="+value.getClass().getSimpleName()+", value="+value;
      } else {
        return "null";
      }
    } catch (StringParameter.StringParameterParsingException e) {
      Throwable cause = e.getCause();
      if( cause == null ) {
        return "exception=\""+e.getMessage()+"\"";
      } else {
        return "exception=\""+e.getMessage()+"\", cause=\""+cause.getClass().getSimpleName()+": "+cause.getMessage()+"\"";
      }
    }
  }

  private String parse( List<String> params, StringParameter<?> ... sps) {
    try {
      Map<String, Object> map = StringParameter.parse(params).with(sps);
      
      if( map.size() <= 1 ) {
        return map.toString();
      } else {
        TreeMap<String, Object> sorted = new TreeMap<String, Object>(map);
        return sorted.toString();
      }
    } catch (StringParameter.StringParameterParsingException e) {
      return "exception=\""+e.getMessage()+"\"";
    }
  }
  
 
  private String parse(List<StringParameter<?>> allParameters, List<String> params) {
    try {
      Map<String, Object> map = StringParameter.parse(params).with(allParameters);
      return map.toString();
    } catch (StringParameter.StringParameterParsingException e) {
      return "exception=\""+e.getMessage()+"\"";
    }
  }

  private String combine(String ... strings ) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( String s : strings ) {
      sb.append(sep).append(s);
      sep = "\n";
    }
    return sb.toString();
  }
  
  private static void saveToFile(Object write, String filename) throws IOException {
    FileOutputStream fos = new FileOutputStream(filename);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(write);
    oos.close();
    fos.close();
  }

  private static Object readFromFile(String filename) throws IOException, ClassNotFoundException {
    FileInputStream fis = new FileInputStream(filename);
    ObjectInputStream ois = new ObjectInputStream(fis);
    Object o = ois.readObject();
    ois.close();
    return o;
  }
  
  private Map<String,String> asMap(String ... strings) {
    Map<String,String> map = new HashMap<String,String>();
    for( int i=0; i<strings.length; i+=2) {
      map.put( strings[i], strings[i+1] );
    }
    return map;
  }
  
  private Map<String,Object> asObjectMap(Object ... objects) {
    Map<String,Object> map = new HashMap<String,Object>();
    for( int i=0; i<objects.length; i+=2) {
      map.put( (String)objects[i], objects[i+1] );
    }
    return map;
  }

  private Set<String> asSet(String ... strings) {
    return new HashSet<String>(Arrays.asList(strings) );
  }

  
  public void testLong() {
    StringParameter<Long> sp = StringParameter.typeLong("test").build();
    Assert.assertEquals( "type=Long, value=5", parse( sp, "5" ) );
    Assert.assertEquals( "null", parse( sp, "" ) );
    Assert.assertEquals( "exception=\"test could not be parsed to Long\", cause=\"NumberFormatException: For input string: \"a\"\"", parse( sp, "a" ) );
  }
  
  public void testLong_default() {
    StringParameter<Long> sp = StringParameter.typeLong("test").defaultValue(1000L).build();
    Assert.assertEquals( "type=Long, value=5", parse( sp, "5" ) );
    Assert.assertEquals( "type=Long, value=1000", parse( sp, "" ) );
    Assert.assertEquals( "type=Long, value=1000", parse( sp, null ) );
    Assert.assertEquals( "exception=\"test could not be parsed to Long\", cause=\"NumberFormatException: For input string: \"a\"\"", parse( sp, "a" ) );
  }
  
  public void testLong_mandatory() {
    StringParameter<Long> sp = StringParameter.typeLong("test").mandatory().build();
    Assert.assertEquals( "type=Long, value=5", parse( sp, "5" ) );
    Assert.assertEquals( "exception=\"test is mandatory\"", parse( sp, "" ) );
    Assert.assertEquals( "exception=\"test is mandatory\"", parse( sp, null ) );
    Assert.assertEquals( "exception=\"test could not be parsed to Long\", cause=\"NumberFormatException: For input string: \"a\"\"", parse( sp, "a" ) );
  }
  
  public void testLong_pattern() {
    StringParameter<Long> sp = StringParameter.typeLong("test").pattern("[1-9]\\d\\d\\d+").build();
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "5" ) );
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "a" ) );
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "999" ) );
    Assert.assertEquals( "type=Long, value=1000", parse( sp, "1000" ) );
    Assert.assertEquals( "type=Long, value=1234", parse( sp, "1234" ) );
  }

  public void testString() {
    StringParameter<String> sp = StringParameter.typeString("test").build();
    Assert.assertEquals( "type=String, value=5", parse( sp, "5" ) );
    Assert.assertEquals( "null", parse( sp, "" ) );
    Assert.assertEquals( "null", parse( sp, null ) );
    Assert.assertEquals( "type=String, value=a", parse( sp, "a" ) );
  }
  
  public void testString_default() {
    StringParameter<String> sp = StringParameter.typeString("test").defaultValue("default").build();
    Assert.assertEquals( "type=String, value=5", parse( sp, "5" ) );
    Assert.assertEquals( "type=String, value=default", parse( sp, "" ) );
    Assert.assertEquals( "type=String, value=a", parse( sp, "a" ) );
  }
 
  public void testString_mandatory() {
    StringParameter<String> sp = StringParameter.typeString("test").mandatory().build();
    Assert.assertEquals( "type=String, value=5", parse( sp, "5" ) );
    Assert.assertEquals( "exception=\"test is mandatory\"", parse( sp, "" ) );
    Assert.assertEquals( "type=String, value=a", parse( sp, "a" ) );
  }
 
  public void testString_pattern() {
    StringParameter<String> sp = StringParameter.typeString("test").pattern("ab.*").build();
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "" ) );
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "axy" ) );
    Assert.assertEquals( "type=String, value=abxy", parse( sp, "abxy" ) );
  }
 
  public void testString_pattern_default() {
    StringParameter<String> sp = StringParameter.typeString("test").pattern("ab.*").defaultValue("default").build();
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "" ) );
    Assert.assertEquals( "type=String, value=default", parse( sp, null ) );
    Assert.assertEquals( "exception=\"test does not match pattern\"", parse( sp, "axy" ) );
    Assert.assertEquals( "type=String, value=abxy", parse( sp, "abxy" ) );
  }

  public void testBoolean() {
    StringParameter<Boolean> sp = StringParameter.typeBoolean("test").build();
    Assert.assertEquals( "exception=\"test could not be parsed to Boolean\"", parse( sp, "5" ) );
    Assert.assertEquals( "null", parse( sp, "" ) );
    Assert.assertEquals( "null", parse( sp, null ) );
    Assert.assertEquals( "type=Boolean, value=true", parse( sp, "true" ) );
    Assert.assertEquals( "type=Boolean, value=false", parse( sp, "false" ) );
    Assert.assertEquals( "exception=\"test could not be parsed to Boolean\"", parse( sp, "fal" ) );
    Assert.assertEquals( "type=Boolean, value=true", parse( sp, "TRUE" ) );
    Assert.assertEquals( "type=Boolean, value=false", parse( sp, "faLSe" ) );
  }
  
  public void testEnum() {
    StringParameter<Testenum> sp = StringParameter.typeEnum(Testenum.class, "test").build();
    Assert.assertEquals( "exception=\"test could not be parsed to Testenum\", cause=\"IllegalArgumentException: No enum const class com.gip.xyna.utils.misc.StringParameterTest$Testenum.5\"", parse( sp, "5" ) );
    Assert.assertEquals( "type=Testenum, value=abc", parse( sp, "abc" ) );
    Assert.assertEquals( "exception=\"test could not be parsed to Testenum\", cause=\"IllegalArgumentException: No enum const class com.gip.xyna.utils.misc.StringParameterTest$Testenum.ABC\"", parse( sp, "ABC" ) );
    Assert.assertEquals( "null", parse( sp, "" ) );
    Assert.assertEquals( "null", parse( sp, null ) );
    
    StringParameter<Testenum> sp2 = StringParameter.typeEnum(Testenum.class, "test", true).build();
    Assert.assertEquals( "type=Testenum, value=abc", parse( sp2, "abc" ) );
    Assert.assertEquals( "type=Testenum, value=abc", parse( sp2, "aBc" ) );
    Assert.assertEquals( "type=Testenum, value=abc", parse( sp2, "ABC" ) );
    Assert.assertEquals( "exception=\"test could not be parsed to Testenum\", cause=\"IllegalArgumentException: No enum const class com.gip.xyna.utils.misc.StringParameterTest$Testenum.5\"", parse( sp2, "5" ) );
    
  }
  
  public void testEnum_default() {
    StringParameter<Testenum> sp = StringParameter.typeEnum(Testenum.class, "test").defaultValue(Testenum.def).build();
    Assert.assertEquals( "type=Testenum, value=abc", parse( sp, "abc" ) );
    Assert.assertEquals( "type=Testenum, value=def", parse( sp, "" ) );
    Assert.assertEquals( "type=Testenum, value=def", parse( sp, null ) );
  }

  public void testStringList() {
    StringParameter<List<String>> sp = StringParameter.typeList(String.class, "test", ListSeparator.WHITESPACE).build();
    ListSeparator separator_colon = new ListSeparator(":", ":", null);
    ListSeparator separator_blank = new ListSeparator(" ", " ", null);
    StringParameter<List<String>> sp2 = StringParameter.typeList(String.class, "test", separator_colon).build();
    StringParameter<List<String>> sp3 = StringParameter.typeList(String.class, "test", ListSeparator.WHITESPACE, false).build();
    StringParameter<List<String>> sp4 = StringParameter.typeList(String.class, "test", separator_blank, false).build();
    Assert.assertEquals( "type=ArrayList, value=[abc]", parse( sp, "abc" ) );
    Assert.assertEquals( "type=ArrayList, value=[abc, def, ghi]", parse( sp, "abc def ghi" ) );
    Assert.assertEquals( "type=ArrayList, value=[abc:def:ghi]", parse( sp, "abc:def:ghi" ) );
    Assert.assertEquals( "type=ArrayList, value=[abc, def, ghi]", parse( sp2, "abc:def:ghi" ) );
    Assert.assertEquals( "type=ArrayList, value=[abc, def, ghi]", parse( sp, " abc \tdef \n  ghi  " ) );
    Assert.assertEquals( "type=ArrayList, value=[, abc, def, x, ghi]", parse( sp3, " abc \tdef \nx  ghi" ) );
    Assert.assertEquals( "type=ArrayList, value=[, abc, \tdef, \nx, , ghi]", parse( sp4, " abc \tdef \nx  ghi" ) );
  }
  
  public void testIntegerList() {
    StringParameter<List<Integer>> sp = StringParameter.typeList(Integer.class, "test", ListSeparator.WHITESPACE ).build();
    StringParameter<List<Integer>> sp2 = StringParameter.typeList(Integer.class, "test" ).build();
    Assert.assertEquals( "type=ArrayList, value=[1]", parse( sp, "1" ) );
    Assert.assertEquals( "type=ArrayList, value=[1, 3, 6]", parse( sp, "1 3 6" ) );
    Assert.assertEquals( "type=ArrayList, value=[1, 3, 5, 56]", parse( sp2, "1,3, 5,56" ) );
    //Assert.assertEquals( "type=ArrayList, value=[abc, def, ghi]", parse( sp2, "abc:def:ghi" ) );
    //Assert.assertEquals( "type=ArrayList, value=[abc, def, ghi]", parse( sp, " abc \tdef \n  ghi  " ) );
    //Assert.assertEquals( "type=ArrayList, value=[abc, \tdef, \nx, , ghi]", parse( sp3, " abc \tdef \nx  ghi" ) );
  }
  
  public void testEnumSet() {
    StringParameter<EnumSet<Testenum>> sp = StringParameter.typeEnumSet(Testenum.class, "test").build();
    
    Assert.assertEquals( "type=RegularEnumSet, value=[abc]", parse( sp, "abc" ) );
    Assert.assertEquals( "type=RegularEnumSet, value=[abc, ghi]", parse( sp, "abc, ghi" ) );
    Assert.assertEquals( "type=RegularEnumSet, value=[abc, def, ghi]", parse( sp, "abc,def,ghi" ) );
    
    Assert.assertEquals( "type=RegularEnumSet, value=[abc, ghi]", parse( sp, EnumSet.of(Testenum.abc, Testenum.ghi ) ) );
    Assert.assertEquals( "type=RegularEnumSet, value=[abc, ghi]", parse( sp, asSet("abc", "ghi") ) );
    Assert.assertEquals( "type=RegularEnumSet, value=[abc, ghi]", parse( sp, asMap("abc", "true", "def", "false", "ghi", "true") ) );
    Assert.assertEquals( "type=RegularEnumSet, value=[abc, ghi]", parse( sp, asObjectMap("abc", 1, "def", false, "ghi", "true") ) );
    
    
    Assert.assertEquals( "exception=\"test could not be parsed to EnumSet\", cause=\"IllegalArgumentException: No enum const class com.gip.xyna.utils.misc.StringParameterTest$Testenum.123\"", parse( sp, "123" ) );
  }
 
  public void testEnumMap() {
    StringParameter<EnumMap<Testenum,String>> sp = StringParameter.typeEnumMap(Testenum.class, "test").build();
    
    Assert.assertEquals( "type=EnumMap, value={abc=123}", parse( sp, "abc=123" ) );
    Assert.assertEquals( "type=EnumMap, value={abc=null}", parse( sp, "abc" ) );
    Assert.assertEquals( "type=EnumMap, value={abc=}", parse( sp, "abc=" ) );
    Assert.assertEquals( "type=EnumMap, value={abc=123, ghi=456}", parse( sp, "abc=123, ghi=456" ) );
    Assert.assertEquals( "type=EnumMap, value={abc=123, def=null, ghi=}", parse( sp, "abc=123,def,ghi=" ) );
    
    Assert.assertEquals( "type=EnumMap, value={abc=123, def=null, ghi=}", parse( sp, asObjectMap("abc", 123, "def", null, "ghi", "" ) ) );
    
    Assert.assertEquals( "type=EnumMap, value={abc=1\"23, def=78, ghi=4,56}", parse( sp, "abc=1\"\"23, \"ghi=4,56\", def=78" ) );
    
    Assert.assertEquals( "exception=\"test could not be parsed to EnumMap\", cause=\"IllegalArgumentException: No enum const class com.gip.xyna.utils.misc.StringParameterTest$Testenum.123\"", parse( sp, "123" ) );
  }
  
  
  public void testMap() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").build();
    
    List<String> params = Arrays.asList( "test1=abc", "test2=string" );
    Assert.assertEquals( "{test1=abc, test2=string}", parse( params, sp1, sp2 ) );
  }
  
  public void testMap_unknown() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").build();
    
    List<String> params = Arrays.asList( "test1=abc", "test2=string", "test3=unknown" );
    Assert.assertEquals( "exception=\"test3 is no StringParameter\"", parse( params, sp1, sp2 ) );
  }
  
  public void testMap_format() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").build();
    
    List<String> params = Arrays.asList( "test1=abc", "test+2=string" );
    Assert.assertEquals( "exception=\"test+2=string is no StringParameter\"", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test1=abc", "test1+2=string" );
    Assert.assertEquals( "exception=\"test1+2=string is no StringParameter\"", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test1=abc", "test2=" );
    Assert.assertEquals( "{test1=abc, test2=null}", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test1=abc", "test2" );
    Assert.assertEquals( "{test1=abc, test2=null}", parse( params, sp1, sp2 ) );
  }
  
  
  
  public void testMap_default() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").defaultValue("default").build();
    
    List<String> params = Arrays.asList( "test1=abc" );
    Assert.assertEquals( "{test1=abc}", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test1=abc", "test2" );
    Assert.assertEquals( "{test1=abc, test2=default}", parse( params, sp1, sp2 ) );
  }

  public void testMap_mandatoryFor() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").mandatoryFor(sp1).build();
    
    List<String> params = Arrays.asList( "test1=abc" );
    Assert.assertEquals( "exception=\"test2 is mandatory for test1\"", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test1=abc", "test2=string" );
    Assert.assertEquals( "{test1=abc, test2=string}", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test2=string" );
    Assert.assertEquals( "{test2=string}", parse( params, sp1, sp2 ) );
  }
  
  public void testMap_mandatoryForValue() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").mandatoryFor(sp1, Testenum.def).build();
    
    List<String> params = Arrays.asList( "test1=abc" );
    Assert.assertEquals( "{test1=abc}", parse( params, sp1, sp2 ) );
  
    params = Arrays.asList( "test1=def" );
    Assert.assertEquals( "exception=\"test2 is mandatory for test1 with value def\"", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test1=def", "test2=string" );
    Assert.assertEquals( "{test1=def, test2=string}", parse( params, sp1, sp2 ) );
    
    params = Arrays.asList( "test2=string" );
    Assert.assertEquals( "{test2=string}", parse( params, sp1, sp2 ) );
  }
  
  public void testMap_multiple_mandatory() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "test1").build();
    StringParameter<Boolean> sp3 = StringParameter.typeBoolean( "test3").build();
    StringParameter<String> sp2 = StringParameter.typeString("test2").
        mandatoryFor(sp1, Testenum.def).
        mandatoryFor(sp1, Testenum.ghi).
        mandatoryFor(sp3).
        build();
    List<StringParameter<?>> allParameters = StringParameter.asList(sp1,sp2,sp3);
    
        
    List<String> params = Arrays.asList( "test1=abc" );
    Assert.assertEquals( "{test1=abc}", parse( allParameters, params ) );
    
    params = Arrays.asList( "test1=abc", "test3=true" );
    Assert.assertEquals( "exception=\"test2 is mandatory for test3\"", parse( allParameters, params) );
    
    params = Arrays.asList( "test1=def" );
    Assert.assertEquals( "exception=\"test2 is mandatory for test1 with value def\"", parse( allParameters, params) );

    params = Arrays.asList( "test1=ghi", "test3=true", "test2=string" );
    Assert.assertEquals( "{test1=ghi, test2=string, test3=true}", parse( allParameters, params ) );
 
  }
  
  public void testExample() {
    StringParameter<Testenum> sp1 = StringParameter.typeEnum(Testenum.class, "testEnum").
        description("beliebige Enums sind möglich").build();
    StringParameter<Boolean> sp3 = StringParameter.typeBoolean("testBoolean").
        description("nur true und false sind caseInsensitive zugelassen").build();
    StringParameter<String> sp2 = StringParameter.typeString("testMandatoryFor").
        mandatoryFor(sp1, Testenum.def).
        mandatoryFor(sp1, Testenum.ghi).
        mandatoryFor(sp3).
        description("Parameter muss angegeben werden, "+
            "falls 'testBoolean' gesetzt wird oder 'testEnum' auf 'def' oder 'ghi' gesetzt wird").
        build();
    StringParameter<Integer> sp4 = StringParameter.typeInteger("testMandatory").
        mandatory().description("dies ist ein Pflichtparameter").build();
    StringParameter<Long> sp5 = StringParameter.typeLong("testDefaultUndValidierung").
        defaultValue(1234L).pattern("[1-9]\\d\\d\\d+").
        description("hat einen Default-Parameter und eine Validierung (>=1000)").build();
    
    List<StringParameter<?>> allParameters = StringParameter.asList(sp1,sp2,sp3,sp4,sp5);
    List<String> params = null;
    
    params = Arrays.asList( "testMandatory=5" );
    try {
      Map<String, Object> map = StringParameter.parse(params).with(allParameters);
      Assert.assertEquals( "{testMandatory=5}", map.toString() );
      Testenum testEnum = sp1.getFromMap(map);
      Assert.assertNull(testEnum);
      String testMandatoryFor = sp2.getFromMap(map);
      Assert.assertNull(testMandatoryFor);
      Boolean testBoolean = sp3.getFromMap(map);
      Assert.assertNull(testBoolean);
      int testMandatory = sp4.getFromMap(map);
      Assert.assertEquals(5, testMandatory);
      long testDefaultUndValidierung = sp5.getFromMap(map);
      Assert.assertEquals(1234, testDefaultUndValidierung); //default, obwohl nicht in Map und in Parametern
    } catch( StringParameterParsingException e ) {
      Assert.fail("No exception expected: "+e.getMessage() );
    }
    
    params = Arrays.asList( "testEnum=abc" );
    Assert.assertEquals( "exception=\"testMandatory is mandatory\"", parse( allParameters, params ) );
       
    params = Arrays.asList( "testMandatory=1", "testEnum=abc" );
    Assert.assertEquals( "{testEnum=abc, testMandatory=1}", parse( allParameters, params ) );
    
    params = Arrays.asList( "testMandatory=2", "testEnum=abc", "testBoolean=true" );
    Assert.assertEquals( "exception=\"testMandatoryFor is mandatory for testBoolean\"", parse( allParameters, params) );
    
    params = Arrays.asList( "testMandatory=3", "testEnum=def" );
    Assert.assertEquals( "exception=\"testMandatoryFor is mandatory for testEnum with value def\"", parse( allParameters, params) );

    params = Arrays.asList( "testMandatory=4",  "testEnum=ghi", "testBoolean=true", "testMandatoryFor=string" );
    Assert.assertEquals( "{testEnum=ghi, testMandatoryFor=string, testBoolean=true, testMandatory=4}", parse( allParameters, params ) );
 
    params = Arrays.asList( "testMandatory=5", "testDefaultUndValidierung" );
    Assert.assertEquals( "{testDefaultUndValidierung=1234, testMandatory=5}", parse( allParameters, params ) );
        
    params = Arrays.asList( "testMandatory=6", "testDefaultUndValidierung=999" );
    Assert.assertEquals( "exception=\"testDefaultUndValidierung does not match pattern\"", parse( allParameters, params) );

    params = Arrays.asList( "testMandatory=7", "testDefaultUndValidierung=1000" );
    Assert.assertEquals( "{testDefaultUndValidierung=1000, testMandatory=7}", parse( allParameters, params ) );
    
  }

  
  public void testEnum_documentation() { 
    StringParameter<Testenum> sp = StringParameter.typeEnum(Testenum.class, "test").
        documentation( Documentation.en("description").de("Beschreibung").build()).
        defaultValue(Testenum.def).build();
    String indentation = "    ";
    StringBuilder output = new StringBuilder();
    StringParameterFormatter.appendStringParameter(output, sp, DocumentationLanguage.EN, indentation);
    output.append("\n");
    StringParameterFormatter.appendStringParameter(output, sp, DocumentationLanguage.DE, indentation);
        
    Assert.assertEquals( combine(
        "test: description (optional, type=Enum(abc, def, ghi), default=def)",
        "test: Beschreibung (optional, type=Enum(abc, def, ghi), default=def)"
        ), output.toString() );
    
    StringParameter<Testenum2> sp2 = StringParameter.typeEnum(Testenum2.class, "test").
        documentation(Documentation.en("description").de("Beschreibung").build()).
        defaultValue(Testenum2.def).build();
    
    
    output = new StringBuilder();
    StringParameterFormatter.appendStringParameter(output, sp2, DocumentationLanguage.EN, indentation);
    output.append("\n");
    StringParameterFormatter.appendStringParameter(output, sp2, DocumentationLanguage.DE, indentation);
   
    Assert.assertEquals( combine(
       "test: description (optional, type=Enum, default=def",
       "    abc: first 3 letters of alphabet",
       "    def: abbreviation for default",
       "    ghi: another three letters",
       "    )",
       "test: Beschreibung (optional, type=Enum, default=def",
       "    abc: erste 3 Buchstaben des Alphabets",
       "    def: Abkürzung für default",
       "    ghi: weitere drei Buchstaben",
       "    )"
     ), output.toString() );
         
  }
  
  public void testEnum_documentation_after_serialization() throws IOException, ClassNotFoundException { 
    StringParameter<Testenum2> sp = StringParameter.typeEnum(Testenum2.class, "test").
        documentation(Documentation.en("description").de("Beschreibung").build()).
        defaultValue(Testenum2.def).build();
    String indentation = "    ";
    
    StringBuilder output = new StringBuilder();
    StringParameterFormatter.appendStringParameter(output, sp, DocumentationLanguage.EN, indentation);
    Assert.assertEquals( combine(
                                 "test: description (optional, type=Enum, default=def",
                                 "    abc: first 3 letters of alphabet",
                                 "    def: abbreviation for default",
                                 "    ghi: another three letters",
                                 "    )"
                               ), output.toString() );

    
    
    saveToFile(sp, "StringParameterTest.ser");
    StringParameter sp2 = (StringParameter) readFromFile("StringParameterTest.ser");
    
    output = new StringBuilder();
    StringParameterFormatter.appendStringParameter(output, sp2, DocumentationLanguage.EN, indentation);
    Assert.assertEquals( combine(
                                 "test: description (optional, type=Enum, default=def",
                                 "    abc: first 3 letters of alphabet",
                                 "    def: abbreviation for default",
                                 "    ghi: another three letters",
                                 "    )"
                               ), output.toString() );
   
  }
  
  public void testToList() throws StringParameterParsingException {
    
    StringParameter<String> sps = StringParameter.typeString("testString").
        defaultValue("ein String").build();
    StringParameter<Boolean> spb = StringParameter.typeBoolean("testBoolean").
        defaultValue(true).build();
    StringParameter<Integer> spi = StringParameter.typeInteger("testInteger").
        defaultValue(123).build();
    StringParameter<List<Integer>> spil = StringParameter.typeList(Integer.class, "testIntList").
        defaultValue(Arrays.asList(1,2,3)).build();
    StringParameter<Testenum> spe = StringParameter.typeEnum(Testenum.class, "testEnum").
        defaultValue(Testenum.def).build();
    StringParameter<EnumSet<Testenum>> spes = StringParameter.typeEnumSet(Testenum.class, "testEnumSet").
        defaultValue(EnumSet.of(Testenum.ghi,Testenum.def)).build();
    EnumMap<Testenum,String> emDefault = new EnumMap<Testenum,String>(Testenum.class);
    emDefault.put(Testenum.abc,"AB, \"");
    emDefault.put(Testenum.def, null);
    emDefault.put(Testenum.ghi, "");
    StringParameter<EnumMap<Testenum,String>> spem = StringParameter.typeEnumMap(Testenum.class, "testEnumMap").
        defaultValue(emDefault).build();

    
    Map<String, Object> emptyMap = new HashMap<String, Object>();
    Assert.assertEquals( "ein String", sps.getFromMap(emptyMap) );
    Assert.assertEquals( Boolean.TRUE, spb.getFromMap(emptyMap) );
    Assert.assertEquals( Integer.valueOf(123), spi.getFromMap(emptyMap) );
    Assert.assertEquals( "[1, 2, 3]", spil.getFromMap(emptyMap).toString() );
    Assert.assertEquals( Testenum.def, spe.getFromMap(emptyMap) );
    Assert.assertEquals( "[def, ghi]", spes.getFromMap(emptyMap).toString() );
    Assert.assertEquals( "{abc=AB, \", def=null, ghi=}", spem.getFromMap(emptyMap).toString() );

    
    List<StringParameter<?>> splist = StringParameter.asList( sps, spb, spi, spil, spe, spes, spem );
     
    List<String> strings = StringParameter.toList(splist, null, true); 
    
    Assert.assertEquals( "[testString=ein String, testBoolean=true, testInteger=123, testIntList=1, 2, 3, testEnum=def, testEnumSet=def, ghi, testEnumMap=\"abc=AB, \"\"\",def=null,ghi=]", strings.toString() );

    
    Map<String, Object> paramMap = null;
    paramMap = StringParameter.parse(strings).with(splist);
    
    Assert.assertEquals( "ein String", sps.getFromMap(paramMap) );
    Assert.assertEquals( Boolean.TRUE, spb.getFromMap(paramMap) );
    Assert.assertEquals( Integer.valueOf(123), spi.getFromMap(paramMap) );
    Assert.assertEquals( "[1, 2, 3]", spil.getFromMap(paramMap).toString() );
    Assert.assertEquals( Testenum.def, spe.getFromMap(paramMap) );
    Assert.assertEquals( "[def, ghi]", spes.getFromMap(paramMap).toString() );
    Assert.assertEquals( "{abc=AB, \", def=null, ghi=}", spem.getFromMap(paramMap).toString() );
    
  }
  
  public void testEnumSet_documentation() { 
    
    
  }
  

  
  public void testParamStringMapToMap() throws StringParameterParsingException {
    List<StringParameter<?>> sps = StringParameter.asList(BOOLEAN,INTEGER,STRING_LIST,INTEGER_LIST,ENUM,ENUMSET);
    
    Map<String,String> params = new HashMap<String,String>();
    params.put("boolean", "true");
    params.put("stringList", "a b c");
    params.put("integerList", "1 23 456");
    params.put("enum", "abc" );
    params.put("enumSet", "abc, ghi" );
    
    Map<String, Object> paramMap = StringParameter.parse(params).with(sps);
    
    //System.out.println(paramMap );
    
    assertEquals("[1, 23, 456]", INTEGER_LIST.getFromMap(paramMap).toString() );
    assertEquals(Testenum.abc, ENUM.getFromMap(paramMap));
    assertEquals(EnumSet.of(Testenum.abc, Testenum.ghi), ENUMSET.getFromMap(paramMap));
    
  }
  
  public void testParamObjectMapToMap() throws StringParameterParsingException {
    List<StringParameter<?>> sps = StringParameter.asList(BOOLEAN,INTEGER,STRING_LIST,INTEGER_LIST,ENUM,ENUMSET);
    
    Map<String,Object> params = new HashMap<String,Object>();
    params.put("boolean", true);
    params.put("stringList", Arrays.asList("a","b","c") );
    params.put("integerList", Arrays.asList(1, 23, 456) );
    params.put("enum", Testenum.abc );
    //params.put("enumSet", EnumSet.of(Testenum.abc, Testenum.ghi) );
    params.put("enumSet", asSet("abc", "ghi") );
    

    Map<String, Object> paramMap = StringParameter.parse(params).with(sps);
    
    //System.out.println(paramMap );
    assertEquals("[1, 23, 456]", INTEGER_LIST.getFromMap(paramMap).toString() );
    assertEquals(Testenum.abc, ENUM.getFromMap(paramMap));
    assertEquals(EnumSet.of(Testenum.abc, Testenum.ghi), ENUMSET.getFromMap(paramMap));

   // FIXME 
    //params.put( "boolean", 1);
    //params.put("integerList", Arrays.asList("a","b","c") );
    //params.put("stringList", Arrays.asList(1, 23, 456) );
    
    //paramMap = StringParameter.paramObjectMapToMap(sps, params);
  }

  public void testDefaultValueModifiable() throws StringParameterParsingException, IOException, ClassNotFoundException {
    DefaultValueModifiable<Boolean> modDefVal = new DefaultValueModifiable<Boolean>(true);
    StringParameter<Boolean> sp = StringParameter.typeBoolean("test").defaultValue(modDefVal).build();
    
    assertEquals("true", StringParameter.parse(Arrays.asList("test=true")).with(sp).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=false")).with(sp).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    modDefVal.setDefaultValue(null);  
    assertNull( StringParameter.parse(Arrays.asList("test=")).with(sp).get("test") );
    modDefVal.setDefaultValue(false);  
    assertEquals("false", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    
    //nun serialisieren und nochmal testen
    saveToFile(sp, "StringParameterTest.ser");
    assertEquals("false", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    modDefVal.setDefaultValue(true);  
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    
    //deserialisieren 
    StringParameter<?> sp2 = (StringParameter<?>) readFromFile("StringParameterTest.ser");
    assertEquals("true", StringParameter.parse(Arrays.asList("test=true")).with(sp2).get("test").toString() );
    modDefVal.setDefaultValue(false);  
    assertEquals("false", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=")).with(sp2).get("test").toString() );
    
    //deserialisiert sind keine Default-Änderungen mehr möglich
    modDefVal.setDefaultValue(true);  
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=")).with(sp2).get("test").toString() );
 
  }
 
  public void testDefaultValueSerialization() throws StringParameterParsingException, IOException, ClassNotFoundException {
    DefaultValueModifiable<Boolean> modDefVal = new DefaultValueModifiable<Boolean>(true);
    StringParameter<Boolean> spM = StringParameter.typeBoolean("test").defaultValue(modDefVal).build();
    StringParameter<Boolean> spF = StringParameter.typeBoolean("test").defaultValue(true).build();
    
    assertEquals("true", StringParameter.parse(Arrays.asList("test=true")).with(spM).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=true")).with(spF).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=false")).with(spM).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=false")).with(spF).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(spM).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(spF).get("test").toString() );

    saveToFile(spM, "StringParameterTest.ser");
    StringParameter<?> spM2 = (StringParameter<?>) readFromFile("StringParameterTest.ser");
    saveToFile(spF, "StringParameterTest.ser");
    StringParameter<?> spF2 = (StringParameter<?>) readFromFile("StringParameterTest.ser");
    
    assertEquals("true", StringParameter.parse(Arrays.asList("test=true")).with(spM2).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=true")).with(spF2).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=false")).with(spM2).get("test").toString() );
    assertEquals("false", StringParameter.parse(Arrays.asList("test=false")).with(spF2).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(spM2).get("test").toString() );
    assertEquals("true", StringParameter.parse(Arrays.asList("test=")).with(spF2).get("test").toString() );
   
  }


  public void testEnumSerialization() throws StringParameterParsingException, IOException, ClassNotFoundException {

    StringParameter<Testenum> sp = StringParameter.typeEnum(Testenum.class, "test").defaultValue(Testenum.abc).build();
    
    assertEquals("def", StringParameter.parse(Arrays.asList("test=def")).with(sp).get("test").toString() );
    assertEquals("abc", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    
    //nun serialisieren und nochmal testen
    saveToFile(sp, "StringParameterTest.ser");
    assertEquals("def", StringParameter.parse(Arrays.asList("test=def")).with(sp).get("test").toString() );
    assertEquals("abc", StringParameter.parse(Arrays.asList("test=")).with(sp).get("test").toString() );
    
    //deserialisieren 
    StringParameter<?> sp2 = (StringParameter<?>) readFromFile("StringParameterTest.ser");
    assertEquals("def", StringParameter.parse(Arrays.asList("test=def")).with(sp2).get("test").toString() );
    assertEquals("abc", StringParameter.parse(Arrays.asList("test=")).with(sp2).get("test").toString() );
   
  }
}

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
package com.gip.xyna.utils.misc;

import java.util.Arrays;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

public class JsonTest {

  
  
  public static void main(String[] args) throws InvalidJSONException, UnexpectedJSONContentException {
    
    TestObject to = TestObject.newInstance(1, TestObject.newInstance(2,null));
    String json = toJson(to);
    
    System.out.println(json);
    
    JsonParser jp = new JsonParser();
    
    TestObject top = jp.parse(json, TestObject.getJsonVisitor() );
    
    System.out.println( toJson(top));
    
  }
  
  private static String toJson(TestObject to) {
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();{
      to.toJson(jb);
    }jb.endObject();
    
    String json = jb.toString();
    return json;
  }
  
  
  private static class TestSubObject implements JsonSerializable {
    
    private String name;
    private int index;

    public TestSubObject(String name, int index) {
      this.name = name;
      this.index = index;
    }

    public void toJson(JsonBuilder jb) {
      jb.addStringAttribute("name", name );
      jb.addIntegerAttribute("index", index);
    }

    public static JsonVisitor<TestSubObject> getJsonVisitor() {
      return new TestSubObjectJsonVisitor();
    }
    
    private static class TestSubObjectJsonVisitor extends EmptyJsonVisitor<TestSubObject> {
      TestSubObject tso = new TestSubObject("",0);
      
      @Override
      public TestSubObject get() {
        return tso;
      }
      @Override
      public TestSubObject getAndReset() {
        TestSubObject ret = tso;
        tso = new TestSubObject("",0);
        return ret;
      }
      
      @Override
      public void attribute(String label, String value, Type type) {
        if( label.equals("index") ) {
          tso.index = Integer.valueOf(value);
          return;
        }
        if( label.equals("name") ) {
          tso.name = value;
          return;
        }
        System.err.println( label + " " + value +" " +type);
      }

    }
  }
  
  
  private static class TestObject implements JsonSerializable {
    int  intVar;
    String stringVar;
    boolean boolVar;
    List<String> stringList;
    TestSubObject subObject;
    List<TestSubObject> objectList;
    TestObject recursive;

    public static TestObject newInstance(int index, TestObject recursive) {
      TestObject to = new TestObject();
      to.intVar = index;
      to.stringVar = "eins";
      to.boolVar = index%2==1;
      to.stringList = Arrays.asList("A","B","C");
      to.subObject = new TestSubObject("a", 1);
      to.objectList = Arrays.asList(new TestSubObject("x", 2), new TestSubObject("y", 3) );
      to.recursive = recursive;
      return to;
    }

    public static JsonVisitor<TestObject> getJsonVisitor() {
      return new TestObjectJsonVisitor();
    }
    
    private static class TestObjectJsonVisitor extends EmptyJsonVisitor<TestObject> {
      TestObject to = new TestObject();
      
      @Override
      public TestObject get() {
        return to;
      }
      @Override
      public TestObject getAndReset() {
        TestObject ret = to;
        to = new TestObject();
        return ret;
      }
      
      @Override
      public void attribute(String label, String value, Type type) {
        if( label.equals("intVar") ) {
          to.intVar = Integer.valueOf(value);
          return;
        }
        if( label.equals("stringVar") ) {
          to.stringVar = value;
          return;
        }
        if( label.equals("boolVar") ) {
          to.boolVar = Boolean.valueOf(value);
          return;
        }
        System.err.println( label + " " + value +" " +type);
      }
      
      
      @Override
      public void list(String label, List<String> value, Type type) {
        if( label.equals("stringList") ) {
          to.stringList = value;
          return;
        }
        System.err.println( label + " " + value +" " +type);
      }
      
      @Override
      public void object(String label, Object value) {
        if( label.equals("subObject") ) {
          to.subObject = (TestSubObject)value;
          return;
        }
        if( label.equals("recursiveObject") ) {
          to.recursive = (TestObject)value;
          return;
        }
        System.err.println( "object "+label);
        
      }
     
      @Override
      public void objectList(String label, List<Object> values) {
        if( label.equals("objectList") ) {
          to.objectList = (List<TestSubObject>)(List)values;
          return;
        }
        System.err.println( label + " " + values );
     }

      @Override
      public JsonVisitor<?> objectStarts(String label) {
        if( label.equals("subObject") ) {
          return TestSubObject.getJsonVisitor();
        }
        if( label.equals("objectList") ) {
          return TestSubObject.getJsonVisitor();
        }
        if( label.equals("recursiveObject") ) {
          return TestObject.getJsonVisitor();
        }
        System.err.println( "objectStarts "+label);
        return null;
      }
      
    }
    

    public void toJson(JsonBuilder jb) {
      jb.addIntegerAttribute("intVar", intVar);
      jb.addStringAttribute("stringVar", stringVar );
      jb.addOptionalBooleanAttribute("boolVar", boolVar);
      if( stringList != null ) {
        jb.addStringListAttribute("stringList", stringList); //FIXME doppelt...
        jb.addListAttribute("stringList");{
          for( String s : stringList ) {
            jb.addStringListElement(s);
          }
          jb.endList();
        }
      }
      jb.addOptionalObjectAttribute("subObject", subObject);
      /*
      if( subObject != null ) {
        jb.addObjectAttribute("subObject"); {
          subObject.toJson(jb);
        }jb.endObject();
      }
      */
      jb.addObjectListAttribute("objectList", objectList);
      /*
      if( objectList != null ) {
        jb.addListAttribute("objectList");{
          for( TestSubObject tso : objectList ) {
            jb.startObject();{
              tso.toJson(jb);
            }jb.endObject();
          }
          jb.endList();
        }
      }*/
      jb.addOptionalObjectAttribute("recursiveObject", recursive);
      /*
      if( recursive != null ) {
        jb.addObjectAttribute("recursiveObject"); {
          recursive.toJson(jb);
        }jb.endObject();
      }
      */
    }
    
    
    
  }
  
}

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
package com.gip.xyna.xfmg.xods.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import junit.framework.TestCase;


/**
 *
 */
public class XynaPropertyUtilsTest extends TestCase {

  private static class XynaPropertySourceImpl extends AbstractXynaPropertySource {
    HashMap<String,String> props = new HashMap<String,String>();
    
    public String getProperty(String name) {
      //System.err.println( "getProperty("+name+")");
  
      return props.get(name);
    }

    public void set(String propertyName, String value) {
      props.put( propertyName, value );
      for( XynaPropertyBase<?,?> xp : getRegisteredProperties(propertyName) ) {
        xp.propertyChanged();
      }
    }

    public void clear() {
      props.clear();
      properties.clear();
    }

    public void setProperty(XynaPropertyBase<?,?> property, String newValue) throws PersistenceLayerException {
      props.put(property.getPropertyName(), newValue);
    }
    
  }

  private static XynaPropertySourceImpl xynaPropertySource = new XynaPropertySourceImpl();
  
  
  protected void setUp() throws Exception {
    XynaPropertyUtils.exchangeXynaPropertySource( xynaPropertySource );
  }
    
  protected void tearDown() throws Exception {
    xynaPropertySource.clear();
  }

  
  public void testStringDefault() {
    XynaPropertyString xps = new XynaPropertyString("test.prop.string", "testStringDefault");
    assertEquals("testStringDefault", xps.get() );
  }
  
  public void testStringGet() {
    XynaPropertyString xps = new XynaPropertyString("test.prop.string", "testStringDefault");
    assertEquals("testStringDefault", xps.get() );
    xynaPropertySource.set("test.prop.string", "newValue");
    assertEquals("newValue", xps.get() );
  }
  
  public void testStringSet() throws PersistenceLayerException {
    XynaPropertyString xps = new XynaPropertyString("test.prop.string", "testStringDefault");
    assertEquals("testStringDefault", xps.get() );
    xps.set("newValue");
    assertEquals("newValue", xps.get() );
    assertEquals("newValue", xynaPropertySource.getProperty("test.prop.string") );
  }

  public void testXynaPropertyBuilds() {
    XynaPropertyBuilds<TestInt> xpb= new XynaPropertyBuilds<TestInt>("test.property.testint",TestInt.builder, new TestInt(2) );
    assertEquals("TestInt(2)", xpb.get().toString() );
    assertEquals(2, xpb.get().intValue() );
    xynaPropertySource.set("test.property.testint", "45");
    assertEquals("TestInt(45)", xpb.get().toString() );
    assertEquals(45, xpb.get().intValue() );
    
    XynaPropertyBuilds<Double> xpbd = new XynaPropertyBuilds<Double>("test.property.testdouble", new Builder<Double>() {
      public Double fromString(String string) {
         return Double.valueOf(string);
      }
      public String toString(Double value) {
        return String.valueOf(value);
      }}, 2.35 );
        
    assertEquals( 2.35, xpbd.get().doubleValue() );
    xynaPropertySource.set("test.property.testdouble", "4.15");
    assertEquals( 4.15, xpbd.get().doubleValue() );
    
    XynaPropertyBuilds<DestinationKey> xpbdk = new XynaPropertyBuilds<DestinationKey>("test.property.destination.key", new DestinationKey("orderType", "applicationName", "versionName") );
    assertEquals( "orderType@applicationName/versionName", xpbdk.get().serializeToString() );
        
    xynaPropertySource.set("test.property.destination.key", "ot@an/vn");
    assertEquals( "ot@an/vn", xpbdk.get().serializeToString() );
  
        
  }
  
  public void testXynaPropertyDuration() {
    XynaPropertyDuration xpt1 = new XynaPropertyDuration("test.property.duration", "1 s" );
    assertEquals( 1000L, xpt1.getMillis() );
    assertEquals( "1 s", xpt1.get().toString() );
    
    XynaPropertyDuration xpt2 = new XynaPropertyDuration("test.property.duration", "2" );
    assertEquals( 2000L, xpt2.getMillis() );
    assertEquals( "2 s", xpt2.get().toString() );
    
    XynaPropertyDuration xpt3 = new XynaPropertyDuration("test.property.duration", "3 min" );
    assertEquals( 180000L, xpt3.getMillis() );
    assertEquals( "3 min", xpt3.get().toString() );
    
    XynaPropertyDuration xpt4 = new XynaPropertyDuration("test.property.duration", "4 min", TimeUnit.SECONDS );
    assertEquals( 240000L, xpt4.getMillis() );
    assertEquals( "4 min", xpt4.get().toString() );
    
    XynaPropertyDuration xpt5 = new XynaPropertyDuration("test.property.duration", "5", TimeUnit.SECONDS );
    assertEquals( 5000L, xpt5.getMillis() );
    assertEquals( "5 s", xpt5.get().toString() );
    
    XynaPropertyDuration xpt6 = new XynaPropertyDuration("test.property.duration", "6", TimeUnit.MILLISECONDS );
    assertEquals( 6L, xpt6.getMillis() );
    assertEquals( "6 ms", xpt6.get().toString() );
    xynaPropertySource.set("test.property.duration", "66");
    assertEquals("66 ms", xpt6.get().toString() );
    assertEquals(66, xpt6.getMillis() );
    xynaPropertySource.set("test.property.duration", "666 s");
    assertEquals("666 s", xpt6.get().toString() );
    assertEquals(666000, xpt6.getMillis() );
    

    XynaPropertyDuration xpt7 = new XynaPropertyDuration("test.property.duration", "7", TimeUnit.MILLISECONDS );
    xynaPropertySource.set("test.property.duration", "77");
    assertEquals( 77L, xpt7.getMillis() );
    assertEquals( "77 ms", xpt7.get().toString() );
    
    xynaPropertySource.set("test.property.duration", "88");
    XynaPropertyDuration xpt8 = new XynaPropertyDuration("test.property.duration", "8", TimeUnit.MILLISECONDS );
    assertEquals( 88L, xpt8.getMillis() );
    assertEquals( "88 ms", xpt8.get().toString() );
    
  }
    
  public void testXynaPropertyList() {
    XynaPropertyBuilds<StringSerializableList<String> > xpbssl = 
        new XynaPropertyBuilds<StringSerializableList<String> >("test.property.list.string", 
            StringSerializableList.separator(String.class));
    assertEquals( "[]", xpbssl.get().toString() );
    
    xpbssl = new XynaPropertyBuilds<StringSerializableList<String> >("test.property.list.string", 
   //     new StringSerializableList<String>("a","b"));
        StringSerializableList.separator(String.class).setValues(Arrays.asList("a","b")) );
    assertEquals( "[a, b]", xpbssl.get().toString() );
    
    xynaPropertySource.set("test.property.list.string", "a, b, c");
    assertEquals( "[a, b, c]", xpbssl.get().toString() );
    
    
    XynaPropertyBuilds<StringSerializableList<Integer> > xpbssli =
        new XynaPropertyBuilds<StringSerializableList<Integer> >("test.property.list.int", 
            StringSerializableList.separator(Integer.class));
    assertEquals( "[]", xpbssli.get().toString() );
    xynaPropertySource.set("test.property.list.int", "1, 7, 3");
    assertEquals( "[1, 7, 3]", xpbssli.get().toString() );
    
  }

  public void testXynaPropertyDestinationKey() {
    
  }



  private static class TestInt {
    int i;

    public TestInt(int i) {
      this.i = i;
    }

    public int intValue() {
      return i;
    }
    
    static XynaPropertyBuilds.Builder<TestInt> builder = new XynaPropertyBuilds.Builder<TestInt>() {

      public TestInt fromString(String string)
          throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
        return new TestInt( Integer.parseInt(string) );
      }

      public String toString(TestInt value) {
        return String.valueOf( value.i );
      }
      
      
    };
    
    public String toString() {
      return "TestInt("+i+")";
    };
    
  }
  
  
}

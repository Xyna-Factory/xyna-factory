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
package com.gip.xyna.utils.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;

import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;

import junit.framework.TestCase;


/**
 *
 */
public class SerializableEnumClassTest extends TestCase { 
  
  public enum Testenum { abc, def, ghi };
  
  public enum Testenum2 implements DocumentedEnum { 
    abc( Documentation.de("erste 3 Buchstaben des Alphabets").
                       en("first 3 letters of alphabet").
                       build() ), 
    def( Documentation.de("Abk�rzung f�r default").
                       en("abbreviation for default").
                       build() ), 
    ghi( Documentation.de("weitere drei Buchstaben").
                       en("another three letters").
                       build() ), ;
  
    private Documentation doc;

    private Testenum2( Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
    
  }

  
  public void testFirst() throws IOException, ClassNotFoundException {
    /*
    SerializableEnumClass<Testenum> sec = new SerializableEnumClass<Testenum>(Testenum.class);
    
    Assert.assertEquals(Testenum.class, sec.getEnumClass() );
    Assert.assertEquals("com.gip.xyna.utils.misc.SerializableEnumClassTest$Testenum", sec.getFqEnumClassName() );
    Assert.assertEquals("[abc, def, ghi]", sec.getEnumConstantsAsStrings().toString() );
    
    saveToFile(sec, "SerializableEnumClassTest.ser");
    */
    
    SerializableEnumClass<?> sec2 = (SerializableEnumClass<?>)readFromFile("SerializableEnumClassTest.ser");
    
    Assert.assertNull( sec2.getEnumClass() );
    Assert.assertEquals( "com.gip.xyna.utils.misc.SerializableEnumClassTest$Testenum", sec2.getFqEnumClassName() );
    Assert.assertEquals("[abc, def, ghi]", sec2.getEnumConstantsAsStrings().toString() );
    
    
  }
  

  private static void saveToFile(Object write, String filename) throws IOException {
    FileOutputStream fos = new FileOutputStream(filename);
    try {
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(write);
      oos.flush();
    } finally {
      fos.close();
    }
  }


  private static Object readFromFile(String filename) throws IOException, ClassNotFoundException {
    FileInputStream fis = new FileInputStream(filename);
    try {
      ObjectInputStream ois = new ObjectInputStream(fis);
      return ois.readObject();
    } finally {
      fis.close();
    }
  }

  
  
}

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
package com.gip.xyna.utils.collections.lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.collections.lists.StringSerializableList.AutoSeparatorCharSerializeAlgorithm;
import com.gip.xyna.utils.collections.lists.StringSerializableList.CSVSerializeAlgorithm;
import com.gip.xyna.utils.collections.lists.StringSerializableList.SeparatorSerializeAlgorithm;

import junit.framework.TestCase;

public class StringSerializableListTest extends TestCase {
  
  public final static List<String> EMPTY = Collections.emptyList();
  public final static List<String> ABC = Arrays.asList("Aa", "Bb", "Cc" );
  public final static List<Integer> NUMBERS = Arrays.asList(1, 2, 3 );
  
  public void testSeparatorSerializeAlgorithm() {
    SeparatorSerializeAlgorithm sa = new SeparatorSerializeAlgorithm();
    assertEquals("", sa.serialize(EMPTY) );
    assertEquals("[]", sa.deserialize("").toString() );
    assertEquals("Aa, Bb, Cc", sa.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa.deserialize("Aa, Bb, Cc").toString() );
    
    SeparatorSerializeAlgorithm sa2 = new SeparatorSerializeAlgorithm(".");
    assertEquals("Aa.Bb.Cc", sa2.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa2.deserialize("Aa.Bb.Cc").toString() );
    

  }
  
  public void testAutoSeparatorCharSerializeAlgorithm() {
    AutoSeparatorCharSerializeAlgorithm sa = new AutoSeparatorCharSerializeAlgorithm();
    assertEquals("", sa.serialize(EMPTY) );
    assertEquals("[]", sa.deserialize("").toString() );
    assertEquals("Aa:Bb:Cc:", sa.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa.deserialize("Aa:Bb:Cc:").toString() );
    
    AutoSeparatorCharSerializeAlgorithm sa2 = new AutoSeparatorCharSerializeAlgorithm(".");
    assertEquals("Aa.Bb.Cc.", sa2.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa2.deserialize("Aa.Bb.Cc.").toString() );
    sa2 = new AutoSeparatorCharSerializeAlgorithm("abcd");
    assertEquals("AadBbdCcd", sa2.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa2.deserialize("AadBbdCcd").toString() );
    
    AutoSeparatorCharSerializeAlgorithm sa3 = new AutoSeparatorCharSerializeAlgorithm("+", ':');
    assertEquals("Aa+Bb+Cc+", sa3.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa3.deserialize("Aa:Bb:Cc").toString() );
    
    AutoSeparatorCharSerializeAlgorithm sa4 = new AutoSeparatorCharSerializeAlgorithm();
    assertEquals("A:a|B:b|C:c|", sa4.serialize( Arrays.asList("A:a", "B:b", "C:c" ) ) );
    assertEquals("[A:a, B:b, C:c]", sa4.deserialize("A:a|B:b|C:c|").toString() );
    
  }
  
  public void testCSVSerializeAlgorithm() {
    CSVSerializeAlgorithm sa = new CSVSerializeAlgorithm();
    assertEquals("", sa.serialize(EMPTY) );
    assertEquals("[]", sa.deserialize("").toString() );
    assertEquals("Aa,Bb,Cc", sa.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa.deserialize("Aa,Bb,Cc").toString() );
    
    CSVSerializeAlgorithm sa2 = new CSVSerializeAlgorithm(";", "\"");
    assertEquals("Aa;Bb;Cc", sa2.serialize(ABC) );
    assertEquals("[Aa, Bb, Cc]", sa2.deserialize("Aa;Bb;Cc").toString() );
    
    CSVSerializeAlgorithm sa3 = new CSVSerializeAlgorithm();
    assertEquals("\"A,a\",Bb,\"C\"\"c\"", sa3.serialize(Arrays.asList("A,a", "Bb", "C\"c") ) );
    assertEquals("[A,a, Bb, C\"c]", sa3.deserialize("\"A,a\",Bb,\"C\"\"c\"").toString() );
 }
  

  public void testStringSerialiableList_Integer() {
    StringSerializableList<Integer> ssl = new StringSerializableList<Integer>(Integer.class, NUMBERS, new CSVSerializeAlgorithm());
    
    String serial = ssl.serializeToString();
    assertEquals("1,2,3", serial);
  }
  
}


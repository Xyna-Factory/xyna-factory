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
package com.gip.xyna.xsor.indices;

import static junit.framework.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Test;

import com.gip.xyna.xsor.indices.UniqueKeyValueMappingIndex;
import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndex;
import com.gip.xyna.xsor.indices.helper.TestObject;
import com.gip.xyna.xsor.indices.management.BasicIndexFactory;
import com.gip.xyna.xsor.indices.management.IndexFactory;



public class TestXCPayloadPrimaryKeyIndex {
  
  private static final int BYTE_ARRAY_LENGTH = 16;
  private static final Random RANDOMIZER = new Random();
  private static final IndexFactory indexFactory = new BasicIndexFactory();
    
  
  @Test
  public void testFind() {
    final int INSERTS = 1000;
    
    AtomicInteger idGenerator = new AtomicInteger(0);
    Map<Integer, TestObject> storage = new HashMap<Integer, TestObject>();
    List<byte[]> generatedPks = new ArrayList<byte[]>();

    XSORPayloadPrimaryKeyIndex pkIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    
    for (int i=0; i < INSERTS; i++) {
      TestObject testObject = new TestObject(generateRandomButUniquePK(generatedPks),idGenerator);
      assertTrue("Generators should have provided unique ids!", pkIndex.put(testObject.getPrimaryKey(), testObject.getObjectIndex()));
      storage.put(testObject.getObjectIndex(), testObject);
    }
    
    int randomId = RANDOMIZER.nextInt(storage.size());
    TestObject randomObject = storage.get(randomId);
    int[] internalIdsForRandomObject = pkIndex.get(randomObject.getPrimaryKey());
    
    assertEquals("UniqueIndex should only return  a single value", 1, internalIdsForRandomObject.length);
    assertEquals(randomObject.getObjectIndex(), internalIdsForRandomObject[0]);
  }
  
  
  @Test
  public void testUniqueKeyConstraintViolation() {
    final int INSERTS = 1000;
    
    AtomicInteger idGenerator = new AtomicInteger(0);
    Map<Integer, TestObject> storage = new HashMap<Integer, TestObject>();
    List<byte[]> generatedPks = new ArrayList<byte[]>();

    XSORPayloadPrimaryKeyIndex pkIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    
    for (int i=0; i < INSERTS; i++) {
      TestObject testObject = new TestObject(generateRandomButUniquePK(generatedPks),idGenerator);
      assertTrue("Generators should have provided unique ids!", pkIndex.put(testObject.getPrimaryKey(), testObject.getObjectIndex()));
      storage.put(testObject.getObjectIndex(), testObject);
    }
    
    int randomId = RANDOMIZER.nextInt(storage.size());
    TestObject randomObject = storage.get(randomId);
    
    TestObject violatingObject = new TestObject(randomObject.getPrimaryKey(), randomObject.getObjectIndex());
    assertFalse(pkIndex.put(violatingObject.getPrimaryKey(), violatingObject.getObjectIndex()));
    violatingObject = new TestObject(randomObject.getPrimaryKey(), idGenerator);
    assertFalse(pkIndex.put(violatingObject.getPrimaryKey(), violatingObject.getObjectIndex()));
    TestObject nonViolatingObjectWithSameIndex = new TestObject(generateRandomButUniquePK(generatedPks), randomObject.getObjectIndex());
    assertTrue(pkIndex.put(nonViolatingObjectWithSameIndex.getPrimaryKey(), nonViolatingObjectWithSameIndex.getObjectIndex()));
  }
  
  
  @Test
  public void testDeletion() {
    final int INSERTS = 1000;
    final int DELETIONS = 500;
    
    int succesfullDeletions = 0;
    AtomicInteger idGenerator = new AtomicInteger(0);
    Map<Integer, TestObject> storage = new HashMap<Integer, TestObject>();
    List<byte[]> generatedPks = new ArrayList<byte[]>();

    XSORPayloadPrimaryKeyIndex pkIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    
    for (int i=0; i < INSERTS; i++) {
      TestObject testObject = new TestObject(generateRandomButUniquePK(generatedPks),idGenerator);
      assertTrue("Generators should have provided unique ids!", pkIndex.put(testObject.getPrimaryKey(), testObject.getObjectIndex()));
      storage.put(testObject.getObjectIndex(), testObject);
    }
    
    assertEquals(INSERTS-succesfullDeletions, pkIndex.keySet().size());
    
    int randomId = RANDOMIZER.nextInt(storage.size());
    TestObject randomObject = storage.get(randomId);
    
     
    pkIndex.delete(randomObject.getPrimaryKey(), randomObject.getObjectIndex());
    storage.remove(randomObject.getObjectIndex());
    succesfullDeletions++;
    assertEquals(INSERTS-succesfullDeletions, pkIndex.keySet().size());
    
    TestObject objectWithDifferentIndex = new TestObject(randomObject.getPrimaryKey(), idGenerator);
    try {
      pkIndex.delete(objectWithDifferentIndex.getPrimaryKey(), objectWithDifferentIndex.getObjectIndex());
      fail();
    } catch (Throwable t) { /* expected*/ }
    assertEquals(INSERTS-succesfullDeletions, pkIndex.keySet().size());
    
    TestObject objectWithDifferentKey = new TestObject(generateRandomButUniquePK(generatedPks), randomObject.getObjectIndex());
    try {
      pkIndex.delete(objectWithDifferentKey.getPrimaryKey(), objectWithDifferentKey.getObjectIndex());
      fail();
    } catch (Throwable t) { /* expected*/ }
    assertEquals(INSERTS-succesfullDeletions, pkIndex.keySet().size());
    
    for (int i=0; i < DELETIONS; i++) {
      randomId = RANDOMIZER.nextInt(storage.size());
      randomObject = (TestObject) new ArrayList<TestObject>(storage.values()).get(randomId);
      pkIndex.delete(randomObject.getPrimaryKey(), randomObject.getObjectIndex());
      succesfullDeletions++;
      assertEquals(INSERTS-succesfullDeletions, pkIndex.keySet().size());
      storage.remove(randomObject.getObjectIndex());
    }
  }
  
  
  @Test
  public void testUniqueMappingRetrievalAndReturnValue() {
    XSORPayloadPrimaryKeyIndex pkIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    
    AtomicInteger idGenerator = new AtomicInteger(0);
    Map<Integer, TestObject> storage = new HashMap<Integer, TestObject>();
    List<byte[]> generatedPks = new ArrayList<byte[]>();
    
    TestObject testObject = new TestObject(generateRandomButUniquePK(generatedPks), idGenerator);
    
    int internalId = pkIndex.getUniqueValueForKey(testObject.getPrimaryKey());
    assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", UniqueKeyValueMappingIndex.NO_VALUE, internalId);
    
    pkIndex.put(testObject.getPrimaryKey(), testObject.getObjectIndex());
    internalId = pkIndex.getUniqueValueForKey(testObject.getPrimaryKey());
    assertEquals("The internal id should now have been found", testObject.getObjectIndex(), internalId);
    
    pkIndex.delete(testObject.getPrimaryKey(), testObject.getObjectIndex());
    internalId = pkIndex.getUniqueValueForKey(testObject.getPrimaryKey());
    assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", UniqueKeyValueMappingIndex.NO_VALUE, internalId);
  }
  
  
  @Test
  public void testReplace() {
    XSORPayloadPrimaryKeyIndex pkIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    
    AtomicInteger idGenerator = new AtomicInteger(0);
    List<byte[]> generatedPks = new ArrayList<byte[]>();
    
    TestObject testObject = new TestObject(generateRandomButUniquePK(generatedPks), idGenerator);
    
    int internalId = pkIndex.getUniqueValueForKey(testObject.getPrimaryKey());
    assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", UniqueKeyValueMappingIndex.NO_VALUE, internalId);
    
    assertTrue(pkIndex.put(testObject.getPrimaryKey(), testObject.getObjectIndex()));
    internalId = pkIndex.getUniqueValueForKey(testObject.getPrimaryKey());
    assertEquals("The internal id should now have been found", testObject.getObjectIndex(), internalId);
    
    TestObject updatedTestObject = new TestObject(testObject.getPrimaryKey(), idGenerator);
    
    assertFalse(pkIndex.put(updatedTestObject.getPrimaryKey(), updatedTestObject.getObjectIndex()));
    assertEquals(testObject.getObjectIndex(), pkIndex.replace(updatedTestObject.getPrimaryKey(), updatedTestObject.getObjectIndex(), testObject.getObjectIndex()));
    
    internalId = pkIndex.getUniqueValueForKey(testObject.getPrimaryKey());
    assertEquals(updatedTestObject.getObjectIndex(), internalId);
    
    TestObject otherTestObject = new TestObject(generateRandomButUniquePK(generatedPks), idGenerator);
    internalId = pkIndex.getUniqueValueForKey(otherTestObject.getPrimaryKey());
    assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", UniqueKeyValueMappingIndex.NO_VALUE, internalId);
    
    assertEquals(UniqueKeyValueMappingIndex.NO_VALUE, pkIndex.replace(otherTestObject.getPrimaryKey(), otherTestObject.getObjectIndex(), updatedTestObject.getObjectIndex()));
    
    internalId = pkIndex.getUniqueValueForKey(otherTestObject.getPrimaryKey());
    assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", otherTestObject.getObjectIndex(), internalId);
    
  }
  
  
  @Test
  public void testGetValues() {
    final int INSERTS = 1000;
    
    AtomicInteger idGenerator = new AtomicInteger(0);
    Map<Integer, TestObject> storage = new HashMap<Integer, TestObject>();
    List<byte[]> generatedPks = new ArrayList<byte[]>();

    XSORPayloadPrimaryKeyIndex pkIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    
    for (int i=0; i < INSERTS; i++) {
      TestObject testObject = new TestObject(generateRandomButUniquePK(generatedPks),idGenerator);
      assertTrue("Generators should have provided unique ids!", pkIndex.put(testObject.getPrimaryKey(), testObject.getObjectIndex()));
      storage.put(testObject.getObjectIndex(), testObject);
    }
    
    int[] internalIds = pkIndex.values();
    assertEquals(storage.size(), internalIds.length);
  }
  
  
  private byte[] generateRandomButUniquePK(List<byte[]> generatedKeys) {
    byte[] bytes = new byte[BYTE_ARRAY_LENGTH];
    RANDOMIZER.nextBytes(bytes);
    while (generatedKeys.contains(bytes)) {
      RANDOMIZER.nextBytes(bytes);
    }
    generatedKeys.add(bytes);
    return bytes;
  }

}

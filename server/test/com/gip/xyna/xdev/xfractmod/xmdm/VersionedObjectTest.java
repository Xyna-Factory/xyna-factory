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
package com.gip.xyna.xdev.xfractmod.xmdm;

import java.util.concurrent.atomic.AtomicLong;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject;

import junit.framework.TestCase;

public class VersionedObjectTest extends TestCase {
  
  public void setUp() {
    final AtomicLong longGenerator = new AtomicLong(100);
    XOUtils.idgen = EasyMock.createMock(IDGenerator.class);
    EasyMock.expect(XOUtils.idgen.getUniqueId("objectversion")).andAnswer(new IAnswer<Long>() {

      public Long answer() throws Throwable {
        return longGenerator.getAndAdd(100);
      }
    }).anyTimes();
    EasyMock.replay(XOUtils.idgen);
  }
  
  public void testElementCount() {
    VersionedObject<String> vo = new VersionedObject<String>();
    vo.add("asd");
    vo.add("asd2");
    assertEquals(2, vo.getAllVersions().size());
  }
  
  public void testVersions() {
    VersionedObject<String> vo = new VersionedObject<String>();
    vo.add("asd");
    vo.add("asd2");
    assertEquals("asd", vo.getAllVersions().get(0).object);
    assertEquals("asd2", vo.getAllVersions().get(1).object);
  }
  
  public void testSearchByVersion() {
    VersionedObject<String> vo = new VersionedObject<String>();
    vo.add("asd"); //100
    vo.add("asd2"); //200
    assertEquals("asd", vo.getVersion(10).object);
    assertEquals("asd", vo.getVersion(99).object);
    assertEquals("asd2", vo.getVersion(100).object);
    assertEquals("asd2", vo.getVersion(101).object);
    assertEquals("asd2", vo.getVersion(199).object);
    assertNull(vo.getVersion(200));
    assertNull(vo.getVersion(201));
  }
  
  public void testSearchByVersionRange() {
    VersionedObject<String> vo = new VersionedObject<String>();
    vo.add("asd"); //100
    vo.add("asd2"); //200
    vo.add("asd3"); //300
    assertEqualVersions(vo, 0, 0, "asd");
    assertEqualVersions(vo, 0, 1, "asd");
    assertEqualVersions(vo, 0, 99, "asd");
    assertEqualVersions(vo, 0, 100, "asd", "asd2");
    assertEqualVersions(vo, 0, 199, "asd", "asd2");
    assertEqualVersions(vo, 0, 200, "asd", "asd2", "asd3");
    assertEqualVersions(vo, 0, 400, "asd", "asd2", "asd3");
    assertEqualVersions(vo, 99, 100, "asd", "asd2");
    assertEqualVersions(vo, 100, 100, "asd2");
    assertEqualVersions(vo, 100, 101, "asd2");
    assertEqualVersions(vo, 100, 199, "asd2");
    assertEqualVersions(vo, 100, 200, "asd2", "asd3");
    assertEqualVersions(vo, 200, 200, "asd3");
    assertEqualVersions(vo, 299, 299, "asd3");
    assertEqualVersions(vo, 136, 255, "asd2", "asd3");
    assertEqualVersions(vo, 300, 300);
    assertEqualVersions(vo, 700, 900);
    try {
      assertEqualVersions(vo, 500, 100);
      fail();
    } catch (NegativeArraySizeException e) {
      
    }
  }

  private void assertEqualVersions(VersionedObject<String> vo ,int start, int end, String ... expectedValues) {
    Version<String>[] versions = vo.getVersions(start, end);
    assertEquals(expectedValues.length, versions.length);
    for (int i = 0; i<expectedValues.length; i++) {
      assertEquals(expectedValues[i], versions[i].object);
    }
  }

}

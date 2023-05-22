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
package com.gip.xyna.xdev.xfractmod.xmdm;



import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject;

import junit.framework.TestCase;



public class XOUtilsTest extends TestCase {

  private String current;
  private VersionedObject<String> oldVersions;


  public void setUp() {
    final AtomicLong longGenerator = new AtomicLong(100);
    XOUtils.idgen = EasyMock.createMock(IDGenerator.class);
    EasyMock.expect(XOUtils.idgen.getUniqueId("objectversion")).andAnswer(new IAnswer<Long>() {

      public Long answer() throws Throwable {
        return longGenerator.getAndAdd(100);
      }
    }).anyTimes();
    EasyMock.replay(XOUtils.idgen);


    current = "current";
    oldVersions = new VersionedObject<>();
    oldVersions.add("v100");
    oldVersions.add("v200");
    oldVersions.add("v300");
    longGenerator.set(301);
    oldVersions.add("v301");
    longGenerator.set(302);
    oldVersions.add("v302");
    longGenerator.set(303);
    oldVersions.add("v303");
    longGenerator.set(400);
    oldVersions.add("v400");
  }


  public void test1() {
    assertThatIntervalsFoundAre(1, 1, new String[] {"1-1-v100"}, new int[] {});
    assertThatIntervalsFoundAre(2, 2, new String[] {"2-2-v100"}, new int[] {});
    assertThatIntervalsFoundAre(99, 99, new String[] {"99-99-v100"}, new int[] {});
    assertThatIntervalsFoundAre(100, 100, new String[] {"100-100-v200"}, new int[] {100});
    assertThatIntervalsFoundAre(101, 101, new String[] {"101-101-v200"}, new int[] {});
    assertThatIntervalsFoundAre(1, 4, new String[] {"1-4-v100"}, new int[] {});
    assertThatIntervalsFoundAre(1, 99, new String[] {"1-99-v100"}, new int[] {});
    assertThatIntervalsFoundAre(98, 99, new String[] {"98-99-v100"}, new int[] {});
    assertThatIntervalsFoundAre(98, 100, new String[] {"98-99-v100", "100-100-v200"}, new int[] {100});
    assertThatIntervalsFoundAre(5, 100, new String[] {"5-99-v100", "100-100-v200"}, new int[] {100});
    assertThatIntervalsFoundAre(5, 101, new String[] {"5-99-v100", "100-101-v200"}, new int[] {100});
    assertThatIntervalsFoundAre(5, 103, new String[] {"5-99-v100", "100-103-v200"}, new int[] {100});
    assertThatIntervalsFoundAre(5, 199, new String[] {"5-99-v100", "100-199-v200"}, new int[] {100});
    assertThatIntervalsFoundAre(5, 200, new String[] {"5-99-v100", "100-199-v200", "200-200-v300"}, new int[] {100, 200});
    assertThatIntervalsFoundAre(5, 201, new String[] {"5-99-v100", "100-199-v200", "200-201-v300"}, new int[] {100, 200});
    assertThatIntervalsFoundAre(99, 201, new String[] {"99-99-v100", "100-199-v200", "200-201-v300"}, new int[] {100, 200});
    assertThatIntervalsFoundAre(100, 201, new String[] {"100-199-v200", "200-201-v300"}, new int[] {100, 200});
    assertThatIntervalsFoundAre(101, 201, new String[] {"101-199-v200", "200-201-v300"}, new int[] {200});
    assertThatIntervalsFoundAre(102, 205, new String[] {"102-199-v200", "200-205-v300"}, new int[] {200});
    assertThatIntervalsFoundAre(299, 302, new String[] {"299-299-v300", "300-300-v301", "301-301-v302", "302-302-v303"},
                                new int[] {300, 301, 302});
    assertThatIntervalsFoundAre(299, 303, new String[] {"299-299-v300", "300-300-v301", "301-301-v302", "302-302-v303", "303-303-v400"},
                                new int[] {300, 301, 302, 303});
    assertThatIntervalsFoundAre(300, 300, new String[] {"300-300-v301"}, new int[] {300});
    assertThatIntervalsFoundAre(300, 301, new String[] {"300-300-v301", "301-301-v302"}, new int[] {300, 301});
    assertThatIntervalsFoundAre(301, 302, new String[] {"301-301-v302", "302-302-v303"}, new int[] {301, 302});
    assertThatIntervalsFoundAre(301, 303, new String[] {"301-301-v302", "302-302-v303", "303-303-v400"}, new int[] {301, 302, 303});
    assertThatIntervalsFoundAre(301, 304, new String[] {"301-301-v302", "302-302-v303", "303-304-v400"}, new int[] {301, 302, 303});
    assertThatIntervalsFoundAre(101, 344, new String[] {"101-199-v200", "200-299-v300", "300-300-v301", "301-301-v302", "302-302-v303",
        "303-344-v400"}, new int[] {200, 300, 301, 302, 303});
    assertThatIntervalsFoundAre(400, 400, new String[] {"400-400-current"}, new int[] {400});
    assertThatIntervalsFoundAre(400, 401, new String[] {"400-401-current"}, new int[] {400});
    assertThatIntervalsFoundAre(399, 401, new String[] {"399-399-v400", "400-401-current"}, new int[] {400});
    assertThatIntervalsFoundAre(4500, 4505, new String[] {"4500-4505-current"}, new int[] {});
  }


  private void assertThatIntervalsFoundAre(int rev1, int rev2, String[] expectedValues, int[] expectedChanges) {
    Set<Long> changes = new HashSet<>();
    List<Triple<String, Long, Long>> intervalsOfAllVersions = XOUtils.getIntervalsOfAllVersions(current, oldVersions, rev1, rev2, changes);
    Set<Long> expectedChangesSet = new HashSet<>();
    for (int i : expectedChanges) {
      expectedChangesSet.add((long) i);
    }
    assertEquals(expectedChangesSet, changes);
    assertEquals(expectedValues.length, intervalsOfAllVersions.size());
    for (int i = 0; i < expectedValues.length; i++) {
      assertThatIntervalIs(intervalsOfAllVersions.get(i), expectedValues[i]);
    }
  }


  private void assertThatIntervalIs(Triple<String, Long, Long> triple, String expected) {
    String[] parts = expected.split("-");
    assertEquals(Long.valueOf(parts[0]), triple.getSecond());
    assertEquals(Long.valueOf(parts[1]), triple.getThird());
    assertEquals(parts[2], triple.getFirst());
  }

}

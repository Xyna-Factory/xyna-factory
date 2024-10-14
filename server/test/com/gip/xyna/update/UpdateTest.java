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
package com.gip.xyna.update;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;


public class UpdateTest extends TestCase {

  public void testVersionComparison() {
    Version v1 = new Version("a.b.c.3");
    Version v2 = new Version("a.b.c.4");
    Version v3 = new Version("a0.b.c.3");
    Version v4 = new Version("a1.b.c.3");
    Version v5 = new Version("a.b.b.4");
    assertTrue(v2.isStrictlyGreaterThan(v1));
    assertTrue(v3.isStrictlyGreaterThan(v1));
    assertTrue(v4.isStrictlyGreaterThan(v2));
    assertTrue(v1.isStrictlyGreaterThan(v5));
    assertFalse(v1.isEqualOrGreaterThan(v2));
    assertFalse(v1.isEqualOrGreaterThan(v3));
    assertFalse(v1.isEqualOrGreaterThan(v4));
    assertFalse(v5.isEqualOrGreaterThan(v1));
  }
  
  public void testUpdateAllowedRanges() throws Exception {
    final AtomicBoolean didUpdate = new AtomicBoolean(false);
    final Version before = new Version("1.2.alpha3.test4");
    final Version target = new Version("2.3.beta4.5");
    Update u1 = new Update() {

      @Override
      protected Version getAllowedVersionForUpdate() {
        return before;
      }

      @Override
      protected Version getVersionAfterUpdate() throws XynaException {
        return target;
      }

      @Override
      public boolean mustUpdateGeneratedClasses() {
        return false;
      }

      @Override
      protected void update() throws XynaException {
        didUpdate.set(true);
      }
      
    };
    Version current = new Version(before).increaseToNextMajorVersion(4);
    assertEquals(new Version("1.2.alpha3.test5"), current);
    Version after = u1.update(current);
    assertFalse(didUpdate.get());    
    assertEquals(current, after);
    
    //test mit automatischer branch-range (1.2.alpha3.testx, x>=4)
    u1.addFollowingBranchVersionsAsAllowedForUpdate(4);
    after = u1.update(current);
    assertTrue(didUpdate.get());
    assertEquals(target, after);
    
    //test mit zwei manuell gesetzten ranges.
    didUpdate.set(false);
    u1.addAllowedVersionRangeForUpdate(new Version("1.3.alpha7.k10"), new Version("2.1.beta4.5555"));
    u1.addAllowedVersionRangeForUpdate(new Version("141.3.alpha70.1"), new Version("141.3.alpha700.4"));

    //updates sollten nicht funktionieren für folgende "currentVersions":
    Version[] notInRange =
        new Version[] {new Version("1.3.alpha6.r1"), new Version("1.2.1.1"), new Version("a.4.4.4"),
            new Version("1.3.alpha7.k9"), new Version("2.1.beta4.55554"), new Version("2.1.beta30.14"),
            new Version("2.10.beta4.5555"), new Version("a.10.beta4.5555"), new Version("141.3.alpha7.K200"),
            new Version("141.3.alpha6000.4"), new Version("141.3.alphaa.3"), new Version("141.2.alpha99.XXXXXX"),
            new Version("2.a.0.0")};

    for (Version vNotInRange : notInRange) {
      after = u1.update(vNotInRange);
      assertEquals(vNotInRange, after);
      assertFalse(didUpdate.get());
    }
    
    //folgende versionen sind in range
    Version[] inRange =
        new Version[] {new Version("141.3.alpha99.XXXXXX"), new Version("1.200.9.9"), new Version("2.0.X.9"),
            new Version("1.3.alpha7.k100"), new Version("2.1.beta4.556"), new Version("2.1.alpha1.1"),
            new Version("141.3.alpha699.R4R"), new Version("141.3.alpha70.K3")};
    
    for (Version vInRange : inRange) {
      after = u1.update(vInRange);
      assertEquals(target, after);
      assertTrue(didUpdate.get());
      didUpdate.set(false);
    }
  }

}

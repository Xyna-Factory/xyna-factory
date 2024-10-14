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

package com.gip.xyna.utils.snmp;

import junit.framework.TestCase;


public class OIDTest extends TestCase {

  public void testEncodeDecode() {
    String someString = "bla";
    OID oid = OID.encodeFromString(someString);
    assertEquals(".3.98.108.97", oid.getOid());
    assertEquals(someString, oid.decodeToString());
  }
  
  public void testDifferentConstructors() {
    OID oid = new OID(new int[]{153, 34111, 4});
    OID oid2 = new OID(new String[]{"153", "34111", "4"});
    OID oid3 = new OID(".153.34111.4");
    assertEquals(oid.toString(), oid2.toString());
    assertEquals(oid.toString(), oid3.toString());
    assertTrue(oid.startsWith(oid2));
    assertTrue(oid2.startsWith(oid));
    assertTrue(oid3.startsWith(oid));
    assertTrue(oid.startsWith(oid3));
    assertTrue(oid3.startsWith(oid2));
    assertTrue(oid2.startsWith(oid3));
  }
  
}

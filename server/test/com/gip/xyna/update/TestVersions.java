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


package com.gip.xyna.update;

import static org.junit.Assert.*;

import org.junit.Test;
import org.apache.log4j.Logger;


public class TestVersions {

  private static Logger _logger = Logger.getLogger(TestVersions.class);
  
  @Test
  public void test1() {
    //Version v0 = new Version(-2, -2, -2, -2);
    //Version v0 = new Version(0, 0, 0, 0);
    Version v0 = new Version("0");
    Version v1 = new Version("0.5.5.5");
    Version v2 = new Version("1.2.3.4");
    Version v3 = new Version("1.2.5.4");
    Version v4 = new Version("1.4.0.1");
    assertTrue(v1.isStrictlyGreaterThan(v0));
    assertTrue(v2.isStrictlyGreaterThan(v1));
    assertTrue(v3.isStrictlyGreaterThan(v2));
    assertTrue(v4.isStrictlyGreaterThan(v3));
  }

}

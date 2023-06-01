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
package com.gip.xyna.xfmg;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBaseTest;
import com.gip.xyna.xfmg.xfctrl.classloading.FilterClassLoaderTest;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoaderTest;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibClassLoaderTest;
import com.gip.xyna.xfmg.xfctrl.classloading.TriggerClassLoaderTest;
import com.gip.xyna.xfmg.xfctrl.classloading.WFClassLoaderTest;


public class AllTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for Xyna Factory Management");
    //$JUnit-BEGIN$
    suite.addTestSuite(ClassLoaderBaseTest.class);
    suite.addTestSuite(FilterClassLoaderTest.class);
    suite.addTestSuite(MDMClassLoaderTest.class);
    suite.addTestSuite(SharedLibClassLoaderTest.class);
    suite.addTestSuite(TriggerClassLoaderTest.class);
    suite.addTestSuite(WFClassLoaderTest.class);
    //$JUnit-END$
    return suite;
  }

}

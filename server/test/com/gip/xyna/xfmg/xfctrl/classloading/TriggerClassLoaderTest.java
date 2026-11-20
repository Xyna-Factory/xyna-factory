/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xfmg.xfctrl.classloading;



import java.io.IOException;
import java.util.HashMap;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;



public class TriggerClassLoaderTest extends TestCase {


  public void testDelegateClassloadingToParent() throws XynaException, ClassNotFoundException, IOException {

    final String requestedClassName = "test.ParentProvidedClass";
    final Class<?> expectedClass = HashMap.class;

    SharedLibClassLoader sharedLibCl =
        new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET) {

          @Override
          protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (requestedClassName.equals(name)) {
              return expectedClass;
            }
            return super.findClass(name);
          }
        };

    String triggerClassName = "triggerClassName";
    try (TriggerClassLoader triggerCl = new TriggerClassLoader(triggerClassName, new SharedLibClassLoader[] {sharedLibCl}, new String[0],
                                                               VersionManagement.REVISION_WORKINGSET)) {
      assertSame("Classes differ", expectedClass, triggerCl.loadClass(requestedClassName));
    }

  }

}

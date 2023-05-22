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

package com.gip.xyna.xfmg.xfctrl.classloading;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;


public class TriggerClassLoaderTest extends TestCase {


  public void testDelegateClassloadingToParent() throws XynaException, MalformedURLException, ClassNotFoundException {

    String oldSharedLibDir = Constants.SHAREDLIB_BASEDIR;
    Constants.SHAREDLIB_BASEDIR = "";

    try {

      String sharedLibName = null;

      File f = new File(Constants.SHAREDLIB_BASEDIR + ".");
      String[] files = f.list();
      for (String file : files) {
        if (new File(file).isDirectory()) {
          sharedLibName = file;
          break;
        }
      }

      if (sharedLibName == null) {
        fail("The test requires the existence of an arbitrary subdirectory of '.'");
      }

      final String sharedLibNameFinal = sharedLibName;

      final Class<?> xyzClass = HashMap.class;

      SharedLibClassLoader sharedLibCl = new SharedLibClassLoader(sharedLibName, VersionManagement.REVISION_WORKINGSET) {

        protected Class<?> findClass(final String name) throws ClassNotFoundException {
          if (name.equals(sharedLibNameFinal))
            return xyzClass;
          return super.findClass(sharedLibNameFinal);
        }

      };

      String triggerClassName = "triggerClassName";
      TriggerClassLoader triggerCl = new TriggerClassLoader(triggerClassName, new SharedLibClassLoader[] {sharedLibCl},
                                                            new String[0], VersionManagement.REVISION_WORKINGSET);

      // Verify that the classloading is correctly delegated to the parents
      assertEquals("Classes differ", xyzClass, triggerCl.loadClass(sharedLibName));

    }
    finally {
      Constants.SHAREDLIB_BASEDIR = oldSharedLibDir;
    }

  }

}

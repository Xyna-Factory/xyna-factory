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

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;


public class SharedLibClassLoaderTest extends TestCase {

  String oldSharedLibDir;
  public void setUp() {
    oldSharedLibDir = Constants.SHAREDLIB_BASEDIR;
    Constants.SHAREDLIB_BASEDIR = "";
  }

  public void tearDown() {
    Constants.SHAREDLIB_BASEDIR = oldSharedLibDir;
  }


  /**
   * Checks the correct reload dependencies when reloading a shared lib classloader: All children
   * are expected to be reloaded as well.
   * 
   * @throws XynaException
   * @throws MalformedURLException
   * @throws ClassNotFoundException 
   */
  public void testSharedLibDependencies() throws XynaException, MalformedURLException, ClassNotFoundException {

    String sharedLibName1 = null;

    File f = new File(Constants.SHAREDLIB_BASEDIR + ".");
    String[] files = f.list();
    for (String file : files) {
      if (new File(file).isDirectory()) {
        sharedLibName1 = file;
        break;
      }
    }

    if (sharedLibName1 == null) {
      fail("The test requires the existence of an arbitrary subdirectory of '.'");
    }

    final String sharedLibName = sharedLibName1;

    final Class<?> xyzClass = HashMap.class;

    SharedLibClassLoader sharedLibCl = new SharedLibClassLoader(sharedLibName, VersionManagement.REVISION_WORKINGSET) {

      protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (name.equals(sharedLibName))
          return xyzClass;
        throw new ClassNotFoundException("Could not find class " + name);
      }

    };

    String mdmClassName = "mdmClassName";
    MDMClassLoader mdmCl = new MDMClassLoader(mdmClassName, new SharedLibClassLoader[] {sharedLibCl}, new String[0],
                                              "", mdmClassName, VersionManagement.REVISION_WORKINGSET);

    String triggerClassName = "triggerClassName";
    TriggerClassLoader triggerCl = new TriggerClassLoader(triggerClassName, new SharedLibClassLoader[] {sharedLibCl},
                                                          new String[0], VersionManagement.REVISION_WORKINGSET);


    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);

    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.MDM, mdmClassName, VersionManagement.REVISION_WORKINGSET)).andReturn(mdmCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Trigger, triggerClassName, VersionManagement.REVISION_WORKINGSET)).andReturn(triggerCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.SharedLib, sharedLibName1, VersionManagement.REVISION_WORKINGSET)).andReturn(sharedLibCl).anyTimes();
    cld.reloadClassLoaderByType(ClassLoaderType.MDM, mdmClassName, VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().times(1);
    cld.reloadClassLoaderByType(ClassLoaderType.SharedLib, sharedLibName1, VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().times(1);
    cld.reloadClassLoaderByType(ClassLoaderType.Trigger, triggerClassName, VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().times(1);

    EasyMock.replay(cld);
    ClassLoaderDispatcherFactory.getInstance().setImpl(cld);

    // mock the xfctrl
    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getClassLoaderDispatcher()).andReturn(cld).anyTimes();

    EasyMock.replay(xfctrl);

    // mock the xfm
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();

    EasyMock.replay(xfm);

    // mock the xyna factory
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();

    EasyMock.replay(xf);
    XynaFactory.setInstance(xf);

    // Verify that the classloading is correctly delegated to the parents
    assertEquals("Classes differ", xyzClass, mdmCl.loadClass(sharedLibName));
    assertEquals("Classes differ", xyzClass, triggerCl.loadClass(sharedLibName));

    sharedLibCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });

    EasyMock.verify(cld, xf, xfm);

  }

}

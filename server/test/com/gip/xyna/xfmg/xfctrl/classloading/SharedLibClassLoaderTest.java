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

import static com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderTestUtils.expectReload;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;


public class SharedLibClassLoaderTest extends TestCase {

  /**
   * Checks that MDM and trigger loaders using a class defined by a shared library are reloaded with that library.
   * 
   * @throws XynaException
   * @throws IOException
   * @throws ClassNotFoundException 
   */
  public void testSharedLibDependencies() throws XynaException, IOException, ClassNotFoundException {

    final String className = MDMClassLoaderTestObject.class.getName();
    final byte[] classBytes;
    try (InputStream input = MDMClassLoaderTestObject.class.getResourceAsStream("MDMClassLoaderTestObject.class")) {
      assertNotNull("Missing shared-library test fixture bytecode", input);
      classBytes = input.readAllBytes();
    }

    SharedLibClassLoader sharedLibCl =
        new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET) {
      @Override
      public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!className.equals(name)) {
          return super.loadClass(name, resolve);
        }
        // Bypass the application loader's copy of the fixture.
        Class<?> loaded = findLoadedClass(name);
        if (loaded == null) {
          loaded = defineClass(name, classBytes, 0, classBytes.length);
        }
        if (resolve) {
          resolveClass(loaded);
        }
        return loaded;
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

    expectReload(cld, ClassLoaderType.MDM, mdmClassName, mdmCl);
    expectReload(cld, ClassLoaderType.SharedLib, SharedLibClassLoader.EMPTYSHAREDLIB, sharedLibCl);
    expectReload(cld, ClassLoaderType.Trigger, triggerClassName, triggerCl);

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
    assertTrue("Construction alone must not register dependencies in reduced mode",
               sharedLibCl.getClassLoadersToReloadIfThisClassLoaderIsRecreated().isEmpty());
    Class<?> loaded = mdmCl.loadClass(className);
    assertSame("The shared-library loader must define the fixture", sharedLibCl, loaded.getClassLoader());
    assertEquals(className, loaded.getName());
    assertSame("Classes differ", loaded, triggerCl.loadClass(className));

    sharedLibCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });

    EasyMock.verify(cld, xf, xfm);

  }

}

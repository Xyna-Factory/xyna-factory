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
import java.util.HashMap;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;



public class MDMClassLoaderTest extends TestCase {

  /**
   * This method tests that filters and workflows are reloaded if they use an MDM object whose classloader is told to
   * reload all dependencies
   * 
   * @throws ClassNotFoundException
   * @throws XynaException
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public void testLoadMDMandCreateDependencies() throws ClassNotFoundException, XynaException, IOException {

    final String mdmClassname = MDMClassLoaderTestObject.class.getName();
    final byte[] classBytes;
    try (InputStream input = MDMClassLoaderTestObject.class.getResourceAsStream("MDMClassLoaderTestObject.class")) {
      assertNotNull("Missing MDM test fixture bytecode", input);
      classBytes = input.readAllBytes();
    }
    String wfClassname = "wfClassName";
    String filterClassname = "filterClassname";
    String triggerClassname = "triggerClassname";

    // Hide the classpath fixture from parents so only the MDM loader can define it.
    final SharedLibClassLoader empty =
        new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET) {

          @Override
          public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (mdmClassname.equals(name)) {
              throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
          }
        };
    final WFClassLoader wfCl = new WFClassLoader(wfClassname, "", wfClassname, VersionManagement.REVISION_WORKINGSET) {

      @Override
      protected ClassLoaderBase[] getParents() {
        return new ClassLoaderBase[] {empty};
      }
    };
    final TriggerClassLoader triggerCl = new TriggerClassLoader(triggerClassname, new SharedLibClassLoader[] {empty}, new String[0], VersionManagement.REVISION_WORKINGSET);
    final FilterClassLoader filterCl = new FilterClassLoader(filterClassname, triggerCl, new SharedLibClassLoader[] {empty}, triggerClassname, VersionManagement.REVISION_WORKINGSET);
    final MDMClassLoader mdmCl = new MDMClassLoader(mdmClassname, new SharedLibClassLoader[] {empty}, new String[0], "", mdmClassname, VersionManagement.REVISION_WORKINGSET) {

      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (mdmClassname.equals(name)) {
          return defineClass(name, classBytes, 0, classBytes.length);
        }
        return super.findClass(name);
      }

    };

    final Class<XynaObject> x = (Class<XynaObject>) mdmCl.loadClass(mdmClassname);
    assertSame("The MDM loader must define the fixture", mdmCl, x.getClassLoader());

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);

    EasyMock.expect(cld.loadMDMClass(mdmClassname, false, null, null, VersionManagement.REVISION_WORKINGSET)).andReturn(x).anyTimes();
    EasyMock
        .expect(cld.getMDMClassLoader(EasyMock.eq(mdmClassname), EasyMock.eq(VersionManagement.REVISION_WORKINGSET), EasyMock.anyBoolean()))
        .andReturn(mdmCl).anyTimes();
    expectReload(cld, ClassLoaderType.MDM, mdmClassname, mdmCl);
    expectReload(cld, ClassLoaderType.WF, wfClassname, wfCl);
    expectReload(cld, ClassLoaderType.Filter, filterClassname, filterCl);

    EasyMock.replay(cld);
    ClassLoaderDispatcherFactory.getInstance().setImpl(cld);

    // mock the xfmctrl
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

    Class<?> result1 = wfCl.loadClass(mdmClassname);
    assertSame("Loaded class differs from the initial one", x, result1);

    Class<?> result2 = filterCl.loadClass(mdmClassname);
    assertSame("Loaded class differs from the initial one", x, result2);

    mdmCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }

    });
    EasyMock.verify(xf, xfm, cld);

  }


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

    String mdmClassName = "mdmClassName";
    try (MDMClassLoader mdmCl = new MDMClassLoader(mdmClassName, new SharedLibClassLoader[] {sharedLibCl}, new String[0], "", mdmClassName,
                                                   VersionManagement.REVISION_WORKINGSET)) {
      // Verify that the classloading is correctly delegated to the parents
      assertSame(expectedClass, mdmCl.loadClass(requestedClassName));
    } catch (IOException e) {
      throw e;
    }
  }

}

/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026  Xyna GmbH, Germany
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
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;



public class WFClassLoaderTest extends TestCase {


  @SuppressWarnings("unchecked")
  public void testDelegateClassloadingToParent() throws XynaException, IOException, ClassNotFoundException {

    final String mdmClassName = MDMClassLoaderTestObject.class.getName();
    final byte[] classBytes;
    try (InputStream input = MDMClassLoaderTestObject.class.getResourceAsStream("MDMClassLoaderTestObject.class")) {
      assertNotNull("Missing MDM test fixture bytecode", input);
      classBytes = input.readAllBytes();
    }

    // Hide the classpath fixture so workflow loading must go through the dispatcher.
    final SharedLibClassLoader sharedLibCl =
        new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET) {
          @Override
          public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (mdmClassName.equals(name)) {
              throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
          }
        };

    MDMClassLoader mdmCl = new MDMClassLoader(mdmClassName, new SharedLibClassLoader[] {sharedLibCl}, new String[0],
                                              "", mdmClassName, VersionManagement.REVISION_WORKINGSET) {
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (mdmClassName.equals(name)) {
          return defineClass(name, classBytes, 0, classBytes.length);
        }
        return super.findClass(name);
      }
    };

    String wfClassName = "wfClassName";
    WFClassLoader wfCl = new WFClassLoader(wfClassName, "", wfClassName, VersionManagement.REVISION_WORKINGSET) {
      @Override
      protected ClassLoaderBase[] getParents() {
        return new ClassLoaderBase[] {sharedLibCl};
      }
    };

    Class<XynaObject> expectedClass = (Class<XynaObject>) mdmCl.loadClass(mdmClassName);
    assertSame("The MDM loader must define the fixture", mdmCl, expectedClass.getClassLoader());

    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cld.loadMDMClass(mdmClassName, false, null, null, VersionManagement.REVISION_WORKINGSET))
        .andReturn(expectedClass).times(1);
    expectReload(cld, ClassLoaderType.MDM, mdmClassName, mdmCl);
    expectReload(cld, ClassLoaderType.WF, wfClassName, wfCl);

    EasyMock.replay(cld);
    ClassLoaderDispatcherFactory.getInstance().setImpl(cld);

    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getClassLoaderDispatcher()).andReturn(cld).anyTimes();
    EasyMock.replay(xfctrl);

    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    EasyMock.replay(xfm);

    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.replay(xf);
    XynaFactory.setInstance(xf);

    assertSame("Classes differ", expectedClass, wfCl.loadClass(mdmClassName));

    // The exact reload expectations verify that class loading registered the workflow dependency.
    mdmCl.reloadDependencies(new ClassLoaderSwitcher() {
      public void switchClassLoader() throws XynaException {
      }
    });

    EasyMock.verify(cld, xfm, xf);
  }

}

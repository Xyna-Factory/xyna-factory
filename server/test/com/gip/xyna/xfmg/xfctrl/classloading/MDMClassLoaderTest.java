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
import java.util.IdentityHashMap;
import java.util.Set;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;


public class MDMClassLoaderTest extends TestCase {

  private static class MyXO extends XynaObject implements DeploymentTask {


    private static final long serialVersionUID = 1L;
    private static boolean deployed = false;

    @Override
    public XynaObject clone() {
      return null;
    }

    public Object get(String path) throws InvalidObjectPathException {
      return null;
    }

    public void set(String name, Object value) {
    }

    @Override
    public String toXml(String varName, boolean onlyContent) {
      return null;
    }

    public void onDeployment() {
      if (deployed) {
        throw new RuntimeException();
      }
      deployed = true;
    }

    public void onUndeployment() {
      if (!deployed) {
        throw new RuntimeException();
      }
      deployed = false;
    }

    public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
      return null;
    }

    public boolean supportsObjectVersioning() {
      return false;
    }

    public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                               Set<Long> datapoints) {
    }
  }
  

  /**
   * This method tests that filters and workflows are reloaded if they use an MDM object whose classloader is told to
   * reload all dependencies
   * 
   * @throws ClassNotFoundException
   * @throws XynaException
   * @throws MalformedURLException
   */
  @SuppressWarnings("unchecked")
  public void testLoadMDMandCreateDependencies() throws ClassNotFoundException, XynaException, MalformedURLException {

    MDMClassLoader mdmCl = null;
    WFClassLoader wfCl = null;
    FilterClassLoader filterCl = null;
    TriggerClassLoader triggerCl = null;

    final String mdmClassname = "mdmClassName";
    String wfClassname = "wfClassName";
    String filterClassname = "filterClassname";
    String triggerClassname = "triggerClassname";

    SharedLibClassLoader empty = new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET);
    wfCl = new WFClassLoader(wfClassname, "", wfClassname, VersionManagement.REVISION_WORKINGSET);
    triggerCl = new TriggerClassLoader(triggerClassname, new SharedLibClassLoader[] {empty}, new String[0], VersionManagement.REVISION_WORKINGSET);
    filterCl = new FilterClassLoader(filterClassname, triggerCl,
                                     new SharedLibClassLoader[] {empty}, triggerClassname, VersionManagement.REVISION_WORKINGSET);

    XynaObject xo = new MyXO();
    final Class<XynaObject> x = (Class<XynaObject>) xo.getClass();

    mdmCl = new MDMClassLoader(mdmClassname, new SharedLibClassLoader[] {empty}, new String[0], "", mdmClassname, VersionManagement.REVISION_WORKINGSET) {

      protected Class<?> findClass(String s) throws ClassNotFoundException {
        if (s.equals(mdmClassname))
          return x;
        throw new ClassNotFoundException();
      }

    };

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);

    EasyMock.expect(cld.loadMDMClass(mdmClassname, false, null, null, VersionManagement.REVISION_WORKINGSET))
                    .andReturn((Class<XynaObject>) mdmCl.loadClass(mdmClassname)).anyTimes();
    EasyMock.expect(cld.getMDMClassLoader(mdmClassname, VersionManagement.REVISION_WORKINGSET, EasyMock.isA(boolean.class))).andReturn(mdmCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.WF, wfClassname, VersionManagement.REVISION_WORKINGSET)).andReturn(wfCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.MDM, mdmClassname, VersionManagement.REVISION_WORKINGSET)).andReturn(mdmCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Filter, filterClassname, VersionManagement.REVISION_WORKINGSET)).andReturn(filterCl).anyTimes();
    cld.reloadClassLoaderByType(ClassLoaderType.MDM, mdmClassname, VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().times(1);
    cld.reloadClassLoaderByType(ClassLoaderType.WF, wfClassname, VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().times(1);
    cld.reloadClassLoaderByType(ClassLoaderType.Filter, filterClassname, VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().times(1);

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

    Class result1 = wfCl.loadClass(mdmClassname);
    assertEquals("Loaded class differs from the initial one", x, result1);

    Class result2 = filterCl.loadClass(mdmClassname);
    assertEquals("Loaded class differs from the initial one", x, result2);

    mdmCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });
    EasyMock.verify(xf, xfm, cld);

  }


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

      String mdmClassName = "mdmClassName";
      MDMClassLoader mdmCl = new MDMClassLoader(mdmClassName, new SharedLibClassLoader[] {sharedLibCl}, new String[0],
                                                "", mdmClassName, VersionManagement.REVISION_WORKINGSET);

      // Verify that the classloading is correctly delegated to the parents
      assertEquals("Classes differ", xyzClass, mdmCl.loadClass(sharedLibName));

    }
    finally {
      Constants.SHAREDLIB_BASEDIR = oldSharedLibDir;
    }

  }

}

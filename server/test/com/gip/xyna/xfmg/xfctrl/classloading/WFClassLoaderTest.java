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

package com.gip.xyna.xfmg.xfctrl.classloading;



import java.io.File;
import java.net.MalformedURLException;
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
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;



public class WFClassLoaderTest extends TestCase {


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

      XynaObject xo = new XynaObject() {

        private static final long serialVersionUID = 1L;


        @Override
        public String toXml(String varName, boolean onlyContent) {
          return null;
        }


        public void set(String name, Object value) {
        }


        public Object get(String path) throws InvalidObjectPathException {
          return null;
        }


        @Override
        public XynaObject clone() {
          return null;
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
      };

      final Class<XynaObject> xyzClass = (Class<XynaObject>) xo.getClass();

      SharedLibClassLoader sharedLibCl = new SharedLibClassLoader(sharedLibName, VersionManagement.REVISION_WORKINGSET);

      String mdmClassName = "mdmClassName";
      MDMClassLoader mdmCl = new MDMClassLoader(mdmClassName, new SharedLibClassLoader[] {sharedLibCl}, new String[0],
                                                "", mdmClassName, VersionManagement.REVISION_WORKINGSET) {

        protected Class<?> findClass(final String name) throws ClassNotFoundException {
          if (name.equals(sharedLibNameFinal))
            return xyzClass;
          return super.findClass(sharedLibNameFinal);
        }

      };

      String wfClassName = "wfClassName";
      WFClassLoader wfCl = new WFClassLoader(wfClassName, "", wfClassName, VersionManagement.REVISION_WORKINGSET);


      // Mock some classes, such as the class loader dispatcher
      ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);

      EasyMock.expect(cld.loadMDMClass(mdmClassName, false, null, null, VersionManagement.REVISION_WORKINGSET)).andReturn(xyzClass).times(1);
      EasyMock.expect(cld.getMDMClassLoader(mdmClassName, VersionManagement.REVISION_WORKINGSET, EasyMock.isA(boolean.class))).andReturn(mdmCl).times(1);
      EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.WF, wfClassName, VersionManagement.REVISION_WORKINGSET)).andReturn(wfCl).anyTimes();
      EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.MDM, mdmClassName, VersionManagement.REVISION_WORKINGSET)).andReturn(mdmCl).anyTimes();
      cld.reloadClassLoaderByType(ClassLoaderType.MDM, mdmClassName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.WF, wfClassName, VersionManagement.REVISION_WORKINGSET);
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


      // Verify that the classloading is correctly delegated to the parents
      assertEquals("Classes differ", xyzClass, wfCl.loadClass(mdmClassName));

      // Verify that a dependency has been added for the mdm ClassLoader
      mdmCl.hasBeenDeployed();
      mdmCl.reloadDependencies(new ClassLoaderSwitcher() {

        public void switchClassLoader() throws XynaException {
        }
        
      });

      EasyMock.verify(cld, xfm, xf);

    } finally {
      Constants.SHAREDLIB_BASEDIR = oldSharedLibDir;
    }

  }

}

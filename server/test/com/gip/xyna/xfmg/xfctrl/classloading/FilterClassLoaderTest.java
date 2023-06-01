/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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



import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;



public class FilterClassLoaderTest extends TestCase {

  /**
   * This method tests that filters and workflows are reloaded if they use an MDM object whose classloader is told to
   * reload all dependencies
   * 
   * @throws XynaException
   * @throws ClassNotFoundException
   */
  public void testReloadTriggerDependencies() throws XynaException, ClassNotFoundException {

    FilterClassLoader filterCl = null;
    TriggerClassLoader triggerCl = null;
    SharedLibClassLoader emptySharedLibCl = null;

    String filterClassname = "filterClassname";
    final String triggerClassname = TriggerPriv.class.getName();
    final String startParamClassname = StartParameterPriv.class.getName();
    final String triggerConnectionClassname = TriggerConnectionPriv.class.getName();

    emptySharedLibCl = new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET);
    triggerCl = new TriggerClassLoader(triggerClassname, new SharedLibClassLoader[] {emptySharedLibCl}, new String[0], VersionManagement.REVISION_WORKINGSET) {

      protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (name.equals(triggerConnectionClassname)) {
          return TriggerConnectionPriv.class;
        }
        if (name.equals(startParamClassname)) {
          return StartParameterPriv.class;
        }
        if (name.equals(triggerClassname)) {
          return TriggerPriv.class;
        }
        throw new ClassNotFoundException();
      }
    };
    filterCl = new FilterClassLoader(filterClassname, triggerCl,
                                     new SharedLibClassLoader[] {emptySharedLibCl}, triggerClassname, VersionManagement.REVISION_WORKINGSET);

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);

    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Filter, filterClassname, VersionManagement.REVISION_WORKINGSET)).andReturn(filterCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Trigger, triggerClassname, VersionManagement.REVISION_WORKINGSET)).andReturn(triggerCl).anyTimes();
    cld.reloadClassLoaderByType(ClassLoaderType.Trigger, triggerClassname, VersionManagement.REVISION_WORKINGSET);
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

    Class<?> result1 = filterCl.loadClass(triggerConnectionClassname);
    assertEquals("Classes differ", TriggerConnectionPriv.class, result1);

    triggerCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });

    EasyMock.verify(xf, xfm, cld);

  }


  /*
   * Helper classes
   */
  private class TriggerConnectionPriv extends TriggerConnection {
  }

  private class StartParameterPriv implements StartParameter {

    public StartParameter build(String... args) {
      return null;
    }

    public String[][] getParameterDescriptions() {
      return null;
    }
    
  }

  private class TriggerPriv extends EventListener<TriggerConnection, StartParameter> {

    public String getClassDescription() {
      return null;
    }

    public void onNoFilterFound(TriggerConnection con) {
    }

    public TriggerConnection receive() {
      return null;
    }

    public void start(StartParameter sourceDefinition) {
    }

    public void stop() {
    }

    @Override
    public void onProcessingRejected(String cause, TriggerConnection con) {
    }
    
  }

}

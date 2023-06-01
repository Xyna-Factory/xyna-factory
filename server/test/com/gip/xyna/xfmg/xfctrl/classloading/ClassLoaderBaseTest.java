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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.PriorityBlockingQueue;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoadingDependencySource;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;


public class ClassLoaderBaseTest extends TestCase {

  private String mdmClassLoaderName = "mdmClassLoaderName";
  private String filterClassLoaderName = "filterClassLoader";
  private String sharedLibClassLoaderName = "sharedLibClassLoader";
  private String triggerClassLoaderName = "triggerClassLoader";
  private String wfClassLoaderName = "wfClassLoader";

  private ClassLoaderBase clb;

  private MDMClassLoader mdmCl = null;
  private FilterClassLoader filterCl = null;
  private SharedLibClassLoader sharedLibCl = null;
  private TriggerClassLoader triggerCl = null;
  private WFClassLoader wfCl = null;


  public void setUp() {

    try {
      sharedLibCl = new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET);
      mdmCl = new MDMClassLoader(mdmClassLoaderName, new SharedLibClassLoader[] {sharedLibCl}, new String[0], "",
                                 mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      triggerCl = new TriggerClassLoader(triggerClassLoaderName, new SharedLibClassLoader[] {sharedLibCl},
                                         new String[0], VersionManagement.REVISION_WORKINGSET);
      filterCl = new FilterClassLoader(filterClassLoaderName, triggerCl,
                                       new SharedLibClassLoader[] {sharedLibCl}, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      wfCl = new WFClassLoader(wfClassLoaderName, "", wfClassLoaderName, VersionManagement.REVISION_WORKINGSET);
    }
    catch (XynaException e) {
      fail(e.getMessage());
    }

    // create a root class loader
    clb = new ClassLoaderBase(null, "some id", new URL[0], new ClassLoaderBase[] {XynaClassLoader.getInstance()}, VersionManagement.REVISION_WORKINGSET);

    clb.addDependencyToReloadIfThisClassLoaderIsRecreated(sharedLibClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.SharedLib, ClassLoadingDependencySource.Backup);

    sharedLibCl.addDependencyToReloadIfThisClassLoaderIsRecreated(mdmClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.MDM, ClassLoadingDependencySource.Backup);
    sharedLibCl.addDependencyToReloadIfThisClassLoaderIsRecreated(triggerClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.Trigger, ClassLoadingDependencySource.Backup);

    mdmCl.addDependencyToReloadIfThisClassLoaderIsRecreated(wfClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.WF, ClassLoadingDependencySource.Backup);
    mdmCl.addDependencyToReloadIfThisClassLoaderIsRecreated(filterClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.Filter, ClassLoadingDependencySource.Backup);

    triggerCl.addDependencyToReloadIfThisClassLoaderIsRecreated(filterClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.Filter, ClassLoadingDependencySource.Backup);

  }

  public void tearDown() {
    mdmCl = null;
    filterCl = null;
    sharedLibCl = null;
    triggerCl = null;
    wfCl = null;
    clb = null;
  }


  public void testConstruction() {

    boolean noParentsRejected = false;
    try {
      clb = new ClassLoaderBase(null, null, new URL[0], new ClassLoaderBase[0], VersionManagement.REVISION_WORKINGSET);
    } catch (IllegalArgumentException e) {
      noParentsRejected = true;
    }

    if (!noParentsRejected)
      fail(ClassLoaderBase.class.getSimpleName() + " did not reject an empty array of parents");

    clb = new ClassLoaderBase(null, "id", new URL[0], new ClassLoaderBase[] {XynaClassLoader.getInstance()}, VersionManagement.REVISION_WORKINGSET);

  }


  public void testLoadJavaClasses() {

    Class<?> result = null;

    // load a java base class
    try {
      result = clb.loadClass("java.lang.Long");
    } catch (ClassNotFoundException e) {
      fail("Could not load class: " + e.getMessage());
    }

    assertEquals("Loaded class has invalid name", Long.class.getSimpleName(), result.getSimpleName());

    // load a more complex class
    try {
      result = clb.loadClass("java.util.concurrent.PriorityBlockingQueue");
    } catch (ClassNotFoundException e) {
      fail("Could not load class: " + e.getMessage());
    }

    assertEquals("Loaded class has invalid name", PriorityBlockingQueue.class.getSimpleName(), result.getSimpleName());

  }


  public void testAppendMDMClassLoader() throws MalformedURLException, XynaException {

    ClassLoaderBase clb = new ClassLoaderBase(null, "id", new URL[0],
                                              new ClassLoaderBase[] {XynaClassLoader.getInstance()}, VersionManagement.REVISION_WORKINGSET);

    String mdmClassLoaderName = "mdmClassLoaderName";

    SharedLibClassLoader slc = new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET);

    @SuppressWarnings("unused")
    MDMClassLoader mdmCl = new MDMClassLoader(mdmClassLoaderName, new SharedLibClassLoader[] {slc}, new String[0], "",
                                              mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET);

    clb.addDependencyToReloadIfThisClassLoaderIsRecreated(mdmClassLoaderName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, ClassLoaderType.MDM, ClassLoadingDependencySource.Backup);
    assertTrue(clb.getClassLoadersToReloadIfThisClassLoaderIsRecreated().get(ClassLoaderType.MDM).keySet()
                    .contains(mdmClassLoaderName));
    assertTrue(slc.getClassLoadersToReloadIfThisClassLoaderIsRecreated().get(ClassLoaderType.MDM).keySet()
                    .contains(mdmClassLoaderName));

  }


  public void testReloadAllDependencies() throws XynaException {

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.SharedLib, sharedLibClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(sharedLibCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.MDM, mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(mdmCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Trigger, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(triggerCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.WF, wfClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(wfCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(filterCl).anyTimes();
    try {
      cld.reloadClassLoaderByType(ClassLoaderType.SharedLib, sharedLibClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.MDM, mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.Trigger, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.WF, wfClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
    }
    catch (XynaException e) {
      fail(e.getMessage());
    }

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

      clb.reloadDependencies(new ClassLoaderSwitcher() {

        public void switchClassLoader() throws XynaException {
        }
        
      });

    EasyMock.verify(cld, xf, xfm);

  }


  public void testReloadSharedLibDependencies() throws XynaException {

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(filterCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Trigger, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(triggerCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.MDM, mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(mdmCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.WF, wfClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(wfCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.SharedLib, SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET)).andReturn(wfCl).anyTimes();
    try {
      cld.reloadClassLoaderByType(ClassLoaderType.MDM, mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.Trigger, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.WF, wfClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.SharedLib, SharedLibClassLoader.EMPTYSHAREDLIB, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
    }
    catch (XynaException e) {
      fail(e.getMessage());
    }

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

    sharedLibCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });

    EasyMock.verify(cld, xf, xfm);

  }


  public void testReloadMDMDependencies() throws XynaException {

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(filterCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.WF, wfClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(wfCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.MDM, mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(mdmCl).anyTimes();
    try {
      cld.reloadClassLoaderByType(ClassLoaderType.MDM, mdmClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.WF, wfClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
    }
    catch (XynaException e) {
      fail(e.getMessage());
    }

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

    mdmCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });

    EasyMock.verify(cld, xf, xfm);

  }


  public void testReloadTriggerDependencies() throws XynaException {

    // Mock some classes, such as the class loader dispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(filterCl).anyTimes();
    EasyMock.expect(cld.getClassLoaderByType(ClassLoaderType.Trigger, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET)).andReturn(triggerCl).anyTimes();
    try {
      cld.reloadClassLoaderByType(ClassLoaderType.Filter, filterClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
      cld.reloadClassLoaderByType(ClassLoaderType.Trigger, triggerClassLoaderName, VersionManagement.REVISION_WORKINGSET);
      EasyMock.expectLastCall().times(1);
    }
    catch (XynaException e) {
      fail(e.getMessage());
    }

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


    triggerCl.reloadDependencies(new ClassLoaderSwitcher() {

      public void switchClassLoader() throws XynaException {
      }
      
    });

    EasyMock.verify(cld, xf, xfm);

  }

}

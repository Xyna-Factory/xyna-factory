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

package com.gip.xyna.xprc.xfractwfe.generation;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.update.Updater;
import com.gip.xyna.update.UpdaterInterface;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentState;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.WorkflowEngine;



public abstract class AGenerationTestTemplate<T extends GenerationBase> extends TestCase {

  private static Logger logger = CentralFactoryLogging.getLogger(AGenerationTestTemplate.class);

  private static final String currentMdmVersion = "1.7";


  protected abstract ArrayList<String> getXmlString();


  protected abstract ArrayList<String> getNames();


  public abstract String getType();


  public abstract String getPath();
  
  
  protected String getFqClassName() {
    return ((getPath() != null && getPath().length() > 0) ? getPath() + "." : "") + getType();
  }


  public abstract void checkAfterGetInstance(T d) throws XynaException;


  public abstract void checkCopyXML(T d) throws XynaException;


  public abstract void checkParse(T d) throws XynaException;


  public abstract void checkValidate(T d) throws XynaException;


  public abstract void checkGeneration(T d) throws Exception;


  public abstract void checkOnDeployment(T d) throws XynaException;


  public abstract void checkAfterReload(T d) throws XynaException;


  public abstract void checkAfterCodeUnchanged(T d) throws XynaException;


  public abstract void checkAfterCodeChanged(T d) throws XynaException;
  
  public abstract void checkBeforeUndeployment(T d) throws XynaException;
  
  public abstract void checkAfterUndeployment(T d) throws XynaException;


  public abstract T getInstance(String fqClassName) throws XynaException;


  public void checkCleanup(T d) throws XynaException {
    // check cache empty
    T d2 = getInstance(getFqClassName());
    assertNotSame(d, d2);
    // check no backup
    File backup = new File(getDeployedXml(getPath(), getType()).getAbsolutePath() + ".old");
    assertFalse(backup.exists());
  }


  public void testGetInstance() {

    try {
      T d = getInstance(getFqClassName());

      checkAfterGetInstance(d);

    } catch (XynaException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

  }


  public void testParse() throws Exception {
    
    T d = getInstance(getFqClassName());
    checkAfterGetInstance(d);
    d.parseGeneration(false, true, true);

    checkParse(d);
    checkValidate(d);
    checkCleanup(d);

  }


  public void testDeployReload() throws IOException, XynaException {

    writeFiles(false);
    
    T d = getInstance(getFqClassName());
    checkAfterGetInstance(d);
    
    d.deploy(DeploymentMode.codeNew, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
    
    d.deploy(DeploymentMode.reload, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);

    checkParse(d);
    checkOnDeployment(d);
    checkCleanup(d);
    checkAfterReload(d);

  }


  public void testDeployCodeUnchanged() throws Exception {
    
    T d = getInstance(getFqClassName());
    checkAfterGetInstance(d);
    d.deploy(DeploymentMode.codeUnchanged, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);

    checkCopyXML(d);
    checkParse(d);
    checkValidate(d);
    checkGeneration(d);
    checkOnDeployment(d);
    checkCleanup(d);

    checkAfterCodeUnchanged(d);

  }


  public void testDeployCodeChanged() throws Exception {

    writeFiles(false);

    T d = getInstance(getFqClassName());
    checkAfterGetInstance(d);
    d.deploy(DeploymentMode.codeChanged, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);

    checkCopyXML(d);
    checkParse(d);
    checkValidate(d);
    checkGeneration(d);
    checkOnDeployment(d);
    checkCleanup(d);

    checkAfterCodeChanged(d);

  }


  public void testUndeploy() throws XynaException, IOException {

    writeFiles(false);

    T d = getInstance(getFqClassName());
    d.deploy(DeploymentMode.codeNew, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);

    checkBeforeUndeployment(d);

    // We have to get a new DOM instance after the deployment has finished
    d = getInstance(getFqClassName());
    d.undeploy(DependentObjectMode.PROTECT, false);

    checkAfterUndeployment(d);

  }


  private void findPathToXSD() {
    // Achtung: unsaubere lösung. funktioniert nur auf hudson und lokal. auf dem hudson ist keine installation,
    // deshalb liegt das xsd dort in einem anderen verzeichnis.
    if (!new File("resources").exists()) {
      if (new File("../_Interfaces/XMDM.xsd").exists()) {
        Constants.PATH_TO_XSD_FILE = "../_Interfaces/XMDM.xsd";
      } else {
        Constants.PATH_TO_XSD_FILE = "../../_Interfaces/XMDM.xsd";
      }
    }
  }


  private void findPathToLibs() {
    // Achtung: unsaubere lösung. funktioniert nur auf hudson und lokal. auf dem hudson ist keine installation,
    // deshalb liegen die libs dort wo anders
    if (!new File("lib").exists()) {
      Constants.LIB_DIR = "../../localbuild/server/" + Constants.LIB_DIR;
    }
  }
  
  private void findPathToServer() {
    if (!new File("bin").exists()) {
      if (!new File("deploy").exists()) {
        Constants.SERVER_CLASS_DIR = "deploy/xynaserver.jar";
      } else {
        Constants.SERVER_CLASS_DIR = "classes";
      }
    }
  }
  
  @Override
  public void setUp() throws SecurityException, IOException, NoSuchMethodException, XynaException,
                  ClassNotFoundException {

    GenerationBasePropertyChangeListener.setInstance(new GenerationBasePropertyChangeListener("") {
      @Override
      public boolean getValidateXsdDisabled() {
        return true;
      }
      @Override
      public boolean getIsRegistered() {
        return true;
      }
    });

    ExceptionGeneration dummyXynaExceptionGeneration = new ExceptionGeneration(ExceptionGeneration.CORE_XYNAEXCEPTION,
                                                                               ExceptionGeneration.CORE_XYNAEXCEPTION, VersionManagement.REVISION_WORKINGSET);
    dummyXynaExceptionGeneration.setStateForTestingPurposes(DeploymentState.cleanupRunning);
    GenerationBase.cacheGlobal(dummyXynaExceptionGeneration);

    XynaPropertyUtils.exchangeXynaPropertySource(new AbstractXynaPropertySource() {
      public String getProperty(String name) {
        if( name.equals(XynaProperty.REMOVE_GENERATED_FILES.getPropertyName()) ) {
          return "false";
        }
        return null;
      }
    });
 
    logger.debug("starting test " + super.getName());
    
    DeploymentManagement.setInstance(new DummyDeploymentManager());

    // Mocks konfigurieren
    // Updater
    UpdaterInterface up = EasyMock.createMock(UpdaterInterface.class);
    up.validateMDMVersion(EasyMock.eq(currentMdmVersion));
    EasyMock.expectLastCall().anyTimes();
    Updater.setInstance(up);

    EasyMock.replay(up);
    
    // DeploymentHandling
    DeploymentHandling dh = new DeploymentHandling();

    DependencyRegister depRegister = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(depRegister.getDependencies(EasyMock.isA(String.class), EasyMock.isA(DependencySourceType.class)))
                    .andReturn(new HashSet<DependencyNode>());
    EasyMock.expect(depRegister.getDependencyNode(EasyMock.isA(String.class), EasyMock.isA(DependencySourceType.class), VersionManagement.REVISION_WORKINGSET))
                    .andReturn(null).anyTimes();
    EasyMock.replay(depRegister);

    // WorkflowEngine
    WorkflowEngine wfe = EasyMock.createMock(WorkflowEngine.class);
    EasyMock.expect(wfe.getDeploymentHandling()).andReturn(dh).anyTimes();

    EasyMock.replay(wfe);

    // XynaProcessing
    XynaProcessingBase xproc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xproc.getWorkflowEngine()).andReturn(wfe).anyTimes();

    EasyMock.replay(xproc);

    // MDMClassLoader
    MDMClassLoader MDMcl = EasyMock.createMock(MDMClassLoader.class);
    //EasyMock.expect(MDMcl.removeDependenciesOfUndeployedObjects(EasyMock.isA(UndeploymentMode.class), EasyMock.anyBoolean(), EasyMock.isA(Set.class), EasyMock.isA(Set.class))).andReturn(false).anyTimes();
    
    EasyMock.replay(MDMcl);
    
    // ClassLoaderDispatcher
    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cld.getMDMClassLoader(EasyMock.isA(String.class), VersionManagement.REVISION_WORKINGSET, EasyMock.isA(boolean.class))).andReturn(MDMcl).anyTimes();
    cld.removeMDMClassLoader(EasyMock.isA(String.class), VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().anyTimes();
    cld.removeWFClassLoader(EasyMock.isA(String.class), VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().anyTimes();
    
    EasyMock.replay(cld);
    
    // XynaFactoryControl
    XynaFactoryControl xfc = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfc.getClassLoaderDispatcher()).andReturn(cld).anyTimes();
    EasyMock.expect(xfc.getDependencyRegister()).andReturn(depRegister).anyTimes();
    
    EasyMock.replay(xfc);
    
    // XynaFactoryManagement
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getProperty(XynaProperty.XYNA_DISABLE_XSD_VALIDATION)).andReturn("false").anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfc).anyTimes();

    EasyMock.replay(xfm);

    // XynaFactory
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    EasyMock.expect(xf.isStartingUp()).andReturn(true).anyTimes();

    EasyMock.replay(xf);

    XynaFactory.setInstance(xf);

    findPathToXSD();
    findPathToLibs();
    findPathToServer();

    writeFiles(false);

  }


  private void writeFiles(boolean toDeploymentDir) throws IOException {
    int counter = 0;

    for (counter = 0; counter < getNames().size(); counter++) {

      File f = null;
      if (toDeploymentDir) {
        f = getDeployedXml(getPath(), getNames().get(counter));
      }
      else {
        f = getSavedXml(getPath(), getNames().get(counter));
      }
      
      if (!f.exists()) {
          f.getParentFile().mkdirs();
          f.createNewFile();
      }
      logger.debug("made sure, file " + f.getAbsolutePath() + " exists");

      FileOutputStream fos = null;
try {
        String s = getXmlString().get(counter);
        fos = new FileOutputStream(f);
        fos.write(s.getBytes(Constants.DEFAULT_ENCODING));
        fos.flush();
}
      finally {
        if (fos != null)
            fos.close();
      }

    }
  }

  
  public void tearDown() {
    GenerationBasePropertyChangeListener.setInstance(null);
    File f = getDeployedXml(getPath(), getType());
    File f2 = getSavedXml(getPath(), getType());
    f.delete();
    f2.delete();
    for (String name: getNames()) {
      f = getDeployedXml(getPath(), name);
      f2 = getSavedXml(getPath(), name);
      f.delete();
      f2.delete();
    }
  }


  private File getSavedXml(String path, String name) {
    return new File(getXmlPath(true, path, name));
  }


  private File getDeployedXml(String path, String name) {
    return new File(getXmlPath(false, path, name));
  }


  private String getXmlPath(boolean saved, String path, String name) {
    String s;
    if (saved)
      s = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    else
      s = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    return s + Constants.fileSeparator + (isEmpty(path) ? "" : path + Constants.fileSeparator) + name + ".xml";
  }
  
  private boolean isEmpty(String  s) {
    return s == null || s.length() == 0;
  }
  
  
  private class DummyDeploymentManager extends DeploymentManagement {
    
    DummyDeploymentManager() {
      super(true);
    }
    
    
      


    
    
    
    
  }

}

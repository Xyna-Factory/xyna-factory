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
package com.gip.xyna.xfmg.xfctrl.deployitem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import junit.framework.TestCase;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.FileUtils;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.DirectoryBasedClassLoader;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;


/**
 * Abstract base test class concerned with mocking and setup utility 
 */
public abstract class TestDeploymentItemBuildSetup extends TestCase {

  protected final static long TEST_REVISION = 123456789L;
  protected final static RuntimeContext TEST_WORKSPACE = new Workspace("test");
  protected final static String DEPLOYMENT_ITEM_TEST_PATH = "xfmg.xfctrl.deployitem";
  protected final static String A_WORKFLOW_NAME = "A";
  protected final static String A_WORKFLOW_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + A_WORKFLOW_NAME;
  protected final static String B_WORKFLOW_NAME = "B";
  protected final static String B_WORKFLOW_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + B_WORKFLOW_NAME;
  protected final static String C_WORKFLOW_NAME = "C";
  protected final static String C_WORKFLOW_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + C_WORKFLOW_NAME;
  protected final static String D_WORKFLOW_NAME = "D";
  protected final static String D_WORKFLOW_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + D_WORKFLOW_NAME;
  protected final static String E_WORKFLOW_NAME = "E";
  protected final static String E_WORKFLOW_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + E_WORKFLOW_NAME;
  protected final static String F_WORKFLOW_NAME = "F";
  protected final static String F_WORKFLOW_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + F_WORKFLOW_NAME;
  protected final static String CONSTANT_LIST_OUTPUT_WF_NAME = "ConstantListOutput";
  protected final static String CONSTANT_LIST_OUTPUT_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + CONSTANT_LIST_OUTPUT_WF_NAME;
  protected final static String LIST_INPUT_WF_NAME = "ListInputWF";
  protected final static String LIST_INPUT_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + LIST_INPUT_WF_NAME;
  protected final static String LIST_CALLER_WF_NAME = "ListCaller";
  protected final static String LIST_CALLER_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + LIST_CALLER_WF_NAME;
  protected final static String LIST_MAPPING_MISSMATCH_WF_NAME = "MappingWithListMissmatch";
  protected final static String LIST_MAPPING_MISSMATCH_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + LIST_MAPPING_MISSMATCH_WF_NAME;
  protected final static String A_DATATYPE_NAME = "ADatatype";
  protected final static String A_DATATYPE_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + A_DATATYPE_NAME;
  protected final static String B_DATATYPE_NAME = "BDatatype";
  protected final static String B_DATATYPE_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + B_DATATYPE_NAME;
  protected final static String D_DATATYPE_NAME = "DDatatype";
  protected final static String D_DATATYPE_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + D_DATATYPE_NAME;
  protected final static String A_SERVICEGROUP_NAME = "AServicegroup";
  protected final static String A_SERVICEGROUP_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + A_SERVICEGROUP_NAME;
  protected final static String NULL_CHECK_CHOICE_WF_NAME = "NullCheckChoice";
  protected final static String NULL_CHECK_CHOICE_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + NULL_CHECK_CHOICE_WF_NAME;
  protected final static String INSTANCE_SERVICE_DATATYPE_C_NAME = "CDatatype";
  protected final static String INSTANCE_SERVICE_DATATYPE_C_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + INSTANCE_SERVICE_DATATYPE_C_NAME;
  protected final static String TEMPLATE_BLOCK_INSTANCE_INVOCATION_NAME = "TemplateBlockInstanceInvocation";
  protected final static String TEMPLATE_BLOCK_INSTANCE_INVOCATION_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + TEMPLATE_BLOCK_INSTANCE_INVOCATION_NAME;
  protected final static String MAPPING_ASSIGNMENT_WF_NAME = "MappingAssignment";
  protected final static String MAPPING_ASSIGNMENT_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + MAPPING_ASSIGNMENT_WF_NAME;
  protected final static String DOCUMENT_PART_NAME = "DocumentPart";
  protected final static String DOCUMENT_PART_FQNAME = "xact.templates." + DOCUMENT_PART_NAME;
  protected final static String DOCUMENT_NAME = "Document";
  protected final static String DOCUMENT_FQNAME = "xact.templates." + DOCUMENT_NAME;
  protected final static String DOCUMENT_TYPE_NAME = "DocumentType";
  protected final static String DOCUMENT_TYPE_FQNAME = "xact.templates." + DOCUMENT_TYPE_NAME;
  protected final static String TEMPLATE_MANAGEMENT_NAME = "TemplateManagement";
  protected final static String TEMPLATE_MANAGEMENT_FQNAME = "xact.templates." + TEMPLATE_MANAGEMENT_NAME;
  protected final static String MODELLED_EXPRESSIONS_WITH_LISTS_WF_NAME = "ModelledExpressionsWithLists";
  protected final static String MODELLED_EXPRESSIONS_WITH_LISTS_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + MODELLED_EXPRESSIONS_WITH_LISTS_WF_NAME;
  protected final static String A_DATATYPE_EXTENSION_NAME = "ADatatypeExtension";
  protected final static String A_DATATYPE_EXTENSION_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + A_DATATYPE_EXTENSION_NAME;
  protected final static String UP_DOWN_CAST_WF_NAME = "UpAndDownCast";
  protected final static String UP_DOWN_CAST_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + UP_DOWN_CAST_WF_NAME;
  protected final static String PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_NAME = "PrimitiveInstanceServiceReturn";
  protected final static String PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_NAME;
  protected final static String MAP_EXTENSION_INTO_SUBLIST_WF_NAME = "MapExtensionTypeIntoSubList";
  protected final static String MAP_EXTENSION_INTO_SUBLIST_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + MAP_EXTENSION_INTO_SUBLIST_WF_NAME;
  protected final static String CALCULATE_WITH_STRINGS_WF_NAME = "CalculateWithStrings";
  protected final static String CALCULATE_WITH_STRINGS_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + CALCULATE_WITH_STRINGS_WF_NAME;
  protected final static String A_INSTANCE_SERVICE_GROUP_NAME = "AInstanceServiceGroup";
  protected final static String A_INSTANCE_SERVICE_GROUP_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + A_INSTANCE_SERVICE_GROUP_NAME;
  protected final static String BASE_CHOICE_HIERARCHY_WF_NAME = "BaseChoiceHierarchy";
  protected final static String BASE_CHOICE_HIERARCHY_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + BASE_CHOICE_HIERARCHY_WF_NAME;
  protected final static String BASE_CHOICE_DT_NAME = "BaseChoice";
  protected final static String BASE_CHOICE_DT_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + BASE_CHOICE_DT_NAME;
  protected final static String SUB_CHOICE_1_DT_NAME = "SubChoice1";
  protected final static String SUB_CHOICE_1_DT_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + SUB_CHOICE_1_DT_NAME;
  protected final static String SUB_CHOICE_2_DT_NAME = "SubChoice2";
  protected final static String SUB_CHOICE_2_DT_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + SUB_CHOICE_2_DT_NAME;
  protected final static String COMPLEX_OBJECT_TO_STRING_WF_NAME = "ComplexObjectToString";
  protected final static String COMPLEX_OBJECT_TO_STRING_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + COMPLEX_OBJECT_TO_STRING_WF_NAME;
  protected final static String LIST_FUNCTIONS_WF_NAME = "ListFunctions";
  protected final static String LIST_FUNCTIONS_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + LIST_FUNCTIONS_WF_NAME;
  protected final static String LIST_FUNCTIONS_2_WF_NAME = "ListFunctions2";
  protected final static String LIST_FUNCTIONS_2_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + LIST_FUNCTIONS_2_WF_NAME;
  protected final static String XYNA_EXCEPTION_BASE_NAME = "XynaExceptionBase";
  protected final static String XYNA_EXCEPTION_BASE_FQNAME = "core.exception." + XYNA_EXCEPTION_BASE_NAME;
  protected final static String XYNA_EXCEPTION_NAME = "XynaException";
  protected final static String XYNA_EXCEPTION_FQNAME = "core.exception." + XYNA_EXCEPTION_NAME;
  protected final static String EXCEPTION_NAME = "Exception";
  protected final static String EXCEPTION_FQNAME = "core.exception." + EXCEPTION_NAME;
  
  protected final static String A_INSTANCE_SERVICE_GROUP_IMPL_PATH = "./test/com/gip/xyna/xfmg/xfctrl/deployitem/AInstanceServiceGroupImpl.jar";
  protected final static String A_INSTANCE_SERVICE_GROUP_THROW_PROPERTY = "xfmg.xfctrl.deployitem.AInstanceServiceGroup.throwOnDeployment";
  
  protected final static ConcurrentMap<String, Boolean>  classloaderPresent = new ConcurrentHashMap<String, Boolean>();
  protected final static ConcurrentMap<String, Boolean>  wfDbEntryPresent = new ConcurrentHashMap<String, Boolean>();
  protected final static ConcurrentMap<String, String>  configuration = new ConcurrentHashMap<String, String>();
  
  protected static DeploymentItemStateManagement dism; 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    configuration.put("xyna.disable.xsd.validation", "true");
    configuration.put("xyna.xprc.xfractwfe.inmemorycompile", "true");
    
    // Factory setup
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    XynaFactory.setInstance(xf);
    
    XynaFactoryManagement xfm = EasyMock.createMock(XynaFactoryManagement.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    RevisionManagement revMgmt = EasyMock.createMock(RevisionManagement.class);
    EasyMock.expect(xfctrl.getRevisionManagement()).andReturn(revMgmt).anyTimes();
    EasyMock.expect(revMgmt.getRevision(EasyMock.isA(RuntimeContext.class))).andReturn(TEST_REVISION).anyTimes();
    EasyMock.expect(revMgmt.isApplicationRevision(EasyMock.isA(Long.class))).andReturn(false).anyTimes();
    EasyMock.expect(revMgmt.isWorkspaceRevision(EasyMock.isA(Long.class))).andReturn(true).anyTimes();
    dism = EasyMock.createMock(DeploymentItemStateManagement.class);
    EasyMock.expect(xfctrl.getDeploymentItemStateManagement()).andAnswer(new IAnswer<DeploymentItemStateManagement>() {
      public DeploymentItemStateManagement answer() throws Throwable {
        return dism;
      }
    }).anyTimes();
    
    XynaFactoryManagementODS mgmtOds = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(mgmtOds).anyTimes();
    
    FutureExecution fe = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(xf.getFutureExecution()).andReturn(fe).anyTimes();
    EasyMock.expect(xf.getFutureExecutionForInit()).andReturn(fe).anyTimes();
    EasyMock.expect(fe.nextId()).andReturn(1).anyTimes();
    fe.execAsync(EasyMock.isA(FutureExecutionTask.class));
    EasyMock.expectLastCall().anyTimes();
    
    XynaProcessingBase xprc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xf.getProcessing()).andReturn(xprc).anyTimes();
    
    ClassLoaderDispatcher clDisp = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(xfctrl.getClassLoaderDispatcher()).andReturn(clDisp).anyTimes();
    
    final Configuration cfg = EasyMock.createMock(Configuration.class);
    EasyMock.expect(mgmtOds.getConfiguration()).andReturn(cfg).anyTimes();
    cfg.addPropertyChangeListener(EasyMock.isA(IPropertyChangeListener.class));
    EasyMock.expectLastCall();
    EasyMock.expect(cfg.getProperty(EasyMock.<String>notNull())).andAnswer(new IAnswer<String>() {
      public String answer() throws Throwable {
        return configuration.get(EasyMock.getCurrentArguments()[0]);
      }
    }).anyTimes();
    
    EasyMock.expect(xfm.getProperty(EasyMock.<String>notNull()))
            .andAnswer(new IAnswer<String>() {
              public String answer() throws Throwable {
                return cfg.getProperty((String) EasyMock.getCurrentArguments()[0]);
              }
            }).anyTimes();
    
    EasyMock.replay(xf, xfm, xfctrl, fe, mgmtOds, cfg);
    
    XynaProcessingODS xods = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xprc.getXynaProcessingODS()).andReturn(xods).anyTimes();
    WorkflowDatabase wfdb = EasyMock.createMock(WorkflowDatabase.class);
    EasyMock.expect(xods.getWorkflowDatabase()).andReturn(wfdb).anyTimes();
    EasyMock.expect(wfdb.isRegisteredByFQ(EasyMock.isA(XMOMType.class), EasyMock.<String>notNull(), EasyMock.isA(Long.class)))
            .andAnswer(new IAnswer<Boolean>() {
              public Boolean answer() throws Throwable {
                String fqName = (String) EasyMock.getCurrentArguments()[1];
                return wfDbEntryPresent.containsKey(fqName) && wfDbEntryPresent.get(fqName);
              }
    }).anyTimes();
    
    EasyMock.expect(clDisp.getClassLoaderByType(EasyMock.isA(ClassLoaderType.class), EasyMock.isA(String.class), EasyMock.isA(Long.class)))
            .andAnswer(new IAnswer<ClassLoaderBase>() {
              public ClassLoaderBase answer() throws Throwable {
                String fqName = (String) EasyMock.getCurrentArguments()[1];
                if (classloaderPresent.containsKey(fqName) && classloaderPresent.get(fqName)) {
                  return new DirectoryBasedClassLoader((ClassLoaderType) EasyMock.getCurrentArguments()[0], fqName, ".") {};
                } else {
                  return null;
                }
              }
            }).anyTimes();
    
    
    EasyMock.replay(xprc, xods, wfdb, clDisp, revMgmt, dism);
    
    Updater up = EasyMock.createMock(Updater.class);
    Updater.setInstance(up);
    up.checkUpdateMdm();
    EasyMock.expectLastCall().anyTimes();
    up.validateMDMVersion(EasyMock.isA(String.class));
    EasyMock.expectLastCall().anyTimes();
    
    EasyMock.replay(up);
    
    // workspace setup
    defaultWorkspaceSetup();
  }

  
  protected void defaultWorkspaceSetup() throws UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(A_WORKFLOW_FQNAME, aWorkflowXML);
    testXMOM.put(B_WORKFLOW_FQNAME, bWorkflowXML);
    testXMOM.put(C_WORKFLOW_FQNAME, cWorkflowXML);
    testXMOM.put(D_WORKFLOW_FQNAME, dWorkflowXML);
    testXMOM.put(E_WORKFLOW_FQNAME, eWorkflowXML);
    testXMOM.put(F_WORKFLOW_FQNAME, fWorkflowXML);
    testXMOM.put(CONSTANT_LIST_OUTPUT_WF_FQNAME, CONSTANT_LIST_OUTPUT_WF_XML);
    testXMOM.put(LIST_INPUT_WF_FQNAME, LIST_INPUT_WF_XML);
    testXMOM.put(LIST_CALLER_WF_FQNAME, LIST_CALLER_WF_XML);
    testXMOM.put(LIST_MAPPING_MISSMATCH_WF_FQNAME, LIST_MAPPING_MISSMATCH_WF_NO_ERRORS_XML);
    testXMOM.put(A_DATATYPE_FQNAME, aDatatypeXML);
    testXMOM.put(B_DATATYPE_FQNAME, bDatatypeXML);
    testXMOM.put(D_DATATYPE_FQNAME, dDatatypeXML);
    testXMOM.put(A_SERVICEGROUP_FQNAME, aServicegroupXML);
    testXMOM.put(NULL_CHECK_CHOICE_WF_FQNAME, NULL_CHECK_CHOICE_WF_XML);
    testXMOM.put(TEMPLATE_BLOCK_INSTANCE_INVOCATION_FQNAME, TEMPLATE_BLOCK_INSTANCE_INVOCATION_XML);
    testXMOM.put(INSTANCE_SERVICE_DATATYPE_C_FQNAME, INSTANCE_SERVICE_DATATYPE_C_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    testXMOM.put(DOCUMENT_TYPE_FQNAME, DOCUMENT_TYPE_XML);
    testXMOM.put(MAPPING_ASSIGNMENT_WF_FQNAME, MAPPING_ASSIGNMENT_WF_XML);
    testXMOM.put(INSTANCE_SERVICE_DATATYPE_C_FQNAME, INSTANCE_SERVICE_DATATYPE_C_XML);
    testXMOM.put(MODELLED_EXPRESSIONS_WITH_LISTS_WF_FQNAME, MODELLED_EXPRESSIONS_WITH_LISTS_WF_XML);
    testXMOM.put(A_DATATYPE_EXTENSION_FQNAME, A_DATATYPE_EXTENSION_XML);
    testXMOM.put(UP_DOWN_CAST_WF_FQNAME, UP_DOWN_CAST_WF_XML);
    testXMOM.put(PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_FQNAME, PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_XML);
    testXMOM.put(MAP_EXTENSION_INTO_SUBLIST_WF_FQNAME, MAP_EXTENSION_INTO_SUBLIST_WF_XML);
    testXMOM.put(CALCULATE_WITH_STRINGS_WF_FQNAME, CALCULATE_WITH_STRINGS_WF_XML);
    testXMOM.put(A_INSTANCE_SERVICE_GROUP_FQNAME, A_INSTANCE_SERVICE_GROUP_XML);
    testXMOM.put(BASE_CHOICE_HIERARCHY_WF_FQNAME, BASE_CHOICE_HIERARCHY_WF_XML);
    testXMOM.put(BASE_CHOICE_DT_FQNAME, BASE_CHOICE_DT_XML);
    testXMOM.put(SUB_CHOICE_1_DT_FQNAME, SUB_CHOICE_1_DT_XML);
    testXMOM.put(SUB_CHOICE_2_DT_FQNAME, SUB_CHOICE_2_DT_XML);
    testXMOM.put(LIST_FUNCTIONS_WF_FQNAME, LIST_FUNCTIONS_WF_XML);
    testXMOM.put(LIST_FUNCTIONS_2_WF_FQNAME, LIST_FUNCTIONS_2_WF_XML);
    testXMOM.put(COMPLEX_OBJECT_TO_STRING_WF_FQNAME, COMPLEX_OBJECT_TO_STRING_XML);
    setupWorkspace(testXMOM);
    Map<String, String> testServices = new HashMap<String, String>();
    testServices.put(A_INSTANCE_SERVICE_GROUP_FQNAME, A_INSTANCE_SERVICE_GROUP_IMPL_PATH);
    setupImpls(testServices);
  }
  
  
  protected void setupWorkspace(Map<String, String> fqNameToXMLMap) throws UnsupportedEncodingException, IOException {
    for (Entry<String, String> xmomEntry : fqNameToXMLMap.entrySet()) {
      setupWorkspace(xmomEntry.getKey(), xmomEntry.getValue());
    }
  }
  
  
  protected void setupWorkspace(String fqName, String xml) throws UnsupportedEncodingException, IOException {
    setupWorkspace(fqName, xml, DeploymentLocation.SAVED);
  }
  
  
  protected void setupWorkspace(String fqName, String xml, DeploymentLocation location) throws UnsupportedEncodingException, IOException {
    String basePath;
    switch (location) {
      case DEPLOYED :
        basePath = GenerationBase.getFileLocationOfXmlNameForDeployment(fqName, TEST_REVISION);
        classloaderPresent.put(fqName, Boolean.TRUE);
        wfDbEntryPresent.put(fqName, Boolean.TRUE);
        break;
      default :
        basePath = GenerationBase.getFileLocationOfXmlNameForSaving(fqName, TEST_REVISION);
        break;
    }
    File file = new File(basePath + ".xml");
    file.getParentFile().mkdirs();
    file.createNewFile();
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(xml.getBytes(Constants.DEFAULT_ENCODING));
      fos.flush();
    } finally {
      fos.close();
    }
  }
  
  
  protected void setupImpls(Map<String, String> fqNameToXMLMap) {
    for (Entry<String, String> implEntry : fqNameToXMLMap.entrySet()) {
      setupImpl(implEntry.getKey(), implEntry.getValue());
    }
  }
  
  
  protected void setupImpl(String fqName, String implPath) {
    setupImpl(fqName, implPath, DeploymentLocation.SAVED);
  }


  private void setupImpl(String fqName, String implPath, DeploymentLocation location) {
    boolean deployed = location == DeploymentLocation.DEPLOYED;
    String revisionImplPath = RevisionManagement.getPathForRevision(PathType.SERVICE, TEST_REVISION, deployed);
    revisionImplPath += "/" + fqName + "/";
    File source = new File(implPath);
    if (source.exists()) {
      try {
        FileUtils.copyRecursivelyWithFolderStructure(source, new File(revisionImplPath));
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  protected abstract DeploymentItemRegistry getRegistry();
  
  protected void save(String... fqNames) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(Arrays.asList(fqNames));
  }
  
  protected void save(Collection<String> fqNames) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    for (String fqName : fqNames) {
      Optional<DeploymentItem> odi = DeploymentItemBuilder.build(fqName, Optional.<XMOMType>empty(), TEST_REVISION);
      assertTrue(fqName + " is not present after save!", odi.isPresent());
      getRegistry().save(odi.get());
    }
  }
  
  protected void deploy(String... fqNames) {
    deploy(Arrays.asList(fqNames));
  }
  
  protected void deploy(Collection<String> fqNames) {
    for (String fqName : fqNames) {
      DeploymentItemState state = getRegistry().get(fqName);
      state.collectUsingObjectsInContext(emptyCtx());
      state.deploymentTransition(DeploymentTransition.SUCCESS, true, Optional.<Throwable>empty());
    }
  }
  
  protected static DeploymentContext emptyCtx() {
    return new TestDeploymentItemStatus.TestDeploymentContext(Collections.<Pair<GenerationBase, DeploymentMode>>emptyList());
  }
  
  
  protected static String getJarFilePath(String fqServiceName, String jarFileBackup, DeploymentLocation location) {
    boolean deployed = location == DeploymentLocation.DEPLOYED; 
    String revisionImplPath = RevisionManagement.getPathForRevision(PathType.SERVICE, TEST_REVISION, deployed);
    revisionImplPath += "/" + fqServiceName + "/";
    revisionImplPath += jarFileBackup.substring(jarFileBackup.lastIndexOf('/'));
    return revisionImplPath;
  }


  protected void purgeWorkspace() {
    FileUtils.deleteDirectoryRecursively(new File(RevisionManagement.getPathForRevision(PathType.XMOM, TEST_REVISION, false)));
    FileUtils.deleteDirectoryRecursively(new File(RevisionManagement.getPathForRevision(PathType.XMOM, TEST_REVISION, true)));
    FileUtils.deleteDirectoryRecursively(new File(RevisionManagement.getPathForRevision(PathType.SERVICE, TEST_REVISION, false)));
    FileUtils.deleteDirectoryRecursively(new File(RevisionManagement.getPathForRevision(PathType.SERVICE, TEST_REVISION, true)));
  }
  
  @Override
  protected void tearDown() throws Exception {
    purgeWorkspace();
    super.tearDown();
  }
  
  
  
  // XML Repo
  protected final static String aDatatypeXML = 
    "<DataType Label=\"A Datatype\" TypeName=\"ADatatype\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Data Label=\"member variable 1\" VariableName=\"memberVariable1\">"
    +   "<Meta>"
    +     "<Type>String</Type>"
    +   "</Meta>"
    + "</Data>"
    + "<Data Label=\"member variable 2\" VariableName=\"memberVariable2\">"
    +   "<Meta>"
    +     "<Type>String</Type>"
    +   "</Meta>"
    + "</Data>"
    + "<Data Label=\"member variable 3\" VariableName=\"memberVariable3\">"
    +   "<Meta>"
    +     "<Type>int</Type>"
    +   "</Meta>"
    + "</Data>"
    + "</DataType>";
  
  protected final static String bDatatypeXML = 
    "<DataType Label=\"B Datatype\" TypeName=\"BDatatype\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Data Label=\"member variable 1\" VariableName=\"memberVariable1\">"
    +   "<Meta>"
    +     "<Type>String</Type>"
    +   "</Meta>"
    + "</Data>"
    + "<Data Label=\"A Datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
    + "</DataType>";
  
  protected final static String dDatatypeXML =
    "<DataType Label=\"d datatype\" TypeName=\"DDatatype\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    +"  <Data Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\"/>"
    +"  <Data Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\" IsList=\"true\"/>"
    +"</DataType>";
  
  
  protected final static String bWorkflowXML = 
    "<Service ID=\"1\" Label=\"B\" TypeName=\"B\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Operation ID=\"0\" Label=\"B\" Name=\"B\">"
    +   "<Input>"
    +     "<Data ID=\"3\" Label=\"A Datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
    +   "</Input>"
    +   "<Output/>"
    +   "<Assign ID=\"2\"/>"
    + "</Operation>"
    + "</Service>";
  
  
  protected final static String aWorkflowXML = 
    "<Service ID=\"1\" Label=\"A\" TypeName=\"A\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Operation ID=\"0\" Label=\"A\" Name=\"A\">"
    +   "<Input>"
    +     "<Data ID=\"3\" Label=\"A Datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
    +       "<Target RefID=\"5\"/>"
    +     "</Data>"
    +   "</Input>"
    +   "<Output/>"
    +   "<ServiceReference ID=\"4\" Label=\"B\" ReferenceName=\"B\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
    +     "<Source RefID=\"5\"/>"
    +     "<Target RefID=\"5\"/>"
    +   "</ServiceReference>"
    +   "<Function ID=\"5\" Label=\"B\">"
    +     "<Source RefID=\"4\"/>"
    +     "<Source RefID=\"3\"/>"
    +     "<Target RefID=\"4\"/>"
    +     "<Invoke ServiceID=\"4\" Operation=\"B\">"
    +       "<Source RefID=\"3\"/>"
    +     "</Invoke>"
    +     "<Receive ServiceID=\"4\"/>"
    +   "</Function>"
    +   "<Assign ID=\"2\"/>"
    + "</Operation>"
    + "</Service>";
  
  protected final static String aServicegroupXML =
    "<DataType Label=\"A Servicegroup\" TypeName=\"AServicegroup\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Meta>"
    +   "<IsServiceGroupOnly>true</IsServiceGroupOnly>"
    + "</Meta>"
    + "<Service ID=\"0\" Label=\"A Servicegroup\" TypeName=\"AServicegroup\">"
    +   "<Operation Label=\"a operation\" IsStatic=\"true\" Name=\"aOperation\">"
    +     "<Input>"
    +       "<Data Label=\"B Datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\"/>"
    +     "</Input>"
    +     "<Output>"
    +       "<Data ID=\"1\" Label=\"B Datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype1\"/>"
    +       "<Data ID=\"2\" Label=\"B Datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype2\"/>"
    +     "</Output>"
    +     "<SourceCode>"
    +       "<CodeSnippet Type=\"Java\">return null;</CodeSnippet>"
    +     "</SourceCode>"
    +   "</Operation>"
    + "</Service>"
    + "</DataType>";
  
  
  protected final static String cWorkflowXML =
    "<Service ID=\"1\" Label=\"C\" TypeName=\"C\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Operation ID=\"0\" Label=\"C\" Name=\"C\">"
    +   "<Input/>"
    +   "<Output/>"
    +   "<ServiceReference ID=\"2\" Label=\"A Servicegroup\" ReferenceName=\"AServicegroup.AServicegroup\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
    +     "<Source RefID=\"3\"/>"
    +     "<Target RefID=\"3\"/>"
    +   "</ServiceReference>"
    +   "<Function ID=\"3\" Label=\"a operation\">"
    +     "<Source RefID=\"2\"/>"
    +     "<Target RefID=\"2\"/>"
    +     "<Target RefID=\"4\"/>"
    +     "<Target RefID=\"5\"/>"
    +     "<Invoke ServiceID=\"2\" Operation=\"aOperation\">"
    +       "<Source/>"
    +     "</Invoke>"
    +     "<Receive ServiceID=\"2\">"
    +       "<Target RefID=\"4\"/>"
    +       "<Target RefID=\"5\"/>"
    +     "</Receive>"
    +   "</Function>"
    +   "<Data ID=\"4\" Label=\"B Datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype1\">"
    +     "<Source RefID=\"3\"/>"
    +   "</Data>"
    +   "<Data ID=\"5\" Label=\"B Datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype2\">"
    +     "<Source RefID=\"3\"/>"
    +   "</Data>"
    +   "<Assign/>"
    + "</Operation>"
    + "</Service>";
  
  // constant wf output on ADatatype.aMemvar
  protected final static String dWorkflowXML =
    "<Service ID=\"1\" Label=\"D\" TypeName=\"D\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    + "<Operation ID=\"0\" Label=\"D\" Name=\"D\">"
    +   "<Input/>"
    +   "<Output>"
    +     "<Data ID=\"2\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
    +       "<Source RefID=\"3\"/>"
    +     "</Data>"
    +   "</Output>"
    +   "<Data ID=\"4\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype4\">"
    +     "<Target RefID=\"3\"/>"
    +     "<Data ID=\"5\" Label=\"a memvar\" VariableName=\"aMemvar\">"
    +       "<Meta>"
    +         "<Type>String</Type>"
    +       "</Meta>"
    +       "<Value>a</Value>"
    +     "</Data>"
    +     "<Data ID=\"6\" Label=\"b memvar\" VariableName=\"bMemvar\">"
    +       "<Meta>"
    +         "<Type>String</Type>"
    +       "</Meta>"
    +     "</Data>"
    +   "</Data>"
    +   "<Assign ID=\"3\">"
    +     "<Source RefID=\"4\"/>"
    +     "<Target RefID=\"2\"/>"
    +     "<Copy>"
    +       "<Source RefID=\"4\">"
    +         "<Meta>"
    +           "<LinkType>Constant</LinkType>"
    +         "</Meta>"
    +       "</Source>"
    +       "<Target RefID=\"2\"/>"
    +     "</Copy>"
    +   "</Assign>"
    + "</Operation>"
    + "</Service>";
  
 //constant subwf input on ADatatype.aMemvar
 protected final static String eWorkflowXML =
   "<Service ID=\"1\" Label=\"E\" TypeName=\"E\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   + "<Operation ID=\"0\" Label=\"E\" Name=\"E\">"
   +   "<Input/>"
   +   "<Output/>"
   +   "<ServiceReference ID=\"2\" Label=\"B\" ReferenceName=\"B\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
   +     "<Source RefID=\"3\"/>"
   +     "<Target RefID=\"3\"/>"
   +   "</ServiceReference>"
   +   "<Function ID=\"3\" Label=\"B\">"
   +     "<Source RefID=\"2\"/>"
   +     "<Source RefID=\"4\"/>"
   +     "<Target RefID=\"2\"/>"
   +     "<Invoke ServiceID=\"2\" Operation=\"B\">"
   +       "<Source RefID=\"4\">"
   +         "<Meta>"
   +           "<LinkType>Constant</LinkType>"
   +         "</Meta>"
   +       "</Source>"
   +     "</Invoke>"
   +     "<Receive ServiceID=\"2\"/>"
   +   "</Function>"
   +   "<Data ID=\"4\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
   +     "<Target RefID=\"3\"/>"
   +     "<Data ID=\"5\" Label=\"a memvar\" VariableName=\"aMemvar\">"
   +       "<Meta>"
   +         "<Type>String</Type>"
   +       "</Meta>"
   +       "<Value>a</Value>"
   +     "</Data>"
   +     "<Data ID=\"6\" Label=\"b memvar\" VariableName=\"bMemvar\">"
   +       "<Meta>"
   +         "<Type>String</Type>"
   +       "</Meta>"
   +       "<Value>b</Value>"
   +     "</Data>"
   +   "</Data>"
   +   "<Assign ID=\"7\"/>"
   + "</Operation>"
   + "</Service>";
 
 protected final static String fWorkflowXML =
                 "<Service ID=\"1\" Label=\"F\" TypeName=\"F\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"F\" Name=\"F\">"+
"    <Input>"+
"      <Data ID=\"3\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\">"+
"        <Target RefID=\"2\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <Choice ID=\"2\" TypeName=\"BaseChoiceTypeFormula\" TypePath=\"server\">"+
"      <Source RefID=\"3\"/>"+
"      <Input>"+
"        <Data ID=\"4\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype4\"/>"+
"        <Source RefID=\"3\"/>"+
"      </Input>"+
"      <Case ID=\"6\" Label=\"true\" Premise=\"%0%.bADatatypeVar!=null\">"+
"        <Assign ID=\"5\"/>"+
"      </Case>"+
"      <Case ID=\"8\" Label=\"false\">"+
"        <Assign ID=\"7\"/>"+
"      </Case>"+
"    </Choice>"+
"    <Assign ID=\"9\"/>"+
"  </Operation>"+
"</Service>";
 
   protected final static String CONSTANT_LIST_OUTPUT_WF_XML =
     "<Service ID=\"1\" Label=\"ConstantListOutput\" TypeName=\"ConstantListOutput\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Meta>"
   +"    <FixedDetailOptions>hideDetailAreas,highDetailsMode</FixedDetailOptions>"
   +"  </Meta>"
   +"  <Operation ID=\"0\" Label=\"ConstantListOutput\" Name=\"ConstantListOutput\">"
   +"    <Input/>"
   +"    <Output>"
   +"      <Data ID=\"2\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype\">"
   +"        <Source RefID=\"3\"/>"
   +"      </Data>"
   +"    </Output>"
   +"    <Data ID=\"4\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype4\">"
   +"      <Target RefID=\"3\"/>"
   +"      <Data ID=\"11\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\">"
   +"        <Value>"
   +"          <Data ID=\"7\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
   +"            <Data ID=\"5\" Label=\"a memvar\" VariableName=\"memberVariable1\">"
   +"              <Meta>"
   +"                <Type>String</Type>"
   +"              </Meta>"
   +"              <Value>a1</Value>"
   +"            </Data>"
   +"            <Data ID=\"6\" Label=\"b memvar\" VariableName=\"memberVariable2\">"
   +"              <Meta>"
   +"                <Type>String</Type>"
   +"              </Meta>"
   +"              <Value>a1</Value>"
   +"            </Data>"
   +"          </Data>"
   +"        </Value>"
   +"        <Value>"
   +"          <Data ID=\"10\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
   +"            <Data ID=\"8\" Label=\"a memvar\" VariableName=\"memberVariable1\">"
   +"              <Meta>"
   +"                <Type>String</Type>"
   +"              </Meta>"
   +"              <Value>a2</Value>"
   +"            </Data>"
   +"            <Data ID=\"9\" Label=\"b memvar\" VariableName=\"memberVariable2\">"
   +"              <Meta>"
   +"                <Type>String</Type>"
   +"              </Meta>"
   +"              <Value>a2</Value>"
   +"            </Data>"
   +"          </Data>"
   +"        </Value>"
   +"      </Data>"
   +"      <Data ID=\"20\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\" IsList=\"true\">"
   +"        <Value>"
   +"          <Data ID=\"15\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
   +"            <Data ID=\"14\" Label=\"b a datatype var\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
   +"              <Data ID=\"12\" Label=\"a memvar\" VariableName=\"memberVariable1\">"
   +"                <Meta>"
   +"                  <Type>String</Type>"
   +"                </Meta>"
   +"                <Value>b1</Value>"
   +"              </Data>"
   +"              <Data ID=\"13\" Label=\"b memvar\" VariableName=\"memberVariable2\">"
   +"                <Meta>"
   +"                  <Type>String</Type>"
   +"                </Meta>"
   +"                <Value>b1</Value>"
   +"              </Data>"
   +"            </Data>"
   +"          </Data>"
   +"        </Value>"
   +"        <Value>"
   +"          <Data ID=\"19\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
   +"            <Data ID=\"18\" Label=\"b a datatype var\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
   +"              <Data ID=\"16\" Label=\"a memvar\" VariableName=\"memberVariable1\">"
   +"                <Meta>"
   +"                  <Type>String</Type>"
   +"                </Meta>"
   +"                <Value>b2</Value>"
   +"              </Data>"
   +"              <Data ID=\"17\" Label=\"b memvar\" VariableName=\"memberVariable2\">"
   +"                <Meta>"
   +"                  <Type>String</Type>"
   +"                </Meta>"
   +"                <Value>b2</Value>"
   +"              </Data>"
   +"            </Data>"
   +"          </Data>"
   +"        </Value>"
   +"      </Data>"
   +"    </Data>"
   +"    <Assign ID=\"3\">"
   +"      <Source RefID=\"4\"/>"
   +"      <Target RefID=\"2\"/>"
   +"      <Copy>"
   +"        <Source RefID=\"4\">"
   +"          <Meta>"
   +"            <LinkType>Constant</LinkType>"
   +"          </Meta>"
   +"        </Source>"
   +"        <Target RefID=\"2\"/>"
   +"      </Copy>"
   +"    </Assign>"
   +"  </Operation>"
   +"</Service>";
   
   protected static String NULL_CHECK_CHOICE_WF_XML =
     "<Service ID=\"1\" Label=\"F\" TypeName=\"NullCheckChoice\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
     + "<Operation ID=\"0\" Label=\"NullCheckChoice\" Name=\"NullCheckChoice\">"
     +   "<Input>"
     +     "<Data ID=\"3\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\">"
     +       "<Target RefID=\"2\"/>"
     +     "</Data>"
     +   "</Input>"
     +   "<Output/>"
     +   "<Choice ID=\"2\" TypeName=\"BaseChoiceTypeFormula\" TypePath=\"server\">"
     +     "<Source RefID=\"3\"/>"
     +     "<Input>"
     +       "<Data ID=\"4\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype4\"/>"
     +       "<Source RefID=\"3\"/>"
     +     "</Input>"
     +     "<Case ID=\"6\" Label=\"true\" Premise=\"%0%.aDatatype!=null\">"
     +       "<Assign ID=\"5\"/>"
     +     "</Case>"
     +     "<Case ID=\"8\" Label=\"false\">"
     +       "<Assign ID=\"7\"/>"
     +     "</Case>"
     +   "</Choice>"
     +   "<Assign ID=\"9\"/>"
     + "</Operation>"
     + "</Service>";
   
   protected static String INSTANCE_SERVICE_DATATYPE_C_XML =
     "<DataType Label=\"C Datatype\" TypeName=\"CDatatype\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
     + "<Data Label=\"a string\" VariableName=\"aString\">"
     +   "<Meta>"
     +     "<Type>String</Type>"
     +   "</Meta>"
     + "</Data>"
     + "<Data Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\"/>"
     + "<Service Label=\"CDatatype\" TypeName=\"CDatatype\">"
     +   "<Operation Label=\"c instance service\" IsStatic=\"false\" Name=\"cInstanceService\">"
     +     "<Input>"
     +       "<Data Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
     +       "<Data Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype0\"/>"
     +     "</Input>"
     +     "<Output>"
     +       "<Data Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\"/>"
     +     "</Output>"
     +     "<SourceCode>"
     +       "<CodeSnippet Type=\"Java\">return null;</CodeSnippet>"
     +     "</SourceCode>"
     +   "</Operation>"
     + "</Service>"
     + "</DataType>";
   
   protected static String TEMPLATE_BLOCK_INSTANCE_INVOCATION_XML =
     "<Service ID=\"1\" Label=\"TemplateBlockInstanceInvocation\" TypeName=\"TemplateBlockInstanceInvocation\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
     +"  <Meta>"
     +"    <FixedDetailOptions>highDetailsMode,showDetailAreas</FixedDetailOptions>"
     +"  </Meta>"
     +"  <Operation ID=\"0\" Label=\"TemplateBlockInstanceInvocation\" Name=\"TemplateBlockInstanceInvocation\">"
     +"    <Input>"
     +"      <Data ID=\"11\" Label=\"C Datatype\" ReferenceName=\"CDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"cDatatype\">"
     +"        <Target RefID=\"9\"/>"
     +"      </Data>"
     +"      <Data ID=\"13\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
     +"        <Target RefID=\"9\"/>"
     +"      </Data>"
     +"    </Input>"
     +"    <Output/>"
     +"    <ServiceReference ID=\"2\" Label=\"Template Management\" ReferenceName=\"TemplateManagement.TemplateManagement\" ReferencePath=\"xact.templates\">"
     +"      <Source RefID=\"3\"/>"
     +"      <Source RefID=\"5\"/>"
     +"      <Target RefID=\"3\"/>"
     +"      <Target RefID=\"5\"/>"
     +"    </ServiceReference>"
     +"    <Function ID=\"3\" Label=\"Begin document\">"
     +"      <Source RefID=\"2\"/>"
     +"      <Source RefID=\"7\"/>"
     +"      <Target RefID=\"2\"/>"
     +"      <Target RefID=\"4\"/>"
     +"      <Invoke ServiceID=\"2\" Operation=\"start\">"
     +"        <Source RefID=\"7\">"
     +"          <Meta>"
     +"            <LinkType>Constant</LinkType>"
     +"          </Meta>"
     +"        </Source>"
     +"      </Invoke>"
     +"      <Receive ServiceID=\"2\">"
     +"        <Target RefID=\"4\"/>"
     +"      </Receive>"
     +"    </Function>"
     +"    <Data ID=\"4\" Label=\"Document Context\" ReferenceName=\"DocumentContext\" ReferencePath=\"xact.templates\" VariableName=\"documentContext\">"
     +"      <Source RefID=\"3\"/>"
     +"      <Target RefID=\"5\"/>"
     +"    </Data>"
     +"    <Mappings ID=\"9\">"
     +"      <Source RefID=\"11\"/>"
     +"      <Source RefID=\"13\"/>"
     +"      <Target RefID=\"8\"/>"
     +"      <Meta>"
     +"        <IsTemplate>true</IsTemplate>"
     +"      </Meta>"
     +"      <Input>"
     +"        <Data ID=\"10\" Label=\"C Datatype\" ReferenceName=\"CDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"cDatatype10\"/>"
     +"        <Source RefID=\"11\">"
     +"          <Meta>"
     +"            <LinkType>UserConnected</LinkType>"
     +"          </Meta>"
     +"        </Source>"
     +"      </Input>"
     +"      <Input>"
     +"        <Data ID=\"12\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype12\"/>"
     +"        <Source RefID=\"13\">"
     +"          <Meta>"
     +"            <LinkType>UserConnected</LinkType>"
     +"          </Meta>"
     +"        </Source>"
     +"      </Input>"
     +"      <Input>"
     +"        <Data ID=\"14\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype14\"/>"
     +"        <Source RefID=\"13\">"
     +"          <Meta>"
     +"            <LinkType>UserConnected</LinkType>"
     +"          </Meta>"
     +"        </Source>"
     +"      </Input>"
     +"      <Output>"
     +"        <Data ID=\"15\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart\"/>"
     +"        <Target RefID=\"8\"/>"
     +"      </Output>"
     +"      <Mapping>%3%.text=concat(\"some string \",%0%.cInstanceService(%1%,%2%).aDatatype.memberVariable1,\"some more string\")</Mapping>" 
     +"    </Mappings>"
     +"    <Function ID=\"5\" Label=\"End document\">"
     +"      <Source RefID=\"2\"/>"
     +"      <Source RefID=\"4\"/>"
     +"      <Target RefID=\"2\"/>"
     +"      <Target RefID=\"6\"/>"
     +"      <Invoke ServiceID=\"2\" Operation=\"stop\">"
     +"        <Source RefID=\"4\"/>"
     +"      </Invoke>"
     +"      <Receive ServiceID=\"2\">"
     +"        <Target RefID=\"6\"/>"
     +"      </Receive>"
     +"    </Function>"
     +"    <Data ID=\"6\" Label=\"Document\" ReferenceName=\"Document\" ReferencePath=\"xact.templates\" VariableName=\"document\">"
     +"      <Source RefID=\"5\"/>"
     +"    </Data>"
     +"    <Data ID=\"7\" Label=\"Document Type\" ReferenceName=\"PlainText\" ReferencePath=\"xact.templates\" VariableName=\"documentType\">"
     +"      <Target RefID=\"3\"/>"
     +"    </Data>"
     +"    <Data ID=\"8\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart8\">"
     +"      <Source RefID=\"9\"/>"
     +"    </Data>"
     +"    <Assign ID=\"16\"/>"
     +"  </Operation>"
     +"</Service>";     
   
   protected static String DOCUMENT_PART_XML =
   "<DataType Label=\"Document part\" TypeName=\"DocumentPart\" TypePath=\"xact.templates\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
     +"  <Meta>"
     +"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"
     +"    <IsXynaComponent>true</IsXynaComponent>"
     +"  </Meta>"
     +"  <Data Label=\"Text\" VariableName=\"text\">"
     +"    <Meta>"
     +"      <Type>String</Type>"
     +"    </Meta>"
     +"  </Data>"
     +"</DataType>";
   
   protected static String DOCUMENT_XML = 
     "<DataType Label=\"Document\" TypeName=\"Document\" TypePath=\"xact.templates\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Meta>"
   +"    <IsXynaComponent>true</IsXynaComponent>"
   +"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"
   +"  </Meta>"
   +"  <Libraries>DocumentImpl.jar</Libraries>"
   +"  <Data Label=\"document type\" ReferenceName=\"DocumentType\" ReferencePath=\"xact.templates\" VariableName=\"documentType\"/>"
   +"  <Data Label=\"text\" VariableName=\"text\">"
   +"    <Meta>"
   +"      <Type>String</Type>"
   +"    </Meta>"
   +"  </Data>"
   +"  <Service Label=\"document\" TypeName=\"Document\">"
   +"    <Operation ID=\"3\" Label=\"Add to buffer\" IsStatic=\"false\" Name=\"addToBuffer\">"
   +"      <Input>"
   +"        <Data ID=\"2\" Label=\"text\" VariableName=\"text\">"
   +"          <Meta>"
   +"            <Type>String</Type>"
   +"          </Meta>"
   +"        </Data>"
   +"      </Input>"
   +"      <Output/>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">getImplementationOfInstanceMethods().addToBuffer(text);</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"    <Operation ID=\"7\" Label=\"Get complete buffer content\" IsStatic=\"false\" Name=\"getCompleteBufferContent\">"
   +"      <Input/>"
   +"      <Output>"
   +"        <Data ID=\"6\" Label=\"content\" VariableName=\"content\">"
   +"          <Meta>"
   +"            <Type>String</Type>"
   +"          </Meta>"
   +"        </Data>"
   +"      </Output>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">return getImplementationOfInstanceMethods().getCompleteBufferContent();</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"    <Operation ID=\"8\" Label=\"Get read buffer\" IsStatic=\"false\" Name=\"getReadBuffer\">"
   +"      <Input/>"
   +"      <Output>"
   +"        <Data ID=\"9\" Label=\"content\" VariableName=\"content\">"
   +"          <Meta>"
   +"            <Type>String</Type>"
   +"          </Meta>"
   +"        </Data>"
   +"      </Output>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">return getImplementationOfInstanceMethods().getReadBuffer();</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"    <Operation ID=\"10\" Label=\"Mark buffer as read\" IsStatic=\"false\" Name=\"markReadBufferAsSend\">"
   +"      <Input/>"
   +"      <Output/>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">getImplementationOfInstanceMethods().markReadBufferAsSend();</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"    <Operation ID=\"5\" Label=\"Read\" IsStatic=\"false\" Name=\"read\">"
   +"      <Input/>"
   +"      <Output/>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">getImplementationOfInstanceMethods().read();</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"  </Service>"
   +"</DataType>";
   
   protected static String TEMPLATE_MANAGEMENT_XML =
   "<DataType Label=\"Template Management\" TypeName=\"TemplateManagement\" TypePath=\"xact.templates\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Meta>"
   +"    <IsServiceGroupOnly>true</IsServiceGroupOnly>"
   +"    <IsXynaComponent>true</IsXynaComponent>"
   +"  </Meta>"
   +"  <Service ID=\"8\" Label=\"Template Management\" TypeName=\"TemplateManagement\">"
   +"    <Meta>"
   +"      <AdditionalDependencies>"
   +"        <Datatype>xact.templates.Document</Datatype>"
   +"        <Datatype>xact.templates.DocumentType</Datatype>"
   +"        <Datatype>xact.templates.NETCONF</Datatype>"
   +"        <Datatype>xact.templates.CommandLineInterface</Datatype>"
   +"        <Datatype>xact.templates.PlainText</Datatype>"
   +"        <Datatype>xact.templates.XML</Datatype>"
   +"      </AdditionalDependencies>"
   +"    </Meta>"
   +"    <Operation ID=\"1\" Label=\"Retrieve document\" IsStatic=\"true\" Name=\"retrieve\">"
   +"      <Meta>"
   +"        <SpecialPurpose>RetrieveDocument</SpecialPurpose>"
   +"      </Meta>"
   +"      <Input/>"
   +"      <Output>"
   +"        <Data ID=\"0\" Label=\"Document\" ReferenceName=\"Document\" ReferencePath=\"xact.templates\" VariableName=\"document13\"/>"
   +"      </Output>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">internalDocumentFromContext.read();"
   +"    return internalDocumentFromContext;</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"    <Operation ID=\"4\" Label=\"Begin document\" IsStatic=\"true\" Name=\"start\">"
   +"      <Meta>"
   +"        <SpecialPurpose>StartDocumentContext</SpecialPurpose>"
   +"      </Meta>"
   +"      <Input>"
   +"        <Data ID=\"2\" Label=\"Document Type\" ReferenceName=\"DocumentType\" ReferencePath=\"xact.templates\" VariableName=\"documentType\"/>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"3\" Label=\"Document Context\" ReferenceName=\"DocumentContext\" ReferencePath=\"xact.templates\" VariableName=\"documentContext\"/>"
   +"      </Output>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">return new xact.templates.DocumentContext();</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"    <Operation ID=\"7\" Label=\"End document\" IsStatic=\"true\" Name=\"stop\">"
   +"      <Meta>"
   +"        <SpecialPurpose>StopDocumentContext</SpecialPurpose>"
   +"      </Meta>"
   +"      <Input>"
   +"        <Data ID=\"5\" Label=\"Document Context\" ReferenceName=\"DocumentContext\" ReferencePath=\"xact.templates\" VariableName=\"documentContext\"/>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"6\" Label=\"Document\" ReferenceName=\"Document\" ReferencePath=\"xact.templates\" VariableName=\"document\"/>"
   +"      </Output>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">return new xact.templates.Document(internalDocumentFromContext.getDocumentType(), internalDocumentFromContext.getCompleteBufferContent());</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"  </Service>"
   +"</DataType>";
   
   protected static String DOCUMENT_TYPE_XML =
   "<DataType Label=\"Document type\" IsAbstract=\"true\" TypeName=\"DocumentType\" TypePath=\"xact.templates\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Service Label=\"document type\" TypeName=\"DocumentType\">"+
"    <Operation ID=\"3\" Label=\"Detect critcal error\" IsAbstract=\"true\" IsStatic=\"false\" Name=\"detectCritcalError\">"+
"      <Input>"+
"        <Data ID=\"0\" Label=\"response\" ReferenceName=\"CommandResponseTuple\" ReferencePath=\"xact.connection\" VariableName=\"response\"/>"+
"      </Input>"+
"      <Output/>"+
"      <Throws>"+
"        <Exception ID=\"2\" Label=\"detected error\" ReferenceName=\"DetectedError\" ReferencePath=\"xact.connection\" VariableName=\"detectedError\"/>"+
"      </Throws>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\"/>"+
"      </SourceCode>"+
"    </Operation>"+
"    <Operation ID=\"6\" Label=\"Is response complete\" IsAbstract=\"true\" IsStatic=\"false\" Name=\"isResponseComplete\">"+
"      <Input>"+
"        <Data ID=\"4\" Label=\"response\" VariableName=\"response\">"+
"          <Meta>"+
"            <Type>String</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"5\" Label=\"is complete\" VariableName=\"isComplete\">"+
"          <Meta>"+
"            <Type>Boolean</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Output>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">return false;</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"    <Operation ID=\"9\" Label=\"Partition commands\" IsAbstract=\"true\" IsStatic=\"false\" Name=\"partitionCommands\">"+
"      <Input>"+
"        <Data ID=\"7\" Label=\"document\" ReferenceName=\"Document\" ReferencePath=\"xact.templates\" VariableName=\"document\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"8\" Label=\"command\" ReferenceName=\"Command\" ReferencePath=\"xact.connection\" VariableName=\"command\" IsList=\"true\"/>"+
"      </Output>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">return null;</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"    <Operation ID=\"12\" Label=\"Remove document type specifics\" IsAbstract=\"true\" IsStatic=\"false\" Name=\"removeDocumentTypeSpecifics\">"+
"      <Input>"+
"        <Data ID=\"10\" Label=\"response\" ReferenceName=\"CommandResponseTuple\" ReferencePath=\"xact.connection\" VariableName=\"response\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"11\" Label=\"response\" ReferenceName=\"CommandResponseTuple\" ReferencePath=\"xact.connection\" VariableName=\"response0\"/>"+
"      </Output>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">return null;</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"  </Service>"+
"</DataType>";
   
   protected final static String MAPPING_ASSIGNMENT_WF_XML =
     "<Service ID=\"1\" Label=\"MappingAssignment\" TypeName=\"MappingAssignment\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Operation ID=\"0\" Label=\"MappingAssignment\" Name=\"MappingAssignment\">"
   +"    <Input>"
   +"      <Data ID=\"7\" Label=\"C Datatype\" ReferenceName=\"CDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"cDatatype7\">"
   +"        <Target RefID=\"6\"/>"
   +"      </Data>"
   +"      <Data ID=\"9\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype9\">"
   +"        <Target RefID=\"6\"/>"
   +"      </Data>"
   +"    </Input>"
   +"    <Output/>"
   +"    <Mappings ID=\"6\" Label=\"Mapping\">"
   +"      <Source RefID=\"7\"/>"
   +"      <Source RefID=\"9\"/>"
   +"      <Target RefID=\"5\"/>"
   +"      <Meta>"
   +"        <FixedDetailOptions>openConfiguration</FixedDetailOptions>"
   +"      </Meta>"
   +"      <Input>"
   +"        <Data ID=\"10\" Label=\"C Datatype\" ReferenceName=\"CDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"cDatatype\"/>"
   +"        <Source RefID=\"7\"/>"
   +"      </Input>"
   +"      <Input>"
   +"        <Data ID=\"11\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
   +"        <Source RefID=\"9\"/>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"4\" Label=\"C Datatype\" ReferenceName=\"CDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"cDatatype4\"/>"
   +"        <Target RefID=\"5\"/>"
   +"      </Output>"
   +"      <Mapping>%2%.aString~=concat(%1%.memberVariable1,\" - baum - \",%1%.memberVariable2)</Mapping>"
   +"      <Mapping>%2%.bDatatype~=%0%.cInstanceService(%1%,%1%)</Mapping>"
   +"    </Mappings>"
   +"    <Data ID=\"5\" Label=\"C Datatype\" ReferenceName=\"CDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"cDatatype5\">"
   +"      <Source RefID=\"6\"/>"
   +"    </Data>"
   +"    <Assign ID=\"12\"/>"
   +"  </Operation>"
   +"</Service>";
   
   protected static final String MODELLED_EXPRESSIONS_WITH_LISTS_WF_XML =
     "<Service ID=\"1\" Label=\"ModelledExpressionsWithLists\" TypeName=\"ModelledExpressionsWithLists\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Operation ID=\"0\" Label=\"ModelledExpressionsWithLists\" Name=\"ModelledExpressionsWithLists\">"
   +"    <Input>"
   +"      <Data ID=\"8\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype\">"
   +"        <Target RefID=\"2\"/>"
   +"        <Target RefID=\"12\"/>"
   +"      </Data>"
   +"      <Data ID=\"9\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\">"
   +"        <Target RefID=\"4\"/>"
   +"        <Target RefID=\"12\"/>"
   +"        <Target RefID=\"22\"/>"
   +"      </Data>"
   +"    </Input>"
   +"    <Output/>"
   +"    <Choice ID=\"2\" TypeName=\"BaseChoiceTypeFormula\" TypePath=\"server\">"
   +"      <Source RefID=\"8\"/>"
   +"      <Input>"
   +"        <Data ID=\"24\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype24\"/>"
   +"        <Source RefID=\"8\"/>"
   +"      </Input>"
   +"      <Case ID=\"26\" Label=\"true\" Premise=\"length(%0%.aDatatype)==&quot;1&quot;\">"
   +"        <Assign ID=\"25\"/>"
   +"      </Case>"
   +"      <Case ID=\"28\" Label=\"false\">"
   +"        <Assign ID=\"27\"/>"
   +"      </Case>"
   +"    </Choice>"
   +"    <Choice ID=\"4\" TypeName=\"BaseChoiceTypeFormula\" TypePath=\"server\">"
   +"      <Source RefID=\"9\"/>"
   +"      <Input>"
   +"        <Data ID=\"29\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype29\" IsList=\"true\"/>"
   +"        <Source RefID=\"9\"/>"
   +"      </Input>"
   +"      <Case ID=\"31\" Label=\"true\" Premise=\"length(%0%)==&quot;1&quot;\">"
   +"        <Assign ID=\"30\"/>"
   +"      </Case>"
   +"      <Case ID=\"33\" Label=\"false\">"
   +"        <Assign ID=\"32\"/>"
   +"      </Case>"
   +"    </Choice>"
   +"    <Mappings ID=\"12\" Label=\"Mapping\">"
   +"      <Source RefID=\"8\"/>"
   +"      <Source RefID=\"9\"/>"
   +"      <Target RefID=\"10\"/>"
   +"      <Meta>"
   +"        <FixedDetailOptions>openConfiguration</FixedDetailOptions>"
   +"      </Meta>"
   +"      <Input>"
   +"        <Data ID=\"11\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype11\"/>"
   +"        <Source RefID=\"8\"/>"
   +"      </Input>"
   +"      <Input>"
   +"        <Data ID=\"13\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype13\" IsList=\"true\"/>"
   +"        <Source RefID=\"9\"/>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"14\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype14\"/>"
   +"        <Target RefID=\"10\"/>"
   +"      </Output>"
   +"      <Mapping>%2%.aDatatype~=%1%</Mapping>"
   +"    </Mappings>"
   +"    <Data ID=\"10\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype0\">"
   +"      <Source RefID=\"12\"/>"
   +"    </Data>"
   +"    <Foreach ID=\"22\">"
   +"      <Source RefID=\"9\"/>"
   +"      <Target RefID=\"23\"/>"
   +"      <InputList RefID=\"9\">"
   +"        <Data ID=\"21\" Label=\"adatatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype21\">"
   +"          <Target RefID=\"19\"/>"
   +"        </Data>"
   +"      </InputList>"
   +"      <OutputList RefID=\"23\">"
   +"        <Data ID=\"18\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype_0\">"
   +"          <Source RefID=\"19\"/>"
   +"        </Data>"
   +"      </OutputList>"
   +"      <Mappings ID=\"19\" Label=\"Mapping\">"
   +"        <Source RefID=\"21\"/>"
   +"        <Target RefID=\"18\"/>"
   +"        <Meta>"
   +"          <FixedDetailOptions>openConfiguration</FixedDetailOptions>"
   +"        </Meta>"
   +"        <Input>"
   +"          <Data ID=\"20\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype20\"/>"
   +"          <Source RefID=\"21\"/>"
   +"        </Input>"
   +"        <Output>"
   +"          <Data ID=\"17\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype17\"/>"
   +"          <Target RefID=\"18\"/>"
   +"        </Output>"
   +"        <Mapping>%1%.aDatatype[\"0\"]~=%0%</Mapping>"
   +"      </Mappings>"
   +"    </Foreach>"
   +"    <Data ID=\"23\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype18\" IsList=\"true\">"
   +"      <Source RefID=\"22\"/>"
   +"    </Data>"
   +"    <Assign ID=\"34\"/>"
   +"  </Operation>"
   +"</Service>";
   
   
   protected final static String A_DATATYPE_EXTENSION_XML =
     "<DataType Label=\"a datatype extension\" TypeName=\"ADatatypeExtension\" TypePath=\"xfmg.xfctrl.deployitem\" BaseTypeName=\"ADatatype\" BaseTypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Data Label=\"member variable 3\" VariableName=\"memberVariable3\">"
   +"    <Meta>"
   +"      <Type>String</Type>"
   +"    </Meta>"
   +"  </Data>"
   +"</DataType>";

   
   protected final static String UP_DOWN_CAST_WF_XML = 
     "<Service ID=\"1\" Label=\"UpAndDownCast\" TypeName=\"UpAndDownCast\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Operation ID=\"0\" Label=\"UpAndDownCast\" Name=\"UpAndDownCast\">"
   +"    <Input>"
   +"      <Data ID=\"6\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
   +"        <Target RefID=\"11\"/>"
   +"      </Data>"
   +"      <Data ID=\"13\" Label=\"a datatype extension\" ReferenceName=\"ADatatypeExtension\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatypeExtension\">"
   +"        <Target RefID=\"15\"/>"
   +"      </Data>"
   +"    </Input>"
   +"    <Output/>"
   +"    <Mappings ID=\"11\" Label=\"upcast\">"
   +"      <Source RefID=\"6\"/>"
   +"      <Target RefID=\"5\"/>"
   +"      <Input>"
   +"        <Data ID=\"10\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype10\"/>"
   +"        <Source RefID=\"6\">"
   +"          <Meta>"
   +"            <LinkType>UserConnected</LinkType>"
   +"          </Meta>"
   +"        </Source>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"12\" Label=\"a datatype extension\" ReferenceName=\"ADatatypeExtension\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatypeExtension12\"/>"
   +"        <Target RefID=\"5\"/>"
   +"      </Output>"
   +"      <Mapping>%1%~=%0%</Mapping>"
   +"    </Mappings>"
   +"    <Data ID=\"5\" Label=\"a datatype extension\" ReferenceName=\"ADatatypeExtension\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatypeExtension5\">"
   +"      <Source RefID=\"11\"/>"
   +"    </Data>"
   +"    <Mappings ID=\"15\" Label=\"downcast\">"
   +"      <Source RefID=\"13\"/>"
   +"      <Target RefID=\"9\"/>"
   +"      <Meta>"
   +"        <FixedDetailOptions>openConfiguration</FixedDetailOptions>"
   +"      </Meta>"
   +"      <Input>"
   +"        <Data ID=\"14\" Label=\"a datatype extension\" ReferenceName=\"ADatatypeExtension\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatypeExtension14\"/>"
   +"        <Source RefID=\"13\">"
   +"          <Meta>"
   +"            <LinkType>UserConnected</LinkType>"
   +"          </Meta>"
   +"        </Source>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"16\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype16\"/>"
   +"        <Target RefID=\"9\"/>"
   +"      </Output>"
   +"      <Mapping>%1%~=%0%</Mapping>"
   +"    </Mappings>"
   +"    <Data ID=\"9\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype9\">"
   +"      <Source RefID=\"15\"/>"
   +"    </Data>"
   +"    <Assign ID=\"17\"/>"
   +"  </Operation>"
   +"</Service>";
   
   
   protected final static String PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_XML =
   "<Service ID=\"1\" Label=\"PrimitiveInstanceServiceReturn\" TypeName=\"PrimitiveInstanceServiceReturn\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
     +"  <Meta>"
     +"    <FixedDetailOptions>hideDetailAreas,highDetailsMode</FixedDetailOptions>"
     +"  </Meta>"
     +"  <Operation ID=\"0\" Label=\"PrimitiveInstanceServiceReturn\" Name=\"PrimitiveInstanceServiceReturn\">"
     +"    <Input>"
     +"      <Data ID=\"3\" Label=\"Document\" ReferenceName=\"Document\" ReferencePath=\"xact.templates\" VariableName=\"document\">"
     +"        <Target RefID=\"2\"/>"
     +"      </Data>"
     +"    </Input>"
     +"    <Output/>"
     +"    <Choice ID=\"2\" TypeName=\"BaseChoiceTypeFormula\" TypePath=\"server\">"
     +"      <Source RefID=\"3\"/>"
     +"      <Input>"
     +"        <Data ID=\"4\" Label=\"Document\" ReferenceName=\"Document\" ReferencePath=\"xact.templates\" VariableName=\"document4\"/>"
     +"        <Source RefID=\"3\"/>"
     +"      </Input>"
     +"      <Case ID=\"6\" Label=\"true\" Premise=\"%0%.getCompleteBufferContent()==&quot;Baum&quot;\">"
     +"        <Assign ID=\"5\"/>"
     +"      </Case>"
     +"      <Case ID=\"8\" Label=\"false\">"
     +"        <Assign ID=\"7\"/>"
     +"      </Case>"
     +"    </Choice>"
     +"    <Assign ID=\"9\"/>"
     +"  </Operation>"
     +"</Service>";
   
   
   protected final static String MAP_EXTENSION_INTO_SUBLIST_WF_XML = 
     "<Service ID=\"1\" Label=\"MapExtensionTypeIntoSubList\" TypeName=\"MapExtensionTypeIntoSubList\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
    +"  <Meta>"
    +"    <FixedDetailOptions>hideDetailAreas,highDetailsMode</FixedDetailOptions>"
    +"  </Meta>"
    +"  <Operation ID=\"0\" Label=\"MapExtensionTypeIntoSubList\" Name=\"MapExtensionTypeIntoSubList\">"
    +"    <Input>"
    +"      <Data ID=\"8\" Label=\"a datatype extension\" ReferenceName=\"ADatatypeExtension\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatypeExtension8\">"
    +"        <Target RefID=\"5\"/>"
    +"        <Target RefID=\"10\"/>"
    +"      </Data>"
    +"    </Input>"
    +"    <Output>"
    +"      <Data ID=\"9\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
    +"        <Source RefID=\"10\"/>"
    +"      </Data>"
    +"    </Output>"
    +"    <Mappings ID=\"5\" Label=\"Mapping\">"
    +"      <Source RefID=\"8\"/>"
    +"      <Target RefID=\"7\"/>"
    +"      <Meta>"
    +"        <FixedDetailOptions>openConfiguration</FixedDetailOptions>"
    +"      </Meta>"
    +"      <Input>"
    +"        <Data ID=\"11\" Label=\"a datatype extension\" ReferenceName=\"ADatatypeExtension\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatypeExtension\"/>"
    +"        <Source RefID=\"8\"/>"
    +"      </Input>"
    +"      <Output>"
    +"        <Data ID=\"12\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype\"/>"
    +"        <Target RefID=\"7\"/>"
    +"      </Output>"
    +"      <Mapping>%1%.aDatatype[\"0\"]~=%0%</Mapping>"
    +"    </Mappings>"
    +"    <Data ID=\"7\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype7\">"
    +"      <Source RefID=\"5\"/>"
    +"    </Data>"
    +"    <Assign ID=\"10\">"
    +"      <Source RefID=\"8\"/>"
    +"      <Target RefID=\"9\"/>"
    +"      <Copy>"
    +"        <Source RefID=\"8\"/>"
    +"        <Target RefID=\"9\"/>"
    +"      </Copy>"
    +"    </Assign>"
    +"  </Operation>"
    +"</Service>";
   
   
   protected final static String CALCULATE_WITH_STRINGS_WF_XML =
   "<Service ID=\"1\" Label=\"Calculate with strings\" TypeName=\"CalculateWithStrings\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
   +"  <Operation ID=\"0\" Label=\"Calculate with strings\" Name=\"CalculateWithStrings\">"
   +"    <Input>"
   +"      <Data ID=\"4\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
   +"        <Target RefID=\"7\"/>"
   +"      </Data>"
   +"    </Input>"
   +"    <Output/>"
   +"    <Mappings ID=\"7\" Label=\"Mapping\">"
   +"      <Source RefID=\"4\"/>"
   +"      <Target RefID=\"5\"/>"
   +"      <Meta>"
   +"        <FixedDetailOptions>openConfiguration</FixedDetailOptions>"
   +"      </Meta>"
   +"      <Input>"
   +"        <Data ID=\"6\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype6\"/>"
   +"        <Source RefID=\"4\"/>"
   +"      </Input>"
   +"      <Output>"
   +"        <Data ID=\"8\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype8\"/>"
   +"        <Target RefID=\"5\"/>"
   +"      </Output>"
   +"      <Mapping>%1%.memberVariable3~=%0%.memberVariable1+\"1337\"</Mapping>"
   +"    </Mappings>"
   +"    <Data ID=\"5\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype0\">"
   +"      <Source RefID=\"7\"/>"
   +"    </Data>"
   +"    <Assign ID=\"9\"/>"
   +"  </Operation>"
   +"</Service>";
   
   
   protected final static String A_INSTANCE_SERVICE_GROUP_XML =
   "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"A instance service group\" TypeName=\"AInstanceServiceGroup\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\">"
   +"  <Libraries>AInstanceServiceGroupImpl.jar</Libraries>"
   +"  <Service Label=\"AInstance Service Group\" TypeName=\"AInstanceServiceGroup\">"
   +"    <Operation IsStatic=\"false\" Label=\"a instance service\" Name=\"aInstanceService\">"
   +"      <Input/>"
   +"      <Output/>"
   +"      <SourceCode>"
   +"        <CodeSnippet Type=\"Java\">getImplementationOfInstanceMethods().aInstanceService();</CodeSnippet>"
   +"      </SourceCode>"
   +"    </Operation>"
   +"  </Service>"
   +"</DataType>";
   
   protected final static String LIST_INPUT_WF_XML =
                   "<Service ID=\"1\" Label=\"ListInputWF\" TypeName=\"ListInputWF\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                     +"  <Operation ID=\"0\" Label=\"ListInputWF\" Name=\"ListInputWF\">"
                     +"    <Input>"
                     +"      <Data ID=\"3\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\"/>"
                     +"      <Data ID=\"2\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype2\"/>"
                     +"    </Input>"
                     +"    <Output/>"
                     +"    <Assign ID=\"4\"/>"
                     +"  </Operation>"
                     +"</Service>";
   
   protected final static String LIST_INPUT_FLIPPED_WF_XML =
                   "<Service ID=\"1\" Label=\"ListInputWF\" TypeName=\"ListInputWF\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                     +"  <Operation ID=\"0\" Label=\"ListInputWF\" Name=\"ListInputWF\">"
                     +"    <Input>"
                     +"      <Data ID=\"2\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype2\"/>"
                     +"      <Data ID=\"3\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\"/>"
                     +"    </Input>"
                     +"    <Output/>"
                     +"    <Assign ID=\"4\"/>"
                     +"  </Operation>"
                     +"</Service>";
   
   protected final static String LIST_CALLER_WF_XML =
                 "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"ListCaller\" TypeName=\"ListCaller\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\">"
                   +"  <Operation ID=\"0\" Label=\"ListCaller\" Name=\"ListCaller\">"
                   +"    <Input/>"
                   +"    <Output/>"
                   +"    <ServiceReference ID=\"2\" Label=\"ListInputWF\" ReferenceName=\"ListInputWF\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
                   +"      <Source RefID=\"3\"/>"
                   +"      <Target RefID=\"3\"/>"
                   +"    </ServiceReference>"
                   +"    <Function ID=\"3\" Label=\"List Input WF\">"
                   +"      <Source RefID=\"2\"/>"
                   +"      <Source RefID=\"4\"/>"
                   +"      <Source RefID=\"5\"/>"
                   +"      <Target RefID=\"2\"/>"
                   +"      <Invoke Operation=\"ListInputWF\" ServiceID=\"2\">"
                   +"        <Source RefID=\"4\">"
                   +"          <Meta>"
                   +"            <LinkType>Constant</LinkType>"
                   +"          </Meta>"
                   +"        </Source>"
                   +"        <Source RefID=\"5\">"
                   +"          <Meta>"
                   +"            <LinkType>Constant</LinkType>"
                   +"          </Meta>"
                   +"        </Source>"
                   +"      </Invoke>"
                   +"      <Receive ServiceID=\"2\"/>"
                   +"    </Function>"
                   +"    <Data ID=\"4\" IsList=\"true\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
                   +"      <Target RefID=\"3\"/>"
                   +"    </Data>"
                   +"    <Data ID=\"5\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype2\">"
                   +"      <Target RefID=\"3\"/>"
                   +"      <Data Label=\"a memvar\" VariableName=\"aMemvar\">"
                   +"        <Meta>"
                   +"          <Type>String</Type>"
                   +"        </Meta>"
                   +"      </Data>"
                   +"      <Data Label=\"b memvar\" VariableName=\"bMemvar\">"
                   +"        <Meta>"
                   +"          <Type>String</Type>"
                   +"        </Meta>"
                   +"      </Data>"
                   +"      <Data Label=\"member variable 3\" VariableName=\"memberVariable3\">"
                   +"        <Meta>"
                   +"          <Type>int</Type>"
                   +"        </Meta>"
                   +"      </Data>"
                   +"    </Data>"
                   +"    <Assign/>"
                   +"  </Operation>"
                   +"</Service>";
   
   protected final static String LIST_MAPPING_MISSMATCH_WF_NO_ERRORS_XML = 
                   "<Service ID=\"1\" Label=\"MappingWithListMissmatch\" TypeName=\"MappingWithListMissmatch\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                     +"  <Operation ID=\"0\" Label=\"MappingWithListMissmatch\" Name=\"MappingWithListMissmatch\">"
                     +"    <Input>"
                     +"      <Data ID=\"13\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype13\">"
                     +"        <Target RefID=\"5\"/>"
                     +"      </Data>"
                     +"    </Input>"
                     +"    <Output/>"
                     +"    <Mappings ID=\"5\" Label=\"Mapping\">"
                     +"      <Source RefID=\"13\"/>"
                     +"      <Target RefID=\"4\"/>"
                     +"      <Input>"
                     +"        <Data ID=\"7\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
                     +"        <Source RefID=\"13\"/>"
                     +"      </Input>"
                     +"      <Output>"
                     +"        <Data ID=\"8\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype\"/>"
                     +"        <Target RefID=\"4\"/>"
                     +"      </Output>"
                     +"      <Mapping>%1%.aDatatype[\"0\"]~=%0%</Mapping>"
                     +"    </Mappings>"
                     +"    <Data ID=\"4\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype4\">"
                     +"      <Source RefID=\"5\"/>"
                     +"    </Data>"
                     +"    <Assign ID=\"12\"/>"
                     +"  </Operation>"
                     +"</Service>";
   
   protected final static String LIST_MAPPING_MISSMATCH_WF_LIST_ONTO_SINGLE_XML = 
                   "<Service ID=\"1\" Label=\"MappingWithListMissmatch\" TypeName=\"MappingWithListMissmatch\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                     +"  <Operation ID=\"0\" Label=\"MappingWithListMissmatch\" Name=\"MappingWithListMissmatch\">"
                     +"    <Input>"
                     +"      <Data ID=\"13\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype13\" IsList=\"true\">"
                     +"        <Target RefID=\"5\"/>"
                     +"      </Data>"
                     +"    </Input>"
                     +"    <Output/>"
                     +"    <Mappings ID=\"5\" Label=\"Mapping\">"
                     +"      <Source RefID=\"13\"/>"
                     +"      <Target RefID=\"4\"/>"
                     +"      <Input>"
                     +"        <Data ID=\"7\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
                     +"        <Source RefID=\"13\"/>"
                     +"      </Input>"
                     +"      <Output>"
                     +"        <Data ID=\"8\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype\"/>"
                     +"        <Target RefID=\"4\"/>"
                     +"      </Output>"
                     +"      <Mapping>%1%.aDatatype[\"0\"]~=%0%</Mapping>"
                     +"    </Mappings>"
                     +"    <Data ID=\"4\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype4\">"
                     +"      <Source RefID=\"5\"/>"
                     +"    </Data>"
                     +"    <Assign ID=\"12\"/>"
                     +"  </Operation>"
                     +"</Service>";
   
   protected final static String LIST_MAPPING_MISSMATCH_WF_SINGLE_ONTO_LIST_XML = 
                   "<Service ID=\"1\" Label=\"MappingWithListMissmatch\" TypeName=\"MappingWithListMissmatch\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                     +"  <Operation ID=\"0\" Label=\"MappingWithListMissmatch\" Name=\"MappingWithListMissmatch\">"
                     +"    <Input>"
                     +"      <Data ID=\"13\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype13\">"
                     +"        <Target RefID=\"5\"/>"
                     +"      </Data>"
                     +"    </Input>"
                     +"    <Output/>"
                     +"    <Mappings ID=\"5\" Label=\"Mapping\">"
                     +"      <Source RefID=\"13\"/>"
                     +"      <Target RefID=\"4\"/>"
                     +"      <Input>"
                     +"        <Data ID=\"7\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
                     +"        <Source RefID=\"13\"/>"
                     +"      </Input>"
                     +"      <Output>"
                     +"        <Data ID=\"8\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype\"/>"
                     +"        <Target RefID=\"4\"/>"
                     +"      </Output>"
                     +"      <Mapping>%1%.aDatatype~=%0%</Mapping>"
                     +"    </Mappings>"
                     +"    <Data ID=\"4\" Label=\"d datatype\" ReferenceName=\"DDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"dDatatype4\">"
                     +"      <Source RefID=\"5\"/>"
                     +"    </Data>"
                     +"    <Assign ID=\"12\"/>"
                     +"  </Operation>"
                     +"</Service>";
   
   protected final static String BASE_CHOICE_DT_XML = 
                   "<DataType Label=\"Base Choice\" IsAbstract=\"true\" TypeName=\"BaseChoice\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\"/>";
   
   protected final static String SUB_CHOICE_1_DT_XML = 
                   "<DataType Label=\"Sub Choice 1\" TypeName=\"SubChoice1\" TypePath=\"xfmg.xfctrl.deployitem\" BaseTypeName=\"BaseChoice\" BaseTypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\"/>";
   
   protected final static String SUB_CHOICE_2_DT_XML = 
                   "<DataType Label=\"Sub Choice 2\" TypeName=\"SubChoice2\" TypePath=\"xfmg.xfctrl.deployitem\" BaseTypeName=\"BaseChoice\" BaseTypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\"/>";
   
   protected final static String SUB_CHOICE_2_WITHOUT_BASE_DT_XML = 
                   "<DataType Label=\"Sub Choice 2\" TypeName=\"SubChoice2\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\"/>";
   
   protected final static String BASE_CHOICE_HIERARCHY_WF_XML = 
                   "<Service ID=\"1\" Label=\"Base Choice Hierarchy\" TypeName=\"BaseChoiceHierarchy\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                     +"  <Operation ID=\"0\" Label=\"Base Choice Hierarchy\" Name=\"BaseChoiceHierarchy\">"
                     +"    <Input>"
                     +"      <Data ID=\"8\" Label=\"Base Choice\" ReferenceName=\"BaseChoice\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"baseChoice8\">"
                     +"        <Target RefID=\"9\"/>"
                     +"        <Target RefID=\"3\"/>"
                     +"        <Target RefID=\"5\"/>"
                     +"        <Target RefID=\"7\"/>"
                     +"      </Data>"
                     +"    </Input>"
                     +"    <Output/>"
                     +"    <Choice ID=\"9\" TypeName=\"BaseChoiceTypeSubclasses\" TypePath=\"server\">"
                     +"      <Source RefID=\"8\"/>"
                     +"      <Input>"
                     +"        <Data ID=\"10\" Label=\"Base Choice\" ReferenceName=\"BaseChoice\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"baseChoice\"/>"
                     +"        <Source RefID=\"8\"/>"
                     +"      </Input>"
                     +"      <Case ID=\"12\" Label=\"Sub Choice 2\" Premise=\"xfmg.xfctrl.deployitem.SubChoice2\">"
                     +"        <Assign ID=\"7\"/>"
                     +"        <Assign ID=\"11\"/>"
                     +"      </Case>"
                     +"      <Case ID=\"14\" Label=\"Sub Choice 1\" Premise=\"xfmg.xfctrl.deployitem.SubChoice1\">"
                     +"        <Assign ID=\"5\"/>"
                     +"        <Assign ID=\"13\"/>"
                     +"      </Case>"
                     +"      <Case ID=\"16\" Label=\"Base Choice\" Premise=\"xfmg.xfctrl.deployitem.BaseChoice\">"
                     +"        <Assign ID=\"3\"/>"
                     +"        <Assign ID=\"15\"/>"
                     +"      </Case>"
                     +"    </Choice>"
                     +"    <Assign ID=\"17\"/>"
                     +"  </Operation>"
                     +"</Service>";
   
   protected final static String LIST_FUNCTIONS_WF_XML =
                   "<Service ID=\"1\" Label=\"List Functions\" TypeName=\"ListFunctions\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"List Functions\" Name=\"ListFunctions\">"+
"    <Input>"+
"      <Data ID=\"8\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\">"+
"        <Target RefID=\"7\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output>"+
"      <Data ID=\"17\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype17\" IsList=\"true\">"+
"        <Source RefID=\"18\"/>"+
"      </Data>"+
"    </Output>"+
"    <Mappings ID=\"7\" Label=\"Mapping\">"+
"      <Source RefID=\"8\"/>"+
"      <Target RefID=\"10\"/>"+
"      <Input>"+
"        <Data ID=\"5\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype5\" IsList=\"true\"/>"+
"        <Source RefID=\"8\"/>"+
"      </Input>"+
"      <Input>"+
"        <Data ID=\"6\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype6\" IsList=\"true\"/>"+
"        <Source RefID=\"8\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"9\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype9\" IsList=\"true\"/>"+
"        <Target RefID=\"10\"/>"+
"      </Output>"+
"      <Mapping>%2%~=concatlists(%0%,%1%)</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"10\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype10\" IsList=\"true\">"+
"      <Source RefID=\"7\"/>"+
"      <Target RefID=\"16\"/>"+
"    </Data>"+
"    <Mappings ID=\"16\" Label=\"Mapping\">"+
"      <Source RefID=\"10\"/>"+
"      <Target RefID=\"15\"/>"+
"      <Input>"+
"        <Data ID=\"13\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype13\" IsList=\"true\"/>"+
"        <Source RefID=\"10\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"14\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype14\" IsList=\"true\"/>"+
"        <Target RefID=\"15\"/>"+
"      </Output>"+
"      <Mapping>%1%~=append(%0%,%0%[\"0\"])</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"15\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype15\" IsList=\"true\">"+
"      <Source RefID=\"16\"/>"+
"      <Target RefID=\"18\"/>"+
"    </Data>"+
"    <Assign ID=\"18\">"+
"      <Source RefID=\"15\"/>"+
"      <Target RefID=\"17\"/>"+
"      <Copy>"+
"        <Source RefID=\"15\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"        <Target RefID=\"17\"/>"+
"      </Copy>"+
"    </Assign>"+
"  </Operation>"+
"</Service>";
   
   
   protected final static String LIST_FUNCTIONS_2_WF_XML =
                   "<Service ID=\"1\" Label=\"List Functions 2\" TypeName=\"ListFunctions2\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                   "  <Operation ID=\"0\" Label=\"List Functions 2\" Name=\"ListFunctions2\">"+
                   "    <Input>"+
                   "      <Data ID=\"8\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\" IsList=\"true\">"+
                   "        <Target RefID=\"22\"/>"+
                   "      </Data>"+
                   "      <Data ID=\"23\" Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype23\">"+
                   "        <Target RefID=\"22\"/>"+
                   "      </Data>"+
                   "    </Input>"+
                   "    <Output>"+
                   "      <Data ID=\"17\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype17\" IsList=\"true\"/>"+
                   "    </Output>"+
                   "    <Mappings ID=\"22\" Label=\"Mapping\">"+
                   "      <Source RefID=\"23\"/>"+
                   "      <Source RefID=\"8\"/>"+
                   "      <Target RefID=\"21\"/>"+
                   "      <Input>"+
                   "        <Data ID=\"19\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype19\" IsList=\"true\"/>"+
                   "        <Source RefID=\"8\"/>"+
                   "      </Input>"+
                   "      <Input>"+
                   "        <Data Label=\"b datatype\" ReferenceName=\"BDatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"bDatatype\"/>"+
                   "        <Source RefID=\"23\"/>"+
                   "      </Input>"+
                   "      <Output>"+
                   "        <Data ID=\"20\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype20\" IsList=\"true\"/>"+
                   "        <Target RefID=\"21\"/>"+
                   "      </Output>"+
                   "      <Mapping>%2%~=append(%0%,%1%.aDatatype)</Mapping>"+
                   "    </Mappings>"+
                   "    <Data ID=\"21\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype21\" IsList=\"true\">"+
                   "      <Source RefID=\"22\"/>"+
                   "    </Data>"+
                   "    <Assign ID=\"18\"/>"+
                   "  </Operation>"+
                   "</Service>";
   
   protected final static String COMPLEX_OBJECT_TO_STRING_XML =
                   "<Service ID=\"1\" Label=\"Complex Object to String\" TypeName=\"ComplexObjectToString\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                   +"  <Operation ID=\"0\" Label=\"Complex Object to String\" Name=\"ComplexObjectToString\">"
                   +"    <Input>"
                   +"      <Data ID=\"4\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
                   +"        <Target RefID=\"7\"/>"
                   +"      </Data>"
                   +"    </Input>"
                   +"    <Output/>"
                   +"    <Mappings ID=\"7\" Label=\"Mapping\">"
                   +"      <Source RefID=\"4\"/>"
                   +"      <Target RefID=\"5\"/>"
                   +"      <Input>"
                   +"        <Data ID=\"6\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype6\"/>"
                   +"        <Source RefID=\"4\"/>"
                   +"      </Input>"
                   +"      <Output>"
                   +"        <Data ID=\"8\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype8\"/>"
                   +"        <Target RefID=\"5\"/>"
                   +"      </Output>"
                   +"      <Mapping>%1%.memberVariable1~=concat(\"\",%0%)</Mapping>"
                   +"    </Mappings>"
                   +"    <Data ID=\"5\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype0\">"
                   +"      <Source RefID=\"7\"/>"
                   +"    </Data>"
                   +"    <Assign ID=\"9\"/>"
                   +"  </Operation>"
                   +"</Service>";
   
   
   protected final static String XYNA_EXCEPTION_BASE_XML =
                   "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\" Name=\"Base\" Version=\"1.8\">"+
"  <ExceptionType BaseTypeName=\"XynaException\" BaseTypePath=\"core.exception\" Code=\"\" IsAbstract=\"true\" Label=\"Xyna Exception Base\" TypeName=\"XynaExceptionBase\" TypePath=\"core.exception\">"+
"  </ExceptionType>"+
" </ExceptionStore>";
   
   protected final static String XYNA_EXCEPTION_XML =
                   "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\" Name=\"Xyna Exception\" Version=\"1.8\">"+
"  <ExceptionType BaseTypeName=\"Exception\" BaseTypePath=\"core.exception\" Code=\"\" IsAbstract=\"true\" Label=\"Server Exception\" TypeName=\"XynaException\" TypePath=\"core.exception\">"+
"  </ExceptionType>"+
" </ExceptionStore>";
   
   protected final static String EXCEPTION_XML =
                   "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\" Name=\"Exception\" Version=\"1.8\">"+
"  <ExceptionType Code=\"\" IsAbstract=\"true\" Label=\"Exception\" TypeName=\"Exception\" TypePath=\"core.exception\">"+
"  </ExceptionType>"+
" </ExceptionStore>";
   
}

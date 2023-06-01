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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.HashMap;
import java.util.Map;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.gip.xyna.xfmg.xfctrl.deployitem.TestDeploymentItemBuildSetup;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;


public class CastAndNewCodeTest extends TestDeploymentItemBuildSetup {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dism = null;
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(DT_BASE_FQNAME, DT_BASE_XML);
    testXMOM.put(DT_SUB1_FQNAME, DT_SUB1_XML);
    testXMOM.put(DT_ABSTRACT_SUB_FQNAME, DT_ABSTRACT_SUB_XML);
    testXMOM.put(DT_SUB2_FQNAME, DT_SUB2_XML);
    testXMOM.put(DT_SUB3_FQNAME, DT_SUB3_XML);
    testXMOM.put(DT_WRAPPER_FQNAME, DT_WRAPPER_XML);
    testXMOM.put(DT_LIST_WRAPPER_FQNAME, DT_LIST_WRAPPER_XML);
    testXMOM.put(DT_INSTANCE_SERVICE_PROVIDER_FQNAME, DT_INSTANCE_SERVICE_PROVIDER_XML);
    testXMOM.put(DT_EXTENDED_SERVICE_PROVIDER_FQNAME, DT_EXTENDED_SERVICE_PROVIDER_XML);
    testXMOM.put(EX_BASE_FQNAME, EX_BASE_XML);
    testXMOM.put(EX_SUB1_FQNAME, EX_SUB1_XML);
    setupWorkspace(testXMOM);
  }
  
  @Override
  protected DeploymentItemRegistry getRegistry() {
    return null;
  }
  
  @Test
  public void test_WF_SIMPLE_NEW() throws Exception {
    String fqName = WF_SIMPLE_NEW_FQNAME;
    String xml = WF_SIMPLE_NEW_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE() throws Exception {
    String fqName = WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME;
    String xml = WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE() throws Exception {
    String fqName = WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_FQNAME;
    String xml = WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT() throws Exception {
    String fqName = WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_FQNAME;
    String xml = WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_CAST_INSTANCE_SERVICE_IN_AND_OUT() throws Exception {
    String fqName = WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_FQNAME;
    String xml = WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_XML;
    setupWorkspace(fqName, xml); // TODO generated code contains an unneeded cast to Sub2
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION() throws Exception {
    String fqName = WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_FQNAME;
    String xml = WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_CAST_FOR_PRIMITVIE_ASSIGN() throws Exception {
    String fqName = WF_CAST_FOR_PRIMITVIE_ASSIGN_FQNAME;
    String xml = WF_CAST_FOR_PRIMITVIE_ASSIGN_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE() throws Exception {
    String fqName = WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_FQNAME;
    String xml = WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_SIMPLE_CAST_DISTRIBUTION() throws Exception {
    String fqName = WF_SIMPLE_CAST_DISTRIBUTION_FQNAME;
    String xml = WF_SIMPLE_CAST_DISTRIBUTION_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_INHERIT_CAST_FROM_ASSIGN() throws Exception {
    String fqName = WF_INHERIT_CAST_FROM_ASSIGN_FQNAME;
    String xml = WF_INHERIT_CAST_FROM_ASSIGN_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  @Test
  public void test_WF_SIMPLE_DOWNCAST_ASSIGNMENT() throws Exception {
    String fqName = WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME;
    String xml = WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  
  @Test
  public void test_WF_WEIRED_CL_CASE() throws Exception {
    String fqName = WF_WEIRED_CL_CASE_FQNAME;
    String xml = WF_WEIRED_CL_CASE_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  
  @Test
  public void test_WF_NEW_EXCEPTION_AND_INITIALIZATION() throws Exception {
    String fqName = WF_NEW_EXCEPTION_AND_INITIALIZATION_FQNAME;
    String xml = WF_NEW_EXCEPTION_AND_INITIALIZATION_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  
  @Test
  public void test_WF_IMPLICIT_EXCPTION_CAST() throws Exception {
    String fqName = WF_IMPLICIT_EXCPTION_CAST_FQNAME;
    String xml = WF_IMPLICIT_EXCPTION_CAST_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  
  @Test
  public void test_WF_CAST_EXCEPTION_FOR_MEMVAR() throws Exception {
    String fqName = WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME;
    String xml = WF_CAST_EXCEPTION_FOR_MEMVAR_XML;
    setupWorkspace(fqName, xml);
    //System.out.println(getCode(fqName, XMOMType.WORKFLOW));
    assertNotNull(getCode(fqName, XMOMType.WORKFLOW));
  }
  
  
  private static CharSequence getCode(String fqName, XMOMType type) throws Exception {
    GenerationBase gb = null;
    switch (type) {
      case DATATYPE :
        gb = DOM.getOrCreateInstance(fqName, new GenerationBaseCache(), TestDeploymentItemBuildSetup.TEST_REVISION);
        break;
      case WORKFLOW :
        gb = WF.getOrCreateInstance(fqName, new GenerationBaseCache(), TestDeploymentItemBuildSetup.TEST_REVISION);
        break;
      default :
        throw new IllegalArgumentException("Unexpected type: " + type);
    }
    JavaFileObject jfo = gb.generateCode(false);
    if (gb.hasError()) {
      Throwable t = gb.getExceptionCause();
      t.printStackTrace();
      fail();
    }
    return jfo.getCharContent(true);
    
  }

  // ############################################################
  //                     Base Datatypes
  // ############################################################
  public final static String DT_BASE_FQNAME = "bg.test.xfl.newAndCast.Base";
  public final static String DT_BASE_XML = 
                  "<DataType Label=\"Base\" IsAbstract=\"true\" TypeName=\"Base\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                                  "  <Data Label=\"a String in Abstract\" VariableName=\"aStringInAbstract\">"+
                                  "    <Meta>"+
                                  "      <Type>String</Type>"+
                                  "    </Meta>"+
                                  "  </Data>"+
                                  "</DataType>";
  
  public final static String DT_SUB1_FQNAME = "bg.test.xfl.newAndCast.Sub1";
  public final static String DT_SUB1_XML = 
                  "<DataType Label=\"Sub 1\" TypeName=\"Sub1\" TypePath=\"bg.test.xfl.newAndCast\" BaseTypeName=\"Base\" BaseTypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\"/>";
  
  public final static String DT_ABSTRACT_SUB_FQNAME = "bg.test.xfl.newAndCast.AbstractSub";
  public final static String DT_ABSTRACT_SUB_XML = 
                  "<DataType Label=\"Abstract Sub\" IsAbstract=\"true\" TypeName=\"AbstractSub\" TypePath=\"bg.test.xfl.newAndCast\" BaseTypeName=\"Base\" BaseTypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                  "  <Data Label=\"String in Abstract Sub\" VariableName=\"stringInAbstractSub\">"+
                  "    <Meta>"+
                  "      <Type>String</Type>"+
                  "    </Meta>"+
                  "  </Data>"+
                  "</DataType>";
  
  public final static String DT_SUB2_FQNAME = "bg.test.xfl.newAndCast.Sub2";
  public final static String DT_SUB2_XML = 
                  "<DataType Label=\"Sub 2\" TypeName=\"Sub2\" TypePath=\"bg.test.xfl.newAndCast\" BaseTypeName=\"AbstractSub\" BaseTypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                  "  <Meta>"+
                  "    <IsServiceGroupOnly>false</IsServiceGroupOnly>"+
                  "  </Meta>"+
                  "  <Data Label=\"a String\" VariableName=\"aString\">"+
                  "    <Meta>"+
                  "      <Type>String</Type>"+
                  "    </Meta>"+
                  "  </Data>"+
                  "  <Data Label=\"a long\" VariableName=\"aLong\">"+
                  "    <Meta>"+
                  "      <Type>long</Type>"+
                  "    </Meta>"+
                  "  </Data>"+
                  "</DataType>";
  
  public final static String DT_SUB3_FQNAME = "bg.test.xfl.newAndCast.Sub3";
  public final static String DT_SUB3_XML = 
  "<DataType Label=\"Sub3\" TypeName=\"Sub3\" TypePath=\"bg.test.xfl.newAndCast\" BaseTypeName=\"Base\" BaseTypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                  "  <Data Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\"/>"+
                  "</DataType>";
  
  public final static String DT_WRAPPER_FQNAME = "bg.test.xfl.newAndCast.Wrapper";
  public final static String DT_WRAPPER_XML =
                  "<DataType Label=\"Wrapper\" TypeName=\"Wrapper\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                  "  <Data Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\"/>"+
                  "  <Data Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1\"/>"+
                  "  <Data Label=\"Sub 2\" ReferenceName=\"Sub2\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub2\"/>"+
                  "  <Data Label=\"Sub 3\" ReferenceName=\"Sub3\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub3\"/>"+
                  "  <Data Label=\"Abstract Sub\" ReferenceName=\"AbstractSub\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"abstractSub\"/>"+
                  "</DataType>";
  
  public final static String DT_LIST_WRAPPER_FQNAME = "bg.test.xfl.newAndCast.ListWrapper";
  public final static String DT_LIST_WRAPPER_XML =
                  "<DataType Label=\"ListWrapper\" TypeName=\"ListWrapper\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                  "  <Data Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\" IsList=\"true\"/>"+
                  "</DataType>";
  
  public final static String DT_INSTANCE_SERVICE_PROVIDER_FQNAME = "bg.test.xfl.newAndCast.InstanceServiceProvider";
  public final static String DT_INSTANCE_SERVICE_PROVIDER_XML =
                  "<DataType Label=\"Instance Service Provider\" TypeName=\"InstanceServiceProvider\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Meta>"+
"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"+
"  </Meta>"+
"  <Service Label=\"Instance Service Provider\" TypeName=\"InstanceServiceProvider\">"+
"    <Operation Label=\"Service\" IsStatic=\"false\" Name=\"service\">"+
"      <Input>"+
"        <Data Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\"/>"+
"      </Output>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">return null;</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"  </Service>"+
"</DataType>";
  
  public final static String DT_EXTENDED_SERVICE_PROVIDER_FQNAME = "bg.test.xfl.newAndCast.ExtendedServiceProvider";
  public final static String DT_EXTENDED_SERVICE_PROVIDER_XML =
  "<DataType Label=\"Extended Service Provider\" TypeName=\"ExtendedServiceProvider\" TypePath=\"bg.test.xfl.newAndCast\" BaseTypeName=\"InstanceServiceProvider\" BaseTypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
  "  <Service Label=\"Extended Service Provider\" TypeName=\"ExtendedServiceProvider\">"+
  "    <Operation ID=\"1\" Label=\"Service\" IsStatic=\"false\" Name=\"extendedService\">"+
  "      <Input/>"+
  "      <Output>"+
  "        <Data ID=\"0\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\"/>"+
  "      </Output>"+
  "      <SourceCode>"+
  "        <CodeSnippet Type=\"Java\">return getImplementationOfInstanceMethods().service_1();</CodeSnippet>"+
  "      </SourceCode>"+
  "    </Operation>"+
  "  </Service>"+
  "</DataType>";
  
  public final static String EX_BASE_FQNAME = "bg.test.xfl.newAndCast.BaseException";
  public final static String EX_BASE_XML =
    "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\" Name=\"ExceptionStore\" Version=\"1.8\">"+
"  <ExceptionType BaseTypeName=\"XynaExceptionBase\" BaseTypePath=\"core.exception\" IsAbstract=\"true\" Label=\"Base Exception\" TypeName=\"BaseException\" TypePath=\"bg.test.xfl.newAndCast\">"+
"    <Data Label=\"Abstract String in Base\" VariableName=\"abstractStringInBase\">"+
"      <Meta>"+
"        <Type>String</Type>"+
"      </Meta>"+
"    </Data>"+
"    <Data Label=\"Abstract boolean in Base\" VariableName=\"abstractBooleanInBase\">"+
"      <Meta>"+
"        <Type>boolean</Type>"+
"      </Meta>"+
"    </Data>"+
"  </ExceptionType>"+
"</ExceptionStore>";
  
  public final static String EX_SUB1_FQNAME = "bg.test.xfl.newAndCast.Sub1Exception";
  public final static String EX_SUB1_XML =
    "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\" Name=\"ExceptionStore\" Version=\"1.8\">"+
"  <ExceptionType BaseTypeName=\"BaseException\" BaseTypePath=\"bg.test.xfl.newAndCast\" Code=\"DEVEL-00000\" Label=\"Sub1 Exception\" TypeName=\"Sub1Exception\" TypePath=\"bg.test.xfl.newAndCast\">"+
"    <Data Label=\"String in Sub1 Exception\" VariableName=\"stringInSub1Exception\">"+
"      <Meta>"+
"        <Type>String</Type>"+
"      </Meta>"+
"    </Data>"+
"    <MessageText Language=\"DE\">Msg 1: %1%    2: %2%   3: %3% (de)</MessageText>"+
"    <MessageText Language=\"EN\">Msg 1: %1%    2: %2%   3: %3% (en)</MessageText>"+
"  </ExceptionType>"+
"</ExceptionStore>";
                    
 // ############################################################
 //                     TestCase WF-XML
 // ############################################################
  
  public final static String WF_SIMPLE_NEW_FQNAME = "bg.test.xfl.newAndCast.NewWF1";
  public final static String WF_SIMPLE_NEW_XML =
                  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"NewWF1\" TypeName=\"NewWF1\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\">"+
"  <Operation ID=\"0\" Label=\"NewWF1\" Name=\"NewWF1\">"+
"    <Input/>"+
"    <Output/>"+
"    <Mappings ID=\"15\" Label=\"Mapping\">"+
"      <Target RefID=\"19\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Output>"+
"        <Data ID=\"14\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base14\"/>"+
"        <Target RefID=\"19\"/>"+
"      </Output>"+
"      <Mapping>%0%~=new(\""+ DT_SUB2_FQNAME +"\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"19\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base19\">"+
"      <Source RefID=\"15\"/>"+
"      <Target RefID=\"17\"/>"+
"      <Target RefID=\"20\"/>"+
"    </Data>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME = "bg.test.xfl.newAndCast.CastTargetAndAssignPrimitive";
  public final static String WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_XML = 
  "<Service ID=\"1\" Label=\"CastTargetAndAssignPrimitive\" TypeName=\"CastTargetAndAssignPrimitive\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Meta>"+
"    <FixedDetailOptions>highDetailsMode,showDetailAreas</FixedDetailOptions>"+
"  </Meta>"+
"  <Operation ID=\"0\" Name=\"default\">"+
"    <Input/>"+
"    <Output/>"+
"    <Mappings ID=\"6\" Label=\"Mapping\">"+
"      <Target RefID=\"4\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Output>"+
"        <Data ID=\"5\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base5\"/>"+
"        <Target RefID=\"4\"/>"+
"      </Output>"+
"      <Mapping>%0%#cast(\"" + DT_SUB2_FQNAME +"\").aString~=\"Täst\"</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"4\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\">"+
"      <Source RefID=\"6\"/>"+
"    </Data>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_FQNAME = "bg.test.xfl.newAndCast.CastWF1";
  public final static String WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_XML =
                  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"CastWF1\" TypeName=\"CastWF1\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\">"+
"  <Operation ID=\"0\" Label=\"CastWF1\" Name=\"CastWF1\">"+
"    <Input/>"+
"    <Output/>"+
"    <Mappings ID=\"15\" Label=\"Mapping\">"+
"      <Target RefID=\"19\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Output>"+
"        <Data ID=\"14\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base14\"/>"+
"        <Target RefID=\"19\"/>"+
"      </Output>"+
"      <Mapping>%0%~=new(\""+ DT_SUB2_FQNAME +"\")</Mapping>"+
"    </Mappings>"+
"    <Mappings ID=\"17\" Label=\"Mapping\">"+
"      <Source RefID=\"19\"/>"+
"      <Target RefID=\"7\"/>"+
"      <Input>"+
"        <Data ID=\"16\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base16\"/>"+
"        <Source RefID=\"19\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"18\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper18\"/>"+
"        <Target RefID=\"7\"/>"+
"      </Output>"+
"      <Mapping>%1%.sub2~=%0%#cast(\""+ DT_SUB2_FQNAME +"\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"7\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
"      <Source RefID=\"17\"/>"+
"    </Data>"+
"    <Mappings ID=\"20\" Label=\"Mapping\">"+
"      <Source RefID=\"19\"/>"+
"      <Target RefID=\"13\"/>"+
"      <Input>"+
"        <Data Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\"/>"+
"        <Source RefID=\"19\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"21\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper21\"/>"+
"        <Target RefID=\"13\"/>"+
"      </Output>"+
"      <Mapping>%1%.sub1~=%0%#cast(\""+ DT_SUB1_FQNAME +"\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"13\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper13\">"+
"      <Source RefID=\"20\"/>"+
"    </Data>"+
"    <Data ID=\"19\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base19\">"+
"      <Source RefID=\"15\"/>"+
"      <Target RefID=\"17\"/>"+
"      <Target RefID=\"20\"/>"+
"    </Data>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_FQNAME = "bg.test.xfl.newAndCast.CastWF2";
  public final static String WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_XML =
                  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"CastWF2\" TypeName=\"CastWF2\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\">"+
"  <Meta>"+
"    <FixedDetailOptions>highDetailsMode,hideDetailAreas</FixedDetailOptions>"+
"  </Meta>"+
"  <Operation ID=\"0\" Label=\"CastWF2\" Name=\"CastWF2\">"+
"    <Input/>"+
"    <Output/>"+
"    <Mappings ID=\"6\" Label=\"Mapping\">"+
"      <Target RefID=\"11\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Output>"+
"        <Data Label=\"List Wrapper\" ReferenceName=\"ListWrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"listWrapper5\"/>"+
"        <Target RefID=\"11\"/>"+
"      </Output>"+
"      <Mapping>%0%.base[\"0\"]~=new(\"bg.test.xfl.newAndCast.Sub2\")</Mapping>"+
"    </Mappings>"+
"    <Mappings ID=\"13\" Label=\"Mapping\">"+
"      <Source RefID=\"11\"/>"+
"      <Target RefID=\"10\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"12\" Label=\"List Wrapper\" ReferenceName=\"ListWrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"listWrapper12\"/>"+
"        <Source RefID=\"11\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"14\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper14\"/>"+
"        <Target RefID=\"10\"/>"+
"      </Output>"+
"      <Mapping>%1%.sub2~=%0%.base[\"0\"]#cast(\"bg.test.xfl.newAndCast.Sub2\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"10\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
"      <Source RefID=\"13\"/>"+
"    </Data>"+
"    <Data ID=\"11\" Label=\"List Wrapper\" ReferenceName=\"ListWrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"listWrapper\">"+
"      <Source RefID=\"6\"/>"+
"      <Target RefID=\"13\"/>"+
"    </Data>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_FQNAME = "bg.test.xfl.newAndCast.CastWF3";
  public final static String WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_XML =
                  "<Service ID=\"1\" Label=\"CastWF3\" TypeName=\"CastWF3\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF3\" Name=\"CastWF3\">"+
"    <Input>"+
"      <Data ID=\"15\" Label=\"Instance Service Provider\" ReferenceName=\"InstanceServiceProvider\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"instanceServiceProvider\">"+
"        <Target RefID=\"14\"/>"+
"      </Data>"+
"      <Data ID=\"16\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\">"+
"        <Target RefID=\"14\"/>"+
"      </Data>"+
"      <Data ID=\"21\" Label=\"List Wrapper\" ReferenceName=\"ListWrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"listWrapper\"/>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"14\" Label=\"invoke service\">"+
"      <Source RefID=\"15\"/>"+
"      <Source RefID=\"16\"/>"+
"      <Target RefID=\"12\"/>"+
"      <Input>"+
"        <Data ID=\"17\" Label=\"Instance Service Provider\" ReferenceName=\"InstanceServiceProvider\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"instanceServiceProvider17\"/>"+
"        <Source RefID=\"15\"/>"+
"      </Input>"+
"      <Input>"+
"        <Data ID=\"18\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base18\"/>"+
"        <Source RefID=\"16\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"13\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper13\"/>"+
"        <Target RefID=\"12\"/>"+
"      </Output>"+
"      <Mapping>%2%.sub2~=%0%.service(%1%#cast(\"bg.test.xfl.newAndCast.Sub1\"))#cast(\"bg.test.xfl.newAndCast.Sub2\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"12\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
"      <Source RefID=\"14\"/>"+
"    </Data>"+
"    <Assign ID=\"9\"/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_FQNAME = "bg.test.xfl.newAndCast.CastWF4";
  public final static String WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_XML =
                  "<Service ID=\"1\" Label=\"CastWF4\" TypeName=\"CastWF4\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF4\" Name=\"CastWF4\">"+
"    <Input>"+
"      <Data ID=\"29\" Label=\"Instance Service Provider\" ReferenceName=\"InstanceServiceProvider\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"extendedServiceProvider\">"+
"        <Target RefID=\"32\"/>"+
"      </Data>"+
"      <Data Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\"/>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"32\" Label=\"Mapping\">"+
"      <Source RefID=\"29\"/>"+
"      <Target RefID=\"30\"/>"+
"      <Input>"+
"        <Data ID=\"31\" Label=\"Instance Service Provider\" ReferenceName=\"InstanceServiceProvider\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"extendedServiceProvider31\"/>"+
"        <Source RefID=\"29\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"33\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper33\"/>"+
"        <Target RefID=\"30\"/>"+
"      </Output>"+
"      <Mapping>%1%.base~=%0%#cast(\"" + DT_EXTENDED_SERVICE_PROVIDER_FQNAME + "\").extendedService()</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"30\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper30\">"+
"      <Source RefID=\"32\"/>"+
"    </Data>"+
"    <Assign ID=\"9\"/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_CAST_FOR_PRIMITVIE_ASSIGN_FQNAME = "bg.test.xfl.newAndCast.CastWF6";
  public final static String WF_CAST_FOR_PRIMITVIE_ASSIGN_XML =
  "<Service ID=\"1\" Label=\"CastWF6\" TypeName=\"CastWF6\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
  "  <Operation ID=\"0\" Label=\"CastWF6\" Name=\"CastWF6\">"+
  "    <Input>"+
  "      <Data ID=\"10\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
  "        <Target RefID=\"7\"/>"+
  "      </Data>"+
  "    </Input>"+
  "    <Output/>"+
  "    <Mappings ID=\"7\" Label=\"Mapping\">"+
  "      <Source RefID=\"10\"/>"+
  "      <Target RefID=\"12\"/>"+
  "      <Input>"+
  "        <Data ID=\"9\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper9\"/>"+
  "        <Source RefID=\"10\"/>"+
  "      </Input>"+
  "      <Output>"+
  "        <Data ID=\"11\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper11\"/>"+
  "        <Target RefID=\"12\"/>"+
  "      </Output>"+
  "      <Mapping>%1%.base#cast(\"bg.test.xfl.newAndCast.Sub2\").stringInAbstractSub=%0%.base.aStringInAbstract</Mapping>"+
  "    </Mappings>"+
  "    <Data ID=\"12\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper12\">"+
  "      <Source RefID=\"7\"/>"+
  "    </Data>"+
  "    <Assign/>"+
  "  </Operation>"+
  "</Service>";
  
  public final static String WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_FQNAME = "bg.test.xfl.newAndCast.CastWF7";
  public final static String WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_XML =
  "<Service ID=\"1\" Label=\"CastWF7\" TypeName=\"CastWF7\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF7\" Name=\"CastWF7\">"+
"    <Input>"+
"      <Data ID=\"12\" Label=\"Instance Service Provider\" ReferenceName=\"InstanceServiceProvider\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"instanceServiceProvider\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"3\">"+
"      <Source RefID=\"12\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Meta>"+
"        <IsTemplate>true</IsTemplate>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"11\" Label=\"Instance Service Provider\" ReferenceName=\"InstanceServiceProvider\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"instanceServiceProvider11\"/>"+
"        <Source RefID=\"12\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"13\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart\"/>"+
"        <Target RefID=\"2\"/>"+
"      </Output>"+
"      <Mapping>%1%.text=concat(\"Result: \",%0%#cast(\"bg.test.xfl.newAndCast.ExtendedServiceProvider\").extendedService().aStringInAbstract,\"\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"2\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart2\">"+
"      <Source RefID=\"3\"/>"+
"    </Data>"+
"    <Assign ID=\"14\"/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_SIMPLE_CAST_DISTRIBUTION_FQNAME = "bg.test.xfl.newAndCast.CastWF9";
  public final static String WF_SIMPLE_CAST_DISTRIBUTION_XML =
  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"CastWF9\" TypeName=\"CastWF9\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\">"+
"  <Meta>"+
"    <FixedDetailOptions>highDetailsMode,showDetailAreas</FixedDetailOptions>"+
"  </Meta>"+
"  <Operation ID=\"0\" Label=\"CastWF9\" Name=\"CastWF9\">"+
"    <Input>"+
"      <Data ID=\"6\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\">"+
"        <Target RefID=\"8\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"8\" Label=\"Mapping\">"+
"      <Source RefID=\"6\"/>"+
"      <Target RefID=\"10\"/>"+
"      <Target RefID=\"12\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>openConfiguration,FlatMode</FixedDetailOptions>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"7\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base7\"/>"+
"        <Source RefID=\"6\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1\"/>"+
"        <Target RefID=\"10\"/>"+
"      </Output>"+
"      <Output>"+
"        <Data ID=\"11\" Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub111\"/>"+
"        <Target RefID=\"12\"/>"+
"      </Output>"+
"      <Mapping>%1%.aStringInAbstract=%0%#cast(\"bg.test.xfl.newAndCast.Sub2\").aLong</Mapping>"+
"      <Mapping>%2%.aStringInAbstract=%0%.aString</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"10\" Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub110\">"+
"      <Source RefID=\"8\"/>"+
"    </Data>"+
"    <Data ID=\"12\" Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub112\">"+
"      <Source RefID=\"8\"/>"+
"    </Data>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_INHERIT_CAST_FROM_ASSIGN_FQNAME = "bg.test.xfl.newAndCast.CastWF10";
  public final static String WF_INHERIT_CAST_FROM_ASSIGN_XML =
  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"CastWF10\" TypeName=\"CastWF10\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\">"+
  "  <Operation ID=\"0\" Label=\"CastWF10\" Name=\"CastWF10\">"+
  "    <Input>"+
  "      <Data ID=\"4\" Label=\"Sub 2\" ReferenceName=\"Sub2\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub2\">"+
  "        <Target RefID=\"7\"/>"+
  "      </Data>"+
  "    </Input>"+
  "    <Output/>"+
  "    <Mappings ID=\"7\" Label=\"Mapping\">"+
  "      <Source RefID=\"4\"/>"+
  "      <Target RefID=\"5\"/>"+
  "      <Input>"+
  "        <Data ID=\"6\" Label=\"Sub 2\" ReferenceName=\"Sub2\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub26\"/>"+
  "        <Source RefID=\"4\"/>"+
  "      </Input>"+
  "      <Output>"+
  "        <Data ID=\"8\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper8\"/>"+
  "        <Target RefID=\"5\"/>"+
  "      </Output>"+
  "      <Mapping>%1%.base~=%0%</Mapping>"+
  "      <Mapping>%1%.base.aString~=\"Baum\"</Mapping>"+
  "    </Mappings>"+
  "    <Data ID=\"5\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
  "      <Source RefID=\"7\"/>"+
  "    </Data>"+
  "    <Assign/>"+
  "  </Operation>"+
  "</Service>";
  
  public final static String WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME = "bg.test.xfl.newAndCast.CastWF11";
  public final static String WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML =
  "<Service ID=\"1\" Label=\"CastWF11\" TypeName=\"CastWF11\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF11\" Name=\"CastWF11\">"+
"    <Input>"+
"      <Data ID=\"5\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\">"+
"        <Target RefID=\"7\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"7\" Label=\"Mapping\">"+
"      <Source RefID=\"5\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"6\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base6\"/>"+
"        <Source RefID=\"5\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"8\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper8\"/>"+
"        <Target RefID=\"4\"/>"+
"      </Output>"+
"      <Mapping>%1%.sub1~=%0%#cast(\"bg.test.xfl.newAndCast.Sub1\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"4\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
"      <Source RefID=\"7\"/>"+
"    </Data>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_WEIRED_CL_CASE_FQNAME = "bg.test.xfl.newAndCast.CastWF13";
  public final static String WF_WEIRED_CL_CASE_XML = 
  "<Service ID=\"1\" Label=\"CastWF13\" TypeName=\"CastWF13\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF13\" Name=\"CastWF13\">"+
"    <Input>"+
"      <Data ID=\"9\" Label=\"Sub 3\" ReferenceName=\"Sub3\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub39\">"+
"        <Target RefID=\"7\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"7\" Label=\"Mapping\">"+
"      <Source RefID=\"9\"/>"+
"      <Target RefID=\"5\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FillMode,openConfiguration,FlatMode</FixedDetailOptions>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"10\" Label=\"Sub 3\" ReferenceName=\"Sub3\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub3\"/>"+
"        <Source RefID=\"9\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"6\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base6\"/>"+
"        <Target RefID=\"5\"/>"+
"      </Output>"+
"      <Output>"+
"        <Data ID=\"8\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper8\"/>"+
"        <Target RefID=\"4\"/>"+
"      </Output>"+
"      <Mapping>%1%~=%0%.wrapper.sub3</Mapping>"+
"      <Mapping>%1%.wrapper.base.aString~=\"Baum\"</Mapping>"+
"      <Mapping>%2%.sub2~=%0%.wrapper.sub3.wrapper.base#cast(\"bg.test.xfl.newAndCast.Sub2\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"4\" Label=\"Wrapper\" ReferenceName=\"Wrapper\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"wrapper\">"+
"      <Source RefID=\"7\"/>"+
"    </Data>"+
"    <Data ID=\"5\" Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\">"+
"      <Source RefID=\"7\"/>"+
"    </Data>"+
"    <Assign ID=\"11\"/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_NEW_EXCEPTION_AND_INITIALIZATION_FQNAME = "bg.test.xfl.newAndCast.NewException";
  public final static String WF_NEW_EXCEPTION_AND_INITIALIZATION_XML = 
  "<Service ID=\"1\" Label=\"NewException\" TypeName=\"NewException\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"NewException\" Name=\"NewException\">"+
"    <Input/>"+
"    <Output/>"+
"    <Throws>"+
"      <Exception Label=\"Xyna Exception Base\" ReferenceName=\"XynaExceptionBase\" ReferencePath=\"core.exception\" VariableName=\"xynaExceptionBase\"/>"+
"    </Throws>"+
"    <Mappings ID=\"7\" Label=\"Mapping\">"+
"      <Target RefID=\"4\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FillMode,FlatMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Output>"+
"        <Exception ID=\"6\" Label=\"Xyna Exception Base\" ReferenceName=\"XynaExceptionBase\" ReferencePath=\"core.exception\" VariableName=\"xynaExceptionBase6\"/>"+
"        <Target RefID=\"4\"/>"+
"      </Output>"+
"      <Mapping>%0%~=new(\"bg.test.xfl.newAndCast.Sub1Exception\")</Mapping>"+
"      <Mapping>%0%.abstractBooleanInBase~=\"true\"</Mapping>"+
"      <Mapping>%0%.stringInSub1Exception~=\"Baum\"</Mapping>"+
"    </Mappings>"+
"    <Throw ID=\"13\" Label=\"Throw Exception\" ExceptionID=\"4\">"+
"      <Source RefID=\"4\"/>"+
"    </Throw>"+
"    <Exception ID=\"4\" Label=\"Xyna Exception Base\" ReferenceName=\"XynaExceptionBase\" ReferencePath=\"core.exception\" VariableName=\"xynaExceptionBase4\">"+
"      <Source RefID=\"7\"/>"+
"      <Target RefID=\"13\"/>"+
"    </Exception>"+
"    <Assign ID=\"9\"/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_IMPLICIT_EXCPTION_CAST_FQNAME = "bg.test.xfl.newAndCast.CastWF14";
  public final static String WF_IMPLICIT_EXCPTION_CAST_XML = 
  "<Service ID=\"1\" Label=\"CastWF14\" TypeName=\"CastWF14\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF14\" Name=\"CastWF14\">"+
"    <Input>"+
"      <Exception ID=\"5\" Label=\"Sub1 Exception\" ReferenceName=\"Sub1Exception\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1Exception\">"+
"        <Target RefID=\"7\"/>"+
"      </Exception>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"7\" Label=\"Mapping\">"+
"      <Source RefID=\"5\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Input>"+
"        <Exception ID=\"6\" Label=\"Sub1 Exception\" ReferenceName=\"Sub1Exception\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1Exception6\"/>"+
"        <Source RefID=\"5\"/>"+
"      </Input>"+
"      <Output>"+
"        <Exception ID=\"8\" Label=\"Base Exception\" ReferenceName=\"BaseException\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"baseException8\"/>"+
"        <Target RefID=\"4\"/>"+
"      </Output>"+
"      <Mapping>%1%~=%0%</Mapping>"+
"      <Mapping>%1%.stringInSub1Exception~=\"Baum\"</Mapping>"+
"    </Mappings>"+
"    <Exception ID=\"4\" Label=\"Base Exception\" ReferenceName=\"BaseException\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"baseException\">"+
"      <Source RefID=\"7\"/>"+
"    </Exception>"+
"    <Assign ID=\"9\"/>"+
"  </Operation>"+
"</Service>";
  
  public final static String WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME = "bg.test.xfl.newAndCast.CastWF15";
  public final static String WF_CAST_EXCEPTION_FOR_MEMVAR_XML = 
    "<Service ID=\"1\" Label=\"CastWF15\" TypeName=\"CastWF15\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"CastWF15\" Name=\"CastWF15\">"+
"    <Input>"+
"      <Exception ID=\"5\" Label=\"Base Exception\" ReferenceName=\"BaseException\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"baseException\">"+
"        <Target RefID=\"8\"/>"+
"      </Exception>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"8\" Label=\"Mapping\">"+
"      <Source RefID=\"5\"/>"+
"      <Target RefID=\"10\"/>"+
"      <Input>"+
"        <Exception ID=\"7\" Label=\"Base Exception\" ReferenceName=\"BaseException\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"baseException7\"/>"+
"        <Source RefID=\"5\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1\"/>"+
"        <Target RefID=\"10\"/>"+
"      </Output>"+
"      <Mapping>%1%.aStringInAbstract~=%0%#cast(\"bg.test.xfl.newAndCast.Sub1Exception\").stringInSub1Exception</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"10\" Label=\"Sub 1\" ReferenceName=\"Sub1\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub110\">"+
"      <Source RefID=\"8\"/>"+
"    </Data>"+
"    <Assign ID=\"6\"/>"+
"  </Operation>"+
"</Service>";
  
}
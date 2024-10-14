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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;



public class WF1Test extends AGenerationTestTemplate<WF> {

  private String testTypeWorkflow = "testTypeWorkflow";
  private String testPath = "testPath";

  private String testType1 = "TestType13";
  private String testType2 = "testType23";
  private String testType = "testType3";

  protected ArrayList<String> getNames() {
    ArrayList<String> result = new ArrayList<String>();
    result.add(testType1);
    result.add(testType2);
    result.add(testType);
    result.add(testTypeWorkflow);
    return result;
  }
  
  protected ArrayList<String> getXmlString() {
    // Do not format this passage... !
    // TODO Should the version be hardcoded here? We should use the element and attribute constants from GenerationBase!
    ArrayList<String> result = new ArrayList<String>();
    result.add(
    "<DataType Label=\"" + testType1 + "\" TypeName=\"" + testType1 + "\" TypePath=\"" + testPath + "\" Version=\"1.7\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">\n"
    + "<Data Label=\"testVar1\" VariableName=\"testVar1Name\">\n"
    + "  <Meta>\n"
    + "    <Type>String</Type>\n"
    + "  </Meta>\n"
    + "</Data>\n"
    + "</DataType>");

    result.add("<DataType Label=\"" + testType2 + "\" TypeName=\"" + testType2 + "\" TypePath=\"" + testPath + "\" Version=\"1.7\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">\n"
    + "  <Data Label=\"testVar2\" VariableName=\"testVar2Name\">\n"
    + "    <Meta>\n"
    + "      <Type>int</Type>\n"
    + "    </Meta>\n"
    + "  </Data>\n"
    + "</DataType>");

    result.add(
        "<DataType Label=\"" + testType + "\" TypeName=\"" + testType + "\" TypePath=\"" + testPath + "\" Version=\"1.7\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">\n"
      + "<Data Label=\"testVar\" VariableName=\"testVarName\">\n"
      + "  <Meta>\n"
      + "    <Type>String</Type>\n"
      + "  </Meta>\n"
      + "</Data>\n"
      + "<Service Label=\"testService\" TypeName=\"testService\">\n"
      + "  <Operation Label=\"testOperation\" IsStatic=\"true\" Name=\"testOperation\">\n"
      + "    <Input>\n"
      + "      <Data Label=\"inputvar1\" ReferenceName=\"" + testType1 + "\" ReferencePath=\"" + testPath + "\" VariableName=\"inputvar1\"/>\n"
      + "    </Input>\n"
      + "    <Output>\n"
      + "      <Data Label=\"outputvar2\" ReferenceName=\"" + testType2 + "\" ReferencePath=\"" + testPath + "\" VariableName=\"outputvar2\"/>\n"
      + "    </Output>\n"
      + "    <Throws><Exception ReferenceName=\"XynaException\" ReferencePath=\"core.exception\"/></Throws>\n"
      + "    <SourceCode>\n"
      + "      <CodeSnippet Type=\"Java\">System.out.println(\"testCode\"); return new " + testType2 + "(123);</CodeSnippet>\n"
      + "    </SourceCode>\n"
      + "  </Operation>\n"
      + "</Service>\n"
      + "</DataType>");

    result.add("<Service ID=\"1\" Label=\"" + testTypeWorkflow + "\" TypeName=\"" + testTypeWorkflow + "\" TypePath=\"" + testPath + "\" Version=\"1.7\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">\n"
    + "    <Operation Name=\"testWorkflow1\">\n"
    + "      <Input>\n"
    + "        <Data ID=\"5\" Label=\"" + testType1 + "\" ReferenceName=\"" + testType1 + "\" ReferencePath=\"" + testPath + "\" VariableName=\"testType15\">\n"
    + "          <Target RefID=\"3\"/>\n"
    + "        </Data>\n"
    + "      </Input>\n"
    + "      <Output>\n"
    + "        <Data ID=\"7\" Label=\"" + testType2 + "\" ReferenceName=\"" + testType2 + "\" ReferencePath=\"" + testPath + "\" VariableName=\"testType27\">\n"
    + "  <Source RefID=\"3\"/>\n"
    + "  </Data>\n"
    + "      </Output>\n"
    + "      <Throws><Exception ReferenceName=\"XynaException\" ReferencePath=\"core.exception\"/></Throws>\n"
    + "      <Function ID=\"3\" Label=\"testOperation\">\n"
    + "        <Source RefID=\"2\"/>\n"
    + "        <Source RefID=\"5\"/>\n"
    + "        <Target RefID=\"2\"/>\n"
    + "        <Target RefID=\"7\"/>\n"
    + "       <Invoke ServiceID=\"2\" Operation=\"testOperation\">\n"
    + "          <Source RefID=\"5\"/>\n"
    + "        </Invoke>\n"
    + "        <Receive ServiceID=\"2\">\n"
    + "          <Target RefID=\"7\"/>\n"
    + "            </Receive>\n"
    + "      </Function>\n"
    + "      <ServiceReference ID=\"2\" Label=\"testService\" ReferenceName=\"" + testType + ".testService\" ReferencePath=\"" + testPath + "\">\n"
    + "        <Source RefID=\"3\"/>\n"
    + "        <Target RefID=\"3\"/>\n"
    + "      </ServiceReference>\n"
    + "    </Operation>\n"
    + "  </Service>");
    
    return result;
  }
  
  public WF getInstance(String fqClassName) throws XynaException {
    return WF.getInstance(fqClassName);
  }


  @Override
  public void checkAfterCodeChanged(WF d) throws XynaException {

    
  }

  @Override
  public void checkAfterCodeUnchanged(WF d) throws XynaException {
  
  }


  @Override
  public void checkAfterGetInstance(WF d) throws XynaException {
 
  }

  @Override
  public void checkAfterReload(WF d) throws XynaException {
 
  }

  @Override
  public void checkCopyXML(WF d) throws XynaException {
  
  }

  @Override
  public void checkGeneration(WF wf) throws XynaException, IOException {

    String fileLocation = DOM.getRelativeJavaFileLocation(wf.getFqClassName(), true, wf.getRevision());
    File f = new File(fileLocation);
    byte[] buf = null;
    FileInputStream fis = new FileInputStream(f);
    buf = new byte[fis.available()];
    fis.read(buf);

    String generated = new String(buf);

    if (!generated.contains("Copyright 20") || !generated.contains("Xyna GmbH, Germany")) {
      fail("no or wrong copyright information in generated java file");
    }

    if (generated.split("\\{").length > generated.split("\\}").length) {
      fail("missing at least one '}' in generated Java code");
    }
    else if (generated.split("\\{").length < generated.split("\\}").length) {
      fail("missing at least one '{' in generated Java code");
    }

    if (!generated.contains("package " + testPath)) {
      fail("no or wrong package information in generated java code: expected 'package " + testPath + "'");
    }

    if (!generated
                    .contains("public class " + testTypeWorkflow + " extends " + XynaProcess.class.getSimpleName() )) {
      fail("Missing correct class definition");
    }

    if (!generated.contains("public " + FractalProcessStep.class.getSimpleName() + "[] getStartSteps()")) {
      fail("missing method 'getStartSteps()' in generated Java code");
    }

    if (!generated.contains("public " + FractalProcessStep.class.getSimpleName() + "[] getAllSteps()")) {
      fail("missing method 'getAllSteps()' in generated Java code");
    }

    if (!generated.contains("public void setInputVars(GeneralXynaObject o) throws XynaException {")) {
      fail("missing method 'setInputVars' in generated Java code");
    }

    if (!generated.contains("private static class Step1 extends " + FractalProcessStep.class.getSimpleName() + "<" + testTypeWorkflow + "> implements JavaCall {")) {
      fail("missing definition of private class 'ProcessStep<" + testTypeWorkflow + "> implements JavaCall {' in generated Java code");
    }

   if (!generated.contains("protected void onDeployment() throws XynaException {")) {
      fail("missing method onDeployment() in generated Java code");
    }

    if (!generated.contains("protected void onUndeployment() throws XynaException {")) {
      fail("missing method onUndeployment() in generated Java code");
    }
  }

  @Override
  public void checkOnDeployment(WF d) throws XynaException {
  
  }

  @Override
  public void checkParse(WF d) throws XynaException {
    }

  @Override
  public void checkValidate(WF d) throws XynaException {
  
  }

  @Override
  public String getType() {
    return testTypeWorkflow;
  }

  @Override
  public String getPath() {
    return testPath;
  }

  @Override
  public void checkBeforeUndeployment(WF d) throws XynaException {
    //check if all files exist
    ArrayList<String> names = new ArrayList<String>();
    names = getNames();
    for (String name : names) {
      File f = new File(Constants.MDM_CLASSDIR + Constants.fileSeparator +  getPath() + Constants.fileSeparator + name + ".class");
      assertEquals(true, f.exists());
    }
  }
  
  @Override
  public void checkAfterUndeployment(WF d) throws XynaException {
    //check if files got deleted
    ArrayList<String> names = new ArrayList<String>();
    names = getNames();
    for (String name : names) {
      File f = new File(Constants.MDM_CLASSDIR + Constants.fileSeparator +  getPath() + Constants.fileSeparator + name + ".class");
      //because dependencies don't get undeployed only the file that equals the result of getType should get undeployed
      if (name.equals(getType())) {
        assertEquals(false, f.exists());
      } else {
        assertEquals(true, f.exists());
      }
    }    
  }

}

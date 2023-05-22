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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;



public class DOM1Test extends DOMTestTemplate {


  private String testType;
  private String testPath;

  private String testOperation = "testOperation";

  @Override
  protected ArrayList<String> getNames() {
    ArrayList<String> result = new ArrayList<String>();
    result.add(testType);
    return result;
  }
  
  @Override
  public void setUp() throws SecurityException, NoSuchMethodException, XynaException, ClassNotFoundException, IOException {
    testType = "TestType";
    testPath = "testPath";
    super.setUp();
  }
  
  private ArrayList<String> xmlStrings;

  @Override
  protected ArrayList<String> getXmlString() {
    // do not format this passage... !
    // TODO read this from a file? that would make it harder to insert references to the XML, though
    xmlStrings = new ArrayList<String>();
    xmlStrings.add(
    "<DataType Label=\"" + testType + "\" TypeName=\"" + testType + "\" TypePath=\"" + testPath + "\" Version=\"1.7\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">\n" // FIXME version
      + "  <Data Label=\"testVar\" VariableName=\"testVarName\">\n"
      + "    <Meta>\n"
      + "      <Type>String</Type>\n"
      + "    </Meta>\n"
      + "  </Data>\n"
      + "  <Service Label=\"testService\" TypeName=\"testService\">\n"
      + "    <Operation Label=\"testOperation\" IsStatic=\"true\" Name=\"" + testOperation + "\">\n"
      + "      <Input/>\n"
      + "      <Output/>\n"
      + "      <SourceCode>\n"
      + "        <CodeSnippet Type=\"Java\">System.out.println(\"testCode\");</CodeSnippet>\n"
      + "      </SourceCode>\n"
      + "      <Throws><Exception ReferenceName=\"XynaException\" ReferencePath=\"core.exception\"/></Throws>\n"
      + "    </Operation>\n"
      + "  </Service>\n"
    + "</DataType>\n");
    return xmlStrings;
  }

  @Override
  public void checkAfterGetInstance(DOM d) throws XynaException {
    super.checkAfterGetInstance(d);
  }

  @Override
  public void checkAfterReload(DOM d) throws XynaException {
    super.checkAfterReload(d);
  }


  @Override
  public void checkAfterCodeChanged(DOM d) throws XynaException {
    super.checkAfterCodeChanged(d);
  }


  @Override
  public void checkCopyXML(DOM d) throws XynaException {
    super.checkCopyXML(d);    
  }


  @Override
  public void checkGeneration(String generatedJava) throws XynaException {

    if(!generatedJava.contains("public static void " + testOperation + "() throws XynaException {")) {
      fail("could not find service operation '" + testOperation + "()' in generated java code");
    }

  }


  @Override
  public void checkOnDeployment(DOM d) throws XynaException {
    super.checkOnDeployment(d);    
  }


  @Override
  public void checkParse(DOM d) throws XynaException {
    super.checkParse(d);
  }


  @Override
  public void checkValidate(DOM d) throws XynaException {
    super.checkValidate(d);    
  }


  @Override
  public void checkAfterCodeUnchanged(DOM d) throws XynaException {
    super.checkAfterCodeUnchanged(d);
  }

  @Override
  public String getType() {
    return testType;
  }

  @Override
  public String getPath() {
    return testPath;
  }
  

  @Override
  public void checkBeforeUndeployment(DOM d) throws XynaException {
    File f = new File(Constants.MDM_CLASSDIR + Constants.fileSeparator +  getPath() + Constants.fileSeparator + getType() + ".class");
    assertEquals(true, f.exists());
  }


  @Override
  public void checkAfterUndeployment(DOM d) throws XynaException {
    File f = new File(Constants.MDM_CLASSDIR + Constants.fileSeparator +  getPath() + Constants.fileSeparator + getType() + ".class");
    assertEquals(false, f.exists());    
  }

 


}

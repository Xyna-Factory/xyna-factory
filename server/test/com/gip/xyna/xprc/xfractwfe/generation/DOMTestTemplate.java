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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;



public abstract class DOMTestTemplate extends AGenerationTestTemplate<DOM> {


  public DOM getInstance(String fqClassName) throws XynaException {
    return DOM.getInstance(getFqClassName());
  }


  public void checkAfterGetInstance(DOM d) throws XynaException {
    assertEquals(0, d.getDependenciesRecursively().getDependencies(false).size());
    assertEquals(getFqClassName(), d.getFqClassName());
    assertEquals(getType(), d.getSimpleClassName());
    assertEquals(0, d.getSharedLibs().length);
    assertEquals(0, d.getMemberVars().size());
  }


  public void checkAfterReload(DOM d) throws XynaException {

 }


 @Override
 public void checkAfterCodeChanged(DOM d) throws XynaException {
 }


 @Override
 public void checkCopyXML(DOM d) throws XynaException {
 }


  public abstract void checkGeneration(String generatedJava) throws XynaException;


  @Override
  public void checkGeneration(DOM d) throws XynaException {

    String fileLocation = DOM.getRelativeJavaFileLocation(d.getFqClassName(), true, d.getRevision());
    File f = new File(fileLocation);
    byte[] buf = null;
    try {
      FileInputStream fis = new FileInputStream(f);
      buf = new byte[fis.available()];
      fis.read(buf);
    } catch (IOException e) {
      fail("Could not read generated java file.");
    }

    String generated = new String(buf);

    if (!generated.contains("Copyright GIP AG 20")) {
      fail("no copyright information in generated java file");
    }

    if (!generated.contains("Copyright GIP AG 2015")) {
      fail("no copyright information for the year 2009 in generated java code: outdated test?");
    }

    if (generated.split("\\{").length > generated.split("\\}").length) {
      fail("missing at least one '}'");
    } else if (generated.split("\\{").length < generated.split("\\}").length) {
      fail("missing at least one '{'");
    }

    if (!generated.contains("package " + getPath())) {
      fail("no or wrong package information in generated java code: expected 'package " + getPath() + "'");
    }

    if (!generated.contains("public class " + getType() + " extends " + XynaObject.class.getSimpleName())) {
      fail("no or wrong class definition in generated java code: ");
    }

    if (!generated.contains("private String testVarName;")) {
      fail("Could not find definition of class variable 'testVarName'");
    }

    if (!generated.contains("public " + getType() + "(String testVarName) {")) {
      fail("could not find default constructor in generated java code");
    }

    if (!generated.contains("public " + getType() + " clone() {")) {
      fail("could not find clone function in generated java code");
    }

    if (!generated.contains("public String toXml(String varName, boolean onlyContent) {")) {
      fail("could not find 'toXml' method in generated java code");
    }

    if (!generated.contains("public HashSet<String> getVarNames() {")) {
      fail("could not find 'getVarNames()' method in generated java code");
    }

    if (!d.isAbstract()) {
      if (!generated.contains("public void onDeployment() throws XynaException {")) {
        fail("could not find onDeployment() function in generated java code");
      }
      if (!generated.contains("public void onUndeployment() throws XynaException {")) {
        fail("could not find onUndeployment() function in generated java code");
      }
    } else {
      if (!generated.contains("public static void staticOnDeployment() throws XynaException {")) {
        fail("could not find staticOnDeployment() function in generated java code");
      }
      if (!generated.contains("public static void staticOnUndeployment() throws XynaException {")) {
        fail("could not find staticOnUndeployment() function in generated java code");
      }
    }

    if (!generated.contains("super.onDeployment()")) {
      fail("found onDeployment() function does not call 'super.onDeployment()'");
    }

    checkGeneration(generated);
  }


  @Override
  public void checkOnDeployment(DOM d) throws XynaException {

  }


  @Override
  public void checkParse(DOM d) throws XynaException {
    assertEquals(0, d.getDependenciesRecursively().getDependencies(false).size()); // only the thrown xynaexception
    assertEquals(1, d.getMemberVars().size());
    assertEquals(true, ((DatatypeVariable) d.getMemberVars().get(0)).isJavaBaseType());
    assertEquals(1, d.getOperations().size());
    assertEquals("testOperation", d.getOperationByName("testOperation").getName());
    assertEquals(true, d.getOperationByName("testOperation").isStatic());
    assertEquals(false, d.getOperationByName("testOperation").isAbstract());
    HashSet<String> jars = new HashSet<String>();
    d.getDependentJarsWithoutRecursion(jars, true, true);
    assertEquals(0, jars.size());
  }


  @Override
  public void checkValidate(DOM d) throws XynaException {

  }


  @Override
  public void checkAfterCodeUnchanged(DOM d) throws XynaException {

  }

}

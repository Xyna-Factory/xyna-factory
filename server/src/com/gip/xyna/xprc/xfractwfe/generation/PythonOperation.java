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

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.python.PythonInterpreter;



public class PythonOperation extends CodeOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(PythonOperation.class);


  public PythonOperation(DOM parent) {
    super(parent, ATT.PYTHON);
  }
  
  @Override
  protected void getImports(Set<String> imports) {
    super.getImports(imports);
    imports.add("java.io.IOException");
    imports.add("com.gip.xyna.XynaFactory");
    imports.add("com.gip.xyna.xdev.xfractmod.xmdm.Container");
    imports.add("com.gip.xyna.xprc.xfractwfe.python.PythonInterpreter");
  }


  protected void generateJavaImplementationInternally(CodeBuffer cb) {
    
    cb.addLine("try (PythonInterpreter interpreter = XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().createPythonInterpreter(getClass().getClassLoader())) {");
    
    for (AVariable var : getInputVars()) {
      cb.addLine("interpreter.set(\"" + var.varName + "\", " + var.varName + ")");
    }

    String input = String.join(", ", getInputVars().stream().map(var -> var.varName).collect(Collectors.toList()));
    cb.addLine("interpreter.exec(\"def " + getNameWithoutVersion() + "(" + input + "):\")");
    String impl = getImpl().replaceAll("(?m)^", "  ");
    cb.addLine("interpreter.exec(\"" + impl + "\")");
    String output = String.join(", ", getOutputVars().stream().map(var -> var.varName).collect(Collectors.toList()));
    cb.addLine("interpreter.exec(\"(" + output + ") = getNameWithoutVersion(" + input + ")\")");

    cb.addLine("} catch (IOException e) {");
    cb.addLine("} finally {");
    for (AVariable var : getOutputVars()) {
      cb.addLine(var.getFQClassName() + " " + var.varName + " = interpreter.get(\"" + var.varName + "\")");
    }
    
    if (getOutputVars().size() > 1) {
      cb.addLine("return new Container(" + output + ")");
    } else if (getOutputVars().size() == 0) {
      cb.addLine("return" + getOutputVars().get(0).getVarName());
    } else {
      cb.addLine("return");
    }
    cb.addLine("}");
  }

}

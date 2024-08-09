/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;

public class PythonOperation extends CodeOperation {

  public PythonOperation(DOM parent) {
    super(parent, ATT.PYTHON);
  }
  
  @Override
  protected void getImports(Set<String> imports) {
    super.getImports(imports);
    imports.add("com.gip.xyna.XynaFactory");
    imports.add("com.gip.xyna.xdev.xfractmod.xmdm.Container");
    imports.add("com.gip.xyna.xprc.xfractwfe.python.PythonInterpreter");
  }


  protected void generateJavaImplementationInternally(CodeBuffer cb) {
    
    for (AVariable var : getOutputVars()) {
      cb.addLine(var.getFQClassName() + " " + var.varName);
    }
    
    cb.addLine("try (PythonInterpreter interpreter = XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().createPythonInterpreter(getClass().getClassLoader())) {");
    
    for (AVariable var : getInputVars()) {
      cb.addLine("interpreter.set(\"" + var.varName + "\", " + var.varName + ")");
    }
    
    String pythonscript;
    
    String input = String.join(", ", getInputVars().stream().map(var -> var.varName).collect(Collectors.toList()));
    pythonscript = "def " + getNameWithoutVersion() + "(" + input + "):";
    String impl = getImpl().replaceAll("(?m)^", "  ");
    impl = impl.replaceAll("\"", "\\\\\\\"");
    impl = impl.replaceAll("\n", "\\\\n");
    pythonscript += "\\n" + impl;
    String output = String.join(", ", getOutputVars().stream().map(var -> var.varName).collect(Collectors.toList()));
    if (getOutputVars().size() == 0) {
      pythonscript += "\\n" + getNameWithoutVersion() + "(" + input + ")";
    } else {
      pythonscript += "\\n(" + output + ") = " + getNameWithoutVersion() + "(" + input + ")";
    }

    cb.addLine("interpreter.exec(\"" + pythonscript + "\")");
    for (AVariable var : getOutputVars()) {
      cb.addLine(var.varName + " = (" + var.getFQClassName() + ") interpreter.get(\"" + var.varName + "\")");
    }
    cb.addLine("}");
    
    if (getOutputVars().size() > 1) {
      cb.addLine("return new Container(" + output + ")");
    } else if (getOutputVars().size() == 1) {
      cb.addLine("return " + getOutputVars().get(0).getVarName());
    } else {
      cb.addLine("return");
    }
  }
}

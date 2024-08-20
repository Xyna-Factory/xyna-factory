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

import java.util.List;
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

  

  private String convertVariableToJava(AVariable var) {
    String type = var.isList ? "List<" : "";
    type += var.isJavaBaseType ? var.getJavaTypeEnum().getJavaTypeName() : var.getOriginalPath() + "." + var.getOriginalName();
    type += var.isList ? ">" : "";
    String result = "(" + type + ")pyMgmt.convertToJava(context, \"" + type + "\", " + var.getVarName() + ")";
    if(var.isList) {
      String fqn = var.getOriginalPath() + "." + var.getOriginalName();
      result = "new com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList("+ result + ", " + fqn + ".class)";
    }
    return result;
  }
  

  private String convertVariableToPython(AVariable var) {
    if (var.isJavaBaseType) {
      return var.getVarName();
    }

    return "pyMgmt.convertToPython(" + var.getVarName() + ")";
  }
  
  private void addSetReturn(CodeBuffer cb) {
    if (getOutputVars().size() > 1) {
      List<String> outputs =  getOutputVars().stream().map(this::convertVariableToJava).collect(Collectors.toList());
      String output = String.join(", ", outputs);
      cb.addLine("return new Container(" + output + ")");
    } else if (getOutputVars().size() == 1) {
      cb.addLine("return " + convertVariableToJava(getOutputVars().get(0)));
    } else {
      cb.addLine("return");
    }
  }
  
  
  private void addLoadMdm(CodeBuffer cb) {
    cb.addLine("interpreter.exec(pyMgmt.getLoaderSnippet())");
    cb.addLine("interpreter.set(\"pyMgmt\", pyMgmt)");
    cb.addLine("interpreter.exec(\"xyna_mdm_module = pyMgmt.createPythonMdm(_context.revision, True, False)\")");
    cb.addLine("interpreter.exec(\"module_to_load = {\\\"mdm\\\": xyna_mdm_module}\")");
    cb.addLine("interpreter.exec(\"_load_module(module_to_load)\")");
    cb.addLine("interpreter.exec(\"import mdm\")");
    cb.addLB();
  }

  private void addExecuteScript(CodeBuffer cb) {
    StringBuilder pythonscript = new StringBuilder();
    String input = String.join(", ", getInputVars().stream().map(var -> var.varName).collect(Collectors.toList()));
    if(!isStatic()) {
      input = "this" + (input.length() > 0 ? ", " : "");
    }
    pythonscript.append("def ").append(getNameWithoutVersion()).append("(").append(input).append("):");
    String impl = getImpl().replaceAll("(?m)^", "  ");
    impl = impl.replaceAll("\"", "\\\\\\\"");
    impl = impl.replaceAll("\n", "\\\\n");
    pythonscript.append("\\n").append(impl);
    String output = String.join(", ", getOutputVars().stream().map(var -> var.varName).collect(Collectors.toList()));
    if (getOutputVars().size() > 0) {
      pythonscript.append("\\n(").append(output).append(") = ");
    }
    pythonscript.append(getNameWithoutVersion()).append("(").append(input).append(")");

    cb.addLine("interpreter.exec(\"" + pythonscript + "\")");
  }
  

  protected void generateJavaImplementationInternally(CodeBuffer cb) {
    cb.addLine("com.gip.xyna.xprc.xfractwfe.XynaPythonSnippetManagement pyMgmt = com.gip.xyna.XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement()");
    for (AVariable var : getOutputVars()) {
      cb.addLine("Object " + var.varName);
    }
    cb.addLine("com.gip.xyna.xprc.xfractwfe.python.Context context = new com.gip.xyna.xprc.xfractwfe.python.Context()");
    cb.addLine("context.revision = ((com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase)getClass().getClassLoader()).getRevision()");
    cb.addLine("try (PythonInterpreter interpreter = pyMgmt.createPythonInterpreter(getClass().getClassLoader())) {");
    cb.addLine("interpreter.set(\"_context\", context)");
    addLoadMdm(cb);
    cb.addLine("interpreter.exec(\"mdm.XynaObject._context = _context\")");
    
    if(!isStatic()) {
      cb.addLine("interpreter.set(\"this\", pyMgmt.convertToPython(this))");
      cb.addLine("interpreter.exec(\"this = mdm.convert_to_python_object(this)\")");
    }
    
    for (AVariable var : getInputVars()) {
      cb.addLine("interpreter.set(\"" + var.varName + "\", " + convertVariableToPython(var) + ")");
      if (var.isList) {
        cb.addLine("interpreter.exec(\"" + var.varName + " = mdm._convert_list(" + var.varName + ")\")");
      } else if (!var.isJavaBaseType()) {
        cb.addLine("interpreter.exec(\"" + var.varName + " = mdm.convert_to_python_object(" + var.varName + ")\")");
      }
    }

    addExecuteScript(cb);

    for (AVariable var : getOutputVars()) {
      cb.addLine(var.varName + " = interpreter.get(\"" + var.varName + "\")");
    }
    addSetReturn(cb);
    cb.addLine("}");
  }
}

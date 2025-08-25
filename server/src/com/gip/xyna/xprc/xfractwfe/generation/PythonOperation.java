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
import com.gip.xyna.xprc.xfractwfe.python.PythonGeneration;
import com.gip.xyna.xprc.xfractwfe.python.PythonMdmGeneration;

public class PythonOperation extends CodeOperation {

  private PythonMdmGeneration _pythonMdmGeneration = new PythonMdmGeneration(); 
  
  public PythonOperation(DOM parent) {
    super(parent, ATT.PYTHON);
  }
  
  @Override
  protected void getImports(Set<String> imports) {
    super.getImports(imports);
    imports.add("com.gip.xyna.XynaFactory");
    imports.add("com.gip.xyna.xdev.xfractmod.xmdm.Container");
    imports.add("com.gip.xyna.xprc.xfractwfe.python.PythonInterpreter");
    imports.add("com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType");
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
    List<String> escapedInputs = getInputVars().stream().map(var -> escape(var.varName)).collect(Collectors.toList());
    String input = String.join(", ", escapedInputs);
    if(!isStatic()) {
      input = "this" + (input.length() > 0 ? ", " : "") + input;
    }
    pythonscript.append("def ").append(getAdaptedNameWithoutVersion()).append("(").append(input).append("):");
    String impl = getImpl().replaceAll("(?m)^", "  ");
    impl = impl.replaceAll("\"", "\\\\\\\"");
    impl = impl.replaceAll("\n", "\\\\n");
    pythonscript.append("\\n").append(impl).append("\\n");
    String output = String.join(", ", getOutputVars().stream().map(var -> escape(var.varName)).collect(Collectors.toList()));
    if (getOutputVars().size() > 0) {
      pythonscript.append("\\n(").append(output).append(") = ");
    }
    pythonscript.append(getAdaptedNameWithoutVersion()).append("(").append(input).append(")");

    cb.addLine("interpreter.exec(\"" + pythonscript + "\")");
  }
  

  protected void generateJavaImplementationInternally(CodeBuffer cb) {
    String getClassString = !isStatic() ? "getClass()": parent.getFqClassName() + ".class";
    cb.addLine("com.gip.xyna.xprc.xfractwfe.XynaPythonSnippetManagement pyMgmt = com.gip.xyna.XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement()");
    for (AVariable var : getOutputVars()) {
      cb.addLine("Object " + var.varName);
    }
    cb.addLine("com.gip.xyna.xprc.xfractwfe.python.Context context = new com.gip.xyna.xprc.xfractwfe.python.Context()");
    cb.addLine("context.revision = ((com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase)" + getClassString + ".getClassLoader()).getRevision()");
    String servicePath = getParent().getOriginalFqName();
    cb.addLine("context.servicePath = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getPathForRevision(PathType.SERVICE, context.revision)"
        + " + \"/" + servicePath + "\"");
    cb.addLine("try (PythonInterpreter interpreter = pyMgmt.createPythonInterpreter(" + getClassString + ".getClassLoader())) {");
    cb.addLine("interpreter.set(\"_context\", context)");
    addLoadMdm(cb);
    cb.addLine("interpreter.exec(\"mdm.XynaObject._context = _context\")");

    cb.addLine("interpreter.exec(\"sys.path.append(_context.servicePath)\")");
    
    if(!isStatic()) {
      cb.addLine("interpreter.set(\"this\", pyMgmt.convertToPython(this))");
      cb.addLine("interpreter.exec(\"this = mdm.convert_to_python_object(this)\")");
    }
    
    for (AVariable var : getInputVars()) {
      cb.addLine("interpreter.set(\"" + escape(var.varName) + "\", " + convertVariableToPython(var) + ")");
      if (var.isList) {
        cb.addLine("interpreter.exec(\"" + escape(var.varName) + " = mdm._convert_list(" + escape(var.varName) + ")\")");
      } else if (!var.isJavaBaseType()) {
        cb.addLine("interpreter.exec(\"" + escape(var.varName) + " = mdm.convert_to_python_object(" + escape(var.varName) + ")\")");
      }
    }

    addExecuteScript(cb);

    for (AVariable var : getOutputVars()) {
      cb.addLine(var.varName + " = interpreter.get(\"" + escape(var.varName) + "\")");
    }
    addSetReturn(cb);
    cb.addLine("}");
  }
  

  public String createImplCallSnippet(boolean andSet) {
    StringBuilder cb = new StringBuilder();
    String fqnPython = PythonGeneration.convertToPythonFqn(parent.getOriginalFqName());
    cb.append("from ").append(fqnPython).append("Impl import ").append(fqnPython).append("Impl\n");
    cb.append("impl = ").append(fqnPython).append("Impl(");
    if (!isStatic()) {
      cb.append("this");
    }
    cb.append(")\n");
    if (getOutputVars() != null && !getOutputVars().isEmpty()) {
      cb.append("return ");
    }
    cb.append("impl.").append(getAdaptedNameWithoutVersion()).append("(");
    if (requiresXynaOrder()) {
      cb.append("correlatedXynaOrder");
      if (!getInputVars().isEmpty()) {
        cb.append(", ");
      }
    }
    cb.append(String.join(", ", getInputVars().stream().map(x -> escape(x.getVarName())).collect(Collectors.toList())));
    cb.append(")");

    String impl = cb.toString().trim();
    if (andSet) {
      setImpl(impl);
    }
    return impl;
  }
  
  
  public String getAdaptedNameWithoutVersion() {
    String ret = super.getNameWithoutVersion();    
    return escape(ret);
  }
  
  private String escape(String s) {
    String ret = s;
    if (_pythonMdmGeneration.getPythonKeywords().contains(ret)) {
      ret = ret + "_";
    }
    return ret;
  }
  
  
}

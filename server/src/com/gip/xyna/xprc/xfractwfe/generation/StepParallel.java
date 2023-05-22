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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.ParallelExecutionStep;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

public class StepParallel extends StepSerial {

  private static final String VARNAME_fractalWorkflowParallelExecutor = "fractWfParallelExecutor";
  private String label;
  
  public StepParallel(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepParallel( this );
  }

  protected void getImports(HashSet<String> alreadyAdded) throws XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException {
    super.getImports(alreadyAdded);
    alreadyAdded.add(FractalWorkflowParallelExecutor.class.getName());
    alreadyAdded.add(ParallelExecutionStep.class.getName());
  }


  private long calcSerialVersionUID() {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    types.add(Pair.of(VARNAME_fractalWorkflowParallelExecutor, FractalWorkflowParallelExecutor.class.getName()));
    return GenerationBase.calcSerialVersionUID(types);
  }


  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException {

    label = "P"+getIdx();
    
    // class declaration
    cb.addLine("private static class ", getClassName(), 
               " extends ", FractalProcessStep.class.getSimpleName(), "<", getParentScope().getClassName(), ">",
               " implements " + ParallelExecutionStep.class.getSimpleName(), "<",getParentScope().getClassName(), ">",
               "{").
               addLB();

    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calcSerialVersionUID()), "L");

    cb.addLine("private ", FractalWorkflowParallelExecutor.class.getSimpleName(), " ",
               VARNAME_fractalWorkflowParallelExecutor).addLB();
    
    // constructor
    cb.addLine("public ", getClassName(), "() {");
    cb.addLine("super(", getIdx() + ")");
    cb.addLine("}").addLB();

    // override reinit method
    cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
    cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
    cb.addLine(VARNAME_fractalWorkflowParallelExecutor, " = null");
    cb.addLine("}").addLB();

    cb.add("public String ", METHODNAME_GET_LABEL, "() { return \"" + GenerationBase.escapeForCodeGenUsageInString(label) + "\"; }")
        .addLB();

    appendGetFractalWorkflowParallelExecutor(cb);
    
    appendExecuteInternally(cb, importedClassesFqStrings);
    
    appendCompensateInternally(cb, importedClassesFqStrings);

    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, new String[0], null, cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, new String[0], null, cb, importedClassesFqStrings);

    generatedGetRefIdMethod(cb);

    // get children
    cb.addLine("protected ", FractalProcessStep.class.getSimpleName(), "<", getParentScope().getClassName(),
               ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    for (int i = 0; i < getChildSteps().size(); i++) {
      cb.addLine("if (i==" + i + ") {");
      cb.addLine("return new ", FractalProcessStep.class.getSimpleName(),
                 "[]{", METHODNAME_GET_PARENT_SCOPE, "()." + getChildSteps().get(i).getVarName() + "};");
      cb.addLine("}");
    }
    cb.addLine("return null");
    cb.addLine("}").addLB();

    // getChildrenTypesLength 
    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return " + getChildSteps().size());
    cb.addLine("}").addLB();

    // end of class
    cb.addLine("}").addLB();

  }

  protected void appendGetFractalWorkflowParallelExecutor(CodeBuffer cb) {
    cb.addLine("public ",FractalWorkflowParallelExecutor.class.getSimpleName()," ", StepForeach.METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR, "() {");
    cb.addLine("if (", VARNAME_fractalWorkflowParallelExecutor, " == null) {");{
      cb.addLine(FractalProcessStep.class.getSimpleName()+"[] allSteps = new "
          +FractalProcessStep.class.getSimpleName()+"["+getChildSteps().size()+"]");
      for (int i = 0; i < getChildSteps().size(); i++) {
        cb.addLine("allSteps["+i+"] = ", METHODNAME_GET_PARENT_SCOPE, "()."+getChildSteps().get(i).getVarName() );
      }
      String peId = "P"+getIdx();
      cb.add(VARNAME_fractalWorkflowParallelExecutor, " = new ",
             FractalWorkflowParallelExecutor.class.getSimpleName(),"(\"",peId,"\",allSteps)").addLB();
    }cb.addLine("}");
    cb.addLine("return ", VARNAME_fractalWorkflowParallelExecutor);
    cb.addLine("}").addLB();
  }
  
  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    // execute legt beim ersten Mal den  FractalWorkflowParallelExecutor an 
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine("", StepForeach.METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR, "().", StepForeach.METHODNAME_PARALLEL_EXECUTOR_INIT,
                    "(", METHODNAME_GET_PROCESS, "(), null).", StepForeach.METHODNAME_PARALLEL_EXECUTOR_EXECUTE, "()");
    cb.addLine("}").addLB();
  }
  
  protected void appendCompensateInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    // compensate
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws " + XynaException.class.getSimpleName() + " {");   
    cb.addLine("", StepForeach.METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR, "().", StepForeach.METHODNAME_PARALLEL_EXECUTOR_INIT,
                   "(", METHODNAME_GET_PROCESS, "(), null).", StepForeach.METHODNAME_PARALLEL_EXECUTOR_COMPENSATE, "()");
    cb.addLine("}").addLB();
  }

  @Override
  public boolean toBeShownInAudit() {
    return true;
  }

  @Override
  public Step getContainerStepForGui() {
    return this;
  }

  @Override
  public void createEmpty() {
    super.createEmpty();

    StepSerial leftStep = new StepSerial(getParentScope(), getCreator());
    leftStep.createEmpty();
    addChild(0, leftStep);

    StepSerial rightStep = new StepSerial(getParentScope(), getCreator());
    rightStep.createEmpty();
    addChild(1, rightStep);
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElement(EL.PARALLEL); {
      for (Step childStep : getChildSteps()) {
        if (childStep instanceof StepSerial) {
          ((StepSerial)childStep).appendXML(xml, true);
        } else {
          childStep.appendXML(xml);
        }
      }
    } xml.endElement(EL.PARALLEL);
  }
}

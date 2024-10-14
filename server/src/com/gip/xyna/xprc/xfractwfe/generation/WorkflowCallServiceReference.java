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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

/**
 * in datentyp definierte operation, indem ein workflow-service referenziert wird
 */
public class WorkflowCallServiceReference extends WorkflowCall {

  public WorkflowCallServiceReference(DOM parent) {
    super(parent);
  }

  /**
   * @param e muss eine Service-Referenz auf einen Workflow sein
   */
  public void parseXmlInternally(Element e) throws XPRC_InvalidPackageNameException {
    setStatic(true);
    // parse operation sonst
    wf = parent.getCachedWFInstanceOrCreate(e.getAttribute(GenerationBase.ATT.REFERENCEPATH) + "." +  
                                            e.getAttribute(GenerationBase.ATT.REFERENCENAME), parent.revision);
    wfFQClassName = wf.getFqClassName();
    wfClassName = wf.getSimpleClassName();
    setName(wfClassName);
    setLabel(e.getAttribute(GenerationBase.ATT.LABEL));
  }
  

  protected void generateJavaImplementation(CodeBuffer cb, Set<String> importedClassesFqStrings) {

    // Create code for the subworkflow creation
    cb.add(XynaOrderServerExtension.class.getSimpleName(), " subworkflow = new ", XynaOrderServerExtension.class.getSimpleName(), "(new ",
           DestinationKey.class.getSimpleName(), "(\"" + wf.getFqClassName() + "\"), ");
    if (wf.getInputVars().size() == 1) {

      if (wf.getInputVars().get(0).isList()) {
        if (wf.getInputVars().get(0) instanceof ExceptionVariable) {
          cb.add("new " + GeneralXynaObjectList.class.getSimpleName() + "(" + wf.getInputVars().get(0).getVarName()
              + ", " + wf.getInputVars().get(0).getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
              + ".class)");
        } else {
          cb.add("new " + XynaObjectList.class.getSimpleName() + "(" + wf.getInputVars().get(0).getVarName() + ", "
              + wf.getInputVars().get(0).getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
              + ".class)");
        }
      } else {
        cb.add(wf.getInputVars().get(0).getVarName());
      }

    } else {
      cb.add("new ", Container.class.getSimpleName(), "(");
      for (AVariable v : wf.getInputVars()) {
        if (v.isList()) {
          if (v instanceof ExceptionVariable) {
            cb.addListElement("new " + GeneralXynaObjectList.class.getSimpleName() + "(" + v.getVarName() + ", "
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
          } else {
            cb.addListElement("new " + XynaObjectList.class.getSimpleName() + "(" + v.getVarName() + ", "
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
          }
        } else {
          cb.addListElement(v.getVarName());
        }
      }
      cb.add(")");
    }
    cb.add(")").addLB();
    cb.addLine("subworkflow.setParentOrder(parentPStep.", Step.METHODNAME_GET_PROCESS, "().", WF.METHODNAME_GET_CORRELATED_XYNA_ORDER, "())");
    cb.addLine("subworkflow.setParentStepNo(parentPStep.", Step.METHODNAME_GET_N, "())");

    cb.addLine("subworkflow = ", XynaFactory.class.getSimpleName(), ".getInstance().getProcessing().getXynaProcessCtrlExecution().", StepFunction.METHODNAME_START_ORDER_SYNC, "(subworkflow, false)");

    cb.addLine("return subworkflow");
  }


  @Override
  public void createMethodSignature(CodeBuffer cb, boolean includeImplementation,
                                       Set<String> importedClassesFqStrings, String operationName, String ... additionalInputParameters) {
    cb.add(XynaOrderServerExtension.class.getSimpleName() + " ");
    cb.add(getName()).add("(");
    for (String additionalInputParameter : additionalInputParameters) {
      cb.addListElement(additionalInputParameter);
    }
    cb.addListElement(FractalProcessStep.class.getSimpleName() + " parentPStep");
    for (AVariable v : wf.getInputVars()) {
      cb.addListElement(v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + " " + v.getVarName());
    }
    cb.add(") throws ", XynaException.class.getSimpleName());
  }

  @Override
  public void generateJavaForInvocation(CodeBuffer cb, String operationName, String ... additionalInputParameters) {    
    throw new RuntimeException(); //FIXME
  }
  

  @Override
  public List<AVariable> getInputVars() {
    return wf.getInputVars();
  }


  @Override
  public List<AVariable> getOutputVars() {
    return wf.getOutputVars();
  }


  @Override
  protected ArrayList<DOM> getDependentDoms() {
    return new ArrayList<DOM>();
  }


  @Override
  protected List<ExceptionGeneration> getDependentExceptions() {
    return new ArrayList<ExceptionGeneration>();
  }

}

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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch.ExceptionHierarchyComparator;
import com.gip.xyna.xprc.xfractwfe.generation.serviceimpl.XynaExceptionResultingFromWorkflowCall;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;



/**
 * operations in services, die als workflow implementiert sind
 */
public class WorkflowCallInService extends WorkflowCall {
  
  private static final String _METHODNAME_CREATE_OR_GET_XYNA_ORDER_ORIG = "createOrGetXynaOrder";
  protected static final String METHODNAME_CREATE_OR_GET_XYNA_ORDER;
  private static final String _METHODNAME_SUSPENDED_ORIG = "suspended";
  protected static final String METHODNAME_SUSPENDED;
  private static final String _METHODNAME_IS_FIRST_EXECUTION_ORIG = "isFirstExecution";
  protected static final String METHODNAME_IS_FIRST_EXECUTION;
  
  
  static {
    //methoden namen auf diese art gespeichert k�nnen von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_CREATE_OR_GET_XYNA_ORDER = ChildOrderStorageStack.class.getDeclaredMethod(_METHODNAME_CREATE_OR_GET_XYNA_ORDER_ORIG, String.class, long.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CREATE_OR_GET_XYNA_ORDER_ORIG + " not found", e);
    }
    try {
      METHODNAME_SUSPENDED = ChildOrderStorageStack.class.getDeclaredMethod(_METHODNAME_SUSPENDED_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_SUSPENDED_ORIG + " not found", e);
    }
    try {
      METHODNAME_IS_FIRST_EXECUTION = ChildOrderStorageStack.class.getDeclaredMethod(_METHODNAME_IS_FIRST_EXECUTION_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_IS_FIRST_EXECUTION_ORIG + " not found", e);
    }
  }

  private static final String VARNAME_XYNAORDER = "_subworkflowXynaOrder";
  private static final String VARNAME_EXCEPTION = "_exceptionVari";
  private static final String VARNAME_CHILDORDERSTORAGE_STACK = "_childOrderStorageStack";


  public WorkflowCallInService(DOM parent) {
    super(parent);
  }


  @Override
  protected void getImports(Set<String> imports) {
    //nun noch die parameter der lokal deklarierten schnittstelle. z.b. k�nnte da eine exception definiert sein, die im workflow nicht geworfen wird
    //die schnittstellen parameter vom workflow sind nicht so wichtig, weil die im code nicht verwendet werden
    
    for (AVariable v : getInputVars()) {
      imports.add(v.getFQClassName());
    }
    for (AVariable v : getOutputVars()) {
      imports.add(v.getFQClassName());
    }
    for (AVariable v : getThrownExceptions()) {
      imports.add(v.getFQClassName());
    }
    
    imports.add(ChildOrderStorage.class.getName());
    imports.add(DOM.getNameForImport(ChildOrderStorageStack.class));
    imports.add(ProcessSuspendedException.class.getName());
    imports.add(XynaException.class.getName());
    imports.add(XynaOrderServerExtension.class.getName());
    imports.add(XynaExceptionResultingFromWorkflowCall.class.getName());
    imports.add(XynaFactory.class.getName());
  }


  /**
   * @param e ist ein operation element, welches ein wf-call kind-element besitzt 
   */
  public void parseXmlInternally(Element e) throws XPRC_InvalidPackageNameException {
    parseXMLOperation(e);

    Element wfCallElement = XMLUtils.getChildElementByName(e, GenerationBase.EL.WORKFLOW_CALL);

    // parse operation sonst
    setWf(wfCallElement.getAttribute(GenerationBase.ATT.REFERENCEPATH) + "." +
          wfCallElement.getAttribute(GenerationBase.ATT.REFERENCENAME),
          parent.revision);
  }


  public void setWf(String fqn, Long revision) throws XPRC_InvalidPackageNameException {
    wf = parent.getCachedWFInstanceOrCreate(fqn, parent.revision);
    wfFQClassName = wf.getFqClassName();
    wfClassName = wf.getSimpleClassName();
  }


  @Override
  protected void generateJavaImplementation(CodeBuffer cb, Set<String> importedClassesFqStrings) {
    cb.addLine(ChildOrderStorageStack.class.getSimpleName(), " ", VARNAME_CHILDORDERSTORAGE_STACK, " = ",
               ChildOrderStorage.class.getSimpleName(), ".", Step.FIELDNAME_CHILD_ORDER_STORAGE_STACK, ".get()");
    cb.addLine(XynaOrderServerExtension.class.getSimpleName(), " ", VARNAME_XYNAORDER, " = ", VARNAME_CHILDORDERSTORAGE_STACK, ".",
               METHODNAME_CREATE_OR_GET_XYNA_ORDER, "(\"", wf.getFqClassName(), "\", ", RevisionManagement.class.getName(),
               ".getRevisionByClass(getClass()))");
    cb.addLine("try {");

    cb.add(VARNAME_XYNAORDER, ".", StepFunction.METHODNAME_XYNA_ORDER_SET_INPUT_PAYLOAD, "(");
    if (getInputVars().size() != 0) {
      cb.add("new Container(");
    }

    cb.addListElement("this"); //instanz �bergeben

    for (AVariable v : getInputVars()) {
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

    if (getInputVars().size() != 0) {
      cb.add(")");
    }

    cb.add(")"); //set inputpayload
    cb.addLB();
    cb.addLine(XynaFactory.class.getSimpleName(), ".getInstance().getProcessing().getXynaProcessCtrlExecution().", StepFunction.METHODNAME_START_ORDER_SYNC, "(",
               VARNAME_XYNAORDER, ", !", VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_IS_FIRST_EXECUTION, "())");

    //output verarbeiten
    if (getOutputVars() == null || getOutputVars().size() == 0) {

    } else if (getOutputVars().size() == 1) {
      cb.addLine("return (",
                 getOutputVars().get(0).getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, false), ")",
                 VARNAME_XYNAORDER, ".", StepFunction.METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()");
    } else {
      cb.addLine("return (", Container.class.getSimpleName(), ")", VARNAME_XYNAORDER, ".", StepFunction.METHODNAME_XYNA_ORDER_GET_OUTPUT_PAYLOAD, "()");
    }

    //exceptions checken
    List<ExceptionVariable> sortedExceptions = new ArrayList<ExceptionVariable>(getThrownExceptions());
    Collections.sort(sortedExceptions, new ExceptionHierarchyComparator());
    for (ExceptionVariable exception : sortedExceptions) {
      cb.addLine("} catch (", exception.getFQClassName(), " ", VARNAME_EXCEPTION, ") {");
      cb.addLine("throw (", exception.getFQClassName(), ") ", VARNAME_EXCEPTION);
    }
    cb.addLine("} catch (", XynaException.class.getSimpleName(), " ", VARNAME_EXCEPTION, ") {");
    cb.addLine("throw new ", XynaExceptionResultingFromWorkflowCall.class.getSimpleName(), "(", VARNAME_EXCEPTION, ")");
    cb.addLine("} catch (", ProcessSuspendedException.class.getSimpleName(), " ", VARNAME_EXCEPTION, ") {");
    cb.addLine(VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_SUSPENDED, "()");
    cb.addLine("throw ", VARNAME_EXCEPTION);
    cb.addLine("}");
    cb.addLB();
  }


  @Override
  public void generateJavaForInvocation(CodeBuffer cb, String operationName,
                                        String... additionalInputParameters) {

    cb.add(operationName).add("(");
    for (String additionalInputParameter : additionalInputParameters) {
      cb.addListElement(additionalInputParameter);
    }
    for (AVariable v : getInputVars()) {
      cb.addListElement(v.getVarName());
    }
    cb.add(")");
  }


  @Override
  public void createMethodSignature(CodeBuffer cb, boolean includeImplementation, Set<String> importedClassesFqStrings,
                                    String operationName, String... additionalInputParameters) {
    cb.add(getOutputParameterOfMethodSignature(importedClassesFqStrings), " ");
    cb.add(operationName).add("(");
    for (String additionalInputParameter : additionalInputParameters) {
      cb.addListElement(additionalInputParameter);
    }

    for (AVariable v : getInputVars()) {
      if (includeImplementation) {
        cb.addListElement(v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + " "
            + v.getVarName());
        //FIXME list generics extensions nicht abw�rtskompatibel zu alten service-impls. siehe auch klasse javaoperation
        //cb.addListElement((v.isList() ? "List " : v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + " ") + v.getVarName());
      } else {
        cb.addListElement(v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + " "
            + v.getVarName());
      }
    }

    cb.add(")");
    if (getThrownExceptions().size() > 0) {
      cb.add(" throws ");
      List<ExceptionVariable> exceptions = getThrownExceptions();
      for (int i = 0; i < getThrownExceptions().size(); i++) {
        ExceptionVariable exceptionVar = exceptions.get(i);
        if (exceptionVar.isPrototype()) {
          throw new RuntimeException("Operation " + operationName + " throws prototype exception");
        }
        cb.add(exceptionVar.getClassName(importedClassesFqStrings));
        if (i < getThrownExceptions().size() - 1) {
          cb.add(", ");
        }
      }
    }
  }

}

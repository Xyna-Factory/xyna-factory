/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_INPUT_IS_NULL;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.exceptions.XPRC_TOO_FEW_PROCESS_INPUT_PARAMETERS;
import com.gip.xyna.xprc.exceptions.XPRC_TOO_MANY_PROCESS_INPUT_PARAMETERS;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Scope;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



/**
 * lokale variablen, lokale steps.
 */
public class ScopeStep extends Step {
  
  private static final String _METHODNAME_GET_PARENT_SCOPE_ORIG = "getParentScope";
  protected static final String METHODNAME_GET_PARENT_SCOPE;
  private static final String _METHODNAME_SET_INPUT_VARS_ORIG = "setInputVars";
  protected static final String METHODNAME_SET_INPUT_VARS;
  private static final String _METHODNAME_GET_OUTPUT_ORIG = "getOutput";
  protected static final String METHODNAME_GET_OUTPUT;
  private static final String _METHODNAME_GET_START_STEPS_ORIG = "getStartSteps";
  protected static final String METHODNAME_GET_START_STEPS;
  private static final String _METHODNAME_GET_ALL_STEPS_ORIG = "getAllSteps";
  protected static final String METHODNAME_GET_ALL_STEPS;
  protected static final String METHODNAME_GET_ALL_LOCAL_STEPS = "getAllLocalSteps";

  
  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_GET_PARENT_SCOPE = Scope.class.getDeclaredMethod(_METHODNAME_GET_PARENT_SCOPE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_PARENT_SCOPE_ORIG + " not found", e);
    }
    try {
      METHODNAME_SET_INPUT_VARS = Scope.class.getDeclaredMethod(_METHODNAME_SET_INPUT_VARS_ORIG, GeneralXynaObject.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_SET_INPUT_VARS_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_OUTPUT = Scope.class.getDeclaredMethod(_METHODNAME_GET_OUTPUT_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_OUTPUT_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_START_STEPS = Scope.class.getDeclaredMethod(_METHODNAME_GET_START_STEPS_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_START_STEPS_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_ALL_STEPS = Scope.class.getDeclaredMethod(_METHODNAME_GET_ALL_STEPS_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_ALL_STEPS_ORIG + " not found", e);
    }
  }

  private static final String VARIABLE_NAME_STARTSTEPS = "startSteps";
  private static final String VARIABLE_NAME_ALLSTEPS = "allSteps";
  
  private AVariable[] inputVars;
  private AVariable[] outputVars;
  protected StepSerial childStep;
  private List<ServiceVariable> privateServiceVars = new ArrayList<ServiceVariable>();
  private List<ExceptionVariable> privateExceptionVars = new ArrayList<ExceptionVariable>();

  public ScopeStep(ScopeStep parentScope, AVariable[] localInputVars, AVariable[] localOutputVars, GenerationBase creator) {
    super(parentScope, creator);
    this.inputVars = localInputVars;
    this.outputVars = localOutputVars;
  }
  
  public ScopeStep(AVariable[] localInputVars, AVariable[] localOutputVars, GenerationBase creator) {
    super(creator);
    this.inputVars = localInputVars;
    this.outputVars = localOutputVars;
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitScopeStep( this );
  }
  
  public void addPrivateVariable(AVariable privateVar) {
    if (privateVar instanceof ServiceVariable) {
      privateServiceVars.add((ServiceVariable) privateVar);
    } else if (privateVar instanceof ExceptionVariable) {
      privateExceptionVars.add((ExceptionVariable) privateVar);
    } else {
      throw new RuntimeException("unsupported var type " + privateVar);
    }
  }
  
  public void setChildStep(StepSerial childStep) {
    this.childStep = childStep;
  }


  protected void generateJavaClassHeader(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    cb.addLine("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName(), "<", getParentScope()
        .getClassName(), "> implements ", Scope.class.getSimpleName(), " {");
    cb.addLB();
    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calculateSerialVersionUID()), "L");
    cb.addLB();
  }


  protected long calculateSerialVersionUID() {
    List<Pair<String, String>> types = new ArrayList<Pair<String,String>>();
    for (AVariable v : inputVars) {
      types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
    }
    for (AVariable v : outputVars) {
      types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
    }
    for (AVariable v : privateServiceVars) {
      types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
    }
    for (AVariable v : privateExceptionVars) {
      types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
    }
    List<Step> allLocalSteps = getAllLocalSubSteps(true);
    for (Step s : allLocalSteps) {
      if (s instanceof ForEachScopeStep) {
        continue;
      }
      List<ServiceVariable> otherVars = s.getServiceVariables();
      if (otherVars != null) {
        for (ServiceVariable v : otherVars) {
          types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
        }
      }
      List<ExceptionVariable> eVars = s.getExceptionVariables();
      if (eVars != null) {
        for (ExceptionVariable v : eVars) {
          types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
        }
      }
      if (!(s instanceof ScopeStep)) {
        //typ des steps mit beachten - weil ein ausgetauschter step den gleichen namen hat, aber für die serialisierung nicht mehr kompatibel sein wird
        types.add(Pair.of(s.getVarName(), s.getClassName() + "$" + s.getClass().getSimpleName()));
      }
    }
    types.add(Pair.of(VARIABLE_NAME_ALLSTEPS, "A$" + FractalProcessStep.class.getName()));
    types.add(Pair.of(VARIABLE_NAME_STARTSTEPS, "A$" + FractalProcessStep.class.getName()));

    return GenerationBase.calcSerialVersionUID(types);
  }

  
  protected void generateJavaConstructor(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    //konstruktor
    cb.addLine("public ", getClassName(), "() {");
    cb.addLine("super(" + getIdx(), ")");
    cb.addLine("}").addLB();
  }


  protected void generateJavaAdditionalPreEnd(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {

  }


  /**
   * alle steps, die die scope als membervars enthält. also ohne this und ohne steps, die unterhalb von weiteren scopes
   * liegen.
   */
  protected List<Step> getAllLocalSubSteps(boolean includeChildScopeStep) {
    List<Step> steps = new ArrayList<Step>();
    addLocalStepsRecursively(steps, childStep.getProxyForCatch(), includeChildScopeStep);
    return steps;
  }
  

  private void addLocalStepsRecursively(List<Step> steps, Step currentStep, boolean includeChildScopeStep) {
    if (currentStep instanceof ScopeStep) {
      if (includeChildScopeStep) {
        steps.add(currentStep);
      }
      return;
    } else {
      steps.add(currentStep);
      List<Step> childSteps = currentStep.getChildSteps();
      if (childSteps != null) {
        for (Step step : childSteps) {
          addLocalStepsRecursively(steps, step, includeChildScopeStep);
        }
      }
    }
  }

  /**
   * macht variablendeklaration transient, falls exceptionvariable.
   * in read/writeObject behandelt
   */
  private static void createJavaForVariableDeclaration(CodeBuffer cb, AVariable v,
                                                       HashSet<String> importedClassesFqStrings, Set<ExceptionVariable> transientExceptionVariables) {
    cb.add("private ");
    if (v instanceof ExceptionVariable) {
      cb.add("transient ");
      transientExceptionVariables.add((ExceptionVariable)v);
    }
    cb.add(v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings), " ", v.getVarName()).addLB();
  }

  protected void generateMemberVariableDeclarations(CodeBuffer cb, HashSet<String> importedClassesFqStrings, Set<ExceptionVariable> transientExceptionVariables) {
    //local var declaration input/output
    for (AVariable sv : inputVars) {
      createJavaForVariableDeclaration(cb, sv, importedClassesFqStrings, transientExceptionVariables);
    }
    for (AVariable v : outputVars) {
      createJavaForVariableDeclaration(cb, v, importedClassesFqStrings, transientExceptionVariables);
    }
    for (AVariable v : privateServiceVars) {
      createJavaForVariableDeclaration(cb, v, importedClassesFqStrings, transientExceptionVariables);
    }
    for (AVariable v : privateExceptionVars) {
      createJavaForVariableDeclaration(cb, v, importedClassesFqStrings, transientExceptionVariables);
    }
  }


  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
      XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException {

    generateJavaClassHeader(cb, importedClassesFqStrings);
    generateJavaConstructor(cb, importedClassesFqStrings);
    generateJavaStaticInitialization(cb, importedClassesFqStrings);

    List<Step> allLocalSteps = getAllLocalSubSteps(true);
    Set<ExceptionVariable> transientExceptionVariables = new HashSet<ExceptionVariable>();

    // variable declarations
    generateMemberVariableDeclarations(cb, importedClassesFqStrings, transientExceptionVariables);

    //weitere variablendeklaration

    for (Step s : allLocalSteps) {
      if (s instanceof ForEachScopeStep) {
        continue;
      }
      List<ServiceVariable> otherVars = s.getServiceVariables();
      if (otherVars != null) {
        for (ServiceVariable v : otherVars) {
          createJavaForVariableDeclaration(cb, v, importedClassesFqStrings, transientExceptionVariables);
        }
      }
      List<ExceptionVariable> eVars = s.getExceptionVariables();
      if (eVars != null) {
        for (ExceptionVariable v : eVars) {
          createJavaForVariableDeclaration(cb, v, importedClassesFqStrings, transientExceptionVariables);
        }
      }
      if (s instanceof StepFunction &&
          ((StepFunction)s).isRemoteCall()) {
        ProgrammaticServiceBasedVariable remoteCallVar = new ProgrammaticServiceBasedVariable(creator, Service.getRemoteCallService("-1", creator), Service.generateRemoteCallVarName(s));
        createJavaForVariableDeclaration(cb, remoteCallVar, importedClassesFqStrings, transientExceptionVariables);
      }
      
    }
    
    cb.addLB();

    generateJavaSetInputVars(cb, importedClassesFqStrings);
    generateJavaGetOutputVars(cb, importedClassesFqStrings);


    for (Step s : allLocalSteps) {
      if (!(s instanceof ScopeStep)) {
        cb.addLine("private ", s.getClassName(), " ", s.getVarName(), " = new ", s.getClassName(), "()");
        if (s instanceof StepChoice) {
          //Für Alias-Fälle in Choices einen zusätzlichen Step generieren. Diese sind vom Typ des referenzierten Steps.
          //Die Steps vom eigenen Typ werden bei Alias-Fällen nicht mehr verwendet, werden aber wegen
          //Abwärtskompatibilität weiter generiert.
          StepChoice choice = (StepChoice)s;
          choice.generateAliasSteps(cb);
        }
      }
    }
    cb.addLB();

    //muss nicht reinitialisiert werden, ist immer der gleiche step
    cb.add("private " + FractalProcessStep.class.getSimpleName() + "[] " + VARIABLE_NAME_STARTSTEPS + " = new "
        + FractalProcessStep.class.getSimpleName() + "[] {");
    cb.addListElement(childStep.getProxyForCatch().getVarName());
    cb.add("};").addLB();
    //muss reinitialisiert werden, weil kann dynamisch sein (foreach oder sowas)
    cb.addLine("private ", FractalProcessStep.class.getSimpleName(), "[] ", VARIABLE_NAME_ALLSTEPS);

    // initialize vars für wiederverwendung durch workflowpools
    generateInitializeMemberVars(cb, importedClassesFqStrings, allLocalSteps );
    
    cb.addLine("public ", FractalProcessStep.class.getSimpleName(), "[] ", METHODNAME_GET_ALL_LOCAL_STEPS, "() {");
    cb.addLine("return ", VARIABLE_NAME_ALLSTEPS);
    cb.addLine("}").addLB();
    
    
    cb.addLine("public ", FractalProcessStep.class.getSimpleName(), "[] ", METHODNAME_GET_START_STEPS, "() {")
        .addLine("return ", VARIABLE_NAME_STARTSTEPS).addLine("}").addLB();

    //entweder dynamisch oder hardgecodet alle steps bestimmen.
    //hardgecodet, falls keine foreach-kindsteps vorhanden sind (diese erzeugen eine dynamische anzahl
    //von steps) und keine scopesteps
    //TODO performance: falls liste für for-each konstant belegt ist, könnte man das auch hardgecodet erzeugen.
    if (containsForeachOrScopeSubStep()) {
      cb.addLine("public " + FractalProcessStep.class.getSimpleName() + "[] ", METHODNAME_GET_ALL_STEPS, "() {");
      cb.addLine("List<", FractalProcessStep.class.getSimpleName(), "> l = new ArrayList<", FractalProcessStep.class.getSimpleName(), ">()"); //TODO performance: liste mit korrekter länge initialisieren
      cb.addLine("l.addAll(Arrays.asList(", VARIABLE_NAME_ALLSTEPS, "))");
      for (Step s : allLocalSteps) {
        if (s instanceof StepForeach) {
          StepForeach sf = (StepForeach) s;
          
          /*
           * es kann sein, dass die childsteps gleichzeitig von einem anderen thread erweitert werden.
           * dann existieren einigermassen gleichzeitig auch steps, die hier nicht gefunden werden.
           * das muss aber vom aufrufer der methode abgefangen werden.
           * typischerweise wird ein workflow-globales flag gesetzt, welches verhindert, dass
           * weitere steps irgendetwas "schlimmes" tun.
           */
          cb.addLine("for (int i=0; i<", sf.getVarName(), ".getChildSteps().size(); i++) {");
          cb.addLine(FractalProcessStep.class.getSimpleName(), " childScope = ", sf.getVarName(), ".getChildSteps().get(i);");
          cb.addLine("if (childScope != null) {");
          cb.addLine("l.add(childScope)");
          cb.addLine("l.addAll(Arrays.asList(((", Scope.class.getName(), ")childScope).", METHODNAME_GET_ALL_STEPS, "()))");
          cb.addLine("}"); //end if
          cb.addLine("}"); //end for
        }
        //woanders kommen scopes derzeit nicht her...
      }
      cb.addLine("return l.toArray(new " + FractalProcessStep.class.getSimpleName() + "[l.size()])").addLine("}")
          .addLB();
    } else {
      cb.addLine("public " + FractalProcessStep.class.getSimpleName() + "[] ", METHODNAME_GET_ALL_STEPS, "() {");
      cb.addLine("return ", VARIABLE_NAME_ALLSTEPS).addLine("}").addLB();
    }

    for (Step s : allLocalSteps) {
      s.generateJava(cb, importedClassesFqStrings);
    }

    generateJavaStepMethods(cb, importedClassesFqStrings);
    generateJavaAdditionalPreEnd(cb, importedClassesFqStrings);
    
    generateJavaReadWriteObject(cb, importedClassesFqStrings, transientExceptionVariables);

    //end class
    cb.addLine("}").addLB();
  }
  
  
  protected void generateJavaStaticInitialization(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
  }


  private void generateInitializeMemberVars(CodeBuffer cb, HashSet<String> importedClassesFqStrings, List<Step> allLocalSteps) {
    cb.addLB().addLine("protected void ", WF.METHODNAME_INITIALIZE_MEMBER_VARS, "() {");
    for (AVariable sv : inputVars) {
      sv.generateJava(cb, false, importedClassesFqStrings);
    }
    for (AVariable v : outputVars) {
      v.generateJava(cb, false, importedClassesFqStrings);
    }
    for (AVariable v : privateServiceVars) {
      v.generateJava(cb, false, importedClassesFqStrings);
    }
    for (AVariable v : privateExceptionVars) {
      v.generateJava(cb, false, importedClassesFqStrings);
    }
    for (Step s : allLocalSteps) {
      if (s instanceof ForEachScopeStep) {
        continue;
      }
      List<ServiceVariable> otherVars = s.getServiceVariables();
      if (otherVars != null) {
        for (ServiceVariable v : otherVars) {
          v.generateJava(cb, false, importedClassesFqStrings);
        }
      }
      List<ExceptionVariable> eVars = s.getExceptionVariables();
      if (eVars != null) {
        for (ExceptionVariable v : eVars) {
          v.generateJava(cb, false, importedClassesFqStrings);
        }
      }
      if (s instanceof StepFunction &&
          ((StepFunction)s).isRemoteCall()) {
        ProgrammaticServiceBasedVariable remoteCallVar = new ProgrammaticServiceBasedVariable(creator, Service.getRemoteCallService("-1", creator), Service.generateRemoteCallVarName(s));
        remoteCallVar.generateJava(cb, false, importedClassesFqStrings);
      }
    }
    
    generateJavaAdditionalInitializeMemberVars(cb, importedClassesFqStrings);

    cb.add(VARIABLE_NAME_ALLSTEPS, " = new " + FractalProcessStep.class.getSimpleName() + "[]{");
    for (Step s : allLocalSteps) {
      if (!(s instanceof ScopeStep)) {
        cb.addListElement(s.getVarName());
        if (s instanceof StepChoice) {
          //für Choices die Alias-Steps zu allSteps hinzufügen
          StepChoice choice = (StepChoice)s;
          choice.addAliasStepVarNames(cb);
        }
      }
    }
    cb.add("};").addLB();

    // cb.addLine("}").addLB();

    cb.addLine("for (", FractalProcessStep.class.getSimpleName(), " s : ",VARIABLE_NAME_ALLSTEPS ,") {").
       addLine("s.", METHODNAME_INIT, "(this)").
       addLine("}");
    
    cb.addLine("}").addLB();
  }
  
  
  protected void generateJavaAdditionalInitializeMemberVars(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
  }
  
  
  public static final Comparator<ExceptionVariable> comparatorForExceptionVariableSerialization = new Comparator<ExceptionVariable>() {

    public int compare(ExceptionVariable o1, ExceptionVariable o2) {
      return o1.getVarName().compareTo(o2.getVarName());
    }
    
  };
  
  private void generateJavaReadWriteObject(CodeBuffer cb, Set<String> currentImportsFqClasses, Set<ExceptionVariable> transientExceptionVariables) {
    //irgendwie muss man bei der deserialisierung wieder an die classloader der causes von exceptions kommen => dazu serializableclassloadedobject verwenden
    
    //TODO sortierung derart, dass eine umbenennung einer variablen nicht zur inkompatibilität mit dem alten stand führt.
    //sortieren, damit die reihenfolge immer gleich ist:
    List<ExceptionVariable> exceptionVariables = new ArrayList<ExceptionVariable>(transientExceptionVariables);
    Collections.sort(exceptionVariables, comparatorForExceptionVariableSerialization);
    cb.addLine("private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {");
    cb.addLine("s.defaultReadObject()");
    for (ExceptionVariable transientExceptionVariable : exceptionVariables) {
      if (transientExceptionVariable.isList()) {
        cb.addLine("{");
        cb.addLine("List<" + SerializableClassloadedException.class.getSimpleName() + "> tmpList = (List<" + SerializableClassloadedException.class.getSimpleName() + ">) s.readObject()" );
        cb.addLine("List<" + transientExceptionVariable.getClassNameDirectly() + "> dataList = new ArrayList<" + transientExceptionVariable.getClassNameDirectly() + ">()" );
        cb.addLine("for (" + SerializableClassloadedException.class.getSimpleName() + " scxo : tmpList) {");
        cb.addLine("dataList.add((" + transientExceptionVariable.getClassNameDirectly() + ") scxo.", XynaObjectCodeGenerator.METHODNAME_GET_THROWABLE, "());");
        cb.addLine("}");
        cb.addLine(transientExceptionVariable.getVarName() + " = dataList");
        cb.addLine("}");
      } else {
        cb.addLine(transientExceptionVariable.getVarName(), " = (", transientExceptionVariable
            .getEventuallyQualifiedClassNameNoGenerics(currentImportsFqClasses), ") ((",
                   SerializableClassloadedException.class.getSimpleName(), ") s.readObject()).", XynaObjectCodeGenerator.METHODNAME_GET_THROWABLE, "()");
      }
    }
    generateJavaAdditionalReadWriteObject(cb,currentImportsFqClasses,true);
    cb.addLine("}").addLB();

    cb.addLine("private void writeObject(ObjectOutputStream s) throws IOException {");
    cb.addLine("s.defaultWriteObject()");
    for (ExceptionVariable transientExceptionVariable : exceptionVariables) {
      if (transientExceptionVariable.isList()) {
        cb.addLine("{");
        cb.addLine("List<" + SerializableClassloadedException.class.getSimpleName() + "> tmpList = new ArrayList<" + SerializableClassloadedException.class.getSimpleName() + ">()" );
        cb.addLine("for (" + transientExceptionVariable.getClassNameDirectly() + " ex : " + transientExceptionVariable.getVarName() + ") {");
        cb.addLine("tmpList.add(new " + SerializableClassloadedException.class.getSimpleName() + "(ex))");
        cb.addLine("}");
        cb.addLine("s.writeObject(tmpList)");
        cb.addLine("}");
      } else {
        cb.addLine("s.writeObject(new ", SerializableClassloadedException.class.getSimpleName(), "(",
                   transientExceptionVariable.getVarName(), "))");
      }
    }
    generateJavaAdditionalReadWriteObject(cb,currentImportsFqClasses,false);
    cb.addLine("}");
    cb.addLB();
  }
  
  protected void generateJavaAdditionalReadWriteObject(CodeBuffer cb, Set<String> importedClassesFqStrings, boolean read) {
  }


  //überschrieben von wf, weil man dort die methoden nicht braucht
  protected void generateJavaStepMethods(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    //executeInternally
    
    appendExecuteInternally(cb, importedClassesFqStrings);

    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getName(), " {");
    cb.addLine("}").addLB();

    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<", getClassName(),
               ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    cb.addLine("return ", VARIABLE_NAME_STARTSTEPS);
    cb.addLine("}").addLB();

    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return 1");
    cb.addLine("}").addLB();
    
    generatedGetRefIdMethod(cb);

    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, null, null, cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, null, null, cb, importedClassesFqStrings);
  }
  
  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getName(), " {");
    if( this instanceof ForEachScopeStep ) {
      cb.addLine("for (", FractalProcessStep.class.getSimpleName(), " s : ", VARIABLE_NAME_STARTSTEPS, ") {");
      cb.addLine("s.", METHODNAME_SET_PARENT_STEP, "(this)");
      cb.addLine("}");
    }
    cb.addLine(METHODNAME_EXECUTE_CHILDREN,"(0)");
    cb.addLine("}").addLB();
  }
  
  @Override
  public List<AVariable> getInputVars() {
    if(inputVars != null)
      return Arrays.asList(inputVars);
    else
      return new ArrayList<AVariable>();
  }
  
  @Override
  public List<AVariable> getOutputVars() {
    if(outputVars != null)
      return Arrays.asList(outputVars);
    else
      return new ArrayList<AVariable>();
  }

  public ExceptionVariable[] getExceptionVars() {
    return new ExceptionVariable[0];
  }

  private boolean containsForeachOrScopeSubStep() {
    for (Step s : getAllLocalSubSteps(true)) {
      if (s instanceof StepForeach) {
        return true;
      } else if (s instanceof ScopeStep) {
        return true;
      }
    }
    return false;
  }


  private String getOutputType(HashSet<String> importedClasseNames) {
    if (outputVars.length != 1) {
      return Container.class.getSimpleName();
    } else if (outputVars[0].isList()) {
      if (outputVars[0] instanceof ExceptionVariable) {
        return GeneralXynaObjectList.class.getSimpleName() + "<? extends " + outputVars[0].getFQClassName() + ">";
      } else {
        return XynaObjectList.class.getSimpleName() + "<? extends " + outputVars[0].getFQClassName() + ">";
      }
    } else {
      return outputVars[0].getEventuallyQualifiedClassNameNoGenerics(importedClasseNames);
    }
  }


  private void generateJavaGetOutputVars(CodeBuffer cb, HashSet<String> importedClasseNames) {
    cb.addLine("public " + getOutputType(importedClasseNames) + " ", METHODNAME_GET_OUTPUT, "() {");
    if (outputVars.length != 1) { // Container
      cb.add("return new " + Container.class.getSimpleName() + "(");
      for (AVariable v : outputVars) {
        if (v.isList()) {
          if (v instanceof ExceptionVariable) {
            cb.addListElement("new " + GeneralXynaObjectList.class.getSimpleName() + "<"
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ">(" + v.getVarName() + ", "
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ".class)");
          } else {
            cb.addListElement("new " + XynaObjectList.class.getSimpleName() + "<"
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ">(" + v.getVarName() + ", "
                + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ".class)");
          }
        } else {
          cb.addListElement(v.getVarName());
        }
      }
      cb.add(")").addLB();
    } else if (outputVars[0].isList()) {
      AVariable v = outputVars[0];
      if (v instanceof ExceptionVariable) {
        cb.addLine("return new " + GeneralXynaObjectList.class.getSimpleName() + "<"
            + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ">(" + outputVars[0].getVarName()
            + ", " + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ".class)");
      } else {
        cb.addLine("return new " + XynaObjectList.class.getSimpleName() + "<"
            + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ">(" + outputVars[0].getVarName()
            + ", " + v.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames) + ".class)");
      }
    } else {
      cb.addLine("return " + outputVars[0].getVarName());
    }
    cb.addLine("}").addLB();
  }


  private void generateJavaSetInputVars(CodeBuffer cb, HashSet<String> importedClasseNames) {
    cb.addLine("public int ", WF.METHODNAME_GET_NEEDED_INPUT_VARS_COUNT, "() {")
      .addLine("return "+(inputVars.length))
      .addLine("}").addLB();

    cb.addLine("public void ", METHODNAME_SET_INPUT_VARS, "(", GeneralXynaObject.class.getSimpleName(), " o) throws ",
               XynaException.class.getSimpleName(), " {").addLB();

    // catch null for workflows that require input parameters
    if (inputVars.length == 0) {
      cb.addLine("if (o == null) {");
      cb.addLine("o = new ", Container.class.getSimpleName(), "()");
      cb.addLine("}");
    } else if (inputVars.length == 1) {
    } else {
      cb.addLine("if (o == null) {");
      cb.addLine("throw new ", XPRC_PROCESS_INPUT_IS_NULL.class.getName(), "(getClass().getName())").addLine("}");
    }
    String invalidArgumentException = XPRC_INVALID_INPUT_PARAMETER_TYPE.class.getName();
    /*
     *      XynaProcess.throwExceptionOfMismatchingType(int position, Class<?> expected, Class<?> got) 
     */
    
    if (inputVars.length != 1) { //"Container"

      cb.addLine("if (!(o instanceof ", Container.class.getSimpleName(), ")) {");
      cb.addLine("throw new ", invalidArgumentException, "(\"0\", \"", Container.class.getName(),
                 "\", o.getClass().getName())");
      cb.addLine("}");
      cb.addLine(Container.class.getSimpleName(), " c = (", Container.class.getSimpleName(), ")o");
      cb.addLine("if (c.size() < " + inputVars.length, ") {");
      cb.addLine("throw new ", XPRC_TOO_FEW_PROCESS_INPUT_PARAMETERS.class.getSimpleName(), "(getClass().getName())");
      cb.addLine("}");
      cb.addLine("if (c.size() > ", inputVars.length + ") {");
      cb.addLine("throw new ", XPRC_TOO_MANY_PROCESS_INPUT_PARAMETERS.class.getSimpleName(), "(getClass().getName())");
      cb.addLine("}");
      int idx = 0;
      for (AVariable sv : inputVars) {

        if (sv.isList()) {
          if (sv instanceof ExceptionVariable) {
            cb.addLine("if (!(c.get(" + idx + ") != null && c.get(" + idx + ") instanceof "
                + GeneralXynaObjectList.class.getSimpleName() + ")) {");

            cb.addLine(XynaProcess.class.getName(),".", WF.METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE, "(" + idx + ", "
                + GeneralXynaObjectList.class.getSimpleName() + ".class, c.get(" + idx + ").getClass())");
            cb.addLine("}");
            cb.addLine(sv.getVarName(), " = ((", GeneralXynaObjectList.class.getSimpleName(), "<",
                sv.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ">) c.get(" + idx,
                ")).getList()");
          } else {
            cb.addLine("if (!(c.get(" + idx + ") != null && c.get(" + idx + ") instanceof "
                + XynaObjectList.class.getSimpleName() + ")) {");
            cb.addLine(XynaProcess.class.getName(),".", WF.METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE, "(" + idx, ", ", 
                XynaObjectList.class.getSimpleName(), ".class, c.get(" + idx, ").getClass())");
            cb.addLine("}");
            cb.addLine(sv.getVarName(), " = ((", XynaObjectList.class.getSimpleName(), "<",
                sv.getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ">) c.get(" + idx,
                ")).getList()");
          }
        } else {
          cb.addLine("if (c.get(" + idx, ") != null && !(c.get(" + idx, ") instanceof ",
                          sv.getEventuallyQualifiedClassNameWithGenerics(importedClasseNames), ")) {");
          cb.addLine(XynaProcess.class.getName(),".", WF.METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE, "(" + idx, ", ",
                          sv.getEventuallyQualifiedClassNameWithGenerics(importedClasseNames),
                          ".class, c.get(" + idx, ").getClass())");
          cb.addLine("}");
          cb.addLine(sv.getVarName(), " = (", sv.getEventuallyQualifiedClassNameWithGenerics(importedClasseNames),
                          ") c.get(" + idx, ")");
        }
        idx++;
      }
    } else if (inputVars[0].isList()) {
      if (inputVars[0] instanceof ExceptionVariable) {
        cb.addLine("if (!(o instanceof ", GeneralXynaObjectList.class.getSimpleName(), ")) {");
        cb.addLine(XynaProcess.class.getName(),".", WF.METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE, "(0, ", GeneralXynaObjectList.class.getName(),
                   ".class, o.getClass())");
        cb.addLine("}");

        cb.addLine(inputVars[0].getVarName(), " = ((", GeneralXynaObjectList.class.getSimpleName(), "<",
                   inputVars[0].getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ">) o).getList()");
      } else {
        cb.addLine("if (!(o instanceof ", XynaObjectList.class.getSimpleName(), ")) {");
        cb.addLine(XynaProcess.class.getName(),".", WF.METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE, "(0, ", XynaObjectList.class.getName(),
                   ".class, o.getClass())");
        cb.addLine("}");

        cb.addLine(inputVars[0].getVarName(), " = ((", XynaObjectList.class.getSimpleName(), "<",
                   inputVars[0].getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ">) o).getList()");
      }
    } else {
      cb.addLine("if (o != null && !(o instanceof ", inputVars[0].getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ")) {");
      cb.addLine(XynaProcess.class.getName(), ".", WF.METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE, "(0, ",
                 inputVars[0].getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ".class, o.getClass())");
      cb.addLine("}");
      cb.addLine(inputVars[0].getVarName(), " = (", inputVars[0].getEventuallyQualifiedClassNameNoGenerics(importedClasseNames), ") o");
    }
    cb.addLB().addLine("}").addLB();

  }

  private int stepCount = 0;


  protected int getStepCount() {
    if (getParentScope() != null) {
      return getParentScope().getStepCount();
    }
    return stepCount++;
  }

  private Service identifyServiceForThisScope(String serviceId) {
    // check inputServices
    for (AVariable sv : inputVars) {
      if (sv instanceof ServiceVariable) {
        ServiceVariable serviceVar = (ServiceVariable) sv;
        if (serviceVar.getService() != null) {
          if (serviceVar.getService().getId().equals(serviceId)) {
            return serviceVar.getService();
          }
        }
      }
    }
    
    for (Step step : getAllLocalSubSteps(false)) {
      if (step.getServices() != null) {
        for (Service s : step.getServices()) {
          if (s.getId().equals(serviceId)) {
            return s;
          }
        }
      }
      if (step.getServiceVariables() != null) {
        for (ServiceVariable sv : step.getServiceVariables()) {
          if (sv.getService() != null) {
            if (sv.getService().getId().equals(serviceId)) {
              return sv.getService();
            }
          }
        }
      }
    }

    return null;
  }


  public AVariable identifyVariableForThisScope(String id) {
    // check inputVars
    for (AVariable sv : inputVars) {
      if (sv.getId().equals(id)) {
        return sv;
      }
    }
    // check outputVars
    if(outputVars != null) {
      for (AVariable v : outputVars) {
        if (v.getId().equals(id)) {
          return v;
        }
      }      
    }

    
    for (AVariable v : privateServiceVars) {
      if (v.getId().equals(id)) {
        return v;
      }
    }
    
    for (AVariable v : privateExceptionVars) {
      if (v.getId().equals(id)) {
        return v;
      }
    }

    for (Step s : getAllLocalSubSteps(false)) {
      List<ServiceVariable> svs = s.getServiceVariables();
      if (svs != null) {
        for (ServiceVariable sv : svs) {
          if (sv.getId().equals(id)) {
            return sv;
          }
        }
      }
      List<ExceptionVariable> evs = s.getExceptionVariables();
      if (evs != null) {
        for (ExceptionVariable ev : evs) {
          if (ev.getId().equals(id)) {
            return ev;
          }
        }
      }      
    }
    return null;
  }
  
  static class Identification {
    ScopeStep scope;
    public String getScopeGetter(ScopeStep localScope) {
      return ScopeStep.getScopeGetter(localScope, scope);
    }
  }
  
  protected static String getScopeGetter(ScopeStep localScope, ScopeStep parentScope) {
    StringBuffer sb = new StringBuffer();
    while (localScope != null) {
      sb.append(METHODNAME_GET_PARENT_SCOPE).append("().");
      if (localScope == parentScope) {
         return sb.toString();
      }
      localScope = localScope.getParentScope();
    }
    return null; //FIXME
  }
  
  public static class ServiceIdentification extends Identification {
    public Service service;
  }

  public static class VariableIdentification extends Identification {
    AVariable variable;

    public AVariable getVariable() {
      return variable;
    }
  }
  
  public VariableIdentification identifyVariable(String id) throws XPRC_InvalidVariableIdException {
    VariableIdentification vi = new VariableIdentification();
    ScopeStep scope = this;
    while (scope != null) {
      AVariable var = scope.identifyVariableForThisScope(id);
      if (var != null) {
        vi.variable = var;
        vi.scope = scope;
        return vi;
      }      
      scope = scope.getParentScope();
    }
    throw new XPRC_InvalidVariableIdException(id);
  }
  
  public ServiceIdentification identifyService(String id) throws XPRC_InvalidServiceIdException {
    ServiceIdentification vi = new ServiceIdentification();
    ScopeStep scope = this;
    while (scope != null) {
      Service sv = scope.identifyServiceForThisScope(id);
      if (sv != null) {
        vi.service = sv;
        vi.scope = scope;
        return vi;
      }      
      scope = scope.getParentScope();
    }
    throw new XPRC_InvalidServiceIdException(id);
  }


  @Override
  public List<Step> getChildSteps() {
    List<Step> steps = new ArrayList<Step>();
    steps.add(childStep);
    return steps;
  }


  @Override
  public boolean replaceChild(Step oldChild, Step newChild) {
    if ( (childStep != oldChild) || !(newChild instanceof StepSerial) ) {
      return false;
    }

    childStep = (StepSerial)newChild;
    return true;
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    List<GenerationBase> deps = new ArrayList<GenerationBase>();
    for (ExceptionVariable ev : privateExceptionVars) {
      if (ev.getDomOrExceptionObject() != null) {
        deps.addAll(ev.getDependencies());
      }
    }
    for (ServiceVariable sv : privateServiceVars) {
      if (sv.getDomOrExceptionObject() != null) {
        deps.addAll(sv.getDependencies());
      }
    }
    for (Step step : getAllLocalSubSteps(false)) {
      if (step instanceof StepFunction &&
          ((StepFunction)step).isRemoteCall()) {
        deps.add(Service.getRemoteCallService("-1", creator).getDom());
      }
    }
    return deps;
  }


  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return privateExceptionVars;
  }


  @Override
  protected void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException,
                  XPRC_InvalidServiceIdException {
    imports.add(Arrays.class.getName());
    imports.add(IOException.class.getName());
    imports.add(ClassNotFoundException.class.getName());
    imports.add(ObjectInputStream.class.getName());
    imports.add(ObjectOutputStream.class.getName());
    imports.add(SerializableClassloadedException.class.getName());
    imports.add(SerializableClassloadedXynaObject.class.getName());
    for (ExceptionVariable ev : privateExceptionVars) {
      ev.getImports(imports);
    }
    for (ServiceVariable sv : privateServiceVars) {
      sv.getImports(imports);
    }
  }


  @Override
  protected List<ServiceVariable> getServiceVariables() {
    return privateServiceVars;
  }


  @Override
  protected List<Service> getServices() {
    return null;
  }


  @Override
  public boolean isExecutionDetached() {
    return false;
  }


  @Override
  protected void parseXML(Element e) throws XPRC_InvalidPackageNameException {
    parseParameter(e);
    childStep.parseXML(e);
  }

  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof ScopeStep)) {
      return true;
    }
    ScopeStep oldScopeStep = (ScopeStep)oldStep;
    
    return childStep.compareImplementation(oldScopeStep.childStep);
  }

  @Override
  protected Set<String> getAllUsedVariableIds() {
    return Collections.emptySet();
  }

  @Override
  protected void removeVariable(AVariable var) {
    //throw new RuntimeException("unsupported to remove variable " + var  + "(" + var.getLabel() + ") from step " + this);
    privateServiceVars.remove(var);
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_ParsingModelledExpressionException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidXMLMissingListValueException,
      XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {
  }

  public StepSerial getChildStep() {
    return childStep;
  }

  public void refreshVars(List<AVariable> input, List<AVariable> output) {
    this.inputVars = input.toArray(new AVariable[input.size()]);
    this.outputVars = output.toArray(new AVariable[output.size()]);
  }

  @Override
  public boolean toBeShownInAudit() {
    return false;
  }

  @Override
  public void addLabelsToParameter() {
    // copy labels for audit since those are not set in parameter-tag
    addLabelsToParameter(Arrays.asList(inputVars), Arrays.asList(outputVars));
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    // TODO Auto-generated method stub
    
  }

  public Set<AVariable> getPrivateVars() {
    Set<AVariable> s = new HashSet<>();
    s.addAll(privateServiceVars);
    s.addAll(privateExceptionVars);
    return s;
  }
  
  //TODO: maybe turn inputVars/outputVars into list
  public void addInput(AVariable inputVar) {
    if(inputVars != null) {
      AVariable[] newInputVarsSingle = new AVariable[inputVars.length + 1];
      System.arraycopy(inputVars, 0, newInputVarsSingle, 0, inputVars.length);
      inputVars = newInputVarsSingle;
      inputVars[inputVars.length - 1] = inputVar;
    }
    else {
      inputVars = new AVariable[] { inputVar };
    }
    
  }
}

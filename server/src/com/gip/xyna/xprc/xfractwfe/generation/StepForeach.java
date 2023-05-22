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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.ForEachScope;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.ParallelExecutionStep;
import com.gip.xyna.xprc.xfractwfe.base.ProcessStepDynamicChildren;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.base.parallel.CapacityBlockingParallelismLimitation;
import com.gip.xyna.xprc.xfractwfe.base.parallel.ConstantBlockingParallelismLimitation;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor.ParallelismLimitation;
import com.gip.xyna.xprc.xfractwfe.base.parallel.NoParallelismLimitation;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.OrderTypeMaxParallelism;


public class StepForeach extends Step {
  
  private static final String _METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR_ORIG = "getFractalWorkflowParallelExecutor";
  protected static final String METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR;
  private static final String _METHODNAME_PARALLEL_EXECUTOR_INIT_ORIG = "init"; 
  protected static final String METHODNAME_PARALLEL_EXECUTOR_INIT;
  private static final String _METHODNAME_PARALLEL_EXECUTOR_EXECUTE_ORIG = "execute";
  protected static final String METHODNAME_PARALLEL_EXECUTOR_EXECUTE;
  private static final String _METHODNAME_PARALLEL_EXECUTOR_COMPENSATE_ORIG = "compensate";
  protected static final String METHODNAME_PARALLEL_EXECUTOR_COMPENSATE;

  
  static {
    //methoden namen auf diese art gespeichert k�nnen von obfuscation tools mit "refactored" werden.
    // ParallelExecutionStep
    try {
      METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR = ParallelExecutionStep.class.getDeclaredMethod(_METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR_ORIG + " not found", e);
    }

    // FractalWorkflowParallelExecutor
    try {
      METHODNAME_PARALLEL_EXECUTOR_INIT = FractalWorkflowParallelExecutor.class.getDeclaredMethod(_METHODNAME_PARALLEL_EXECUTOR_INIT_ORIG, XynaProcess.class, ParallelismLimitation.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_PARALLEL_EXECUTOR_INIT_ORIG + " not found", e);
    }
    try {
      METHODNAME_PARALLEL_EXECUTOR_EXECUTE = FractalWorkflowParallelExecutor.class.getDeclaredMethod(_METHODNAME_PARALLEL_EXECUTOR_EXECUTE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_PARALLEL_EXECUTOR_EXECUTE_ORIG + " not found", e);
    }
    try {
      METHODNAME_PARALLEL_EXECUTOR_COMPENSATE = FractalWorkflowParallelExecutor.class.getDeclaredMethod(_METHODNAME_PARALLEL_EXECUTOR_COMPENSATE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_PARALLEL_EXECUTOR_COMPENSATE_ORIG + " not found", e);
    }
    
  }

  private static interface ConstructorGenerator {

    String construct(String limit);

  }
  private enum LimitType implements ConstructorGenerator {

    Capacity(CapacityBlockingParallelismLimitation.class) {

      public String construct(String limit) {
        return "new " + CapacityBlockingParallelismLimitation.class.getSimpleName() + "(" + " new "
            + OrderTypeMaxParallelism.class.getSimpleName() + "(\"" + limit + "\", getRevisionForOrderType(new "
            + DestinationKey.class.getSimpleName() + "(\"" + limit + "\")))" + ")";
      }
      
      protected Class<?>[] createImports() {
        return new Class<?>[]{CapacityBlockingParallelismLimitation.class, OrderTypeMaxParallelism.class, DestinationKey.class};
      }
      

      public void addFields(CodeBuffer cb, String limit) {
      }

    },
    Constant(ConstantBlockingParallelismLimitation.class) {
      public String construct(String limit) {
        int i = Integer.valueOf(limit);
        if (i <= 0) {
          throw new RuntimeException("Limit must be positive");
        }
        return "new " + ConstantBlockingParallelismLimitation.class.getSimpleName() + "(" + limit + ")";
      }
    },
    NoLimit(NoParallelismLimitation.class) {
      public String construct(String limit) {
        return "new " + NoParallelismLimitation.class.getSimpleName() + "()";
      }
    };

    protected Class<?> limitationClass;
    protected Class<?>[] imports;
    
    private LimitType(Class<?> limitationClass) {
      this.limitationClass = limitationClass;
    }
    
    public Class<?>[] getImports() {
      if( imports == null ) {
        imports = createImports();
      }
      return imports;
    }
    
    protected Class<?>[] createImports() {
      return new Class<?>[]{limitationClass};
    }

    public void addFields(CodeBuffer cb, String limit) {
    }
     
  }
  
  private AVariable[] inputVarsSingle;
  private AVariable[] outputVarsSingle;
  private InputConnections input;
  private String[] outputListRefs;
  private ScopeStep childScope;
  private boolean parallelExecution;
  private LimitType limitType;
  private String limit; //wert aus limit attribut. falls LimitType=Capacity, wird es �berschrieben mit dem ordertype des sub-workflows
  private String label;
  
  private final static String VARNAME_fractalWorkflowParallelExecutor = "fractWfParallelExecutor";
  private static final String LIMIT_UNKNOWN = "_unknown_";
  
  public StepForeach(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }
  
  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepForeach( this );
  }

  @Override
  public List<Step> getChildSteps() {
    List<Step> list = new ArrayList<Step>(1);
    list.add(childScope);
    return list;
  }

  @Override
  public boolean replaceChild(Step oldChild, Step newChild) {
    if ( (childScope == oldChild) && (newChild instanceof ScopeStep) ) {
      childScope = (ScopeStep)newChild;
      return true;
    }

    return false;
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    return null; //dependencies sind in dem childscope enthalten
  }

  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return null;
  }


  @Override
  protected void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException,
                  XPRC_InvalidServiceIdException {
    imports.add(ProcessStepDynamicChildren.class.getName());
    imports.add(FractalProcessStep.class.getName());
    imports.add(FractalWorkflowParallelExecutor.class.getName());
    imports.add(ForEachScope.class.getName());

    if (this.parallelExecution) {
      imports.add(ParallelExecutionStep.class.getName());
      imports.add(ParallelismLimitation.class.getCanonicalName());
      if (limitType != null) {
        for( Class<?> clazz : limitType.getImports() ) {
          imports.add( clazz.getName() );
        }
      }
    }
  }
  
  
  @Override
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }


  @Override
  protected List<ServiceVariable> getServiceVariables() {
    return null;
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

    parseId(e);
    parseUnknownMetaTags(e, new ArrayList<String>());

    parallelExecution = XMLUtils.isTrue(e, GenerationBase.ATT.FOREACH_PARALLEL);
    if (parallelExecution) {
      String s = e.getAttribute(GenerationBase.ATT.FOREACH_LIMITTYPE);
      if (s != null && s.length() > 0) {
        limitType = LimitType.valueOf(s);
        limit = e.getAttribute(GenerationBase.ATT.FOREACH_LIMIT);
      }
    }
    List<Element> inputLists = XMLUtils.getChildElementsByName(e, GenerationBase.EL.INPUT_LIST);
    
    //inputs parsen
    input = new InputConnections(inputLists.size());
    List<AVariable> inputVarsListSingle = new ArrayList<AVariable>(inputLists.size());
    for (int i = 0; i<inputLists.size(); i++) {
      input.parseSourceElement(inputLists.get(i), i);

      Element dataElement = XMLUtils.getChildElementByName(inputLists.get(i), GenerationBase.EL.DATA);
      if (dataElement != null) {
        ServiceVariable sv = new ServiceVariable(creator);
        sv.parseXML(dataElement);
        inputVarsListSingle.add(sv);
      } else {
        //exceptionvar?
        Element exceptionElement = XMLUtils.getChildElementByName(inputLists.get(i), GenerationBase.EL.EXCEPTION);
        if (exceptionElement != null) {
          ExceptionVariable ev = new ExceptionVariable(creator);
          ev.parseXML(exceptionElement);
          inputVarsListSingle.add(ev);
        }
      }
    }
    inputVarsSingle = inputVarsListSingle.toArray(new AVariable[inputLists.size()]);
    
    //outputs parsen
    //TODO duplicate code (s.o.)
    List<Element> outputLists = XMLUtils.getChildElementsByName(e, GenerationBase.EL.OUTPUT_LIST);
    outputListRefs = new String[outputLists.size()];
    List<AVariable> outputVarsListSingle = new ArrayList<AVariable>(outputLists.size());
    for (int i = 0; i<outputLists.size(); i++) {
      outputListRefs[i] = outputLists.get(i).getAttribute(GenerationBase.ATT.REFID);  //TODO path angabe verwerten
      
      Element dataElement = XMLUtils.getChildElementByName(outputLists.get(i), GenerationBase.EL.DATA);
      if (dataElement != null) {
        ServiceVariable sv = new ServiceVariable(creator);
        sv.parseXML(dataElement);
        outputVarsListSingle.add(sv);
      } else {
        //exceptionvar?
        Element exceptionElement = XMLUtils.getChildElementByName(outputLists.get(i), GenerationBase.EL.EXCEPTION);
        if (exceptionElement != null) {
          ExceptionVariable ev = new ExceptionVariable(creator);
          ev.parseXML(exceptionElement);
          outputVarsListSingle.add(ev);
        }
      }
    }

    outputVarsSingle = outputVarsListSingle.toArray(new AVariable[outputLists.size()]);
    
    childScope = new ForEachScopeStep(getParentScope(), inputVarsSingle, outputVarsSingle, creator);
    StepSerial childSerial = new StepSerial(childScope, creator);
    childScope.setChildStep(childSerial);

    childScope.parseXML(e);
    childSerial.setXmlId(-getXmlId()); // create unique id (different from id of StepForeach) for zeta-gui

    if (parallelExecution) {
      if( limitType == null ) {
        limitType = LimitType.NoLimit;
      }
      if( limit == null || limit.length() == 0 ) {
        switch( limitType ) {
          case Capacity:
            try {
              limit = getLimitForLimitTypeCapacity();
            } catch (Exception ex) { //XPRC_InvalidServiceIdException, XPRC_OperationUnknownException
              limit = LIMIT_UNKNOWN;
            }
            if( limit == null ) {
              limitType = LimitType.NoLimit;
            }
            break;
          case Constant:
            limitType = LimitType.NoLimit;
            break;
          case NoLimit:
            break;
          default:
            throw new IllegalStateException("Unexpected LimitType "+limitType);
        }
      }
    }    
  }


  private long calcSerialVersionUID() throws XPRC_InvalidVariableIdException {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    types.add(Pair.of(VARNAME_fractalWorkflowParallelExecutor, FractalWorkflowParallelExecutor.class.getName()));
    types.add(Pair.of("needsReinit", boolean.class.getName()));
    for (int i = 0; i < input.length(); i++) {
      String inputListRef = input.getVarIds()[i];
      VariableIdentification inputListVarIdent = getParentScope().identifyVariable(inputListRef);
      types.add(Pair.of("listIn_" + i, inputListVarIdent.getVariable().getUniqueTypeName()));
    }

    for (int i = 0; i < outputListRefs.length; i++) {
      String outputListRef = outputListRefs[i];
      VariableIdentification outputListVarIdent = getParentScope().identifyVariable(outputListRef);
      types.add(Pair.of("listOut_" + i, outputListVarIdent.getVariable().getUniqueTypeName()));
    }

    types.add(Pair.of("childSteps", List.class.getName()));
    return GenerationBase.calcSerialVersionUID(types);
  }


  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
                  XPRC_OperationUnknownException, XPRC_InvalidServiceIdException {
    
    label = "F"+getIdx();
        
    cb.add("private static class ", getClassName(), " extends ", ProcessStepDynamicChildren.class.getSimpleName(), "<",
           getParentScope().getClassName(), ">");
    if (this.parallelExecution) {
      cb.add(" implements ", ParallelExecutionStep.class.getSimpleName(), "<", getParentScope().getClassName(), ">");
    }
    cb.addLine(" {").addLB();

    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calcSerialVersionUID()), "L");

    cb.addLine("private ", FractalWorkflowParallelExecutor.class.getSimpleName(), " ",
               VARNAME_fractalWorkflowParallelExecutor).addLB();

    cb.addLine("private boolean needsReinit = true");

    if( parallelExecution ) {
      limitType.addFields(cb,limit);
    }
    
    //listen inputs
    for (int i = 0; i < input.length(); i++) {
      String inputListRef = input.getVarIds()[i];
      VariableIdentification inputListVarIdent = getParentScope().identifyVariable(inputListRef);
      cb.addLine("private ", inputListVarIdent.variable
                      .getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings), " listIn_" + i);
    }
    cb.addLB();

    //listen outputs
    for (int i = 0; i < outputListRefs.length; i++) {
      String outputListRef = outputListRefs[i];
      VariableIdentification outputListVarIdent = getParentScope().identifyVariable(outputListRef);
      cb.addLine("private List<", outputListVarIdent.variable
                      .getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), "> listOut_" + i);
    }
    cb.addLB();
    
    //constructor
    cb.addLine("public ", getClassName() + "() {");
    cb.addLine("super(" + getIdx() + ")");
    cb.addLine("}").addLB();

    cb.addLine("private List<", childScope.getClassName(), "> childSteps = new ArrayList<", childScope.getClassName(),
               ">()").addLB();
    cb.addLine("public List<", childScope.getClassName(), "> getChildSteps() {");
    cb.addLine("return childSteps");
    cb.addLine("}").addLB();
    
    cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
    cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
    cb.addLine("childSteps.clear();");
    cb.addLine(VARNAME_fractalWorkflowParallelExecutor, " = null");
    //listen inputs
    for (int i = 0; i < input.length(); i++) {
      cb.addLine("listIn_" + i, " = null");
    }
    //listen outputs
    for (int i = 0; i < outputListRefs.length; i++) {
      cb.addLine("listOut_" + i, " = null");
    }
    cb.addLine("needsReinit = true");
    cb.addLine("}").addLB();

    cb.add("public String ", METHODNAME_GET_LABEL, "() { return \""+GenerationBase.escapeForCodeGenUsageInString(label)+"\"; }").addLB().addLB();

    appendPrepareChildSteps(cb);

    if (parallelExecution) {
      appendGetFractalWorkflowParallelExecutor(cb);
    }

    appendExecuteInternally(cb, importedClassesFqStrings);
    
    appendCompensateInternally(cb, importedClassesFqStrings);
    
    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<" + getParentScope().getClassName()
        + ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    cb.addLine("return new ", childScope.getClassName(), "[]{childSteps.get(i)};");
    cb.addLine("}").addLB();

    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return childSteps.size()");
    cb.addLine("}").addLB();

    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, input.getVarIds(), new String[input.length()],
                                          cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, outputListRefs,
                                          new String[outputListRefs.length], cb, importedClassesFqStrings);
    generatedGetRefIdMethod(cb);

    //stepklasse
    cb.addLine("}").addLB();
  }

  protected void appendPrepareChildSteps(CodeBuffer cb) throws XPRC_InvalidVariableIdException, XPRC_InvalidServiceIdException, XPRC_OperationUnknownException {
    cb.addLine("private void prepareChildSteps() throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine("if (! needsReinit) {");{
      //Reinitialisierung der ParentSteps, da transient
      cb.addLine("for (",childScope.getClassName(), " childStep : childSteps) {");{
        cb.addLine("childStep.", METHODNAME_SET_PARENT_STEP, "(this)");
      }cb.addLine("}");
      cb.addLine("return");
    }cb.addLine("}");
    cb.addLine("needsReinit = false");
    
    //listen inputs
    for (int i = 0; i < input.length(); i++) {
      String inputListRef = input.getVarIds()[i];
      VariableIdentification inputListVarIdent = getParentScope().identifyVariable(inputListRef);
      cb.addLine("if (listIn_", i + " == null) {");{
        cb.addLine("listIn_" + i + " = ", inputListVarIdent.getScopeGetter(getParentScope()), 
                   inputListVarIdent.variable.getVarName());
      }cb.addLine("}");
    }

    // init forschleife
    cb.addLine("if (listIn_0 != null) {");{
      cb.addLine("for (int i = 0; i<listIn_0.size(); i++) {");{
        cb.add(childScope.getClassName(), " childStep = new ", childScope.getClassName(), "(i)").addLB();
        cb.addLine("childStep.", METHODNAME_INIT, "(", METHODNAME_GET_PARENT_SCOPE, "())");
        cb.addLine("childStep.initializeMemberVars()");
        cb.addLine("childSteps.add(childStep)");
        cb.addLine("childStep.", METHODNAME_SET_PARENT_STEP, "(this)");
        if (input.length() == 1) {
          cb.addLine("childStep.", ScopeStep.METHODNAME_SET_INPUT_VARS, "(listIn_0.get(i))"); 
        } else { //>1
          cb.add("childStep.", ScopeStep.METHODNAME_SET_INPUT_VARS, "(new ", Container.class.getSimpleName(), "(");
          for (int inputListRefsIndex = 0; inputListRefsIndex < input.length(); inputListRefsIndex++) {
            cb.addListElement("listIn_" + inputListRefsIndex + ".get(i)");
          }
          cb.add("))").addLB();
        }
      }cb.addLine("}"); // end for
    }cb.addLine("}"); // end if
    cb.addLine("}").addLB();
  }
 
  protected void appendGetFractalWorkflowParallelExecutor(CodeBuffer cb) {
    cb.addLine("public ",FractalWorkflowParallelExecutor.class.getSimpleName()," ", METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR, "() {");
    cb.addLine("if (", VARNAME_fractalWorkflowParallelExecutor, " == null) {");{
      String peId = "F"+getIdx();
      cb.add(VARNAME_fractalWorkflowParallelExecutor, " = new ",
             FractalWorkflowParallelExecutor.class.getSimpleName(),"(\"",peId,"\",childSteps)").addLB();
    }cb.addLine("}");
    cb.addLine("return ", VARNAME_fractalWorkflowParallelExecutor);
    cb.addLine("}").addLB();
  }

  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException, XPRC_InvalidServiceIdException, XPRC_OperationUnknownException {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    //Ausf�hrung der childSteps
    if (parallelExecution) {
      cb.addLine(ParallelismLimitation.class.getSimpleName()," limitation = ", limitType.construct(limit) );
      cb.addLine("prepareChildSteps()");
      cb.addLine(METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR, "().", METHODNAME_PARALLEL_EXECUTOR_INIT, "(getProcess(), limitation).", METHODNAME_PARALLEL_EXECUTOR_EXECUTE, "()");
    } else {
      cb.addLine("prepareChildSteps()");
      cb.addLine("for (int i = 0; i<childSteps.size(); i++) {");
      cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(i)");
      cb.addLine("}");
    }
    appendGatherOutput(cb,importedClassesFqStrings); //Sammeln und Zuweisen des Outputs
    cb.addLine("}").addLB();
  }
  
  protected void appendCompensateInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException, XPRC_InvalidServiceIdException, XPRC_OperationUnknownException {
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getName(), "{");
    if (parallelExecution) {
      cb.addLine(ParallelismLimitation.class.getSimpleName()," limitation = ", limitType.construct(limit) );
      cb.addLine("prepareChildSteps()");
      cb.addLine(METHODNAME_GET_FRACTAL_WF_PARALLEL_EXECUTOR, "().", METHODNAME_PARALLEL_EXECUTOR_INIT, "(getProcess(), limitation).", METHODNAME_PARALLEL_EXECUTOR_COMPENSATE, "()");
    } else {
      //default mechanismus kompensiert alle kindschritte automatisch in der richtigen reihenfolge
    }
    cb.addLine("}").addLB();
  }
  
  private void appendGatherOutput(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    //Sammeln und Zuweisen des Outputs
    if (outputListRefs.length == 0) {
      //kein Output
    } else {
      boolean isContainer = outputListRefs.length > 1;
      for (int outputIndex = 0; outputIndex < outputListRefs.length; outputIndex++) {
        appendCreateOutputList(cb, outputIndex, importedClassesFqStrings);
      }
      cb.addLine("if (listIn_0 != null) {");
      cb.addLine("for (int i = 0; i<listIn_0.size(); i++) {");{
        if( isContainer ) {
          cb.addLine(Container.class.getName(), " c = childSteps.get(i).", ScopeStep.METHODNAME_GET_OUTPUT, "()");
        }
        for (int outputIndex = 0; outputIndex < outputListRefs.length; outputIndex++) {
          appendFillOutputList(cb, outputIndex, importedClassesFqStrings, isContainer);
        }
      }
      cb.addLine("}").addLine("}");
      for (int outputIndex = 0; outputIndex < outputListRefs.length; outputIndex++) {
        String outputListRef = outputListRefs[outputIndex];
        VariableIdentification outputListVarIdent = getParentScope().identifyVariable(outputListRef);
        cb.addLine(outputListVarIdent.getScopeGetter(getParentScope()) + outputListVarIdent.variable.getVarName()
                        + " = listOut_" + outputIndex);
      }
    }

  }

  /**
   * @return
   * @throws XPRC_InvalidServiceIdException 
   * @throws XPRC_OperationUnknownException 
   */
  private String getLimitForLimitTypeCapacity() throws XPRC_InvalidServiceIdException, XPRC_OperationUnknownException {
    //limit auf ordertype des function-calls setzen
    List<Step> childSteps = childScope.getChildSteps();
    if (childSteps.size() != 1) {
      throw new RuntimeException("Foreach supports one childstep only.");
    }
    if (childSteps.get(0) instanceof StepSerial) {
      StepSerial stepSerial = (StepSerial) childSteps.get(0);
      Step firstChildOfSerial = stepSerial.getChildSteps().get(0);
      StepFunction sf;
      if (firstChildOfSerial instanceof StepCatch) {
        sf = (StepFunction) ((StepCatch) firstChildOfSerial).getStepInTryBlock();
      } else if (firstChildOfSerial instanceof StepFunction) {
        sf = (StepFunction) firstChildOfSerial;
      } else if(firstChildOfSerial instanceof StepForeach) {
        return ((StepForeach)firstChildOfSerial).getLimitForLimitTypeCapacity();
      } else {
        throw new RuntimeException("unsupported steptype: " + firstChildOfSerial.getClass().getSimpleName());
      }
      Service s = getParentScope().identifyService(sf.getServiceId()).service;
      WF wf = s.getWF();
      
      if (wf == null) {
        //evtl wf-reference in servicegroup?
        DOM dom = s.getDom();
        if (dom == null) {
          throw new RuntimeException("Service contains neither DOM nor WF");
        }
        Operation operation = dom.getOperationByName(sf.getOperationName());
        if (operation instanceof WorkflowCallServiceReference) {
          WorkflowCall wfCall = (WorkflowCall) operation;
          wf = wfCall.getWf();
        }
      }

      if (wf != null) {
        return wf.getFqClassName();
      }
    } else {
      throw new RuntimeException("Stepserial expected as childstep of scope. got "
          + childSteps.get(0).getClass().getSimpleName());
    }
    return null;
  }

  private void appendCreateOutputList(CodeBuffer cb, int index, Set<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    String outputListIndex = String.valueOf(index);
    String outputListRef = outputListRefs[index];
    VariableIdentification outputListVarIdent = getParentScope().identifyVariable(outputListRef);
    cb.addLine("if (listOut_", outputListIndex, " == null) {");
    {
      cb.addLine("listOut_", outputListIndex, " = new ArrayList<",
                 outputListVarIdent.variable.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings), ">()");
    }
    cb.addLine("}");
  }


  private void appendFillOutputList(CodeBuffer cb, int index, HashSet<String> importedClassesFqStrings, boolean isContainer) throws XPRC_InvalidVariableIdException {
    AVariable v = outputVarsSingle[index];
    StringBuilder sb = new StringBuilder();
    sb.append("listOut_").append(index).append("."); //Listnamen
    sb.append( v.isList() ? "addAll" : "add").append("("); //Add-Methode
    sb.append("(").append(v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings)).append(")"); //Cast
    if( isContainer ) {
      sb.append("c.get(").append(index).append(")"); //Zugriff auf Container
    } else {
      sb.append("childSteps.get(i).").append(ScopeStep.METHODNAME_GET_OUTPUT).append("()"); //Zugriff auf Output direkt
    }
    sb.append(")");
    cb.addLine(sb.toString());
  }

  @Override
  protected boolean compareImplementation(Step oldStep) {    
    if (oldStep == null || !(oldStep instanceof StepForeach)) {
      return true;
    }
    StepForeach oldForeachStep = (StepForeach)oldStep;
  
   if (parallelExecution != oldForeachStep.parallelExecution) {
     return true;
   }
   
   if (!Arrays.equals(input.getVarIds(), oldForeachStep.input.getVarIds()) ||
       !Arrays.equals(outputListRefs, oldForeachStep.outputListRefs)) {
     return true;
   }
      
   if (childScope != null) {
      childScope.compareImplementation(oldForeachStep.childScope);
   } else if (oldForeachStep.childScope != null) {
      return true;
    }
    
    return false;
  }

  protected Set<String> getAllUsedVariableIds() {
    if(outputListRefs == null)
      return new HashSet<String>();
    return createVariableIdSet(input.getVarIds(), outputListRefs);
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException, XPRC_PrototypeDeployment,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE,
      XPRC_JAVATYPE_UNSUPPORTED, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED {
    //validierung, mindestens ein input.
    if (input.length() == 0) {
      throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.ATT.FOREACH_PARALLEL, GenerationBase.EL.INPUT_LIST);
    }
    if (inputVarsSingle.length != input.length()) {
      //TODO fehlermeldung: oder exception
      throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.INPUT_LIST, GenerationBase.EL.DATA);
    }
    if (outputVarsSingle.length != outputListRefs.length) {
      throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.OUTPUT_LIST, GenerationBase.EL.DATA);
    }
    if (LIMIT_UNKNOWN.equals(limit)) {
      try {
        limit = getLimitForLimitTypeCapacity();
      } catch (Exception ex) { //XPRC_InvalidServiceIdException, XPRC_OperationUnknownException
        throw new RuntimeException("Could not determine ordertype for LimitType " + limitType, ex);
      }
      //ansonsten ok, dann wurde limit halt nun korrekt ermittelt
    }
    
    for (AVariable v : inputVarsSingle) {
      v.validate();
    }
    for (AVariable v : outputVarsSingle) {
      v.validate();
    }
  }

  public ScopeStep getChildScope() {
    return childScope;
  }
  
  public boolean getParallelExecution() {
    return parallelExecution;
  }
  
  public AVariable[] getInputVarsSingle() {
    return inputVarsSingle;
  }
  
  public AVariable[] getOutputVarsSingle() {
    return outputVarsSingle;
  }
  
  public void setOutputVarsSingle(AVariable[] vars) {
    outputVarsSingle = vars;
  }
  
  public String[] getInputListRefs() {
    return input.getVarIds();
  }
  
  public String[] getOutputListRefs() {
    return outputListRefs;
  }
  
  public void setOutputListRefs(String[] refs) {
    outputListRefs = refs;
  }
  
  @Override
  public String[] getInputVarIds() {
    return getInputListRefs();
  }

  @Override
  public String[] getOutputVarIds() {
    return getOutputListRefs();
  }
  
  @Override
  public List<AVariable> getInputVars() {
    return childScope.getInputVars();
  }

  //returns List Variants of Variables defined inside ForEach
  //does not contain single version of variable we iterate over
  @Override
  public List<AVariable> getOutputVars() {
    List<AVariable> result = new ArrayList<AVariable>();
    
    if(outputListRefs == null)
      return result;
    
    for(int i=0; i< outputListRefs.length; i++) {
      try {
        //usually variable is in childScope
        result.add(getChildScope().identifyVariable(outputListRefs[i]).getVariable());
      } catch (XPRC_InvalidVariableIdException e) {
        boolean found = false;
        
        //sometimes variable is in globalStepSerial
        List<ServiceVariable> sv = getGlobalStepSerial().getServiceVariables();
        //it might be in our parentStepSerial, too
        if(getParentStep() instanceof StepSerial) {
          sv.addAll(((StepSerial)getParentStep()).getServiceVariables());
        }
        
        for(ServiceVariable s : sv) {
          if(s.getId() != null && s.getId().equals(outputListRefs[i])) {
            result.add(s);
            found = true;
            break;
          }
          
        }
        
        

        if(!found) {
          //sometimes variable is in a parent StepForeach output
          StepForeach parent = getParentStepForeachOrNull(this);
          while(parent != null) {
            for(int j=0; j< parent.getOutputVarsSingle().length; j++) {
              AVariable candidate  = parent.getOutputVarsSingle()[j];
              if(candidate != null && candidate.getId().equals(outputListRefs[i])) {
                result.add(candidate);
                found = true;
                break;
              }
            }
            if(found)
              break;

            parent = getParentStepForeachOrNull(parent);
          }
          
        }

        
        if(found)
          continue;
        
        throw new RuntimeException("Could not identify variable: " + outputListRefs[i]);
      }
    }
    
    return result;
  }
  
  public InputConnections getInputConnections() {
    return input;
  }
  
  public void createEmpty() {
    setXmlId(creator.getNextXmlId());
    
    childScope = new ForEachScopeStep(getParentScope(), inputVarsSingle, outputVarsSingle, creator);
    StepSerial childStep = new StepSerial(childScope, creator);
    childStep.setXmlId(-getXmlId()); // create unique id (different from id of StepForeach) for zeta-gui
    childScope.setChildStep(childStep);
    
    outputListRefs = new String[0];
    outputVarsSingle = new AVariable[0];
  }
  
  public void setParallelExecution(LimitType type, String limit) {
    this.parallelExecution = true;
    this.limit = limit;
    this.limitType = type;
  }
  
  public void setSequentialExecution() {
    this.parallelExecution = false;
  }
  
  /*
   *  inputs sind die listenwertigen variablen weiter oben im workflow, �ber die synchron iteriert wird, d.h. ihre kardinalit�ten m�ssen identisch sein.
   */
  public void addInput(AVariable inputVar) {
    if (inputVarsSingle == null) {
      inputVarsSingle = new AVariable[1];
      input = new InputConnections(1);
    } else {
      AVariable[] newInputVarsSingle = new AVariable[inputVarsSingle.length + 1];
      System.arraycopy(inputVarsSingle, 0, newInputVarsSingle, 0, inputVarsSingle.length);
      inputVarsSingle = newInputVarsSingle;
      
      InputConnections newIC = new InputConnections(inputVarsSingle.length);
      System.arraycopy(input.getVarIds(), 0, newIC.getVarIds(), 0, input.getVarIds().length);
      input = newIC;
    }

    input.getVarIds()[inputVarsSingle.length - 1] = inputVar.getId(); //reference to list version of variable
    inputVarsSingle[inputVarsSingle.length - 1] = inputVar;
    
    //inputVar for scope is single -> loop variable
    ServiceVariable single = createVariable(inputVar, false);
    inputVarsSingle[inputVarsSingle.length - 1] = single;
    childScope.addInput(single);
  }
  
  //removes single from SingleOutputVars
  //also removes matching listRef from outputListRefs
  public void removeOutputVar(AVariable singleVar) {
    if (outputVarsSingle == null)
      throw new RuntimeException("Output not found.");

    int index = -1;
    for (int i = 0; i < outputVarsSingle.length; i++) {
      if (outputVarsSingle[i].getId().equals(singleVar.getId())) {
        index = i;
        break;
      }
    }

    if (index == -1) {
      throw new RuntimeException("Output not found.");
    }

    //-1 (in X.length - index - 1) is the removed Variable

    //remove and shorten outputVarsSingle (remove entry at index)
    AVariable[] newOutputVarsSingle = new AVariable[outputVarsSingle.length - 1];
    System.arraycopy(outputVarsSingle, 0, newOutputVarsSingle, 0, index);
    if (outputVarsSingle.length > index + 1)
      System.arraycopy(outputVarsSingle, index + 1, newOutputVarsSingle, index, outputVarsSingle.length - index - 1);
    outputVarsSingle = newOutputVarsSingle;

    //remove and shorten outputListRefs (remove entry at index)
    String[] newOutputListRefs = new String[outputListRefs.length - 1];
    System.arraycopy(outputListRefs, 0, newOutputListRefs, 0, index);
    if (outputListRefs.length > index + 1)
      System.arraycopy(outputListRefs, index + 1, newOutputListRefs, index, outputListRefs.length - index - 1);
    outputListRefs = newOutputListRefs;
  }
  
  
  //inputs sind die (potentiell) singlewertigen variablen aus dem Schritt im Foreach
  //in outputVarsSingle schreiben wir aber die vom OutputStep getrennten Variablen.
  public void addOutputVar(AVariable singleVar) {
    if (outputVarsSingle == null) {
      outputVarsSingle = new AVariable[1];
      outputListRefs = new String[1];
    } else {
      AVariable[] newOutputVarsSingle = new AVariable[outputVarsSingle.length + 1];
      System.arraycopy(outputVarsSingle, 0, newOutputVarsSingle, 0, outputVarsSingle.length);
      outputVarsSingle = newOutputVarsSingle;
      
      String[] newOutputListRefs = new String[outputListRefs.length + 1];
      System.arraycopy(outputListRefs, 0, newOutputListRefs, 0, outputListRefs.length);
      outputListRefs = newOutputListRefs;
    }
    
    String idToFind = null;
    Step stepProvidingOutput= getChildScope().getChildStep().getChildSteps().get(0);
    List<AVariable> childOutputs = stepProvidingOutput.getOutputVars();
    for(int i=0; i<childOutputs.size(); i++) {
      if(childOutputs.get(i).getId() != null && childOutputs.get(i).getId().equals(singleVar.getId())) {
        idToFind = getChildScope().getChildStep().getChildSteps().get(0).getOutputVarIds()[i];
        break;
      }
    }
    
    if(idToFind == null)
      throw new RuntimeException("variable not found");
    
    //find correct variable to add
    //variable can be in one of five places:
    //serviceVariable in childStep - add output to child (ForEach already exists)
    //serviceVariable in stepSerial - create ForEach with child already having output
    //serviceVariable in outer stepSerial - if there is a ForEach around us already
    //output of inner ForEach - child of this ForEach is another ForEach
    //output of outer ForEach - this is an inner ForEach and the outer ForEach holds the variable
    AVariable varToAdd = null;
    AVariable orgVarToAdd = null;
    StepSerial childStepSerial = ((StepSerial)childScope.getChildStep());
    List<AVariable> candidates = new ArrayList<AVariable>();
    StepForeach outerForeach = getParentStepForeachOrNull(this);
    List<AVariable> innerForeachOutputs = findInnerForeachOutputVariables(this);
    List<AVariable> outerForeachOutputs = outerForeach != null ?
        outerForeach.getOutputVarsSingle(false) :
        new ArrayList<AVariable>();
    List<AVariable> parentSerialServiceVars = new ArrayList<AVariable>();
    Set<AVariable> childScopeVars = getChildScope().getPrivateVars();
    List<ServiceVariable> childrenOfParentScope = getParentScope().getChildStep().getServiceVariables();
    List<ServiceVariable> varsOfParentScope = this.getParentScope().getServiceVariables();
    candidates.addAll(childStepSerial.getServiceVariables());
    candidates.addAll(getGlobalStepSerial().getServiceVariables());
    candidates.addAll(innerForeachOutputs);
    candidates.addAll(outerForeachOutputs);
    candidates.addAll(childScopeVars);
    candidates.addAll(varsOfParentScope);
    
    if(getParentStep() instanceof StepSerial) {
      parentSerialServiceVars.addAll(((StepSerial)getParentStep()).getServiceVariables());
    }
    
    candidates.addAll(childrenOfParentScope);
    
    for(AVariable candidate : candidates) {
      if(candidate.getId().equals(idToFind)) {
        varToAdd = candidate;
        orgVarToAdd = candidate;
        break;
      }
    }
    
    if(varToAdd == null) {
      varToAdd = singleVar;
    }
    
    outputVarsSingle[outputVarsSingle.length - 1] = varToAdd;
    ServiceVariable newOutput = createVariable(singleVar, true);
    newOutput.getSourceIds().add(getStepId());
    getGlobalStepSerial().addVar(newOutput);
    outputListRefs[outputListRefs.length - 1] = newOutput.getId();
    
    
    //remove varToAdd from childScope().getChildStep()
    if(childStepSerial.getServiceVariables().contains(varToAdd))
      childStepSerial.removeVariable(varToAdd);
    
    //remove Variable from global scope -> otherwise it gets added to
    //ForEach scope - but we already have it in outputList
    if(getGlobalStepSerial().getServiceVariables().contains(varToAdd)) {
      getGlobalStepSerial().removeVar(varToAdd);
    }
    
    //remove variable from outer ForEach output -> it is no longer available there
    if(outerForeachOutputs.contains(orgVarToAdd)) {
      StepForeach sfe = findStepForeachWithOutput(orgVarToAdd);
        sfe.removeOutputVar(orgVarToAdd);
    }
    
    //remove variable from childScope -> happens for StepChoice
    if(childScopeVars.contains(varToAdd)) {
      getChildScope().removeVariable(varToAdd);
    }
    
    //remove from parentStepSerial
    if(parentSerialServiceVars.contains(varToAdd) && 
      ((StepSerial)getParentStep()).getServiceVariables().contains(varToAdd)) 
    {
      ((StepSerial)getParentStep()).removeVariable(varToAdd);
    }
    
    //remove from child of parentScope
    //var might have been removed already
    if(getParentScope().getChildStep().getServiceVariables().contains(varToAdd)) {
      getParentScope().getChildStep().removeVariable(varToAdd);
    }

    //remove from parent Scope
    if (getParentScope().getServiceVariables().contains(varToAdd)) {
      getParentScope().removeVariable(varToAdd);
    }

  }
  
  
  private List<AVariable> findInnerForeachOutputVariables(StepForeach current){
    StepForeach innerStepForeach = getChildStepForeachOrNull(current);
    
    if(innerStepForeach == null)
      return new ArrayList<AVariable>();
    
    List<AVariable> result = new ArrayList<AVariable>(innerStepForeach.getOutputVars());
    List<AVariable> furtherDownOutputs = findInnerForeachOutputVariables(innerStepForeach);
    result.addAll(furtherDownOutputs);
    
    return result;
  }
  
  private StepForeach getChildStepForeachOrNull(StepForeach sfe) {
    if( sfe.getChildScope() == null || 
        sfe.getChildScope().getChildStep() == null || 
        sfe.getChildScope().getChildStep().getChildSteps() == null ||
        sfe.getChildScope().getChildStep().getChildSteps().size() == 0 ||
        !(sfe.getChildScope().getChildStep().getChildSteps().get(0) instanceof StepForeach))
      return null;
    
    return (StepForeach)sfe.getChildScope().getChildStep().getChildSteps().get(0);
  }
  
  
  public static StepForeach getParentStepForeachOrNull(Step sfe) {
    if( sfe.getParentStep() == null ||
        !(sfe.getParentStep() instanceof StepSerial) ||
        sfe.getParentStep().getParentStep() == null ||
        !(sfe.getParentStep().getParentStep() instanceof ForEachScopeStep) ||
        sfe.getParentStep().getParentStep().getParentStep() == null ||
        !(sfe.getParentStep().getParentStep().getParentStep() instanceof StepForeach)){
      return null;
    }
    return (StepForeach)sfe.getParentStep().getParentStep().getParentStep();
  }
  
  public List<AVariable> getOutputVarsSingle(boolean recursive){
    List<AVariable> result = new ArrayList<AVariable>();
    
    for(int i=0; i<outputVarsSingle.length; i++) {
      result.add(outputVarsSingle[i]);
    }
    
    if(recursive) {
      StepForeach parent = getParentStepForeachOrNull(this);
      if(parent != null)
        result.addAll(parent.getOutputVarsSingle(recursive));
    }
    
    return result;
  }
  
  public List<AVariable> findOuterForeachOutputVariables(StepForeach current){
    StepForeach parentStep = getParentStepForeachOrNull(current);
    if(parentStep == null)
      return new ArrayList<AVariable>();
    
    List<AVariable> result = new ArrayList<AVariable>(parentStep.getOutputVars());
    List<AVariable> furtherOutOutputs = findOuterForeachOutputVariables(parentStep);
    result.addAll(furtherOutOutputs);
    
    return result;
  }
  
  private StepForeach findStepForeachWithOutput(AVariable var) {
    StepForeach currentStepForeach = getParentStepForeachOrNull(this);
    //search parentSteps
    while(currentStepForeach != null) {
      for(int i=0; i<currentStepForeach.getOutputVarsSingle().length; i++) {
        if(currentStepForeach.getOutputVarsSingle()[i] == var)
          return currentStepForeach;
      }
      currentStepForeach = getParentStepForeachOrNull(currentStepForeach);
    }
    
    currentStepForeach = getChildStepForeachOrNull(this);
    //search childSteps
    while(currentStepForeach != null) {
      for(int i=0; i<currentStepForeach.getOutputVarsSingle().length; i++) {
        if(currentStepForeach.getOutputVarsSingle()[i] == var)
          return currentStepForeach;
      }
      currentStepForeach = getChildStepForeachOrNull(currentStepForeach);
    }
    
    return null;
  }
  
  private StepSerial getGlobalStepSerial() {
    StepSerial result = null;
    Step parent = getParentStep();
    
    while(parent != null) {
      if(parent instanceof StepSerial)
        result = (StepSerial)parent;
      
      parent = parent.getParentStep();
    }
    
    return result;
  }
  
  
  //creates list/single version of single/list variable
  private ServiceVariable createVariable(AVariable inputVar, boolean isList) {
    ServiceVariable result = new ServiceVariable(creator);
    result.setIsList(false);
    result.setFQClassName(inputVar.getFQClassName());
    result.setLabel(inputVar.getLabel());
    result.setIsList(isList); //unlike inputVar
    result.setFQClassName(inputVar.getFQClassName());
    result.setOriginalClassName(inputVar.getOriginalName());
    result.setOriginalPath(inputVar.getOriginalPath());
    result.setId(creator.getNextXmlId().toString());
    result.setClassName(inputVar.getClassNameDirectly());
    result.setGenerationBaseObject(inputVar.getDomOrExceptionObject());
    
    return result;
  }
  
  public void removeInput(AVariable singleVar) {
    if(inputVarsSingle == null)
      throw new RuntimeException("Input not found.");
    int index = -1;
    for(int i= 0; i<inputVarsSingle.length; i++) {
      if(inputVarsSingle[i] == singleVar) {
        index = i;
        break;
      }
    }
        
    if (index == -1) {
      throw new RuntimeException("Input not found.");
    }
    
    //remove and shorten inputVarsSingle (remove entry at index)
    AVariable[] newInputVarsSingle = new AVariable[inputVarsSingle.length - 1];
    System.arraycopy(inputVarsSingle, 0, newInputVarsSingle, 0, index);
    if (inputVarsSingle.length > index + 1) {
      System.arraycopy(inputVarsSingle, index + 1, newInputVarsSingle, index, inputVarsSingle.length - index - 1);
    }
    inputVarsSingle = newInputVarsSingle;
    
    //remove and shorten input (remove entry at index)
    InputConnections newInputListRefs = new InputConnections(inputVarsSingle.length); // = getInputListRefs().size - 1
    
    for (int i = 0; i < index; i++) {
      newInputListRefs.getVarIds()[i] = getInputListRefs()[i];
    }
    for (int i = index + 1; i < newInputListRefs.length() + 1; i++) {
      newInputListRefs.getVarIds()[i - 1] = getInputListRefs()[i];
    }
    
    input = newInputListRefs;
    
    List<AVariable> ovars = getChildScope().getOutputVars();
    List<AVariable> ivars = new ArrayList<AVariable>(getChildScope().getInputVars());
    
    ivars.remove(singleVar);
    getChildScope().refreshVars(ivars, ovars);
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.FOREACH); {
      boolean parallelExecution = getParallelExecution();
      if (parallelExecution) {
        xml.addAttribute(ATT.FOREACH_PARALLEL, Boolean.toString(parallelExecution));
        if (limitType != null) {
          xml.addAttribute(ATT.FOREACH_LIMITTYPE, XMLUtils.escapeXMLValue(limitType.toString()));
          xml.addAttribute(ATT.FOREACH_LIMIT, XMLUtils.escapeXMLValue(limit)); // TODO
        }
      }
      
      Integer xmlId = getXmlId();
      if (xmlId != null) {
        xml.addAttribute(ATT.ID, xmlId.toString());
      }
      xml.endAttributes();
      
      // <Meta>
      if (hasUnknownMetaTags()) {
        xml.startElement(EL.META); {
          appendUnknownMetaTags(xml);
        } xml.endElement(EL.META);
      }
      
      // <Source>
      for (int inputNo = 0; inputNo < input.length(); inputNo++) {
        appendSource(xml, input.getVarIds()[inputNo], input.getUserConnected()[inputNo], input.getConstantConnected()[inputNo], false, input.getUnknownMetaTags().get(inputNo));
      }
      
      // <Target>
      for (String id : outputListRefs) {
        appendTarget(xml, id, false);
      }
      
      // <InputList>
      for (int inputNo = 0; inputNo < inputVarsSingle.length; inputNo++) {
        AVariable var = inputVarsSingle[inputNo];
        xml.startElementWithAttributes(EL.INPUT_LIST); {
          xml.addAttribute(ATT.REFID, input.getVarIds()[inputNo]);
          xml.endAttributes();
          
          var.appendXML(xml);
        } xml.endElement(EL.INPUT_LIST);
      }
      
      // <OutputList>
      for (int outputNo = 0; outputNo < outputVarsSingle.length; outputNo++) {
        AVariable var = outputVarsSingle[outputNo];
        xml.startElementWithAttributes(EL.OUTPUT_LIST); {
          xml.addAttribute(ATT.REFID, outputListRefs[outputNo]);
          xml.endAttributes();
          
          var.appendXML(xml);
        } xml.endElement(EL.OUTPUT_LIST);
      }
      
      childScope.getChildStep().appendXML(xml);
    } xml.endElement(EL.FOREACH);
  }
}

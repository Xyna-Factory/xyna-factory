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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
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
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep.FractalProcessStepFilter;
import com.gip.xyna.xprc.xfractwfe.base.StartVariableContextStep;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.CastExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.DynamicResultTypExpression;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo.ModelledType;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.EmptyVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.MapTree;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.StepBasedType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class StepMapping extends Step implements FormulaContainer, HasDocumentation {

  private static final String DOCUMENT_PART_FQN        = "xact.templates.DocumentPart";
  private static final String DOCUMENT_PART_LABEL      = "Document part";
  private static final String TEMPLATE_INITIAL_FORMULA = "%0%.text=concat(\"\")";

  private List<ModelledExpression> parsedExpressions = new ArrayList<ModelledExpression>();
  private List<String> rawExpressions = new ArrayList<String>();
  private String[] outputIds;
  private String[] outputPaths;
  private InputConnections input;
  private String[] localIds;
  private String[] localPaths;
  private Step compensateStep;
  private Step catchStep;
  private final List<ServiceVariable> variables = new ArrayList<ServiceVariable>();
  private final Map<String, AVariable> refIdToVariable = new HashMap<String, AVariable>();
  private final List<ExceptionVariable> eVars = new ArrayList<ExceptionVariable>();
  private List<AVariable> allVariablesInOrder = new ArrayList<AVariable>();
  private boolean isTemplateMapping = false;
  private boolean isConditionMapping = false;
  private String label;
  private List<AVariable> inputVars = new ArrayList<AVariable>();
  private List<AVariable> outputVars = new ArrayList<AVariable>();
  private String documentation = "";
  
  public StepMapping(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepMapping( this );
  }

  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepMapping)) {
      return true;
    }
    
    StepMapping oldStepMapping = (StepMapping)oldStep;
    if (!Arrays.equals(outputIds, oldStepMapping.outputIds)) {
      return true;
    }
    if (!Arrays.equals(outputPaths, oldStepMapping.outputPaths)) {
      return true;
    }
    if (!Arrays.equals(input.getVarIds(), oldStepMapping.input.getVarIds())) {
      return true;
    }
    if (!Arrays.equals(input.getPaths(), oldStepMapping.input.getPaths())) {
      return true;
    }
    if (!Arrays.equals(localIds, oldStepMapping.localIds)) {
      return true;
    }
    if (!Arrays.equals(localPaths, oldStepMapping.localPaths)) {
      return true;
    }
    
    if (compensateStep == null && oldStepMapping.compensateStep != null) {
      return true;
    }
    
    if (compensateStep != null && oldStepMapping.compensateStep == null) {
      return true;
    }
    
    if (catchStep == null && oldStepMapping.catchStep != null) {
      return true;
    }
    
    if (catchStep != null && oldStepMapping.catchStep == null) {
      return true;
    }
    
    if (isTemplateMapping != oldStepMapping.isTemplateMapping) {
      return true;
    }

    if (isConditionMapping != oldStepMapping.isConditionMapping) {
      return true;
    }

    if( ! parsedExpressions.equals(oldStepMapping.parsedExpressions) ) { // verwendet ModelledExpression.equals
      return true;
    }
    
    return false;
  }


  public boolean isTemplateMapping() {
    return isTemplateMapping;
  }


  public boolean isConditionMapping() {
    return isConditionMapping;
  }


  public void setIsConditionMapping(boolean isConditionalMapping) {
    this.isConditionMapping = isConditionalMapping;
  }


  @Override
  public List<Step> getChildSteps() {
    return null;
  }


  @Override
  protected List<ServiceVariable> getServiceVariables() {
    ArrayList<ServiceVariable> allVars = new ArrayList<ServiceVariable>(variables);
    return allVars;
  }


  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    ArrayList<ExceptionVariable> allVars = new ArrayList<ExceptionVariable>(eVars);
    return allVars;
  }

  
  @Override
  protected void removeVariable(AVariable var) {
    if (var instanceof ExceptionVariable) {
      if (!eVars.remove(var)) {
        throw new RuntimeException("didn't find var " + var + " in step " + this);
      }
    } else if (var instanceof ServiceVariable) {
      if (!variables.remove(var)) {
        throw new RuntimeException("didn't find var " + var + " in step " + this);
      }
    } else {
      throw new RuntimeException("unsupported var type " + var);
    }
  }


  @Override
  protected List<Service> getServices() {
    return null;
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    List<GenerationBase> allVars = new ArrayList<GenerationBase>();
    for (ServiceVariable s : variables) {
      allVars.addAll(s.getDependencies());
    }
    for (ExceptionVariable s : eVars) {
      allVars.addAll(s.getDependencies());
    }
    DynamicDependencyFinder ddf = new DynamicDependencyFinder();
    for (ModelledExpression expr : parsedExpressions) {
      if (expr != null) {
        expr.visitSourceExpression(ddf);
        expr.visitTargetExpression(ddf);
      }
    }
    try {
      for (Entry<String, XMOMType> dependency : ddf.getAllDynamicTypes().entrySet()) {
        GenerationBase gb;
        switch (dependency.getValue()) {
          case DATATYPE :
            gb = creator.getCachedDOMInstanceOrCreate(dependency.getKey(), creator.revision);
            break;
          case WORKFLOW :
            gb = creator.getCachedWFInstanceOrCreate(dependency.getKey(), creator.revision);
            break;
          case EXCEPTION :
            gb = creator.getCachedExceptionInstanceOrCreate(dependency.getKey(), creator.revision);
            break;
          default :
            throw new IllegalArgumentException("Invalid type for instantiation: " + dependency.getValue());
        }
        allVars.add(gb);
      }
    } catch (XPRC_InvalidPackageNameException e) {
      // TODO ntbd or rather rethrow?
    }
    return allVars;
  }
  
  

  private long calcSerialVersionUID() {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    return GenerationBase.calcSerialVersionUID(types);
  }

  
  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
      XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException {

    cb.addLine("/*  " + GenerationBase.escapeForCodeGenUsageInComment(label) + "  */");
    cb.addLine("private static class ", getClassName() + " extends " + FractalProcessStep.class.getSimpleName(), "<"
                   + getParentScope().getClassName(), "> {").addLB();

    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calcSerialVersionUID()), "L");

    cb.addLB().addLine("public ", getClassName(), "() {");
    cb.addLine("super(" + getIdx() + ")");
    cb.addLine("}").addLB();

    appendExecuteInternally(cb, importedClassesFqStrings);
    
    appendParameterValueGetters(cb, importedClassesFqStrings);

    // generate a mapping to the refId
    generatedGetRefIdMethod(cb);

    //compensation
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    if (isExecutionDetached()) {
      cb.addLine("// nothing to be done, execution is detached and cannot be compensated");
    } else {
      if (compensateStep != null) {
        cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(0)");
      }
    }
    cb.addLine("}").addLB();

    //getChildren
    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<" + getParentScope().getClassName()
        + ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    if (compensateStep != null) {
      cb.addLine("if (i == 0) {");
      cb.addLine("return new " + FractalProcessStep.class.getSimpleName() + "[]{", METHODNAME_GET_PARENT_SCOPE, "()."
          + compensateStep.getVarName() + "};");
      cb.addLine("}");
    }
    cb.addLine("return null").addLine("}").addLB();

    //getChildrenLength
    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return ", compensateStep == null ? "0" : "1");
    cb.addLine("}").addLB();
    cb.addLine("}").addLB();
  }
  
  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getName(), "{");

    cb.addLine("try {");
    
    MapTree mapTree = new MapTree();
    for( ModelledExpression expr : parsedExpressions ) {
      expr.initTypesOfParsedFormula(importedClassesFqStrings, mapTree);
      cb.addLB();
    }
    
    for( ModelledExpression expr : parsedExpressions ) {
      Long uniqueId = cnt.incrementAndGet();
      expr.writeAssignmentExpressionToCodeBuffer(cb, importedClassesFqStrings, uniqueId, mapTree, isTemplateMapping);
      cb.addLB(2);
      if (isTemplateMapping) {
        generateDocumentAddition(cb, uniqueId);
      }
    }
    
    cb.addLine("} catch (", Throwable.class.getName() ," e) {");
    cb.addLine("if (e instanceof ", RuntimeException.class.getName(), ") { throw (RuntimeException)e; }");
    cb.addLine("throw new ", XDEV_PARAMETER_NAME_NOT_FOUND.class.getName() ,"(\" - \" ,e)");
    cb.addLine("}");
    
    cb.addLine("}").addLB();
  }


  private void appendParameterValueGetters(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException {
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, input.getVarIds(), input.getPaths(), cb,
                                          importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, outputIds, outputPaths, cb,
                                          importedClassesFqStrings);
  }
  
  
  private void generateDocumentAddition(CodeBuffer cb, Long uniqueId) {
    cb.addLine(StartVariableContextStep.class.getSimpleName(), " startDocumentStep = ", METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK, "(",
               StartVariableContextStep.class.getSimpleName(), ".class, new " + FractalProcessStepFilter.class.getSimpleName(), 
               "<" + StartVariableContextStep.class.getSimpleName(), ">() {");
    cb.addLine("public boolean ", METHODNAME_STEP_FILTER_MATCHES, "(", StartVariableContextStep.class.getSimpleName(), " step) {");
    cb.addLine("return step.", METHODNAME_GET_CONTEXT_VARIABLE, "() != null;");
    cb.addLine("}");
    cb.addLine("});");
    cb.addLine("if (startDocumentStep == null) {");
    cb.addLine("throw new " + RuntimeException.class.getSimpleName() + "(\"No previous start encountered." +
      " Please ensure that the invocation of this template block is enclosed by \'Start Document\'- and \'End Document\'-Operations.\")");
    cb.addLine("}");
    cb.addLine(XynaObject.class.getSimpleName() + " document = startDocumentStep.", METHODNAME_GET_CONTEXT_VARIABLE, "()");
    cb.addLine("if (document == null) {");
    cb.addLine("throw new " + RuntimeException.class.getSimpleName() + "(\"No Document started!\")");
    cb.addLine("}");
    cb.addLine("((xact.templates.Document)document).addToBuffer(" + ModelledExpression.TEMP_VARIABLE_PREFIX + uniqueId + ")");
  }


  @Override
  protected void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException,
      XPRC_InvalidServiceIdException {
    for (ServiceVariable sv : variables) {
      sv.getImports(imports);
    }
    for (ExceptionVariable exVar : eVars) {
      imports.add(exVar.getFQClassName());
    }
    imports.add(ArrayList.class.getName());
    imports.add(FractalProcessStepFilter.class.getCanonicalName());
    if (isTemplateMapping) {
      imports.add(StartVariableContextStep.class.getName());
    }
  }


  private static AtomicLong cnt = new AtomicLong(0);

  @Override
  protected void parseXML(Element mappingsElement) throws XPRC_InvalidPackageNameException {
    
    label = mappingsElement.getAttribute(GenerationBase.ATT.LABEL);
    
    // Meta
    parseUnknownMetaTags(mappingsElement, Arrays.asList(EL.ISTEMPLATE, EL.ISCONDITION, EL.DOCUMENTATION));
    Element meta = XMLUtils.getChildElementByName(mappingsElement, GenerationBase.EL.META);
    if (meta != null) {
      Element isTemplate = XMLUtils.getChildElementByName(meta, GenerationBase.EL.ISTEMPLATE);
      if (isTemplate != null) {
        String isTemplateContent = XMLUtils.getTextContent(isTemplate);
        if (isTemplateContent != null && Boolean.valueOf(isTemplateContent)) {
          isTemplateMapping = true;
        }
      }

      Element isConditionElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.ISCONDITION);
      if (isConditionElement != null) {
        String isConditionContent = XMLUtils.getTextContent(isConditionElement);
        if (isConditionContent != null && Boolean.valueOf(isConditionContent)) {
          isConditionMapping = true;
        }
      }
      
      Element documentationElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        documentation = XMLUtils.getTextContent(documentationElement);
      }
    }
    
    // parse input/output variables
    List<Element> inputs = XMLUtils.getChildElementsByName(mappingsElement, GenerationBase.EL.MAPPINGSINPUT);
    input = new InputConnections(inputs.size());
    for (int i = 0; i < inputs.size(); i++) {
      AVariable v = null;
      Element dataElement = XMLUtils.getChildElementByName(inputs.get(i), GenerationBase.EL.DATA);
      if (dataElement == null) {
        Element exceptionElement = XMLUtils.getChildElementByName(inputs.get(i), GenerationBase.EL.EXCEPTION);
        if (exceptionElement == null) {
          continue;
        } else {
          ExceptionVariable ev = new ExceptionVariable(creator);
          ev.parseXML(exceptionElement);
          eVars.add(ev);
          v = ev;
        }
      } else {
        ServiceVariable sv = new ServiceVariable(creator);
        sv.parseXML(dataElement);
        variables.add(sv);
        v = sv;
      }

      Element sourceElement = XMLUtils.getChildElementByName(inputs.get(i), GenerationBase.EL.SOURCE);
      if (sourceElement != null) {
        input.parseSourceElement(sourceElement, i);
      } else {
        if (v.getId() == null || v.getId().length() == 0) {
          v.setId("mapping" + cnt.incrementAndGet());
        }
        input.getVarIds()[i] = v.getId();
        input.getPaths()[i] = "";
      }
      refIdToVariable.put(input.getVarIds()[i], v);
      allVariablesInOrder.add(v);
      inputVars.add(v);
    }
    
    
    List<Element> locals = XMLUtils.getChildElementsByName(mappingsElement, GenerationBase.EL.MAPPINGSLOCAL);
    localIds = new String[locals.size()];
    localPaths = new String[locals.size()];
    for (int i = 0; i < locals.size(); i++) {
      AVariable v = null;
      Element dataElement = XMLUtils.getChildElementByName(locals.get(i), GenerationBase.EL.DATA);
      if (dataElement == null) {
        Element exceptionElement = XMLUtils.getChildElementByName(locals.get(i), GenerationBase.EL.EXCEPTION);
        if (exceptionElement == null) {
          continue;
        } else {
          ExceptionVariable ev = new ExceptionVariable(creator);
          ev.parseXML(exceptionElement);
          eVars.add(ev);
          v = ev;
        }
      } else {
        ServiceVariable sv = new ServiceVariable(creator);
        sv.parseXML(dataElement);
        variables.add(sv);
        v = sv;
      }

      if (v.getId() == null || v.getId().length() == 0) {
        v.setId("mapping" + cnt.incrementAndGet());
      }
      localIds[i] = v.getId();
      localPaths[i] = "";
      refIdToVariable.put(localIds[i], v);
    }
    

    List<Element> outputs = XMLUtils.getChildElementsByName(mappingsElement, GenerationBase.EL.MAPPINGSOUTPUT);
    outputIds = new String[outputs.size()];
    outputPaths = new String[outputs.size()];
    for (int i = 0; i < outputs.size(); i++) {
      Element dataElement = XMLUtils.getChildElementByName(outputs.get(i), GenerationBase.EL.DATA);
      AVariable v;
      if (dataElement == null) {
        Element exceptionElement = XMLUtils.getChildElementByName(outputs.get(i), GenerationBase.EL.EXCEPTION);
        if (exceptionElement == null) {
          continue;
        } else {
          ExceptionVariable ev = new ExceptionVariable(creator);
          ev.parseXML(exceptionElement);
          eVars.add(ev);
          v = ev;
        }
      } else {
        ServiceVariable sv = new ServiceVariable(creator);
        sv.parseXML(dataElement);
        variables.add(sv);
        v = sv;
      }

      Element targetElement = XMLUtils.getChildElementByName(outputs.get(i), GenerationBase.EL.TARGET);
      if (targetElement != null) {
        outputIds[i] = targetElement.getAttribute(GenerationBase.ATT.REFID);
        outputPaths[i] = targetElement.getAttribute(GenerationBase.ATT.PATH);
      }
      refIdToVariable.put(outputIds[i], v);
      allVariablesInOrder.add(v);
      outputVars.add(v);
    }
    
    
    List<Element> mappingElements = XMLUtils.getChildElementsByName(mappingsElement, GenerationBase.EL.MAPPING);
    for( Element mel : mappingElements ) {
      String raw = XMLUtils.getTextContent(mel);
      ModelledExpression parsed = null;
      try {
        parsed = ModelledExpression.parse( this, raw );
      } catch (XPRC_ParsingModelledExpressionException e) {
        //validate checkt das dann nochmal
      }
      rawExpressions.add(raw);
      parsedExpressions.add(parsed);
    }

    List<Element> catchElements = XMLUtils.getChildElementsByName(mappingsElement, GenerationBase.EL.CATCH);
    if (catchElements.size() > 0) {
      catchStep = new StepCatch(getParentScope(), this, creator);
      catchStep.parseXML(mappingsElement);
    } else {
      // nur, wenn kein catch drum rum ist, weil ansonsten generiert der die auditdaten
      parseId(mappingsElement);
    }

    // compensate
    Element compensateEl = XMLUtils.getChildElementByName(mappingsElement, GenerationBase.EL.COMPENSATE);
    if (compensateEl != null) {
      StepSerial serial = new StepSerial(getParentScope(), creator);
      serial.parseXML(compensateEl);
      compensateStep = serial.getProxyForCatch();
    }
    
    parseParameter(mappingsElement);
  }


  @Override
  public boolean isExecutionDetached() {
    return false;
  }


  @Override
  public String[] getInputVarIds() {
    return input.getVarIds();
  }


  @Override
  public String[] getInputVarPaths() {
    return input.getPaths();
  }


  public boolean isUserConnected(String varId) {
    return input.isUserConnected(varId);
  }


  public String[] getLocalVarIds() {
    return localIds;
  }


  public String[] getLocalVarPaths() {
    return localPaths;
  }


  @Override
  public String[] getOutputVarIds() {
    return outputIds;
  }


  public void addOutputVarId(int index, String id) {
    List<String> newOutputIds = new ArrayList<String>(Arrays.asList(outputIds));
    newOutputIds.add(index, id);
    outputIds = newOutputIds.toArray(new String[newOutputIds.size()]);

    // reset all inner data structures for input- and output variables and re-add inputs and outputs to inner data structures
    refreshContainers();
  }


  public void removeOutputVarId(int index) {
    List<String> newOutputIds = new ArrayList<String>(Arrays.asList(outputIds));
    newOutputIds.remove(index);
    outputIds = newOutputIds.toArray(new String[newOutputIds.size()]);

    // reset all inner data structures for input- and output variables and re-add inputs and outputs to inner data structures
    refreshContainers();
  }


  @Override
  public String[] getOutputVarPaths() {
    return outputPaths;
  }


  public Step getProxyForCatch() {
    if (catchStep != null) {
      return catchStep;
    }
    return this;
  }
  
  
  public List<ModelledExpression> getParsedExpressions() {
    return parsedExpressions;
  }


  public List<String> getRawExpressions() {
    return rawExpressions;
  }


  public AVariable getVariable(int varNum) throws XPRC_InvalidVariableIdException {
    if (varNum >= allVariablesInOrder.size()) {
      throw new XPRC_InvalidVariableIdException(String.valueOf(varNum));
    } else {
      return allVariablesInOrder.get(varNum);
    }
  }


  @Override
  protected Set<String> getAllUsedVariableIds() {
    return createVariableIdSet(input.getVarIds(), localIds, outputIds, getVarIds(allVariablesInOrder)); 
  }


  private String[] getVarIds(List<AVariable> vars) {
    String[] varIds = new String[vars.size()];
    for (int varNr = 0; varNr < vars.size(); varNr++) {
      AVariable var = vars.get(varNr);
      varIds[varNr] = var.getId();
    }

    return varIds;
  }


  public String getDataModel(String varId) throws XPRC_InvalidVariableIdException {
    AVariable v = refIdToVariable.get(varId);
    DataModelInformation dmi = v.getDataModelInformation();
    if (dmi == null) {
      return null;
    }
    return dmi.getModelNames()[0];
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException, XPRC_PrototypeDeployment,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_ParsingModelledExpressionException,
      XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED {
    for (int i = 0; i < input.length(); i++) {
      if (input.getVarIds()[i] == null) {
        throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.MAPPINGS, GenerationBase.EL.EXCEPTION);
      }
      if (input.getVarIds()[i].trim().length() == 0) {
        throw new XPRC_EmptyVariableIdException(GenerationBase.EL.MAPPINGSINPUT + "." + GenerationBase.EL.SOURCE + " "
            + GenerationBase.EL.MAPPINGS);
      }
    }
    for (int i = 0; i < localIds.length; i++) {
      if (localIds[i] == null) {
        throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.MAPPINGS, GenerationBase.EL.EXCEPTION);
      }
    }

    for (int i = 0; i < outputIds.length; i++) {
      if (outputIds[i] == null) {
        throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.MAPPINGSOUTPUT, GenerationBase.EL.EXCEPTION);
      }
      if (outputIds[i].trim().length() == 0) {
        throw new XPRC_EmptyVariableIdException(GenerationBase.EL.MAPPINGSOUTPUT + "." + GenerationBase.EL.TARGET + " "
            + GenerationBase.EL.MAPPINGS);
      }
    }

    if (rawExpressions != null) {
      for (int i = 0; i < rawExpressions.size(); i++) {
        String raw = rawExpressions.get(i);
        ModelledExpression parsed = parsedExpressions.get(i);
        if (parsed == null) {
          parsedExpressions.set(i, ModelledExpression.parse(this, raw ) );
        } else {
          if (parsed.getFoundAssign() == null && !parsed.hasPathMapTarget()) {
            //TODO hier müsste man noch überprüfen, dass das pathmaptarget auch ohne values zurecht kommt.
            throw new XPRC_ParsingModelledExpressionException(raw, 0, new RuntimeException("Mapping is missing assignment operator."));
          }
        }
      }
    }
    
    for (AVariable v : eVars) {
      v.validate();
    }
    for (AVariable v : variables) {
      v.validate();
    }
  }
  
  private void distributeCasts() throws XPRC_ParsingModelledExpressionException {
    // 1. Collection
    //    CastCollectingVisitor
    //      onFunctionStarts 
    //        if (isCast(fe))
    //          fe.getSubExpressions().get(1).visit(CastTargetKeyGenerator)
    //            Map<String, String> accessPathToCastedFqNameMap.put(CastTargetKeyGenerator.getKey, fe.getSubExpressions().get(0).getValue())
    // 1 1/3 Assignment Casts
    //   add a cast on left side for every right type?
    // 1 1/2 Cast Inheritance
    //   for every casting key
    //     for the accessPath of every source, if it is fully covered by the casting key
    //       replace %[0-9]+% with left assignment accessPath
    // 2. Distribution
    //    CastDistributingVistor
    //       onVariable || onVariablePart 
    //         generateKey over current part
    //         if map contains key, write cast (ignore previous casts so we don't duplicate)
    Map<String, String> collectedCasts = new HashMap<String, String>();
    List<ModelledExpression> intrim = new ArrayList<ModelledExpression>(rawExpressions.size());
    boolean wasThereAnyProgress = true;
    while (wasThereAnyProgress) {
      intrim.clear();
      
      for (String raw : rawExpressions ) {
        intrim.add( ModelledExpression.parse(this, raw) );
      }
      // 1
      CastHandlingVisitor castCollector = CastHandlingVisitor.collector(collectedCasts);
      for (ModelledExpression me : intrim) {
        me.visitTargetExpression(castCollector);
        if (me.getFoundAssign() != null) {
          me.visitSourceExpression(castCollector);
        }
      }
      
      collectedCasts = castCollector.getCollectedCasts();
    
      Map<String, String> inheritedCollectedCasts = new HashMap<String, String>();
      inheritedCollectedCasts.putAll(collectedCasts);
      
      // 1 1/3
      for (ModelledExpression me : intrim) {
        try {
          me.initTypesOfParsedFormula(Collections.<String> emptySet(), new MapTree());
          if (me.getFoundAssign() != null && me.getTargetType().isModelledType()) {
            CastHandlingVisitor keyGen = CastHandlingVisitor.keyGen();
            me.visitTargetExpression(keyGen);
            TypeInfo ti = me.getSourceType();
            if (ti != null &&
                ti.isModelledType() &&
                isDownCast(me.getTargetType().getModelledType(), ti.getModelledType())) {
              inheritedCollectedCasts.put(keyGen.getXFLExpression(), ti.getModelledType().getFqXMLName());
            }
          }
        } catch (XPRC_InvalidVariableIdException | XPRC_InvalidVariableMemberNameException | XPRC_ParsingModelledExpressionException | RuntimeException e) {
          // validate checkt das dann nochmal
        }
      }
      
      // 1 1/2
      Map<String, String> castCopy = new HashMap<String, String>();
      castCopy.putAll(inheritedCollectedCasts);
      for (Entry<String, String> castingKeyToType : inheritedCollectedCasts.entrySet()) {
        for (ModelledExpression me : intrim) {
          if (me.getFoundAssign() != null) {
            CastHandlingVisitor sourceKeyGen = CastHandlingVisitor.keyGen();
            me.visitSourceExpression(sourceKeyGen);
            if (castingKeyToType.getKey().startsWith(sourceKeyGen.getXFLExpression())) {
              CastHandlingVisitor targetKeyGen = CastHandlingVisitor.keyGen();
              me.visitTargetExpression(targetKeyGen);
              String newKey = targetKeyGen.getXFLExpression() + castingKeyToType.getKey().substring(sourceKeyGen.getXFLExpression().length());
              castCopy.put(newKey, castingKeyToType.getValue());
            }
          }
        }
      }
      
      inheritedCollectedCasts = castCopy;
      if (collectedCasts.size() == inheritedCollectedCasts.size()) {
        wasThereAnyProgress = false;
      }
      collectedCasts = inheritedCollectedCasts;
    
      // 2
      rawExpressions.clear();
      for( ModelledExpression parsed : intrim ) {
        rawExpressions.add( rewriteFormula(parsed, collectedCasts) );
      }
    
    }
    
  }
  
  
  private boolean isDownCast(ModelledType targetType, ModelledType sourceType) {
    DomOrExceptionGenerationBase dt1 = ((StepBasedType)targetType).getGenerationType();
    DomOrExceptionGenerationBase dt2 = ((StepBasedType)sourceType).getGenerationType();
    if (dt1 == dt2) {
      return false;
    }
    return DomOrExceptionGenerationBase.isSuperClass(dt1, dt2);
  }


  private String rewriteFormula(ModelledExpression me, Map<String, String> accessPathToTypeCast) {
    CastHandlingVisitor castDistributor = CastHandlingVisitor.distributor(accessPathToTypeCast);
    me.visitTargetExpression(castDistributor);
    String newExpression = castDistributor.getXFLExpression();
    // XFL-Identitity generation appears to add an additional pair of braces, this is problematic for conditional branches, we'll remove them
    if (newExpression.startsWith("(") && newExpression.endsWith(")")) {
      newExpression = newExpression.substring(1, newExpression.length() - 1);
    }
    if (me.getFoundAssign() != null) {
      castDistributor = CastHandlingVisitor.distributor(accessPathToTypeCast);
      me.visitSourceExpression(castDistributor);
      String newSourceExpression = castDistributor.getXFLExpression();
      if (newSourceExpression.startsWith("(") && newSourceExpression.endsWith(")")) {
        newSourceExpression = newSourceExpression.substring(1, newSourceExpression.length() - 1);
      }
      newExpression = newExpression + me.getFoundAssign().toXFL() + newSourceExpression;
    }
    return newExpression;
  }
  
  
  private static enum CastHandling {
    COLLECT(true, false, false) {
    },
    KEY_GENERATOR(false, true, false) {
    },
    DISTRIBUTOR(false, true, true) {
    };
    
    private final boolean collectCasts;
    private final boolean skipIdentityCastHandling;
    private final boolean writeCasts; // TODO merge skipIdentityCastHandling & writeCasts ?
    
    private CastHandling(boolean collectCasts, boolean skipIdentityCastHandling, boolean writeCasts) {
      this.collectCasts = collectCasts;
      this.skipIdentityCastHandling = skipIdentityCastHandling;
      this.writeCasts = writeCasts;
    }
    
  }
  
  private static class CastHandlingVisitor extends IdentityCreationVisitor {
    
    private final CastHandling handling;
    private final Map<String, String> accessPathToTypeCast;
    private final Object keyGenerationBreakPoint;
    private final Stack<Variable> variableStack = new Stack<Variable>();
    private String keyGenerationAtBreakPoint;
    
    
    CastHandlingVisitor(CastHandling handling, Map<String, String> accessPathToTypeCast, Object keyGenerationBreakPoint) {
      this.handling = handling;
      this.accessPathToTypeCast = accessPathToTypeCast;
      this.keyGenerationBreakPoint = keyGenerationBreakPoint;
    }
    
    private static CastHandlingVisitor collector() {
      return new CastHandlingVisitor(CastHandling.COLLECT, new HashMap<String, String>(), new Object());
    }
    
    private static CastHandlingVisitor collector(Map<String, String> collectedCasts) {
      return new CastHandlingVisitor(CastHandling.COLLECT, collectedCasts, new Object());
    }
    
    private static CastHandlingVisitor keyGen() {
      return new CastHandlingVisitor(CastHandling.KEY_GENERATOR, Collections.<String, String>emptyMap(), new Object());
    }
    
    private static CastHandlingVisitor keyGen(Object keyGenerationBreakPoint) {
      return new CastHandlingVisitor(CastHandling.KEY_GENERATOR, Collections.<String, String>emptyMap(), keyGenerationBreakPoint);
    }
    
    private static CastHandlingVisitor distributor(Map<String, String> accessPathToTypeCast) {
      return new CastHandlingVisitor(CastHandling.DISTRIBUTOR, accessPathToTypeCast, new Object());
    }
    
    @Override
    public void functionStarts(FunctionExpression fe) {
      boolean isCast = ModelledExpression.isCast(fe);
      if (handling.skipIdentityCastHandling) {
        lastFunction.push(fe);
        if (isCast) {
          skipNextLiteral = true;
          return;
        } else {
          sb.append(fe.getFunction().getName());
          if (!fe.getFunction().getName().equals("null")) {
            sb.append("(");
          }
        }
      } else {
        super.functionStarts(fe);
      }
      if (isCast && handling.collectCasts) {
        CastExpression ce = (CastExpression) fe;
        CastHandlingVisitor keyGenerator = keyGen();
        ce.getWrappedAccessPath().visit(keyGenerator);
        String accessPath = keyGenerator.getXFLExpression();
        accessPathToTypeCast.put(accessPath, ce.getDynamicTypeName());
      }
    }
    
    @Override
    public void functionEnds(FunctionExpression fe) {
      if (handling.skipIdentityCastHandling) {
        if (fe.getIndexDef() != null) {
          appendIndexDefEnd();
        }
      } else {
        super.functionEnds(fe);
      }
    }
    
    @Override
    public void variableStarts(Variable variable) {
      super.variableStarts(variable);
      variableStack.push(variable);
    }
    
    @Override
    public void variableEnds(Variable variable) {
      super.variableEnds(variable);
      if (handling.writeCasts) {
        CastHandlingVisitor keyGenerator = keyGen(variable);
        variable.visit(keyGenerator);
        if (accessPathToTypeCast.containsKey(keyGenerator.getXFLExpression())) {
          sb.append("#cast(\"")
            .append(accessPathToTypeCast.get(keyGenerator.getXFLExpression()))
            .append("\")");
        }
      }
      if (variable == keyGenerationBreakPoint) {
        keyGenerationAtBreakPoint = sb.toString();
      }
    }
    
    @Override
    public void variablePartEnds(VariableAccessPart part) {
      super.variablePartEnds(part);
      if (handling.writeCasts) {
        CastHandlingVisitor keyGenerator = keyGen(part);
        if (!variableStack.isEmpty()) {
          variableStack.peek().visit(keyGenerator);
        } else if (!lastFunction.isEmpty()) {
          lastFunction.peek().visit(keyGenerator);
        } else {
          throw new IllegalArgumentException("WTF");
        }
        if (accessPathToTypeCast.containsKey(keyGenerator.getXFLExpression())) {
          sb.append("#cast(\"")
            .append(accessPathToTypeCast.get(keyGenerator.getXFLExpression()))
            .append("\")");
        }
      }
      if (part == keyGenerationBreakPoint) {
        keyGenerationAtBreakPoint = sb.toString();
      }
    }
    
    @Override
    public void allPartsOfVariableFinished(Variable variable) {
      super.allPartsOfVariableFinished(variable);
      variableStack.pop();
    }
    
    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      super.allPartsOfFunctionFinished(fe);
    }
    
    public Map<String, String> getCollectedCasts() {
      return accessPathToTypeCast;
    }
    
    @Override
    public String getXFLExpression() {
      if (keyGenerationAtBreakPoint == null) {
        return super.getXFLExpression();
      } else {
        return keyGenerationAtBreakPoint;
      }
    }
    
  }
  
  
  private static class DynamicDependencyFinder extends EmptyVisitor {
    
    private final Map<String, XMOMType> dynamicTypes = new HashMap<String,XMOMType>();
                    
    @Override
    public void functionStarts(FunctionExpression fe) {
      if (fe instanceof DynamicResultTypExpression) {
        DynamicResultTypExpression drte = (DynamicResultTypExpression)fe;
        try {
          XMOMType dynamicType = drte.getDynamicTypeType();
          if (dynamicType != null) {
            dynamicTypes.put(drte.getDynamicTypeName(), dynamicType);
          }
        } catch (Exception e) {
          //ignore, fehler passiert später beim validieren. bei getDependencies soll es keinen fehler geben
        }
      }
    }
    
    private Map<String, XMOMType> getAllDynamicTypes() {
      return dynamicTypes;
    }
    
  }


  public void reevaluateMappings() {
    try {
      distributeCasts();
    } catch (XPRC_ParsingModelledExpressionException e) {
      //validate checkt das dann nochmal
    } catch (RuntimeException e) {
      //validate checkt das dann nochmal
    }
    parsedExpressions.clear();
    for( String raw : rawExpressions ) {
      ModelledExpression parsed = null;
      try {
        parsed = ModelledExpression.parse(this, raw );
      } catch (XPRC_ParsingModelledExpressionException e) {
        //validate checkt das dann nochmal
      } catch (RuntimeException e) {
        //validate checkt das dann nochmal
      }
      parsedExpressions.add( parsed );
    }
  }

  public void createEmpty(String label) {
    setXmlId(creator.getNextXmlId());
    setLabel(label);

    input = new InputConnections(0);
    localIds = new String[0];
    localPaths = new String[0];
    outputIds = new String[0];
    outputPaths = new String[0];

    reevaluateMappings();
  }

  public void createTemplate() throws XPRC_InvalidPackageNameException {
    setXmlId(creator.getNextXmlId());
    setLabel(label);

    input = new InputConnections(0);
    localIds = new String[0];
    localPaths = new String[0];
    isTemplateMapping = true;

    // add output variable
    DOM dom = DOM.getOrCreateInstance(DOCUMENT_PART_FQN, getCreator().getCacheReference(), getCreator().getRevision());
    dom.setLabel(DOCUMENT_PART_LABEL);
    ServiceVariable outputVar = new ServiceVariable(getCreator());
    outputVar.createDomOrException(dom.getLabel(), dom);
    variables.add(outputVar);
    outputVars.add(outputVar);
    outputIds = new String[1];
    outputPaths = new String[1];

    addFormula(0, TEMPLATE_INITIAL_FORMULA);
  }

  public String getLabel() {
    return label;
  }

  @Override
  public List<AVariable> getInputVars() {
    return inputVars;
  }

  @Override
  public List<AVariable> getOutputVars() {
    return outputVars;
  }

  /**
   * Updates indices in formula expressions. To be called after adding new input variable.
   */
  public void inputVarAdded(int index) {
    varAdded(index);
  }
  
  /**
   * Updates indices in formula expressions. To be called after adding new output variable.
   */
  public void outputVarAdded(int index) {
    varAdded(getInputVars().size() + index);
  }
  
  /**
   * Updates indices in formula expressions. To be called after removing an input variable.
   */
  public void inputVarRemoved(int index) {
    varRemoved(index);
  }

  /**
   * Updates indices in formula expressions. To be called after removing an output variable.
   */
  public void outputVarRemoved(int index) {
    varRemoved(getInputVars().size() + index);
  }

  public void inputVarMoved(int sourceIndex, int destinationIndex) {
    varMoved(sourceIndex, destinationIndex);
  }

  public void outputVarMoved(int sourceIndex, int destinationIndex) {
    int inputVarSize = getOutputVars().size();
    varMoved(inputVarSize + sourceIndex, inputVarSize + destinationIndex);
  }

  private void varMoved(int sourceIndex, int destinationIndex) {
    for (int formulaIdx = 0; formulaIdx < getFormulaCount(); formulaIdx++) {
      StringBuilder updatedFormula = new StringBuilder();
      List<XFLLexem> lexems = XFLLexer.lex(getFormula(formulaIdx), true);
      for (XFLLexem lexem : lexems) {
        if(TokenType.VARIABLE == lexem.getType()) {
          Integer index = Integer.valueOf(lexem.getToken().replaceAll("%", ""));

          if(index == sourceIndex) {
            // current index = source index --> change it to the destination index
            updatedFormula.append("%" + destinationIndex + "%");
          } else if(destinationIndex < sourceIndex && index >= destinationIndex && index < sourceIndex) {
            // destination index is smaller then source index
            // current index is between destination index and source index --> increment index
            updatedFormula.append("%" + (index + 1) + "%");
          } else if (destinationIndex > sourceIndex && index > sourceIndex && index <= destinationIndex) {
            // destination index is greater then source index
            // current index is between destination index and source index --> decrement index
            updatedFormula.append("%" + (index - 1) + "%");
          } else {
            // leave the token as it is
            updatedFormula.append(lexem.getToken());
          }
        } else if (TokenType.ARGUMENT_SEPERATOR == lexem.getType()) {
          // add a blank after argument seperator
          updatedFormula.append(lexem.getToken()).append(" ");
        } else {
          // leave the token as it is
          updatedFormula.append(lexem.getToken());
        }
      }

      rawExpressions.set(formulaIdx, updatedFormula.toString());
    }

    reevaluateMappings();
  }

  private String changeVariableIndex(String expression, int oldIndex, int newIndex) {
    StringBuilder updatedFormula = new StringBuilder();
    List<XFLLexem> lexems = XFLLexer.lex(expression, true);

    for (XFLLexem lexem : lexems) {
      if(TokenType.VARIABLE == lexem.getType()) {
        if(lexem.getToken().equals("%" + oldIndex + "%")) {
          // change the variable index to the new value
          updatedFormula.append("%").append(newIndex).append("%");
        } else {
          // leave the variable index as it is
          updatedFormula.append(lexem.getToken());
        }
      } else if (TokenType.ARGUMENT_SEPERATOR == lexem.getType()) {
        // add a blank after argument seperator
        updatedFormula.append(lexem.getToken()).append(" ");
      } else {
        // leave the token as it is
        updatedFormula.append(lexem.getToken());
      }
    }

    return updatedFormula.toString();
  }

  private String removeVariableIndex(String expression, int index) {
    StringBuilder updatedFormula = new StringBuilder();
    List<XFLLexem> lexems = XFLLexer.lex(expression, true);
    for (XFLLexem lexem : lexems) {
      if(TokenType.VARIABLE == lexem.getType()) {
        if(!lexem.getToken().equals("%" + index + "%")) {
          // leave the variable index as it is
          updatedFormula.append(lexem.getToken());
        }
      } else if (TokenType.ARGUMENT_SEPERATOR == lexem.getType()) {
        // add a blank after argument seperator
        updatedFormula.append(lexem.getToken()).append(" ");
      } else {
        // leave the token as it is
        updatedFormula.append(lexem.getToken());
      }
    }

    return updatedFormula.toString();
  }

  private void varAdded(int index) {
    List<AVariable> allVars = getAllVars();
    for (int varIdx = allVars.size()-1; varIdx >= index; varIdx--) {
      for (int formulaIdx = 0; formulaIdx < getFormulaCount(); formulaIdx++) {
        String updatedFormula = changeVariableIndex(getFormula(formulaIdx), varIdx, varIdx + 1);
        rawExpressions.set(formulaIdx, updatedFormula);
      }
    }

    reevaluateMappings();
  }

  private void varRemoved(int index) {
    // remove deleted variable from all formulas
    for (int formulaIdx = 0; formulaIdx < getFormulaCount(); formulaIdx++) {
      String updatedFormula = removeVariableIndex(getFormula(formulaIdx), index);
      rawExpressions.set(formulaIdx, updatedFormula);
    }

    // update indices of variables following the deleted one
    List<AVariable> allVars = getAllVars();
    for (int varIdx = index+1; varIdx < allVars.size()+1; varIdx++) {
      for (int formulaIdx = 0; formulaIdx < getFormulaCount(); formulaIdx++) {
        String updatedFormula = changeVariableIndex(getFormula(formulaIdx), varIdx, varIdx - 1);
        rawExpressions.set(formulaIdx, updatedFormula);
      }
    }

    reevaluateMappings();
  }

  private List<AVariable> getAllVars() {
    List<AVariable> allVars = new ArrayList<AVariable>(getInputVars());
    allVars.addAll(getOutputVars());

    return allVars;
  }

  @Override
  public String getDocumentation() {
    return documentation;
  }

  @Override
  public void setDocumentation(String documentation) {
   this.documentation = documentation;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setInputVars(List<AVariable> inputVars) {
    this.inputVars = inputVars;
  }

  public void setOutputVars(ArrayList<AVariable> outputVars) {
    this.outputVars = outputVars;
  }

  @Override
  public void addFormula(int index, String expression) {
    rawExpressions.add(index, expression);
    reevaluateMappings();
  }

  private static class ExpressionComparator implements Comparator<String> {
    public int compare(String formulaA, String formulaB) {
      if (formulaA == formulaB) {
          return 0;
      }
      if (formulaA == null) {
          return -1;
      }
      if (formulaB == null) {
          return 1;
      }

      // make sure formula assignments come before member accesses
      String formulaAcompare = formulaA.replace("~=", "\u0000\u0000").replaceFirst("=", "\u0000");
      String formulaBcompare = formulaB.replace("~=", "\u0000\u0000").replaceFirst("=", "\u0000");

      return formulaAcompare.compareTo(formulaBcompare);
    }
  }

  public void sortFormulas() {
    java.util.Collections.sort(rawExpressions, new ExpressionComparator());
  }

  @Override
  public String getFormula(int index) {
    return rawExpressions.get(index);
  }

  @Override
  public int getFormulaCount() {
    return rawExpressions.size();
  }

  public void replaceFormula(int index, String newExpression) {
    rawExpressions.set(index, newExpression);
    reevaluateMappings();
  }

  public String removeFormula(int index) {
    String removed = rawExpressions.remove(index);
    reevaluateMappings();
    return removed;
  }

  public void refreshContainers() {
    // reset all inner data structures for input- and output variables
    resetContainers();

    // restore variables
    addToContainers(getInputVars(), getInputVarIds());
    addToContainers(getOutputVars(), getOutputVarIds());
  }

  private void resetContainers() {
    refIdToVariable.clear();
    allVariablesInOrder.clear();
    variables.clear();
    eVars.clear();
  }

  /*
   * adds the given input- or output-variables and IDs to the internal data structures that are used for cross-referencing
   */
  private void addToContainers(List<AVariable> vars, String[] ids) {
    for (int varNr = 0; varNr < vars.size(); varNr++) {
      AVariable var = vars.get(varNr);
      allVariablesInOrder.add(var);

      if (ids[varNr] == null) { // TODO: Is this really necessary?
        if (var.getId() == null || var.getId().length() == 0) {
          var.setId("mapping" + cnt.incrementAndGet());
        }
        ids[varNr] = var.getId();
        ids[varNr] = "";
      }
      refIdToVariable.put(ids[varNr], var);

      if (var instanceof ServiceVariable) {
        variables.add((ServiceVariable)var);
      } else {
        eVars.add((ExceptionVariable)var);
      }
    }
  }

  @Override
  public void addLabelsToParameter() {  
    // copy labels from mapping since those are not set in parameter-tag
    addLabelsToParameter(inputVars, outputVars);
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.MAPPINGS); {
      Integer xmlId = getXmlId();
      if (xmlId != null) {
        xml.addAttribute(ATT.ID, xmlId.toString());
      }
      xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(getLabel()));
      xml.endAttributes();
      
      // <Meta>
      if ( (isTemplateMapping()) || (isConditionMapping()) || (!"".equals(getDocumentation())) || (hasUnknownMetaTags()) ) {
        xml.startElement(EL.META); {
          if (isTemplateMapping()) {
            xml.element(EL.ISTEMPLATE, "true");
          }
          if (isConditionMapping()) {
            xml.element(EL.ISCONDITION, "true");
          }
          if (!"".equals(getDocumentation())) {
            xml.element(EL.DOCUMENTATION, XMLUtils.escapeXMLValueAndInvalidChars(getDocumentation(), false, false));
          }

          appendUnknownMetaTags(xml);
        } xml.endElement(EL.META);
      }
      
      // <Input>
      appendVars(xml, getInputVars(), getInputVarIds(), true);
      
      // <Output>
      appendVars(xml, getOutputVars(), getOutputVarIds(), false);
      
      // <Mapping>
      for (String expression : rawExpressions) {
        xml.element(EL.MAPPING, XMLUtils.escapeXMLValueAndInvalidChars(expression, false, false));
      }
      
      // TODO: catches and compensations?
    } xml.endElement(EL.MAPPINGS);
  }

  private void appendVars(XmlBuilder xml, List<AVariable> vars, String[] varIds, boolean isInput) {
    String elementName = isInput ? EL.MAPPINGSINPUT : EL.MAPPINGSOUTPUT;
    for (int varNr = 0; varNr < vars.size(); varNr++) {
      AVariable var = vars.get(varNr);
      String varId = varIds[varNr];

      xml.startElement(elementName); {
        if (isInput) {
          var.appendXML(xml);
          appendSource(xml, varId, isUserConnected(varId), input.isConstantConnected(varId), false, input.getUnknownMetaTags(varId));
        } else {
          var.appendXML(xml);
          appendTarget(xml, varId, false);
        }
      } xml.endElement(elementName);
    }
  }

  public InputConnections getInputConnections() {
    return input;
  }
}

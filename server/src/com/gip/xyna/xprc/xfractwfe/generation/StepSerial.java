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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.XynaException;
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
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

public class StepSerial extends Step implements Catchable {

  private List<Step> children = new ArrayList<Step>();
  private List<Service> services = new ArrayList<Service>();
  private List<ServiceVariable> variables = new ArrayList<ServiceVariable>();
  private List<ExceptionVariable> eVars = new ArrayList<ExceptionVariable>();
  private List<String> inputVarIds = new ArrayList<String>();
  private StepCatch catchStep;

  private boolean isExecutionDetached = false;

  //gibt an, ob die xmlId parametrisiert werden soll,
  //z.B. falls der Step bei Choices von mehreren Cases verwendet wird (Alias-Steps)
  private boolean parameteriseXmlId = false;
  
  public StepSerial(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }  
  
  public StepSerial(ScopeStep parentScope, ArrayList<String> inputVarIds, GenerationBase creator) {
    super(parentScope, creator);
    this.inputVarIds = inputVarIds;
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepSerial( this );
  }

  @Override
  protected void removeVariable(AVariable var) {
    if (!variables.remove(var)) {
      if (!eVars.remove(var)) {
        throw new RuntimeException("var " + var + " not found in step " + this);
      }
    }
  }
  
  public void removeVar(AVariable var) {
    removeVariable(var);
  }

  /**
   * parst variablen und kind-steps
   * @param e
   */
  public void parseXML(Element e) throws XPRC_InvalidPackageNameException {

    parseUnknownMetaTags(e, new ArrayList<String>()); // TODO: also handle other meta-tags of this step
    isExecutionDetached = doesMetaElementDetachedExist(e);

    // get id
    parseId(e);

    //parse services
    List<Element> ss = XMLUtils.getChildElementsByName(e, GenerationBase.EL.SERVICEREFERENCE);
    for (Element s : ss) {
      Service service = new Service(creator);
      service.parseXML(s, null);
      services.add(service);
    }

    // parse variables
    List<Element> ds = XMLUtils.getChildElementsByName(e, GenerationBase.EL.DATA);
    for (Element d : ds) {
      ServiceVariable sv = new ServiceVariable(creator);
      sv.parseXML(d);
      variables.add(sv);
    }

    // parse ExceptionVariables
    ds = XMLUtils.getChildElementsByName(e, GenerationBase.EL.EXCEPTION);
    for (Element d : ds) {
      ExceptionVariable ev = new ExceptionVariable(creator);
      ev.parseXML(d);
      eVars.add(ev);
    }
 
    ds = XMLUtils.getChildElementsByName(e, GenerationBase.EL.CATCH);
    if (ds.size() > 0) {
      catchStep = new StepCatch(getParentScope(), this, creator);
      catchStep.parseXML(e);  
    }

    // parse child f/c-s in korrekter reihenfolge
    List<Element> childelements = XMLUtils.getChildElements(e);
    for (Element ce : childelements) {
      parseStepByElement(ce);
    }
    
    parseParameter(e);
  }


  private void parseStepByElement(Element ce) throws XPRC_InvalidPackageNameException {
    Step s = null;
    if (ce.getTagName().equals(GenerationBase.EL.FUNCTION)) {
      StepFunction sf = new StepFunction(getParentScope(), creator);
      sf.parseXML(ce);
      children.add(sf.getProxyForCatch());
    } else if (ce.getTagName().equals(GenerationBase.EL.SERIAL)) {
      StepSerial serial = new StepSerial(getParentScope(), creator);
      serial.parseXML(ce);
      children.add(serial.getProxyForCatch());
    } else if (ce.getTagName().equals(GenerationBase.EL.PARALLEL)) {
      s = new StepParallel(getParentScope(), creator);
    } else if (ce.getTagName().equals(GenerationBase.EL.CHOICE)) {
      s = new StepChoice(getParentScope(), creator);
    } else if (ce.getTagName().equals(GenerationBase.EL.THROW)) {
      s = new StepThrow(getParentScope(), creator);
    } else if (ce.getTagName().equals(GenerationBase.EL.ASSIGN)) {
      s = new StepAssign(getParentScope(), creator);
    } else if (ce.getTagName().equals(GenerationBase.EL.FOREACH)) {
      s = new StepForeach(getParentScope(), creator);
    } else if (ce.getTagName().equals(GenerationBase.EL.RETRY)) {
      s = new StepRetry(getParentScope(), creator);
    } else if (ce.getTagName().equals(GenerationBase.EL.MAPPINGS)) {
      StepMapping mapping = new StepMapping(getParentScope(), creator);
      mapping.parseXML(ce);
      children.add(mapping.getProxyForCatch());
    }
    if (s != null) {
      s.parseXML(ce);
      children.add(s);
    }
  }


  @Override
  public void setCatchStep(StepCatch catchStep) {
    this.catchStep = catchStep;
  }


  @Override
  public Step getProxyForCatch() {
    if (catchStep != null) {
      return catchStep;
    }
    return this;
  }


  protected void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException {
    for (ServiceVariable sv : variables) {
      sv.getImports(imports);
    }
    for (Service s : services) {
      if (s.isDOMRef()) {
        imports.add(s.getFQClassName());
      }
    }
    for (ExceptionVariable exVar: eVars) {
      imports.add(exVar.getFQClassName());
    }
    
    if (catchStep != null) {
      catchStep.getImports(imports);
    }
  }
  
  public void removeUnusedConstants(List<AVariable> usedConstants) {
    List<AVariable> candidates = new ArrayList<AVariable>(variables);
    candidates.addAll(eVars);
    
    for(AVariable v : candidates) {
      //if v is constant variable but not in usedConstants, remove it!
      if( v.getVarName().equals("const_" + v.getFQClassName()) ||
          (v.getChildren() != null && v.getChildren().size() > 0)) {
        //v is constant
        if(!usedConstants.contains(v)) {
          removeVariable(v);
        }
      }
    }
  }


  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
 throws XPRC_InvalidVariableIdException {

    cb.addLine("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName(), "<",
               getParentScope().getClassName(), ">", " {").addLB();

    cb.addLine("private static final long serialVersionUID = ", Step.SERIAL_VERSION_UID_NO_VARS, "L");

    if (parameteriseXmlId) {
      cb.addLine("private Integer xmlId;").addLB();
    }
    
    cb.addLine("public ", getClassName(), "() {").addLine("super(" + getIdx(), ")");
    cb.addLine("}").addLB();

    if (parameteriseXmlId) {
      cb.addLine("public ", getClassName(), "(Integer xmlId) {").addLine("super(" + getIdx(), ")");
      cb.addLine("this.xmlId = xmlId;");
      cb.addLine("}").addLB();
    }
    
    appendExecuteInternally(cb, importedClassesFqStrings);

    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    // keine compensation notwendig
    cb.addLine("}").addLB();

    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, inputVarIds.toArray(new String[0]),
                                          new String[inputVarIds.size()], cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, new String[0], null, cb, importedClassesFqStrings);

    generatedGetRefIdMethod(cb);

    cb.addLine("protected ", FractalProcessStep.class.getSimpleName(), "<", getParentScope().getClassName(),
               ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    cb.addLine("if (i == 0) {");
    cb.add("return new ", FractalProcessStep.class.getSimpleName(), "[]{");
    for (Step s : children) {
      cb.addListElement(METHODNAME_GET_PARENT_SCOPE + "()." + s.getVarName());
    }
    cb.add("};").addLB().addLine("}");
    cb.addLine("return null");
    cb.addLine("}").addLB();

    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return 1");
    cb.addLine("}").addLB();

    cb.addLine("}").addLB();

  }
  
  @Override
  protected void generatedGetRefIdMethod(CodeBuffer cb) {
    cb.addLine("public Integer getXmlId() {");
    if (parameteriseXmlId) {
      cb.addLine("if (xmlId != null) {");
      cb.addLine("return xmlId;");
      cb.addLine("}");
    }
    cb.addLine("return " + getXmlId());
    cb.addLine("}").addLB();
  }

  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(0)");
    cb.addLine("}").addLB();
  }
  
  
  @Override
  public List<Step> getChildSteps() {
    return children;
  }


  @Override
  public boolean replaceChild(Step oldChild, Step newChild) {
    for (int childNo = 0; childNo < children.size(); childNo++) {
      if (children.get(childNo) == oldChild) {
        children.set(childNo, newChild);
        return true;
      }
    }

    return false;
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    ArrayList<GenerationBase> allVars = new ArrayList<GenerationBase>();
    for (Service s : services) {
      if (s.isDOMRef()) {
        if (s.getDom() != null) {
          allVars.add(s.getDom());
        }
      } else {
        if (s.getWF() != null) {
          allVars.add(s.getWF());
        }
      }
    }
    for (ServiceVariable s : variables) {
      allVars.addAll(s.getDependencies());
    }
    for (ExceptionVariable s : eVars) {
      allVars.addAll(s.getDependencies());
    }
    return allVars;
  }


  @Override
  public List<ExceptionVariable> getExceptionVariables() {
    ArrayList<ExceptionVariable> allVars = new ArrayList<ExceptionVariable>(eVars);
    return allVars;
  }


  protected void addExceptionVariable(ExceptionVariable exceptionVariable) {
    eVars.add(exceptionVariable);
  }


  protected void removeExceptionVariable(ExceptionVariable exceptionVariable) {
    eVars.remove(exceptionVariable);
  }


  @Override
  protected List<Service> getServices() {
    ArrayList<Service> allServices = new ArrayList<Service>();
    allServices.addAll(services);
    for (ServiceVariable sv : variables) {
      if (sv.getService() != null) {
        allServices.add(sv.getService());
      }
    }
    return allServices;
  }


  @Override
  public List<ServiceVariable> getServiceVariables() {
    ArrayList<ServiceVariable> allVars = new ArrayList<ServiceVariable>(variables);
    return allVars;
  }

  public List<AVariable> getVariablesAndExceptions(){
    ArrayList<AVariable> result = new ArrayList<AVariable>();
    result.addAll(getServiceVariables());
    result.addAll(getExceptionVariables());
    
    return result;
  }

  public void resetVariablesAndExceptions() {
    variables = new ArrayList<>();
    eVars = new ArrayList<>();
  }


  @Override
  public boolean isExecutionDetached() {
    return isExecutionDetached;
  }

  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepSerial)) {
      return true;
    }
    StepSerial oldSerialStep = (StepSerial)oldStep;
    
    if (children != null && oldSerialStep.children != null) {
      if (children.size() != oldSerialStep.children.size()) {
        return true;
      }
      for (int i=0; i < children.size(); i++) {
        if (children.get(i).compareImplementation(oldSerialStep.children.get(i))) {
          return true;
        }
      }
    } else if (children == null ^ oldSerialStep.children == null) {
      return true;
    }
    
    if (catchStep != null) {
      if (catchStep.compareImplementation(oldSerialStep.catchStep)) {
        return true;
      }
    } else if (oldSerialStep.catchStep != null) {
      return true;
    }
    
    return false;
  }

  @Override
  protected Set<String> getAllUsedVariableIds() {
    return createVariableIdSet(inputVarIds);
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException, XPRC_PrototypeDeployment,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_InvalidXMLMissingListValueException,
      XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED {
    for (AVariable v : eVars) {
      v.validate();
    }
    for (AVariable v : variables) {
      v.validate();
    }
  }

  
  public void setParameteriseXmlId(boolean parameteriseXmlId) {
    this.parameteriseXmlId = parameteriseXmlId;
  }

  public void createEmpty() {
    setXmlId(creator.getMaxXmlId()+1);
  }

  public void addChild(int index, Step child) {
    children.add(child instanceof StepAssign ? index : clipIndex(index), child);
  }

  public void addChildren(List<Step> children) {
   addChildren(this.children.size(), children); 
  }

  public void addChildren(int index, List<Step> children) {
    this.children.addAll(clipIndex(index), children);
  }

  private int clipIndex(int index) {
    int size = children.size();
    if (index > 0 && size > 0 && index >= size && children.get(size - 1) instanceof StepAssign) {
      index = size - 1; // StepAssign must stay at last position
    } else if (index == 0 && size > 1 && children.get(0) instanceof StepAssign) {
      index++; // StepAssign must stay at first position
    }

    return index;
  }

  public void removeChild(Step child) {
    children.remove(child);
  }

  public boolean replaceChild(int index, Step newChild) {
    if ( (index < 0) || (index >= children.size()) || (children.get(index) instanceof StepAssign) ) {
      return false;
    }

    children.set(index, newChild);
    return true;
  }

  public void addVar(AVariable var) {
    if (var instanceof ServiceVariable) {
      variables.add((ServiceVariable)var);
    } else if (var instanceof ExceptionVariable) {
      eVars.add((ExceptionVariable)var);
    }
  }

  public boolean replaceVar(AVariable oldVar, AVariable newVar) {
    if (oldVar instanceof ServiceVariable && newVar instanceof ServiceVariable) {
      int index = variables.indexOf(oldVar);
      if (index < 0) {
        return false;
      }
      variables.set(index, (ServiceVariable)newVar);

      return true;
    } else if (oldVar instanceof ExceptionVariable && newVar instanceof ExceptionVariable) {
      int index = eVars.indexOf(oldVar);
      if (index < 0) {
        return false;
      }
      eVars.set(index, (ExceptionVariable)newVar);

      return true;
    }

    return false;
  }

  public StepAssign findFirstAssign() {
    for (Step childStep : getChildSteps()) {
      if (childStep instanceof StepAssign) {
        return (StepAssign)childStep;
      }
    }

    return null;
  }

  @Override
  public boolean isInRetryLoop() {
    if (super.isInRetryLoop()) {
      return true;
    }

    Step parentStep = getParentStep();
    if (parentStep instanceof StepCatch) {
      StepCatch stepCatch = (StepCatch)parentStep;
      Step triedStep = stepCatch.getStepInTryBlock();
      if (triedStep != null && triedStep.hasBeenExecuted() && triedStep.getParameterList().stream().anyMatch(x -> x.getRetryCounter() > 0)) {
        // step has been retried
        return true;
      }

      for (Step executedCatch : stepCatch.getExecutedCatches()) {
        if (executedCatch.getParameterList().stream().anyMatch(x -> x.getRetryCounter() > 0)) {
          // another branch has been retried, hence step is part of a retry loop
          return true;
        }
      }
    }

    return false;
  }

  public Step getLastExecutedStep(Parameter parameter) {
    Step lastExecutedStep = null;
    for (Step childStep : getChildSteps()) {
      if (childStep.hasBeenExecuted(parameter.getForeachIndices(), parameter.getRetryCounter())) {
        lastExecutedStep = childStep;
      }
    }

    return lastExecutedStep;
  }

  @Override
  public boolean toBeShownInAudit() {
    return false;
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    appendXML(xml, false);
  }

  public void appendXML(XmlBuilder xml, boolean wrapInElement) {
    if (wrapInElement) {
      xml.startElement(EL.SERIAL);
    }

      // <Meta>
      if (hasUnknownMetaTags()) {
        xml.startElement(EL.META); {
          appendUnknownMetaTags(xml);
        } xml.endElement(EL.META);
      }

      List<StepAssign> detainedAssignSteps = new ArrayList<StepAssign>();
      boolean isWorkflowContainer = getParentStep() instanceof WFStep;
      for (Step childStep : getChildSteps()) {
        if (isWorkflowContainer && childStep instanceof StepAssign) {
          // the Flash-GUI needs assign-steps to be written after the global variables
          detainedAssignSteps.add((StepAssign)childStep);
        } else {
          childStep.appendXML(xml);
        }
      }

      for (ServiceVariable variable : variables) {
        variable.appendXML(xml);
      }

      // falls stepserial innerhalb von foreach, dortige lokale variablen rendern
      if (getParentStep() instanceof ForEachScopeStep) {
        for (AVariable v : getParentScope().getPrivateVars()) {
          v.appendXML(xml);
        }
      }

      for (ExceptionVariable variable : eVars) {
        variable.appendXML(xml);
      }

      // Flash-GUI needs assign-steps to be written after the global variables
      for (StepAssign stepAssign : detainedAssignSteps) {
        stepAssign.appendXML(xml);
      }

      // <Catch>
      if (catchStep != null) {
        catchStep.appendCatchAreas(xml);
      }

    if (wrapInElement) {
      xml.endElement(EL.SERIAL);
    }
  }

}

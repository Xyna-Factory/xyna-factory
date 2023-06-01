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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.ProcessStepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformationThrowable;



public class StepThrow extends Step {

  private String exceptionID;
  private boolean includeCause;
  private VariableIdentification targetExceptionVariable;
  private String label;
  private String preferedExceptionType = null;
  private String documentation = "";
  private InputConnections input;

  public StepThrow(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }
  
  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepThrow(this);
  }

  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {

    // write a class declaration
    cb.add("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName());
    cb.add("<", getParentScope().getClassName(), "> {").addLB();

    cb.addLine("private static final long serialVersionUID = ", Step.SERIAL_VERSION_UID_NO_VARS, "L");

    cb.addLB().addLine("public " + getClassName() + "() {");
    cb.addLine("super(" + getIdx() + ")");
    cb.addLine("}").addLB();

    appendExecuteInternally(cb, importedClassesFqStrings);
    
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine("}").addLB();
    
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, new String[]{exceptionID}, new String[]{""}, cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, null, null, cb, importedClassesFqStrings);

    generatedGetRefIdMethod(cb);

    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<" + getParentScope().getClassName() + ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    cb.addLine("return null");
    cb.addLine("}").addLB();

    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return 0");
    cb.addLine("}").addLB();

    // end
    cb.addLine("}").addLB();

  }

  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", targetExceptionVariable.variable.getClassName(importedClassesFqStrings), " {");

    // this is the interesting part: the exception is built up using the correct parameters
    /*
     * positiver nebeneffekt: hier wird der stacktrace neu erstellt.
     * negative nebeneffekt: erst exception erstellen, dann in mapping anpassen, dann werfen, führt nicht zu einem stacktrace von "weiter vorne" im workflow.
     * TODO: mit exception stacktraces korrekt umgehen.
     * vgl auch TODOs in XOUtils (ObjectVersionBase.equals/hashcode)
     */
    if (targetExceptionVariable.variable.isJavaBaseType() &&
        targetExceptionVariable.variable.getFQClassName().equals(XynaException.class.getName())) {
      cb.addLine(XynaException.class.getSimpleName() + " newException");
      cb.addLine("if (", targetExceptionVariable.getScopeGetter(getParentScope()),
                 targetExceptionVariable.variable.getVarName(), " != null) {");
      cb.addLine("newException = new ", XynaException.class.getSimpleName(), "(",
                 targetExceptionVariable.getScopeGetter(getParentScope()),
                 targetExceptionVariable.variable.getVarName(), ".getCode(), ",
                 targetExceptionVariable.getScopeGetter(getParentScope()),
                 targetExceptionVariable.variable.getVarName(), ".getArgs())");
      cb.addLine("} else {");
      cb.addLine("newException = new ", XynaException.class.getSimpleName(),
                 "(\"There is no exception code defined for this exception!\")");
      cb.addLine("}");
    } else {
      //FIXME das clone führt zu hässlichem stacktrace. der fängt dann immer mit clone() an, wo man sich fragt "wieso?"
      cb.addLine(targetExceptionVariable.variable.getClassName(importedClassesFqStrings) + " newException = "
                      + targetExceptionVariable.getScopeGetter(getParentScope())
                      + targetExceptionVariable.variable.getVarName() + ".cloneWithoutCause()");
    }

    if (isIncludeCause()) {
      //TODO besser zur generierungszeit schon berechnen, welcher step der catch-step ist. das muss man nicht erst zur laufzeit berechnen.
      //das hat auch den vorteil, dass man nicht über die xynaexceptioninformationthrowable gehen muss.
      cb.addLB();
      cb.addLine(FractalProcessStep.class.getSimpleName() + " parent = ", METHODNAME_GET_PARENT_STEP, "()");
      cb.addLine("while (parent != null) {");
      cb.addLine("if (parent instanceof ", ProcessStepCatch.class.getSimpleName(), ") {");
      String exVarContainerExString = XynaExceptionInformationThrowable.class.getSimpleName();
      cb.addLine(Throwable.class.getSimpleName(), " tmpEx");
      cb.addLine("tmpEx = new ", exVarContainerExString, "(parent.", METHODNAME_GET_CAUGHT_EXCEPTION, "())");
      cb.addLine("newException.initCause(tmpEx)");
      cb.addLine("break");
      cb.addLine("} else {");
      cb.addLine("parent = parent.", METHODNAME_GET_PARENT_STEP, "()");
      cb.addLine("}");
      cb.addLine("}");
    }
    cb.addLine("throw newException");
    cb.addLine("}").addLB();
  }


  public void parseXML(Element e) throws XPRC_InvalidPackageNameException { // FIXME: too much duplicated code with other parse-methods (for instance in StepMapping)

    parseId(e);

    label = e.getAttribute(GenerationBase.ATT.LABEL);
    exceptionID = e.getAttribute(GenerationBase.ATT.EXCEPTION_ID);

    // parse input
    Element source = XMLUtils.getChildElementByName(e, GenerationBase.EL.SOURCE);
    input = new InputConnections(1);
    if (source != null) {
      input.parseSourceElement(source, 0);
    }

    // Meta
    parseUnknownMetaTags(e, Arrays.asList(EL.DOCUMENTATION, EL.PREFERED_EXCEPTION_TYPE));
    Element meta = XMLUtils.getChildElementByName(e, GenerationBase.EL.META);
    if (meta != null) {
      Element documentationElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        documentation = XMLUtils.getTextContent(documentationElement);
      }

      Element preferedExceptionElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.PREFERED_EXCEPTION_TYPE);
      if (preferedExceptionElement != null) {
        preferedExceptionType = XMLUtils.getTextContent(preferedExceptionElement);
      }
    }

    includeCause = !"false".equalsIgnoreCase(e.getAttribute(GenerationBase.ATT.INCLUDE_CAUSE));

    parseParameter(e);
  }


  /**
   * If the step is part of a catch step, this indicates whether the cause of that step should be included. It may be
   * unwanted in some cases to include the cause because it could reveal more information than wanted to the user
   */
  protected boolean isIncludeCause() {
    return includeCause;
  }

  @Override
  protected void getImports(HashSet<String> imports) {
    if (isIncludeCause()) {
      imports.add(XynaExceptionInformationThrowable.class.getName());
      imports.add(ProcessStepCatch.class.getName());
    }
    imports.add(XynaException.class.getName());
  }

  @Override
  protected List<GenerationBase> getDependencies() {
    List<GenerationBase> result = new ArrayList<GenerationBase>();
    if (targetExceptionVariable != null) {
      result.addAll(targetExceptionVariable.variable.getDependencies());
    }
    return result;
  }

  @Override
  public String getLabel() {
    return label;
  }

  public String getExceptionTypeFqn() {
    // try to determine targetExceptionVariable
    
    //if we are not connected
    if(exceptionID == null || exceptionID.length() == 0) 
      return preferedExceptionType;
    
    try {
      targetExceptionVariable = getParentScope().identifyVariable(exceptionID);
    } catch (Exception e) {}

    if (targetExceptionVariable != null) {
      String name = targetExceptionVariable.getVariable().getOriginalName();
      String path = targetExceptionVariable.getVariable().getOriginalPath();
      String fqn = path + "." + name;
      return fqn;
    } else {
      return preferedExceptionType;
    }
  }
  
  public String getPrefedExceptionType() {
    return preferedExceptionType;
  }
  
  public String getDocumentation() {
    return documentation;
  }

  public InputConnections getInputConnections() {
    return input;
  }

  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return null;
  }
  
  @Override
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }

  @Override
  protected List<Service> getServices() {
    return null;
  }

  @Override
  protected List<ServiceVariable> getServiceVariables() {
    return null;
  }


  @Override
  public List<Step> getChildSteps() {
    return null;
  }


  @Override
  public boolean isExecutionDetached() {
    return false;
  }

  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepThrow)) {
      return true;
    }
    StepThrow oldThrowStep = (StepThrow)oldStep;
    return !exceptionID.equals(oldThrowStep.exceptionID);
  }

  @Override
  public Set<String> getAllUsedVariableIds() {
    return createVariableIdSet(new String[]{exceptionID});
  }
  
  public VariableIdentification getTargetExceptionVariable() {
    return targetExceptionVariable;
  }

  @Override
  public List<ExceptionVariable> getAllThrownExceptions(boolean considerRetryAsHandled) {
    List<ExceptionVariable> exceptions = new ArrayList<ExceptionVariable>();
    VariableIdentification target = getTargetExceptionVariable();
    ExceptionVariable thrownException;
    if (target != null) {
      thrownException = (ExceptionVariable)target.getVariable();
    } else { // TODO: remove duplicate code with IdentifiedVariablesStepThrow.java
      thrownException = createDummyVar();
    }

    exceptions.add(thrownException);

    return exceptions;
  }


  public ExceptionVariable createDummyVar() {
    ExceptionVariable dummy = new ExceptionVariable(getCreator());
    String exceptionFqn = getExceptionTypeFqn();

    if (GenerationBase.isReservedServerObjectByFqClassName(exceptionFqn)) {
      try {
        Class<?> clazz = getClass().getClassLoader().loadClass(exceptionFqn);
        String actualFqn = GenerationBase.getXmlNameForReservedClass(clazz);
        exceptionFqn = actualFqn;
      } catch (ClassNotFoundException e) {
      }
    }
    
    Pair<String, String> pathAndName = GenerationBase.getPathAndNameFromJavaName(exceptionFqn);
    try {
      dummy.init(pathAndName.getFirst(), pathAndName.getSecond());
      dummy.setLabel(pathAndName.getSecond());
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException("Can't create dummy variable", e);
    }

    return dummy;
  }

  @Override
  public String[] getInputVarIds() {
    return input.getVarIds();
  }

  @Override
  public List<AVariable> getInputVars() {
    List<AVariable> inputVars = new ArrayList<AVariable>();

    if(exceptionID == null || exceptionID.length() == 0)
      return inputVars;
    
    // try to determine targetExceptionVariable
    try {
      targetExceptionVariable = getParentScope().identifyVariable(exceptionID);
    } catch (Exception e) {}

    if (targetExceptionVariable != null) {
      inputVars.add(targetExceptionVariable.getVariable());
    }

    return inputVars;
  }

  public void create(String fqn, String label) {
    setXmlId(creator.getNextXmlId());
    this.input = new InputConnections(1);
    includeCause = true;

    this.preferedExceptionType = fqn;
    this.label = label;
  }

  public void setExceptionID(String id) {
    exceptionID = id;
  }
  
  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_InvalidVariableIdException {
    if (exceptionID.trim().length() == 0) {
      throw new XPRC_EmptyVariableIdException(GenerationBase.EL.THROW);
    }
    
    targetExceptionVariable = getParentScope().identifyVariable(exceptionID);
    if (!(targetExceptionVariable.variable instanceof ExceptionVariable)) {
      throw new XPRC_InvalidVariableIdException(exceptionID);
    }

  }

  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.THROW); {
      Integer xmlId = getXmlId();
      if (xmlId != null) {
        xml.addAttribute(ATT.ID, xmlId.toString());
      }
      xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(getLabel()));
      if ( (exceptionID != null) && (exceptionID.length() > 0) ) {
        xml.addAttribute(ATT.EXCEPTION_ID, exceptionID);
      }
      if (!includeCause) {
        xml.addAttribute(ATT.INCLUDE_CAUSE, ATT.FALSE);
      }
      xml.endAttributes();

      // <Meta>
      boolean hasDocumentation = (documentation != null) && (documentation.length() > 0);
      if (hasDocumentation || preferedExceptionType != null || hasUnknownMetaTags()) {
        xml.startElement(EL.META); {
          if (hasDocumentation) {
            xml.element(EL.DOCUMENTATION, XMLUtils.escapeXMLValueAndInvalidChars(documentation, false, false));
          }
          xml.optionalElement(EL.PREFERED_EXCEPTION_TYPE, XMLUtils.escapeXMLValueAndInvalidChars(preferedExceptionType, false, false));
          appendUnknownMetaTags(xml);
        } xml.endElement(EL.META);
      }

      // <Source>
      if (input.length() > 0) {
        String varId = input.getVarIds()[0];
        appendSource(xml, varId, input.isUserConnected(varId), input.isConstantConnected(varId), false, input.getUnknownMetaTags(varId));
      }
    } xml.endElement(EL.THROW);
  }

}

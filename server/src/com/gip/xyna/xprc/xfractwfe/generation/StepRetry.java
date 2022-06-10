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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.RetryException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.IRetryStep;
import com.gip.xyna.xprc.xfractwfe.base.ProcessStepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public class StepRetry extends Step implements HasDocumentation {
  
  private static final String _METHODNAME_RESET_EXECUTIONS_COUNTER_ORIG = "resetExecutionsCounter";
  protected static final String METHODNAME_RESET_EXECUTIONS_COUNTER;
  
  
  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_RESET_EXECUTIONS_COUNTER = IRetryStep.class.getDeclaredMethod(_METHODNAME_RESET_EXECUTIONS_COUNTER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_RESET_EXECUTIONS_COUNTER_ORIG + " not found", e);
    }
  }
  
  private final static String FQNAME_RETRY_PARAMETER = "xprc.retry.RetryParameter";
  private final static String RETRY_PARAMETER_VARIABLE_NAME_RETRY_LIMIT = "RetryLimit";

  private VariableIdentification input = null;
  private InputConnections inputConnections;
  private String label;
  private String documentation;
  
  public StepRetry(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepRetry( this );
  }

  @Override
  public void parseXML(Element e) throws XPRC_InvalidPackageNameException {
    parseId(e);

    label = e.getAttribute(GenerationBase.ATT.LABEL);
    List<Element> sources = XMLUtils.getChildElementsByName(e, GenerationBase.EL.SOURCE);
    inputConnections = new InputConnections(1);
    if (sources != null && sources.size() == 1) {
      inputConnections.parseSourceElement(sources.get(0), 0);

    } else if (sources != null && sources.size() > 1) {
      // nicht modellierbar => RuntimeException
      throw new RuntimeException("Too many inputs (" + sources.size() + ") for <" + getClass().getSimpleName() + ">");
    }

    parseUnknownMetaTags(e, Arrays.asList(EL.DOCUMENTATION));
    Element meta = XMLUtils.getChildElementByName(e, GenerationBase.EL.META);
    if (meta != null) {
      Element documentationElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        documentation = XMLUtils.getTextContent(documentationElement);
      }
    }

    parseParameter(e);
  }


  private long calcSerialVersionUID() {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    types.add(Pair.of("executions", long.class.getName()));
    return GenerationBase.calcSerialVersionUID(types);
  }


  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
                  XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException {
    
    //TODO eigtl wäre schön, das beim validate zu machen. in parseXML ist es zu früh dafür
    input = getParentScope().identifyVariable(inputConnections.getVarIds()[0]);

    //input kann nicht null sein, das macht identifyVariable
    if (input.variable == null) {
      throw new RuntimeException("variable undefined.");
    }
    if (!input.variable.getFQClassName().equals(FQNAME_RETRY_PARAMETER)) {
      // das sollte GUI-seitig nicht modellierbar sein => RuntimeException
      throw new RuntimeException("Invalid input for step, expected <" + FQNAME_RETRY_PARAMETER + ">, got <"
          + input.variable.getFQClassName() + ">");
    }
    
    
    // write a class declaration
    cb.add("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName());
    cb.add("<", getParentScope().getClassName(), "> implements ", IRetryStep.class.getSimpleName(), " {").addLB(2);

    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calcSerialVersionUID()), "L");

    cb.addLB().addLine("public ", getClassName(), "() {");
    cb.addLine("super(" + getIdx(), ")");
    cb.addLine("}").addLB();

    cb.addLine("private long executions = 0;");
    cb.addLB();
    
    cb.addLine("public void ", METHODNAME_RESET_EXECUTIONS_COUNTER, "() {");
    cb.addLine("executions = 0;");
    cb.addLine("}").addLB();
    
    appendExecuteInternally(cb, importedClassesFqStrings);
    
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine("}").addLB();

    if (input != null) {
      generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, inputConnections.getVarIds(), inputConnections.getPaths(), cb, importedClassesFqStrings);
    } else {
      generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, null, null, cb, importedClassesFqStrings);
    }
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
    cb.add("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", RetryException.class.getSimpleName());
    if (input != null) {
      cb.add(", ", XynaException.class.getSimpleName());
    }
    cb.add(" {").addLB();

    cb.addLine("executions++;");
    
    if (input != null) {
      cb.add("long remainingTries = ");
      cb.add(input.getScopeGetter(getParentScope()) + input.variable.getVarName());
      cb.add(".get", RETRY_PARAMETER_VARIABLE_NAME_RETRY_LIMIT, "() - executions;");
      cb.addLB();
      cb.addLine("if (remainingTries >= 0) {");
      cb.addLine("throw new ", RetryException.class.getSimpleName(), "()");
      cb.addLine("} else {");
      cb.addLine(FractalProcessStep.class.getSimpleName(), " parent = ", METHODNAME_GET_PARENT_STEP, "()");
      cb.addLine("while (parent != null) {");
      cb.addLine("if (parent instanceof " + ProcessStepCatch.class.getSimpleName() + ") {");
      //FIXME exception handling übernehmen aus zb stepcatch
      cb.addLine("if (parent.", METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, "().getThrowable() instanceof ", XynaException.class.getSimpleName(), ") {"); 
      cb.addLine("throw (", XynaException.class.getSimpleName(), ") parent.", METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, "().getThrowable();");
      cb.addLine("} else if (parent.", METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, "().getThrowable() instanceof ", RuntimeException.class.getSimpleName(), ") {");
      cb.addLine("throw (", RuntimeException.class.getSimpleName(), ") parent.", METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, "().getThrowable();");
      cb.addLine("} else {");
      cb.addLine("throw new ", RuntimeException.class.getSimpleName(), "(parent.", METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, "().getThrowable());");
      cb.addLine("}");
      cb.addLine("} else {");
      cb.addLine("parent = parent.", METHODNAME_GET_PARENT_STEP, "()");
      cb.addLine("}");
      cb.addLine("}");
      cb.addLine("}").addLB();
    } else {
      cb.addLine("throw new ", RetryException.class.getSimpleName(), "()");
    }
    cb.addLine("}").addLB();
  }

  @Override
  protected void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException,
                  XPRC_InvalidServiceIdException {
    imports.add(RetryException.class.getName());
  }
  
  
  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return null;
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
  protected boolean compareImplementation(Step oldStep) {
    return false;
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    return null;
  }

  
  @Override
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }


  @Override
  public boolean isExecutionDetached() {
    return false;
  }


  @Override
  protected Set<String> getAllUsedVariableIds() {
    return createVariableIdSet(inputConnections.getVarIds());
  }


  public InputConnections getInputConnections() {
    return inputConnections;
  }

  @Override
  public String[] getInputVarIds() {
    return inputConnections.getVarIds();
  }
  

  @Override
  public String getLabel() {
    return label;
  }


  @Override
  public String getDocumentation() {
    return documentation;
  }


  @Override
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }


  public void create(String label, String documentation) {
    setXmlId(creator.getNextXmlId());
    inputConnections = new InputConnections(1);
    this.label = label;
    this.documentation = documentation;
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException {
    
  }


  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.RETRY); {
      Integer xmlId = getXmlId();
      if (xmlId != null) {
        xml.addAttribute(ATT.ID, xmlId.toString());
      }
      if (inputConnections.length() > 0) {
        String varId = inputConnections.getVarIds()[0];
        if (varId != null) {
          xml.addAttribute(ATT.RETRY_PARAMETER_ID, varId.toString());
        }
      }
      xml.addAttribute(ATT.LABEL, getLabel());

      xml.endAttributes();

      // <Meta>
      String documentation = XMLUtils.escapeXMLValueAndInvalidChars(getDocumentation(), false, false);
      if ( ((documentation != null) && (documentation.length() > 0)) || (hasUnknownMetaTags()) ) {
        xml.startElement(EL.META); {
          if ( (documentation != null) && (documentation.length() > 0) ) {
            xml.optionalElement(EL.DOCUMENTATION, documentation);
          }

          appendUnknownMetaTags(xml);
        } xml.endElement(EL.META);
      }

      // <Source>
      if (inputConnections.length() > 0) {
        String varId = inputConnections.getVarIds()[0];
        appendSource(xml, varId, inputConnections.isUserConnected(varId), inputConnections.isConstantConnected(varId), false, inputConnections.getUnknownMetaTags(varId));
      }
    } xml.endElement(EL.RETRY);
  }

}

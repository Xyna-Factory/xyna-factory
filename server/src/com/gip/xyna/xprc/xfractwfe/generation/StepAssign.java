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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class StepAssign extends Step {

  private static final Logger logger = CentralFactoryLogging.getLogger(Service.class);

  private InputConnections input;
  private String[] targetIds;
  private String[] targetPaths;


  public StepAssign(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepAssign( this );
  }
  
  @Override
  public void parseXML(Element e) throws XPRC_InvalidPackageNameException {
    parseId(e);
    parseUnknownMetaTags(e, new ArrayList<String>());

    List<Element> copies = XMLUtils.getChildElementsByName(e, GenerationBase.EL.COPY);
    input = new InputConnections(copies.size());
    targetIds = new String[input.length()];
    targetPaths = new String[input.length()];
    for (int i = 0; i < input.length(); i++) {
      // source element
      Element source = XMLUtils.getChildElementByName(copies.get(i), GenerationBase.EL.SOURCE);
      input.parseSourceElement(source, i);

      // target element
      Element target = XMLUtils.getChildElementByName(copies.get(i), GenerationBase.EL.TARGET);
      targetIds[i] = target.getAttribute(GenerationBase.ATT.REFID);
      targetPaths[i] = target.getAttribute(GenerationBase.ATT.PATH);
    }
  }


  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {

    cb.addLine("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName(),
               "<" + getParentScope().getClassName(), "> {").addLB();

    cb.addLine("private static final long serialVersionUID = ", Step.SERIAL_VERSION_UID_NO_VARS, "L");

    cb.addLB().addLine("public " + getClassName() + "() {");
    cb.addLine("super(" + getIdx() + ")");
    cb.addLine("}").addLB();

    appendExecuteInternally(cb, importedClassesFqStrings);

    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, input.getVarIds(), input.getPaths(), cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, targetIds, targetPaths, cb, importedClassesFqStrings);

    generatedGetRefIdMethod(cb);

    // compensation
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getName(), " {").addLine("}").addLB();
    //getChildren
    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<" + getParentScope().getClassName() + ">[] getChildren(int i) {");
    cb.addLine("return null").addLine("}");
    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {").addLine("return 0");
    cb.addLine("}").addLB().addLine("}").addLB();
  }


  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getName(), " {");
    VariableIdentification[] sourceVars = new VariableIdentification[input.length()];
    VariableIdentification[] targetVars = new VariableIdentification[input.length()];

    for (int i = 0; i < input.length(); i++) {
      VariableIdentification sourceVar = getParentScope().identifyVariable(input.getVarIds()[i]);
      VariableIdentification targetVar = getParentScope().identifyVariable(targetIds[i]);
      sourceVars[i] = sourceVar;
      targetVars[i] = targetVar;
    }

    for (int i = 0; i < input.length(); i++) {
      cb.add(targetVars[i].getScopeGetter(getParentScope()));
      //casten zum typ, typen müssen immer verträglich sein.
      cb.add(targetVars[i].variable.getSetter("("
                                                  + targetVars[i].variable
                                                      .getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + ") "
                                                  + sourceVars[i].getScopeGetter(getParentScope())
                                                  + sourceVars[i].variable.getGetter(input.getPaths()[i]), targetPaths[i]));
      cb.addLB();
    }
    cb.addLine("}").addLB();
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    return null;
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
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }


  @Override
  public List<Step> getChildSteps() {
    return null;
  }


  @Override
  protected void getImports(HashSet<String> imports) {
  }


  @Override
  public boolean isExecutionDetached() {
    return false;
  }


  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepAssign)) {
      return true;
    }
    StepAssign oldAssignStep = (StepAssign) oldStep;

    if (!Arrays.equals(input.getVarIds(), oldAssignStep.input.getVarIds()) || !Arrays.equals(input.getPaths(), oldAssignStep.input.getPaths())
        || !Arrays.equals(targetIds, oldAssignStep.targetIds) || !Arrays.equals(targetPaths, oldAssignStep.targetPaths)) {
      return true;
    }

    return false;
  }


  @Override
  public Set<String> getAllUsedVariableIds() {
    return createVariableIdSet(input.getVarIds(), targetIds);
  }


  public String[] getInputVarIds() {
    return input.getVarIds();
  }

  public String[] getOutputVarIds() {
    return targetIds;
  }


  @Override
  public List<AVariable> getInputVars() {
    List<AVariable> inputVars = new ArrayList<AVariable>();
    String[] inputVarIds = getInputVarIds();
    for (String varId : inputVarIds) {
      try {
        inputVars.add(getParentScope().identifyVariable(varId).getVariable());
      } catch (XPRC_InvalidVariableIdException e) {
        logger.error(e);
        throw new RuntimeException("Couldn't determine input variables for " + this, e);
      }
    }

    return inputVars;
  }


  @Override
  public List<AVariable> getOutputVars() {
    List<AVariable> outputVars = new ArrayList<AVariable>();
    String[] outputVarIds = getOutputVarIds();
    StepForeach parentSF = StepForeach.getParentStepForeachOrNull(getParentStep().getParentStep());
    for (String varId : outputVarIds) {
      //if we are in a stepForeach, it may have our output
      if(parentSF != null) {
        List<AVariable> candidates = parentSF.getOutputVarsSingle(true);
        AVariable outputVar = null;
        for(int i=0; i<candidates.size(); i++) {
          AVariable candidate = candidates.get(i);
          if(candidate.getId() != null && candidate.getId().equals(varId)) {
            outputVar = candidate;
            break;
          }
        }
        
        if(outputVar == null) {
          try {
            outputVar = getParentScope().identifyVariable(varId).getVariable();
          } catch (XPRC_InvalidVariableIdException e) {
            e.printStackTrace();
          }
        }
        outputVars.add(outputVar);
      }
      else {
        try {
          outputVars.add(getParentScope().identifyVariable(varId).getVariable());
        } catch (XPRC_InvalidVariableIdException e) {
          e.printStackTrace();
        }
      }
    }

    
    return outputVars;
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException {
    for (int i = 0; i < input.length(); i++) {
      if (input.getVarIds()[i].trim().length() == 0) {
        throw new XPRC_EmptyVariableIdException(GenerationBase.EL.ASSIGN + "[" + i + "]." + GenerationBase.EL.SOURCE);
      }
      if (targetIds[i].trim().length() == 0) {
        throw new XPRC_EmptyVariableIdException(GenerationBase.EL.ASSIGN + "[" + i + "]." + GenerationBase.EL.TARGET);
      }
    }
  }

  public void createEmpty() {
    setXmlId(creator.getNextXmlId());
    input = new InputConnections(0);
    targetIds = new String[]{};
  }

  // TODO: remove when h5devel-branch is merged into trunk
  @Deprecated
  public void replaceOutputVars(List<AVariable> outputVars) {
    targetIds = new String[outputVars.size()];
    input = new InputConnections(outputVars.size());
    for( int i=0; i<outputVars.size(); ++i ) {
      targetIds[i] = outputVars.get(i).getId();
//      sourceIds[i] = "S"+outputVars.get(i).getId();
    }
  }

  public void replaceVars(InputConnections input, String[] outputVarIds) {
    this.input = input; 
    targetIds = outputVarIds;
    targetPaths = new String[outputVarIds.length];
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.ASSIGN); {
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
      for (int inputNr = 0; inputNr < input.getVarIds().length; inputNr++) {
        String id = input.getVarIds()[inputNr];
        appendSource(xml, id);
      }
      
      // <Target>
      for (int i = 0; i < targetIds.length; i++) {
        //if (input.getVarIds()[i] != null) { // only write target id when a source is assigned //unless TypeChoice
          appendTarget(xml, targetIds[i], false);
        //}
      }
      
      // <Copy>
      for (int i = 0; i < input.length(); i++) {
        //if (input.getVarIds()[i] != null) { // only write <Copy>-tag when a source is assigned //unless TypeChoice
          xml.startElement(EL.COPY); {
            if(input.getVarIds()[i] != null && input.getVarIds()[i].length() > 0) {
              String id = input.getVarIds()[i];
              appendSource(xml, id, input.getUserConnected()[i], input.getConstantConnected()[i], false, input.getUnknownMetaTags().get(i));             
            }
            else { //empty Source
              xml.startElement(EL.SOURCE);
              xml.endElement(EL.SOURCE);
            }
            appendTarget(xml, targetIds[i], false);
          } xml.endElement(EL.COPY);
        //}
      }
    } xml.endElement(EL.ASSIGN);
  }
  
  public InputConnections getInputConnections() {
    return input;
  }

}

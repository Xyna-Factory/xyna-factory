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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.Configuration;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DocumentOrder;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DocumentOrderType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.FinalizationDocumentOrder;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.LabelInformation;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.WorkFinalizer;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.WorkUnit;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.XMLElementType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.XMOMObjectRefactoringResult;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.MapTree;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.PositionDecider;


public class MoveOperationWork extends BaseWorkCollection<OperationRefactoringElement>{
  
  protected final Long revision;
  
  //performance variables
  protected final Set<String> oldFqServiceGroupNames;
  protected final Set<String> newFqXmlNames;
  protected final Map<String, OperationRefactoringElement> oldFqOperationNames;
  protected final Map<String, OperationRefactoringElement> newFqServiceGroupNames;
  protected final Map<String, OperationRefactoringElement> oldOperationNames;
  
  
  protected MoveOperationWork(Collection<OperationRefactoringElement> refactorings, Configuration config, Long revision) {
    super(refactorings, config);
    this.revision = revision;
    
    oldFqServiceGroupNames = new HashSet<String>();
    newFqXmlNames = new HashSet<String>();
    oldFqOperationNames = new HashMap<String, OperationRefactoringElement>();
    newFqServiceGroupNames = new HashMap<String, OperationRefactoringElement>();
    oldOperationNames = new HashMap<String, OperationRefactoringElement>();
    
    for (OperationRefactoringElement ore : refactorings) {
      oldFqServiceGroupNames.add(ore.fqXmlNameOld + "." + ore.serviceGroupNameOld);
      newFqXmlNames.add(ore.fqXmlNameNew);
      newFqServiceGroupNames.put(ore.fqXmlNameNew + "." + ore.serviceGroupNameNew, ore);
      oldFqOperationNames.put(ore.fqXmlNameOld + "." + ore.serviceGroupNameOld + "." + ore.operationNameOld, ore);
      oldOperationNames.put(ore.operationNameOld, ore);
    }
    AddOrRemoveOperationFromDatatype aorofd = new AddOrRemoveOperationFromDatatype();
    workUnits.add(aorofd);
    workUnits.add(new RewriteOperationInvocations());
    workUnits.add(new RefactorXFLOperationInvocations());
    finalizers.add(aorofd);
  }


  protected class AddOrRemoveOperationFromDatatype extends WorkUnit implements WorkFinalizer {

    // fqServiceGroupNew -> Pair.of serviceElementToAppendTo & List.of Pair.of newOperationName & operationElementToRemove
    private Map<String, Pair<Element, List<Pair<OperationRefactoringElement, Element>>>> serviceElementsToAppendTo;
    
    protected AddOrRemoveOperationFromDatatype() {
      super(RefactoringTargetType.DATATYPE);
      serviceElementsToAppendTo = new HashMap<String, Pair<Element, List<Pair<OperationRefactoringElement, Element>>>>();
    }

    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      if (oldFqXmlNames.containsKey(fqXmlName)) {
        List<Element> serviceElements = XMLUtils.getChildElementsByName(typeInformationCarrier, GenerationBase.EL.SERVICE);
        for (Element serviceElement : serviceElements) {
          String serviceName = serviceElement.getAttribute(GenerationBase.ATT.TYPENAME);
          if (oldFqServiceGroupNames.contains(fqXmlName + "." + serviceName)) {
            List<Element> operationElements =
                XMLUtils.getChildElementsByName(serviceElement, GenerationBase.EL.OPERATION);
            XMOMObjectRefactoringResult result = null;
            for (Element operationElement : operationElements) {
              String operationName = operationElement.getAttribute(GenerationBase.ATT.OPERATION_NAME);
              String operationFqName = fqXmlName + "." + serviceName + "." + operationName;
              OperationRefactoringElement ore = oldFqOperationNames.get(operationFqName);
              if (ore != null) {
                if (ore.operationMove) {
                  storeOperationToRemove(oldFqOperationNames.get(operationFqName), operationElement);
                } else if (ore.operationRename) {
                  result = new XMOMObjectRefactoringResult(fqXmlName, type);
                  
                  if (config.deactivateRefactoredOperationCodeSnippets()) {
                    Element sourceCode = XMLUtils.getChildElementByName(operationElement, GenerationBase.EL.SOURCECODE);
                    if (sourceCode != null) {
                      Element codeSnippet = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.CODESNIPPET);
                      if (codeSnippet != null) {
                        codeSnippet.setAttribute(GenerationBase.ATT.CODESNIPPET_ACTIVE, Boolean.FALSE.toString());
                        result.deactivatedOperationSnippets.add(ore.fqOperationNameNew);
                      }
                    }
                  }
                  
                  LabelInformation info = XMLRefactoringUtils.refactorLabel(operationElement, ore.labelOld, ore.labelNew, XMLElementType.OPERATION);
                  if (info != null) {
                    result.unmodifiedLabels.add(info);
                  }
                  operationElement.setAttribute(GenerationBase.ATT.OPERATION_NAME, ore.operationNameNew);
                }
              }
            }
            if (result != null) {
              return new DocumentOrder(DocumentOrderType.SAVE, result);
            }
          }
        }
      }

      //falls newtype => hinzufügen
      if (newFqXmlNames.contains(fqXmlName)) {
        List<Element> serviceElements = XMLUtils.getChildElementsByName(typeInformationCarrier, GenerationBase.EL.SERVICE);
        for (Element serviceElement : serviceElements) {
          String serviceName = serviceElement.getAttribute(GenerationBase.ATT.TYPENAME);
          String fqServiceGroupName = fqXmlName + "." + serviceName;
          if (newFqServiceGroupNames.containsKey(fqServiceGroupName)) {
            storeServiceGroupToAppend(newFqServiceGroupNames.get(fqServiceGroupName), serviceElement);
          }
        }
      }
      return DocumentOrder.getNothing();
    }
    
    
    private void storeOperationToRemove(OperationRefactoringElement ore, Element element) {
      String fqServiceGroupNameNew = ore.fqXmlNameNew + "." + ore.serviceGroupNameNew;
      Pair<Element, List<Pair<OperationRefactoringElement, Element>>> serviceGroupOperationsPair = serviceElementsToAppendTo.get(fqServiceGroupNameNew);
      if (serviceGroupOperationsPair == null) {
        serviceGroupOperationsPair = Pair.<Element, List<Pair<OperationRefactoringElement, Element>>>of(null, new ArrayList<Pair<OperationRefactoringElement, Element>>());
        serviceElementsToAppendTo.put(fqServiceGroupNameNew, serviceGroupOperationsPair);
      }
      serviceGroupOperationsPair.getSecond().add(Pair.of(ore, element));
    }
    
    
    private void storeServiceGroupToAppend(OperationRefactoringElement ore, Element element) {
      String fqServiceGroupNameNew = ore.fqXmlNameNew + "." + ore.serviceGroupNameNew;
      Pair<Element, List<Pair<OperationRefactoringElement, Element>>> serviceGroupOperationsPair = serviceElementsToAppendTo.get(fqServiceGroupNameNew);
      if (serviceGroupOperationsPair == null) {
        serviceGroupOperationsPair = Pair.<Element, List<Pair<OperationRefactoringElement, Element>>>of(element, new ArrayList<Pair<OperationRefactoringElement, Element>>());
        serviceElementsToAppendTo.put(fqServiceGroupNameNew, serviceGroupOperationsPair);
      } else {
        serviceGroupOperationsPair.setFirst(element);
      }
    }
    

    public List<FinalizationDocumentOrder> finalizeWork() {
      for (Entry<String, Pair<Element, List<Pair<OperationRefactoringElement, Element>>>> serviceElementToAppendToEntry : serviceElementsToAppendTo.entrySet()) {
        String fqServiceName = serviceElementToAppendToEntry.getKey();
        fqServiceName = fqServiceName.substring(0, fqServiceName.lastIndexOf('.'));
        Pair<Element, List<Pair<OperationRefactoringElement, Element>>> serviceElementToAppendTo = serviceElementToAppendToEntry.getValue();
        if (serviceElementToAppendTo.getFirst() == null || serviceElementToAppendTo.getSecond().size() <= 0) {
          RefactoringManagement.logger.debug("AddOrRemoveOperationFromDatype was unable to find all required elements");
        } else {
          Document newDocument = serviceElementToAppendTo.getFirst().getOwnerDocument();
          for (Pair<OperationRefactoringElement, Element> operationElementToRemove : serviceElementToAppendTo.getSecond()) {
            Document oldDocument = operationElementToRemove.getSecond().getOwnerDocument();
            Node orphanNode = operationElementToRemove.getSecond().getParentNode().removeChild(operationElementToRemove.getSecond());
            Element adoptedNode = (Element) newDocument.importNode(orphanNode, true);
            serviceElementToAppendTo.getFirst().appendChild(adoptedNode);
            adoptedNode.setAttribute(GenerationBase.ATT.OPERATION_NAME, operationElementToRemove.getFirst().operationNameNew);
            XMOMObjectRefactoringResult xmomResult = new XMOMObjectRefactoringResult(operationElementToRemove.getFirst().fqXmlNameNew, RefactoringTargetType.DATATYPE);
            LabelInformation info = XMLRefactoringUtils.refactorLabel(adoptedNode,
                                                           operationElementToRemove.getFirst().labelNew,
                                                           operationElementToRemove.getFirst().labelOld, XMLElementType.OPERATION);
            if (info != null) {
              xmomResult.unmodifiedLabels.add(info);
            }
            if (config.deactivateRefactoredOperationCodeSnippets()) {
              Element sourceCode = XMLUtils.getChildElementByName(adoptedNode, GenerationBase.EL.SOURCECODE);
              if (sourceCode != null) {
                Element codeSnippet = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.CODESNIPPET);
                if (codeSnippet != null) {
                  codeSnippet.setAttribute(GenerationBase.ATT.CODESNIPPET_ACTIVE, Boolean.FALSE.toString());
                  xmomResult.deactivatedOperationSnippets.add(operationElementToRemove.getFirst().fqOperationNameNew);
                }
              }
            }
            File newFile = config.getFileLocation(operationElementToRemove.getFirst().fqXmlNameNew, revision);
            File oldFile = config.getFileLocation(operationElementToRemove.getFirst().fqXmlNameOld, revision);
            List<FinalizationDocumentOrder> finals = new ArrayList<XMLRefactoringUtils.FinalizationDocumentOrder>();
            finals.add(new FinalizationDocumentOrder(oldFile, oldDocument, DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(operationElementToRemove.getFirst().fqXmlNameOld, RefactoringTargetType.DATATYPE)));
            finals.add(new FinalizationDocumentOrder(newFile, newDocument, DocumentOrderType.SAVE, xmomResult));
            return finals;
          }
        }
      }
      return Collections.singletonList(FinalizationDocumentOrder.getNothing());
    }
    
  }
  
  
  //referenzen fixen: service einfügen falls noch nicht vorhanden und dann funktionsaufruf auf neues serviceobjekt zeigen lassen.
  protected class RewriteOperationInvocations extends WorkUnit {

    protected RewriteOperationInvocations() {
      super(RefactoringTargetType.WORKFLOW);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      List<Element> functionElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.FUNCTION);
      List<Element> servicerefElements =
          XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.SERVICEREFERENCE);
      
      boolean changed = false;

      // newFqServiceGroupNames -> Pair.of (Pair.of newServiceGroupElement& nldServiceGroupElement) & List.of operationsToMoveInto 
      Map<String,Pair<Pair<Element, Element>,List<Element>>> operationsToMove = new HashMap<String,Pair<Pair<Element, Element>,List<Element>>>();
      for (Element servicerefElement : servicerefElements) {
        String referencePath = servicerefElement.getAttribute(GenerationBase.ATT.REFERENCEPATH);
        String referenceName = servicerefElement.getAttribute(GenerationBase.ATT.REFERENCENAME);
        String fqServiceGroupName = referencePath + "." + referenceName;
        if (oldFqServiceGroupNames.contains(fqServiceGroupName)) {
          // checken, wie oft benutzt. falls nur mit der gesuchten operation benutzt, kann man ihn ändern.
          // falls gesuchte operation UND andere, dann so lassen und neue service-ref einfügen.
          // falls nur andere operations => nichts machen.
          String serviceId = servicerefElement.getAttribute(GenerationBase.ATT.ID);
          // nun erstmal alle operations bestimmen, die von den servicerefs aufgerufen werden
          for (Element functionElement : functionElements) {
            Element invokeElement = XMLUtils.getChildElementByName(functionElement, GenerationBase.EL.INVOKE);
            String refId = invokeElement.getAttribute(GenerationBase.ATT.SERVICEID);
            if (refId != null && refId.equals(serviceId)) {
              String operationAttributeValue = invokeElement.getAttribute(GenerationBase.ATT.INVOKE_OPERATION);
              String fqOperationName = fqServiceGroupName + "." + operationAttributeValue;
              if (oldFqOperationNames.containsKey(fqOperationName)) {
                OperationRefactoringElement ore = oldFqOperationNames.get(fqOperationName);
                storeServiceGroupToRemoveFrom(ore, servicerefElement, operationsToMove);
                if (ore.operationRename) {
                  functionElement.setAttribute(GenerationBase.ATT.INVOKE_OPERATION, ore.operationNameNew);
                  invokeElement.setAttribute(GenerationBase.ATT.INVOKE_OPERATION, ore.operationNameNew);
                  if(functionElement.getAttribute(GenerationBase.ATT.LABEL).equals(ore.labelOld)) {
                    functionElement.setAttribute(GenerationBase.ATT.LABEL, ore.labelNew);
                  }
                  changed = true;
                }
                if (ore.operationMove) {
                  storeOperationToMove(ore, invokeElement, operationsToMove);
                }
              }
            }
          }
        }
        if (newFqServiceGroupNames.containsKey(fqServiceGroupName)) {
          OperationRefactoringElement ore = newFqServiceGroupNames.get(fqServiceGroupName);
          storeServiceGroupToInsert(ore, servicerefElement, operationsToMove);
        }
      }
      
      if (operationsToMove.size() > 0) {
        changed = true;
      }
      
      Element serviceReferenceHolder = XMLUtils.getChildElementByName(typeInformationCarrier, GenerationBase.EL.OPERATION);
      
      // we no have our mapping, let's check for serviceGroupCreations
      for (Entry<String,Pair<Pair<Element,Element>,List<Element>>> entry : operationsToMove.entrySet()) {
        if (entry.getValue().getFirst().getFirst() == null) { // need to create it
          String newServiceId = "" + (getHighestIdInWF(root) + 1);
          Element newServicerefElement = doc.createElement(GenerationBase.EL.SERVICEREFERENCE);
          XMLUtils.insertChild(serviceReferenceHolder, newServicerefElement, new PositionDecider() {
            
            public boolean decideInsertionBetweenSiblings(Element predecessor, Element successor) {
              if (successor.getTagName().equals(GenerationBase.EL.INPUT) || 
                  successor.getTagName().equals(GenerationBase.EL.OUTPUT) ||
                  successor.getTagName().equals(GenerationBase.EL.SERVICEREFERENCE)) {
                return false;
              } else {
                return true;
              }
            }
          });
          //serviceReferenceHolder.appendChild(newServicerefElement);
          newServicerefElement.setAttribute(GenerationBase.ATT.ID, newServiceId);
          String[] nameParts = RefactoringManagement.splitOperationFqName(entry.getKey());
          newServicerefElement.setAttribute(GenerationBase.ATT.LABEL, nameParts[1]);
          newServicerefElement.setAttribute(GenerationBase.ATT.REFERENCENAME, nameParts[1] + "." + nameParts[2]);
          newServicerefElement.setAttribute(GenerationBase.ATT.REFERENCEPATH, nameParts[0]);
          entry.getValue().getFirst().setFirst(newServicerefElement);
        }
      }
      
      // refactor invocations
      for (Pair<Pair<Element,Element>,List<Element>> entry : operationsToMove.values()) {
        Element newServiceReference = entry.getFirst().getFirst();
        Element oldServiceReference = entry.getFirst().getSecond();
        for (Element invokeElement : entry.getSecond()) {
          String newServiceId = newServiceReference.getAttribute(GenerationBase.ATT.ID);
          String oldServiceId = invokeElement.getAttribute(GenerationBase.ATT.SERVICEID);
          invokeElement.setAttribute(GenerationBase.ATT.SERVICEID, newServiceId);
          Element functionElement = (Element) invokeElement.getParentNode();

          Element receiveElement = XMLUtils.getChildElementByName(functionElement, GenerationBase.EL.RECEIVE);
          receiveElement.setAttribute(GenerationBase.ATT.SERVICEID, newServiceId);
          // sourcetarget von function verarzten...
          List<Element> sourceElements = XMLUtils.getChildElementsByName(functionElement, GenerationBase.EL.SOURCE);
          List<Element> targetElements = XMLUtils.getChildElementsByName(functionElement, GenerationBase.EL.TARGET);
          sourceElements.addAll(targetElements);
          for (Element sourceTargetElement : sourceElements) {
            String sourceTargetRefId = sourceTargetElement.getAttribute(GenerationBase.ATT.REFID);
            if (sourceTargetRefId != null && sourceTargetRefId.equals(oldServiceId)) {
              sourceTargetElement.setAttribute(GenerationBase.ATT.REFID, newServiceId);
            }
          }
          // sourcetarget von serviceref verarzten...
          String functionId = functionElement.getAttribute(GenerationBase.ATT.ID);
          sourceElements = XMLUtils.getChildElementsByName(oldServiceReference, GenerationBase.EL.SOURCE);
          targetElements = XMLUtils.getChildElementsByName(oldServiceReference, GenerationBase.EL.TARGET);
          sourceElements.addAll(targetElements);
          for (Element sourceTargetElement : sourceElements) {
            String sourceTargetRefId = sourceTargetElement.getAttribute(GenerationBase.ATT.REFID);
            if (sourceTargetRefId != null && sourceTargetRefId.equals(functionId)) {
              final Element e = (Element) oldServiceReference.removeChild(sourceTargetElement);
              XMLUtils.insertChild(newServiceReference, e, new PositionDecider() {

                public boolean decideInsertionBetweenSiblings(Element predecessor, Element successor) {
                  // alphabetisch sortiert
                  if (successor == null) {
                    return true;
                  }
                  String successorName = successor.getTagName();
                  String name = e.getTagName();
                  if (predecessor == null) {
                    return name.compareTo(successorName) <= 0;
                  }
                  String predecessorName = predecessor.getTagName();
                  return name.compareTo(successorName) <= 0 && predecessorName.compareTo(name) <= 0;
                }

              });
            }
          }
        }
      }
      
      // check if some oldServiceRefs can now be removed
      for (Pair<Pair<Element,Element>,List<Element>> entry : operationsToMove.values()) {
        Element oldServiceRef = entry.getFirst().getSecond();
        if (oldServiceRef != null && XMLUtils.getChildElementsByName(oldServiceRef, GenerationBase.EL.SOURCE).size() <= 0) {
          oldServiceRef.getParentNode().removeChild(oldServiceRef);
        }
      }

      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
      } else {
        return DocumentOrder.getNothing();
      }
    }
  
    
    private void storeOperationToMove(OperationRefactoringElement ore, Element element, Map<String,Pair<Pair<Element, Element>,List<Element>>> operationsToMove) {
      String fqServiceGroupNameNew = ore.fqXmlNameNew + "." + ore.serviceGroupNameNew;
      Pair<Pair<Element, Element>,List<Element>> serviceGroupOperationsPair = operationsToMove.get(fqServiceGroupNameNew);
      if (serviceGroupOperationsPair == null) {
        serviceGroupOperationsPair = Pair.<Pair<Element, Element>,List<Element>>of(Pair.<Element, Element>of(null, null), new ArrayList<Element>());
        operationsToMove.put(fqServiceGroupNameNew, serviceGroupOperationsPair);
      }
      serviceGroupOperationsPair.getSecond().add(element);
    }
    
    
    private void storeServiceGroupToInsert(OperationRefactoringElement ore, Element element, Map<String,Pair<Pair<Element, Element>,List<Element>>> operationsToMove) {
      String fqServiceGroupNameNew = ore.fqXmlNameNew + "." + ore.serviceGroupNameNew;
      Pair<Pair<Element, Element>,List<Element>> serviceGroupOperationsPair = operationsToMove.get(fqServiceGroupNameNew);
      if (serviceGroupOperationsPair == null) {
        serviceGroupOperationsPair = Pair.<Pair<Element, Element>,List<Element>>of(Pair.<Element, Element>of(element, null), new ArrayList<Element>());
        operationsToMove.put(fqServiceGroupNameNew, serviceGroupOperationsPair);
      } else {
        serviceGroupOperationsPair.getFirst().setFirst(element);
      }
    }
    
    
    private void storeServiceGroupToRemoveFrom(OperationRefactoringElement ore, Element element, Map<String,Pair<Pair<Element, Element>,List<Element>>> operationsToMove) {
      String fqServiceGroupNameNew = ore.fqXmlNameNew + "." + ore.serviceGroupNameNew;
      Pair<Pair<Element, Element>,List<Element>> serviceGroupOperationsPair = operationsToMove.get(fqServiceGroupNameNew);
      if (serviceGroupOperationsPair == null) {
        serviceGroupOperationsPair = Pair.<Pair<Element, Element>,List<Element>>of(Pair.<Element, Element>of(null, element), new ArrayList<Element>());
        operationsToMove.put(fqServiceGroupNameNew, serviceGroupOperationsPair);
      } else {
        serviceGroupOperationsPair.getFirst().setSecond(element);
      }
    }
    
    
    private int getHighestIdInWF(Element e) {
      List<Element> children = XMLUtils.getChildElements(e);
      int i = 0;
      for (Element child : children) {
        String id = child.getAttribute(GenerationBase.ATT.ID);
        if (id != null && id.length() > 0) {
          int val = Integer.valueOf(id);
          i = Math.max(i, val);
        }
        i = Math.max(i, getHighestIdInWF(child));
      }
      return i;
    }
    
  }
  
  
  private final static Pattern INSTANCE_INVOCATION_PATTERN = Pattern.compile("(?<=\\.)[a-z][a-zA-Z0-9_]+(?=\\()"); 
  
  protected class RefactorXFLOperationInvocations extends WorkUnit {
    
    
    public RefactorXFLOperationInvocations() {
      super(RefactoringTargetType.WORKFLOW);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      List<Element> relevantChoices = new ArrayList<Element>();
      List<Element> choiceElements = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.CHOICE);
      choiceLoop: for (Element choiceElement : choiceElements) {
        String fqChoiceClassName;
        try {
          fqChoiceClassName = GenerationBase.transformNameForJava(choiceElement.getAttribute(GenerationBase.ATT.TYPEPATH),
                                                                  choiceElement.getAttribute(GenerationBase.ATT.TYPENAME));
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        }
        if (fqChoiceClassName.equals(StepChoice.BASECHOICE_FORMULA)) {
          List<Element> caseElements  = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.CASE);
          for (Element caseElement : caseElements) {
            String premise = caseElement.getAttribute(GenerationBase.ATT.CASECOMPLEXNAME);
            for (OperationRefactoringElement ore : refactorings) {
              if (premise.contains("." + ore.operationNameOld + "(")) {
                relevantChoices.add(choiceElement);
                continue choiceLoop;
              }
            }
          }
        }
      }
      
      List<Element> relevantMappingss = new ArrayList<Element>();
      List<Element> mappingsElements = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.MAPPINGS);
      mappingLoop: for (Element mappings : mappingsElements) {
        List<Element> mappingElements = XMLUtils.getChildElementsRecursively(mappings, GenerationBase.EL.MAPPING);
        for (Element mapping : mappingElements) {
          String mappingFormula = XMLUtils.getTextContent(mapping);
          for (OperationRefactoringElement ore : refactorings) {
            if (mappingFormula.contains("." + ore.operationNameOld + "(")) {
              relevantMappingss.add(mappings);
              continue mappingLoop;
            }
          }
        }
      }
      
      if (relevantChoices.size() > 0 || relevantMappingss.size() > 0) {
        WF wf;
        try {
          wf = WF.getOrCreateInstance(fqXmlName, new GenerationBaseCache(), revision);
          wf.parseGeneration(config.refactorInDeploymentDir, false);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        } catch (XPRC_InheritedConcurrentDeploymentException e) {
          throw new RuntimeException(e);
        } catch (AssumedDeadlockException e) {
          throw new RuntimeException(e);
        } catch (XPRC_MDMDeploymentException e) {
          throw new RuntimeException(e);
        }
        ScopeStep step = wf.getWfAsStep();
        Set<Step> allSteps =new HashSet<Step>();
        WF.addChildStepsRecursively(allSteps, step);
        
        boolean changes = false;
        if (relevantChoices.size() > 0 ) {
          for (Element relevantChoice : relevantChoices) {
            Element outerConditionPart = null;
            Element metaElement = XMLUtils.getChildElementByName(relevantChoice, GenerationBase.EL.META);
            if (metaElement != null) {
              outerConditionPart = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.CONDITIONAL_BRANCHING_OUTER_CONDITION_ELEMENT);
            }
            int choiceId = Integer.parseInt(relevantChoice.getAttribute(GenerationBase.ATT.ID));
            for (Step childStep : allSteps) {
              if (childStep.getXmlId() != null &&
                  childStep.getXmlId() == choiceId) {
                StepChoice choiceStep = (StepChoice) childStep;
                List<ModelledExpression> formulas = choiceStep.getParsedFormulas();
                if (formulas != null && formulas.size() > 0) {
                  List<Element> cases = XMLUtils.getChildElementsByName(relevantChoice, GenerationBase.EL.CASE);
                  for (int i = 0; i < formulas.size(); i++) {
                    try {
                      formulas.get(i).initTypesOfParsedFormula(wf.getImports(), new MapTree());
                    } catch (XPRC_InvalidVariableIdException e) {
                      throw new RuntimeException(e);
                    } catch (XPRC_InvalidVariableMemberNameException e) {
                      throw new RuntimeException(e);
                    } catch (XPRC_ParsingModelledExpressionException e) {
                      throw new RuntimeException(e);
                    } catch (XPRC_OperationUnknownException e) {
                      throw new RuntimeException(e);
                    } catch (XPRC_InvalidServiceIdException e) {
                      throw new RuntimeException(e);
                    }
                    if (refactorChoiceFormula(formulas.get(i), cases.get(i))) {
                      if (outerConditionPart != null) {
                        String outerFormula = XMLUtils.getTextContent(outerConditionPart);
                        String refactoredFormula = cases.get(i).getAttribute(GenerationBase.ATT.CASECOMPLEXNAME);
                        if (!refactoredFormula.startsWith(outerFormula.substring(0, outerFormula.length() - 1))) {
                          Matcher outerFormulaMatcher = INSTANCE_INVOCATION_PATTERN.matcher(outerFormula);
                          Matcher refactoredFormulaMatcher = INSTANCE_INVOCATION_PATTERN.matcher(refactoredFormula);
                          StringBuffer refactoredOuterCondition = new StringBuffer();
                          while (outerFormulaMatcher.find()) {
                            if (!refactoredFormulaMatcher.find()) {
                              RefactoringManagement.logger.warn("Malformed outer condition for conditional branch in " + fqXmlName);
                              return DocumentOrder.getNothing();
                            }
                            outerFormulaMatcher.appendReplacement(refactoredOuterCondition, refactoredFormulaMatcher.group());
                          }
                          outerFormulaMatcher.appendTail(refactoredOuterCondition);
                          XMLUtils.setTextContent(outerConditionPart, refactoredOuterCondition.toString());
                        }
                      }
                      changes = true;
                    }
                  }
                }
              }
            }
          }
        }
        for (Element relevantMappings : relevantMappingss) {
          int mappingsId = Integer.parseInt(relevantMappings.getAttribute(GenerationBase.ATT.ID));
          for (Step childStep : allSteps) {
            if (childStep.getXmlId() != null &&
                childStep.getXmlId() == mappingsId) {
              StepMapping mappingsStep = (StepMapping) childStep;
              List<ModelledExpression> formulas = mappingsStep.getParsedExpressions();
              if (formulas != null && formulas.size() > 0) {
                List<Element> cases = XMLUtils.getChildElementsByName(relevantMappings, GenerationBase.EL.MAPPING);
                for (int i = 0; i < formulas.size(); i++) {
                  try {
                    formulas.get(i).initTypesOfParsedFormula(wf.getImports(), new MapTree());
                  } catch (Exception e) {
                    RefactoringManagement.logger.debug("XFL '" + formulas.get(i).getExpression() + "' in " + fqXmlName + " could not be parsed.");
                    return DocumentOrder.getNothing();
                  }
                  if (refactorMappingFormula(formulas.get(i), cases.get(i))) {
                    changes = true;
                  }
                }
              }
            }
          }
        }
        
        if (changes) {
          return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
        }
      }
      return DocumentOrder.getNothing();
    }
    
    

    private boolean refactorChoiceFormula(ModelledExpression me, Element choice) {
      String previousXFL = choice.getAttribute(GenerationBase.ATT.CASECOMPLEXNAME);
      String newFormula = refactorFormula(me);
      if (!previousXFL.equals(newFormula)) {
        choice.setAttribute(GenerationBase.ATT.CASECOMPLEXNAME, newFormula);
        return true;
      } else {
        return false;
      }
    }
    
    private boolean refactorMappingFormula(ModelledExpression me, Element mapping) {
      String previousXFL = XMLUtils.getTextContent(mapping);
      String newFormula = refactorFormula(me);
      if (!previousXFL.equals(newFormula)) {
        XMLUtils.setTextContent(mapping, newFormula);
        return true;
      } else {
        return false;
      }
    }
    
    private String refactorFormula(ModelledExpression me) {
      OperationRefactoringVisitor orv = new OperationRefactoringVisitor();
      me.visitTargetExpression(orv);
      String newExpression = orv.getXFLExpression();
      // XFL-Identitity generation appears to add an additional pair of braces, this is problematic for conditional branches, we'll remove them
      if (newExpression.startsWith("(") && newExpression.endsWith(")")) {
        newExpression = newExpression.substring(1, newExpression.length() - 1);
      }
      if (me.getFoundAssign() != null) {
        orv = new OperationRefactoringVisitor();
        me.visitSourceExpression(orv);
        String newSourceExpression = orv.getXFLExpression();
        if (newSourceExpression.startsWith("(") && newSourceExpression.endsWith(")")) {
          newSourceExpression = newSourceExpression.substring(1, newSourceExpression.length() - 1);
        }
        newExpression = newExpression + me.getFoundAssign().toXFL() + newSourceExpression;
      }
      return newExpression;
    }
    
    
    private class OperationRefactoringVisitor extends IdentityCreationVisitor {
      
      private Stack<Variable> currentVariable = new Stack<Variable>();
      
      @Override
      public void variableStarts(Variable variable) {
        currentVariable.push(variable);
        super.variableStarts(variable);
      }
      
      
      @Override
      public void allPartsOfVariableFinished(Variable variable) {
        currentVariable.pop();
        super.variableEnds(variable);
      }
      
      @Override
      public void variablePartStarts(VariableAccessPart part) {
        if (oldOperationNames.containsKey(part.getName())) {
          Variable var = currentVariable.peek();
          VariableInfo invocationLocationInformation;
          int index = var.getParts().indexOf(part);
          if (index == 0) {
            invocationLocationInformation = var.getBaseVariable();
          } else {
            try {
              invocationLocationInformation = var.follow(index - 1);
            } catch (XPRC_InvalidVariableMemberNameException e) {
              throw new RuntimeException(e);
            }   
          }
          TypeInfo type = invocationLocationInformation.getTypeInfo(true);
          String fqJavaName = type.getJavaName();
          for (OperationRefactoringElement ore : refactorings) {
            try {
              if (ore.operationNameOld.equals(part.getName()) &&
                  GenerationBase.transformNameForJava(ore.fqXmlNameOld).equals(fqJavaName)) {
                sb.append(".").append(ore.operationNameNew);
                if (part.isMemberVariableAccess() && part.getIndexDef() != null) {
                  appendIndexDefStart();
                }
                return;
              }
            } catch (XPRC_InvalidPackageNameException e) {
              throw new RuntimeException(e);
            }
          }
        }
        super.variablePartStarts(part);
      }
    }
  }
  

}

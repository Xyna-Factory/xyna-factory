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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.Configuration;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DocumentOrder;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DocumentOrderType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.LabelInformation;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.WorkFinalizer;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.WorkUnit;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.XMLElementType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.XMOMObjectRefactoringResult;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public abstract class BaseWorkCollection<E extends RefactoringElement> {

  private final static XPathFactory factory = XPathFactory.newInstance();
  
  protected final List<WorkUnit> workUnits;
  protected final List<WorkFinalizer> finalizers;
  
  protected final Configuration config;
  protected final Collection<E> refactorings;
  // performance variables
  protected final Map<String, E> oldFqXmlNames;
  protected final Map<String, E> oldFqJavaNames;
  
  protected BaseWorkCollection(Collection<E> refactorings, Configuration config) {
    this.workUnits = new ArrayList<WorkUnit>();
    this.finalizers= new ArrayList<WorkFinalizer>(); 
    this.refactorings = refactorings;
    this.config = config;
    this.oldFqXmlNames = new HashMap<String, E>();
    this.oldFqJavaNames = new HashMap<String, E>();
    try {
      for (E refactoringElement : refactorings) {
        this.oldFqXmlNames.put(refactoringElement.fqXmlNameOld, refactoringElement);
        this.oldFqJavaNames.put(GenerationBase.transformNameForJava(refactoringElement.fqXmlNameOld), refactoringElement);
      }
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public List<WorkUnit> getWorkUnits() {
    return workUnits;
  }
  
  
  public List<WorkFinalizer> getWorkFinalizers() {
    return finalizers;
  }
  
  
  //additionaldependencies fixen
  protected class RefactorAdditionalDependencies extends WorkUnit {

    protected RefactorAdditionalDependencies() {
      super(RefactoringTargetType.DATATYPE);
    }

    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      List<Element> additionalDeps = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.ADDITIONALDEPENDENCIES);
      for (Element additionalDep : additionalDeps) {
        List<Element> dataTypeDeps = XMLUtils.getChildElementsByName(additionalDep, GenerationBase.EL.DEPENDENCY_DATATYPE);
        for (Element dataTypeDep : dataTypeDeps) {
          String content = XMLUtils.getTextContent(dataTypeDep).trim();
          if (oldFqXmlNames.containsKey(content)) {
            XMLUtils.setTextContent(dataTypeDep, oldFqXmlNames.get(content).fqXmlNameNew);
            return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type)); 
          }
        }
      }
      return DocumentOrder.getNothing();
    }
    
  }
  
  
  //filter additionaldependencies fixen
  protected class RefactorFilterAdditionalDependencies extends WorkUnit {

    protected RefactorFilterAdditionalDependencies() {
      super(RefactoringTargetType.FILTER);
    }

    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      Element additionalDeps = XMLUtils.getChildElementByName(root, GenerationBase.EL.ADDITIONALDEPENDENCIES);
      if (additionalDeps != null && type.hasRelevantAdditionalDependencies()) {
        boolean changed = false;
        AdditionalDependencyType[] relevantTypes = type.getAdditionalDependencies();
        for (AdditionalDependencyType relevantType : relevantTypes) {
          if (relevantType == AdditionalDependencyType.ORDERTYPE) {
            List<Element> orderTypeDeps = XMLUtils.getChildElementsByName(additionalDeps, AdditionalDependencyContainer.AdditionalDependencyType.ORDERTYPE.getXmlElementName());
            for (Element orderTypeDep : orderTypeDeps) {
              String content = XMLUtils.getTextContentOrNull(orderTypeDep);
              if (oldFqJavaNames.containsKey(content)) {
                try {
                  XMLUtils.setTextContent(orderTypeDep, GenerationBase.transformNameForJava(oldFqJavaNames.get(content).fqXmlNameNew));
                } catch (XPRC_InvalidPackageNameException e) {
                  throw new RuntimeException(e);
                }
                changed = true;
              }
            }
          } else {
            List<Element> workflowDeps = XMLUtils.getChildElementsByName(additionalDeps, relevantType.getXmlElementName());
            for (Element workflowDep : workflowDeps) {
              String content = XMLUtils.getTextContentOrNull(workflowDep);
              if (oldFqXmlNames.containsKey(content)) {
                XMLUtils.setTextContent(workflowDep, oldFqXmlNames.get(content).fqXmlNameNew);
                changed = true;
              }
            }
          }
        }
        if (changed) {
          //filtername wird später gesetzt, weil man ihn hier nicht weiss
          XMOMObjectRefactoringResult result = new XMOMObjectRefactoringResult(null, RefactoringTargetType.FILTER);
          return new DocumentOrder(DocumentOrderType.SAVE, result);
        }
      }
      return DocumentOrder.getNothing();
    }
    
  }
    
   
  //oberklasse fixen
  protected class RefactorBaseType extends WorkUnit {

    protected RefactorBaseType(RefactoringTargetType... relevantRootType) {
      super(relevantRootType);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      String basePath = typeInformationCarrier.getAttribute(GenerationBase.ATT.BASETYPEPATH);
      String baseName = typeInformationCarrier.getAttribute(GenerationBase.ATT.BASETYPENAME);
      if (basePath != null && basePath.length() > 0 &&
          baseName != null && baseName.length() > 0) {
        String baseFqName = basePath + "." + baseName;
        if (oldFqXmlNames.containsKey(baseFqName)) {
          typeInformationCarrier.setAttribute(GenerationBase.ATT.BASETYPENAME, oldFqXmlNames.get(baseFqName).nameNew);
          typeInformationCarrier.setAttribute(GenerationBase.ATT.BASETYPEPATH, oldFqXmlNames.get(baseFqName).packageNew);
          return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type)); 
        }
      }
      return DocumentOrder.getNothing();
    }
    
  }
  
  
  /*
   * datamodel-base in datamodel-typen
   * <DataType xmlns="http://www.gip.com/xyna/xdev/xfractmod" Label="Black" TypeName="Black" TypePath="xdnc.datamodels.snmp.xynamib.1" Version="1.7">
       <Meta>
         <DataModel>
           <ModelName>xdnc.datamodels.snmp.xynamib.1.XynaBlackMIB</ModelName>
         </DataModel>
         <IsServiceGroupOnly>false</IsServiceGroupOnly>
       </Meta>
   */
  protected class RefactorDataModelInDefinition extends WorkUnit {

    protected RefactorDataModelInDefinition() {
      super(RefactoringTargetType.DATATYPE);
    }

    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      List<Element> modelNameElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.MODELNAME);
      boolean changed = false;
      for (Element modelNameElement : modelNameElements) {
        String modelNameContent = XMLUtils.getTextContent(modelNameElement);
        if (modelNameContent != null && oldFqXmlNames.containsKey(modelNameContent)) {
          XMLUtils.setTextContent(modelNameElement, oldFqXmlNames.get(modelNameContent).fqXmlNameNew);
          changed = true;
        }
      }
      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, RefactoringTargetType.DATATYPE));
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
  }
  
  /*
   * datamodel in mappings
   *  <Mappings ID="18" Label="Create Var Bindings">
        <Target RefID="13"/>
        <Meta>
          <DataModel>
            <ModelName>xdnc.model.snmp.xynamib.v5.XynaBlackMIB</ModelName>
          </DataModel>
        </Meta>
   */
  protected class RefactorDataModelInMapping extends WorkUnit {
    
    protected RefactorDataModelInMapping() {
      super(RefactoringTargetType.WORKFLOW);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      boolean changed = false;
      List<Element> modelNameElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.MODELNAME);
      for (Element modelNameElement : modelNameElements) {
        String modelNameContent = XMLUtils.getTextContent(modelNameElement);
        if (modelNameContent != null && oldFqXmlNames.containsKey(modelNameContent)) {
          XMLUtils.setTextContent(modelNameElement, oldFqXmlNames.get(modelNameContent).fqXmlNameNew);
          changed = true;
        }
      }
      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
  }
    
  
  
  //eventuell ist das das zu verschiebende xml:
  protected class MoveRefactoringTarget extends WorkUnit {

    protected MoveRefactoringTarget(RefactoringTargetType... relevantRootType) {
      super(relevantRootType);
    }

    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      if (oldFqXmlNames.containsKey(fqXmlName)) {
        typeInformationCarrier.setAttribute(GenerationBase.ATT.TYPEPATH, oldFqXmlNames.get(fqXmlName).packageNew);
        typeInformationCarrier.setAttribute(GenerationBase.ATT.TYPENAME, oldFqXmlNames.get(fqXmlName).nameNew);
        XMOMObjectRefactoringResult result = new XMOMObjectRefactoringResult(fqXmlName, type);
        result.fqXmlNameNew = oldFqXmlNames.get(fqXmlName).fqXmlNameNew;
        typeInformationCarrier.setAttribute(GenerationBase.ATT.LABEL, oldFqXmlNames.get(fqXmlName).labelNew);
        
        if (type == RefactoringTargetType.WORKFLOW) {
          //operation umbenennen
          Element operation = XMLUtils.getChildElementByName(typeInformationCarrier, GenerationBase.EL.OPERATION);
          operation.setAttribute(GenerationBase.ATT.OPERATION_NAME, oldFqXmlNames.get(fqXmlName).nameNew);
          operation.setAttribute(GenerationBase.ATT.LABEL, oldFqXmlNames.get(fqXmlName).labelNew);
        } else if (type == RefactoringTargetType.DATATYPE) {
          List<Element> serviceGroups = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.SERVICE);
          for (Element serviceGroup : serviceGroups) {
            String previousTypeName = serviceGroup.getAttribute(GenerationBase.ATT.TYPENAME);
            if (previousTypeName.equals(oldFqXmlNames.get(fqXmlName).nameOld)) {
              serviceGroup.setAttribute(GenerationBase.ATT.TYPENAME, oldFqXmlNames.get(fqXmlName).nameNew);
              serviceGroup.setAttribute(GenerationBase.ATT.LABEL, oldFqXmlNames.get(fqXmlName).labelNew);
            }
          }
          if (config.refactorInDeploymentDir) {
            List<Element> libraryElements = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.LIBRARIES);
            for (Element libraryElement : libraryElements) {
              String libraryName = XMLUtils.getTextContent(libraryElement);
              if (libraryName!= null && libraryName.equals(oldFqXmlNames.get(fqXmlName).nameOld + "Impl.jar")) {
                libraryElement.getParentNode().removeChild(libraryElement);
              }
            }
            List<Element> codeSnippets = XMLUtils.getChildElementsByName(typeInformationCarrier, GenerationBase.EL.CODESNIPPET);
            for (Element codeSnippet : codeSnippets) {
              codeSnippet.setAttribute(GenerationBase.ATT.CODESNIPPET_ACTIVE, Boolean.FALSE.toString());
              //result.deactivatedOperationSnippets.add(""); // TODO insert with new fq operationName
            }
          } else {
            List<Element> libraryElements = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.LIBRARIES);
            for (Element libraryElement : libraryElements) {
              String libraryName = XMLUtils.getTextContent(libraryElement);
              if (libraryName!= null && libraryName.equals(oldFqXmlNames.get(fqXmlName).nameOld + "Impl.jar")) {
                XMLUtils.setTextContent(libraryElement, oldFqXmlNames.get(fqXmlName).nameNew + "Impl.jar");
              }
            }
          }
        }
        if (oldFqXmlNames.get(fqXmlName).fqXmlNameOld.equals(oldFqXmlNames.get(fqXmlName).fqXmlNameNew)) {
          return new DocumentOrder(DocumentOrderType.SAVE, result);
        } else {
          return new DocumentOrder(DocumentOrderType.MOVE, result);
        }
      }
      return DocumentOrder.getNothing();
    }
    
  }
  
  
  /* 
   * manche elemente enthalten konstant vorbelegt den fqnamen als value:
   * query selection masks:
   * <Data ID="16" Label="Selection mask" ReferenceName="SelectionMask" ReferencePath="xnwh.persistence" VariableName="const_SelectionMask">
       <Target RefID="15"/>
       <Data ID="32" Label="Root type" VariableName="rootType">
         <Meta>
           <Type>String</Type>
         </Meta>
         <Value>xucc.core.model.user.User</Value>
       </Data>
     </Data>
   */
  protected class RefactorPersistenceReferences extends WorkUnit {

    protected RefactorPersistenceReferences() {
      super(RefactoringTargetType.WORKFLOW);
    }

    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      boolean changed = false;
      List<Element> dataElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.DATA);
      for (Element dataElement : dataElements) {
        String referencePath = dataElement.getAttribute(GenerationBase.ATT.REFERENCEPATH);
        if (referencePath != null && referencePath.equals(XMLRefactoringUtils.PERSISTENCE_PATH)) {
          String referenceName = dataElement.getAttribute(GenerationBase.ATT.REFERENCENAME);
          if (referenceName != null && referenceName.equals(XMLRefactoringUtils.PERSISTENCE_SELECTIONMASK_NAME)) {
            List<Element> memberVarDataElements = XMLUtils.getChildElementsByName(dataElement, GenerationBase.EL.DATA);
            for (Element memberVarDataElement : memberVarDataElements) {
              String varName = memberVarDataElement.getAttribute(GenerationBase.ATT.VARIABLENAME);
              if (varName != null && varName.equals(XMLRefactoringUtils.PERSISTENCE_SELECTIONMASK_VARNAME)) {
                Element valueElement = XMLUtils.getChildElementByName(memberVarDataElement, GenerationBase.EL.VALUE);
                if (valueElement != null) {
                  String content = XMLUtils.getTextContent(valueElement);
                  if (oldFqXmlNames.containsKey(content)) {
                    XMLUtils.setTextContent(valueElement, oldFqXmlNames.get(content).fqXmlNameNew);
                    changed = true;
                  }
                }
              }
            }
          }
        }
      }
      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
  }
  
  
  /*
   * choice nach subtypen
   * <Choice ID="9" TypeName="BaseChoiceTypeSubclasses" TypePath="server">
   *   <Case Label="IPv4" Premise="base.IPv4" Alias="base.IP"/>
   *   <Case Label="IP" Premise="base.IP">
   */
  protected class RefactorSubtypeChoices extends WorkUnit {
    
    protected RefactorSubtypeChoices() {
      super(RefactoringTargetType.WORKFLOW);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      boolean changed = false;
      List<Element> choiceElements = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.CHOICE);
      for (Element choiceElement : choiceElements) {
        List<Element> caseElements = XMLUtils.getChildElementsByName(choiceElement, GenerationBase.EL.CASE);
        for (Element caseElement : caseElements) {
          String premise = caseElement.getAttribute(GenerationBase.ATT.CASECOMPLEXNAME);
          if (premise != null && oldFqXmlNames.containsKey(premise)) {
            caseElement.setAttribute(GenerationBase.ATT.CASECOMPLEXNAME, oldFqXmlNames.get(premise).fqXmlNameNew);
            caseElement.setAttribute(GenerationBase.ATT.LABEL, oldFqXmlNames.get(premise).labelNew);
            changed = true;
            //kann zusätzlich auch als alias definiert sein, deshalb hier kein break
          } else {
            String alias = caseElement.getAttribute(GenerationBase.ATT.CASEALIAS);
            if (alias != null && oldFqXmlNames.containsKey(alias)) {
              caseElement.setAttribute(GenerationBase.ATT.CASEALIAS, oldFqXmlNames.get(alias).fqXmlNameNew);
              changed = true;
            }
          }
        }
      }
      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
  }
  
  
  /*
   *  <Invoke Operation="WFBase" ServiceID="2">
        <Source RefID="23">
          <Meta>
            <ExpectedType>cl.synch.K1</ExpectedType>
   */
  protected class RefactorExpectedTypesInServiceCalls extends WorkUnit {
    
    protected RefactorExpectedTypesInServiceCalls() {
      super(RefactoringTargetType.WORKFLOW);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      boolean changed = false;
      List<Element> expectedTypeElements = XMLUtils.getChildElementsRecursively(typeInformationCarrier, GenerationBase.EL.EXPECTED_TYPE);
      for (Element expectedTypeElement : expectedTypeElements) {
        String content = XMLUtils.getTextContent(expectedTypeElement);
        if (oldFqXmlNames.containsKey(content)) {
          XMLUtils.setTextContent(expectedTypeElement, oldFqXmlNames.get(content).fqXmlNameNew);
          changed = true;
        }
      }
      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
  }
  
  
  /*
   * XFL: instanceof
   * wo gibt es überall XFLs?
   * - conditional choices
   * 
   *   <Case Label="true" Premise="typeof(%0%,&quot;base.IPv6&quot;)">
   *
   * - mappings
   * 
   * <Mappings ID="22" Label="Mapping">
   *   <Mapping>%2%["0"].value=typeof(%0%,"base.IPv6")</Mapping>
   * 
   * - query filtercondition NICHT! (unterstützt die funktion nicht)
   * 
   * - conditional branching
   * <Choice ID="6" TypeName="BaseChoiceTypeFormula" TypePath="server">
   *   <Meta>
   *     <OuterConditionPart>typeof(?,"xact.connection.Command")</OuterConditionPart>
   */
  protected class RefactorDatatypeXFLReferences extends WorkUnit {
    
    protected RefactorDatatypeXFLReferences() {
      super(RefactoringTargetType.WORKFLOW);
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      Element root = doc.getDocumentElement();
      boolean changed = false;
      List<Element> caseElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.CASE);
      for (Element caseElement : caseElements) {
        String premise = caseElement.getAttribute(GenerationBase.ATT.CASECOMPLEXNAME);
        // TODO can we batch this instead of iterating?
        if (premise != null) {
          for (RefactoringElement refactoring : refactorings) {
            String refactoredPremise = XMLRefactoringUtils.refactorXFLTypes(premise, root, refactoring.fqXmlNameOld, refactoring.fqXmlNameNew);
            if (!premise.equals(refactoredPremise)) {
              caseElement.setAttribute(GenerationBase.ATT.CASECOMPLEXNAME, refactoredPremise);
              changed = true;
            }
          }
        }
      }

      List<Element> mappingElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.MAPPING);
      for (Element mappingElement : mappingElements) {
        String xfl = XMLUtils.getTextContent(mappingElement);
        if (xfl != null) {
          for (RefactoringElement refactoring : refactorings) {
            String refactoredXFL = XMLRefactoringUtils.refactorXFLTypes(xfl, root, refactoring.fqXmlNameOld, refactoring.fqXmlNameNew);
            if (!xfl.equals(refactoredXFL)) {
              XMLUtils.setTextContent(mappingElement, refactoredXFL);
              changed = true;
            }
          }
        }
      }

      List<Element> outerConditionPartElements =
          XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.CONDITIONAL_BRANCHING_OUTER_CONDITION_ELEMENT);
      for (Element outerConditionPartElement : outerConditionPartElements) {
        String xfl = XMLUtils.getTextContent(outerConditionPartElement);
        if (xfl != null) {
          for (RefactoringElement refactoring : refactorings) {
            String refactoredXFL = XMLRefactoringUtils.refactorXFLTypes(xfl, root, refactoring.fqXmlNameOld, refactoring.fqXmlNameNew);
            if (!xfl.equals(refactoredXFL)) {
              XMLUtils.setTextContent(outerConditionPartElement, refactoredXFL);
              changed = true;
            }
          }
        }
      }
      if (changed) {
        return new DocumentOrder(DocumentOrderType.SAVE, new XMOMObjectRefactoringResult(fqXmlName, type));
      } else {
        return DocumentOrder.getNothing();
      }
    }
    
        
  }
  
  protected class RefactorAllReferences extends WorkUnit {
    
    private final List<Pair<XPathExpression, RefactoringElement>> referencePathFinders;

    protected RefactorAllReferences(RefactoringTargetType... relevantRootType) {
      super(relevantRootType);
      XPath xpathObj = factory.newXPath();
      try { 
        referencePathFinders = new ArrayList<Pair<XPathExpression,RefactoringElement>>();
        for (RefactoringElement refactoringElement : refactorings) {
          xpathObj.setXPathVariableResolver(new XPathVariableResolver() {
            
            @Override
            public Object resolveVariable(QName variableName) {
              if ("packageOld".equals(variableName.getLocalPart())) {
                return refactoringElement.packageOld;
              }
              return null;
            }
          });
          referencePathFinders.add(Pair.of(xpathObj.compile("//*[@" + GenerationBase.ATT.REFERENCEPATH + "=$packageOld]"),
                                   refactoringElement));
        }
      } catch (XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public DocumentOrder work(RefactoringTargetType type, Document doc, String fqXmlName, Element typeInformationCarrier) {
      XMOMObjectRefactoringResult result = null;
      for (Pair<XPathExpression, RefactoringElement> referenceFinder : referencePathFinders) {
        Object foundReferences;
        try {
          foundReferences = referenceFinder.getFirst().evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
        NodeList nodes = (NodeList) foundReferences;
        if (nodes.getLength() > 0) {
          for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
              Element element = (Element) node;
              String referenceName = element.getAttribute(GenerationBase.ATT.REFERENCENAME);
              boolean matches = false;
              if (referenceName.equals(referenceFinder.getSecond().nameOld)) {
                element.setAttribute(GenerationBase.ATT.REFERENCENAME, referenceFinder.getSecond().nameNew);
                matches = true;
              } else if (referenceName.equals(referenceFinder.getSecond().nameOld + "." + referenceFinder.getSecond().nameOld)) {
                element.setAttribute(GenerationBase.ATT.REFERENCENAME, referenceFinder.getSecond().nameNew + "." + referenceFinder.getSecond().nameNew);
                matches = true;
              }
              if (matches) {
                if (result == null) {
                  result = new XMOMObjectRefactoringResult(fqXmlName, type);
                }                element.setAttribute(GenerationBase.ATT.REFERENCEPATH, referenceFinder.getSecond().packageNew);
                if (type != RefactoringTargetType.FORM) {
                  LabelInformation label = XMLRefactoringUtils.refactorLabel(element, referenceFinder.getSecond().labelOld,
                                                                             referenceFinder.getSecond().labelNew, XMLElementType.fromTagName(element.getTagName()));
                  if (label != null) {              
                    result.unmodifiedLabels.add(label);
                  }
                }
                if (config.deactivateRefactoredOperationCodeSnippets() && type == RefactoringTargetType.DATATYPE) {
                  Node operationNode = element.getParentNode().getParentNode();
                  if (operationNode.getNodeType() == Node.ELEMENT_NODE &&
                      ((Element) operationNode).getTagName().equals(GenerationBase.EL.OPERATION)) {
                    Element sourceCode = XMLUtils.getChildElementByName((Element) operationNode, GenerationBase.EL.SOURCECODE);
                    if (sourceCode != null) {
                      Element codeSnippet = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.CODESNIPPET);
                      if (codeSnippet != null) {
                        codeSnippet.setAttribute(GenerationBase.ATT.CODESNIPPET_ACTIVE, Boolean.FALSE.toString());
                        result.deactivatedOperationSnippets.add(((Element) operationNode).getAttribute(GenerationBase.ATT.LABEL));
                      }
                    }
                  }
                }
                if (type == RefactoringTargetType.WORKFLOW && element.getTagName().equals(GenerationBase.EL.SERVICEREFERENCE)) {
                  String refactoredServiceReferenceId =  element.getAttribute(GenerationBase.ATT.ID);
                  Element root = doc.getDocumentElement();
                  List<Element> functionElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.FUNCTION);
                  for (Element functionElement : functionElements) {
                    Element operationElement = XMLUtils.getChildElementByName(functionElement, GenerationBase.EL.INVOKE);
                    if (operationElement != null) {
                      String operationName = operationElement.getAttribute(GenerationBase.ATT.INVOKE_OPERATION);
                      String serviceRefId = operationElement.getAttribute(GenerationBase.ATT.SERVICEID);
                      if (operationName != null && operationName.equals(referenceFinder.getSecond().nameOld)
                          && serviceRefId.equals(refactoredServiceReferenceId)) {
                        operationElement.setAttribute(GenerationBase.ATT.INVOKE_OPERATION, referenceFinder.getSecond().nameNew);
                        XMLRefactoringUtils.refactorLabel(functionElement, referenceFinder.getSecond().labelOld,
                                                          referenceFinder.getSecond().labelNew, XMLElementType.FUNCTION);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (result == null) {
        return DocumentOrder.getNothing();
      } else {
        return new DocumentOrder(DocumentOrderType.SAVE, result);
      }
    }
    
  }


}

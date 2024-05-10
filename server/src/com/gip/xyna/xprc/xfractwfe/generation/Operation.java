/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMethodAbstractAndStaticException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.InterfaceVersion;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SpecialPurposeIdentifier;



public abstract class Operation implements HasDocumentation {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Operation.class);
  
  public static final String METHOD_INTERNAL_IMPLEMENTATION_SUFFIX = "_InternalImplementation";
  public static final String METHOD_INTERNAL_SUPERCALL_DESTINATION_SUFFIX = "_InternalSuperCallDestination";
  public static final String METHOD_INTERNAL_SUPERCALL_PROXY = "_InternalSuperCallProxy";
  public static final String VAR_SUPERCALL_DELEGATOR = "internalSuperCallDelegator";

  private boolean isStatic;
  private String operationName;
  private String operationNameWithoutVersion;
  private InterfaceVersion version = InterfaceVersion.BASE;
  private String operationLabel;
  protected DOM parent;
  private SpecialPurposeIdentifier specialPurposeIdentifier;
  private Map<String, String> additionalSpecialPurposeAttributes = new HashMap<>();
  private boolean isStepEventListener;
  private boolean isAbstract = false;
  private boolean isFinal = false; 
  private String documentation = "";
  private boolean hasBeenPersisted = false;

  private ArrayList<AVariable> inputVars = new ArrayList<AVariable>();
  private ArrayList<AVariable> outputVars = new ArrayList<AVariable>();
  private List<ExceptionVariable> thrownExceptions = new ArrayList<ExceptionVariable>();
  private UnknownMetaTagsComponent unknownMetaTagsComponent = new UnknownMetaTagsComponent();


  public boolean isStatic() {
    return isStatic;
  }


  public void setStatic(boolean b) {
    isStatic = b;
  }
  
  
  public boolean isStepEventListener() {
    return isStepEventListener;
  }


  public void setIsStepEventListener(boolean b) {
    isStepEventListener = b;
  }


  public Operation(DOM parent) {
    this.parent = parent;
  }


  public DOM getParent() {
    return parent;
  }


  public void setName(String operationName) {
    this.operationName = operationName;
    if (operationName != null &&
        operationName.endsWith(getVersion().getSuffix())) {
      operationNameWithoutVersion = operationName.substring(0, operationName.length() - getVersion().getSuffix().length());//_<version> abschneiden
    } else {
      operationNameWithoutVersion = operationName;
    }
  }


  public void setVersion(InterfaceVersion v) {
    this.version = v;
    //operationNameWithoutVersion setzen
    if (operationName != null) {
      setName(operationName);
    }
  }


  public String getName() {
    return operationName;
  }
  
  
  public void setLabel(String operationLabel) {
    this.operationLabel = operationLabel;
  }

  
  public String getLabel() {
    return operationLabel;
  }


  public final void parseXML(Element op) throws XPRC_InvalidPackageNameException {

    Element metaElement = XMLUtils.getChildElementByName(op, GenerationBase.EL.META);
    if (metaElement != null) {
      Element specialPurposeElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.SPECIAL_PURPOSE);
      if (specialPurposeElement != null) {
        specialPurposeIdentifier = SpecialPurposeIdentifier.getSpecialPurposeElementByXmlIdentifier(XMLUtils.getTextContent(specialPurposeElement));
        NamedNodeMap attributes = specialPurposeElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          additionalSpecialPurposeAttributes.put(attributes.item(i).getLocalName(), attributes.item(i).getTextContent());
        }
      }
    }

    parseXmlInternally(op);

  }


  public void validate() throws XPRC_InvalidXmlMissingRequiredElementException, XPRC_InvalidXmlMethodAbstractAndStaticException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_PrototypeDeployment,
      XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED  {
    if (isAbstract && isStatic()) {
      throw new XPRC_InvalidXmlMethodAbstractAndStaticException(getName());
    }
    for (AVariable v : inputVars) {
      v.validate();
    }
    for (AVariable v : outputVars) {
      v.validate();
    }
    for (AVariable v : thrownExceptions) {
      v.validate();
    }
  }


  protected void parseXMLOperation(Element operation) throws XPRC_InvalidPackageNameException {

    isAbstract = XMLUtils.isTrue(operation, GenerationBase.ATT.ABSTRACT);
    setName(operation.getAttribute(GenerationBase.ATT.OPERATION_NAME));
    setLabel(operation.getAttribute(GenerationBase.ATT.LABEL));
    setStatic(XMLUtils.isTrue(operation, GenerationBase.ATT.ISSTATIC));
    isFinal = XMLUtils.isTrue(operation, GenerationBase.ATT.ISFINAL); // -> default = false
    hasBeenPersisted = true;

    Element metaElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.META);
    if (metaElement != null) {
      Element documentationElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        documentation = XMLUtils.getTextContent(documentationElement);
      }

      Element persistedElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.HAS_BEEN_PERSISTED);
      if (persistedElement != null) {
        try {
          hasBeenPersisted = Boolean.parseBoolean(XMLUtils.getTextContent(persistedElement));
        } catch (Exception e) {
          logger.warn("Could not parse meta tag " + GenerationBase.EL.HAS_BEEN_PERSISTED, e);
        }
      }

      Element versionElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.VERSION);
      if (versionElement != null) {
        String version = XMLUtils.getTextContent(versionElement);
        Element currentVersionElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.CURRENTVERSION);
        boolean current = false;
        if (currentVersionElement != null) {
          current = Boolean.valueOf(XMLUtils.getTextContent(currentVersionElement));
        }
        setVersion(new InterfaceVersion(version, current));
      }
      
      List<String> knownMetaTags = Arrays.asList(GenerationBase.EL.DOCUMENTATION,
                                                 GenerationBase.EL.HAS_BEEN_PERSISTED,
                                                 GenerationBase.EL.VERSION);
      unknownMetaTagsComponent.parseUnknownMetaTags(operation, knownMetaTags);
    }
    
    // input/output variables
    inputVars.addAll( Service.parseInputOutput(operation, GenerationBase.EL.OPERATION, GenerationBase.EL.INPUT, parent) );
    outputVars.addAll( Service.parseInputOutput(operation, GenerationBase.EL.OPERATION, GenerationBase.EL.OUTPUT, parent) );
    
    Element throwsElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.THROWS);

    // thrown exceptions
    if (throwsElement != null) {
      List<Element> thrownExceptionElements = XMLUtils.getChildElementsByName(throwsElement,
                                                                              GenerationBase.EL.EXCEPTION);
      if (thrownExceptionElements.size() > 0) {
        for (Element singleExceptionElement : thrownExceptionElements) {
          ExceptionVariable v = new ExceptionVariable(parent);
          v.parseXML(singleExceptionElement);
          thrownExceptions.add(v);
        }
      }
    }

  }
  
  /**
   * enthält ggfs auch xynaexception-klasse
   */
  public List<ExceptionVariable> getThrownExceptions() {
    return Collections.unmodifiableList(thrownExceptions);
  }


  public List<ExceptionVariable> getThrownExceptionsForMod() {
    return thrownExceptions;
  }
  
  public List<String> getUnknownMetaTags() {
    return unknownMetaTagsComponent.getUnknownMetaTags();
  }
  
  public void setUnknownMetaTags(List<String> unknownMetaTags) {
    unknownMetaTagsComponent.setUnknownMetaTags(unknownMetaTags);
  }


  protected abstract void getImports(Set<String> imports);


  protected abstract List<WF> getDependentWFs();


  public boolean isAbstract() {
    return isAbstract;
  }


  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }


  public List<AVariable> getInputVars() {
    return inputVars;
  }


  public List<AVariable> getOutputVars() {
    return outputVars;
  }


  protected ArrayList<DOM> getDependentDoms() {
    ArrayList<DOM> l = new ArrayList<DOM>();
    for (AVariable v : getInputVars()) {
      if (v.getDomOrExceptionObject() instanceof DOM) {
        l.add((DOM) v.getDomOrExceptionObject());
      }
      if (v.getDefaultTypeRestriction() instanceof DOM) {
        l.add((DOM) v.getDefaultTypeRestriction());
      }
    }
    for (AVariable v : getOutputVars()) {
      if (v.getDomOrExceptionObject() instanceof DOM) {
        l.add((DOM) v.getDomOrExceptionObject());
      }
      if (v.getDefaultTypeRestriction() instanceof DOM) {
        l.add((DOM) v.getDefaultTypeRestriction());
      }
    }
    return l;
  }


  protected List<ExceptionGeneration> getDependentExceptions() {
    ArrayList<ExceptionGeneration> l = new ArrayList<ExceptionGeneration>();
    for (AVariable v : getInputVars()) {
      if (v.getDomOrExceptionObject() instanceof ExceptionGeneration) {
        l.add((ExceptionGeneration) v.getDomOrExceptionObject());
      }
      if (v.getDefaultTypeRestriction() instanceof ExceptionGeneration) {
        l.add((ExceptionGeneration) v.getDefaultTypeRestriction());
      }
    }
    for (AVariable v : getOutputVars()) {
      if (v.getDomOrExceptionObject() instanceof ExceptionGeneration) {
        l.add((ExceptionGeneration) v.getDomOrExceptionObject());
      }
      if (v.getDefaultTypeRestriction() instanceof ExceptionGeneration) {
        l.add((ExceptionGeneration) v.getDefaultTypeRestriction());
      }
    }
    for (ExceptionVariable exVar : getThrownExceptions()) {
      if (exVar.isPrototype()) {
        continue;
      }
      // no need to check castability here
      l.add((ExceptionGeneration) exVar.getDomOrExceptionObject());
    }
    return l;
  }

  
  protected void generateJava(CodeBuffer cb, Set<String> importedClassesFqStrings) {
    // method declaration
    if (isStatic()) {
      cb.add("public static ");
      createMethodSignature(cb, true, importedClassesFqStrings, getName());
    } else {
      if (!isAbstract()) {
        cb.add("protected ");
        createMethodSignature(cb, true, importedClassesFqStrings, getName()
            + METHOD_INTERNAL_SUPERCALL_DESTINATION_SUFFIX, getParent().getSimpleClassName() + " "
            + VAR_SUPERCALL_DELEGATOR);
        cb.add(" {").addLB();
        if (getOutputVars() != null && getOutputVars().size() > 0) {
          cb.add("return ");
        }
        if (implementedInJavaLib()) {
          //delegation in die javalib
          generateJavaForInvocation(cb, VAR_SUPERCALL_DELEGATOR + "." + getName());
        } else {
          //delegation an die hiesige implementierung
          generateJavaForInvocation(cb, getName() + METHOD_INTERNAL_IMPLEMENTATION_SUFFIX);
        }
        cb.addLB();
        cb.addLine("}").addLB();
      }

      cb.add("public ");
      if (isAbstract()) {
        cb.add("abstract ");
      }
      createMethodSignature(cb, true, importedClassesFqStrings, getName());
      if (isAbstract()) {
        cb.addLB(2);
        return;
      }
      cb.add(" {").addLB();
      if (implementedInJavaLib()) {
        cb.addLine(DOM.INIT_METHODNAME, "()"); //lazy init
      }
      
      if (getOutputVars() != null && getOutputVars().size() > 0) {
        cb.add("return ");
      }
      generateJavaForInvocation(cb, getName()
          + METHOD_INTERNAL_IMPLEMENTATION_SUFFIX);
      cb.addLB();
      cb.addLine("}").addLB();

      cb.add("private ");
      createMethodSignature(cb, true, importedClassesFqStrings, getName()
          + METHOD_INTERNAL_IMPLEMENTATION_SUFFIX);
    }

    cb.add(" {").addLB();
    generateJavaImplementation(cb, importedClassesFqStrings);
    cb.addLine("}").addLB();
  } 
  
  /**
   * true, falls weder normales codesnippet noch workflowcall, sondern einfach eine delegation ins impl.jar
   */
  public boolean implementedInJavaLib() {
    return false;
  }

  
  /**
   * signature ab output, ohne öffnende klammer für impl, also z.b.<br>
   * Type1 m(Type2 para1, Type3 para2) throws A, B<br>
   * endet nicht mit leerzeichen
   */
  public abstract void createMethodSignature(CodeBuffer cb, boolean includeImplementation,
                                             Set<String> importedClassesFqStrings, String operationName,
                                             String... additionalInputParameters);


  public abstract void generateJavaForInvocation(CodeBuffer cb, String operationName,
                                                 String... additionalInputParameters);

  
  public String getOutputParameterOfMethodSignature(Set<String> importedClassesFqStrings) {
    if (getOutputVars().size() == 0) {
      return "void";
    } else if (getOutputVars().size() == 1) {
      return getOutputVars().get(0).getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings);
    } else {
      return Container.class.getSimpleName();
    }
  }
  
  /**
   * inhalt der impl methode, also ohne signature etc 
   */
  protected abstract void generateJavaImplementation(CodeBuffer cb, Set<String> importedClassesFqStrings);


  protected abstract void parseXmlInternally(Element op) throws XPRC_InvalidPackageNameException;


  /**
   * @return true, falls nicht kompatibel
   */
  protected boolean compareImplementation(Operation otherOp) {
    //operationName
    if (!otherOp.getName().equals(getName())) {
      return true;
    }
    //abstract
    if (isAbstract() != otherOp.isAbstract()) {
      return true;
    }
    //vars
    if (!parametersAreEqual(getInputVars(), otherOp.getInputVars())) {
      return true;
    }
    if (!parametersAreEqual(getOutputVars(), otherOp.getOutputVars())) {
      return true;
    }
    return false;
  }


  public SpecialPurposeIdentifier getSpecialPurposeIdentifier() {
    return specialPurposeIdentifier;
  }
  
  
  public boolean isSpecialPurpose(final SpecialPurposeIdentifier... identifier) {
    if (identifier == null || identifier.length == 0) {
      if (specialPurposeIdentifier == null) {
        return true;
      } else {
        return false;
      }
    } else {
      for (SpecialPurposeIdentifier specialPurposeIdentifier : identifier) {
        if (specialPurposeIdentifier == this. specialPurposeIdentifier) {
          return true;
        }
      }
      return false;
    }
  }


  public Map<String, String> getSpecialPurposeAttributes() {
    return additionalSpecialPurposeAttributes;
  }

  
  public boolean isFinal() {
    return isFinal;
  }


  /**
   * vergleicht methodenname, und inputvariablen auf kompatibilität. es berücksichtigt nicht "throws"-parameter und outputparameter 
   */
  public boolean hasEqualSignature(Operation otherOperation) {
    if (otherOperation == null) {
      return false;
    }
    if (!otherOperation.getName().equals(getName())) {
      return false;
    }
    if (!parametersAreEqual(getInputVars(), otherOperation.getInputVars())) {
      return false;
    }
    return true;
  }


  public static boolean parametersAreEqual(List<AVariable> vars, List<AVariable> vars2) {
    if (vars.size() == vars2.size()) {
      for (int i = 0; i < vars.size(); i++) {
        AVariable a = vars.get(i);
        AVariable aSuper = vars2.get(i);
        if ((a.isJavaBaseType() && a.getJavaTypeEnum() == aSuper.getJavaTypeEnum())
            || (a.isPrototype() && aSuper.isPrototype())
            || (!a.isJavaBaseType() && a.getFQClassName() != null && a.getFQClassName().equals(aSuper.getFQClassName()))) {
          if (a.isList() == aSuper.isList()) {
            // ok, weiter
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
    } else {
      return false;
    }
    return true;
  }


  @Override
  public String getDocumentation() {
    return documentation;
  }


  @Override
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }


  public boolean hasBeenPersisted() {
    return hasBeenPersisted;
  }


  public void setHasBeenPersisted(boolean hasBeenPersisted) {
    this.hasBeenPersisted = hasBeenPersisted;
  }


  public String getNameWithoutVersion() {
    return operationNameWithoutVersion;
  }


  public InterfaceVersion getVersion() {
    return version;
  }


  public void takeOverSignature(Operation opToCopyFrom) {
    this.inputVars = new ArrayList<AVariable>(opToCopyFrom.inputVars);
    this.outputVars = new ArrayList<AVariable>(opToCopyFrom.outputVars);
    this.thrownExceptions = new ArrayList<ExceptionVariable>(opToCopyFrom.thrownExceptions);
  }

  /**
   * Returns true if the operation is inherited or overriden in the given data type, false otherwise.
   */
  public boolean isInheritedOrOverriden(DOM dataType) {
    OperationInformation[] operationInformations = dataType.collectOperationsOfDOMHierarchy(true);
    for (OperationInformation opInfo : operationInformations) {
      if ( (opInfo.getOperation() == this) && (!opInfo.getDefiningType().equals(dataType)) ) {
        return true;
      }
    }

    return false;
  }

}

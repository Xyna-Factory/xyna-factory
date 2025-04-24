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
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMethodAbstractAndStaticException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.GenericInputAsContextStep;
import com.gip.xyna.xprc.xfractwfe.base.StartVariableContextStep;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SpecialPurposeIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;



public abstract class CodeOperation extends Operation {

  // used for manual interaction
  public static final String CORRELATED_XYNA_ORDER_VAR_NAME = "correlatedXynaOrder";

  // used for synchronization service
  public static final String VARNAME_INTERNAL_XYNA_STEP_ID_PARAMETER = "internalXynaStepIdParameter";
  public static final String VARNAME_INTERNAL_XYNA_FIRST_WAITING_TIME_PARAMETER = "internalXynaFirstWaitingTimeParameter";

  //used for waitSuspendFeature service 
  public static final String VARNAME_INTERNAL_XYNA_SUSPENSION_TIME_PARAMETER = "internalXynaSuspensionTimeParameter";
  public static final String VARNAME_INTERNAL_XYNA_RESUME_TIME_PARAMETER = "internalXynaResumeTimeParameter";

  //used for waitSuspendFeature, synchronization service
  public static final String VARNAME_INTERNAL_XYNA_LANE_ID_PARAMETER = "internalXynaLaneIdParameter";

  //used for activation document handling
  public static final String VARNAME_INTERNAL_DOCUMENT_FROM_CONTEXT = "internalDocumentFromContext";

  private final String CODE_LANGUAGE;
  private boolean requiresXynaOrder;
  private String impl;
  private boolean isImplActive = true;


  public CodeOperation(DOM parent, String codeLanguage) {
    super(parent);
    this.CODE_LANGUAGE = codeLanguage;
  }


  public boolean requiresXynaOrder() {
    return requiresXynaOrder;
  }


  public String getImpl() {
    return impl;
  }


  public void setImpl(String impl) {
    this.impl = impl;
  }


  public boolean hasEmptyImpl() {
    return impl == null || impl.trim().length() == 0;
  }


  public void setActive(boolean active) {
    isImplActive = active;
  }


  public boolean isActive() {
    return isImplActive;
  }


  public String getCodeLanguage() {
    return CODE_LANGUAGE;
  }


  protected void getImports(Set<String> imports) {
    for (AVariable v : getInputVars()) {
      if (v.isJavaBaseType) {
        if (v.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          imports.add(XynaObject.class.getName());
        }
      } else if (v.getFQClassName() != null) {
        imports.add(v.getFQClassName());
      }
    }
    for (AVariable v : getOutputVars()) {
      if (v.isJavaBaseType) {
        if (v.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          imports.add(XynaObject.class.getName());
        }
      } else if (v.getFQClassName() != null) {
        imports.add(v.getFQClassName());
      }
    }
    for (ExceptionVariable v : getThrownExceptions()) {
      if (v.getFQClassName() != null) {
        imports.add(v.getFQClassName());
      }
    }
    if (isSpecialPurpose(SpecialPurposeIdentifier.STARTDOCUMENTCONTEXT, SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT,
                         SpecialPurposeIdentifier.RETRIEVEDOCUMENT)) {
      imports.add(StartVariableContextStep.class.getName());
    } else if (isSpecialPurpose(SpecialPurposeIdentifier.STARTGENERICCONTEXT, SpecialPurposeIdentifier.STOPGENERICCONTEXT)) {
      imports.add(GenericInputAsContextStep.class.getName());
    }
  }


  protected void generateJavaImplementation(CodeBuffer cb, Set<String> importedClassesFqStrings) {
    // FIXME: damit klassen vom compiler kompiliert werden, muss ihre benutzung erkannt werden können.
    // wenn aber output ein container ist, und die klassen ggfs im code nirgendwo referenziert werden, werden
    // sie auch nicht kompiliert:
    // workaround fuer fehlende imports, wenn ein container zurueckgegeben wird
    // und impl "return null" ist und typen nicht als import definiert sind
    // ist das die beste loesung? der compiler könnte die klassen-verwendung ja auch durchaus wegoptimieren
    if (getOutputVars().size() > 1) {
      int cnt = 0;
      for (AVariable outputVar : getOutputVars()) {
        if ((outputVar.getFQClassName() != null) && !importedClassesFqStrings.contains(outputVar.getFQClassName())) {
          cnt++;
          cb.addLine(outputVar.getFQClassName() + " dummyVarForMissingImport" + cnt + " = null");
        }
      }
    }

    if (XynaFactory.isFactoryServer() && XynaProperty.INVALIDATE_WF_EXECUTION.get()) {
      DeploymentItemStateManagement dism = GenerationBase.getDeploymentItemStateManagement();
      if (dism != null) {
        DeploymentItemState dis = dism.get(getParent().getOriginalFqName(), getParent().getRevision());
        if (dis != null) {
          DeploymentLocation location =
              getParent().getDeploymentMode().shouldCopyXMLFromSavedToDeployed() ? DeploymentLocation.SAVED : DeploymentLocation.DEPLOYED;
          if (dis.hasServiceImplInconsistencies(location, true)) {
            cb.addLine("throw new ", RuntimeException.class.getName(),
                       "(\"Operation is implemented as java library call, but library is out-of-date.\")");
            return;
          }
        }
      }
    }

    if (!XynaFactory.isFactoryServer()) {
      // code generation from script access
      cb.addLine("throw new ", RuntimeException.class.getName(), "(\"Class was generated outside a running factory.\")");
    } else if (!isActive()) {
      // stub generation from code access uses this atm
      cb.addLine("throw new ", RuntimeException.class.getName(), "(\"Class was generated as stub.\")");
    } else if (implementedInJavaLib() && !getParent().libraryExists()) {
      //zustand beim ersten speichern von der gui aus -> soll kompilieren. TODO über irgendein flag steuern, ob das
      //hier eine runtimeexception zur laufzeit oder zur deployzeit wirft.
      cb.addLine("throw new ", RuntimeException.class.getName(),
                 "(\"Operation is implemented as java library call, but library is not defined in xml.\")");
    } else {
      generateJavaImplementationInternally(cb);
    }
  }


  protected abstract void generateJavaImplementationInternally(CodeBuffer cb);


  public void generateJavaForInvocation(CodeBuffer cb, String operationName, String... additionalInputParameters) {
    cb.add(operationName).add("(");
    for (String additionalInputParameter : additionalInputParameters) {
      cb.addListElement(additionalInputParameter);
    }

    if (requiresXynaOrder()) {
      cb.addListElement(CORRELATED_XYNA_ORDER_VAR_NAME);
    }
    if (isSpecialPurpose(SpecialPurposeIdentifier.RETRIEVEDOCUMENT, SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT)) {
      cb.addListElement(VARNAME_INTERNAL_DOCUMENT_FROM_CONTEXT);
    }
    for (AVariable v : getInputVars()) {
      cb.addListElement(v.getVarName());
    }

    final boolean specialPurposeAwaitSynchronization = isSpecialPurpose(SpecialPurposeIdentifier.SYNC_AWAIT);
    final boolean specialPurposeLongRunningAwaitSynchronization = isSpecialPurpose(SpecialPurposeIdentifier.SYNC_LONG_AWAIT);
    final boolean specialPurposeNotifySynchronization = isSpecialPurpose(SpecialPurposeIdentifier.SYNC_NOTIFY);
    final boolean specialPurposeWaitOrSuspend = isSpecialPurpose(SpecialPurposeIdentifier.WAIT, SpecialPurposeIdentifier.SUSPEND);

    if (specialPurposeAwaitSynchronization || specialPurposeNotifySynchronization || specialPurposeLongRunningAwaitSynchronization) {
      cb.addListElement(VARNAME_INTERNAL_XYNA_STEP_ID_PARAMETER);
      if (specialPurposeAwaitSynchronization || specialPurposeLongRunningAwaitSynchronization) {
        cb.addListElement(VARNAME_INTERNAL_XYNA_FIRST_WAITING_TIME_PARAMETER);
      }
      cb.addListElement(VARNAME_INTERNAL_XYNA_LANE_ID_PARAMETER);
    }
    if (specialPurposeWaitOrSuspend) {
      cb.addListElement(VARNAME_INTERNAL_XYNA_SUSPENSION_TIME_PARAMETER);
      cb.addListElement(VARNAME_INTERNAL_XYNA_RESUME_TIME_PARAMETER);
      cb.addListElement(VARNAME_INTERNAL_XYNA_LANE_ID_PARAMETER);
    }

    cb.add(")");
  }


  public void createMethodSignature(CodeBuffer cb, boolean includeImplementation, Set<String> importedClassesFqStrings,
                                    String operationName, String... additionalInputParameters) {
    // outputvar
    cb.add(getOutputParameterOfMethodSignature(importedClassesFqStrings), " ");
    cb.add(operationName).add("(");
    for (String additionalInputParameter : additionalInputParameters) {
      cb.addListElement(additionalInputParameter);
    }

    if (requiresXynaOrder()) {
      cb.addListElement(XynaOrderServerExtension.class.getSimpleName() + " " + CORRELATED_XYNA_ORDER_VAR_NAME);
    }
    if (isSpecialPurpose(SpecialPurposeIdentifier.RETRIEVEDOCUMENT, SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT)) {
      cb.addListElement("xact.templates.Document " + VARNAME_INTERNAL_DOCUMENT_FROM_CONTEXT);
    }
    for (AVariable v : getInputVars()) {
      cb.addListElement(v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + " " + v.getVarName());
    }

    final boolean specialPurposeAwaitSynchronization = isSpecialPurpose(SpecialPurposeIdentifier.SYNC_AWAIT);
    final boolean specialPurposeLongRunningAwaitSynchronization = isSpecialPurpose(SpecialPurposeIdentifier.SYNC_LONG_AWAIT);
    final boolean specialPurposeNotifySynchronization = isSpecialPurpose(SpecialPurposeIdentifier.SYNC_NOTIFY);
    final boolean specialPurposeWaitOrSuspend = isSpecialPurpose(SpecialPurposeIdentifier.WAIT, SpecialPurposeIdentifier.SUSPEND);

    if (specialPurposeAwaitSynchronization || specialPurposeNotifySynchronization || specialPurposeLongRunningAwaitSynchronization) {
      cb.addListElement("Integer " + VARNAME_INTERNAL_XYNA_STEP_ID_PARAMETER);
      if (specialPurposeAwaitSynchronization || specialPurposeLongRunningAwaitSynchronization) {
        cb.addListElement("Long " + VARNAME_INTERNAL_XYNA_FIRST_WAITING_TIME_PARAMETER);
      }
      cb.addListElement("String " + VARNAME_INTERNAL_XYNA_LANE_ID_PARAMETER);
    }
    if (specialPurposeWaitOrSuspend) {
      cb.addListElement("Long " + VARNAME_INTERNAL_XYNA_SUSPENSION_TIME_PARAMETER);
      cb.addListElement("java.util.concurrent.atomic.AtomicLong " + VARNAME_INTERNAL_XYNA_RESUME_TIME_PARAMETER);
      cb.addListElement("String " + VARNAME_INTERNAL_XYNA_LANE_ID_PARAMETER);
    }

    cb.add(")");
    if (getThrownExceptions().size() > 0) {
      cb.add(" throws ");
      List<ExceptionVariable> exceptions = getThrownExceptions();
      for (int i = 0; i < getThrownExceptions().size(); i++) {
        ExceptionVariable exceptionVar = exceptions.get(i);
        if (exceptionVar.isPrototype()) {
          throw new RuntimeException("Operation " + operationName + " throws prototype exception");
        }
        cb.add(exceptionVar.getClassName(importedClassesFqStrings));
        if (i < getThrownExceptions().size() - 1) {
          cb.add(", ");
        }
      }
    }

  }


  /**
  * @param operation muss ein operation element sein
  */
  protected void parseXmlInternally(Element operation) throws XPRC_InvalidPackageNameException {
    parseXMLOperation(operation);
    // general stuff
    requiresXynaOrder = XMLUtils.isTrue(operation, GenerationBase.ATT.REQUIRES_XYNA_ORDER);

    // impl
    if (!isAbstract()) {
      Element sourceCode = XMLUtils.getChildElementByName(operation, GenerationBase.EL.SOURCECODE);
      if (sourceCode == null) {
        return;
      }

      Element snippet = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.CODESNIPPET);
      extractActiveSetting(snippet);

      impl = XMLUtils.getTextContent(snippet);

      // add other Attribute-EventListener-Marker here as they get implemented
      setIsStepEventListener(XMLUtils.isTrue(snippet, GenerationBase.ATT.ISCANCELABLE));
    } else {
      Element sourceCode = XMLUtils.getChildElementByName(operation, GenerationBase.EL.SOURCECODE);
      if (sourceCode != null) {
        Element snippet = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.CODESNIPPET);
        extractActiveSetting(snippet);
        if (snippet != null) {
          setIsStepEventListener(XMLUtils.isTrue(snippet, GenerationBase.ATT.ISCANCELABLE));
        }
      }
    }
  }


  private void extractActiveSetting(Element snippet) {
    if (snippet != null) {
      String isActiveAttribute = snippet.getAttribute(GenerationBase.ATT.CODESNIPPET_ACTIVE);
      isImplActive = isActiveAttribute == null || !isActiveAttribute.equals(Boolean.FALSE.toString());
    }
  }


  protected ArrayList<WF> getDependentWFs() {
    return new ArrayList<WF>();
  }


  @Override
  public void validate() throws XPRC_InvalidXmlMissingRequiredElementException, XPRC_InvalidXmlMethodAbstractAndStaticException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE,
      XPRC_PrototypeDeployment, XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException,
      XPRC_MEMBER_DATA_NOT_IDENTIFIED {
    super.validate();
    if (!isAbstract()) {
      if (impl == null) {
        throw new XPRC_InvalidXmlMissingRequiredElementException(GenerationBase.EL.OPERATION, GenerationBase.EL.SOURCECODE);
      }
    }
  }

}

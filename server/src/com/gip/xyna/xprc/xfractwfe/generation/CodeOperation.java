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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMethodAbstractAndStaticException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;



public abstract class CodeOperation extends Operation {

  private static Logger logger = CentralFactoryLogging.getLogger(JavaOperation.class);

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

  private boolean requiresXynaOrder;
  private String impl;
  private boolean isImplActive = true;


  public CodeOperation(DOM parent) {
    super(parent);
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

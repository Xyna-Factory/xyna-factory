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

package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public abstract class Operation implements XmlAppendable {
  
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.OPERATION); {
      xml.addAttribute(ATT.ID, getId());
      xml.addAttribute(ATT.LABEL, (getLabel() == null) ? XMLUtils.escapeXMLValue(getName(), true, false)
                                                       : XMLUtils.escapeXMLValue(getLabel(), true, false));
      xml.addAttribute(ATT.OPERATION_NAME, XMLUtils.escapeXMLValue(getName(), true, false));
      xml.addAttribute(ATT.ISSTATIC, String.valueOf(isStatic()));
      if (isAbstract()) {
        xml.addAttribute(ATT.ABSTRACT, String.valueOf(Boolean.TRUE.toString()));
      }
      if (isFinal()) {
        xml.addAttribute(ATT.ISFINAL, String.valueOf(Boolean.TRUE.toString()));
      }
      if (requiresXynaOrder()) {
        xml.addAttribute(ATT.REQUIRES_XYNA_ORDER, Boolean.TRUE.toString());
      }
      xml.endAttributes();
      
      xml.startElement(EL.INPUT); {
        for( Variable input : getInputs() ) {
          input.appendXML(xml);
        }
      } xml.endElement(EL.INPUT);
      
      xml.startElement(EL.OUTPUT); {
        for( Variable output : getOutputs() ) {
          output.appendXML(xml);
        }
      } xml.endElement(EL.OUTPUT);
      
      if( !getExceptions().isEmpty() ) {
        xml.startElement(EL.THROWS); {
          for( Variable exception : getExceptions() ) {
            exception.appendXML(xml);
          }
        } xml.endElement(EL.THROWS);
      }
      
      appendOperationContentToXML(xml);
    } xml.endElement(EL.OPERATION);
  }
  
  protected abstract void appendOperationContentToXML(XmlBuilder xml);
  public abstract String getId();
  public abstract String getLabel();
  public abstract String getName();
  public abstract boolean isStatic();
  public abstract boolean isFinal();
  public abstract boolean isAbstract();
  public abstract boolean requiresXynaOrder();
  public abstract String getDocumentation();
  public abstract boolean hasBeenPersisted();
  public abstract List<Variable> getInputs();
  public abstract List<Variable> getOutputs();
  public abstract List<Variable> getExceptions();
  
  public abstract boolean hasUnknownMetaTags();
  public abstract void appendUnknownMetaTags(XmlBuilder xml);
}

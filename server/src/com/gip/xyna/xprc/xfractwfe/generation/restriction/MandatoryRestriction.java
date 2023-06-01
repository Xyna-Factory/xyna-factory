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
package com.gip.xyna.xprc.xfractwfe.generation.restriction;

import java.util.Arrays;

import org.w3c.dom.Element;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.Mandatory;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public class MandatoryRestriction extends Restriction<Mandatory> {
  

  private boolean mandatory;

  public void specialParseXml(Element restrictionElement) {
    mandatory = true;
  }
  
  public boolean isMandatory() {
    return mandatory;
  }
  
  public RestrictionType getType() {
    return RestrictionType.MANDATORY;
  }
  
  public void generateJava(CodeBuffer cb) {
    cb.add("@", generateFqAnnotationNameForJava(Mandatory.class), "(");
    appendUtilization(cb);
    cb.add(")")
      .add(Constants.LINE_SEPARATOR); // explicit LB instead of addLB to prevent invalid semicolon
  }
  
  protected void read(Mandatory annotation) {
    if (annotation.utilizationPolicy() != null &&
        annotation.utilizationPolicy().length > 0) {
      this.utilizations = Arrays.asList(annotation.utilizationPolicy());
    }
  }
  
  @Override
  public String toString() {
    return "Mandatory";
  }


  @Override
  public void appendXML(XmlBuilder xml) {
    if (utilizations == null || utilizations.size() == 0) {
      xml.element(EL.RESTRICTION_MANDATORY, Boolean.toString(mandatory));
    } else {
      xml.startElementWithAttributes(EL.RESTRICTION_MANDATORY);
      xml.addAttribute(GenerationBase.ATT.RESTRICTION_UTILIZATION_POLICY, String.join(", ", utilizations));
      xml.endAttributesNoLineBreak();
      xml.append(Boolean.toString(mandatory));
      xml.endElementNoIdent(EL.RESTRICTION_MANDATORY);
    }
  }

}

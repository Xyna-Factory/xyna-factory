/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.MaxLength;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public class MaxLengthRestriction extends Restriction<MaxLength> {
  

  private int limit;
  
  
  public MaxLengthRestriction() {
  }
  
  public MaxLengthRestriction(int limit) {
    super();
    this.limit = limit;
  }

  public void specialParseXml(Element restrictionElement) {
    String maxLengthContent = XMLUtils.getTextContentOrNull(restrictionElement);
    if (maxLengthContent != null) {
      try {
        limit = Integer.parseInt(maxLengthContent);
      } catch (NumberFormatException nfe) {
        // maxLength setting will be silently ignored, log? 
      }
    }
  }
  
  public void generateJava(CodeBuffer cb) {
    cb.add("@", generateFqAnnotationNameForJava(MaxLength.class), "(", MaxLength.PARAMETER_NAME_LIMIT, "=", Integer.toString(limit));
    if (utilizations != null &&
        utilizations.size() > 0) {
      cb.add(", ");
      appendUtilization(cb);
    }
    cb.add(")")
      .add(Constants.LINE_SEPARATOR); // explicit LB instead of addLB to prevent invalid semicolon
  }
  
  public int getLimit() {
    return limit;
  }

  public RestrictionType getType() {
    return RestrictionType.MAX_LENGTH;
  }
 
  
  protected void read(MaxLength annotation) {
    if (annotation.utilizationPolicy() != null &&
        annotation.utilizationPolicy().length > 0) {
      this.utilizations = Arrays.asList(annotation.utilizationPolicy());
    }
    this.limit = annotation.limit();
  }
  
  @Override
  public String toString() {
    return "MaxLength:" + limit;
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    if (utilizations == null || utilizations.size() == 0) {
      xml.element(EL.RESTRICTION_MAX_LENGTH, Integer.toString(limit));
    } else {
      xml.startElementWithAttributes(EL.RESTRICTION_MAX_LENGTH);
      xml.addAttribute(GenerationBase.ATT.RESTRICTION_UTILIZATION_POLICY, String.join(", ", utilizations));
      xml.endAttributesNoLineBreak();
      xml.append(Integer.toString(limit));
      xml.endElementNoIdent(EL.RESTRICTION_MAX_LENGTH);
    }
  }

}

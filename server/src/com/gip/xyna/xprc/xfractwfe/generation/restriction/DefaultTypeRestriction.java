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
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.DefaultType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public class DefaultTypeRestriction extends Restriction<DefaultType> {
  

  private String defaultType;

  public void specialParseXml(Element restrictionElement) {
    String name = XMLUtils.getTextContentOrNull(restrictionElement);
    defaultType = name;
  }
  
  
  public String getDefaultType() {
    return defaultType;
  }


  public RestrictionType getType() {
    return RestrictionType.DEFAULT_TYPE;
  }


  public void generateJava(CodeBuffer cb) {
    cb.add("@", generateFqAnnotationNameForJava(DefaultType.class), "(", DefaultType.PARAMETER_NAME_DEFAULT_TYPE, "=\"", defaultType, "\"");
    if (utilizations != null &&
        utilizations.size() > 0) {
      cb.add(", ");
      appendUtilization(cb);
    }
    cb.add(")")
      .add(Constants.LINE_SEPARATOR); // explicit LB instead of addLB to prevent invalid semicolon
  }


  protected void read(DefaultType annotation) {
    if (annotation.utilizationPolicy() != null &&
        annotation.utilizationPolicy().length > 0) {
      this.utilizations = Arrays.asList(annotation.utilizationPolicy());
    }
    this.defaultType = annotation.defaultType();
  }


  @Override
  public void appendXML(XmlBuilder xml) {
    if (utilizations == null || utilizations.size() == 0) {
      xml.element(EL.RESTRICTION_DEFAULT_TYPE, defaultType);
    } else {
      xml.startElementWithAttributes(EL.RESTRICTION_DEFAULT_TYPE);
      xml.addAttribute(GenerationBase.ATT.RESTRICTION_UTILIZATION_POLICY, String.join(", ", utilizations));
      xml.endAttributesNoLineBreak();
      xml.append(defaultType);
      xml.endElementNoIdent(EL.RESTRICTION_DEFAULT_TYPE);
    }
  }

}

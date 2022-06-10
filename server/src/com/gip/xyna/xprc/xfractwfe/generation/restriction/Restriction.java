/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlAppendable;


public abstract class Restriction<A extends Annotation> implements XmlAppendable {
  
  protected List<String> utilizations; 
  
  public void parseXml(Element restrictionElement) {
    if (restrictionElement.hasAttribute(GenerationBase.ATT.RESTRICTION_UTILIZATION_POLICY)) {
      utilizations = new ArrayList<String>();
      String utilizationPolicy = restrictionElement.getAttribute(GenerationBase.ATT.RESTRICTION_UTILIZATION_POLICY);
      for (String utilization : utilizationPolicy.split(",")) {
        utilizations.add(utilization.trim());
      }
    }
    specialParseXml(restrictionElement);
  }
  
  
  protected void appendUtilization(CodeBuffer cb) {
    if (utilizations != null &&
        utilizations.size() > 0) {
      cb.add(ModelledRestriction.PARAMETER_NAME_UTILIZATION_POLICY, "={");
      Iterator<String> utilizationIter = utilizations.iterator();
      while (utilizationIter.hasNext()) {
        cb.add("\"", utilizationIter.next(), "\"");
        if (utilizationIter.hasNext()) {
          cb.add(", ");
        }
      }
      cb.add("}");
    }
  }
  
  
  public abstract void specialParseXml(Element restrictionElement);
  
  public abstract RestrictionType getType();
  
  
  public static Restriction<?> parse(Element restrictionElement) {
    return RestrictionType.instantiateAndParseRestriction(restrictionElement);
  }
  
  public static Restriction<?> parse(Annotation restrictionAnnotation) {
    return RestrictionType.instantiateAndFillRestriction(restrictionAnnotation);
  }
  
  public boolean isApplicable(String utilization) {
    if (utilization == null || // no utilization filter
        utilizations == null ||
        utilizations.size() <= 0) {
      return true;
    } else {
      return utilizations.contains(utilization);
    }
  }


  public abstract void generateJava(CodeBuffer cb);
  
  protected static String generateFqAnnotationNameForJava(Class<?> annotationClass) {
    return annotationClass.getName().replace('$', '.');
  }


  protected abstract void read(A annotation);
  
  public String toString() {
    return this.getClass().getSimpleName();
  }
  
}

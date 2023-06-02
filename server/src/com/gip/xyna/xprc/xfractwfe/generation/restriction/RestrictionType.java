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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

public enum RestrictionType {

  MANDATORY(GenerationBase.EL.RESTRICTION_MANDATORY,
            ModelledRestriction.Mandatory.class,
            MandatoryRestriction.class),
  MAX_LENGTH(GenerationBase.EL.RESTRICTION_MAX_LENGTH, 
             ModelledRestriction.MaxLength.class, 
             MaxLengthRestriction.class),
  DEFAULT_TYPE(GenerationBase.EL.RESTRICTION_DEFAULT_TYPE, 
               ModelledRestriction.DefaultType.class, 
               DefaultTypeRestriction.class);
  
  
  private final String tagName;
  private final Class<? extends Annotation> annotationClass;
  private final Class<? extends Restriction<?>> restrictionClass;
  
  private RestrictionType(String tagName, Class<? extends Annotation> annotationClass, Class<? extends Restriction<?>> restrictionClass) {
    this.tagName = tagName;
    this.annotationClass = annotationClass;
    this.restrictionClass = restrictionClass;
  }
  
  
  private static RestrictionType identifyByTagName(Element element) {
    for (RestrictionType type : values()) {
      if (type.tagName.equals(element.getNodeName())) {
        return type;
      }
    }
    return null;
  }
  
  public static Restriction<?> instantiateAndParseRestriction(Element element) {
    RestrictionType type = identifyByTagName(element);
    if (type != null) {
      Restriction<?> restriction;
      try {
        restriction = type.restrictionClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | 
               NoSuchMethodException | SecurityException | InvocationTargetException e) {
        throw new RuntimeException("Failed to instantiate Restriction of type " + type.toString());
      } 
      restriction.parseXml(element);
      return restriction;
    } else {
      return null;
    }
  }
  
  
  private static RestrictionType identifyByAnnotation(Annotation annotation) {
    for (RestrictionType type : values()) {
      if (type.annotationClass.isInstance(annotation)) {
        return type;
      }
    }
    return null;
  }


  @SuppressWarnings("unchecked")
  public static Restriction<?> instantiateAndFillRestriction(Annotation annotation) {
    RestrictionType type = identifyByAnnotation(annotation);
    if (type != null) {
      @SuppressWarnings("rawtypes")
      Restriction restriction;
      try {
        restriction = type.restrictionClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | 
               NoSuchMethodException | SecurityException | InvocationTargetException e) {
        throw new RuntimeException("Failed to instantiate Restriction of type " + type.toString());
      }
      restriction.read(annotation);
      return restriction;
    }
    return null;
  }
  
}

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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

public class RestrictionUtils {
  
  
  @SuppressWarnings("unchecked")
  public static Collection<? extends Annotation> findModelledRestrictions(Class<? extends GeneralXynaObject> gxo) {
    List<Annotation> matches = new ArrayList<Annotation>();
    Class<? extends GeneralXynaObject> current = gxo;
    while (current != null) {
      for (Field field : current.getDeclaredFields()) {
        matches.addAll(getModelledRestrictions(field));
      }
      if (current.getSuperclass() != null &&
          GeneralXynaObject.class.isAssignableFrom(current.getSuperclass())) {
        current = (Class<? extends GeneralXynaObject>) current.getSuperclass();
      }
    }
    return matches;
  }
  
  public static Collection<? extends Annotation> getModelledRestrictions(Field field) {
    Annotation[] annotations = field.getAnnotations();
    if (annotations.length > 0) {
      List<Annotation> matches = new ArrayList<Annotation>();
      for (Annotation annotation : annotations) {
        if (annotation.annotationType().getAnnotation(ModelledRestriction.class) != null) {
          matches.add(annotation);
        }
      }
      return matches;
    } else {
      return Collections.emptyList();
    }
  }
  
  public static <A extends Annotation> A getModelledRestrictions(Field field, Class<A> type) {
    if (type.getAnnotation(ModelledRestriction.class) == null) {
      return null; // TODO or throw NotAModelledRestriction
    }
    return field.getAnnotation(type);
  }
  
  
  public static Map<String, Restrictions> parseClass(Class<? extends GeneralXynaObject> gxo) {
    Map<String, Restrictions> restrictionMap = new HashMap<>();
    Collection<Field> allFields = collectFieldsRecursivly(gxo);
    for (Field field : allFields) {
      Restrictions restrictions = new Restrictions(); 
      for (Annotation annotation : field.getAnnotations()) {
        if (isModelledRestriction(annotation)) {
          Restriction<?> restriction = Restriction.parse(annotation);
          if (restriction != null) {
            Collection<Restriction<?>> typedRestrictions = restrictions.getRestriction(restriction.getType());
            if (typedRestrictions == null) {
              typedRestrictions = new ArrayList<Restriction<?>>();
              restrictions.getRestrictions().put(restriction.getType(), typedRestrictions);
            }
            typedRestrictions.add(restriction);
          }
        }
      }
      if (restrictions.getRestrictions().size() > 0) {
        restrictionMap.put(field.getName(), restrictions);
      }
    }
    return restrictionMap;
  }
  

  @SuppressWarnings("unchecked")
  private static Collection<Field> collectFieldsRecursivly(Class<? extends GeneralXynaObject> gxo) {
    Collection<Field> fields = new ArrayList<>();
    fields.addAll(Arrays.asList(gxo.getDeclaredFields()));
    if (gxo.getSuperclass() != null &&
        GeneralXynaObject.class.isAssignableFrom(gxo.getSuperclass())) {
      fields.addAll(collectFieldsRecursivly((Class<? extends GeneralXynaObject>) gxo.getSuperclass()));
    }
    return fields;
  }
  
  
  private static boolean isModelledRestriction(Annotation annotation) {
    return annotation.annotationType().getAnnotation(ModelledRestriction.class) != null;
  }

}

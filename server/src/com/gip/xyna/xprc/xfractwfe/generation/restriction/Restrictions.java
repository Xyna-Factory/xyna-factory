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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public class Restrictions {
  
  private EnumMap<RestrictionType, Collection<Restriction<?>>> restrictions;
  
  
  public Restrictions() {
    restrictions = new EnumMap<>(RestrictionType.class);
  }
  
  
  public void parseXml(Element restrictionElement) {
    List<Element> children = XMLUtils.getChildElements(restrictionElement);
    for (Element child : children) {
      Restriction<?> restriction = Restriction.parse(child);
      if (restriction != null) {
        addRestriction(restriction);
      }
    }
  }
  
  
  public <R extends Restriction<?>> R getRestriction(RestrictionType type) {
    return getRestriction(type, null);
  }
  
  @SuppressWarnings("unchecked")
  public <R extends Restriction<?>> R getRestriction(RestrictionType type, String utilization) {
    Collection<Restriction<?>> typedRestrictions = restrictions.get(type);
    if (typedRestrictions != null) {
      for (Restriction<?> typedRestriction : typedRestrictions) {
        if (typedRestriction.isApplicable(utilization)) {
          return (R) typedRestriction;
        }
      }
      return null;
    } else {
      return null;
    }
  }
  
  public boolean hasRestriction(RestrictionType type) {
    return hasRestriction(type, null);
  }
  
  public boolean hasRestriction(RestrictionType type, String utilization) {
    return hasApplicableRestriction(type, utilization);
  }
  
  public EnumMap<RestrictionType, Collection<Restriction<?>>> getRestrictions() {
    return restrictions;
  }

  public boolean hasApplicableRestriction(RestrictionType type, String utilization) {
    return getRestriction(type, utilization) != null;
  }

  public void generateJava(CodeBuffer cb) {
    for (Collection<Restriction<?>> typedRestrictions : restrictions.values()) {
      for (Restriction<?> typedRestriction : typedRestrictions) {
        typedRestriction.generateJava(cb);
      }
    }
  }


  public void addRestriction(Restriction<?> restriction) {
    Collection<Restriction<?>> typedRestrictions = restrictions.get(restriction.getType());
    if (typedRestrictions == null) {
      typedRestrictions = new ArrayList<Restriction<?>>();
      restrictions.put(restriction.getType(), typedRestrictions);
    }
    typedRestrictions.add(restriction);
  }


  
  
}

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



import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;




public class AdditionalDependencyContainer implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum AdditionalDependencyType {

    XYNA_PROPERTY(GenerationBase.EL.DEPENDENCY_XYNA_PROPERTY), DATATYPE(GenerationBase.EL.DEPENDENCY_DATATYPE), WORKFLOW(
                    GenerationBase.EL.DEPENDENCY_WORKFLOW), SHARED_LIB(GenerationBase.EL.DEPENDENCY_SHARED_LIB), TRIGGER(
                    GenerationBase.EL.DEPENDENCY_TRIGGER), FILTER(GenerationBase.EL.DEPENDENCY_FILTER), EXCEPTION(
                    GenerationBase.EL.DEPENDENCY_EXCEPTION), ORDERTYPE(GenerationBase.EL.DEPENDENCY_ORDERTYPE);

    private String xmlElementName;


    private AdditionalDependencyType(String name) {
      this.xmlElementName = name;
    }


    public String getXmlElementName() {
      return xmlElementName;
    }


    private static final AdditionalDependencyType[] xmomTypes = new AdditionalDependencyType[] {AdditionalDependencyType.WORKFLOW,
        AdditionalDependencyType.DATATYPE, AdditionalDependencyType.EXCEPTION};


    public static AdditionalDependencyType[] xmomTypes() {
      return xmomTypes;
    }

  }


  private final Map<AdditionalDependencyType, Set<String>> mapDependencyTypeToCorrespondingDependencies = new HashMap<AdditionalDependencyType, Set<String>>();
  private static final Set<String> emptyStringSet = Collections.unmodifiableSet(new HashSet<String>());
  
  private Long revision;


  public AdditionalDependencyContainer() {
  }


  public AdditionalDependencyContainer(Element s) {
    this(s, null);
  }
  
  public AdditionalDependencyContainer(Element s, Long revision) {
    if (s == null)
      throw new IllegalArgumentException("parent element may not be null");
    parseMore(s);
    this.revision = revision;
  }


  public void parseMore(Element s) {
    Element additionalDepsContainer = XMLUtils.getChildElementByName(s, EL.ADDITIONALDEPENDENCIES);
    if (additionalDepsContainer != null) {

      // iterate over all types of additional dependencies
      for (AdditionalDependencyType dependencyType : AdditionalDependencyType.values()) {
        for (Element e : XMLUtils.getChildElementsByName(additionalDepsContainer, dependencyType.getXmlElementName())) {
          String content = XMLUtils.getTextContent(e).trim();
          if (content != null && content.length() != 0) {
            Set<String> additionalDatatypeDependencies = mapDependencyTypeToCorrespondingDependencies
                            .get(dependencyType);
            if (additionalDatatypeDependencies == null) {
              additionalDatatypeDependencies = new HashSet<String>();
              mapDependencyTypeToCorrespondingDependencies.put(dependencyType, additionalDatatypeDependencies);
            }
            additionalDatatypeDependencies.add(content);
          }
        }
      }

    }
  }

  
  public Set<String> getAdditionalDependencies(AdditionalDependencyType type) {
    Set<String> result = mapDependencyTypeToCorrespondingDependencies.get(type);
    if (result == null) {
      return emptyStringSet;
    } else {
      return result;
    }
  }


  protected void addAdditionalSharedLibDependencies(Set<String> libs) {
    Set<String> existing = mapDependencyTypeToCorrespondingDependencies.get(AdditionalDependencyType.SHARED_LIB);
    if (existing == null) {
      mapDependencyTypeToCorrespondingDependencies.put(AdditionalDependencyType.SHARED_LIB, new HashSet<String>(libs));
    } else {
      existing.addAll(libs);
    }
  }

  public Long getRevision() {
    return revision;
  }
  
}

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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;



/**
 * persistenceinfo aus dem globalen meta element eines datentyps
 */
public class PersistenceInformation {

  public static final String PERSISTENCE_FEATURE = "Feature";
  public static final String HISTORIZATION = "Historization";
  public static final String INDEX = "Index";
  public static final String FLAT = "Flat";
  public static final String FLAT_EXCLUSION = "FlatExclusion";
  public static final String FLATTEN_TO_INTERFACE = "FlattenToInterface";
  public static final String CONSTRAINT = "Constraint";
  public static final String TRANSIENT = "Transient";
  public static final String CUSTOM_FIELD_0 = "CustomField0";
  public static final String CUSTOM_FIELD_1 = "CustomField1";
  public static final String CUSTOM_FIELD_2 = "CustomField2";
  public static final String CUSTOM_FIELD_3 = "CustomField3";
  public static final String REFERENCE = "Reference";

  private Set<String> indices;
  private Set<String> constraints;
  private Set<String> transience;
  private Set<String> customField0;
  private Set<String> customField1;
  private Set<String> customField2;
  private Set<String> customField3;
  private Set<String> references;
  private Set<String> flattened;
  private Set<String> flatExclusions;
  private boolean flattenToInterface;
  
  private PrimitiveType primaryKeyType;


  public Set<String> getTransients() {
    return transience;
  }


  public Set<String> getCustomField0() {
    return customField0;
  }


  public Set<String> getCustomField1() {
    return customField1;
  }


  public Set<String> getCustomField2() {
    return customField2;
  }


  public Set<String> getCustomField3() {
    return customField3;
  }


  public Set<String> getReferences() {
    return references;
  }
  
  public Set<String> getIndices() {
    return indices;
  }
  
  public Set<String> getConstraints() {
    return constraints;
  }
  
  public Set<String> getFlattened() {
    return flattened;
  }
  
  public Set<String> getFlatExclusions() {
    return flatExclusions;
  }
  
  public boolean hasTableRepresentation() {
    return !flattenToInterface;
  }

  protected static final PersistenceInformation EMPTY = new PersistenceInformation();
  static {
    EMPTY.indices = parsePaths(null, INDEX);
    EMPTY.constraints = parsePaths(null, CONSTRAINT);
    EMPTY.transience = parsePaths(null, TRANSIENT);
    EMPTY.customField0 = parsePaths(null, CUSTOM_FIELD_0);
    EMPTY.customField1 = parsePaths(null, CUSTOM_FIELD_1);
    EMPTY.customField2 = parsePaths(null, CUSTOM_FIELD_2);
    EMPTY.customField3 = parsePaths(null, CUSTOM_FIELD_3);
    EMPTY.references = parsePaths(null, REFERENCE);
    EMPTY.flattened = parsePaths(null, FLAT);
    EMPTY.flatExclusions = parsePaths(null, FLAT_EXCLUSION);
    EMPTY.primaryKeyType = PrimitiveType.LONG;
  }


  protected static PersistenceInformation parse(Element metaElement) {
    Element persistenceElement = null;
    if (metaElement == null) {
      return EMPTY;
    } else {
      persistenceElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.PERSISTENCE);
      if (persistenceElement == null) {
        return EMPTY;
      }
    }
    PersistenceInformation persi = new PersistenceInformation();
    persi.indices = parsePaths(persistenceElement, INDEX);
    persi.constraints = parsePaths(persistenceElement, "Constraint");
    persi.transience = parsePaths(persistenceElement, "Transient");
    persi.customField0 = parsePaths(persistenceElement, "CustomField0");
    persi.customField1 = parsePaths(persistenceElement, "CustomField1");
    persi.customField2 = parsePaths(persistenceElement, "CustomField2");
    persi.customField3 = parsePaths(persistenceElement, "CustomField3");
    persi.references = parsePaths(persistenceElement, "Reference");
    persi.flattened = parsePaths(persistenceElement, FLAT);
    persi.flatExclusions = parsePaths(persistenceElement, FLAT_EXCLUSION);
    persi.primaryKeyType = PrimitiveType.LONG;
    persi.flattenToInterface = parseBoolean(persistenceElement, FLATTEN_TO_INTERFACE);
    return persi;
  }

  protected static PersistenceInformation forSuperType(PrimitiveType primaryKeyType) {
    PersistenceInformation persi = new PersistenceInformation();
    persi.indices = parsePaths(null, INDEX);
    persi.constraints = parsePaths(null, "Constraint");
    persi.transience = parsePaths(null, "Transient");
    persi.customField0 = parsePaths(null, "CustomField0");
    persi.customField1 = parsePaths(null, "CustomField1");
    persi.customField2 = parsePaths(null, "CustomField2");
    persi.customField3 = parsePaths(null, "CustomField3");
    persi.references = parsePaths(null, "Reference");
    persi.flattened = parsePaths(null, FLAT);
    persi.flatExclusions = parsePaths(null, FLAT_EXCLUSION);
    persi.primaryKeyType = primaryKeyType;
    return persi;
  }

  public void setPkType(PrimitiveType pktype) {
    primaryKeyType = pktype;
  }


  private static boolean parseBoolean(Element persistenceElement, String tag) {
    if (persistenceElement == null) {
      return false;
    }
    List<Element> set = XMLUtils.getChildElementsByName(persistenceElement, tag);
    if (set == null || set.size() <= 0) {
      return false;
    } else {
      for (Element element : set) {
        String contentText = XMLUtils.getTextContent(element);
        if (contentText != null && contentText.length() > 0) {
          return Boolean.parseBoolean(contentText);
        }
      }
      return false;
    }
  }
  
  
  private static Set<String> parsePaths(Element persistenceElement, String tag) {
    if (persistenceElement == null) {
      return new HashSet<String>();
    }
    List<Element> set = XMLUtils.getChildElementsByName(persistenceElement, tag);
    if (set == null || set.size() <= 0) {
      return new HashSet<String>();
    } else {
      Set<String> indexSet = new HashSet<String>();
      for (Element element : set) {
        String indexText = XMLUtils.getTextContent(element);
        if (indexText != null && indexText.length() > 0) {
          indexSet.add(indexText);
        }
      }
      return indexSet;
    }
  }
  
  
  public PrimitiveType getPrimaryKeyType() {
    return primaryKeyType;
  }
  
  
  public static PrimitiveType detectPrimaryKeyType(Collection<AVariable> variables) {
    boolean currentVersionFound = false;
    boolean historizationStampFound = false;
    AVariable uidVariable = null;
    for (AVariable aVariable : variables) {
      if (aVariable.persistenceTypes != null) {
        if (aVariable.persistenceTypes.contains(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP)) {
          historizationStampFound = true;
        } else if (aVariable.persistenceTypes.contains(PersistenceTypeInformation.CURRENTVERSION_FLAG)) {
          currentVersionFound = true;
        } else if (aVariable.persistenceTypes.contains(PersistenceTypeInformation.UNIQUE_IDENTIFIER)) {
          uidVariable = aVariable;
        }
      }
    }
    if (currentVersionFound && historizationStampFound) {
      return PrimitiveType.STRING;
    } else {
      if (uidVariable == null) {
        return null;
      } else {
        return uidVariable.getJavaTypeEnum();
      }
    }
  }


  protected void validate(DOM dom) {
    // TODO validate indices
    boolean currentVersionFound = false;
    boolean historizationStampFound = false;
    List<AVariable> memberVars = new ArrayList<AVariable>(dom.getMemberVars());
    for (AVariable aVariable : memberVars) {
      if (aVariable.persistenceTypes != null) {
        if (aVariable.persistenceTypes.contains(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP)) {
          historizationStampFound = true;
        } else if (aVariable.persistenceTypes.contains(PersistenceTypeInformation.CURRENTVERSION_FLAG)) {
          currentVersionFound = true;
        }
      }
    }
    if (currentVersionFound != historizationStampFound) {
      throw new RuntimeException("Storable Datatype " + dom.getOriginalFqName()
          + " specifies his desire to be historized but does not contained the necessary columns!");
    }

    for (String flat : flattened) {
      if (references.contains(flat)) {
        throw new RuntimeException("Path " + flat + " in " + dom.getOriginalFqName()
            + " is definied as flat and referenced, which is not both possible.");
      }
      /*for (String flat2 : flattened) {
        if (flat2.startsWith(flat + ".")) {
          throw new RuntimeException("Path " + flat + " in " + dom.getOriginalFqName() + " is subpath of " + flat2
              + ". Both are defined as flat.");
        }
      }*/
      for (String t : transience) {
        if (flat.startsWith(t + ".")) {
          throw new RuntimeException("Path " + t + " in " + dom.getOriginalFqName() + " is subpath of " + flat
              + ". The first is defined as transient, so the latter is not allowed to be flat.");
        }
      }
    }
  }

  public int getRestrictionsCount() {
    return getIndices().size() + getConstraints().size() + getTransients().size() + getCustomField0().size() + getCustomField1().size()
        + getCustomField2().size() + getCustomField3().size() + getReferences().size() + getFlattened().size() + getFlatExclusions().size();
  }

}

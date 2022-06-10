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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;

import java.util.Set;

import com.gip.xyna.xprc.xfractwfe.generation.PersistenceInformation;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 *
 */
public class Meta implements XmlAppendable {
  
  private PrimitiveType type;
  private String documentation;
  private DataModel dataModel;
  private String description;
  private Boolean isServiceGroupOnly;
  private PersistenceInformation persistenceInformation;
  private Set<PersistenceTypeInformation> persistenceTypes;

  public Meta() {
  }

  public Meta(Meta meta) {
    this.type = meta.type;
    this.documentation = meta.documentation;
    this.dataModel = meta.dataModel;
    this.description = meta.description;
    this.isServiceGroupOnly = meta.isServiceGroupOnly;
    this.persistenceInformation = meta.persistenceInformation;
    this.persistenceTypes = meta.persistenceTypes;
  }

  
  public void appendXML(XmlBuilder xml) {
    if (!hasEntries()) {
      return;
    }

    xml.startElement(EL.META);{
      // TODO Reihenfolge!

      if ( (persistenceInformation != null && persistenceInformation.getRestrictionsCount() > 0) ||
           (persistenceTypes != null && persistenceTypes.size() > 0) ) {
        xml.startElement(EL.PERSISTENCE); {
          if (persistenceInformation != null) {
            for (String index : persistenceInformation.getIndices()) {
              xml.element(PersistenceInformation.INDEX, index);
            }
            for (String constraint : persistenceInformation.getConstraints()) {
              xml.element(PersistenceInformation.CONSTRAINT, constraint);
            }
            for (String transientPersistence : persistenceInformation.getTransients()) {
              xml.element(PersistenceInformation.TRANSIENT, transientPersistence);
            }
            for (String customField0 : persistenceInformation.getCustomField0()) {
              xml.element(PersistenceInformation.CUSTOM_FIELD_0, customField0);
            }
            for (String customField1 : persistenceInformation.getCustomField1()) {
              xml.element(PersistenceInformation.CUSTOM_FIELD_1, customField1);
            }
            for (String customField2 : persistenceInformation.getCustomField2()) {
              xml.element(PersistenceInformation.CUSTOM_FIELD_2, customField2);
            }
            for (String customField3 : persistenceInformation.getCustomField3()) {
              xml.element(PersistenceInformation.CUSTOM_FIELD_3, customField3);
            }
            for (String reference : persistenceInformation.getReferences()) {
              xml.element(PersistenceInformation.REFERENCE, reference);
            }
            for (String flattened : persistenceInformation.getFlattened()) {
              xml.element(PersistenceInformation.FLAT, flattened);
            }
            for (String flatExclusion : persistenceInformation.getFlatExclusions()) {
              xml.element(PersistenceInformation.FLAT_EXCLUSION, flatExclusion);
            }
          }

          if (persistenceTypes != null) {
            for (PersistenceTypeInformation type : persistenceTypes) {
              type.appendXML(xml);
            }
          }
        } xml.endElement(EL.PERSISTENCE);
      }

      if (isServiceGroupOnly != null) {
        xml.element(EL.IS_SERVICE_GROUP_ONLY, Boolean.toString(isServiceGroupOnly));
      }

      if( type != null ) {
        xml.optionalElement(EL.METATYPE, XMLUtils.escapeXMLValueAndInvalidChars(type.getClassOfType(), false, false));
      }

      if( dataModel != null ) {
        dataModel.appendXML(xml);
      }

      if (documentation != null && documentation.length() > 0) {
        xml.element(EL.DOCUMENTATION, XMLUtils.escapeXMLValueAndInvalidChars(documentation, false, false));
      }

      if (description != null && description.length() > 0) {
        xml.element(EL.DESCRIPTION, XMLUtils.escapeXMLValueAndInvalidChars(description, false, false));
      }
    } xml.endElement(EL.META);
  }

  public boolean hasEntries() {
    return (persistenceInformation != null && persistenceInformation.getRestrictionsCount() > 0)
        || (persistenceTypes != null && persistenceTypes.size() > 0)
        || (isServiceGroupOnly != null) 
        || (type != null)
        || (dataModel != null)
        || (documentation != null && documentation.length() > 0)
        || (description != null && description.length() > 0);
  }

  public DataModel getDataModel() {
    return dataModel;
  }
  
  public PrimitiveType getType() {
    return type;
  }

  public String getDocumentation() {
    return documentation;
  }

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public Boolean isServiceGroupOnly() {
    return isServiceGroupOnly;
  }

  public void setIsServiceGroupOnly(Boolean isServiceGroupOnly) {
    this.isServiceGroupOnly = isServiceGroupOnly;
  }

  public PersistenceInformation getPersistenceInformation() {
    return persistenceInformation;
  }

  public void setPersistenceInformation(PersistenceInformation persistenceInformation) {
    this.persistenceInformation = persistenceInformation;
  }

  public static Meta simpleType(PrimitiveType simpleType) {
    Meta m = new Meta();
    m.type = simpleType;
    return m;
  }

  public static Meta simpleType(Meta meta, PrimitiveType simpleType) {
    Meta m = new Meta(meta);
    m.type = simpleType;
    return m;
  }
  
  public static Meta documentation(String documentation) {
    Meta m = new Meta();
    m.documentation = documentation;
    return m;
  }

  public static Meta documentation(Meta meta, String documentation) {
    Meta m = new Meta(meta);
    m.documentation = documentation;
    return m;
  }

  public static Meta persistenceTypes(Set<PersistenceTypeInformation> persistenceTypes) {
    Meta m = new Meta();
    m.persistenceTypes = persistenceTypes;
    return m;
  }

  public static Meta persistenceTypes(Meta meta, Set<PersistenceTypeInformation> persistenceTypes) {
    Meta m = new Meta(meta);
    m.persistenceTypes = persistenceTypes;
    return m;
  }

  public static Meta dataModel(DataModel dataModel) {
    Meta m = new Meta();
    m.dataModel = dataModel;
    return m;
  }

  public static Meta dataModel_documentation(DataModel dataModel, String documentation) {
    Meta m = new Meta();
    m.dataModel = dataModel;
    m.documentation = documentation;
    return m;
  }

  public static Meta description(String description) {
    Meta m = new Meta();
    m.description = description;
    return m;
  }
  
}

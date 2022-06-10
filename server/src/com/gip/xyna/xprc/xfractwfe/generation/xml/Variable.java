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

import java.util.Collection;
import java.util.EnumMap;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Restriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.RestrictionType;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Restrictions;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.UnsupportedJavaTypeException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;


/**
 *
 */
public class Variable implements XmlAppendable {
  
  protected String name;                  // Name 
  protected String label;                 // Label
  protected XmomType typeReference;       // Typ: entweder XMOM-Type oder null
  protected String abstractReferenceName; // oder abstrakter Typ
  protected Meta meta;                    // oder einfacher Java-Typ
  protected boolean isList;
  protected String id;
  protected boolean isException;
  protected boolean isAbstract;
  protected Restrictions restrictions = null;
  
  private Variable() {
  }
  
  private Variable(Variable variable) {
    this.name = variable.name;
    this.label = variable.label;
    this.typeReference = variable.typeReference;
    this.abstractReferenceName = variable.abstractReferenceName;
    this.meta = variable.meta;
    this.isList = variable.isList;
    this.id = variable.id;
    this.isException = variable.isException;
    this.isAbstract = variable.isAbstract;
    this.restrictions = variable.restrictions;
  }
 
  public Variable(String name, String label, XmomType type, Meta meta, boolean isList) { // TODO: id notwendig?
    this.name = name;
    this.label = label;
    this.typeReference = type;
    this.meta = meta;
    this.isList = isList;
  }
  
  public void appendXML(XmlBuilder xml) {
    String elementName = isException ? EL.EXCEPTION : EL.DATA;
    xml.startElementWithAttributes(elementName);
    if ( (id != null) && (id.length() > 0) ) {
      xml.addAttribute(ATT.ID, id);
    }
    xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(label, true, false));
    xml.addAttribute(ATT.VARIABLENAME, XMLUtils.escapeXMLValue(name, true, false));
    if (isList) {
      xml.addAttribute(ATT.ISLIST, ATT.TRUE);
    }
    if (typeReference != null) {
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(typeReference.getName(), true, false));
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(typeReference.getPath(), true, false));
    } else if (isAbstract) {
      xml.addAttribute(ATT.ABSTRACT, ATT.TRUE);
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(abstractReferenceName, true, false));
    } else if ( (meta != null) && (meta.getType() == PrimitiveType.XYNAEXCEPTION) ) {
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_NAME, true, false));
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_PATH, true, false));
    } else if ( (meta != null) && (meta.getType() == PrimitiveType.XYNAEXCEPTIONBASE) ) {
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(GenerationBase.DEFAULT_EXCEPTION_BASE_REFERENCE_NAME, true, false));
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(GenerationBase.DEFAULT_EXCEPTION_BASE_REFERENCE_PATH, true, false));
    } else if ( (meta != null) && (meta.getType() == PrimitiveType.EXCEPTION) ) {
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(GenerationBase.CORE_EXCEPTION_REFERENCE_NAME, true, false));
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(GenerationBase.CORE_EXCEPTION_REFERENCE_PATH, true, false));
    } else if ( (meta != null) && (meta.getType() == PrimitiveType.ANYTYPE) ) {
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(GenerationBase.ANYTYPE_REFERENCE_NAME, true, false));
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(GenerationBase.ANYTYPE_REFERENCE_PATH, true, false));
    }

    if (meta == null && restrictions == null) {
      xml.endElement();
    } else {
      xml.endAttributes();

      if (meta != null) {
        meta.appendXML(xml);
      }

      if (restrictions != null) {
        xml.startElement(EL.RESTRICTION); {
          EnumMap<RestrictionType, Collection<Restriction<?>>> restrictionMap = restrictions.getRestrictions();
          for (RestrictionType restrictionType : restrictionMap.keySet()) {
            for (Restriction<?> restriction : restrictionMap.get(restrictionType)) {
              restriction.appendXML(xml);
            }
          }
        } xml.endElement(EL.RESTRICTION);
      }

      xml.endElement(elementName);
    }
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public Meta getMeta() {
    return meta;
  }
  
  public XmomType getTypeReference() {
    return typeReference;
  }

  public static VariableBuilder create(String name) {
    return new VariableBuilder(name, false);
  }
  public static VariableBuilder createException(String name) {
    return new VariableBuilder(name, true);
  }
  
  public static class VariableBuilder {
    private Variable variable;
    private PrimitiveType simpleType;
    private String documentation;
    private Set<PersistenceTypeInformation> persistenceTypes;
    private String label;
    private boolean isList;
    private String id;
    private Restrictions restrictions;
    
    public VariableBuilder(String name, boolean isException) {
      variable = new Variable();
      variable.name = name;
      variable.isException = isException;
    }

    public VariableBuilder type(XmomType type) {
      variable.typeReference = type;
      return this;
    }
    
    public VariableBuilder meta(Meta meta) {
      variable.meta = meta;
      return this;
    }

    public VariableBuilder label(String label) {
      this.label = label;
      return this;
    }
    
    public VariableBuilder isList(boolean isList) {
      this.isList = isList;
      return this;
    }
    
    public VariableBuilder id(String id) {
      this.id = id;
      return this;
    }

    public VariableBuilder simpleType(PrimitiveType simpleType) {
      this.simpleType = simpleType;
      return this;
    }

    public VariableBuilder documentation(String documentation) {
      this.documentation = documentation;
      return this;
    }

    public VariableBuilder persistenceTypes(Set<PersistenceTypeInformation> persistenceTypes) {
      this.persistenceTypes = persistenceTypes;
      return this;
    }

    public VariableBuilder abstractType(String variableName, String referenceName) {
      variable.isAbstract = true;
      variable.name = variableName;
      variable.abstractReferenceName = referenceName;
      
      return this;
    }
    
    public VariableBuilder complexType(XmomType xmomType) {
      variable.typeReference = xmomType;
      return this;
    }

    public VariableBuilder restrictions(Restrictions restrictions) {
      this.restrictions = restrictions;
      return this;
    }

    public Variable build() {
      if (simpleType != null) {
        if (variable.meta == null) {
          variable.meta = Meta.simpleType(simpleType);
        } else {
          variable.meta = Meta.simpleType(variable.meta, simpleType);
        }
      }

      if (documentation != null) {
        if (variable.meta == null) {
          variable.meta = Meta.documentation(documentation);
        } else {
          variable.meta = Meta.documentation(variable.meta, documentation);
        }
      }

      if (persistenceTypes != null) {
        if (variable.meta == null) {
          variable.meta = Meta.persistenceTypes(persistenceTypes);
        } else {
          variable.meta = Meta.persistenceTypes(variable.meta, persistenceTypes);
        }
      }

      variable.label = label != null ? label : variable.name;
      variable.isList = isList;
      variable.id = id;
      variable.restrictions = restrictions;

      return new Variable(variable);
    }
  }

  @Deprecated
  public static Variable simple(String name, String label, Class<?> simpleType, boolean isList) {
    return new Variable(name, label, null, Meta.simpleType(getPrimitiveType(simpleType)), isList);
  }
  @Deprecated
  public static Variable complex(String name, String label, XmomType complexType, boolean isList) {
    return new Variable(name, label, complexType, null, isList);
  }
  @Deprecated
  public static Variable simple(String name, String label, Class<?> simpleType, Meta meta, boolean isList) {
    return new Variable(name, label, null, Meta.simpleType(meta,getPrimitiveType(simpleType)), isList);
  }
  @Deprecated
  public static Variable complex(String name, String label, XmomType complexType, Meta meta, boolean isList) {
    return new Variable(name, label, complexType, meta, isList);
  }
  @Deprecated
  public static PrimitiveType getPrimitiveType(Class<?> simpleType) {
    try {
      return PrimitiveType.create(simpleType.getSimpleName());
    } catch (UnsupportedJavaTypeException e) {
      Logger.getLogger(Variable.class).warn("No PrimitiveType for "+simpleType, e);
      return PrimitiveType.STRING;
    }
  }
  
}

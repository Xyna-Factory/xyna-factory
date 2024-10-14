/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Updater;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable.VariableBuilder;


/**
 *
 */
public class Datatype extends HierarchyTypeWithVariables {
  
  private static String XMOM_VERSION = "unknown";
  static {
    try {
      if (XynaFactory.isFactoryServer()) {
        XMOM_VERSION = Updater.getInstance().getXMOMVersion().getString();
      }
    } catch (Exception e ) { //XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException
      Logger.getLogger(Datatype.class).info("Could not get XMOM_VERSION", e);
    }
  }
  private final static String NAMESPACE = "http://www.gip.com/xyna/xdev/xfractmod";
  
  protected List<Operation> operations;
  protected String[] sharedLibs;
  protected Set<String> additionalLibNames;
  protected List<String> pythonLibNames;

  private Datatype() {
  }
  
  public Datatype(XmomType type) {
    this.type = type; 
    this.variables = Collections.emptyList();
    this.operations = Collections.emptyList();
  }
  
  @Deprecated
  public Datatype(XmomType type, List<Variable> variables) {
    this.type = type; 
    this.variables = variables;
    this.operations = Collections.emptyList();
  }
  
  @Deprecated
  public Datatype(XmomType type, XmomType basetype, Meta meta, List<Variable> variables) {
    this.type = type; 
    this.basetype = basetype;
    this.meta = meta;
    this.variables = variables;
    this.operations = Collections.emptyList();
  }

  private Datatype(Datatype datatype) {
    this.type = datatype.type; 
    this.basetype = datatype.basetype;
    this.meta = datatype.meta;
    this.sharedLibs = datatype.sharedLibs;
    this.additionalLibNames = datatype.additionalLibNames;
    this.pythonLibNames = datatype.pythonLibNames;
    this.variables = clone(datatype.variables);
    this.operations = clone(datatype.operations);
  }

  private <T> List<T> clone(List<T> list) {
    if( list == null || list.isEmpty() ) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList( new ArrayList<T>(list) );
  }

  public String getFQTypeName() {
    return type.getFQTypeName();
  }
  
  public String toXML() {
    XmlBuilder xml = new XmlBuilder();
    if(!XynaProperty.XML_HEADER_COMMENT.get().isBlank()) {
      xml.append("<!--");
      xml.append(XynaProperty.XML_HEADER_COMMENT.get());
      xml.append("-->");
    }
    xml.startElementWithAttributes(EL.DATATYPE);
    xml.addAttribute(ATT.XMLNS, NAMESPACE );
    xml.addAttribute(ATT.MDM_VERSION, XMOM_VERSION);
    xml.addAttribute(ATT.TYPENAME, type.getName() );
    xml.addAttribute(ATT.TYPEPATH, type.getPath() );
    xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(type.getLabel(), true, false) );
    xml.addAttribute(ATT.ABSTRACT, Boolean.toString(type.isAbstract()) );
    if (basetype != null) {
      xml.addAttribute(ATT.BASETYPENAME, basetype.getName() );
      xml.addAttribute(ATT.BASETYPEPATH, basetype.getPath() );
    }
    if( meta == null && variables.isEmpty() && operations.isEmpty() ) {
      xml.endAttributesAndElement();
    } else {
      xml.endAttributes();
      if( meta != null ) {
        meta.appendXML(xml);
      }
      for (Variable variable : variables) {
        variable.appendXML(xml);
      }

      // java libraries
      if (additionalLibNames != null) {
        for (String libName : additionalLibNames) {
          xml.element(EL.LIBRARIES, libName);
        }
      }
      
      // python libraries
      if (pythonLibNames != null) {
        for (String libName : pythonLibNames) {
          xml.element(EL.PYTHONLIBRARIES, libName);
        }
      }

      // java shared libraries
      if (sharedLibs != null) {
        for (String sharedLibName : sharedLibs) {
          xml.element(EL.SHAREDLIB, sharedLibName);
        }
      }

      if ( (!operations.isEmpty()) ||
           (meta != null && meta.isServiceGroupOnly() != null && meta.isServiceGroupOnly()) ) { // service groups need an service-tag - even when it doesn't contain any operations
        appendService(xml);
      }
      xml.endElement(EL.DATATYPE);
    }

    return xml.toString();
  }

  private void appendService(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.SERVICE);
    xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(type.getLabel(), true, false) );
    xml.addAttribute(ATT.TYPENAME, type.getName() );
    xml.endAttributes();
    for( Operation operation : operations ) {
      operation.appendXML(xml);
    }
    xml.endElement(EL.SERVICE);
  }

  public static Datatype base(XmomType type, Variable ... vs) {
    return Datatype.create(type).variables(Arrays.asList(vs)).build();
  }

  public static Datatype derived(XmomType type, XmomType base, Variable ... vs) {
    return Datatype.create(type).basetype(base).variables(Arrays.asList(vs)).build();
  }

  public static Datatype derived(XmomType type, XmomType base, List<Variable> variables) {
    return Datatype.create(type).basetype(base).variables(variables).build();
  }

  public static Datatype meta(XmomType type, XmomType base, Meta meta, Variable ... vs) {
    return Datatype.create(type).basetype(base).meta(meta).variables(Arrays.asList(vs)).build();
  }

  public static Datatype meta(XmomType type, XmomType base, Meta meta, List<Variable> variables) {
    return Datatype.create(type).basetype(base).meta(meta).variables(variables).build();
  }

  public XmomType getType() {
    return type;
  }

  public XmomType getBaseType() {
    return basetype;
  }

  public Meta getMeta() {
    return meta;
  }

  public List<Variable> getVariables() {
    return variables;
  }

  public static DatatypeBuilder create(XmomType type) {
    return new DatatypeBuilder(type);
  }

  public static class DatatypeBuilder extends HierarchyTypeWithVariablesBuilder<Datatype> {
    private Datatype datatype;
    
    public DatatypeBuilder(XmomType type) {
      datatype = new Datatype();
      datatype.type = type;
    }


    public DatatypeBuilder basetype(XmomType basetype) {
      datatype.basetype = basetype;
      return this;
    }

    public DatatypeBuilder meta(Meta meta) {
      datatype.meta = meta;
      return this;
    }

    public DatatypeBuilder sharedLibs(String[] sharedLibs) {
      datatype.sharedLibs = sharedLibs;
      return this;
    }

    public DatatypeBuilder additionalLibNames(Set<String> additionalLibNames) {
      datatype.additionalLibNames = additionalLibNames;
      return this;
    }
    
    public DatatypeBuilder pythonLibNames(List<String> pythonLibNames) {
      datatype.pythonLibNames = pythonLibNames;
      return this;
    }

    public DatatypeBuilder variable(Variable variable) {
      getOrCreateVariables().add(variable);
      return this;
    }
    
    public DatatypeBuilder variable(VariableBuilder variable) {
      getOrCreateVariables().add(variable.build());
      return this;
    }
    
    private List<Variable> getOrCreateVariables() {
      if( datatype.variables == null ) {
        datatype.variables = new ArrayList<Variable>();
      }
      return datatype.variables;
    }


    public DatatypeBuilder variables(List<Variable> variables) {
      if( variables == null ) {
        datatype.variables = null;
      } else {
        if( datatype.variables == null ) {
          datatype.variables = new ArrayList<Variable>();
        }
        datatype.variables.addAll(variables);
      }
      return this;
    }

    public DatatypeBuilder operations(List<Operation> operations) {
      for (Operation operation : operations) {
        operation(operation);
      }

      return this;
    }

    public DatatypeBuilder operation(Operation operation) {
      if( datatype.operations == null ) {
        datatype.operations = new ArrayList<Operation>();
      }
      datatype.operations.add(operation);
      return this;
    }

    
    public Datatype build() {
      return new Datatype(datatype);
    }

  }

}

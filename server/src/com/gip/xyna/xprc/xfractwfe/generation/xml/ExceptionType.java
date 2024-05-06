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
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 *
 */
public class ExceptionType extends HierarchyTypeWithVariables {
  
  private static String XMOM_VERSION = "unknown";
  static {
    try {
      if (XynaFactory.isFactoryServer()) {
        XMOM_VERSION = Updater.getInstance().getXMOMVersion().getString();
      }
    } catch (Exception e ) { //XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException
      Logger.getLogger(ExceptionType.class).info("Could not get XMOM_VERSION", e);
    }
  }
  //private final static String NAMESPACE = "http://www.gip.com/xyna/xdev/xfractmod";
  private final static String NAMESPACE = "http://www.gip.com/xyna/3.0/utils/message/storage/1.1";
  
  private String code;
  private List<Pair<String, String>> messages;

  private ExceptionType() {
  }

  private ExceptionType(ExceptionType exceptionType) {
    this.type = exceptionType.type; 
    this.basetype = exceptionType.basetype;
    this.meta = exceptionType.meta;
    this.code = exceptionType.code;
    this.variables = clone(exceptionType.variables);
    this.messages = clone(exceptionType.messages);
  }

  private <T> List<T> clone(List<T> list) {
    if( list == null || list.isEmpty() ) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList( new ArrayList<T>(list) );
  }

  public String getFqXmlName() {
    return type.getFQTypeName();
  }

  
  public String toXML() {
    XmlBuilder xml = new XmlBuilder();
    if(!XynaProperty.XML_HEADER_COMMENT.get().isBlank()) {
      xml.append("<!--");
      xml.append(XynaProperty.XML_HEADER_COMMENT.get());
      xml.append("-->");
    }
    xml.startElementWithAttributes(EL.EXCEPTIONSTORAGE); {
      xml.addAttribute(ATT.XMLNS, NAMESPACE );
      xml.addAttribute(ATT.MDM_VERSION, XMOM_VERSION);
      xml.addAttribute("Name", EL.EXCEPTIONSTORAGE); //TODO
      xml.endAttributes();

      xml.startElementWithAttributes(EL.EXCEPTIONTYPE); {
        if (code != null && code.length() > 0) {
          xml.addAttribute(ATT.EXCEPTION_CODE, code );
        }
        xml.addAttribute(ATT.TYPENAME, type.getName() );
        xml.addAttribute(ATT.TYPEPATH, type.getPath() );
        xml.addAttribute(ATT.LABEL, XMLUtils.escapeXMLValue(type.getLabel(), true, false) );
        xml.addAttribute(ATT.ABSTRACT, Boolean.toString(type.isAbstract()) );
        xml.addAttribute(ATT.BASETYPENAME, basetype.getName() );
        String baseTypePath = basetype.getPath().equals(XynaExceptionBase.class.getPackage().getName()) ? "core.exception" : basetype.getPath();
        xml.addAttribute(ATT.BASETYPEPATH, baseTypePath );
        xml.endAttributes();

        if (meta != null) {
          meta.appendXML(xml);
        }

        for (Variable variable : variables) {
          variable.appendXML(xml);
        }

        for (Pair<String,String> msg : messages) {
          xml.startElementWithAttributes("MessageText");
          xml.addAttribute("Language", msg.getFirst() );
          xml.endAttributesAndElement(XmlBuilder.encode(msg.getSecond()), "MessageText");
        }
      } xml.endElement(EL.EXCEPTIONTYPE);
    } xml.endElement(EL.EXCEPTIONSTORAGE);

    return xml.toString();
  }
  
  public static ExceptionTypeBuilder create(XmomType type) {
    return new ExceptionTypeBuilder(type);
  }

  public static class ExceptionTypeBuilder extends HierarchyTypeWithVariablesBuilder<ExceptionType> {
    private static final XmomType DEFAULT_BASE_TYPE = new XmomType("core.exception", "XynaExceptionBase", "Xyna Exception Base");
    private ExceptionType exceptionType;
    
    public ExceptionTypeBuilder(XmomType type) {
      exceptionType = new ExceptionType();
      exceptionType.type = type;
      exceptionType.basetype = DEFAULT_BASE_TYPE;
    }

    public ExceptionTypeBuilder basetype(XmomType basetype) {
      exceptionType.basetype = basetype;
      return this;
    }

    public ExceptionTypeBuilder meta(Meta meta) {
      exceptionType.meta = meta;
      return this;
    }

    public ExceptionTypeBuilder code(String code) {
      exceptionType.code = code;
      return this;
    }
    
    public ExceptionTypeBuilder variable(Variable variable) {
      if( exceptionType.variables == null ) {
        exceptionType.variables = new ArrayList<Variable>();
      }
      exceptionType.variables.add(variable);
      return this;
    }

    public ExceptionTypeBuilder variables(List<Variable> variables) {
      for (Variable variable : variables) {
        variable(variable);
      }

      return this;
    }

    public ExceptionTypeBuilder messageText(String lang, String message) {
      if( exceptionType.messages == null ) {
        exceptionType.messages = new ArrayList<Pair<String,String>>();
      }
      exceptionType.messages.add(Pair.of(lang, message));
      return this;
    }

    public ExceptionTypeBuilder messageTexts(List<Pair<String, String>> messageTexts) {
      for (Pair<String, String> messageText : messageTexts) {
        messageText(messageText.getFirst(), messageText.getSecond());
      }

      return this;
    }

    public ExceptionType build() {
      return new ExceptionType(exceptionType);
    }
  }

  public String getFQTypeName() {
    return type.getFQTypeName();
  }
  
}

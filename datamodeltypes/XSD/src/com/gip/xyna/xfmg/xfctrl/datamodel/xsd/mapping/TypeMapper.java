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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException.XynaObjectCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;

public class TypeMapper {
  
  private static Logger logger = CentralFactoryLogging.getLogger(TypeMapper.class);

  private XynaObjectClassInfo xynaObjectClassInfo;
  private List<TypeMapperMember> members = new ArrayList<TypeMapperMember>();
  private HashMap<String,TypeMapperMember> elements;
  private HashMap<String,TypeMapperMember> attributes;
  private TypeMapperMember text;
  private TypeMapper parent;
  private TypeInfo typeInfo;
  private final TypeMapperCache cache;

  public TypeMapper(XynaObjectClassInfo xynaObjectClassInfo, TypeMapperCache cache) {
    this.xynaObjectClassInfo = xynaObjectClassInfo;
    this.cache = cache;
  }

  @Override
  public String toString() {
    return "TypeMapper("+typeInfo.getName()+","+xynaObjectClassInfo.getClassName()+")";
  }

  public String getTypeClassName() {
    return xynaObjectClassInfo.getClassName();
  }
  
  public XynaObject createEmptyXynaObject() throws XynaObjectCreationException {
    try {
      XynaObject xo = instantiate(xynaObjectClassInfo.getXynaObjectClass());
      return xo;
    } catch( XynaObjectCreationException xoce ) {
      if( xoce.getType() == null ) {
        xoce.setType(getTypeClassName());
      }
      throw xoce;
    }
  }
  public XynaObject createXynaObject(Node node) throws XynaObjectCreationException {
    if (node instanceof Element) {
      Element el = (Element) node;
      Attr type = el.getAttributeNodeNS("http://www.w3.org/2001/XMLSchema-instance", "type");
      if (type != null) {
        FQName xsiTypeFQName = FQName.parse(type.getValue(), node);
        if (!xsiTypeFQName.equals(this.typeInfo.getName())) {
          return getTypeMapperFor(xsiTypeFQName).createXynaObject(node);
        }
      }
    }
    try {
      XynaObject xo = instantiate(xynaObjectClassInfo.getXynaObjectClass());
      
      for( Node child : getNodeChildren(node) ) {
        TypeMapperMember member = getMember(child);
        if( member != null ) {
          member.fillXynaObjectWithData(xo, child);
        }
      }
      return xo;
    } catch( XynaObjectCreationException xoce ) {
      if( xoce.getType() == null ) {
        xoce.setType(getTypeClassName());
      }
      throw xoce;
    }
  }


  private TypeMapper getTypeMapperFor(FQName type) {
    TypeMapper mapper = cache.getTypeMapperFor(type);
    if (mapper == null) {
      throw new RuntimeException("No TypeMapper found for xsi:type=" + type);
    }
    return mapper;
  }

  public static List<org.w3c.dom.Node> getNodeChildren(org.w3c.dom.Node node) {
    List<org.w3c.dom.Node> list = new ArrayList<org.w3c.dom.Node>();
    NodeList nl = node.getChildNodes();
    if( nl.getLength() == 1 ) {
      list.add( nl.item(0) );
    } else {
      for( int n=0; n<nl.getLength(); ++n) {
        if( nl.item(n) instanceof org.w3c.dom.Text ) {
          //Text zwischen den Tags ignorieren
        } else {
          list.add( nl.item(n) );
        }
      }
    }
    NamedNodeMap nnm = node.getAttributes();
    for( int n=0; n<nnm.getLength(); ++n ) {
      String namespaceURI = nnm.item(n).getNamespaceURI();
      if( namespaceURI == null ) {
        list.add( nnm.item(n) );
      } else if( namespaceURI.equals( "http://www.w3.org/2000/xmlns/") ) {
        //verhindert xmlns
      } else if( namespaceURI.equals( "http://www.w3.org/2001/XMLSchema-instance") ) {
        //verhindert schemaLocation
      } else {
        list.add( nnm.item(n) );
      }
    }
    return list;
  }

  private XynaObject instantiate(Class<? extends XynaObject> clazz) throws XynaObjectCreationException {
    XynaObject xo;
    try {
      xo = clazz.newInstance();
    } catch (Exception e) {
      throw new XynaObjectCreationException(XynaObjectCreationFailure.Instantiation, clazz.getName(), e );
    }
    return xo;
  }
  
  private TypeMapperMember getMember(Node child) {
    if( child instanceof org.w3c.dom.Element ) {
      return getElement(child.getLocalName());
    } else if( child instanceof org.w3c.dom.Attr ) {
      return getAttribute(child.getLocalName());
    } else if( child instanceof org.w3c.dom.Text ) {
      return text;
    } else {
      logger.info( "Unexpected node of type "+child.getClass().getCanonicalName()
                   +" with interfaces "+ Arrays.asList(child.getClass().getInterfaces())
                   +" and data "+child.getTextContent() );
      return null;
    }
  }

  private TypeMapperMember getMemberByName(String name) {
    TypeMapperMember tmm = null;
    if( elements != null ) {
      tmm = elements.get(name);
      if( tmm != null ) {
        return tmm;
      }
    }
    if( attributes != null ) {
      tmm = attributes.get(name);
      if( tmm != null ) {
        return tmm;
      }
    }
    return null;
  }
  
  
  private TypeMapperMember getElement(String name) {
    TypeMapperMember el = null;
    if( elements != null ) {
      el = elements.get(name);
    }
    if( el == null && parent != null ) {
      el = parent.getElement(name);
    }
    if( el == null ) {
      logger.info( "Searched Element "+name+" but found only "+(elements==null?"[]":elements.keySet().toString())+" in "+typeInfo.getName());
    }
    return el;
  }
  
  private TypeMapperMember getAttribute(String name) {
    TypeMapperMember attr = null;
    if( attributes != null ) {
      attr = attributes.get(name);
    }
    if( attr == null && parent != null ) {
      attr = parent.getAttribute(name);
    }
    if( attr == null ) {
      logger.info( "Searched Attribute "+name+" but found only "+(attributes==null?"[]":attributes.keySet().toString())+" in "+typeInfo.getName());
    }
    return attr;
  }

  public void initialize(TypeInfo typeInfo) {
    this.typeInfo = typeInfo;
    for( TypeInfoMember tim : typeInfo.getMembers() ) {
      TypeMapperMember tmm = new TypeMapperMember(xynaObjectClassInfo,tim);
      initializeMember(tmm);
    }
  }

  private void initializeMember(TypeMapperMember tmm) {
    int position = tmm.getPosition();
    for( int i=members.size(); i<position+1; ++i ) {
      members.add(null); //TODO RandomAccessArrayList...
    }
    members.set( position, tmm );
    switch( tmm.getType() ) {
      case Element:
        initializeElement(tmm.getName(), tmm);
        break;
      case Attribute:
        if( attributes == null ) {
          attributes = new HashMap<String,TypeMapperMember>();
        }
        attributes.put( tmm.getName().getName(), tmm);
        break;
      case Text:
        this.text = tmm;
        break;
      case Choice:
        initializeElement(tmm.getName(), tmm);
        for( Pair<FQName, TypeInfo> cm : tmm.getChoiceMember() ) {
          initializeElement( cm.getFirst(), tmm );
        }
        break;
      case Any:
        initializeElement(tmm.getName(), tmm);
        break;
    }
  }
  
  private void initializeElement(FQName name, TypeMapperMember tmm) {
    if( elements == null ) {
      elements = new HashMap<String,TypeMapperMember>();
    }
    elements.put( name.getName(), tmm);
  }

  public void initializeSetter(SetterGetterCreator setterGetterCreator) throws TypeMapperCreationException {
    String parentFQClassName = xynaObjectClassInfo.getParentXynaObjectClassName();
    if( parentFQClassName == null ) {
      logger.info(xynaObjectClassInfo + " has no parent" );
      parent = null;
    } else {
      parent = setterGetterCreator.getTypeMapper(parentFQClassName);
    }
    try {
      for( TypeMapperMember tmm : members ) {
        if( tmm != null ) {
          tmm.initialize(setterGetterCreator);
        }
      }
    } catch( TypeMapperCreationException tmce ) {
      tmce.setName( typeInfo.getName().toString() );
      throw tmce;
    }
  }

  public FQName getName() {
    return typeInfo.getName();
  }

  public List<FQName> getRootElements() {
    return typeInfo.getRootElements(); 
  }
  
  public Element fillXmlElement(XmlContext xmlContext, Element element, XynaObject xo) throws XmlCreationException {
    try {
      addMembers(xmlContext, element, xo);
      return element;
    } catch( XmlCreationException xce ) {
      if( xce.getType() == null ) {
        xce.setType(getTypeClassName());
      }
      throw xce;
    }
  }

  private void addMembers(XmlContext xmlContext, Element element, XynaObject xo) throws XmlCreationException {
    if( parent != null ) {
      parent.addMembers(xmlContext, element, xo);
    }
    for( TypeMapperMember member : members ) {
      member.addXml(xmlContext, element, xo);
    }
  }

  public XynaObjectClassInfo getXynaObjectClassInfo() {
    return xynaObjectClassInfo;
  }

  public void fillMember(XynaObject xo, String fieldName, String value) throws XynaObjectCreationException {
    TypeMapperMember member = getMemberByName(fieldName);
    if( member == null ) {
      logger.warn("Could not fillMember "+fieldName+": not found" );
      return;
    }
    member.fillXynaObjectWithString(xo, value);
  }

  public void fillMember(XynaObject xo, String fieldName, XynaObject value) throws XynaObjectCreationException {
    TypeMapperMember member = getMemberByName(fieldName);
    if( member == null ) {
      logger.warn("Could not fillMember "+fieldName+": not found" );
      return;
    }
    member.fillXynaObjectWithXynaObject(xo, value);
  }

  public String getMemberString(XynaObject xo, String fieldName, CreateXmlOptions cxo) throws XmlCreationException {
    TypeMapperMember member = getMemberByName(fieldName);
    if( member == null ) {
      logger.warn("Could not getMemberString "+fieldName+": not found" );
      return null;
    }
    return member.getString(xo,cxo);
  }

  public XynaObject getMemberXynaObject(XynaObject xo, String fieldName, CreateXmlOptions cxo) throws XmlCreationException {
    TypeMapperMember member = getMemberByName(fieldName);
    if( member == null ) {
      logger.warn("Could not getMemberXynaObject "+fieldName+": not found" );
      return null;
    }
    return member.getXynaObject(xo, cxo);
  }


}

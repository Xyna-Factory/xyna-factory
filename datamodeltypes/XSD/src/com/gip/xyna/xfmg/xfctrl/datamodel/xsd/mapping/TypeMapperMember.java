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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.AnySetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.ChoiceSetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.ListSetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.SetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.StringBasedSetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.StringSetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.XynaObjectBasedSetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.SetterGetterCreator.XynaObjectSetterGetter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.SetterGetterException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException.XmlCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException.XynaObjectCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.MemberType;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;


public class TypeMapperMember {
  
  private XynaObjectClassInfo parent;
  private TypeInfoMember typeInfoMember;
  private SetterGetter<?> setterGetter; 
  
  
  public TypeMapperMember(XynaObjectClassInfo parent, TypeInfoMember typeInfoMember) {
    this.parent = parent;
    this.typeInfoMember = typeInfoMember;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Member(").append(typeInfoMember).append(",").append(typeInfoMember.getVarName()).append(")");
    return sb.toString();
  }

  public FQName getName() {
    return typeInfoMember.getName();
 }
 
  public MemberType getType() {
    return typeInfoMember.getMemberType();
  }

  public int getPosition() {
    return typeInfoMember.getPosition();
  }
  
  public List<Pair<FQName, TypeInfo>> getChoiceMember() {
    return typeInfoMember.getChoiceMember();
  }

  public void initialize(SetterGetterCreator setterGetterCreator) throws TypeMapperCreationException {
    setterGetter = setterGetterCreator.createSetterGetter( typeInfoMember, parent);
  }
  
  public void fillXynaObjectWithData(XynaObject xo, Node node) throws XynaObjectCreationException {
    try {
      setterGetter.fill(xo, node);
    } catch( SetterGetterException e ) {
      throw new XynaObjectCreationException( XynaObjectCreationFailure.Creation, typeInfoMember.getName().toString(), e );
    }
  }
  
  public void addXml(XmlContext xmlContext, Element element, XynaObject xo) throws XmlCreationException {
    try {
      switch( typeInfoMember.getMemberType() ) {
      case Element:
        addXmlElement(xmlContext, element, xo);
        break;
      case Attribute:
        addXmlAttribute(xmlContext, element, xo);
        break;
      case Text:
        addXmlText(xmlContext, element, xo);
        break;
      case Choice:
        addXmlChoice(xmlContext, element, xo);
        break;
      case Any:
        addXmlAny(xmlContext, element, xo);
        break;
      }
    } catch( SetterGetterException e ) {
      throw new XmlCreationException( XmlCreationFailure.Creation, getName().toString(), e);
    }
  }

  private void addXmlAttribute(XmlContext xmlContext, Element parent, XynaObject xoParent) throws XmlCreationException, SetterGetterException {
    Attr attr = xmlContext.createAttribute(typeInfoMember.isQualified(), getName());
    if( setterGetter != null ) {
      Object o = ((SetterGetter<?>)setterGetter).get(xoParent);
      if( o != null ) {
        attr.setValue( String.valueOf(o) );
      } else {
        if( typeInfoMember.isOptional() ) {
          return; //optionale attribute, die null-wertig sind, nicht rendern?!
        } else {
          attr.setValue( null );
        }
      }
    }
    parent.setAttributeNodeNS(attr);
  }
  
  private void addXmlElement(XmlContext xmlContext, Element parent, XynaObject xoParent) throws XmlCreationException, SetterGetterException {
    if( setterGetter instanceof StringSetterGetter ) {
      addXmlNode( xmlContext, parent, ((StringSetterGetter<?>)setterGetter).getString(xoParent, xmlContext.getCreateXmlOptions()) );
    } else if( setterGetter instanceof XynaObjectSetterGetter ) {
      TypeMapper typeMapper = ((XynaObjectSetterGetter<?>)setterGetter).getTypeMapper();
      XynaObject xo = ((XynaObjectSetterGetter<?>)setterGetter).getXynaObject(xoParent);
      addXmlNode( xmlContext, parent, typeMapper, xo, getName() );

    } else if( setterGetter instanceof ListSetterGetter ) {
      ListSetterGetter<?,?> listSG = (ListSetterGetter<?,?>)setterGetter;
      SetterGetter<?> baseSG = listSG.getBaseSetterGetter();
      List<?> list = listSG.getListForXml(xoParent, xmlContext.getCreateXmlOptions());
      
      if( baseSG instanceof StringBasedSetterGetter ) {
        @SuppressWarnings("unchecked")
        List<String> stringList = (List<String>)list; 
        for( String value : stringList ) {
          addXmlNode(xmlContext, parent, value );
        }
      } else if( baseSG instanceof XynaObjectBasedSetterGetter ) {
        @SuppressWarnings("unchecked")
        XynaObjectBasedSetterGetter<XynaObject> xoSG = (XynaObjectBasedSetterGetter<XynaObject>)baseSG;
        TypeMapper typeMapper = xoSG.getTypeMapper();
        @SuppressWarnings("unchecked")
        List<XynaObject> xynaObjectList = (List<XynaObject>)list;
        for( XynaObject xo : xynaObjectList ) {
          addXmlNode(xmlContext, parent, typeMapper, xo, getName());
        }
      }
    } else {
      throw new IllegalStateException( "Unexpected setterGetter "+setterGetter);
    }
  }

  private void addXmlText(XmlContext xmlContext, Element parent, XynaObject xoParent) throws XmlCreationException, SetterGetterException {
    String value = null;
    if( setterGetter != null ) {
      Object o = ((SetterGetter<?>)setterGetter).get(xoParent);
      if( o != null ) {
        value = String.valueOf(o);
      }
    }
    parent.setTextContent(value);
  }

  private void addXmlNode(XmlContext xmlContext, Element parent, String value) {
    if( value != null ) {
      Element el = xmlContext.createElement(typeInfoMember.isQualified(), typeInfoMember.getName());
      el.setTextContent( value );
      parent.appendChild(el);
    } else {
      if( typeInfoMember.isOptional() ) {
        return; //einfach weglassen
      } else {
        if( xmlContext.getCreateXmlOptions().omitNullTags() ) {
          return; //einfach weglassen 
        } else {
          Element el = xmlContext.createElement(typeInfoMember.isQualified(), typeInfoMember.getName());
          el.setTextContent( value );
          parent.appendChild(el);
        }
      }
    }
  }

  private void addXmlNode(XmlContext xmlContext, Element parent, TypeMapper typeMapper, XynaObject xo, FQName name) throws XmlCreationException {
    Element el;
    if( xo != null ) {
      el = typeMapper.fillXmlElement(xmlContext, xmlContext.createElement(typeInfoMember.isQualified(), name), xo);
    } else {
      if( typeInfoMember.isOptional() ) {
        return; //einfach weglassen
      } else {
        if( xmlContext.getCreateXmlOptions().omitNullTags() ) {
          return; //einfach weglassen 
        } else {
          el = xmlContext.createElement(typeInfoMember.isQualified(), name);
        }
      }
    }
    parent.appendChild(el);
  }

  private void addXmlChoice(XmlContext xmlContext, Element parent, XynaObject xoParent) throws SetterGetterException, XmlCreationException {
    if( setterGetter instanceof ChoiceSetterGetter ) {
      ChoiceSetterGetter<?> choiceSetterGetter = (ChoiceSetterGetter<?>)setterGetter;
      XynaObject xo = choiceSetterGetter.getXynaObject(xoParent);
      if( xo != null ) {
        Pair<FQName,TypeMapper> pair = choiceSetterGetter.getChoiceData(xo);
        addXmlNode( xmlContext, parent, pair.getSecond(), xo, pair.getFirst());
      } else {
        addXmlNode( xmlContext, parent, null, xo, getName());
      }
    } else {
      throw new IllegalStateException( "Unexpected setterGetter "+setterGetter);
    }
  }

  private void addXmlAny(XmlContext xmlContext, Element parent, XynaObject xoParent) throws SetterGetterException, XmlCreationException {
    if( setterGetter instanceof AnySetterGetter ) {
      AnySetterGetter anySetterGetter = (AnySetterGetter)setterGetter;
      XynaObject xo = anySetterGetter.getXynaObject(xoParent);
      boolean elementWritten = anySetterGetter.addXmlAny(xmlContext, parent, xmlContext.createElement(typeInfoMember.isQualified(), getName()), xo);
      if( ! elementWritten ) {
        addXmlNode( xmlContext, parent, null, null, getName() );
      }
    } else if( setterGetter instanceof ListSetterGetter ) {
      @SuppressWarnings("unchecked")
      ListSetterGetter<XynaObject,XynaObject> listSG = (ListSetterGetter<XynaObject,XynaObject>)setterGetter;
      List<XynaObject> xol = listSG.getListForXml(xoParent, xmlContext.getCreateXmlOptions());
      AnySetterGetter anySetterGetter = (AnySetterGetter)listSG.getBaseSetterGetter();
      for( XynaObject xo : xol) {
        boolean elementWritten = anySetterGetter.addXmlAny(xmlContext, parent, xmlContext.createElement(typeInfoMember.isQualified(), getName()), xo);
        if( ! elementWritten ) {
          addXmlNode( xmlContext, parent, null, null, getName() );
        }
      }
    } else {
      throw new IllegalStateException( "Unexpected setterGetter "+setterGetter);
    }
  }
  
  public void fillXynaObjectWithString(XynaObject xo, String value) throws XynaObjectCreationException {
    try {
      if( setterGetter instanceof StringBasedSetterGetter ) {
        ((StringBasedSetterGetter<?>)setterGetter).setString(xo, value);
      } else {
        throw new XynaObjectCreationException( XynaObjectCreationFailure.Creation, typeInfoMember.getName().toString() );
      }
    } catch( SetterGetterException e ) {
      throw new XynaObjectCreationException( XynaObjectCreationFailure.Creation, typeInfoMember.getName().toString(), e );
    }
  }
  
  public void fillXynaObjectWithXynaObject(XynaObject xo, XynaObject value) throws XynaObjectCreationException {
    try {
      if( setterGetter instanceof XynaObjectBasedSetterGetter ) {
        ((XynaObjectBasedSetterGetter<?>)setterGetter).setXynaObject(xo, value);
      } else {
        throw new XynaObjectCreationException( XynaObjectCreationFailure.Creation, typeInfoMember.getName().toString() );
      }
    } catch( SetterGetterException e ) {
      throw new XynaObjectCreationException( XynaObjectCreationFailure.Creation, typeInfoMember.getName().toString(), e );
    }
  }
  
  public String getString(XynaObject xo, CreateXmlOptions cxo) throws XmlCreationException {
    try {
      if( setterGetter instanceof StringBasedSetterGetter ) {
        return ((StringBasedSetterGetter<?>)setterGetter).getString(xo, cxo);
      } else {
        throw new XmlCreationException( XmlCreationFailure.Creation, typeInfoMember.getName().toString() );
      }
    } catch( SetterGetterException e ) {
      throw new XmlCreationException( XmlCreationFailure.Creation, typeInfoMember.getName().toString(), e );
    }
  }
  
  public XynaObject getXynaObject(XynaObject xo, CreateXmlOptions cxo) throws XmlCreationException {
    try {
      if( setterGetter instanceof XynaObjectBasedSetterGetter ) {
        return ((XynaObjectBasedSetterGetter<?>)setterGetter).getXynaObject(xo);
      } else {
        throw new XmlCreationException( XmlCreationFailure.Creation, typeInfoMember.getName().toString() );
      }
    } catch( SetterGetterException e ) {
      throw new XmlCreationException( XmlCreationFailure.Creation, typeInfoMember.getName().toString(), e );
    }
  }
  
}

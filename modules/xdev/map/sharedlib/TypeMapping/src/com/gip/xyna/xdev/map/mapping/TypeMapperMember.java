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
package com.gip.xyna.xdev.map.mapping;

import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.map.mapping.SetterGetterCreator.ChoiceSetterGetter;
import com.gip.xyna.xdev.map.mapping.SetterGetterCreator.ListSetterGetter;
import com.gip.xyna.xdev.map.mapping.SetterGetterCreator.SetterGetter;
import com.gip.xyna.xdev.map.mapping.SetterGetterCreator.StringSetterGetter;
import com.gip.xyna.xdev.map.mapping.SetterGetterCreator.XynaObjectListSetterGetter;
import com.gip.xyna.xdev.map.mapping.SetterGetterCreator.XynaObjectSetterGetter;
import com.gip.xyna.xdev.map.mapping.exceptions.SetterGetterException;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.XynaObjectCreationException.XynaObjectCreationFailure;
import com.gip.xyna.xdev.map.types.FQName;
import com.gip.xyna.xdev.map.types.MemberType;
import com.gip.xyna.xdev.map.types.TypeInfo;
import com.gip.xyna.xdev.map.types.TypeInfoMember;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class TypeMapperMember {
  
  private XynaObjectClassInfo parent;
  private TypeInfoMember typeInfoMember;
  //private SetterGetter<?> setterGetter; //TODO eigenartige Compile-Probleme "inconvertible types"
  private Object setterGetter;            //mit ant und java 5 werden hiermit vermieden 
  
  
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

  public String getName() {
     return typeInfoMember.getName().getName();
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
    setterGetter = setterGetterCreator.createSetterGetter( typeInfoMember.getVarName(), parent, typeInfoMember.getChoiceMember());
  }

  public void addXmlAttribute(Document doc, Element parent, XynaObject xoParent, CreateXmlOptions createXmlOptions) throws XmlCreationException, SetterGetterException {
    Attr attr = doc.createAttributeNS( typeInfoMember.getQualifiedNamespace(), typeInfoMember.getName().getName() );
    if( setterGetter != null ) {
      Object o = ((SetterGetter<?>)setterGetter).get(xoParent);
      if( o != null ) {
        attr.setValue( String.valueOf(o) );
      } else {
        if( !typeInfoMember.isOptional() ) {
          attr.setValue( null );
        }
      }
    }
    parent.setAttributeNodeNS(attr);
  }
  
  public void addXmlElement(Document doc, Element parent, XynaObject xoParent, CreateXmlOptions createXmlOptions) throws XmlCreationException, SetterGetterException {
    if( setterGetter instanceof StringSetterGetter ) {
      addXmlNode(doc, parent, ((StringSetterGetter<?>)setterGetter).getString(xoParent, createXmlOptions), createXmlOptions );
    } else if( setterGetter instanceof XynaObjectSetterGetter ) {
      TypeMapper typeMapper = ((XynaObjectSetterGetter<?>)setterGetter).getTypeMapper();
      XynaObject xo = ((XynaObjectSetterGetter<?>)setterGetter).getXynaObject(xoParent);
      addXmlNode( doc, parent, typeMapper, xo, getName(), createXmlOptions );

    } else if( setterGetter instanceof ListSetterGetter ) {
      List<String> list = ((ListSetterGetter<?>)setterGetter).getStringList(xoParent, createXmlOptions);
      if( list != null ) {
        for( String value : list ) {
          addXmlNode(doc, parent, value, createXmlOptions );
        }
      }

    } else if( setterGetter instanceof XynaObjectListSetterGetter ) {
      TypeMapper typeMapper = ((XynaObjectListSetterGetter<?>)setterGetter).getTypeMapper();
      List<XynaObject> children = ((XynaObjectListSetterGetter<?>)setterGetter).getXynaObjectList(xoParent);
      if( children != null ) {
        for( XynaObject xo : children ) {
          addXmlNode(doc, parent, typeMapper, xo, getName(), createXmlOptions);
        }
      }
    } else {
      throw new IllegalStateException( "Unexpected setterGetter "+setterGetter);
    }
  }

  public void addXmlText(Document doc, Element parent, XynaObject xoParent, CreateXmlOptions createXmlOptions) throws XmlCreationException, SetterGetterException {
    String value = null;
    if( setterGetter != null ) {
      Object o = ((SetterGetter<?>)setterGetter).get(xoParent);
      if( o != null ) {
        value = String.valueOf(o);
      }
    }
    parent.setTextContent(value);
  }

  
  
  public void fillXynaObjectWithData(XynaObject xo, Node node) throws XynaObjectCreationException {
    try {
      if( setterGetter instanceof StringSetterGetter ) {
        
        String value;
        if (node instanceof Element) {
          value = XMLUtils.getTextContent((Element) node);
        } else {
          value = node.getTextContent();
        }
        ((StringSetterGetter<?>)setterGetter).setString( xo, value );
      } else if( setterGetter instanceof XynaObjectSetterGetter ) {
        TypeMapper typeMapper = ((XynaObjectSetterGetter<?>)setterGetter).getTypeMapper();
        XynaObject xoChild = typeMapper.createXynaObject(node);
        ((XynaObjectSetterGetter<?>)setterGetter).setXynaObject( xo, xoChild);
      } else if( setterGetter instanceof ListSetterGetter ) {
        String value;
        if (node instanceof Element) {
          value = XMLUtils.getTextContent((Element) node);
        } else {
          value = node.getTextContent();
        }
        ((ListSetterGetter<?>)setterGetter).addString( xo, value );
      } else if( setterGetter instanceof XynaObjectListSetterGetter ) {
        TypeMapper typeMapper = ((XynaObjectListSetterGetter<?>)setterGetter).getTypeMapper();
        XynaObject xoChild = typeMapper.createXynaObject(node);
        ((XynaObjectListSetterGetter<?>)setterGetter).addXynaObject( xo, xoChild );
      } else if( setterGetter instanceof ChoiceSetterGetter ) {
        TypeMapper typeMapper = ((ChoiceSetterGetter<?>)setterGetter).getTypeMapper(node);
        XynaObject xoChild = typeMapper.createXynaObject(node);
        ((ChoiceSetterGetter<?>)setterGetter).setXynaObject( xo, xoChild);        
      } else {
        throw new IllegalStateException( "Unexpected setterGetter "+setterGetter +" for "+this);
      }
    } catch( SetterGetterException e ) {
      throw new XynaObjectCreationException( XynaObjectCreationFailure.Creation, typeInfoMember.getName().toString(), e );
    }
  }

  
  private void addXmlNode(Document doc, Element parent, String value, CreateXmlOptions createXmlOptions) {
    if( value != null && value.length() != 0 ) {
      Element el = createElement(doc, getName());
      el.setTextContent( value );
      parent.appendChild(el);
    } else {
      if( typeInfoMember.isOptional() ) {
        return; //einfach weglassen
      } else {
        if( createXmlOptions.omitNullTags() ) {
          return; //einfach weglassen 
        } else {
          Element el = createElement(doc, getName());
          el.setTextContent( value );
          parent.appendChild(el);
        }
      }
    }
  }

  private Element createElement(Document doc, String name) {
    return doc.createElementNS(typeInfoMember.getQualifiedNamespace(), name);
  }

  private void addXmlNode(Document doc, Element parent, TypeMapper typeMapper, XynaObject xo, String name, CreateXmlOptions createXmlOptions) throws XmlCreationException {
    Element el;
    if( xo != null ) {
      el = typeMapper.fillXmlElement(doc, createElement(doc, name), xo, createXmlOptions);
    } else {
      if( typeInfoMember.isOptional() ) {
        return; //einfach weglassen
      } else {
        if( createXmlOptions.omitNullTags() ) {
          return; //einfach weglassen 
        } else {
          el = createElement(doc, name);
        }
      }
    }
    parent.appendChild(el);
  }

  public void addXmlChoice(Document doc, Element parent, XynaObject xoParent, CreateXmlOptions createXmlOptions) throws SetterGetterException, XmlCreationException {
    if( setterGetter instanceof ChoiceSetterGetter ) {
      ChoiceSetterGetter<?> choiceSetterGetter = (ChoiceSetterGetter<?>)setterGetter;
      XynaObject xo = choiceSetterGetter.getXynaObject(xoParent);
      if( xo != null ) {
        Pair<String,TypeMapper> pair = choiceSetterGetter.getChoiceData(xo);
        addXmlNode( doc, parent, pair.getSecond(), xo, pair.getFirst(), createXmlOptions);
      } else {
        addXmlNode( doc, parent, null, xo, getName(), createXmlOptions);
      }
    } else {
      throw new IllegalStateException( "Unexpected setterGetter "+setterGetter);
    }
  }


  
}

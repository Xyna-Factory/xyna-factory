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
package com.gip.xyna.xdev.map.xmlparser;



import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;





//FIXME xerces als lib besser als xerces in com.sun - package
public class XSComplexType { //bzw complex type


  XSComplexTypeDefinition type;


  public XSComplexType(XSComplexTypeDefinition type) {
    this.type = type;
  }


  public String getTypeName() {
    return type.getName();
  }


  public XSComplexType getChildType(String namespace, String name) {
    List<XSElementDeclaration> elements = new ArrayList<XSElementDeclaration>();
    addChildElements(type, elements);
    for (XSElementDeclaration el : elements) {
      if (el.getNamespace().equals(namespace) && el.getName().equals(name)) {
        XSTypeDefinition typeDef = el.getTypeDefinition();
        if (typeDef instanceof XSComplexTypeDefinition) {
          return new XSComplexType((XSComplexTypeDefinition) typeDef);
        } else if (typeDef instanceof XSSimpleTypeDefinition) {
          return new XSComplexType(null);
        } else {
          throw new RuntimeException("unsupported child type : " + typeDef.getClass().getName());
        }
      }
    }
    return null;
  }


  //kinder eines complextypes, eines elements, oder einer group hinzufügen
  private void addChildElements(XSObject o, List<XSElementDeclaration> l) {
    if (o instanceof XSComplexTypeDefinition) {
      XSComplexTypeDefinition ct = (XSComplexTypeDefinition) o;
      XSParticle particle = ct.getParticle();
      if (particle != null) {
        XSTerm term = particle.getTerm();
        addChildElements(term, l);
      }
    } else if (o instanceof XSModelGroup) {
      XSModelGroup group = (XSModelGroup) o;
      XSObjectList children = group.getParticles();
      for (int i = 0; i < children.getLength(); i++) {
        XSObject child = children.item(i);
        if (child instanceof XSParticle) {
          XSParticle particle = (XSParticle) child;
          XSTerm term = particle.getTerm();
          addChildElements(term, l);
        } else {
          throw new RuntimeException("unsupported particle type");
        }
      }
    } else if (o instanceof XSElementDeclaration) {
      XSElementDeclaration el = (XSElementDeclaration) o;
      l.add(el);
      XSTypeDefinition type = el.getTypeDefinition();
      if (type instanceof XSComplexTypeDefinition) {
        addChildElements(type, l);
      }
    } else if (o instanceof XSWildcard) {
      //ntbd
    } else {
      throw new RuntimeException("unsupported type of child of group: " + o.getClass().getName());
    }

  }


  public boolean isChildAnAttribute(String namespace, String name) {
    XSObjectList atts = type.getAttributeUses();
    for (int i = 0; i < atts.getLength(); i++) {
      XSObject att = atts.item(i);
      if (att instanceof XSAttributeUse) {
        XSAttributeUse xsatt = (XSAttributeUse) att;
        if (xsatt.getName().equals(name)) {
          return true;
        }
      } else {
        throw new RuntimeException();
      }
    }
    return false;
  }


  public String getNameSpace() {
    return type.getNamespace();
  }


  public boolean isComplexType() {
    return type != null;
  }

}

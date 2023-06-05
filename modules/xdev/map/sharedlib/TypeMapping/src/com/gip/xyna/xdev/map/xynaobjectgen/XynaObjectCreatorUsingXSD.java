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
package com.gip.xyna.xdev.map.xynaobjectgen;



import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.map.TypeMappingCache;
import com.gip.xyna.xdev.map.exceptions.TypeMappingInstantiationException;
import com.gip.xyna.xdev.map.xmlparser.XElement;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



/**
 * 
 * verwendung:
 * <pre>
 * //schema und creator wiederverwenden!
 * XSchema schema = XSchema.parse(new String[]{"/tmp/myxsd.xsd"}); 
 * XynaObjectCreatorUsingXSD objectCreator = new XynaObjectCreatorUsingXSD();

 * //eigentliches parsen des xmls
 * XDocument doc = new XDocument(xmlString);
 * doc.setSchema(schema);
 * XynaObject xo = objectCreator.createXynaObjectsForData(targetId, doc.getDocumentElement(), null);
 * </pre>
 */
public class XynaObjectCreatorUsingXSD {
  //TODO andere xynaobjectcreator wiederverwenden

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaObjectCreatorUsingXSD.class);


  public static final class MissingTypeMappingException extends RuntimeException {

    public MissingTypeMappingException(String targetId, String key) {
      super("missing type mapping for target=" + targetId + " and key=" + key);
    }


    private static final long serialVersionUID = 1L;

  }


  private final TypeMappingCache typeMappingCache;


  public XynaObjectCreatorUsingXSD() throws PersistenceLayerException {
    typeMappingCache = new TypeMappingCache();
  }


  public XynaObjectCreatorUsingXSD(TypeMappingCache typeMappingCache) {
    this.typeMappingCache = typeMappingCache;
  }


  private Object transformType(Object value, Class<?> type) {
    if (type.isAssignableFrom(value.getClass())) {
      return value;
    } else if (type == int.class || type == Integer.class) {
      return Integer.valueOf(value.toString());
    } else if (type == String.class) {
      if (value instanceof Double) {
        //aus 1.0 mache "1", nicht "1.0".
        Double d = (Double) value;
        if (d == Math.floor(d)) {
          return String.valueOf(Math.round(d));
        }
      } else if (value instanceof Float) {
        Float d = (Float) value;
        if (d == Math.floor(d)) {
          return String.valueOf(Math.round(d));
        }
      }
      return String.valueOf(value);
    } else if (type == boolean.class || type == Boolean.class) {
      return Boolean.valueOf(value.toString());
    } else if (type == long.class || type == Long.class) {
      return Long.valueOf(value.toString());
    }
    throw new RuntimeException("unsupported transformation of " + value.getClass().getName() + " to " + type.getName());
  }


  private Class<?> getGenericTypeOfList(Field f) {
    return (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
  }


  private String transformXMLToMemberVarName(String targetId, String ns, String typeName, String memberVarName,
                                             boolean isAttribute) {
    String ae;
    if (isAttribute) {
      ae = ":a";
    } else {
      ae = ":e";
    }
    String ret = typeMappingCache.lookup(targetId, ns + ":" + typeName + ":" + memberVarName + ae);
    if (ret == null) {
      throw new MissingTypeMappingException(targetId, ns + ":" + typeName + ":" + memberVarName + ae);
    }
    return ret.substring(ret.indexOf(':') + 1);
  }


  /*
    public XynaObject createXynaObject(String targetId, String namespace, String typeName) {
      Class<? extends XynaObject> clazz =
          typeMappingCache.lookupClass(getClass().getClassLoader(), targetId, namespace + ":" + typeName);
      if (clazz == null) {
        throw new MissingTypeMappingException(targetId, namespace + ":" + typeName);
      }
      XynaObject o;
      try {
        o = clazz.newInstance();
      } catch (InstantiationException e1) {
        throw new RuntimeException(e1);
      } catch (IllegalAccessException e1) {
        throw new RuntimeException(e1);
      }

      return o;
    }*/


  /**
   * setzt value in o, falls nicht bereits vorhanden
   */
  private String setMemberVar(String targetId, XynaObject o, String ns, String typeName, String memberVarName,
                              Object value, boolean isAttribute) throws TypeMappingInstantiationException,
      NoSuchFieldException {
    if (ns != null) { //FIXME unschön
      memberVarName = transformXMLToMemberVarName(targetId, ns, typeName, memberVarName, isAttribute);
    }
    //type herausfinden, dann danach casten und wert setzen.
    Class<?> type = null;
    try {
      Field f = o.getClass().getDeclaredField(memberVarName);
      f.setAccessible(true);
      type = f.getType();
      if (type == List.class) {
        //wird ggfs mehrfach aufgerufen, entspricht dann einem add zur liste
        Class<?> genericTypeOfList = getGenericTypeOfList(f);
        Method m;
        try {
          m =
              o.getClass().getDeclaredMethod("addTo" + String.valueOf(memberVarName.charAt(0)).toUpperCase()
                                                 + memberVarName.substring(1), genericTypeOfList);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        }
        try {
          m.invoke(o, transformType(value, genericTypeOfList));
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      } else {
        if (!type.isPrimitive() && f.get(o) != null) {
          return memberVarName;
        }
        f.set(o, transformType(value, type));
      }
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NumberFormatException e) {
      throw new TypeMappingInstantiationException(o.getClass().getName() + "." + memberVarName + " must be a "
          + type.getName(), e);
    } catch (IllegalArgumentException e) {
      throw new TypeMappingInstantiationException(o.getClass().getName() + "." + memberVarName + " must be a "
          + type.getName(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return memberVarName;
  }


  public XynaObject createXynaObjectsForData(String targetId, XElement xElement, XElement parentElement)
      throws TypeMappingInstantiationException {
    String ns = xElement.getNamespaceURI();
    String type = xElement.getTypeNameUsedInTypeMapping();

    Class<? extends XynaObject> clazz =
        typeMappingCache.lookupClass(getClass().getClassLoader(), targetId, ns + ":" + type);
    if (clazz == null) {
      throw new MissingTypeMappingException(targetId, ns + ":" + type);
    }
    XynaObject o;
    try {
      o = clazz.newInstance();
    } catch (InstantiationException e1) {
      throw new RuntimeException(e1);
    } catch (IllegalAccessException e1) {
      throw new RuntimeException(e1);
    }
    String contentName = xElement.getLocalName();

    //child-elements
    List<XElement> children = xElement.getChildElements();
    for (XElement childElement : children) {
      if (!childElement.isComplexType()) {
        //dann wird der inhalt direkt als textcontent gesetzt
        try {
          setMemberVar(targetId, o, ns, xElement.getTypeNameUsedInTypeMapping(), childElement.getLocalName(),
                       childElement.getTextContent(), false);
        } catch (NoSuchFieldException e) {
          throw new TypeMappingInstantiationException("did not find field " + childElement.getLocalName()
              + " in xynaobject " + o.getClass().getName(), e);
        }
        continue;
      }
      //ansonsten ist es offenbar ein komplexer typ -> weiter.
      XynaObject child = createXynaObjectsForData(targetId, childElement, xElement);
      try {
        setMemberVar(targetId, o, ns, xElement.getTypeNameUsedInTypeMapping(), childElement.getLocalName(), child,
                     false);
      } catch (NoSuchFieldException e) {
        throw new TypeMappingInstantiationException("did not find field " + childElement.getLocalName()
            + " in xynaobject " + o.getClass().getName(), e);
      }
      if (childElement.getLocalName().equals(contentName)) {
        contentName = contentName + "Content"; //TODO nicht besonders elegant, aber sollte so im zusammenspiel mit der generierung funktionieren. ggfs muss man nochmal checken, ob dies nicht auch noch eine kollision ergibt
      }
    }

    //attributes
    NamedNodeMap attributesMap = xElement.getAttributes();
    for (int i = 0; i < attributesMap.getLength(); i++) {
      Node node = attributesMap.item(i);
      Attr attribute = (Attr) node;
      String value = attribute.getValue();
      String attName = attribute.getLocalName();
      String prefix = attribute.getPrefix();
      if (prefix != null && (prefix.equals("http://www.w3.org/2000/xmlns/") || prefix.equals("http://www.w3.org/2001/XMLSchema-instance"))) {
        continue;
      }
      try {
        setMemberVar(targetId, o, ns, xElement.getTypeNameUsedInTypeMapping(), attName, value, true);
      } catch (NoSuchFieldException e) {
        throw new TypeMappingInstantiationException("did not find field " + attName + " in xynaobject "
            + o.getClass().getName(), e);
      } //FIXME
      if (attName.equals(contentName)) {
        contentName = contentName + "Content"; //TODO nicht besonders elegant, aber sollte so im zusammenspiel mit der generierung funktionieren. ggfs muss man nochmal checken, ob dies nicht auch noch eine kollision ergibt
      }
    }
    String content = xElement.getTextContent();
    if (content != null) {
      content = content.trim();
      try {
        setMemberVar(targetId, o, ns, parentElement == null ? null : parentElement.getTypeNameUsedInTypeMapping(),
                     contentName, content, false);
      } catch (MissingTypeMappingException e) {
        if (content.length() > 0) {
          throw e;
        } else {
          logger.debug("ignoring empty content for xml element " + xElement.getLocalName());
        }
      } catch (NoSuchFieldException e) {
        if (content.length() > 0) {
          throw new TypeMappingInstantiationException("did not find field " + xElement.getLocalName()
              + " in xynaobject " + o.getClass().getName(), e);
        } else {
          logger.debug("ignoring empty content for xml element " + xElement.getLocalName());
        }
      }
    }
    return o;
  }


}

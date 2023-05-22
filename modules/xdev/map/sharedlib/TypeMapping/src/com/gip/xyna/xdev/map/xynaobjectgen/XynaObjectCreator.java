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
package com.gip.xyna.xdev.map.xynaobjectgen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xdev.map.exceptions.XynaObjectCreationException;
import com.gip.xyna.xdev.map.xynaobjectgen.TypeConverterFactory.TypeConverter;
import com.gip.xyna.xdev.map.xynaobjectgen.XynaObjectCreatorCache.TypeMappingCacheForTarget;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;

/**
 * XynaObjectCreator kann XynaObjecte erstelllen, indem XMLs �ber das TypeMapping interpretiert werden.
 *
 * Ein wenig Konfiguration ist �ber die XOCStrategy m�glich.
 *
 * XynaObjectCreator kann zwar auch direkt verwendet werden, ein Aufruf �ber den XynaObjectCreatorCache
 * ist aber besser.
 *
 */
public class XynaObjectCreator {

  private static Logger logger = Logger.getLogger(XynaObjectCreator.class);


  /**
   * Fehlermeldungen (Module in  XynaObjectCreationException)
   */
  public static enum XOCExceptionType {
    Lookup,
    ClassLoading,
    Setter,
    SetterCreation,
    TypeConverter,
    TypeConverterCreation,
    Instantiation;
  }


  private TypeMappingCacheForTarget typeMappingCache;

  private HashMap<String,Setter> setters = new HashMap<String,Setter>();
  private XynaObjectClassInfo xynaObjectClassInfo;
  private XynaObjectCreatorCache xynaObjectCreatorCache;
  private XOCStrategy xocStrategy;
  private String namespace;

  public XynaObjectCreator(XOCStrategy xocStrategy, TypeMappingCacheForTarget typeMappingCache) {
    this.xynaObjectCreatorCache = null;
    this.xocStrategy = xocStrategy;
    this.typeMappingCache = typeMappingCache;
  }

  public XynaObjectCreator(String targetId, String namespace, XynaObjectCreatorCache xynaObjectCreatorCache) {
    this.namespace = namespace;
    this.xynaObjectCreatorCache = xynaObjectCreatorCache;
    this.xocStrategy = xynaObjectCreatorCache.getXocStrategy();
    this.typeMappingCache = new TypeMappingCacheForTarget(xynaObjectCreatorCache.getTypeMappingCache(), targetId);
  }

  public XynaObject createXynaObject(Node node) throws XynaObjectCreationException {
    try {
      XynaObject xo = instantiate(getClass(node));
      for( Node child : getNodeChildren(node) ) {
        Setter setter = getSetter(child);
        if( setter != null ) {
          setter.fillXynaObject( xo, child );
        }
      }
      return xo;
    } catch( XynaObjectCreationException xoce ) {
      //xoce.setType(xoc.getTypeName()); FIXME klappt wegen BUG 15152 nicht, daher WorkAround
      xoce.getArgs()[0] = getTypeName();
      throw xoce;
    }
  }

  public String getTypeName() {
    if( xynaObjectClassInfo != null ) {
      return xynaObjectClassInfo.getClassName();
    }
    return "unknown";
  }

  private Setter getSetter(Node child) throws XynaObjectCreationException {
    boolean isElement = true;
    if( child instanceof org.w3c.dom.Element ) {
      isElement = true;
    } else if( child instanceof org.w3c.dom.Attr ) {
      isElement = false;
      if( "xmlns".equals(child.getNodeName() ) ) {
        //FIXME
        return null;
      }
    } else if( child instanceof org.w3c.dom.Text ) {
      //Text zwischen den Tags ignorieren
      return null;
    } else {
      logger.info( "Unexpected node of type "+child.getClass().getCanonicalName()
          +" with interfaces "+ Arrays.asList(child.getClass().getInterfaces())
          +" and data "+child.getTextContent() );
      return null;
    }
    //ok, valider Node

    String key = child.getLocalName()+(isElement?":e":":a");

    Setter setter = setters.get( key );
    if( setter == null ) {
      setter = createSetter(isElement,child);
      setters.put(key, setter);
    }
    return setter;
  }

  /**
   * @param isElement
   * @param child
   * @return
   * @throws XynaObjectCreationException
   */
  private Setter createSetter(boolean isElement, Node child) throws XynaObjectCreationException {
    String childKey = child.getLocalName() +(isElement?":e":":a");

    String fieldName = xynaObjectClassInfo.getFieldName(typeMappingCache,childKey);
    if( fieldName == null ) {
      if( xocStrategy.canLookupFailureBeIgnored( childKey ) ) {
        return new NOPSetter();
      } else {
        throw new XynaObjectCreationException( "?", XOCExceptionType.Lookup.name(),
                                               xynaObjectClassInfo.getXsdTypes()+":"+childKey);
      }
    } else {
      try {
        Field field = xynaObjectClassInfo.getField(fieldName);

        Class<?> type = field.getType();
        logger.debug( "generating setter \""+fieldName+"\" for "+type.getName() );
        if( type == List.class ) {
          return new ListSetter(field,this,child);
        } else {
          return new ConversionSetter(field, getTypeConverterForType(type,child) );
        }

      } catch (Exception e) {
        //TypeMapping-Tabelle sagt, dass es Feld memberVarName geben sollte, daher unerwartet
        throw new XynaObjectCreationException( "?", XOCExceptionType.SetterCreation.name(), fieldName, e);
      }
    }

  }


  public static List<org.w3c.dom.Node> getNodeChildren(org.w3c.dom.Node node) {
    List<org.w3c.dom.Node> list = new ArrayList<org.w3c.dom.Node>();
    NodeList nl = node.getChildNodes();
    for( int n=0; n<nl.getLength(); ++n) {
      list.add( nl.item(n) );
    }
    NamedNodeMap nnm = node.getAttributes();
    for( int n=0; n<nnm.getLength(); ++n ) {
      list.add( nnm.item(n) );
    }
    return list;
  }

  private Class<? extends XynaObject> getClass(Node node) throws XynaObjectCreationException {
    if( xynaObjectClassInfo == null ) {
      String xmlTypeName = xocStrategy.getTypeName(node);
      if( xmlTypeName == null ) {
        xmlTypeName = typeMappingCache.lookup( namespace+":"+node.getLocalName()+":_root" );
        if( xmlTypeName.endsWith(":_root") ) {
          xmlTypeName = xmlTypeName.substring(0, xmlTypeName.length()-6);
        }
      }
      if( xmlTypeName == null ) {
        throw new XynaObjectCreationException( "?", XOCExceptionType.Lookup.name(), namespace+":"+node.getLocalName() );
      }
      xynaObjectClassInfo = new XynaObjectClassInfo(typeMappingCache, getClass().getClassLoader(), xmlTypeName);
    }
    return xynaObjectClassInfo.getXynaObjectClass();
  }

  private XynaObject instantiate(Class<? extends XynaObject> clazz) throws XynaObjectCreationException {
    XynaObject xo;
    try {
      xo = clazz.newInstance();
    } catch (Exception e) {
      throw new XynaObjectCreationException("?", XOCExceptionType.Instantiation.name(), clazz.getName(), e );
    }
    return xo;
  }

  private TypeConverter<?> getTypeConverterForType( Class<?> type, Node child ) throws XynaObjectCreationException {
    if( type == String.class ) {
      return new TypeConverterFactory.StringConverter();
    } else if( XynaObject.class.isAssignableFrom(type) ) {
      return new TypeConverterFactory.XynaObjectTypeConverter(getXynaObjectCreatorForChild(child));
    } else {
      return TypeConverterFactory.getTypeConverterForType( type );
    }
  }

  private XynaObjectCreator getXynaObjectCreatorForChild(Node child) throws XynaObjectCreationException {
    if( xynaObjectCreatorCache != null ) {
      return xynaObjectCreatorCache.getXynaObjectCreator(typeMappingCache.getTargetId(), namespace, child, xynaObjectClassInfo.getClassName());
    } else {
      return new XynaObjectCreator(xocStrategy, typeMappingCache);
    }
  }

  private static interface Setter {
    public void fillXynaObject(XynaObject xo, Node child) throws XynaObjectCreationException;
  }

  private static class ConversionSetter implements Setter {
    private Field field;
    private TypeConverter<?> typeConverter;

    public ConversionSetter(Field field, TypeConverter<?> typeConverter) {
      this.field = field;
      this.typeConverter = typeConverter;
    }

    public void fillXynaObject(XynaObject xo, Node child) throws XynaObjectCreationException {
      try {
        field.set(xo, typeConverter.convert(child) );
      } catch (XynaObjectCreationException e) {
        throw e;
      } catch (Exception e) {
        throw new XynaObjectCreationException( "?", XOCExceptionType.Setter.name(), field.getName(), e );
      }
    }

  }

  /**
   * Setter kann nicht komplette Liste auf einmal setzen,
   * sondern wird mehrfach aufgerufen und tr�gt Daten mit addTo{VarName}(...) ein
   */
  private static class ListSetter implements Setter {
    private Class<?> listType;
    private Method addToMethod;
    private TypeConverter<?> typeConverter;

    public ListSetter(Field field, XynaObjectCreator xocParent, Node child) throws XynaObjectCreationException {
      listType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
      String memberVarName = field.getName();
      String methodName = "addTo" + memberVarName.substring(0,1).toUpperCase() + memberVarName.substring(1);
      try {
        addToMethod = xocParent.xynaObjectClassInfo.getMethod(methodName, listType);
      } catch (NoSuchMethodException e) {
        throw new XynaObjectCreationException("?", XOCExceptionType.SetterCreation.name(), methodName, e);
      }
      this.typeConverter = xocParent.getTypeConverterForType(listType, child);
    }

    public void fillXynaObject(XynaObject xo, Node child) throws XynaObjectCreationException {
      Object value = typeConverter.convert(child);
      try {
        addToMethod.invoke(xo, value);
      } catch (Exception e) {
        throw new XynaObjectCreationException( "?", XOCExceptionType.Setter.name(), child.getTextContent(), e );
      }
    }
  }

  private static class NOPSetter implements Setter {
    public void fillXynaObject(XynaObject xo, Node child) throws XynaObjectCreationException {
    }
  }

}

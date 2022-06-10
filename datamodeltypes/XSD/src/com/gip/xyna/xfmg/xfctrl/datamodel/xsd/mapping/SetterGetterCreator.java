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

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.Constants;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XercesUtils;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.TypeConverterFactory.TypeConverter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.SetterGetterException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.SetterGetterException.SetterGetterFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException.TypeMapperCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException.XmlCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException.XynaObjectCreationFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 *
 */
public class SetterGetterCreator {

  private static Logger logger = CentralFactoryLogging.getLogger(SetterGetterCreator.class);
  
  private Map<String, TypeMapper> typeMappers;
  private TypeMapperForType typeMapperForType;
  
  public SetterGetterCreator(Map<String, TypeMapper> typeMappers) {
    this.typeMappers = typeMappers;
    this.typeMapperForType = new TypeMapperForType(typeMappers);
  }
  
  public TypeMapper getTypeMapper(String fqClassName) {
    TypeMapper tm = typeMappers.get(fqClassName);
    if( tm == null ) {
      logger.warn("No TypeMapper found for "+fqClassName);
      if( fqClassName == null || fqClassName.equals("null") ) {
        logger.warn("Called for "+(fqClassName == null?"NULL":fqClassName)+" from", new Exception());
      }
    }
    return tm;
  }
  
  public static enum Type {
    Simple, List, XynaObject, Choice, Any;
  }
  
  public SetterGetter<?> createSetterGetter(TypeInfoMember typeInfoMember, XynaObjectClassInfo parent) throws TypeMapperCreationException {
    String fieldName = typeInfoMember.getVarName();
    try {
      Field field = parent.getField(fieldName);

      Class<?> type = field.getType();
      if( logger.isTraceEnabled() ) {
        logger.trace( "generating setter \""+field.getName()+"\" for "+type.getName() + " in type "+ parent.getClassName() +" with "+typeInfoMember );
      }
      
      if( typeInfoMember.isList() ) {
        return createListSetterGetter(typeInfoMember,parent,field);
      } else {
        switch( typeInfoMember.getMemberType() ) {
        case Any:
          return (SetterGetter<?>)createAnySetterGetter(type, field);
        case Attribute:
          break;
        case Choice:
          return (SetterGetter<?>)createChoiceSetterGetter(type.getName(), typeInfoMember.getChoiceMember(), field);
        case Element:
          break;
        case Text:
          break;
        }
        if( XynaObject.class.isAssignableFrom(type)) {
          return (SetterGetter<?>)createXynaObjectSetterGetter(type.getName(), field);
        } else {
          return createSimpleSetterGetter( type, field);
        }
      }
    } catch (TypeMapperCreationException tmce) {
      logger.warn( "Failed to generate setter in type "+ parent.getClassName() +" with "+typeInfoMember );
      tmce.setFieldName(fieldName);
      throw tmce;
    } catch (Exception e) {
      logger.warn( "Failed to generate setter in type "+ parent.getClassName() +" with "+typeInfoMember );
      //TypeMapping-Tabelle sagt, dass es Feld memberVarName geben sollte, daher unerwartet
      TypeMapperCreationException tmce = new TypeMapperCreationException( TypeMapperCreationFailure.FieldAccess, fieldName+" in "+parent.getClassName(), e);
      tmce.setFieldName(fieldName);
      throw tmce;
    }
  }
  
  private SetterGetter<?> createListSetterGetter(TypeInfoMember typeInfoMember, XynaObjectClassInfo parent,
      Field field) throws TypeMapperCreationException {
    Class<?> listType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    String memberVarName = field.getName();
    String methodName = "addTo" + memberVarName.substring(0,1).toUpperCase() + memberVarName.substring(1);
    try {
      if( XynaObject.class.isAssignableFrom(listType) ) {
        Method addToMethod = parent.getMethod(methodName, listType);
        if( listType.getName().equals(Constants.getAnyType_FqName() ) ) {
          return createAnyListSetterGetter(listType.getName(), field, addToMethod );
        } else {
          return createXynaObjectListSetterGetter(listType.getName(), field, addToMethod );
        }
      } else {
        Method addToMethod = null;
        try {
          addToMethod = parent.getMethod(methodName, listType);
        } catch( NoSuchMethodException e ) {
          Class<?> primListType = getPrimitiveType(listType);
          if( primListType != null ) {
            try {
              addToMethod = parent.getMethod(methodName, primListType);
            } catch( NoSuchMethodException e2 ) {
              throw e;
            }
          }
        }
        return createListSetterGetter(listType, field, addToMethod );
      }
    } catch (NoSuchMethodException e) {
      throw new TypeMapperCreationException( TypeMapperCreationFailure.MethodAccess, methodName, e);
    }
  }
  
  private Class<?> getPrimitiveType(Class<?> listType) {
    if( listType.equals(Integer.class) ) {
      return int.class;
    } else if( listType.equals(Long.class) ) {
      return long.class;
    } else if( listType.equals(Boolean.class) ) {
      return boolean.class;
    } else if( listType.equals(Double.class) ) {
      return double.class;
    } else if( listType.equals(Byte.class) ) {
      return byte.class;
    }
    return null;
  }

  private <T> SetterGetter<List<T>> createListSetterGetter(Class<T> type, Field field, Method addToMethod) throws TypeMapperCreationException {
    StringSetterGetter<T> stringSG = new StringSetterGetter<T>(field, TypeConverterFactory.getTypeConverterForType(type) );
    return new ListSetterGetter<String,T>(field, addToMethod, stringSG);
  }
  
  private <T extends XynaObject> SetterGetter<List<T>> createXynaObjectListSetterGetter(String fqClassName, Field field, Method addToMethod) throws TypeMapperCreationException {
    XynaObjectSetterGetter<T> xynaObjectSG = new XynaObjectSetterGetter<T>(field, getTypeMapper(fqClassName) );
    return new ListSetterGetter<XynaObject,T>(field, addToMethod, xynaObjectSG );
  }
  private SetterGetter<List<XynaObject>> createAnyListSetterGetter(String fqClassName, Field field, Method addToMethod) throws TypeMapperCreationException {
    AnySetterGetter anySG = new AnySetterGetter(field, getTypeMapper(fqClassName), typeMapperForType);
    return new ListSetterGetter<XynaObject,XynaObject>(field, addToMethod, anySG );
  }

  private <T> SetterGetter<T> createSimpleSetterGetter(Class<T> type, Field field) throws TypeMapperCreationException {
    return new StringSetterGetter<T>(field, TypeConverterFactory.getTypeConverterForType(type) );
  }
  
  private <T extends XynaObject> SetterGetter<T> createXynaObjectSetterGetter(String fqClassName, Field field) throws TypeMapperCreationException {
    return new XynaObjectSetterGetter<T>(field, getTypeMapper(fqClassName) );
  }

  private <T extends XynaObject> SetterGetter<T> createChoiceSetterGetter( String fqClassName, List<Pair<FQName, TypeInfo>> choiceMember, Field field) throws TypeMapperCreationException {
    Map<String, TypeMapper> choiceMapper = new HashMap<String, TypeMapper>();
    for( Pair<FQName, TypeInfo> cm : choiceMember ) {
      String fqTypeName = cm.getSecond().getXmomType().getFQTypeName();
      TypeMapper tm;
      try {
        tm = getTypeMapper(GenerationBase.transformNameForJava(fqTypeName));
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
      choiceMapper.put( cm.getFirst().getName(), tm ); 
    }
    return new ChoiceSetterGetter<T>(field, getTypeMapper(fqClassName), choiceMapper);
  }
  
  private SetterGetter<XynaObject> createAnySetterGetter(Class<?> type, Field field) throws TypeMapperCreationException {
    if( type.getName().equals(Constants.getAnyType_FqName() ) ) {
      return new AnySetterGetter(field, getTypeMapper(type.getName()), typeMapperForType );
    } else {
      throw new TypeMapperCreationException(TypeMapperCreationFailure.UnexpectedType, "Expected "+Constants.getAnyType_FqName()+" instead of "+type.getName());
    }
  }

  
  public static abstract class SetterGetter<T> {
    
    protected Type type;
    protected Field field;
    
    public SetterGetter(Type type, Field field) {
      this.type = type;
      this.field = field;
    }
    
    public Type getType() {
      return type;
    }
    
    public String fieldToString() {
      return field.getDeclaringClass().getName()+"."+field.getName(); 
    }
    
    @SuppressWarnings("unchecked")
    public T get(XynaObject xo) throws SetterGetterException {
      try {
        return (T) field.get(xo);
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.Getter, field.getName(), e );
      }
    }

    public void set(XynaObject xo, T value) throws SetterGetterException {
      try {
        field.set(xo, value );
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.Setter, field.getName(), e );
      }
    }

    public void fill(XynaObject xo, Node node) throws SetterGetterException, XynaObjectCreationException {
      set(xo, parse(node));
    }

    public abstract T parse(Node node) throws SetterGetterException, XynaObjectCreationException;

  }

  public static abstract class StringBasedSetterGetter<T> extends SetterGetter<T> {

    public StringBasedSetterGetter(Type type, Field field) {
      super(type, field);
    }
    
    protected String getText(Node node) {
      if (node instanceof Element) {
        return XMLUtils.getTextContent((Element) node);
      } else {
        return node.getTextContent();
      }
    }

    public abstract void setString(XynaObject xo, String value) throws SetterGetterException;
    public abstract String getString(XynaObject xo, CreateXmlOptions cxo) throws SetterGetterException;
    public abstract String convertToString(T value, CreateXmlOptions cxo);

  }
  
  public static abstract class XynaObjectBasedSetterGetter<T> extends SetterGetter<T> {

    public XynaObjectBasedSetterGetter(Type type, Field field) {
      super(type, field);
    }
    
    public abstract TypeMapper getTypeMapper();
    
    public XynaObject getXynaObject(XynaObject xo) throws SetterGetterException {
      return (XynaObject)get(xo);
    }

    @SuppressWarnings("unchecked")
    public void setXynaObject(XynaObject xo, XynaObject value) throws SetterGetterException {
      set(xo, (T)value);
    }

  }

  
  public static class StringSetterGetter<T> extends StringBasedSetterGetter<T> {
    protected TypeConverter<T> typeConverter;
    
    public StringSetterGetter(Field field, TypeConverter<T> typeConverter) {
      super(Type.Simple,field);
      this.typeConverter = typeConverter;
    }
    
    public Type getType() {
      return type;
    }
    
    public void setString(XynaObject xo, String value) throws SetterGetterException {
      set(xo, typeConverter.fromString(value) );
    }
    public String getString(XynaObject xo, CreateXmlOptions cxo) throws SetterGetterException {
      return typeConverter.toString( get(xo), cxo );
    }
    
    public String convertToString(T value, CreateXmlOptions cxo) {
      return typeConverter.toString( value, cxo );
    }

    @Override
    public T parse(Node node) throws SetterGetterException {
      return typeConverter.fromString( getText(node) );
    }

  }

  public static class XynaObjectSetterGetter<T extends XynaObject> extends XynaObjectBasedSetterGetter<T> {

    private TypeMapper typeMapper;

    public XynaObjectSetterGetter(Field field, TypeMapper typeMapper) {
      super(Type.XynaObject, field);
      this.typeMapper = typeMapper;
    }

    public TypeMapper getTypeMapper() {
      return typeMapper;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T parse(Node node) throws SetterGetterException, XynaObjectCreationException {
      return (T)typeMapper.createXynaObject(node);
    }

  }
  
  public static class ChoiceSetterGetter<T extends XynaObject> extends XynaObjectBasedSetterGetter<T> {

    private TypeMapper baseTypeMapper;
    private Map<String, TypeMapper> nameMapper;
    private Map<String, Pair<FQName,TypeMapper>> classMapper;

    public ChoiceSetterGetter(Field field, TypeMapper baseTypeMapper,
        Map<String, TypeMapper> choiceMapper) {
      super(Type.Choice, field);
      this.baseTypeMapper = baseTypeMapper;
      this.nameMapper = choiceMapper;
      this.classMapper = new HashMap<String,Pair<FQName,TypeMapper>>();
      for( Map.Entry <String, TypeMapper> entry : nameMapper.entrySet() ) {
        TypeMapper tm = entry.getValue();
        classMapper.put( tm.getTypeClassName(), Pair.of( tm.getName(), tm) );
      }
      classMapper.put( baseTypeMapper.getTypeClassName(), Pair.of( baseTypeMapper.getName(), baseTypeMapper) );
    }
    
    @Override
    public TypeMapper getTypeMapper() {
      return baseTypeMapper;
    }

    private TypeMapper getTypeMapper(Node node) {
      String name = node.getLocalName();
      TypeMapper tm = nameMapper.get(name);
      if( tm == null ) {
        logger.warn("Could not find "+name+" in "+nameMapper.keySet() + " for " +fieldToString() );
        tm = baseTypeMapper;
      }
      return tm;
    }

    public Pair<FQName, TypeMapper> getChoiceData(XynaObject xo) {
      String xoClassName = xo.getClass().getName();
      Pair<FQName, TypeMapper> pair = classMapper.get(xoClassName);
      if( pair == null ) {
        logger.warn("Could not find "+xoClassName+" in "+classMapper.keySet() + " for " +fieldToString() );
        pair = Pair.of( baseTypeMapper.getName(), baseTypeMapper);
      }
      return pair;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T parse(Node node) throws SetterGetterException, XynaObjectCreationException {
      TypeMapper typeMapper = getTypeMapper(node);
      return (T) typeMapper.createXynaObject(node);
    }

  }

  public static class ListSetterGetter<X,T> extends SetterGetter<List<T>> {
    private Method addToMethod;
    private SetterGetter<T> baseSetterGetter;
    
    public ListSetterGetter(Field field, Method addToMethod, SetterGetter<T> baseSetterGetter) {
      super(Type.List, field);
      this.addToMethod = addToMethod;
      this.baseSetterGetter = baseSetterGetter;
    }
    
    public void add(XynaObject xo, T value) throws SetterGetterException {
      try {
        addToMethod.invoke(xo, value );
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.SetterMethod, addToMethod.getName(), e );
      }
    }
    
    @Override
    public void fill(XynaObject xo, Node node) throws SetterGetterException, XynaObjectCreationException {
      try {
        addToMethod.invoke(xo, baseSetterGetter.parse(node) );
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.SetterMethod, addToMethod.getName(), e );
      }
    }
    
    @Override
    public List<T> parse(Node node) throws SetterGetterException, XynaObjectCreationException {
      return Collections.singletonList( baseSetterGetter.parse(node) );
    }

    public SetterGetter<T> getBaseSetterGetter() {
      return baseSetterGetter;
    }

    @SuppressWarnings("unchecked")
    public List<X> getListForXml(XynaObject xo, CreateXmlOptions cxo) throws SetterGetterException {
      List<T> list = get(xo);
      if( list == null || list.isEmpty() ) {
        return Collections.emptyList();
      }
       if( baseSetterGetter instanceof StringBasedSetterGetter ) {
         List<X> ret = new ArrayList<X>();
         StringBasedSetterGetter<T> stringSG = (StringBasedSetterGetter<T>)baseSetterGetter;
        for( T value : list ) {
          ret.add( (X)stringSG.convertToString(value,cxo) );
        }
        return ret;
      } else if( baseSetterGetter instanceof XynaObjectBasedSetterGetter ) {
        return (List<X>)list;
      } else {
        throw new IllegalStateException( "Unexpected setterGetter "+baseSetterGetter);
      }
     
    }
    
  }
  
  public static class AnySetterGetter extends XynaObjectBasedSetterGetter<XynaObject> {
    
    private TypeMapper typeMapper;
    private TypeMapperForType typeMapperForType;
    
    public AnySetterGetter(Field field, TypeMapper typeMapper, TypeMapperForType typeMapperForType) {
      super(Type.Any,field);
      this.typeMapper = typeMapper;
      this.typeMapperForType = typeMapperForType;
    }
    
    @Override
    public TypeMapper getTypeMapper() {
      return typeMapper;
    }
    
    @Override
    public XynaObject parse(Node node) throws SetterGetterException, XynaObjectCreationException {
      String namespace = null;
      String type = null;
      Element el = (Element)node;
      Attr typeAttr = el.getAttributeNodeNS("http://www.w3.org/2001/XMLSchema-instance", "type" );
      if( typeAttr != null ) {
        String typeString = typeAttr.getNodeValue();
        int idx = typeString.indexOf(':');
        if( idx >=0 ) {
          namespace = typeAttr.lookupNamespaceURI( typeString.substring(0, idx) );
          type = typeString.substring(idx+1);
        } else {
          namespace = typeAttr.lookupNamespaceURI(null);
          type = typeString;
        }
      }

      XynaObject xoChild = typeMapper.createEmptyXynaObject();
      typeMapper.fillMember( xoChild, Constants.FIELD_ANYTYPE_NAMESPACE, namespace);
      typeMapper.fillMember( xoChild, Constants.FIELD_ANYTYPE_TYPE_NAME, type );
      try {
        typeMapper.fillMember( xoChild, Constants.FIELD_ANYTYPE_STRING_VALUE, getXmlContent( node, true) );
      } catch( TransformerException te ) {
        throw new XynaObjectCreationException(XynaObjectCreationFailure.Parsing, "anyType "+typeMapper.getName(), te);
      }
      
      TypeMapper tm = typeMapperForType.getTypeMapper(namespace, type); 
      if( tm != null ) {
        typeMapper.fillMember( xoChild, Constants.FIELD_ANYTYPE_VALUE, tm.createXynaObject(node) );
      }
      return xoChild;
    }

    public boolean addXmlAny(XmlContext xmlContext, Element parent, Element el, XynaObject xo) throws DOMException, XmlCreationException {
      if( xo == null ) {
        //Fehlender XSD_AnyType, daher leeres Tag ausgeben
        return false;
      }
      String typeName = typeMapper.getMemberString(xo, Constants.FIELD_ANYTYPE_TYPE_NAME, xmlContext.getCreateXmlOptions());
      String namespace = typeMapper.getMemberString(xo, Constants.FIELD_ANYTYPE_NAMESPACE, xmlContext.getCreateXmlOptions());
      String stringValue = typeMapper.getMemberString(xo, Constants.FIELD_ANYTYPE_STRING_VALUE, xmlContext.getCreateXmlOptions());
      XynaObject any = typeMapper.getMemberXynaObject(xo, Constants.FIELD_ANYTYPE_VALUE, xmlContext.getCreateXmlOptions() );
      
      if( typeName == null && namespace == null && stringValue == null && any == null ) {
        //Leerer XSD_AnyType, daher leeres Tag ausgeben
        return false;
      }
      
      TypeMapper tm = null;
      if( any != null ) {
        tm = typeMapperForType.getTypeMapper(any.getClass().getName());
      }
      
      //Fehlende typeName oder namespace aus any ableiten
      if( typeName == null || namespace == null ) {
        //nur any ist gesetzt, daraus müssen typeName und namespace abgeleitet werden
        if( tm == null ) {
          logger.warn("Could not determine type and namespace for "+field.getName()+": any="+any+", typeMapper="+tm);
        } else {
          namespace = tm.getName().getNamespace();
          typeName = tm.getName().getName();
        }
      }
      
      if( typeName != null && namespace != null ) {
        Attr attrType = xmlContext.createTypeAttribute( namespace, typeName );
        el.setAttributeNodeNS(attrType);

        if( any != null && tm != null ) {
          //Schreiben des any-Objekts bevorzugen
          tm.fillXmlElement(xmlContext, el, any);
        } else if( stringValue != null ) {
          //Ansonsten Schreiben der String-Representation
          if( stringValue.contains("<") ) {
            try {
              Document doc = XercesUtils.parseXml(stringValue);
              Element elVal = doc.getDocumentElement();
              //TODO namespaces umtragen?
              for( Element ev : XMLUtils.getChildElements(elVal) ) {
                el.appendChild( xmlContext.importNode(ev) );
              }
            } catch( Exception e ) {
              throw new XmlCreationException(XmlCreationFailure.InvalidData, e);
            }
            
          } else {
            el.setTextContent(stringValue);
          }
        } else if( any == null && stringValue == null ) {
          el.setTextContent(null); //TODO oder ist das Fehler?
        } else {
          //Fehler
          String value = null;
          if( any != null && tm == null ) {
            value = "XSD_AnyType in field "+field.getName()+": belongs to other datamodel";
          } else {
            value = "XSD_AnyType in field "+field.getName()+": any="+(any==null?"null":any.getClass())+", typeMapper="+tm+", type="+type+", namespace="+namespace;
          }
          throw new XmlCreationException(XmlCreationFailure.InvalidData, value);
        }
      } else {
        String value = "XSD_AnyType in field "+field.getName()+": type="+type+", namespace="+namespace;
        throw new XmlCreationException(XmlCreationFailure.InvalidData, value);
      }

      parent.appendChild(el);
      return true;
    }


    private static String getXmlContent(Node n, boolean onlyContent) throws TransformerException {
      if( onlyContent ) {
        List<Element> cs = XMLUtils.getChildElements(n);
        if( cs.size() == 0 ) {
          return n.getTextContent();
        } else if ( cs.size() == 1 ) {
          return getXmlContent(cs.get(0));
        }
      }
      return getXmlContent(n);
    }
    
    private static String getXmlContent(Node n) throws TransformerException {
      StringWriter writer = new StringWriter();
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(n), new StreamResult(writer));
      return writer.toString();
      
    }

  }

  /**
   * Suche zur Laufzeit für dynamische Typen; z-B bei AnyType
   *
   */
  private static class TypeMapperForType {

    private Map<String, TypeMapper> allTypeMappers;
    private ConcurrentHashMap<FQName,TypeMapper> typeMappers;
    private static final TypeMapper NONE = new TypeMapper(null, null);
    
    public TypeMapperForType(Map<String, TypeMapper> allTypeMappers) {
      this.allTypeMappers = allTypeMappers;
      this.typeMappers = new ConcurrentHashMap<FQName,TypeMapper>();
    }

    public TypeMapper getTypeMapper(String fqClassName) {
      TypeMapper tm = allTypeMappers.get(fqClassName);
      return tm;
    }

    public TypeMapper getTypeMapper(String namespace, String type) {
      FQName name = new FQName(namespace,type);
      
      TypeMapper tm = typeMappers.get(name);
      if( tm == null ) {
        tm = findTypeMapper(name);
      }
      if( tm == NONE ) {
        return null; //nichts gefunden
      }
      return tm;
    }

    private TypeMapper findTypeMapper(FQName name) {
      TypeMapper tmFound = typeMappers.get(name);
      
      if( tmFound == null ) {
        for( TypeMapper tm : allTypeMappers.values() ) {
          if( tm.getName().equals(name) ) {
            tmFound = tm; 
            break;
          }
        }
      }
      if( tmFound == null ) {
        tmFound = NONE; //Suche wird nie wieder Resultate bringen, daher negatives Suchergebnis cachen
      }
      typeMappers.putIfAbsent(name, tmFound);
      return typeMappers.get(name);
    }
    
  }

}

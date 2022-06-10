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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.map.mapping.TypeConverterFactory.TypeConverter;
import com.gip.xyna.xdev.map.mapping.exceptions.SetterGetterException;
import com.gip.xyna.xdev.map.mapping.exceptions.SetterGetterException.SetterGetterFailure;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xdev.map.mapping.exceptions.TypeMapperCreationException.TypeMapperCreationFailure;
import com.gip.xyna.xdev.map.types.FQName;
import com.gip.xyna.xdev.map.types.TypeInfo;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;


/**
 *
 */
public class SetterGetterCreator {

  private static Logger logger = CentralFactoryLogging.getLogger(SetterGetterCreator.class);
  
  private Map<String, TypeMapper> typeMappers;
  
  public SetterGetterCreator(Map<String, TypeMapper> typeMappers) {
    this.typeMappers = typeMappers;
  }
  
  public TypeMapper getTypeMapper(String fqClassName) {
    return typeMappers.get(fqClassName);
  }
  
  public static enum Type {
    Simple, List, XynaObject, Choice;
  }
  
  public SetterGetter<?> createSetterGetter(String fieldName, XynaObjectClassInfo parent, List<Pair<FQName, TypeInfo>> choiceMember) throws TypeMapperCreationException {
    try {
      Field field = parent.getField(fieldName);

      Class<?> type = field.getType();
      if( logger.isTraceEnabled() ) {
        logger.trace( "generating setter \""+field.getName()+"\" for "+type.getName() + " in type "+ parent.getClassName() );
      }
      if( type == List.class ) {
        Class<?> listType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        String memberVarName = field.getName();
        String methodName = "addTo" + memberVarName.substring(0,1).toUpperCase() + memberVarName.substring(1);
        try {
          Method addToMethod = parent.getMethod(methodName, listType);
          if( XynaObject.class.isAssignableFrom(listType) ) {
            return createXynaObjectListSetterGetter(listType.getName(), field, addToMethod );
          } else {
            return createListSetterGetter(listType, field, addToMethod );
          }
        } catch (NoSuchMethodException e) {
          throw new TypeMapperCreationException( TypeMapperCreationFailure.MethodAccess, methodName, e);
        }
      } else if( XynaObject.class.isAssignableFrom(type)) {
        if( choiceMember != null ) {
          return (SetterGetter<?>)createChoiceSetterGetter(type.getName(), choiceMember, field);
         } else {
          return (SetterGetter<?>)createXynaObjectSetterGetter(type.getName(), field);
        }
      } else {
        return createSimpleSetterGetter( type, field);
      }
    } catch (TypeMapperCreationException tmce) {
      tmce.setFieldName(fieldName);
      throw tmce;
    } catch (Exception e) {
      //TypeMapping-Tabelle sagt, dass es Feld memberVarName geben sollte, daher unerwartet
      TypeMapperCreationException tmce = new TypeMapperCreationException( TypeMapperCreationFailure.FieldAccess, e);
      tmce.setFieldName(fieldName);
      throw tmce;
    }
  }
  
  private <T> SetterGetter<List<T>> createListSetterGetter(Class<T> type, Field field, Method addToMethod) throws TypeMapperCreationException {
    return new ListSetterGetter<T>(field, addToMethod, TypeConverterFactory.getTypeConverterForType(type));
  }
  
  private <T extends XynaObject> SetterGetter<List<T>> createXynaObjectListSetterGetter(String fqClassName, Field field, Method addToMethod) throws TypeMapperCreationException {
    return new XynaObjectListSetterGetter<T>(field, addToMethod, getTypeMapper(fqClassName) );
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
      TypeMapper tm = getTypeMapper(fqTypeName);
      choiceMapper.put( cm.getFirst().getName(), tm ); 
    }
    return new ChoiceSetterGetter<T>(field, getTypeMapper(fqClassName), choiceMapper);
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

  }

  public static class StringSetterGetter<T> extends SetterGetter<T> {
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
    

  }

  public static class XynaObjectSetterGetter<T extends XynaObject> extends SetterGetter<T> {

    private TypeMapper typeMapper;

    public XynaObjectSetterGetter(Field field, TypeMapper typeMapper) {
      super(Type.XynaObject, field);
      this.typeMapper = typeMapper;
    }

    public TypeMapper getTypeMapper() {
      return typeMapper;
    }

    public XynaObject getXynaObject(XynaObject xo) throws SetterGetterException {
      return get(xo);
    }

    @SuppressWarnings("unchecked")
    public void setXynaObject(XynaObject xo, XynaObject value) throws SetterGetterException {
      set(xo, (T)value);
    }
    
  }
  
  public static class ChoiceSetterGetter<T extends XynaObject> extends SetterGetter<T> {

    private TypeMapper baseTypeMapper;
    private Map<String, TypeMapper> nameMapper;
    private Map<String, Pair<String,TypeMapper>> classMapper;

    public ChoiceSetterGetter(Field field, TypeMapper baseTypeMapper,
        Map<String, TypeMapper> choiceMapper) {
      super(Type.Choice, field);
      this.baseTypeMapper = baseTypeMapper;
      this.nameMapper = choiceMapper;
      this.classMapper = new HashMap<String,Pair<String,TypeMapper>>();
      for( Map.Entry <String, TypeMapper> entry : nameMapper.entrySet() ) {
        TypeMapper tm = entry.getValue();
        classMapper.put( tm.getTypeClassName(), Pair.of( entry.getKey(), tm) );
      }
      classMapper.put( baseTypeMapper.getTypeClassName(), Pair.of( baseTypeMapper.getName().getName(), baseTypeMapper) );
    }

    public XynaObject getXynaObject(XynaObject xo) throws SetterGetterException {
      return get(xo);
    }

    @SuppressWarnings("unchecked")
    public void setXynaObject(XynaObject xo, XynaObject value) throws SetterGetterException {
      set(xo, (T)value);
    }

    public TypeMapper getTypeMapper(Node node) {
      String name = node.getLocalName();
      TypeMapper tm = nameMapper.get(name);
      if( tm == null ) {
        logger.warn("Could not find "+name+" in "+nameMapper.keySet() + " for " +fieldToString() );
        
        tm = baseTypeMapper;
      }
      return tm;
    }

    public Pair<String, TypeMapper> getChoiceData(XynaObject xo) {
      String xoClassName = xo.getClass().getName();
      Pair<String, TypeMapper> pair = classMapper.get(xoClassName);
      if( pair == null ) {
        logger.warn("Could not find "+xoClassName+" in "+classMapper.keySet() + " for " +fieldToString() );
        pair = Pair.of( baseTypeMapper.getName().getName(), baseTypeMapper);
      }
      return pair;
    }
    
  }

  public static class ListSetterGetter<T> extends SetterGetter<List<T>> {
    private Method addToMethod;
    protected TypeConverter<T> typeConverter;
    
    public ListSetterGetter(Field field, Method addToMethod, TypeConverter<T> typeConverter) {
      super(Type.List, field);
      this.addToMethod = addToMethod;
      this.typeConverter =typeConverter; 
    }

    /**
     * Komplette Liste nicht auf einmal setzen, sondern wird mehrfach aufrufen 
     * und Daten mit addTo{VarName}(...) eintragen
     */
    public void addString(XynaObject xo, String value) throws SetterGetterException {
      try {
        addToMethod.invoke(xo, typeConverter.fromString(value) );
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.SetterMethod, addToMethod.getName(), e );
      }
    }

    public List<String> getStringList(XynaObject xo, CreateXmlOptions cxo) throws SetterGetterException {
      List<T> list = get(xo);
      if( list == null || list.isEmpty() ) {
        return Collections.emptyList();
      }
      ArrayList<String> stringList = new ArrayList<String>();
      for( T value : list ) {
        stringList.add( typeConverter.toString(value,cxo) );
      }
      return stringList;
    }
    
  }
  
  public static class XynaObjectListSetterGetter<T extends XynaObject> extends SetterGetter<List<T>> {
    private Method addToMethod;
    protected TypeMapper typeMapper;
    
    public XynaObjectListSetterGetter(Field field, Method addToMethod, TypeMapper typeMapper) {
      super(Type.List, field);
      this.addToMethod = addToMethod;
      this.typeMapper = typeMapper; 
    }

    /**
     * Komplette Liste nicht auf einmal setzen, sondern mehrfach aufrufen 
     * und Daten mit addTo{VarName}(...) eintragen
     */
    public void addXynaObject(XynaObject xo, XynaObject value) throws SetterGetterException {
      try {
        addToMethod.invoke(xo, value );
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.SetterMethod, addToMethod.getName(), e );
      }
    }
    
    public TypeMapper getTypeMapper() {
      return typeMapper;
    }

    @SuppressWarnings("unchecked")
    public List<XynaObject> getXynaObjectList(XynaObject xo) throws SetterGetterException {
      return (List<XynaObject>) get(xo);
    }

  }
 
}

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
package com.gip.xyna.xact.filter.util.xo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.CentralFactoryLogging;


public class XynaObjectJsonBuilder {
  
  private final long revision;
  private final long[] backupRevisions;
  private final JsonBuilder builder;
  protected XynaObjectVisitor visitor;
  
  public static final XynaPropertyBoolean LONG2STRING = new XynaPropertyBoolean("zeta.json.long2string", false).
      setDefaultDocumentation(DocumentationLanguage.DE, "Legt fest, ob in JSONs Member vom Typ Long/long als String gesendet/erwartet werden, siehe XBE-254.").
      setDefaultDocumentation(DocumentationLanguage.EN, "Determines whether JSON-members of type Long/long are encoded/decoded as Strings, see XBE-254.");
  
  
  public XynaObjectJsonBuilder(long revision) {
    this(revision, new JsonBuilder());
  }
  
  public XynaObjectJsonBuilder(long revision, long[] backupRevisions) {
    this(revision, backupRevisions, new JsonBuilder());
  }
  
  
  public XynaObjectJsonBuilder(RuntimeContext rc, JsonBuilder builder) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    this(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rc), builder);
  }
  
  public XynaObjectJsonBuilder(RuntimeContext rc, RuntimeContext[] backupRCs, JsonBuilder builder) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    this.backupRevisions = new long[backupRCs.length];
    for(int i=0; i< this.backupRevisions.length; i++){
      this.backupRevisions[i] = rm.getRevision(backupRCs[i]);
    }
    this.revision = rm.getRevision(rc);
    this.builder = builder;
    this.visitor = new XynaObjectVisitor();
  }
  
  public XynaObjectJsonBuilder(long revision, JsonBuilder builder) {
    this(revision, null, builder);
  }
  
  public XynaObjectJsonBuilder(long revision, long[] backupRevisions, JsonBuilder builder) {
    this.builder = builder;
    this.backupRevisions = backupRevisions;
    this.revision = revision;
    this.visitor = new XynaObjectVisitor();
  }
  
  
  public void build(GeneralXynaObject gxo) {
    buildGeneralXynaObjectJson(gxo);
  }
  
  public String buildJson(GeneralXynaObject gxo) {
    buildGeneralXynaObjectJson(gxo);
    return builder.toString();
  }
  
  @SuppressWarnings("unchecked")
  private void buildGeneralXynaObjectJson(GeneralXynaObject gxo) {
    if (gxo instanceof Container) {
      buildContainerJson((Container)gxo);
    } else if (gxo instanceof GeneralXynaObjectList) {
      buildListJson((GeneralXynaObjectList<? extends GeneralXynaObject>)gxo);
    } else {
      if (gxo != null) {
        buildXynaObjectJson(gxo);
      } else {
        builder.addPrimitiveListElement("null");
      }
    }
  }
  
  private void buildContainerJson(Container container) {
    builder.startList();
    for (int i = 0; i < container.size(); i++) {
      buildGeneralXynaObjectJson(container.get(i));
    }
    builder.endList();
  }
  
  @SuppressWarnings("unchecked")
  private void buildListJson(GeneralXynaObjectList<? extends GeneralXynaObject> gxol) {
    if (gxol.getContainedClass() != null) {
      buildListJson((Class<? extends GeneralXynaObject>) gxol.getContainedClass(), gxol);
    } else {
      Class<? extends GeneralXynaObject> clazz = null;
      try{
        clazz = loadListClass(gxol, revision);
        buildListJson(clazz, gxol);
      } catch (RuntimeException e) {
        tryBackupRevisionsToBuildListJson(gxol, e);
      }
    }
  }


  private Class<? extends GeneralXynaObject> loadListClass(GeneralXynaObjectList<? extends GeneralXynaObject> gxol, long revToUse){
    long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
                                      .getRevisionDefiningXMOMObjectOrParent(gxol.getContainedFQTypeName(), revToUse);
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    try {
      String fqClassName = GenerationBase.transformNameForJava(gxol.getContainedFQTypeName());
      Class<? extends GeneralXynaObject> clazz = null;
      
      if(GenerationBase.isReservedServerObjectByFqClassName(fqClassName)){
        return (Class<? extends GeneralXynaObject>)getClass().getClassLoader().loadClass(fqClassName);
      }
      
      try{
        clazz = (Class<? extends GeneralXynaObject>) cld.loadClassWithClassLoader(ClassLoaderType.MDM, fqClassName, fqClassName, rev);
      }
      catch(Exception e){
        clazz = (Class<? extends GeneralXynaObject>) cld.loadClassWithClassLoader(ClassLoaderType.Exception, fqClassName, fqClassName, rev);
      }
      return clazz;
    } catch (XPRC_InvalidPackageNameException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  private void tryBackupRevisionsToBuildListJson(GeneralXynaObjectList<? extends GeneralXynaObject> gxol, Exception e){
    if(backupRevisions == null){
      throw new RuntimeException(e);
    }
    Class<? extends GeneralXynaObject> clazz = null;
    for(int i=0; i<backupRevisions.length; i++){
      try{
        clazz = loadListClass(gxol, backupRevisions[i]);
        buildListJson(clazz, gxol);
        return;
      }
      catch(Exception ex){
      //try next
      }
    }
    //could not be loaded by backup revisions
    throw new RuntimeException(e);
  }


  private void buildListJson(Class<? extends GeneralXynaObject> listType, List<? extends GeneralXynaObject> gxol) {
    builder.startObject();
    MetaInfo meta = buildMetaForXynaObjectClass(listType);
    builder.addObjectAttribute(XynaObjectVisitor.META_TAG, meta);
    builder.addListAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG);
    for (GeneralXynaObject gxo : gxol) {
      if (gxo != null) {
        buildGeneralXynaObjectJson(gxo);
      } else {
        builder.addPrimitiveListElement("null");
      }
    }
    builder.endList();
    builder.endObject();
  }
  
  @SuppressWarnings("unchecked")
  private void buildXynaObjectJson(GeneralXynaObject gxo) {
    builder.startObject();
    MetaInfo meta = buildMetaForXynaObject(gxo);
    builder.addObjectAttribute(XynaObjectVisitor.META_TAG, meta);
    Set<String> variableNames = getVariableNames(gxo);
    for (String variableName : variableNames) {
      try {
        Object value = gxo.get(variableName);
        if (value != null) {
          Field field = visitor.oFindField(gxo.getClass(), variableName);
          Class<?> fieldType = field.getType();
          PrimitiveType primitiveType = PrimitiveType.createOrNull(fieldType.getSimpleName());
          if (primitiveType != null) {
            switch (primitiveType) {
              case CONTAINER:
              case VOID:
              case BYTE:
              case BYTE_OBJ:
                throw new IllegalArgumentException("Unexpected contenttype " + primitiveType + " in XynaObject " + gxo + " - " + variableName);
              case BOOLEAN:
              case BOOLEAN_OBJ:
                builder.addBooleanAttribute(variableName, (Boolean)value);
                continue;
              case DOUBLE:
              case DOUBLE_OBJ:
              case INT:
              case INTEGER:
                builder.addNumberAttribute(variableName, (Number)value);
                continue;
              case LONG:
              case LONG_OBJ:
                if (LONG2STRING.get()) {
                  builder.addStringAttribute(variableName, ((Number)value).toString());
                } else {
                  builder.addNumberAttribute(variableName, (Number)value);
                }
                continue;
              case STRING :
                builder.addStringAttribute(variableName, (String)value);
                continue;
              case ANYTYPE:
              case EXCEPTION:
              case XYNAEXCEPTION:
              case XYNAEXCEPTIONBASE:
              default :
                break;
            }
          }
          builder.nextObjectAsAttribute(variableName);
          if (List.class.isAssignableFrom(fieldType)) {
            Class<?> typeOfList = getGenericTypeOfList(field); 
            if (GeneralXynaObject.class.isAssignableFrom(typeOfList)) {
              buildListJson((Class<? extends GeneralXynaObject>)typeOfList, (List<? extends GeneralXynaObject>)value);
            } else {
              buildPrimitiveListJson(field, (List<?>)value);
            }
          } else if (GeneralXynaObject.class.isAssignableFrom(fieldType)) {
            buildXynaObjectJson((GeneralXynaObject) value);
          } else {
            throw new IllegalArgumentException("Unexpected field type " + fieldType);
          }
        } // else skip it?
      } catch (InvalidObjectPathException e) {
        // should not be possible
        throw new RuntimeException(e);
      }
    }
    builder.endObject();
  }
  
  private void buildPrimitiveListJson(Field field, List<?> values) {
    builder.startList();
    Class<?> fieldType = getGenericTypeOfList(field); 
    PrimitiveType primitiveType = PrimitiveType.createOrNull(fieldType.getSimpleName());
    for (Object value : values) {
      if (primitiveType == PrimitiveType.STRING) {
        if (value == null) {
          builder.addPrimitiveListElement("null");
        } else {
          builder.addStringListElement(value.toString());
        }
      } else {
        //boolean, zahlen
        builder.addPrimitiveListElement(String.valueOf(value));
      }
    }
    builder.endList();
  }
  
  protected static Class<?> getGenericTypeOfList(Field listField) {
    Type[] types = ((java.lang.reflect.ParameterizedType)listField.getGenericType()).getActualTypeArguments();
    if (types != null &&
        types.length > 0) {
      // for list cases we should only ever need [0]
      return (Class<?>) types[0];
    } else {
      throw new IllegalArgumentException("Failed to determine list parameters");
    }
  }


  private MetaInfo buildMetaForXynaObject(GeneralXynaObject gxo) {
    return buildMetaForXynaObjectClass(gxo.getClass());
  }
  
  @SuppressWarnings("resource")
  private MetaInfo buildMetaForXynaObjectClass(Class<? extends GeneralXynaObject> cGxo) {
    XynaObjectAnnotation xoa = cGxo.getAnnotation(XynaObjectAnnotation.class);
    RuntimeContext rtc = null;
    
    if(xoa == null){
      String fqn = cGxo.getCanonicalName();
      if(GenerationBase.isReservedServerObjectByFqClassName(fqn)){
        fqn = GenerationBase.getXmlNameForReservedClass(cGxo);
      }
      rtc = getRTCFromFqn(fqn);
      return new MetaInfo(fqn, rtc);
    }
    
    ClassLoader cl = cGxo.getClassLoader();
    if (cl != null &&
        cl instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) cl;
      try {
        rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(clb.getRevision());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    } else {
      rtc = getRTCFromFqn(xoa.fqXmlName());
    }
    return new MetaInfo(xoa.fqXmlName(), rtc);  
  }


  private RuntimeContext getRTCFromFqn(String fqn){
    RuntimeContext rtc = null;
    try{
      Long crevision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject(fqn, revision);
      rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(crevision);
    } catch(Exception e){
      return null; // base.AnyType
    }
    return rtc;
  }
  
 @SuppressWarnings("unchecked")
  protected Set<String> getVariableNames(GeneralXynaObject gxo) {
    if(!(gxo instanceof com.gip.xyna.utils.exceptions.XynaException)){
      try {
        Method m = gxo.getClass().getDeclaredMethod("getVariableNames");
        return (Set<String>) m.invoke(gxo);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        throw new RuntimeException(e);
      }
    } else{
      Class<?> clazz = gxo.getClass();
      java.util.HashSet<String> result = new java.util.HashSet<String>();
      while(clazz != com.gip.xyna.utils.exceptions.XynaException.class){
        Field[] fields = clazz.getDeclaredFields();
        for(int i=0; i<fields.length; i++){
          Field f = fields[i];
          if(f.getModifiers() == 2){ //private
            result.add(f.getName());
          }
        }
        clazz = clazz.getSuperclass();
      }

      return result;
    }
  }
  

}

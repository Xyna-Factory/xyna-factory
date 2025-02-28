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
package com.gip.xyna.xact.filter.util.xo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
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


public class XynaObjectJsonBuilder {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaObjectJsonBuilder.class);
  
  private final long[] revisions;
  private final JsonBuilder builder;
  protected XynaObjectVisitor visitor;
  
  public static final XynaPropertyBoolean LONG2STRING = new XynaPropertyBoolean("zeta.json.long2string", false).
      setDefaultDocumentation(DocumentationLanguage.DE, "Legt fest, ob in JSONs Member vom Typ Long/long als String gesendet/erwartet werden.").
      setDefaultDocumentation(DocumentationLanguage.EN, "Determines whether JSON-members of type Long/long are encoded/decoded as Strings.");
  
  
  public XynaObjectJsonBuilder(long revision) {
    this(revision, new JsonBuilder());
  }
  
  public XynaObjectJsonBuilder(long revision, long[] backupRevisions) {
    this(revision, backupRevisions, new JsonBuilder());
  }
  
  public XynaObjectJsonBuilder(RuntimeContext rc, JsonBuilder builder) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    this(convertRtcs(rc)[0], builder);
  }
  
  public XynaObjectJsonBuilder(RuntimeContext rc, RuntimeContext[] backupRCs, JsonBuilder builder) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    this(convertRtcs(rc)[0], convertRtcs(backupRCs), builder);
  }
  
  public XynaObjectJsonBuilder(long revision, JsonBuilder builder) {
    this(revision, null, builder);
  }
  
  public XynaObjectJsonBuilder(long revision, long[] backupRevisions, JsonBuilder builder) {
    this.builder = builder;
    revisions = new long[backupRevisions == null ? 1 : backupRevisions.length + 1];
    revisions[0] = revision;
    for (int i = 1; i < this.revisions.length; i++) {
      revisions[i] = backupRevisions[i - 1];
    }
    this.visitor = new XynaObjectVisitor();
  }
  
  public void build(GeneralXynaObject gxo) {
    buildGeneralXynaObjectJson(gxo);
  }
  
  public String buildJson(GeneralXynaObject gxo) {
    buildGeneralXynaObjectJson(gxo);
    return builder.toString();
  }

  private static long[] convertRtcs(RuntimeContext... rtcs) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    long[] result = new long[rtcs.length];
    for (int i = 0; i < rtcs.length; i++) {
      result[i] = rm.getRevision(rtcs[i]);
    }
    return result;
  }
  
  private void buildGeneralXynaObjectJson(GeneralXynaObject gxo) {
    if (gxo instanceof Container) {
      buildContainerJson((Container)gxo);
    } else if (gxo instanceof GeneralXynaObjectList) {
      buildListJson((GeneralXynaObjectList<?>)gxo);
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
      return;
    }
     
    try {
      Class<? extends GeneralXynaObject> clazz = loadClass(gxol.getContainedFQTypeName());
      buildListJson(clazz, gxol);
    } catch (ClassNotFoundException | XPRC_InvalidPackageNameException e) {
      throw new RuntimeException("Could not load inner list class: '" + gxol.getContainedFQTypeName() +"'.", e);
    }
  }
  
  @SuppressWarnings("unchecked")
  private Class<? extends GeneralXynaObject> loadClass(String fqn) throws XPRC_InvalidPackageNameException, ClassNotFoundException {
    String fqClassName = GenerationBase.transformNameForJava(fqn);
    if (GenerationBase.isReservedServerObjectByFqClassName(fqClassName)) {
      return (Class<? extends GeneralXynaObject>) GenerationBase.getReservedClass(fqn).getClassLoader().loadClass(fqClassName);
    }
    
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    for (int i = 0; i < revisions.length; i++) {
      long revToUse = revisions[i];
      ClassLoaderBase cl = cld.findClassLoaderByType(fqClassName, revToUse, ClassLoaderType.MDM, true);
      if (cl == null) {
        cl = cld.findClassLoaderByType(fqClassName, revToUse, ClassLoaderType.Exception, true);
      }
      if (cl != null) {
        return (Class<? extends GeneralXynaObject>) cl.loadClass(fqn);
      }
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("Could not find classloader for ").append(fqn).append(" in revisions ");
    sb.append(revisions[0]);
    for(int i=1; i<revisions.length; i++) {
      sb.append(", ").append(revisions[i]);
    }
    throw new RuntimeException(sb.toString());
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
  
  private Object getValueFromXynaObject(GeneralXynaObject gxo, String variableName) {
    try {
      return gxo.get(variableName);
    } catch (InvalidObjectPathException e) {
      // should not be possible
      throw new RuntimeException("Invalid object path '" + variableName + "' in " + gxo, e);
    }
  }
  
  private void addPrimitiveMember(PrimitiveType primitiveType, String variableName, Object value, GeneralXynaObject gxo) {
    switch (primitiveType) {
      case CONTAINER :
      case VOID :
      case BYTE :
      case BYTE_OBJ :
        throw new IllegalArgumentException("Unexpected contenttype " + primitiveType + " in XynaObject " + gxo + " - "  + variableName);
      case BOOLEAN :
      case BOOLEAN_OBJ :
        builder.addBooleanAttribute(variableName, (Boolean) value);
        return;
      case DOUBLE :
      case DOUBLE_OBJ :
      case INT :
      case INTEGER :
        builder.addNumberAttribute(variableName, (Number) value);
        return;
      case LONG :
      case LONG_OBJ :
        if (LONG2STRING.get()) {
          builder.addStringAttribute(variableName, ((Number) value).toString());
        } else {
          builder.addNumberAttribute(variableName, (Number) value);
        }
        return;
      case STRING :
        builder.addStringAttribute(variableName, (String) value);
        return;
      case ANYTYPE :
      case EXCEPTION :
      case XYNAEXCEPTION :
      case XYNAEXCEPTIONBASE :
      default :
        return;
    }
  }
  
  @SuppressWarnings("unchecked")
  private void buildXynaObjectJson(GeneralXynaObject gxo) {
    builder.startObject();
    MetaInfo meta = buildMetaForXynaObject(gxo);
    builder.addObjectAttribute(XynaObjectVisitor.META_TAG, meta);
    Set<String> variableNames = getVariableNames(gxo);
    Object value = null;
    for (String variableName : variableNames) {
      value = getValueFromXynaObject(gxo, variableName);
      if(value == null) {
        continue;
      }
      Field field = visitor.oFindField(gxo.getClass(), variableName);
      Class<?> fieldType = field.getType();
      PrimitiveType primitiveType = PrimitiveType.createOrNull(fieldType.getSimpleName());
      if (primitiveType != null) {
        addPrimitiveMember(primitiveType, variableName, value, gxo);
        continue;
      }
      if (!List.class.isAssignableFrom(fieldType) && !GeneralXynaObject.class.isAssignableFrom(fieldType)) {
        continue; //skip member
      }
      builder.nextObjectAsAttribute(variableName);
      if (List.class.isAssignableFrom(fieldType)) {
        Class<?> typeOfList = getGenericTypeOfList(field);
        if (GeneralXynaObject.class.isAssignableFrom(typeOfList)) {
          buildListJson((Class<? extends GeneralXynaObject>) typeOfList, (List<? extends GeneralXynaObject>) value);
        } else {
          buildPrimitiveListJson(field, (List<?>) value);
        }
      } else if (GeneralXynaObject.class.isAssignableFrom(fieldType)) {
        buildXynaObjectJson((GeneralXynaObject) value);
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
    if (types != null && types.length > 0) {
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
    
    if(xoa == null) {
      String fqn = cGxo.getCanonicalName();
      if(GenerationBase.isReservedServerObjectByFqClassName(fqn)){
        fqn = GenerationBase.getXmlNameForReservedClass(cGxo);
      }
      rtc = getRTCFromFqn(fqn);
      return new MetaInfo(fqn, rtc);
    }
    
    ClassLoader cl = cGxo.getClassLoader();
    if (cl != null && cl instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) cl;
      try {
        rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(clb.getRevision());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException("Could not get RuntimeContext of " + cGxo, e);
      }
    } else {
      rtc = getRTCFromFqn(xoa.fqXmlName());
    }
    return new MetaInfo(xoa.fqXmlName(), rtc);  
  }


  private RuntimeContext getRTCFromFqn(String fqn) {
    if("base.AnyType".equals(fqn)) {
      return null;
    }
    
    try {
      XynaFactoryControl xfctl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl(); 
      Long crevision = xfctl.getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject(fqn, revisions[0]);
      return xfctl.getRevisionManagement().getRuntimeContext(crevision);
    } catch(Exception e) {
      // Try to add content of revisions array to exception
      if(revisions.length > 0) {
          StringBuilder sb = new StringBuilder();
          sb.append("Could not get RTC from fqn:").append(fqn).append(" in revisions ");
          sb.append(revisions[0]);
          for(int i=1; i<revisions.length; i++) {
            sb.append(", ").append(revisions[i]);
          }
          logger.error(sb.toString(),e);
      }
      else {
          logger.error("Could not get RTC from fqn: " + fqn,e);
      }
      return null;
    }
  }
  

  @SuppressWarnings("unchecked")
  protected Set<String> getVariableNames(GeneralXynaObject gxo) {
    if (gxo instanceof XynaObject) {
      return ((XynaObject) gxo).getVariableNames();
    }

    Set<String> result = new HashSet<String>();
    if (gxo instanceof XynaException) {
      Class<?> clazz = gxo.getClass();
      while (clazz != XynaException.class) {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
          Field f = fields[i];
          if (f.getModifiers() == 2) { //private
            result.add(f.getName());
          }
        }
        clazz = clazz.getSuperclass();
      }
    } else {
      try {
        Method m = gxo.getClass().getDeclaredMethod("getVariableNames");
        return (Set<String>) m.invoke(gxo);
      } catch (Exception e) {
        throw new RuntimeException("Could not load variable names from " + gxo, e);
      }
    }
    return result;
  }
  

}

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
package xmcp.factorymanager.impl.orderinputconverter.json;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

public class XynaObjectJsonBuilder {
  
  private final long revision;
  private final JsonBuilder builder;
  
  public XynaObjectJsonBuilder(long revision) {
    this(revision, new JsonBuilder());
  }
  
  public XynaObjectJsonBuilder(RuntimeContext rc, JsonBuilder builder) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    this(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rc), builder);
  }
  
  public XynaObjectJsonBuilder(long revision, JsonBuilder builder) {
    this.builder = builder;
    this.revision = revision;
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
      long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
                                          .getRevisionDefiningXMOMObjectOrParent(gxol.getContainedFQTypeName(), revision);
      ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
      try {
        String fqClassName = GenerationBase.transformNameForJava(gxol.getContainedFQTypeName());
        // TODO could be list of XynaExceptions...
        Class<? extends GeneralXynaObject> clazz = (Class<? extends GeneralXynaObject>) cld.loadClassWithClassLoader(ClassLoaderType.MDM, fqClassName, fqClassName, rev);
        buildListJson(clazz, gxol);
      } catch (XPRC_InvalidPackageNameException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  private void buildListJson(Class<? extends GeneralXynaObject> listType, List<? extends GeneralXynaObject> gxol) {
    builder.startObject();
    MetaInfo meta = buildMetaForXynaObjectClass(listType);
    builder.addObjectAttribute(XynaObjectVisitor.META_TAG, meta);
    builder.addListAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG);
    for (GeneralXynaObject gxo : gxol) {
      if (gxo != null) {
        buildXynaObjectJson(gxo);
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
          Field field = XynaObjectVisitor.findField(gxo.getClass(), variableName);
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
              case LONG:
              case LONG_OBJ:
                builder.addNumberAttribute(variableName, (Number)value);
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
    ClassLoader cl = cGxo.getClassLoader();
    if (cl != null &&
        cl instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) cl;
      RuntimeContext rtc;
      try {
        rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(clb.getRevision());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      return new MetaInfo(xoa.fqXmlName(), rtc);
    } else {
      return new MetaInfo(xoa.fqXmlName());  
    }
  }
  
  @SuppressWarnings("unchecked")
  private Set<String> getVariableNames(GeneralXynaObject gxo) {
    try {
      Method m = gxo.getClass().getDeclaredMethod("getVariableNames");
      return (Set<String>) m.invoke(gxo);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }
  

}

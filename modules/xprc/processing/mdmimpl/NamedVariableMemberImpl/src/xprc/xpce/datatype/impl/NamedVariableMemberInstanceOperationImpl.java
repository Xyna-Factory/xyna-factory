/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xprc.xpce.datatype.impl;



import base.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.log4j.Logger;

import xprc.xpce.datatype.NamedVariableMemberSuperProxy;
import xprc.xpce.datatype.NamedVariableMemberInstanceOperation;
import xprc.xpce.datatype.NamedVariableMember;



public class NamedVariableMemberInstanceOperationImpl extends NamedVariableMemberSuperProxy implements NamedVariableMemberInstanceOperation {

  private static final long serialVersionUID = 1L;
  private static Logger logger = CentralFactoryLogging.getLogger(NamedVariableMemberInstanceOperationImpl.class);


  public NamedVariableMemberInstanceOperationImpl(NamedVariableMember instanceVar) {
    super(instanceVar);
  }


  @Override
  public GeneralXynaObject getMemberObject() {
    try {
      return (GeneralXynaObject) parentObject.get(getInstanceVar().getVarName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public String getMemberValue() {
    Object current = getUntypedMemberValue(getInstanceVar().getVarName());
    return current == null ? null : String.valueOf(current);

  }


  private Object getUntypedMemberValue(String path) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("getUntypedMemberValue: " + path);
      }
      List<String> parts = new ArrayList<String>();
      String[] nonListParts = StringUtils.fastSplit(path, '.', -1);
      for (String nonListPart : nonListParts) {
        if (nonListPart.endsWith("]")) {
          int listAccessIndex = nonListPart.indexOf('[');
          parts.add(nonListPart.substring(0, listAccessIndex));
          parts.add(nonListPart.substring(listAccessIndex + 1));
        } else {
          parts.add(nonListPart);
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("parts: " + parts);
      }
      Object current = parentObject;
      for (String part : parts) {
        if (part.endsWith("]")) {
          current = ((List<?>) current).get(Integer.parseInt(part.substring(0, part.length() - 1)));
        } else {
          current = ((GeneralXynaObject) current).get(part);
        }
      }
      return current;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void setMemberObject(GeneralXynaObject anyType) {
    try {
      XynaObject.set(parentObject, getInstanceVar().getVarName(), anyType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private GeneralXynaObject parentObject;


  public void setParentObject(GeneralXynaObject parentObject) {
    this.parentObject = parentObject;
  }


  private Field field;


  public Field getField() {
    try {
      if (field == null) {
        GeneralXynaObject gxo;
        String shortenedVarName;
        int dotIndex = getInstanceVar().getVarName().lastIndexOf('.');
        if (dotIndex >= 0) {
          gxo = (GeneralXynaObject) getUntypedMemberValue(getInstanceVar().getVarName().substring(0, dotIndex));
          shortenedVarName = getInstanceVar().getVarName().substring(dotIndex + 1);
        } else {
          gxo = parentObject;
          shortenedVarName = getInstanceVar().getVarName();
        }
        Class<?> currentClazz = gxo.getClass();
        try {
          field = currentClazz.getDeclaredField(shortenedVarName);
        } catch (NoSuchFieldException e) {
          Class<?> currentContextClazz = currentClazz.getSuperclass();
          while (currentContextClazz != null) {
            try {
              field = currentContextClazz.getDeclaredField(shortenedVarName);
              break;
            } catch (NoSuchFieldException ee) {

            }
            currentContextClazz = currentContextClazz.getSuperclass();
          }
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    return field;
  }


  @Override
  public void setMemberObjectList(List<GeneralXynaObject> anyType) {
    try {
      XynaObject.set(parentObject, getInstanceVar().getVarName(), anyType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void setMemberValue(String value) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("setting varName: " + getInstanceVar().getVarName());
      }
      GeneralXynaObject gxo;
      String shortenedVarName;
      int dotIndex = getInstanceVar().getVarName().lastIndexOf('.');
      if (dotIndex >= 0) {
        gxo = (GeneralXynaObject) getUntypedMemberValue(getInstanceVar().getVarName().substring(0, dotIndex));
        shortenedVarName = getInstanceVar().getVarName().substring(dotIndex + 1);
      } else {
        gxo = parentObject;
        shortenedVarName = getInstanceVar().getVarName();
      }

      Class<?> fieldType = getField().getType();
      Object adjustedValue;
      if (fieldType.isAssignableFrom(String.class)) {
        adjustedValue = value;
      } else if (fieldType.isAssignableFrom(Integer.class) || fieldType.isAssignableFrom(int.class)) {
        adjustedValue = Integer.parseInt(value);
      } else if (fieldType.isAssignableFrom(Long.class) || fieldType.isAssignableFrom(long.class)) {
        adjustedValue = Long.parseLong(value);
      } else if (fieldType.isAssignableFrom(Boolean.class) || fieldType.isAssignableFrom(boolean.class)) {
        adjustedValue = Boolean.parseBoolean(value);
      } else if (fieldType.isAssignableFrom(Float.class) || fieldType.isAssignableFrom(float.class)) {
        adjustedValue = Float.parseFloat(value);
      } else if (fieldType.isAssignableFrom(Double.class) || fieldType.isAssignableFrom(double.class)) {
        adjustedValue = Float.parseFloat(value);
      } else {
        // or throw?
        adjustedValue = value;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("gxo: " + gxo);
        logger.debug("shortenedVarName: " + shortenedVarName);
        logger.debug("adjustedValue: " + adjustedValue);
      }
      XynaObject.set(gxo, shortenedVarName, adjustedValue);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void setMemberValueListWrapped(List<? extends Text> value) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("setting varName: " + getInstanceVar().getVarName());
      }
      GeneralXynaObject gxo;
      String shortenedVarName;
      int dotIndex = getInstanceVar().getVarName().lastIndexOf('.');
      if (dotIndex >= 0) {
        gxo = (GeneralXynaObject) getUntypedMemberValue(getInstanceVar().getVarName().substring(0, dotIndex));
        shortenedVarName = getInstanceVar().getVarName().substring(dotIndex + 1);
      } else {
        gxo = parentObject;
        shortenedVarName = getInstanceVar().getVarName();
      }

      String fieldtype = getInstanceVar().getType();
      Function<String, Object> translator;
      if ("java.lang.String".equals(fieldtype)) {
        translator = (s) -> s;
      } else if ("int".equals(fieldtype) || "Integer".equals(fieldtype)) {
        translator = Integer::valueOf;
      } else if ("long".equals(fieldtype) || "Long".equals(fieldtype)) {
        translator = Long::valueOf;
      } else if ("boolean".equals(fieldtype) || "Boolean".equals(fieldtype)) {
        translator = Boolean::valueOf;
      } else if ("double".equals(fieldtype) || "Double".equals(fieldtype)) {
        translator = Double::valueOf;
      } else {
        throw new RuntimeException("unsupported type: " + fieldtype);
      }
      List<?> list = createList(value, translator);

      if (logger.isDebugEnabled()) {
        logger.debug("gxo: " + gxo);
        logger.debug("shortenedVarName: " + shortenedVarName);
      }
      XynaObject.set(gxo, shortenedVarName, list);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private <T> List<T> createList(List<? extends Text> data, Function<String, T> translator) {
    List<T> result = new ArrayList<T>();
    for (Text t : data) {
      if (t == null) {
        continue;
      }
      result.add(translator.apply(t.getText()));
    }
    return result;
  }


  @Override
  public void setMemberValueWrapped(Text text) {
    setMemberValue(text.getText());
  }
  
  
  @Override
  public List<String> getMemberListValue() {
    Object current = getUntypedMemberValue(getInstanceVar().getVarName());
    if (!getInstanceVar().getIsList()) {
      throw new RuntimeException("Member var must be a list");
    }
    if (current == null) {
      return null;
    } else {
      List<String> sl = new ArrayList<>();
      for (Object o : (List<?>)current) {
        sl.add(o == null ? null : String.valueOf(o));
      }
      return sl;
    }
  }

  @Override
  public List<GeneralXynaObject> getMemberObjectList() {
    try {
      return (List<GeneralXynaObject>)parentObject.get(getInstanceVar().getVarName());
    } catch( Exception e ) {
      throw new RuntimeException(e);
    }
  }

  
}

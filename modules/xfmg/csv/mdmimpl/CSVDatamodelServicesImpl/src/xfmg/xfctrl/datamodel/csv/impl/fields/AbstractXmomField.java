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
package xfmg.xfctrl.datamodel.csv.impl.fields;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

public abstract class AbstractXmomField implements XmomField {
  
  protected final String label;
  private final XmomField parent;
  private final String name;
  
  private Field field;
  private final FieldType fieldType;
  private final String fqClassName;
  
  public AbstractXmomField(String label, String name, XmomField parent, FieldType fieldType, String fqClassName) {
    this.label = label;
    this.name = name;
    this.parent = parent;
    this.fieldType = fieldType;
    this.fqClassName = fqClassName;
  }

  public String getLabel() {
    return label;
  }

  public String getPath() {
    String pp = parent.getPath();
    if( pp.isEmpty() ) {
      return this.name;
    } else {
      return pp+"."+name;
    }
  }
  
  private static final Pattern splitPattern = Pattern.compile("\\.");
  
  public String[] getPathElements() {
    return splitPattern.split(getPath());
  }
  

  public abstract Object getObject(GeneralXynaObject gxo);

  
  protected Object getBaseObject(GeneralXynaObject gxo) {
    Object po = parent.getObject(gxo);
    if( po == null ) {
      return null;
    }
    try {
      if( field == null ) {
        createField(po.getClass());
      }
      return field.get(po);
    } catch( Exception e ) {
      throw new RuntimeException("Failed to access "+name+" for "+po.getClass().getName(), e );
    }
  }

  private void createField(Class<?> xoClass) throws NoSuchFieldException {
    Class<?> currentClass = xoClass;
    while(this.field == null &&
          currentClass != null) {
      try {
        this.field = currentClass.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        currentClass = currentClass.getSuperclass();
      }  
    }
    this.field.setAccessible(true);
  }

  @Override
  public FieldType getType() {
    return fieldType;
  }

  @Override
  public XmomField getParent() {
    return parent;
  }
  
  public String getFqClassName() {
    return fqClassName;
  }
  
  public String getVarName() {
    return name;
  }
  
  public boolean isList() {
    return false;
  }

}

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import xfmg.xfctrl.datamodel.csv.parameter.StringTransformation;
import xfmg.xfctrl.datamodel.csv.parameter.StringTransformationImplementation;
import xfmg.xfctrl.datamodel.csv.parameter.StringTransformationMethodCall;

public class XmomFieldStringTransformation implements XmomField {

  private XmomField parent;
  private StringTransformationImpl stringTransformationImpl;

  public XmomFieldStringTransformation(XmomField parent, StringTransformation stringTransformation) {
    this.parent = parent;
    this.stringTransformationImpl = parseStringTransformation(stringTransformation);
  }

  
  private StringTransformationImpl parseStringTransformation(StringTransformation stringTransformation) {
    if( stringTransformation instanceof StringTransformationMethodCall ) {
      String methodName = ((StringTransformationMethodCall)stringTransformation).getMethodName();
      return new StringTransformationMethodCallImpl(methodName);
    } else if( stringTransformation instanceof StringTransformationImplementation ) {
      return new StringTransformationImplementationImpl((StringTransformationImplementation)stringTransformation);
    } else {
      throw new IllegalArgumentException("Unexpected StringTransformation "+stringTransformation);
    }
  }

  public String getLabel() {
   return parent.getLabel();
  }

  public String getPath() {
    return parent.getPath();
  }

  public Object getObject(GeneralXynaObject gxo) {
    return stringTransformationImpl.getObject(parent.getObject(gxo));
  }
  
  
  private interface StringTransformationImpl {

    Object getObject(Object object);
    
  }
  
  private static class StringTransformationMethodCallImpl implements StringTransformationImpl {

    private String methodName;
    private Method method;

    public StringTransformationMethodCallImpl(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public Object getObject(Object object) {
      if( object == null ) {
        return null;
      }
      try {
        return getOrCreateMethod(object).invoke(object);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    private Method getOrCreateMethod(Object object) {
      if( method == null ) {
        for( Method m : object.getClass().getDeclaredMethods() ) {
          if( m.getName().equals(methodName) ) {
            method = m;
            break;
          }
        }
        if( method == null ) {
          throw new IllegalStateException("No method "+methodName+" for "+object.getClass());
        }
      }
      return method;
    }
    
  }
  private static class StringTransformationImplementationImpl implements StringTransformationImpl {

    private StringTransformationImplementation impl;

    public StringTransformationImplementationImpl(StringTransformationImplementation impl) {
      this.impl = impl;
    }

    @Override
    public Object getObject(Object object) {
      if( object == null ) {
        return null;
      }
      if( object instanceof GeneralXynaObject ) {
        return impl.transformToString((GeneralXynaObject)object);
      } else {
        throw new IllegalArgumentException("StringTransformationImplementation cannot be applied to "+object.getClass());
      }
    }
    
  }

  public FieldType getType() {
    return null;
  }

  public XmomField getParent() {
    return null;
  }

  public boolean isList() {
    return false;
  }
}

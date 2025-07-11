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



import java.lang.reflect.Method;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xprc.xpce.datatype.NamedVariableMember;
import xprc.xpce.datatype.NamedXMOMMember;



public class XmomReflection {

  public static void setParentObject(NamedVariableMember nvm, GeneralXynaObject rootObject) {
    set(nvm, "setParentObject", GeneralXynaObject.class, rootObject);
  }


  public static void setParentObject(NamedXMOMMember nxm, GeneralXynaObject xynaObject) {
    set(nxm, "setParentObject", GeneralXynaObject.class, xynaObject);
  }


  public static void setAnyType(NamedXMOMMember nxm, GeneralXynaObject o) {
    set(nxm, "setAnyType", GeneralXynaObject.class, o);
  }


  public static void setDom(NamedXMOMMember nxm, DOM dom) {
    set(nxm, "setDOM", DOM.class, dom);
  }


  private static void set(Object nvm, String methodName, Class<?> argClass, Object value) {
    try {
      Object instOpImpl = getImplementation(nvm);
      Method setParentMethod = instOpImpl.getClass().getMethod(methodName, argClass);
      setParentMethod.invoke(instOpImpl, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Object getImplementation(Object obj) throws Exception {
    Method interimGetter = obj.getClass().getDeclaredMethod("getImplementationOfInstanceMethods");
    interimGetter.setAccessible(true);
    Object interim = interimGetter.invoke(obj);
    Method implGetter = interim.getClass().getDeclaredMethod("getInstanceOperationInstance");
    implGetter.setAccessible(true);
    return implGetter.invoke(interim);
  }
}

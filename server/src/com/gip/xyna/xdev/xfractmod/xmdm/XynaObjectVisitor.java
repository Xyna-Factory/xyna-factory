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

package com.gip.xyna.xdev.xfractmod.xmdm;



import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;



public abstract class XynaObjectVisitor {
  
  public abstract void visitXynaObject(GeneralXynaObject xo);
  
  public static void visit(GeneralXynaObject xo, XynaObjectVisitor xov) {
    xov.visitXynaObject(xo);
    try {
      Method getVariableNamesMethod = xo.getClass().getDeclaredMethod("getVariableNames");
      Set<String> varNames = (Set<String>) getVariableNamesMethod.invoke(xo);
      for (String varName : varNames) {
        Object value = xo.get(varName);
        if (value instanceof List) {
          for (Object listValue : (List)value) {
            if (listValue instanceof GeneralXynaObject) {
              visit((GeneralXynaObject) listValue, xov);
            }
          }
        } else if (value instanceof GeneralXynaObject) {
          visit((GeneralXynaObject) value, xov);
        } else {
          // ntb
        }
      }
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }
    
  }

  

}
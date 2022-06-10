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
package com.gip.xyna;



import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.IdentityHashMap;



public class ReflectiveObjectVisitor {

  private final Object o;


  public ReflectiveObjectVisitor(Object o) {
    this.o = o;
  }


  public void traverse() {
    traverse(o, 0, new IdentityHashMap<Object, Boolean>());
  }


  private int depth;


  public int getDepth() {
    return depth;
  }

  public interface Filter {
    public boolean skipClass(Class<?> clazz, Object value);
  }

  private void traverse(Object value, int depth, IdentityHashMap<Object, Boolean> alreadyDone) {
    this.depth = depth;
    if (value == null) {
      visitNull();
    } else if (value instanceof Boolean) {
      visitBoolean(value);
    } else if (value instanceof Number) {
      visitNumber(value);
    } else if (value instanceof String) {
      visitString(value);
    } else if (value instanceof Enum) {
      visitEnum(value);
    } else if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      visitArrayStart(value, length);
      for (int i = 0; i < length; i++) {
        Object oAtIndex = Array.get(value, i);
        visitNextArrayElement(i, length);
        traverse(oAtIndex, depth + 1, alreadyDone);
        this.depth = depth;
      }
      visitArrayEnd();
    } else {
      boolean existed = alreadyDone.put(value, Boolean.TRUE) != null;
      if (existed) {
        visitReference(value);
        return;
      }

      Class<?> c = value.getClass();
      if (filter != null && filter.skipClass(c, value)) {
        return;
      }
      visitFieldsOfObjectStart(value);
      int cnt = 0;
      while (c != Object.class) {
        try {
          Field[] fields = c.getDeclaredFields();
          int length = fields.length;
          for (int i = 0; i < length; i++) {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers())) {
              continue;
            }

            visitNextField(cnt++, field);
            field.setAccessible(true);
            try {
              traverse(field.get(value), depth + 1, alreadyDone);
              this.depth = depth;
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          }

          c = c.getSuperclass();
        } catch (AccessControlException ace) {
          // can happen if tracing the object hierarchy leads into sun-packages
          break;
        }

      }
      visitFieldsOfObjectEnd(value);
    }
  }


  public void visitNextField(int i, Field field) {
  }


  public void visitNextArrayElement(int i, int length) {
  }


  public void visitFieldsOfObjectEnd(Object value) {
  }


  public void visitFieldsOfObjectStart(Object value) {
  }


  public void visitReference(Object value) {
  }


  public void visitArrayEnd() {
  }


  public void visitArrayStart(Object value, int length) {
  }


  public void visitEnum(Object value) {
  }


  public void visitString(Object value) {
  }


  public void visitNumber(Object value) {
  }


  public void visitBoolean(Object value) {
  }


  public void visitNull() {
  }


  private Filter filter;
  
  public void setFilter(Filter filter) {
    this.filter = filter;
  }

}

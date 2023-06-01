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
package com.gip.xyna;



import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.gip.xyna.ReflectiveObjectVisitor.Filter;



//TODO arrays von simplen datentypen ohne zeilenumbruch darstellen
public class ObjectStringRepresentation {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ObjectStringRepresentation.class);

  public static void createStringRepOfObject(final StringBuilder sb, Object value) throws IllegalArgumentException,
      IllegalAccessException {
    ReflectiveObjectVisitor rov = new ReflectiveObjectVisitor(value) {

      @Override
      public void visitNextField(int i, Field field) {
        if (i > 0) {
          sb.append(",\n");
          createDepth(sb, getDepth()+1);
        } 
        sb.append(field.getName()).append(" = ");
      }

      @Override
      public void visitNextArrayElement(int i, int length) {
        if (i > 0) {
          sb.append(",\n");
          createDepth(sb, getDepth());
          sb.append(" ");
        }
      }

      @Override
      public void visitFieldsOfObjectEnd(Object value) {
        sb.append("}");
      }

      @Override
      public void visitFieldsOfObjectStart(Object value) {
        checkLen();
        sb.append(value.getClass().getName()).append("={\n");
        createDepth(sb, getDepth()+1);
      }

      private void checkLen() {
        if (sb.length() > (Integer.MAX_VALUE >> 2)) {
          sb.append("\ntoo much data");
          throw new RuntimeException();
        }
      }

      @Override
      public void visitReference(Object value) {
        sb.append("@ref");
      }

      @Override
      public void visitArrayEnd() {
        sb.append("]");
      }

      @Override
      public void visitArrayStart(Object value, int length) {
        checkLen();
        sb.append("[");
      }

      @Override
      public void visitEnum(Object value) {
        sb.append(String.valueOf(value));
      }

      @Override
      public void visitString(Object value) {
        sb.append("'").append(String.valueOf(value)).append("'");
      }

      @Override
      public void visitNumber(Object value) {
        sb.append(String.valueOf(value));
      }

      @Override
      public void visitBoolean(Object value) {
        sb.append(String.valueOf(value));
      }

      @Override
      public void visitNull() {
        sb.append("null");
      }
      
    };
    rov.setFilter(new Filter() {
      public boolean skipClass(Class<?> clazz, Object value) {
        if (EnumSet.class.isAssignableFrom(clazz)) {
          sb.append("EnumSet: ").append(String.valueOf(value));
          return true;
        }
        if (EnumMap.class.isAssignableFrom(clazz)) {
          sb.append("EnumMap: ").append(String.valueOf(value));
          return true;
        }
        if (LinkedList.class.isAssignableFrom(clazz)) {
          sb.append("LinkedList: ").append(String.valueOf(value));
          return true;
        }
        return false;
      }
    });
    try {
      rov.traverse();
    } catch (RuntimeException e) {
      logger.info(null, e);
    }
  }


  private static void createDepth(StringBuilder sb, int depth) {
    sb.append("\t\t");
    for (int i = 0; i < depth; i++) {
      sb.append("   ");
    }
  }

}

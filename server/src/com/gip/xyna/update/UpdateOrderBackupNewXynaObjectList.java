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
package com.gip.xyna.update;



import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.ReflectiveObjectVisitor;
import com.gip.xyna.update.UpdateRewriteOrderBackupAndCronLikeOrders.Transformation;
import com.gip.xyna.update.outdatedclasses_6_1_2_3.Container;
import com.gip.xyna.update.outdatedclasses_6_1_2_3.XynaObjectList;
import com.gip.xyna.update.specialstorablesignoringserialversionuid.OrderInstanceBackupIgnoringSerialVersionUID;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.SerialVersionIgnoringObjectInputStream;



public class UpdateOrderBackupNewXynaObjectList extends UpdateJustVersion {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateOrderBackupNewXynaObjectList.class);


  public UpdateOrderBackupNewXynaObjectList(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion, true, true, true, true);
    setExecutionTime(ExecutionTime.afterUpdateGeneratedClassesBeforeRewriteOrderBackup);
  }


  private enum ChildRelationType {
    FIELD, ARRAY_ELEMENT;
  }

  private static class ChildRelation {

    private ChildRelationType type;
    private Field field;
    private int arrayIndex;
    private Object owner;


    public static ChildRelation fieldOf(Object value) {
      ChildRelation cr = new ChildRelation();
      cr.type = ChildRelationType.FIELD;
      cr.owner = value;
      return cr;
    }


    public static ChildRelation elementOf(Object value) {
      ChildRelation cr = new ChildRelation();
      cr.type = ChildRelationType.ARRAY_ELEMENT;
      cr.owner = value;
      return cr;
    }


    public void setArrayIndex(int idx) {
      this.arrayIndex = idx;
    }


    public void setField(Field field) {
      this.field = field;
    }
  }

  public interface ObjectTransformation {

    public boolean decideReplacement(Stack<ChildRelation> parents, Object candidate);


    public Object transform(Object value);
  }

  public static class ObjectReplacement {

    //kann nicht den root ersetzen
    public static void replace(Object root, final ObjectTransformation transf) {
      final Stack<ChildRelation> stack = new Stack<ChildRelation>();
      ReflectiveObjectVisitor rov = new ReflectiveObjectVisitor(root) {

        private final IdentityHashMap<Object, Object> transformations = new IdentityHashMap<Object, Object>();


        @Override
        public void visitNextField(int i, Field field) {
          stack.peek().setField(field);
        }


        @Override
        public void visitNextArrayElement(int i, int length) {
          stack.peek().setArrayIndex(i);
        }


        @Override
        public void visitFieldsOfObjectEnd(Object value) {
          stack.pop();
          value = checkReplacement(value);
        }


        @Override
        public void visitFieldsOfObjectStart(Object value) {
          stack.add(ChildRelation.fieldOf(value));
        }


        private Object checkReplacement(Object value) {
          if (transf.decideReplacement(stack, value)) {
            Object newValue = transf.transform(value);
            transformations.put(value, newValue);
            setInParent(newValue);
            if (logger.isTraceEnabled()) {
              logger.trace("transformed " + value + " to " + newValue + " to set in " + getPath());
            }
            value = newValue;
          }
          return value;
        }


        private void setInParent(Object changedObject) {
          ChildRelation cr = stack.peek();
          switch (cr.type) {
            case FIELD :
              try {
                cr.field.set(cr.owner, changedObject);
              } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("could not set field " + cr.field.getName() + " in " + cr.owner + " (" + getPath()
                    + ") to " + changedObject, e);
              } catch (SecurityException e) {
                throw new RuntimeException(e);
              } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
              }
              break;
            case ARRAY_ELEMENT :
              try {
                Array.set(cr.owner, cr.arrayIndex, changedObject);
              } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("could not set index " + cr.arrayIndex + " in " + cr.owner + " (" + getPath() + ") to "
                    + changedObject, e);
              }
              break;
            default :
              throw new RuntimeException();
          }
        }


        protected String getPath() {
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < stack.size(); i++) {
            ChildRelation cr = stack.get(i);
            if (i > 0) {
              sb.append("->");
            }
            switch (cr.type) {
              case FIELD :
                sb.append("field '").append(cr.field.getName()).append("'");
                break;
              case ARRAY_ELEMENT :
                sb.append("index ").append(cr.arrayIndex);
                break;
              default :
                sb.append("unknown");
            }
          }
          return sb.toString();
        }


        @Override
        public void visitReference(Object value) {
          Object transformed = transformations.get(value);
          if (transformed != null) {
            setInParent(transformed);
          }
        }


        @Override
        public void visitArrayEnd() {
          stack.pop();
        }


        @Override
        public void visitArrayStart(Object value, int length) {
          stack.add(ChildRelation.elementOf(value));
        }

      };
      rov.traverse();
    }
  }


  private static final Transformation transformContainerAndList = new Transformation() {

    public void transform(OrderInstanceBackupIgnoringSerialVersionUID ob) {
      transformObject(ob);
    }

  };


  private static void transformObject(Object o) {
    ObjectReplacement.replace(o, new ObjectTransformation() {

      public boolean decideReplacement(Stack<ChildRelation> parents, Object candidate) {
        if (candidate instanceof Container) {
          return true;
        } else if (candidate instanceof XynaObjectList) {
          if (parentIsNotContainerArray(parents)) {
            return true;
          }
        }
        return false;
      }


      private boolean parentIsNotContainerArray(Stack<ChildRelation> parents) {
        ChildRelation last = parents.peek();
        if (last.type == ChildRelationType.ARRAY_ELEMENT) {
          ChildRelation beforeLast = parents.get(parents.size() - 2);
          if (beforeLast.type == ChildRelationType.FIELD) {
            if (beforeLast.owner instanceof Container) {
              return false;
            }
          }
        }
        return true;
      }


      public Object transform(Object value) {
        if (value instanceof Container) {
          return ((Container) value).readResolvePublic();
        } else if (value instanceof XynaObjectList) {
          return ((XynaObjectList<?>) value).readResolvePublic();
        }
        return null;
      }
    });
  }


  @Override
  protected void update() throws XynaException {

    //xynaobjectlist wurde ge�ndert, indem es nun von generalxynaobjectlist ableitet, anstatt eine eigene implementierung zu haben
    //mit folgendem mapping werden deserialisierte xynaobjectlisten mit dem classdescriptor der alten liste deserialisiert.
    SerialVersionIgnoringObjectInputStream.addClassDescriptorMapping("com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList",
                                                                     XynaObjectList.class);
    SerialVersionIgnoringObjectInputStream.addClassDescriptorMapping("com.gip.xyna.xdev.xfractmod.xmdm.Container", Container.class);
    //eintrag wird nach updaterewriterorderbackup gecleared.

    //dann werden die objekte im objektbaum transformiert.
    //leider hat verwendung von readResolve nicht funktioniert, weil:
    //1. XynaObjectList ist kein XynaObject mehr, aber alte Container-Objekte sind als XynaObject[] serialisiert worden
    //2. Workaround: XynaObjectList erst nach ReadResolve von Container umwandeln
    //2a. Daf�r muss man wissen, wann Container deserialisiert werden
    //2b. Daf�r bekam Container readObject
    //2c. Deshalb war Container.readObject im Stack beim deserialisieren
    //2d. Deshalb wurde der falsche Classloader f�r Container-Inhalte verwendet (resolveClassLoader verwendet den letzten Classloader im Stack, der =! null ist)
    //3. Workaround: Classloader von Container �ndern -> schwer
    //4. -> Also Objekte nicht per readResolve transformieren, sondern in eigener Transformation
    UpdateRewriteOrderBackupAndCronLikeOrders.addTransformation(transformContainerAndList);
  }

}

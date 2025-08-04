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
package base.list.impl;

import base.list.SortCriterion;
import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GeneralComparator {
   private GeneralComparator() {
   }

   public static Comparator<GeneralXynaObject> createComparator(List<? extends SortCriterion> sortCriteria) {
      ComparatorList comparator = new ComparatorList(sortCriteria.size());

      for(SortCriterion sc : sortCriteria) {
         comparator.add(new PartComparator(sc));
      }

      return (Comparator<GeneralXynaObject>) ComparatorUtils.nullAware(comparator, false);
   }

   public static Comparator<GeneralXynaObject> createComparator(base.list.comparator.Comparator comparator) {
      return new GeneralXynaObjectComparator(comparator);
   }

   private static class PartComparator implements Comparator<GeneralXynaObject>, ObjectComparator.NotComparableHandler {
      private GetterSetter getterSetter;
      private Comparator<Object> comp;

      public PartComparator(SortCriterion sc) {
         this.getterSetter = new GetterSetter(sc.getFieldName());
         Comparator<Object> c = new ObjectComparator(this);
         if (sc.getDescending()) {
            c =  (Comparator<Object>) ComparatorUtils.reverse(c);
         }

         this.comp = (Comparator<Object>) ComparatorUtils.nullAware(c, sc.getNullFirst());
      }

      public int compare(GeneralXynaObject o1, GeneralXynaObject o2) {
         try {
            return this.comp.compare(this.getterSetter.getFrom(o1), this.getterSetter.getFrom(o2));
         } catch (InvalidObjectPathException e) {
            throw new SortException("Could not access " + this.getterSetter.getPath(), e);
         }
      }

      public int compareNotComparable(Object o1, Object o2) {
         throw new SortException(o1.getClass() + " is not instanceof Comparable");
      }
   }

   public static class SortException extends RuntimeException {
      private static final long serialVersionUID = 1L;

      public SortException(String message, Throwable cause) {
         super(message, cause);
      }

      public SortException(String message) {
         super(message);
      }
   }

   private static class ComparatorList implements Comparator<GeneralXynaObject> {
      private List<Comparator<GeneralXynaObject>> subComparators;

      public ComparatorList(int size) {
         this.subComparators = new ArrayList<Comparator<GeneralXynaObject>>(size);
      }

      public void add(Comparator<GeneralXynaObject> comparator) {
         this.subComparators.add(comparator);
      }

      public int compare(GeneralXynaObject o1, GeneralXynaObject o2) {
         int comp = 0;

         for(Comparator<GeneralXynaObject> sub : this.subComparators) {
            comp = sub.compare(o1, o2);
            if (comp != 0) {
               return comp;
            }
         }

         return 0;
      }
   }

   private static class ObjectComparator implements Comparator<Object> {
      private NotComparableHandler notComparableHandler;

      public ObjectComparator(NotComparableHandler notComparableHandler) {
         this.notComparableHandler = notComparableHandler;
      }

      public int compare(Object o1, Object o2) {
         return o1 instanceof Comparable ? ((Comparable)o1).compareTo(o2) : this.notComparableHandler.compareNotComparable(o1, o2);
      }

      public interface NotComparableHandler {
         int compareNotComparable(Object var1, Object var2);
      }
   }

   public static class GeneralXynaObjectComparator implements Comparator<GeneralXynaObject> {
      private base.list.comparator.Comparator comparator;

      public GeneralXynaObjectComparator(base.list.comparator.Comparator comparator) {
         this.comparator = comparator;
      }

      public int compare(GeneralXynaObject gxo1, GeneralXynaObject gxo2) {
         return this.comparator.compare(gxo1, gxo2);
      }
   }
}

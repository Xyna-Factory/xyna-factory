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
package base.list.comparator.impl;

import base.list.SortCriterion;
import base.list.comparator.SortCriteriaComparator;
import base.list.comparator.SortCriteriaComparatorInstanceOperation;
import base.list.comparator.SortCriteriaComparatorSuperProxy;
import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.List;

public class SortCriteriaComparatorInstanceOperationImpl extends SortCriteriaComparatorSuperProxy implements SortCriteriaComparatorInstanceOperation {
   private static final long serialVersionUID = 1L;
   private transient Comparator comparator;

   public SortCriteriaComparatorInstanceOperationImpl(SortCriteriaComparator instanceVar) {
      super(instanceVar);
   }

   public int compare(GeneralXynaObject anyType1, GeneralXynaObject anyType2) {
      if (this.comparator == null) {
         this.comparator = createComparator(this.getInstanceVar().getSortCriterion());
      }

      return this.comparator.compare(anyType1, anyType2);
   }


   private void writeObject(ObjectOutputStream s) throws IOException {
      s.defaultWriteObject();
   }

   private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
      s.defaultReadObject();
   }

   public static Comparator createComparator(List<? extends SortCriterion> sortCriteria) {
      ComparatorUtils.ComparatorList<GeneralXynaObject> comparator = new ComparatorUtils.ComparatorList(sortCriteria.size());

      for(SortCriterion sc : sortCriteria) {
         comparator.add(new PartComparator(sc));
      }

      return ComparatorUtils.nullAware(comparator, false);
   }

   private static class PartComparator implements Comparator<GeneralXynaObject>, ComparatorUtils.ObjectComparator.NotComparableHandler {
      private String path;
      private Comparator comp;

      public PartComparator(SortCriterion sc) {
         String path = sc.getFieldName();
         if (path.startsWith("%")) {
            int idx = path.indexOf(46);
            this.path = path.substring(idx + 1);
         } else {
            this.path = path;
         }

         Comparator<? super Object> c = new ComparatorUtils.ObjectComparator(this);
         if (sc.getDescending()) {
            c = ComparatorUtils.reverse(c);
         }

         this.comp = ComparatorUtils.nullAware(c, sc.getNullFirst());
      }

      public int compare(GeneralXynaObject o1, GeneralXynaObject o2) {
         try {
            return this.comp.compare(this.getPath(o1), this.getPath(o2));
         } catch (InvalidObjectPathException e) {
            throw new SortException("Could not access " + this.path, e);
         }
      }

      private Object getPath(GeneralXynaObject o) throws InvalidObjectPathException {
         try {
            return o.get(this.path);
         } catch (NullPointerException var3) {
            return null;
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
}

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

import base.Count;
import base.list.ListUtilsServiceOperation;
import base.list.SortCriterion;
import base.list.XmomField;
import base.list.ZeroComparison;
import base.list.comparator.Comparator;
import base.list.filter.Filter;
import base.math.IntegerNumber;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ListUtilsServiceOperationImpl implements XynaObject.ExtendedDeploymentTask, ListUtilsServiceOperation {
   public void onDeployment() throws XynaException {
   }

   public void onUndeployment() throws XynaException {
   }

   public Long getOnUnDeploymentTimeout() {
      return null;
   }

   public XynaObject.BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
      return null;
   }

   private boolean isNullOrEmpty(List<GeneralXynaObject> list) {
      return list == null || list.isEmpty();
   }

   public List<GeneralXynaObject> sort(List<GeneralXynaObject> list, Comparator comparator) {
      if (this.isNullOrEmpty(list)) {
         return Collections.emptyList();
      } else {
         List<GeneralXynaObject> ret = new ArrayList<>(list);
         Collections.sort(ret, GeneralComparator.createComparator(comparator));
         return ret;
      }
   }

   public List<GeneralXynaObject> sortWithCriteria(List<GeneralXynaObject> list, List<? extends SortCriterion> sortCriteria) {
      if (this.isNullOrEmpty(list)) {
         return Collections.emptyList();
      } else {
         List<GeneralXynaObject> ret = new ArrayList<>(list);
         Collections.sort(ret, GeneralComparator.createComparator(sortCriteria));
         return ret;
      }
   }

  public List<GeneralXynaObject> filter(List<GeneralXynaObject> list, Filter filter) {
      return this.isNullOrEmpty(list) ? Collections.emptyList() : CollectionUtils.filter(list, FilterUtils.createFilter(filter));
   }

   public List<GeneralXynaObject> filterByComparator(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo) {
      return this.isNullOrEmpty(list) ? Collections.emptyList() : CollectionUtils.filter(list, FilterUtils.createFilter(comparator, compareTo));
   }

   public List<GeneralXynaObject> filterByComparatorAndZeroComparison(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo, ZeroComparison zeroComparison) {
      return this.isNullOrEmpty(list) ? Collections.emptyList() : CollectionUtils.filter(list, FilterUtils.createFilter(comparator, compareTo, zeroComparison));
   }

   public List<GeneralXynaObject> sortInnerList(List<GeneralXynaObject> list, XmomField field, Comparator comparator) {
      return null;
   }

   public List<GeneralXynaObject> sortInnerPrimitiveList(List<GeneralXynaObject> list, SortCriterion sortCriterion) {
      return null;
   }

   public List<GeneralXynaObject> unique(List<GeneralXynaObject> list, Comparator comparator) {
      return this.isNullOrEmpty(list) ? Collections.emptyList() : UniqueUtils.unique(list, GeneralComparator.createComparator(comparator));
   }

   public List<GeneralXynaObject> notUnique(List<GeneralXynaObject> list, Comparator comparator) {
      return this.isNullOrEmpty(list) ? Collections.emptyList() : UniqueUtils.notUnique(list, GeneralComparator.createComparator(comparator));
   }

   public Count count(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo) {
      return this.isNullOrEmpty(list) ? new Count(0L) : new Count((long)CountUtils.count(list, FilterUtils.createFilter(comparator, compareTo)));
   }

   public Count countWithZeroComparison(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo, ZeroComparison zeroComparison) {
      return this.isNullOrEmpty(list) ? new Count(0L) : new Count((long)CountUtils.count(list, FilterUtils.createFilter(comparator, compareTo, zeroComparison)));
   }

   public List<base.list.pair.Pair> merge(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB, Comparator comparator) {
      return UniqueUtils.merge(listA, listB, GeneralComparator.createComparator(comparator));
   }

   public List<GeneralXynaObject> difference(List<GeneralXynaObject> minuend, List<GeneralXynaObject> subtrahend, Comparator comparator) {
      if (this.isNullOrEmpty(minuend)) {
         return Collections.emptyList();
      } else {
         return this.isNullOrEmpty(subtrahend) ? minuend : SetUtils.difference(minuend, subtrahend, GeneralComparator.createComparator(comparator));
      }
   }

   public List<GeneralXynaObject> intersection(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB, Comparator comparator) {
      if (this.isNullOrEmpty(listA)) {
         return Collections.emptyList();
      } else {
         return this.isNullOrEmpty(listB) ? Collections.emptyList() : SetUtils.intersection(listA, listB, GeneralComparator.createComparator(comparator));
      }
   }

   public Container split(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo) {
      Container c = new Container(new XynaObject[0]);
      if (this.isNullOrEmpty(list)) {
         c.add(new XynaObjectList(compareTo.getClass()));
         c.add(new XynaObjectList(compareTo.getClass()));
         return c;
      } else {
         XynaObjectAnnotation xoa = (XynaObjectAnnotation)compareTo.getClass().getAnnotation(XynaObjectAnnotation.class);
         Pair<List<GeneralXynaObject>, List<GeneralXynaObject>> split = split(list, FilterUtils.createFilter(comparator, compareTo));
         c.add(new XynaObjectList((List)split.getFirst(), xoa.fqXmlName()));
         c.add(new XynaObjectList((List)split.getSecond(), xoa.fqXmlName()));
         return c;
      }
   }


   public static <T> Pair<List<T>, List<T>> split(Collection<T> from, CollectionUtils.Filter<T> filter) {
     List<T> toTrue = new ArrayList<>();
     List<T> toFalse = new ArrayList<>();

     for (T f : from) {
       if (filter.accept(f)) {
         toTrue.add(f);
       } else {
         toFalse.add(f);
       }
     }

      return Pair.of(toTrue, toFalse);
   }

   public List<GeneralXynaObject> remove(List<GeneralXynaObject> list, GeneralXynaObject element, Comparator comparator) {
      return this.isNullOrEmpty(list) ? Collections.emptyList() : SetUtils.remove(list, element, GeneralComparator.createComparator(comparator));
   }

   public List<GeneralXynaObject> replace(List<GeneralXynaObject> list, GeneralXynaObject element, Comparator comparator) {
      return this.isNullOrEmpty(list) ? Collections.singletonList(element) : SetUtils.replace(list, element, GeneralComparator.createComparator(comparator), false);
   }

   public List<GeneralXynaObject> replaceOrAdd(List<GeneralXynaObject> list, GeneralXynaObject element, Comparator comparator) {
      return this.isNullOrEmpty(list) ? Collections.singletonList(element) : SetUtils.replace(list, element, GeneralComparator.createComparator(comparator), true);
   }

   public List<GeneralXynaObject> removeAndAddOrReplace(List<GeneralXynaObject> existing, List<GeneralXynaObject> remove, List<GeneralXynaObject> addOrReplace, Comparator comparator) {
      return this.isNullOrEmpty(existing) ? addOrReplace : SetUtils.removeAndAddOrReplace(existing, remove, addOrReplace, GeneralComparator.createComparator(comparator));
   }

   public List<base.list.pair.Pair> pair(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB) {
      Iterator<GeneralXynaObject> iterA = listA.iterator();
      Iterator<GeneralXynaObject> iterB = listB.iterator();
      List<base.list.pair.Pair> ret = new ArrayList<>();

      while(iterA.hasNext() && iterB.hasNext()) {
         base.list.pair.Pair pair = new base.list.pair.Pair();
         pair.setFirst((GeneralXynaObject)iterA.next());
         pair.setSecond((GeneralXynaObject)iterB.next());
         ret.add(pair);
      }

      while(iterA.hasNext()) {
         base.list.pair.Pair pair = new base.list.pair.Pair();
         pair.setFirst((GeneralXynaObject)iterA.next());
         ret.add(pair);
      }

      while(iterB.hasNext()) {
         base.list.pair.Pair pair = new base.list.pair.Pair();
         pair.setSecond((GeneralXynaObject)iterB.next());
         ret.add(pair);
      }

      return ret;
   }

   public List<GeneralXynaObject> removeEmpty(List<GeneralXynaObject> list) {
      List<GeneralXynaObject> ret = new ArrayList<>();

      for(GeneralXynaObject gxo : list) {
         if (gxo != null) {
            ret.add(gxo);
         }
      }

      return ret;
   }

   public List<GeneralXynaObject> simpleListToXMOMList(GeneralXynaObject from, XmomField fromField, GeneralXynaObject outputType, XmomField toField) {
      List<GeneralXynaObject> ret = new ArrayList<>();

      try {
         GetterSetter gsFrom = new GetterSetter(fromField.getField());
         List<?> innerList = (List<?>)gsFrom.getFrom(from);
         GetterSetter gsTo = new GetterSetter(toField.getField());

         for(Object o : innerList) {
            ret.add(gsTo.setTo(outputType.clone(), o));
         }

         return ret;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public List<GeneralXynaObject> subList(List<GeneralXynaObject> list, IntegerNumber start, IntegerNumber length) {
      int size = list.size();
      int begin = (int)start.getValue();
      if (begin < 0) {
         begin += size;
         if (begin < 0) {
            begin = 0;
         }
      }

      int len = (int)length.getValue();
      if (len <= 0) {
         len = size;
      }

      int end = begin + len;
      if (end > size) {
         end = size;
      }

      return (new ArrayList<>(list)).subList(begin, end);
   }
}

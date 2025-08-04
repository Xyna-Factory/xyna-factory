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



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

import base.Count;
import base.list.ListUtilsServiceOperation;
import base.list.SortCriterion;
import base.list.XmomField;
import base.list.ZeroComparison;
import base.list.comparator.Comparator;
import base.list.filter.Filter;
import base.list.pair.Pair;



public class ListUtilsServiceOperationImpl implements ExtendedDeploymentTask, ListUtilsServiceOperation {

  public void onDeployment() throws XynaException {
    // do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  private boolean isNullOrEmpty(List<GeneralXynaObject> list) {
    return list == null || list.isEmpty();
  }


  @Override
  public List<GeneralXynaObject> sort(List<GeneralXynaObject> list, Comparator comparator) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    List<GeneralXynaObject> ret = new ArrayList<>(list);
    Collections.sort(ret, GeneralComparator.createComparator(comparator));
    return ret;
  }


  @Override
  public List<GeneralXynaObject> sortWithCriteria(List<GeneralXynaObject> list, List<? extends SortCriterion> sortCriteria) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    List<GeneralXynaObject> ret = new ArrayList<>(list);
    Collections.sort(ret, GeneralComparator.createComparator(sortCriteria));
    return ret;
  }


  @Override
  public List<GeneralXynaObject> filter(List<GeneralXynaObject> list, Filter filter) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    return CollectionUtils.filter(list, FilterUtils.createFilter(filter));
  }


  @Override
  public List<GeneralXynaObject> filterByComparator(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    return CollectionUtils.filter(list, FilterUtils.createFilter(comparator, compareTo));
  }


  @Override
  public List<GeneralXynaObject> filterByComparatorAndZeroComparison(List<GeneralXynaObject> list, Comparator comparator,
                                                                     GeneralXynaObject compareTo, ZeroComparison zeroComparison) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    return CollectionUtils.filter(list, FilterUtils.createFilter(comparator, compareTo, zeroComparison));
  }


  @Override
  public List<GeneralXynaObject> sortInnerList(List<GeneralXynaObject> list, XmomField field, Comparator comparator) {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public List<GeneralXynaObject> sortInnerPrimitiveList(List<GeneralXynaObject> list, SortCriterion sortCriterion) {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public List<GeneralXynaObject> unique(List<GeneralXynaObject> list, Comparator comparator) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    return UniqueUtils.unique(list, GeneralComparator.createComparator(comparator));
  }


  @Override
  public List<GeneralXynaObject> notUnique(List<GeneralXynaObject> list, Comparator comparator) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    return UniqueUtils.notUnique(list, GeneralComparator.createComparator(comparator));
  }


  @Override
  public Count count(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo) {
    if (isNullOrEmpty(list)) {
      return new Count(0);
    }
    return new Count(CountUtils.count(list, FilterUtils.createFilter(comparator, compareTo)));
  }


  @Override
  public Count countWithZeroComparison(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo,
                                       ZeroComparison zeroComparison) {
    if (isNullOrEmpty(list)) {
      return new Count(0);
    }
    return new Count(CountUtils.count(list, FilterUtils.createFilter(comparator, compareTo, zeroComparison)));
  }


  @Override
  public List<Pair> merge(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB, Comparator comparator) {
    return UniqueUtils.merge(listA, listB, GeneralComparator.createComparator(comparator));
  }


  @Override
  public List<GeneralXynaObject> difference(List<GeneralXynaObject> minuend, List<GeneralXynaObject> subtrahend, Comparator comparator) {
    if (isNullOrEmpty(minuend)) {
      return Collections.emptyList();
    }
    if (isNullOrEmpty(subtrahend)) {
      return minuend;
    }
    return SetUtils.difference(minuend, subtrahend, GeneralComparator.createComparator(comparator));
  }


  @Override
  public List<GeneralXynaObject> intersection(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB, Comparator comparator) {
    if (isNullOrEmpty(listA)) {
      return Collections.emptyList();
    }
    if (isNullOrEmpty(listB)) {
      return Collections.emptyList();
    }
    return SetUtils.intersection(listA, listB, GeneralComparator.createComparator(comparator));
  }


  //FIXME Server generiert in ListUtils: 
  //public static Container split(List<GeneralXynaObject> anyType, Comparator comparator, GeneralXynaObject anyType25) {
  //  null dummyVarForMissingImport1 = null;
  //  null dummyVarForMissingImport2 = null;
  //  return base.list.ListUtilsImpl.split(anyType, comparator, anyType25);
  //}
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Container split(List<GeneralXynaObject> list, Comparator comparator, GeneralXynaObject compareTo) {
    Container c = new Container();
    if (isNullOrEmpty(list)) {
      c.add(new XynaObjectList(compareTo.getClass()));
      c.add(new XynaObjectList(compareTo.getClass()));
      return c;
    }
    XynaObjectAnnotation xoa = compareTo.getClass().getAnnotation(XynaObjectAnnotation.class);

    com.gip.xyna.utils.collections.Pair<List<GeneralXynaObject>, List<GeneralXynaObject>> split =
        split(list, FilterUtils.createFilter(comparator, compareTo));

    c.add(new XynaObjectList(split.getFirst(), xoa.fqXmlName()));
    c.add(new XynaObjectList(split.getSecond(), xoa.fqXmlName()));
    return c;
  }


  /**
   * FIXME in CollectionUtils 
   * Filtert alle Elemente aus from und trägt diese in to ein
   * @param from
   * @param filter
   * @return
   */
  public static <T> com.gip.xyna.utils.collections.Pair<List<T>, List<T>> split(Collection<T> from, CollectionUtils.Filter<T> filter) {
    List<T> toTrue = new ArrayList<T>();
    List<T> toFalse = new ArrayList<T>();
    for (T f : from) {
      if (filter.accept(f)) {
        toTrue.add(f);
      } else {
        toFalse.add(f);
      }
    }
    return com.gip.xyna.utils.collections.Pair.of(toTrue, toFalse);
  }


  @Override
  public List<GeneralXynaObject> remove(List<GeneralXynaObject> list, GeneralXynaObject element, Comparator comparator) {
    if (isNullOrEmpty(list)) {
      return Collections.emptyList();
    }
    return SetUtils.remove(list, element, GeneralComparator.createComparator(comparator));
  }


  @Override
  public List<GeneralXynaObject> replace(List<GeneralXynaObject> list, GeneralXynaObject element, Comparator comparator) {
    if (isNullOrEmpty(list)) {
      return Collections.singletonList(element);
    }
    return SetUtils.replace(list, element, GeneralComparator.createComparator(comparator), false);
  }


  @Override
  public List<GeneralXynaObject> replaceOrAdd(List<GeneralXynaObject> list, GeneralXynaObject element, Comparator comparator) {
    if (isNullOrEmpty(list)) {
      return Collections.singletonList(element);
    }
    return SetUtils.replace(list, element, GeneralComparator.createComparator(comparator), true);
  }


  @Override
  public List<GeneralXynaObject> removeAndAddOrReplace(List<GeneralXynaObject> existing, List<GeneralXynaObject> remove,
                                                       List<GeneralXynaObject> addOrReplace, Comparator comparator) {
    if (isNullOrEmpty(existing)) {
      return addOrReplace;
    }
    return SetUtils.removeAndAddOrReplace(existing, remove, addOrReplace, GeneralComparator.createComparator(comparator));
  }


  @Override
  public List<Pair> pair(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB) {
    Iterator<GeneralXynaObject> iterA = listA.iterator();
    Iterator<GeneralXynaObject> iterB = listB.iterator();
    List<Pair> ret = new ArrayList<>();
    while (iterA.hasNext() && iterB.hasNext()) {
      Pair pair = new Pair();
      pair.setFirst(iterA.next());
      pair.setSecond(iterB.next());
      ret.add(pair);
    }
    while (iterA.hasNext()) {
      Pair pair = new Pair();
      pair.setFirst(iterA.next());
      ret.add(pair);
    }
    while (iterB.hasNext()) {
      Pair pair = new Pair();
      pair.setSecond(iterB.next());
      ret.add(pair);
    }
    return ret;
  }


  @Override
  public List<GeneralXynaObject> removeEmpty(List<GeneralXynaObject> list) {
    List<GeneralXynaObject> ret = new ArrayList<>();
    for (GeneralXynaObject gxo : list) {
      if (gxo != null) {
        ret.add(gxo);
      }
    }
    return ret;
  }


  @Override
  public List<GeneralXynaObject> simpleListToXMOMList(GeneralXynaObject from, XmomField fromField, GeneralXynaObject outputType,
                                                      XmomField toField) {
    List<GeneralXynaObject> ret = new ArrayList<>();
    try {
      GetterSetter gsFrom = new GetterSetter(fromField.getField());
      List<?> innerList = (List<?>) gsFrom.getFrom(from);
      GetterSetter gsTo = new GetterSetter(toField.getField());
      for (Object o : innerList) {
        ret.add(gsTo.setTo(outputType.clone(), o));
      }
      return ret;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<GeneralXynaObject> subList(List<GeneralXynaObject> list, base.math.IntegerNumber start, base.math.IntegerNumber length) {
    int size = list.size();
    int begin = (int) start.getValue();
    if (begin < 0) {
      begin = begin + size;
      if (begin < 0) {
        begin = 0;
      }
    }
    int len = (int) length.getValue();
    if (len <= 0) {
      len = size;
    }

    int end = begin + len;
    if (end > size) {
      end = size;
    }

    return new ArrayList<>(list).subList(begin, end);
  }

}

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
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

import base.list.SortCriterion;



public class GeneralComparator {

  private GeneralComparator() {
  }


  public static Comparator<? super GeneralXynaObject> createComparator(List<? extends SortCriterion> sortCriteria) {
    ComparatorList<GeneralXynaObject> comparator = new ComparatorList<>(sortCriteria.size());
    for (SortCriterion sc : sortCriteria) {
      comparator.add(new PartComparator(sc));
    }
    return ComparatorUtils.nullAware(comparator, false);
  }


  private static class PartComparator implements Comparator<GeneralXynaObject>, ObjectComparator.NotComparableHandler {

    private GetterSetter getterSetter;
    private Comparator<? super Object> comp;


    public PartComparator(SortCriterion sc) {
      getterSetter = new GetterSetter(sc.getFieldName());
      Comparator<? super Object> c = new ObjectComparator(this);
      if (sc.getDescending()) {
        c = ComparatorUtils.reverse(c);
      }
      this.comp = ComparatorUtils.nullAware(c, sc.getNullFirst());
    }


    @Override
    public int compare(GeneralXynaObject o1, GeneralXynaObject o2) {

      try {
        return comp.compare(getterSetter.getFrom(o1), getterSetter.getFrom(o2));
      } catch (InvalidObjectPathException e) {
        throw new SortException("Could not access " + getterSetter.getPath(), e);
      }
    }


    @Override
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


  /**
   * FIXME in ComparatorUtils auslagern
   */
  private static class ComparatorList<T> implements Comparator<T> {

    private List<Comparator<T>> subComparators;


    public ComparatorList(int size) {
      subComparators = new ArrayList<>(size);
    }


    public void add(Comparator<T> comparator) {
      this.subComparators.add(comparator);
    }


    @Override
    public int compare(T o1, T o2) {
      int comp = 0;
      for (Comparator<T> sub : subComparators) {
        comp = sub.compare(o1, o2);
        if (comp != 0) {
          return comp;
        }
      }
      return 0;
    }

  }

  /**
   * FIXME in ComparatorUtils auslagern
   */
  private static class ObjectComparator implements Comparator<Object> {

    public static interface NotComparableHandler {

      public int compareNotComparable(Object o1, Object o2);
    }


    private NotComparableHandler notComparableHandler;


    public ObjectComparator(NotComparableHandler notComparableHandler) {
      this.notComparableHandler = notComparableHandler;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public int compare(Object o1, Object o2) {
      if (o1 instanceof Comparable) {
        return ((Comparable) o1).compareTo(o2);
      } else {
        return notComparableHandler.compareNotComparable(o1, o2);
      }
    }

  }


  public static Comparator<? super GeneralXynaObject> createComparator(base.list.comparator.Comparator comparator) {
    return new GeneralXynaObjectComparator(comparator);
  }


  public static class GeneralXynaObjectComparator implements Comparator<GeneralXynaObject> {

    private base.list.comparator.Comparator comparator;


    public GeneralXynaObjectComparator(base.list.comparator.Comparator comparator) {
      this.comparator = comparator;
    }


    @Override
    public int compare(GeneralXynaObject gxo1, GeneralXynaObject gxo2) {
      return comparator.compare(gxo1, gxo2);
    }

  }


}

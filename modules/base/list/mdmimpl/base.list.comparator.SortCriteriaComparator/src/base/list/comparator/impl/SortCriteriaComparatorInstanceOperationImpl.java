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



import java.util.Comparator;
import java.util.List;

import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.utils.misc.ComparatorUtils.ComparatorList;
import com.gip.xyna.utils.misc.ComparatorUtils.ObjectComparator;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

import base.list.SortCriterion;
import base.list.comparator.SortCriteriaComparator;
import base.list.comparator.SortCriteriaComparatorInstanceOperation;
import base.list.comparator.SortCriteriaComparatorSuperProxy;



public class SortCriteriaComparatorInstanceOperationImpl extends SortCriteriaComparatorSuperProxy
    implements
      SortCriteriaComparatorInstanceOperation {

  private static final long serialVersionUID = 1L;
  private transient Comparator<? super GeneralXynaObject> comparator;


  public SortCriteriaComparatorInstanceOperationImpl(SortCriteriaComparator instanceVar) {
    super(instanceVar);
  }


  public int compare(GeneralXynaObject anyType1, GeneralXynaObject anyType2) {
    if (comparator == null) {
      comparator = createComparator(getInstanceVar().getSortCriterion());
    }
    return comparator.compare(anyType1, anyType2);
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


  public static Comparator<? super GeneralXynaObject> createComparator(List<? extends SortCriterion> sortCriteria) {
    ComparatorList<GeneralXynaObject> comparator = new ComparatorList<>(sortCriteria.size());
    for (SortCriterion sc : sortCriteria) {
      comparator.add(new PartComparator(sc));
    }
    return ComparatorUtils.nullAware(comparator, false);
  }


  private static class PartComparator implements Comparator<GeneralXynaObject>, ObjectComparator.NotComparableHandler {

    private String path;
    private Comparator<? super Object> comp;


    public PartComparator(SortCriterion sc) {
      String path = sc.getFieldName(); //FIXME besser GetterSetter verwenden? leider in anderem Service. Instancemethoden in XMomField?
      if (path.startsWith("%")) {
        int idx = path.indexOf('.');
        this.path = path.substring(idx + 1);
      } else {
        this.path = path;
      }
      Comparator<? super Object> c = new ObjectComparator(this);
      if (sc.getDescending()) {
        c = ComparatorUtils.reverse(c);
      }
      this.comp = ComparatorUtils.nullAware(c, sc.getNullFirst());
    }


    @Override
    public int compare(GeneralXynaObject o1, GeneralXynaObject o2) {
      try {
        return comp.compare(getPath(o1), getPath(o2));
      } catch (InvalidObjectPathException e) {
        throw new SortException("Could not access " + path, e);
      }
    }


    private Object getPath(GeneralXynaObject o) throws InvalidObjectPathException {
      try {
        return o.get(path);
      } catch (NullPointerException e) {
        return null;
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


}

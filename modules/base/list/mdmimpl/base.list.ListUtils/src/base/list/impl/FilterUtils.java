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



import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import base.list.ZeroComparison;
import base.list.comparator.Comparator;



public class FilterUtils {


  public static Filter<GeneralXynaObject> createFilter(Comparator comparator, GeneralXynaObject compareTo) {
    return new ZeroFilter(GeneralComparator.createComparator(comparator), compareTo);
  }


  public static Filter<GeneralXynaObject> createFilter(Comparator comparator, GeneralXynaObject compareTo, ZeroComparison zeroComparison) {
    if ("=".equals(zeroComparison.getOperator())) {
      return new ZeroFilter(GeneralComparator.createComparator(comparator), compareTo);
    } else {
      return new OperationFilter(GeneralComparator.createComparator(comparator), compareTo, zeroComparison.getOperator());
    }
  }


  private static class ZeroFilter implements Filter<GeneralXynaObject> {

    private java.util.Comparator<? super GeneralXynaObject> comparator;
    private GeneralXynaObject compareTo;


    public ZeroFilter(java.util.Comparator<? super GeneralXynaObject> comparator, GeneralXynaObject compareTo) {
      this.comparator = comparator;
      this.compareTo = compareTo;
    }


    @Override
    public boolean accept(GeneralXynaObject value) {
      return comparator.compare(value, compareTo) == 0;
    }

  }

  private static class OperationFilter implements Filter<GeneralXynaObject> {

    private java.util.Comparator<? super GeneralXynaObject> comparator;
    private GeneralXynaObject compareTo;
    private OperationAcceptor operationAcceptor;


    public OperationFilter(java.util.Comparator<? super GeneralXynaObject> comparator, GeneralXynaObject compareTo, String operation) {
      this.comparator = comparator;
      this.compareTo = compareTo;
      this.operationAcceptor = OperationAcceptor.forOperation(operation);
      if ("=".equals(operation)) {

      }

    }


    @Override
    public boolean accept(GeneralXynaObject value) {
      return operationAcceptor.accept(comparator.compare(value, compareTo));
    }

  }

  private abstract static class OperationAcceptor {

    public abstract boolean accept(int value);


    public static OperationAcceptor forOperation(String operation) {
      if ("=".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value == 0;
          }
        };
      } else if ("!=".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value != 0;
          }
        };
      } else if ("<>".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value != 0;
          }
        };
      } else if ("<".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value < 0;
          }
        };
      } else if ("<=".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value <= 0;
          }
        };
      } else if (">".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value > 0;
          }
        };
      } else if (">=".equals(operation)) {
        return new OperationAcceptor() {

          public boolean accept(int value) {
            return value >= 0;
          }
        };
      }
      throw new UnsupportedOperationException("Operation " + operation + " is not supported");
    }
  }


  public static Filter<GeneralXynaObject> createFilter(base.list.filter.Filter filter) {
    return new GeneralXynaObjectFilter(filter);
  }


  public static class GeneralXynaObjectFilter implements Filter<GeneralXynaObject> {

    private base.list.filter.Filter filter;


    public GeneralXynaObjectFilter(base.list.filter.Filter filter) {
      this.filter = filter;
    }


    @Override
    public boolean accept(GeneralXynaObject value) {
      return filter.accept(value);
    }

  }


}
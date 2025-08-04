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

import base.list.ZeroComparison;
import base.list.filter.Filter;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import java.util.Comparator;

public class FilterUtils {
   public static CollectionUtils.Filter<GeneralXynaObject> createFilter(base.list.comparator.Comparator comparator, GeneralXynaObject compareTo) {
      return new ZeroFilter(GeneralComparator.createComparator(comparator), compareTo);
   }

   public static CollectionUtils.Filter<GeneralXynaObject> createFilter(base.list.comparator.Comparator comparator, GeneralXynaObject compareTo, ZeroComparison zeroComparison) {
      return (CollectionUtils.Filter)("=".equals(zeroComparison.getOperator()) ? new ZeroFilter(GeneralComparator.createComparator(comparator), compareTo) : new OperationFilter(GeneralComparator.createComparator(comparator), compareTo, zeroComparison.getOperator()));
   }

   public static CollectionUtils.Filter<GeneralXynaObject> createFilter(Filter filter) {
      return new GeneralXynaObjectFilter(filter);
   }

   private static class ZeroFilter implements CollectionUtils.Filter<GeneralXynaObject> {
      private Comparator<GeneralXynaObject> comparator;
      private GeneralXynaObject compareTo;

      public ZeroFilter(Comparator<GeneralXynaObject> comparator, GeneralXynaObject compareTo) {
         this.comparator = comparator;
         this.compareTo = compareTo;
      }

      public boolean accept(GeneralXynaObject value) {
         return this.comparator.compare(value, this.compareTo) == 0;
      }
   }

   private static class OperationFilter implements CollectionUtils.Filter<GeneralXynaObject> {
      private Comparator<GeneralXynaObject> comparator;
      private GeneralXynaObject compareTo;
      private OperationAcceptor operationAcceptor;

      public OperationFilter(Comparator<GeneralXynaObject> comparator, GeneralXynaObject compareTo, String operation) {
         this.comparator = comparator;
         this.compareTo = compareTo;
         this.operationAcceptor = FilterUtils.OperationAcceptor.forOperation(operation);
         if ("=".equals(operation)) {
         }

      }

      public boolean accept(GeneralXynaObject value) {
         return this.operationAcceptor.accept(this.comparator.compare(value, this.compareTo));
      }
   }

   private abstract static class OperationAcceptor {
      public abstract boolean accept(int var1);

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
         } else {
            throw new UnsupportedOperationException("Operation " + operation + " is not supported");
         }
      }
   }

   public static class GeneralXynaObjectFilter implements CollectionUtils.Filter<GeneralXynaObject> {
      private Filter filter;

      public GeneralXynaObjectFilter(Filter filter) {
         this.filter = filter;
      }

      public boolean accept(GeneralXynaObject value) {
         return this.filter.accept(value);
      }
   }
}

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
package com.gip.xyna.xsor.indices.search;

import java.util.Arrays;

public enum ComparisionAlgorithm {
  Object() {
    @Override
    public boolean areEqual(java.lang.Object one, java.lang.Object another) {
      return one.equals(another);
    }
    @Override
    public int calculateHashCode(java.lang.Object object) {
      return object.hashCode();
    }
    @Override
    public int compare(java.lang.Object one, java.lang.Object another) {
      if (one instanceof Comparable &&
          another instanceof Comparable) {
        return ((Comparable)one).compareTo(another);
      } else {
        throw new IllegalArgumentException("Can not compare uncomperable objects");
      }
    }
  },
  ByteArray(byte[].class) {
    @Override
    public boolean areEqual(java.lang.Object one, java.lang.Object another) {
      if (one instanceof byte[] &&
          another instanceof byte[]) {
        return Arrays.equals((byte[])one, (byte[])another);
      } else {
        return false;
      }
    }
    @Override
    public int calculateHashCode(java.lang.Object object) {
      if (object instanceof byte[]) {
        return Arrays.hashCode((byte[])object);
      } else {
        return 0; // TODO throw something?
      }
    }
    @Override
    public int compare(java.lang.Object one, java.lang.Object another) {
      if (one instanceof byte[] &&
          another instanceof byte[]) {
        return ComparisionAlgorithm.compareByteArrays((byte[])one, (byte[])another);
      } else {
        throw new IllegalArgumentException("Can not compare uncomperable objects");
      }
    }
  };
  
  
  private Class<?>[] types;
  
  private ComparisionAlgorithm(Class<?>... typesForAlgorithm) {
    this.types = typesForAlgorithm;
  }
  
  
  public abstract boolean areEqual(Object one, Object another);
  
  public abstract int calculateHashCode(Object object);
  
  public abstract int compare(Object one, Object another);
  
  public static ComparisionAlgorithm getComparisonAlgorithmType(Object object) {
    for (ComparisionAlgorithm algorithmType : values()) {
      for (Class<?> type : algorithmType.types) {
        if (type.isInstance(object)) {
          return algorithmType;
        }
      }
    }
    return ComparisionAlgorithm.Object;
  }
  
  
  private static int compareByteArrays(byte[] a, byte[] b) {
    if (a.length > b.length) {
      return 1;
    }
    if (a.length < b.length) {
      return -1;
    }
    for (int i=a.length-1; i>=0; i--) {
      if (a[i] > b[i]) {
        return 1;
      }
      if (a[i] < b[i]) {
        return -1;
      }
    }
    return 0;
  }
  
}

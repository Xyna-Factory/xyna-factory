/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;


public class TreePathSegment {

  private final String name;
  private final int index;
  
  
  public TreePathSegment(String name) {
    this(name, 0);
  }
  
  
  public TreePathSegment(String name, int index) {
    if (name == null) {
      throw new IllegalArgumentException("TreePathSegment name must not be null.");
    }
    this.name = name;
    this.index = index;
  }


  public String getName() {
    return name;
  }


  public int getIndex() {
    return index;
  }


  public String asString() {
    if (index > 0) {
      return asStringWithIndex();
    }
    return name;
  }
  
  
  public String asStringWithIndex() {
    return name + "[" + index + "]";
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TreePathSegment)) {
      return false;
    }
    TreePathSegment seg = (TreePathSegment) obj;
    if (seg.index != this.index) {
      return false;
    }
    return this.name.equals(seg.name);
  }
  
}

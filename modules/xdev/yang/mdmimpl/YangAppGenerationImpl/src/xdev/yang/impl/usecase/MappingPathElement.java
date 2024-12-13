/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xdev.yang.impl.usecase;

import java.util.List;

public class MappingPathElement implements Comparable<MappingPathElement> {

  private String yangPath;
  private String namespace;
  private String keyword;


  public MappingPathElement(String yangPath, String namespace, String keyword) {
    this.yangPath = yangPath;
    this.namespace = namespace;
    this.keyword = keyword;
  }


  public String getYangPath() {
    return yangPath;
  }


  public String getNamespace() {
    return namespace;
  }


  public String getKeyword() {
    return keyword;
  }


  @Override
  public int compareTo(MappingPathElement o) {
    int pathComparision = yangPath.compareTo(o.yangPath);
    if (pathComparision != 0) {
      return pathComparision;
    }

    return namespace.compareTo(o.namespace);
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof MappingPathElement)) {
      return false;
    }

    return compareTo((MappingPathElement) obj) == 0;
  }
  
  @Override
  public String toString() {
    return String.format("MappingPathElement: \n\t%s\n\t%s\n\t%s", yangPath, namespace, keyword);
  }
  
  public static int compareLists(List<MappingPathElement> pathList, List<MappingPathElement> otherPathList) {
    int minLength = Math.min(pathList.size(), otherPathList.size());
    for (int i = 0; i < minLength; i++) {
      int elementComparision = pathList.get(i).compareTo(otherPathList.get(i));
      if(elementComparision != 0) {
        return elementComparision;
      }
    }

    //all entries up to the minimum length are the same
    return pathList.size() - otherPathList.size();
  }
  
  public static boolean isMoreSpecificPath(List<MappingPathElement> base, List<MappingPathElement> candidate) {
    if(candidate.size() < base.size()) {
      return false;
    }
    return compareLists(base, candidate.subList(0, base.size())) == 0;
  }
}

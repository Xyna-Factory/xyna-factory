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

package xdev.yang.impl.operation;


public class PathTools {

  public boolean identifiersAreEqual(String id1, String id2) {
    return removeOptionalPrefix(id1).equals(removeOptionalPrefix(id2));
  }
  
  
  private String removeOptionalPrefix(String id) {
    if (id == null) { return ""; }
    if (!id.contains(":")) { return id; }
    return id.substring(id.indexOf(":") + 1);
  }

}

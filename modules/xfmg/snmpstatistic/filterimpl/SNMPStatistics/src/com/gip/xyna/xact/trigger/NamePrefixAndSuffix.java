/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xact.trigger;



public class NamePrefixAndSuffix {

  private final String prefix;
  private final String suffix;


  public NamePrefixAndSuffix(String prefix, String suffix) {
    this.prefix = prefix;
    this.suffix = suffix;
  }


  public String getPrefix() {
    return prefix;
  }


  public String getSuffix() {
    return suffix;
  }


  public boolean equals(Object otherObject) {
    if (!(otherObject instanceof NamePrefixAndSuffix)) {
      return false;
    }
    if (otherObject == this) {
      return true;
    }
    return prefix.equals(((NamePrefixAndSuffix) otherObject).getPrefix())
        && suffix.equals(((NamePrefixAndSuffix) otherObject).getSuffix());
  }


  public int hashCode() {
    return prefix.hashCode() + suffix.hashCode();
  }

}

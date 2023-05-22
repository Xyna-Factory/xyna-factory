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

package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.util.Objects;



public class CustomOrderEntrance extends OrderEntrance {

  private static final long serialVersionUID = 1L;

  private Long definingRevision;


  public CustomOrderEntrance(String name, Long definingRevision) {
    super(OrderEntranceType.custom, name);
    this.definingRevision = definingRevision;
  }


  public Long getDefiningRevision() {
    return definingRevision;
  }


  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CustomOrderEntrance other = (CustomOrderEntrance) obj;
    return Objects.equals(name, other.name) && Objects.equals(type, other.type)
        && Objects.deepEquals(definingRevision, other.definingRevision);
  }


  public int hashCode() {
    return Objects.hash(name, type, definingRevision);
  }

}

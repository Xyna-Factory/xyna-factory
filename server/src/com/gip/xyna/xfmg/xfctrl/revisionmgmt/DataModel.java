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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;

import com.gip.xyna.utils.StringUtils;


/**
 *
 */
public class DataModel extends RuntimeContext {

  private static final long serialVersionUID = 1L;

  public DataModel(String dataModel) {
    super(dataModel);
  }

  public int compareTo(RuntimeContext o) {
    if (o instanceof DataModel) {
      return this.getName().compareTo(o.getName());
    }
    return super.compareToClass(o.getClass());
  }

  public String serializeToString() {
    return StringUtils.mask(getName(),'/');
  }
  
  @Override
  public String toString() {
    return "DataModel '" + getName() + "'";
  }
  
  @Override
  public RuntimeContextType getType() {
    return RuntimeContextType.DataModel;
  }
  
  @Override
  public String getGUIRepresentation() {
    throw new UnsupportedOperationException("getGUIRepresentation not supported for dataModel");
  }
}

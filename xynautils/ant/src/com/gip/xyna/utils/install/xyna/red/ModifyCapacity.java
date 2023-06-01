/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.utils.install.xyna.red;


/**
 *
 */
public class ModifyCapacity extends CapacityTask {
  
  private String newName = null;
  private int cardinality = -1;
  private boolean includeAvailable = false;
  private boolean available;

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.install.xfbb.SOAPTask#getMessage()
   */
  @Override
  protected String getMessage() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("<ns1:modifyCapacityRequest xmlns:ns1=\"" + NAMESPACE_MSGS + "\">");
    buffer.append("<ns1:name>");
    buffer.append(getName());
    buffer.append("</ns1:name>");
    if ((newName != null) && (!newName.equals(""))) {
      buffer.append("<ns1:newName>");
      buffer.append(newName);
      buffer.append("</ns1:newName>");
    }
    if (cardinality > -1) {
      buffer.append("<ns1:cardinality>");
      buffer.append(cardinality);
      buffer.append("</ns1:cardinality>");
    }
    if (includeAvailable) {
      buffer.append("<ns1:available>");
      buffer.append(available);
      buffer.append("</ns1:available>");
    }
    buffer.append("</ns1:modifyCapacityRequest>");
    return buffer.toString();
  }


  /* (non-Javadoc)
   * @see com.gip.xyna.utils.install.xfbb.SOAPTask#getOperation()
   */
  @Override
  protected String getSOAPAction() {
    return NAMESPACE_OPERATION + "/modifyCapacity";
  }


  /**
   * @param newName the newName to set
   */
  public void setNewName(String newName) {
    this.newName = newName;
  }


  /**
   * @param cardinality the cardinality to set
   */
  public void setCardinality(int cardinality) {
    this.cardinality = cardinality;
  }


  /**
   * @param available the available to set
   */
  public void setAvailable(boolean available) {
    this.available = available;
    includeAvailable = true;
  }


}
